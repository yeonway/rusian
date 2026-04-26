const storageKey = "rusian-web-state-v1";

const routes = [
  { id: "alphabet", label: "알파벳", icon: "Aa" },
  { id: "cards", label: "카드", icon: "Cd" },
  { id: "quiz", label: "퀴즈", icon: "Qz" },
  { id: "chat", label: "채팅", icon: "Ch" },
  { id: "settings", label: "설정", icon: "St" },
];

const russianLetterSpeech = {
  a: "а",
  be: "бэ",
  ve: "вэ",
  ghe: "гэ",
  de: "дэ",
  ie: "е",
  yo: "ё",
  zhe: "жэ",
  ze: "зэ",
  i: "и",
  shorti: "и краткое",
  ka: "ка",
  el: "эль",
  em: "эм",
  en: "эн",
  o: "о",
  pe: "пэ",
  er: "эр",
  es: "эс",
  te: "тэ",
  u: "у",
  ef: "эф",
  kha: "ха",
  tse: "цэ",
  che: "чэ",
  sha: "ша",
  shcha: "ща",
  hard: "твёрдый знак",
  yeri: "ы",
  soft: "мягкий знак",
  e: "э",
  yu: "ю",
  ya: "я",
};

const defaultState = {
  route: "quiz",
  theme: "system",
  dailyGoal: 20,
  categoryId: "all",
  alphabetFilter: "all",
  cardIndex: 0,
  reveal: false,
  quizIndex: 0,
  quizAnswered: null,
  quizCorrect: 0,
  quizTotal: 0,
  quizSeed: 1,
  weakLetters: [],
  reviews: {},
  chat: [
    {
      role: "bot",
      text: "러시아어 표현이나 한국어 뜻을 입력하면 데이터셋에서 가까운 카드를 찾아 보여줍니다.",
    },
  ],
};

const app = document.querySelector("#app");
let data = null;
let state = loadState();

init();

async function init() {
  try {
    const response = await fetch("./assets/content-pack.json");
    data = await response.json();
    applyTheme();
    render();
  } catch (error) {
    app.innerHTML = `<main class="boot"><p>콘텐츠 데이터를 불러오지 못했습니다.</p><small>${escapeHtml(error.message)}</small></main>`;
  }
}

function loadState() {
  try {
    return { ...defaultState, ...JSON.parse(localStorage.getItem(storageKey) || "{}") };
  } catch {
    return { ...defaultState };
  }
}

function saveState() {
  localStorage.setItem(storageKey, JSON.stringify(state));
}

function updateState(patch) {
  state = { ...state, ...patch };
  saveState();
  applyTheme();
  render();
}

function applyTheme() {
  const isDark = state.theme === "dark" ||
    (state.theme === "system" && window.matchMedia?.("(prefers-color-scheme: dark)").matches);
  document.documentElement.dataset.theme = isDark ? "dark" : "light";
}

function render() {
  const screen = renderScreen();
  app.innerHTML = `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">
          <img src="./assets/icon.webp" alt="" />
          <div>
            <div class="brand-title">Rusian Learner</div>
            <div class="brand-subtitle">러시아어 오프라인 학습</div>
          </div>
        </div>
        ${renderNav("nav")}
        <div class="sidebar-foot">
          데이터셋 ${escapeHtml(data.datasetVersion)}<br />
          ${data.alphabet.length}자 · ${activeWords().length}개 표현
        </div>
      </aside>
      <main class="main">${screen}</main>
      ${renderNav("mobile-nav")}
    </div>
  `;
  bindEvents();
}

function renderNav(className) {
  return `
    <nav class="${className}" aria-label="주요 화면">
      ${routes.map((route) => `
        <button type="button" data-route="${route.id}" aria-current="${state.route === route.id ? "page" : "false"}">
          <span class="nav-icon">${route.icon}</span>
          <span>${route.label}</span>
        </button>
      `).join("")}
    </nav>
  `;
}

