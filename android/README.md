# android — 보호자 앱

Android Studio로 이 폴더에 프로젝트를 만든다.

1. Android Studio → New Project → **Empty Activity** (Jetpack Compose)
2. Language: Kotlin / Save location: 이 `android` 폴더
3. 폴더가 비어 있지 않다는 경고가 나오면 이 README.md를 지우고 다시 만든다

## 코드 구조 (제안)

```
app/src/main/
├─ java/.../calmmap/
│  ├─ ui/       onboarding · map · place · visit · measure · settings (화면별)
│  ├─ audio/    AudioRecord 마이크 입력, YAMNet 추론
│  ├─ data/     Supabase 통신
│  └─ domain/   맞춤 점수 계산, 권고 문구
└─ assets/      yamnet.tflite, 라벨 목록
```

## 주의

- 마이크 입력은 16kHz 모노. 파일로 저장하는 코드를 만들지 않는다
- 지도 SDK 키, Supabase anon key는 `local.properties`에 두고 git에 올리지 않는다
- Supabase service key는 앱에 넣지 않는다
