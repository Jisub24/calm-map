// Vercel 이 올리기 전에 실행한다 (vercel.json 의 buildCommand).
// 화면 파일을 dist/ 로 복사하고, Vercel 에 넣은 값(SUPABASE_URL · SUPABASE_ANON_KEY · KAKAO_JS_KEY)으로 dist/config.js 를 만든다.
// config.js 는 키가 들어가서 저장소에 없다. 내 PC 에서는 config.example.js 를 복사해서 쓴다.
const fs = require("fs");
const path = require("path");

const FILES = ["index.html", "style.css", "app.js"];
const out = path.join(__dirname, "dist");

const supabaseUrl = (process.env.SUPABASE_URL || "").trim();
const supabaseAnonKey = (process.env.SUPABASE_ANON_KEY || "").trim();
const kakaoJsKey = (process.env.KAKAO_JS_KEY || "").trim();

function stop(message) {
  console.error(message);
  process.exit(1);
}

// service key 는 키 안의 role 이 service_role 이다. 웹에 들어가면 누구나 브라우저에서 보게 되니 올리기 전에 막는다
function isServiceKey(key) {
  if (key.startsWith("sb_secret_")) return true;
  try {
    return JSON.parse(Buffer.from(key.split(".")[1], "base64url").toString()).role === "service_role";
  } catch {
    return false;
  }
}

// Supabase 값이 없으면 DB 를 못 읽는 빈 화면이 올라간다. 그럴 바엔 배포를 멈춘다
if (!supabaseUrl || !supabaseAnonKey) {
  stop("SUPABASE_URL · SUPABASE_ANON_KEY 가 비어 있다. Vercel 프로젝트 → Environment Variables 를 확인한다");
}
if (isServiceKey(supabaseAnonKey)) {
  stop("SUPABASE_ANON_KEY 에 service key 가 들어 있다. anon key 로 바꾼다");
}
// 카카오 키는 없어도 올린다. 무던한 지도에 이미 있는 가게만 검색된다
if (!kakaoJsKey) console.warn("KAKAO_JS_KEY 가 비어 있다. 카카오 검색 · 지도 없이 올린다");

fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(out);
for (const f of FILES) fs.copyFileSync(path.join(__dirname, f), path.join(out, f));
fs.writeFileSync(
  path.join(out, "config.js"),
  `window.CALM_CONFIG = ${JSON.stringify({ supabaseUrl, supabaseAnonKey, kakaoJsKey }, null, 2)};\n`,
);
console.log(`dist/ 에 ${FILES.length + 1}개 파일을 만들었다`);