function renderScreen() {
  if (state.route === "alphabet") return renderAlphabet();
  if (state.route === "cards") return renderCards();
  if (state.route === "chat") return renderChat();
  if (state.route === "settings") return renderSettings();
  return renderQuiz();
}

function renderAlphabet() {
  const weak = new Set(state.weakLetters);
  const filters = [
    ["all", "전체"],
    ["vowel", "모음"],
    ["consonant", "자음"],
    ["sign", "기호"],
    ["weak", `헷갈림 ${weak.size}`],
  ];
  const letters = data.alphabet
    .filter((letter) => {
      if (state.alphabetFilter === "weak") return weak.has(letter.id);
      if (state.alphabetFilter === "all") return true;
      return letter.letterType.toLowerCase() === state.alphabetFilter;
    })
    .sort((a, b) => a.sortOrder - b.sortOrder);

  return `
    <section class="screen">
      <header class="screen-header">
        <div>
          <h1>알파벳 감각 익히기</h1>
          <p>러시아 문자를 누르면 발음을 들을 수 있습니다.</p>
        </div>
        <button type="button" class="button-secondary" data-quiz-alpha>알파벳 퀴즈</button>
      </header>
      <div class="toolbar">
        <div class="chip-row">
          ${filters.map(([id, label]) => `
            <button type="button" class="chip ${state.alphabetFilter === id ? "chip-selected" : ""}" data-alpha-filter="${id}">
              ${escapeHtml(label)}
            </button>
          `).join("")}
        </div>
      </div>
      <div class="alphabet-grid">
        ${letters.map((letter) => renderLetterTile(letter, weak.has(letter.id))).join("")}
      </div>
    </section>
  `;
}

function renderLetterTile(letter, isWeak) {
  return `
    <button type="button" class="letter-tile ${isWeak ? "is-weak" : ""}" data-letter="${letter.id}">
      <span class="letter-main">${letter.uppercase} ${letter.lowercase}</span>
      <span class="letter-hint">${escapeHtml(letter.pronunciationHint || letter.nameKo || letter.romanization || "")}</span>
    </button>
  `;
}

function renderCards() {
  const words = filteredWords();
  const card = words[boundedIndex(state.cardIndex, words.length)];
  const progress = categoryProgress();

  return `
    <section class="screen">
      <header class="screen-header">
        <div>
          <h1>표현 카드</h1>
          <p>러시아어 표현을 보고 뜻과 예문을 확인합니다.</p>
        </div>
        ${renderCategorySelect()}
      </header>
      <div class="grid grid-3">
        <div class="panel metric">
          <span class="metric-value">${dueWords().length}</span>
          <span class="metric-label">남은 카드</span>
        </div>
        <div class="panel metric">
          <span class="metric-value">${learnedWordCount()}</span>
          <span class="metric-label">학습한 카드</span>
        </div>
        <div class="panel metric">
          <span class="metric-value">${Math.min(state.dailyGoal, words.length)}</span>
          <span class="metric-label">하루 목표</span>
        </div>
      </div>
      ${card ? renderStudyCard(card, words.length) : `<div class="panel empty">선택한 카테고리에 카드가 없습니다.</div>`}
      <div class="panel panel-pad">
        <h2 class="panel-title">상황별 진행도</h2>
        <div class="grid">
          ${progress.map((item) => `
            <div>
              <div class="toolbar">
                <strong>${escapeHtml(item.name)}</strong>
                <span class="muted">${item.learned}/${item.total}</span>
              </div>
              <div class="progress"><span style="width:${item.total ? (item.learned / item.total) * 100 : 0}%"></span></div>
            </div>
          `).join("")}
        </div>
      </div>
    </section>
  `;
}

