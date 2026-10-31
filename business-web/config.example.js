// 이 파일을 복사해서 config.js 로 이름을 바꾸고 실제 값을 넣으세요.
//   Git Bash : cp business-web/config.example.js business-web/config.js
//
// config.js 는 .gitignore 가 막고 있어서 git 에 올라가지 않습니다.
// Vercel 에 올릴 때는 Vercel 환경 변수(SUPABASE_URL · SUPABASE_ANON_KEY · KAKAO_JS_KEY)로
// vercel-build.js 가 config.js 를 만듭니다.
//
// 세 값 모두 브라우저에 그대로 보이는 값입니다.
//   Supabase anon key   RLS 와 DB 함수 권한으로 막는다. service key 는 절대 넣지 않습니다
//   카카오 JavaScript 키 카카오 개발자 콘솔 → 플랫폼 키 → JavaScript 키 → JavaScript SDK 도메인에 등록한 주소에서만 동작한다
window.CALM_CONFIG = {
  // android/local.properties 의 SUPABASE_URL · SUPABASE_ANON_KEY 와 같은 값
  supabaseUrl: "",
  supabaseAnonKey: "",
  // 카카오 개발자 콘솔 → 앱 설정 → 앱 → 플랫폼 키 → 사업장 웹용 JavaScript 키. 비워 두면 지도에 있는 장소만 검색된다
  kakaoJsKey: "",
};
