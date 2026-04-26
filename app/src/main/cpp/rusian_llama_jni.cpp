#include <jni.h>
#include <android/log.h>

#include <algorithm>
#include <atomic>
#include <cstdint>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

#include "llama.h"

namespace {

constexpr const char * LOG_TAG = "RusianLlama";

struct LlamaSession {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    const llama_vocab * vocab = nullptr;
    int n_ctx = 2048;
    int n_threads = 2;
    std::atomic_bool cancel_requested { false };
};

std::once_flag backend_once;

void throw_runtime(JNIEnv * env, const std::string & message) {
    jclass clazz = env->FindClass("java/lang/RuntimeException");
    if (clazz != nullptr) {
        env->ThrowNew(clazz, message.c_str());
    }
}

std::string jstring_to_string(JNIEnv * env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        return {};
    }
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

std::vector<llama_token> tokenize_prompt(const llama_vocab * vocab, const std::string & prompt) {
    int32_t count = llama_tokenize(
        vocab,
        prompt.c_str(),
        static_cast<int32_t>(prompt.size()),
        nullptr,
        0,
        true,
        true
    );
    if (count == INT32_MIN) {
        throw std::runtime_error("Prompt is too large to tokenize.");
    }
    if (count < 0) {
        count = -count;
    }
    std::vector<llama_token> tokens(static_cast<size_t>(count));
    int32_t actual = llama_tokenize(
        vocab,
        prompt.c_str(),
        static_cast<int32_t>(prompt.size()),
        tokens.data(),
        count,
        true,
        true
    );
    if (actual < 0) {
        throw std::runtime_error("Failed to tokenize prompt.");
    }
    tokens.resize(static_cast<size_t>(actual));
    return tokens;
}

std::string token_to_piece(const llama_vocab * vocab, llama_token token) {
    char buffer[256];
    int32_t length = llama_token_to_piece(vocab, token, buffer, sizeof(buffer), 0, false);
    if (length < 0) {
        std::vector<char> dynamic_buffer(static_cast<size_t>(-length));
        length = llama_token_to_piece(
            vocab,
            token,
            dynamic_buffer.data(),
            static_cast<int32_t>(dynamic_buffer.size()),
            0,
            false
        );
        if (length <= 0) {
            return {};
        }
        return std::string(dynamic_buffer.data(), static_cast<size_t>(length));
    }
    if (length == 0) {
        return {};
    }
    return std::string(buffer, static_cast<size_t>(length));
}

void decode_tokens(LlamaSession * session, llama_batch & batch, const std::vector<llama_token> & tokens, int & n_past) {
    const int n_batch = std::max(1, static_cast<int>(batch.n_tokens));
    for (size_t offset = 0; offset < tokens.size(); offset += static_cast<size_t>(n_batch)) {
        if (session->cancel_requested.load()) {
            throw std::runtime_error("Generation canceled.");
        }
        const int n_eval = std::min(n_batch, static_cast<int>(tokens.size() - offset));
        batch.n_tokens = n_eval;
        for (int i = 0; i < n_eval; ++i) {
            batch.token[i] = tokens[offset + static_cast<size_t>(i)];
            batch.pos[i] = n_past + i;
            batch.n_seq_id[i] = 1;
            batch.seq_id[i][0] = 0;
            batch.logits[i] = (offset + static_cast<size_t>(i) == tokens.size() - 1) ? 1 : 0;
        }
        if (llama_decode(session->ctx, batch) != 0) {
            throw std::runtime_error("llama_decode failed while reading the prompt.");
        }
        n_past += n_eval;
    }
}

void report_progress(JNIEnv * env, jobject callback, jmethodID method, float progress) {
    if (callback == nullptr || method == nullptr) {
        return;
    }
    env->CallVoidMethod(callback, method, std::clamp(progress, 0.0f, 1.0f));
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_rusian_app_data_chat_NativeLlama_nativeCreate(
    JNIEnv * env,
    jobject,
    jstring model_path,
    jint context_size,
    jint threads
) {
    try {
        std::call_once(backend_once, [] {
            llama_backend_init();
        });

        const std::string path = jstring_to_string(env, model_path);
        if (path.empty()) {
            throw std::runtime_error("Model path is empty.");
        }

        auto * session = new LlamaSession();
        session->n_ctx = std::max(512, static_cast<int>(context_size));
        session->n_threads = std::max(1, static_cast<int>(threads));

        llama_model_params model_params = llama_model_default_params();
        model_params.n_gpu_layers = 0;
        model_params.use_mmap = true;
        model_params.use_mlock = false;

        session->model = llama_model_load_from_file(path.c_str(), model_params);
        if (session->model == nullptr) {
            delete session;
            throw std::runtime_error("Failed to load GGUF model.");
        }

        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.n_ctx = static_cast<uint32_t>(session->n_ctx);
        ctx_params.n_batch = 512;
        ctx_params.n_ubatch = 128;
        ctx_params.n_threads = session->n_threads;
        ctx_params.n_threads_batch = session->n_threads;
        ctx_params.abort_callback = [](void * data) -> bool {
            auto * active_session = static_cast<LlamaSession *>(data);
            return active_session != nullptr && active_session->cancel_requested.load();
        };
        ctx_params.abort_callback_data = session;

        session->ctx = llama_init_from_model(session->model, ctx_params);
        if (session->ctx == nullptr) {
            llama_model_free(session->model);
            delete session;
            throw std::runtime_error("Failed to create llama context.");
        }

        session->vocab = llama_model_get_vocab(session->model);
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Loaded model: %s", path.c_str());
        return reinterpret_cast<jlong>(session);
    } catch (const std::exception & e) {
        throw_runtime(env, e.what());
        return 0;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_rusian_app_data_chat_NativeLlama_nativeGenerate(
    JNIEnv * env,
    jobject,
    jlong handle,
    jstring prompt,
    jint max_tokens,
    jfloat temperature,
    jint top_k,
    jfloat top_p,
    jobject progress_callback
) {
    try {
        auto * session = reinterpret_cast<LlamaSession *>(handle);
        if (session == nullptr || session->ctx == nullptr || session->model == nullptr) {
            throw std::runtime_error("Local model is not loaded.");
        }
        session->cancel_requested.store(false);

        jmethodID progress_method = nullptr;
        if (progress_callback != nullptr) {
            jclass callback_class = env->GetObjectClass(progress_callback);
            progress_method = env->GetMethodID(callback_class, "onProgress", "(F)V");
            env->DeleteLocalRef(callback_class);
        }
        report_progress(env, progress_callback, progress_method, 0.0f);

        const std::string prompt_text = jstring_to_string(env, prompt);
        std::vector<llama_token> prompt_tokens = tokenize_prompt(session->vocab, prompt_text);
        if (prompt_tokens.empty()) {
            throw std::runtime_error("Prompt produced no tokens.");
        }
        if (static_cast<int>(prompt_tokens.size()) >= session->n_ctx - 8) {
            const size_t keep = static_cast<size_t>(session->n_ctx - 8);
            prompt_tokens.erase(prompt_tokens.begin(), prompt_tokens.end() - static_cast<std::ptrdiff_t>(keep));
        }

        llama_memory_clear(llama_get_memory(session->ctx), true);

        llama_batch batch = llama_batch_init(512, 0, 1);
        int n_past = 0;
        decode_tokens(session, batch, prompt_tokens, n_past);
        report_progress(env, progress_callback, progress_method, 0.05f);

        llama_sampler * sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
        llama_sampler_chain_add(sampler, llama_sampler_init_top_k(std::max(1, static_cast<int>(top_k))));
        llama_sampler_chain_add(sampler, llama_sampler_init_top_p(std::clamp(static_cast<float>(top_p), 0.05f, 1.0f), 1));
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(std::max(0.05f, static_cast<float>(temperature))));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

        std::string output;
        const int limit = std::max(1, static_cast<int>(max_tokens));
        bool canceled = false;
        for (int i = 0; i < limit && n_past < session->n_ctx; ++i) {
            if (session->cancel_requested.load()) {
                canceled = true;
                break;
            }
            llama_token token = llama_sampler_sample(sampler, session->ctx, -1);
            if (token == LLAMA_TOKEN_NULL || llama_vocab_is_eog(session->vocab, token)) {
                break;
            }
            llama_sampler_accept(sampler, token);
            output += token_to_piece(session->vocab, token);

            batch.n_tokens = 1;
            batch.token[0] = token;
            batch.pos[0] = n_past;
            batch.n_seq_id[0] = 1;
            batch.seq_id[0][0] = 0;
            batch.logits[0] = 1;
            if (llama_decode(session->ctx, batch) != 0) {
                break;
            }
            ++n_past;
            const float token_progress = static_cast<float>(i + 1) / static_cast<float>(limit);
            report_progress(env, progress_callback, progress_method, std::min(0.99f, token_progress));
        }

        llama_sampler_free(sampler);
        llama_batch_free(batch);

        if (!canceled) {
            report_progress(env, progress_callback, progress_method, 1.0f);
        }

        return env->NewStringUTF(output.c_str());
    } catch (const std::exception & e) {
        throw_runtime(env, e.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_rusian_app_data_chat_NativeLlama_nativeCancel(
    JNIEnv *,
    jobject,
    jlong handle
) {
    auto * session = reinterpret_cast<LlamaSession *>(handle);
    if (session != nullptr) {
        session->cancel_requested.store(true);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_rusian_app_data_chat_NativeLlama_nativeClose(
    JNIEnv *,
    jobject,
    jlong handle
) {
    auto * session = reinterpret_cast<LlamaSession *>(handle);
    if (session == nullptr) {
        return;
    }
    if (session->ctx != nullptr) {
        llama_free(session->ctx);
    }
    if (session->model != nullptr) {
        llama_model_free(session->model);
    }
    delete session;
}