function renderStudyCard(card, total) {
  const review = state.reviews[card.id] || {};
  return `
    <article class="panel study-card">
      <div>
        <div class="chip-row" style="justify-content:center;margin-bottom:18px">
          <span class="chip">${state.cardIndex + 1}/${total}</span>
          <span class="chip">${escapeHtml(categoryName(card.categoryId))}</span>
          <span class="chip">복습 ${review.count || 0}</span>
        </div>
        <h2>${escapeHtml(card.word)}</h2>
        <p class="pronunciation">${escapeHtml(card.pronunciationKo || card.transliteration || card.stress || "")}</p>
        ${state.reveal ? renderMeaning(card) : `<button type="button" class="button-secondary" data-reveal style="margin-top:28px">뜻 보기</button>`}
      </div>
    </article>
    <div class="toolbar">
      <div class="button-row">
        <button type="button" class="button-ghost" data-card-prev>이전</button>
        <button type="button" class="button-ghost" data-card-next>다음</button>
      </div>
      <div class="button-row">
        <button type="button" class="button-secondary" data-rate="again">다시</button>
        <button type="button" class="button-secondary" data-rate="hard">어려움</button>
        <button type="button" data-rate="good">좋음</button>
        <button type="button" data-rate="easy">쉬움</button>
      </div>
    </div>
  `;
}

function renderMeaning(card) {
  return `
    <div class="meaning">
      <strong>${escapeHtml(card.meanings.join(", "))}</strong>
      ${card.example?.ru ? `<span>${escapeHtml(card.example.ru)}</span>` : ""}
      ${card.example?.ko ? `<span class="example">${escapeHtml(card.example.ko)}</span>` : ""}
      ${card.usageNote ? `<span class="example">${escapeHtml(card.usageNote)}</span>` : ""}
    </div>
  `;
}

function renderQuiz() {
  const questions = quizQuestions();
  const current = questions[state.quizIndex % questions.length];
  if (!current) {
    return `<section class="screen"><div class="panel empty">퀴즈로 풀 카드가 없습니다.</div></section>`;
  }

  const answered = state.quizAnswered;
  const ratio = questions.length ? ((state.quizIndex % questions.length) / questions.length) * 100 : 0;

  return `
    <section class="screen">
      <header class="screen-header">
        <div>
          <h1>단어 뜻 보기</h1>
          <p>한국어 뜻에 맞는 러시아어 표현을 고릅니다.</p>
        </div>
        ${renderCategorySelect()}
      </header>
      <div class="panel panel-pad">
        <div class="toolbar">
          <span class="chip">문제 ${(state.quizIndex % questions.length) + 1}/${questions.length}</span>
          <span class="chip">정답 ${state.quizCorrect}/${state.quizTotal}</span>
        </div>
        <div class="progress" style="margin-top:12px"><span style="width:${ratio}%"></span></div>
      </div>
      <article class="panel study-card">
        <div>
          <div class="chip-row" style="justify-content:center;margin-bottom:18px">
            <span class="chip">${escapeHtml(categoryName(current.answer.categoryId))}</span>
          </div>
          <h2>${escapeHtml(current.answer.meanings.join(", "))}</h2>
          ${answered ? renderQuizResult(current, answered) : ""}
        </div>
      </article>
      <div class="quiz-options">
        ${current.options.map((option) => renderQuizOption(option, current.answer, answered)).join("")}
      </div>
      ${answered ? `<button type="button" data-quiz-next>${state.quizIndex >= questions.length - 1 ? "다시 풀기" : "다음 문제"}</button>` : ""}
    </section>
  `;
}

function renderQuizOption(option, answer, answered) {
  let className = "quiz-option";
  if (answered && option.id === answer.id) className += " correct";
  if (answered && option.id === answered && option.id !== answer.id) className += " wrong";
  return `
    <button type="button" class="${className}" data-quiz-option="${option.id}" ${answered ? "disabled" : ""}>
      <strong>${escapeHtml(option.word)}</strong><br />
      <small>${escapeHtml(option.pronunciationKo || option.transliteration || "")}</small>
    </button>
  `;
}

function renderQuizResult(question, selectedId) {
  const correct = selectedId === question.answer.id;
  return `
    <div class="meaning" style="background:${correct ? "var(--success-2)" : "var(--danger-2)"};color:${correct ? "var(--success)" : "var(--danger)"}">
      <strong>${correct ? "맞았어요" : `정답: ${escapeHtml(question.answer.word)}`}</strong>
      ${question.answer.example?.ru ? `<span>${escapeHtml(question.answer.example.ru)}</span>` : ""}
      ${question.answer.example?.ko ? `<span>${escapeHtml(question.answer.example.ko)}</span>` : ""}
    </div>
  `;
}

