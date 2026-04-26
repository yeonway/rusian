# Rusian Learner (Offline-First Android)

오프라인 우선 러시아어 학습 앱 MVP입니다.
목표는 단순 암기가 아니라 러시아어의 톤과 상황 감각을 익히는 것입니다.

## Stack

- Kotlin
- Jetpack Compose
- Room
- DataStore
- WorkManager
- Hilt
- Navigation Compose

## Data files

- 앱 기본 시드: `app/src/main/assets/content-pack.json`
- 서버 manifest 예시: `app/src/main/assets/manifest.example.json`
- 알파벳 데이터: `letterType(VOWEL/CONSONANT/SIGN)`, `soundFeel`, `confusionNote`, `usageFrequency`, `tmiNote`
- 표현 데이터: `tone(FORMAL/NEUTRAL/CASUAL/PLAYFUL/SLANG)`, `situationHint`, `usageNote`, `pairKey`, `contextTag`

## Sync model

- 기본 동작은 로컬 DB만으로 완전 동작
- 서버가 열려 있을 때 `manifest.json` 확인 후 새 버전일 때만 갱신
- 체크섬 검증 실패 시 롤백(트랜잭션)
- `Clean Install`: 콘텐츠 재설치 + `remoteStableId` 기반 진행도 재연결
- `Factory Reset`: 자동 백업 후 진행도/이력 초기화
- 알파벳과 단어/표현은 같은 SRS 파이프라인에서 동작

## Safety checklist

- `Clean Install` 실행 전 백업 자동 생성
- `Factory Reset` 2단계 확인 다이얼로그
- `Factory Reset` 실행 전 백업 실패 시 중단
- 서버 미연결 시 로컬 시드로 clean install fallback