function renderChat() {
  return `
    <section class="screen">
      <header class="screen-header">
        <div>
          <h1>채팅</h1>
          <p>내장 콘텐츠 기반으로 표현을 찾아 답합니다.</p>
        </div>
        <button type="button" class="button-ghost" data-clear-chat>대화 지우기</button>
      </header>
      <div class="panel panel-pad">
        <div class="chat-log">
          ${state.chat.map((message) => `
            <div class="bubble ${message.role === "user" ? "bubble-user" : "bubble-bot"}">${formatMessage(message.text)}</div>
          `).join("")}
        </div>
      </div>
      <form class="chat-input" data-chat-form>
        <input name="message" autocomplete="off" placeholder="예: 고마워, привет, 미안해" />
        <button type="submit">보내기</button>
      </form>
    </section>
  `;
}

function renderSettings() {
  return `
    <section class="screen">
      <header class="screen-header">
        <div>
          <h1>설정</h1>
          <p>웹 브라우저에 저장되는 학습 설정입니다.</p>
        </div>
      </header>
      <div class="grid grid-2">
        <div class="panel panel-pad form-grid">
          <h2 class="panel-title">화면</h2>
          <div class="field">
            <label for="theme">테마</label>
            <select id="theme" data-setting="theme">
              <option value="system" ${state.theme === "system" ? "selected" : ""}>시스템</option>
              <option value="light" ${state.theme === "light" ? "selected" : ""}>라이트</option>
              <option value="dark" ${state.theme === "dark" ? "selected" : ""}>다크</option>
            </select>
          </div>
        </div>
        <div class="panel panel-pad form-grid">
          <h2 class="panel-title">학습</h2>
          <div class="field">
            <label for="dailyGoal">하루 목표 ${state.dailyGoal}개</label>
            <div class="range-line">
              <input id="dailyGoal" type="range" min="5" max="100" step="5" value="${state.dailyGoal}" data-setting="dailyGoal" />
              <strong>${state.dailyGoal}</strong>
            </div>
          </div>
        </div>
        <div class="panel panel-pad form-grid">
          <h2 class="panel-title">데이터셋</h2>
          <span class="muted">${escapeHtml(data.datasetVersion)}</span>
          <div class="chip-row">
            <span class="chip">카테고리 ${data.categories.length}</span>
            <span class="chip">알파벳 ${data.alphabet.length}</span>
            <span class="chip">표현 ${activeWords().length}</span>
          </div>
        </div>
        <div class="panel panel-pad form-grid">
          <h2 class="panel-title">진행도</h2>
          <div class="chip-row">
            <span class="chip">리뷰 ${Object.values(state.reviews).reduce((sum, review) => sum + (review.count || 0), 0)}</span>
            <span class="chip">헷갈림 ${state.weakLetters.length}</span>
          </div>
          <button type="button" class="button-danger" data-reset-progress>학습 기록 초기화</button>
        </div>
      </div>
    </section>
  `;
}

function renderCategorySelect() {
  return `
    <label class="field" style="min-width:220px">
      <span class="muted">카테고리</span>
      <select data-category-select>
        <option value="all" ${state.categoryId === "all" ? "selected" : ""}>전체</option>
        ${data.categories.map((category) => `
          <option value="${category.id}" ${state.categoryId === category.id ? "selected" : ""}>${escapeHtml(category.name)}</option>
        `).join("")}
      </select>
    </label>
  `;
}

function bindEvents() {
  document.querySelectorAll("[data-route]").forEach((button) => {
    button.addEventListener("click", () => updateState({ route: button.dataset.route }));
  });

  document.querySelectorAll("[data-alpha-filter]").forEach((button) => {
    button.addEventListener("click", () => updateState({ alphabetFilter: button.dataset.alphaFilter }));
  });

  document.querySelectorAll("[data-letter]").forEach((button) => {
    button.addEventListener("click", () => speakLetter(button.dataset.letter));
    button.addEventListener("contextmenu", (event) => {
      event.preventDefault();
      toggleWeakLetter(button.dataset.letter);
    });
  });

  document.querySelector("[data-quiz-alpha]")?.addEventListener("click", () => {
    const first = data.alphabet[0];
    if (first) speak(first.uppercase, "ru-RU");
  });

  document.querySelector("[data-category-select]")?.addEventListener("change", (event) => {
    updateState({ categoryId: event.target.value, cardIndex: 0, quizIndex: 0, quizAnswered: null, quizSeed: Date.now() });
  });

  document.querySelector("[data-reveal]")?.addEventListener("click", () => updateState({ reveal: true }));
  document.querySelector("[data-card-prev]")?.addEventListener("click", () => moveCard(-1));
  document.querySelector("[data-card-next]")?.addEventListener("click", () => moveCard(1));

  document.querySelectorAll("[data-rate]").forEach((button) => {
    button.addEventListener("click", () => rateCurrentCard(button.dataset.rate));
  });

  document.querySelectorAll("[data-quiz-option]").forEach((button) => {
    button.addEventListener("click", () => answerQuiz(button.dataset.quizOption));
  });

  document.querySelector("[data-quiz-next]")?.addEventListener("click", nextQuiz);

  document.querySelector("[data-chat-form]")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    sendChat(String(formData.get("message") || ""));
  });

  document.querySelector("[data-clear-chat]")?.addEventListener("click", () => {
    updateState({ chat: defaultState.chat });
  });

  document.querySelectorAll("[data-setting]").forEach((input) => {
    input.addEventListener("input", (event) => {
      const key = event.target.dataset.setting;
      const value = key === "dailyGoal" ? Number(event.target.value) : event.target.value;
      updateState({ [key]: value });
    });
  });

  document.querySelector("[data-reset-progress]")?.addEventListener("click", () => {
    if (confirm("학습 기록을 초기화할까요?")) {
      updateState({ reviews: {}, weakLetters: [], cardIndex: 0, quizIndex: 0, quizAnswered: null, quizCorrect: 0, quizTotal: 0 });
    }
  });
}

function activeWords() {
  return data.words.filter((word) => word.active !== false);
}

function filteredWords() {
  const words = activeWords().filter((word) => state.categoryId === "all" || word.categoryId === state.categoryId);
  return words.sort((a, b) => categorySort(a.categoryId) - categorySort(b.categoryId) || a.difficulty - b.difficulty || a.word.localeCompare(b.word));
}

function dueWords() {
  return filteredWords().filter((word) => !(state.reviews[word.id]?.learned));
}

function learnedWordCount() {
  return activeWords().filter((word) => state.reviews[word.id]?.learned).length;
}

function categoryProgress() {
  return data.categories.map((category) => {
    const words = activeWords().filter((word) => word.categoryId === category.id);
    const learned = words.filter((word) => state.reviews[word.id]?.learned).length;
    return { name: category.name, total: words.length, learned };
  });
}

function quizQuestions() {
  const words = filteredWords();
  const seeded = shuffle(words, state.quizSeed).slice(0, Math.min(state.dailyGoal, words.length));
  return seeded.map((answer, index) => ({
    answer,
    options: shuffle(
      [answer, ...shuffle(words.filter((word) => word.id !== answer.id), answer.id.length + index).slice(0, 3)],
      index + state.quizSeed,
    ),
  }));
}

function moveCard(delta) {
  const words = filteredWords();
  if (!words.length) return;
  updateState({ cardIndex: (boundedIndex(state.cardIndex, words.length) + delta + words.length) % words.length, reveal: false });
}

function rateCurrentCard(rating) {
  const words = filteredWords();
  const card = words[boundedIndex(state.cardIndex, words.length)];
  if (!card) return;
  writeReview(card.id, rating === "again" || rating === "hard" ? false : true);
  moveCard(1);
}

function answerQuiz(selectedId) {
  const question = quizQuestions()[state.quizIndex % quizQuestions().length];
  if (!question) return;
  const correct = selectedId === question.answer.id;
  writeReview(question.answer.id, correct);
  state = {
    ...state,
    quizAnswered: selectedId,
    quizCorrect: state.quizCorrect + (correct ? 1 : 0),
    quizTotal: state.quizTotal + 1,
  };
  saveState();
  render();
}

function nextQuiz() {
  const questions = quizQuestions();
  const nextIndex = state.quizIndex >= questions.length - 1 ? 0 : state.quizIndex + 1;
  updateState({
    quizIndex: nextIndex,
    quizAnswered: null,
    quizSeed: nextIndex === 0 ? Date.now() : state.quizSeed,
  });
}

function writeReview(id, correct) {
  const current = state.reviews[id] || { count: 0, correct: 0, wrong: 0, learned: false };
  state.reviews = {
    ...state.reviews,
    [id]: {
      count: current.count + 1,
      correct: current.correct + (correct ? 1 : 0),
      wrong: current.wrong + (correct ? 0 : 1),
      learned: correct || current.learned,
      lastReviewedAt: Date.now(),
    },
  };
  saveState();
}

function speakLetter(id) {
  const letter = data.alphabet.find((item) => item.id === id);
  if (!letter) return;
  speak(russianLetterSpeech[id] || letter.uppercase, "ru-RU");
}

function speak(text, lang) {
  if (!window.speechSynthesis) return;
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = lang;
  window.speechSynthesis.cancel();
  window.speechSynthesis.speak(utterance);
}

function toggleWeakLetter(id) {
  const weak = new Set(state.weakLetters);
  if (weak.has(id)) weak.delete(id);
  else weak.add(id);
  updateState({ weakLetters: [...weak] });
}

function sendChat(rawMessage) {
  const message = rawMessage.trim();
  if (!message) return;
  const matches = searchWords(message).slice(0, 4);
  const reply = matches.length
    ? matches.map((word) => `${word.word} - ${word.meanings.join(", ")}${word.example?.ru ? `\n${word.example.ru}` : ""}`).join("\n\n")
    : "가까운 표현을 찾지 못했습니다. 다른 단어나 뜻으로 검색해 보세요.";
  updateState({ chat: [...state.chat, { role: "user", text: message }, { role: "bot", text: reply }] });
}

function searchWords(query) {
  const normalized = normalizeText(query);
  return activeWords()
    .map((word) => {
      const haystack = normalizeText([
        word.word,
        word.transliteration,
        word.pronunciationKo,
        word.meanings.join(" "),
        word.example?.ru,
        word.example?.ko,
        word.tags?.join(" "),
      ].filter(Boolean).join(" "));
      let score = 0;
      if (haystack.includes(normalized)) score += 10;
      normalized.split(/\s+/).forEach((token) => {
        if (token && haystack.includes(token)) score += 2;
      });
      return { word, score };
    })
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score)
    .map((item) => item.word);
}

function categoryName(id) {
  return data.categories.find((category) => category.id === id)?.name || id;
}

function categorySort(id) {
  return data.categories.find((category) => category.id === id)?.sortOrder || 999;
}

function boundedIndex(index, length) {
  if (!length) return 0;
  return Math.max(0, Math.min(index, length - 1));
}

function shuffle(items, seed) {
  const result = [...items];
  let value = typeof seed === "number" ? seed : hashCode(String(seed));
  for (let index = result.length - 1; index > 0; index -= 1) {
    value = (value * 1664525 + 1013904223) >>> 0;
    const swapIndex = value % (index + 1);
    [result[index], result[swapIndex]] = [result[swapIndex], result[index]];
  }
  return result;
}

function hashCode(value) {
  return [...value].reduce((hash, char) => ((hash << 5) - hash + char.charCodeAt(0)) | 0, 0);
}

function normalizeText(value) {
  return String(value).trim().toLowerCase().normalize("NFKD");
}

function formatMessage(text) {
  return escapeHtml(text).replace(/\n/g, "<br />");
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
