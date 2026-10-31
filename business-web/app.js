"use strict";
// 무던한 지도 · 사업장 웹
// 로그인 없이 Supabase anon key 로 붙는다 (docs/03 8번). 쓰기는 business_calm_hours 정책(012)과
// add_business_place · set_place_hours 함수(029), remove_business_place 함수(030)로만 한다.

const cfg = window.CALM_CONFIG || {};
const WEEKDAYS = ["월", "화", "수", "목", "금", "토", "일"];
// business_calm_hours.actions 값 (012). short 는 앱 장소 상세와 같은 말
const ACTIONS = [
  { key: "music_off", label: "매장 음악 끄기", short: "음악 끔" },
  { key: "light_down", label: "조명 낮추기", short: "조명 낮춤" },
  { key: "announcement_min", label: "안내방송 줄이기", short: "방송 최소" },
];
const PLACE_COLUMNS = "id,name,category,address,lat,lng,kakao_id,source";
const BUSINESS_SOURCE = "사업장등록"; // places.source (029). 이 웹으로 들어온 가게만 지도에서 지울 수 있다
const STORE_KEY = "calm-business-place"; // 마지막으로 연 가게. 다시 열면 바로 그 가게 화면
const MY_KEY = "calm-business-my-places"; // 이 브라우저에서 연 가게들 (최근 순). 가게가 여러 개인 사장님용

const $ = (id) => document.getElementById(id);
const db = cfg.supabaseUrl && cfg.supabaseAnonKey ? window.supabase.createClient(cfg.supabaseUrl, cfg.supabaseAnonKey) : null;

const state = {
  places: [], // 무던한 지도에 있는 장소 (검색용)
  place: null, // 고른 가게
  pending: null, // '이 가게가 맞나요?' 에서 보는 가게
  hours: null, // place_hours 7줄. 아직 없으면 null
  hoursDraft: [], // 영업시간 입력 중인 값
  calm: [], // business_calm_hours 줄
  editing: null, // 수정 중인 무던한 시간 묶음
  kakao: false, // 카카오 지도 SDK 를 불러왔는지
};

// ── 작은 도구 ─────────────────────────────────────────

/** 요소 만들기. 사용자 · 카카오에서 온 글자는 textContent 로만 넣는다 */
function el(tag, props = {}, children = []) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (v == null) continue;
    if (k === "class") node.className = v;
    else if (k === "text") node.textContent = v;
    else if (k.startsWith("on")) node.addEventListener(k.slice(2), v);
    else node.setAttribute(k, v);
  }
  for (const c of [].concat(children)) if (c != null) node.append(c);
  return node;
}

const toMin = (t) => {
  const [h, m] = t.slice(0, 5).split(":").map(Number);
  return h * 60 + m;
};
const fromMin = (m) => `${String(Math.floor(m / 60)).padStart(2, "0")}:${String(m % 60).padStart(2, "0")}`;
/** "10:00:00" → "10", "10:30:00" → "10:30". 앱 장소 상세와 같은 모양 */
const clockShort = (t) => (t.slice(3, 5) === "00" ? String(Number(t.slice(0, 2))) : t.slice(0, 5).replace(/^0/, ""));
const hhmm = (t) => t.slice(0, 5);

/** 30분 단위 시각 목록 [from, to] */
function halfHours(from, to) {
  const list = [];
  for (let m = from; m <= to; m += 30) list.push(fromMin(m));
  return list;
}
const OPEN_TIMES = halfHours(0, 23 * 60 + 30);
const CLOSE_TIMES = halfHours(30, 24 * 60);

/** 요일 묶음 글자. [1,2,3,4,5] → "월–금", [2,4] → "화 · 목" */
function daysLabel(days) {
  const sorted = [...new Set(days)].sort((a, b) => a - b);
  const run = sorted.length >= 3 && sorted.every((d, i) => i === 0 || d === sorted[i - 1] + 1);
  if (run) return `${WEEKDAYS[sorted[0] - 1]}–${WEEKDAYS[sorted.at(-1) - 1]}`;
  return sorted.map((d) => WEEKDAYS[d - 1]).join(" · ");
}

/** 마커용 짧은 이름. 앱 MarkerIcon.shortName 과 같다 */
function shortName(name) {
  const head = name.trim().split(" ")[0];
  return head.length <= 7 ? head : head.slice(0, 6) + "…";
}

/** 서울 기준 지금 요일(1=월) · 분 */
function seoulNow() {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat("en-US", { timeZone: "Asia/Seoul", weekday: "short", hour: "2-digit", minute: "2-digit", hourCycle: "h23" })
      .formatToParts(new Date())
      .map((p) => [p.type, p.value]),
  );
  const weekday = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].indexOf(parts.weekday) + 1;
  return { weekday, minute: Number(parts.hour) * 60 + Number(parts.minute) };
}

function toast(message) {
  const t = $("toast");
  t.textContent = message;
  t.classList.add("show");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => t.classList.remove("show"), 2600);
}

/** 확인 창. 확인을 누르면 true */
function confirmDialog(title, body, okText) {
  const d = $("dialog");
  $("dialog-title").textContent = title;
  $("dialog-body").textContent = body;
  $("dialog-ok").textContent = okText;
  d.showModal();
  return new Promise((resolve) => {
    const done = (v) => {
      d.close();
      $("dialog-ok").onclick = $("dialog-cancel").onclick = null;
      resolve(v);
    };
    $("dialog-ok").onclick = () => done(true);
    $("dialog-cancel").onclick = () => done(false);
    d.oncancel = () => done(false);
  });
}

function show(view) {
  for (const v of ["find", "confirm", "manage", "loading"]) $(`view-${v}`).hidden = v !== view;
  $("current-place").hidden = view !== "manage";
  window.scrollTo({ top: 0 });
}

// ── 1. 내 가게 찾기 ───────────────────────────────────

function loadKakao() {
  if (!cfg.kakaoJsKey) {
    $("kakao-notice").hidden = false;
    return;
  }
  const s = document.createElement("script");
  s.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(cfg.kakaoJsKey)}&libraries=services&autoload=false`;
  s.onload = () =>
    window.kakao.maps.load(() => {
      state.kakao = true;
      if ($("search").value.trim()) runSearch();
    });
  // 등록 안 된 주소에서 열었거나 키가 틀리면 여기로 온다. 지도에 있는 장소 검색은 계속 된다
  s.onerror = () => ($("kakao-notice").hidden = false);
  document.head.append(s);
}

function kakaoSearch(q) {
  return new Promise((resolve) => {
    new window.kakao.maps.services.Places().keywordSearch(
      q,
      (data, status) => resolve(status === window.kakao.maps.services.Status.OK ? data : []),
      { size: 15 },
    );
  });
}

let searchTimer;
let searchSeq = 0;

async function runSearch() {
  const q = $("search").value.trim();
  const seq = ++searchSeq;
  const box = $("results");
  if (!q) {
    box.replaceChildren();
    return;
  }
  const known = state.places.filter((p) => p.name.includes(q) || (p.address || "").includes(q));
  let fromKakao = state.kakao ? await kakaoSearch(q) : [];
  // 늦게 온 이전 검색 결과가 새 결과를 덮지 않게
  if (seq !== searchSeq) return;
  const knownKakao = new Set(state.places.map((p) => p.kakao_id).filter(Boolean));
  fromKakao = fromKakao
    .filter((k) => !knownKakao.has(k.id))
    .map((k) => ({
      kakaoId: k.id,
      name: k.place_name,
      category: k.category_name ? k.category_name.split(" > ").pop() : "",
      categoryGroup: k.category_group_code || "",
      categoryName: k.category_name || "",
      address: k.road_address_name || k.address_name,
      lat: Number(k.y),
      lng: Number(k.x),
    }));

  const groups = [];
  if (known.length) groups.push(resultGroup("무던한 지도에 있는 가게", known, true));
  if (fromKakao.length) groups.push(resultGroup("카카오 장소 검색", fromKakao, false));
  if (!groups.length) groups.push(el("p", { class: "empty", text: `'${q}' 로 찾은 가게가 없어요. 지점 이름까지 넣어 보세요.` }));
  box.replaceChildren(...groups);
}

function resultGroup(title, list, known) {
  return el("div", { class: "result-group" }, [
    el("p", { class: "result-head", text: title }),
    ...list.map((p) =>
      el("button", { class: "result", type: "button", onclick: () => openConfirm(p) }, [
        el("span", { class: `result-icon${known ? " known" : ""}`, "aria-hidden": "true" }, storeIcon()),
        el("span", { class: "result-body" }, [
          el("span", { class: "result-name", text: p.name }),
          el("span", { class: "result-sub", text: [p.category, p.address].filter(Boolean).join(" · ") }),
        ]),
        known ? el("span", { class: "result-badge", text: "지도에 있음" }) : null,
      ]),
    ),
  ]);
}

function storeIcon() {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("width", "22");
  svg.setAttribute("height", "22");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("fill", "none");
  svg.setAttribute("stroke", "currentColor");
  svg.setAttribute("stroke-width", "1.8");
  svg.setAttribute("stroke-linecap", "round");
  svg.setAttribute("stroke-linejoin", "round");
  svg.innerHTML = '<path d="M4 9h16l-1.5-5h-13z"/><path d="M5 9v11h14V9"/><path d="M10 20v-5h4v5"/>';
  return svg;
}

// ── 1-2. 이 가게가 맞나요 ─────────────────────────────
// 같은 이름 지점이 많아서(스타벅스 홍대 근처만 여럿) 지도로 한 번 더 확인한다. 앱 검색과 같은 흐름

function openConfirm(p) {
  state.pending = p;
  $("confirm-name").textContent = p.name;
  $("confirm-sub").textContent = [p.category, p.address].filter(Boolean).join(" · ");
  $("confirm-note").textContent = p.id
    ? "이미 무던한 지도에 있는 가게예요. 바로 시간을 등록할 수 있어요."
    : "확인하면 이 가게가 무던한 지도에 새로 생겨요.";
  show("confirm");
  const box = $("confirm-map");
  if (!state.kakao) {
    box.textContent = "카카오 키를 넣으면 여기에 지도가 보여요";
    return;
  }
  box.textContent = "";
  const pos = new window.kakao.maps.LatLng(p.lat, p.lng);
  const map = new window.kakao.maps.Map(box, { center: pos, level: 3 });
  new window.kakao.maps.Marker({ position: pos, map });
}

$("confirm-no").addEventListener("click", () => {
  show("find");
  $("search").focus();
});

$("confirm-yes").addEventListener("click", async () => {
  const p = state.pending;
  const btn = $("confirm-yes");
  btn.disabled = true;
  try {
    let place = p.id ? p : null;
    if (!place) {
      const { data: id, error } = await db.rpc("add_business_place", {
        p_kakao_id: p.kakaoId,
        p_name: p.name,
        p_category_group: p.categoryGroup,
        p_category_name: p.categoryName,
        p_address: p.address,
        p_lat: p.lat,
        p_lng: p.lng,
      });
      if (error) throw error;
      const res = await db.from("places").select(PLACE_COLUMNS).eq("id", id).single();
      if (res.error) throw res.error;
      place = res.data;
      if (!state.places.some((x) => x.id === place.id)) state.places.push(place);
      toast("무던한 지도에 가게를 추가했어요");
    }
    await openPlace(place);
  } catch (e) {
    console.warn("가게 추가 실패", e);
    toast("가게를 추가하지 못했어요. 잠시 뒤 다시 시도해 주세요");
  } finally {
    btn.disabled = false;
  }
});

// ── 2 · 3. 가게 화면 ─────────────────────────────────

async function openPlace(place) {
  state.place = place;
  state.editing = null;
  try {
    localStorage.setItem(STORE_KEY, String(place.id));
  } catch (_) {
    // 저장이 막힌 브라우저면 다음에 다시 고르면 된다
  }
  rememberPlace(place.id);
  $("current-name").textContent = place.name;
  $("remove-card").hidden = place.source !== BUSINESS_SOURCE;
  show("manage");
  resetCalmForm();
  await Promise.all([loadHours(), loadCalm()]);
}

/** 첫 화면(내 가게 목록 + 검색). 마지막 가게 기억을 지워서 새로고침해도 첫 화면에 남는다 */
function goHome() {
  try {
    localStorage.removeItem(STORE_KEY);
  } catch (_) {
    // 위와 같다
  }
  state.place = null;
  $("search").value = "";
  $("results").replaceChildren();
  renderMyPlaces();
  show("find");
  $("search").focus();
}

$("change-place").addEventListener("click", goHome);
// 로고는 href="./" 라 새로 열리면 마지막 가게로 다시 들어간다. 첫 화면으로 보낸다
document.querySelector(".brand").addEventListener("click", (e) => {
  e.preventDefault();
  goHome();
});

// ── 내 가게 목록 ─────────────────────────────────────
// 로그인이 없어서 '내 가게' 는 이 브라우저가 기억하는 목록이다. 다른 기기에서는 다시 검색해서 연다

function myPlaceIds() {
  try {
    const ids = JSON.parse(localStorage.getItem(MY_KEY) || "[]");
    return Array.isArray(ids) ? ids.filter(Number.isInteger) : [];
  } catch (_) {
    return [];
  }
}

function saveMyPlaceIds(ids) {
  try {
    localStorage.setItem(MY_KEY, JSON.stringify(ids));
  } catch (_) {
    // 저장이 막힌 브라우저면 목록 없이 검색으로 연다
  }
}

function rememberPlace(id) {
  saveMyPlaceIds([id, ...myPlaceIds().filter((x) => x !== id)]);
}

function renderMyPlaces() {
  // 지도에서 지워진 가게는 목록에서도 뺀다
  const mine = myPlaceIds()
    .map((id) => state.places.find((p) => p.id === id))
    .filter(Boolean);
  $("my-places").hidden = !mine.length;
  $("search-label").textContent = mine.length ? "다른 가게 찾기" : "내 가게 찾기";
  $("my-places-list").replaceChildren(
    ...mine.map((p) =>
      el("div", { class: "my-row" }, [
        el("button", { class: "result", type: "button", onclick: () => openPlace(p) }, [
          el("span", { class: "result-icon known", "aria-hidden": "true" }, storeIcon()),
          el("span", { class: "result-body" }, [
            el("span", { class: "result-name", text: p.name }),
            el("span", { class: "result-sub", text: [p.category, p.address].filter(Boolean).join(" · ") }),
          ]),
        ]),
        el("button", {
          class: "btn btn-ghost btn-sm",
          type: "button",
          text: "빼기",
          "aria-label": `${p.name} 목록에서 빼기`,
          onclick: () => {
            saveMyPlaceIds(myPlaceIds().filter((x) => x !== p.id));
            renderMyPlaces();
          },
        }),
      ]),
    ),
  );
}

// ── 가게 삭제 ────────────────────────────────────────
// 이 웹으로 등록한 가게만 버튼이 보인다. 지우면 앱 지도에서도 사라진다 (030).
// 측정 · 방문 기록이 생긴 가게는 함수가 남겨 두고 false 를 돌려준다

$("remove-place").addEventListener("click", async () => {
  const p = state.place;
  const ok = await confirmDialog(`${p.name} 삭제할까요?`, "사용자 앱 지도에서 사라지고, 등록한 영업시간 · 무던한 시간도 같이 지워져요.", "삭제");
  if (!ok) return;
  const { data: removed, error } = await db.rpc("remove_business_place", { p_place_id: p.id });
  if (error) {
    console.warn("가게 삭제 실패", error);
    toast("삭제하지 못했어요. 잠시 뒤 다시 시도해 주세요");
    return;
  }
  if (!removed) {
    toast("측정 · 방문 기록이 있는 가게라 지도에서 지울 수 없어요");
    return;
  }
  state.places = state.places.filter((x) => x.id !== p.id);
  saveMyPlaceIds(myPlaceIds().filter((x) => x !== p.id));
  goHome();
  toast("가게를 삭제했어요");
});

// ── 영업시간 ─────────────────────────────────────────

async function loadHours() {
  // 읽는 동안 기본값 입력칸을 보여주면, 영업시간이 있는 가게도 없는 것처럼 보여 덮어쓰게 된다
  setHoursMode("loading");
  const { data, error } = await db
    .from("place_hours")
    .select("weekday,is_closed,open_time,close_time,closes_next_day")
    .eq("place_id", state.place.id)
    .order("weekday");
  if (error) {
    console.warn("영업시간 읽기 실패", error);
    setHoursMode("failed");
    return;
  }
  state.hours = data && data.length === 7 ? data : null;
  state.hoursDraft = WEEKDAYS.map((_, i) => {
    const row = state.hours?.find((r) => r.weekday === i + 1);
    if (!row) return { weekday: i + 1, closed: false, open: "10:00", close: "22:00" };
    return {
      weekday: i + 1,
      closed: row.is_closed,
      open: row.open_time ? hhmm(row.open_time) : "10:00",
      close: row.close_time ? hhmm(row.close_time) : "22:00",
    };
  });
  // 저장된 영업시간이 있으면 요약, 없으면 바로 입력
  setHoursMode(state.hours ? "summary" : "edit");
  renderPreview();
}

/** edit 입력 · summary 요약 · loading 읽는 중 · failed 못 읽음 */
function setHoursMode(mode) {
  const editing = mode === "edit";
  $("hours-editor").hidden = !editing;
  $("hours-summary").hidden = editing;
  $("hours-edit").hidden = mode !== "summary";
  $("hours-cancel").hidden = !(editing && state.hours);
  // 처음 가게는 기본값(10:00–22:00)이 채워져 있어서 이미 저장된 것처럼 보인다
  $("hours-unsaved").hidden = !(editing && !state.hours);
  $("hours-error").hidden = true;
  if (editing) renderHoursRows();
  else if (mode === "summary") renderHoursSummary();
  else if (mode === "loading") $("hours-summary").replaceChildren(el("p", { class: "loading", text: "불러오는 중…" }));
  else
    $("hours-summary").replaceChildren(
      el("p", { class: "error", text: "영업시간을 불러오지 못했어요" }),
      el("button", { class: "btn btn-secondary btn-sm", type: "button", text: "다시 시도", style: "margin-top:10px", onclick: loadHours }),
    );
}

function optionList(select, times, value) {
  select.replaceChildren(...times.map((t) => el("option", { value: t, text: t === "24:00" ? "24:00 (자정)" : t })));
  select.value = value;
}

function renderHoursRows() {
  const rows = state.hoursDraft.map((d) => {
    const open = el("select", { class: "select", "aria-label": `${WEEKDAYS[d.weekday - 1]}요일 여는 시각` });
    const close = el("select", { class: "select", "aria-label": `${WEEKDAYS[d.weekday - 1]}요일 닫는 시각` });
    optionList(open, OPEN_TIMES, d.open);
    optionList(close, CLOSE_TIMES, d.close);
    const nextDay = el("span", { class: "next-day" });
    const update = () => {
      d.open = open.value;
      d.close = close.value;
      open.disabled = close.disabled = d.closed;
      // 닫는 시각이 여는 시각보다 앞이면 다음날 새벽에 닫는 가게
      nextDay.textContent = !d.closed && toMin(d.close) < toMin(d.open) ? "다음날" : "";
      seg.forEach((b, i) => b.setAttribute("aria-pressed", String(i === 1 ? d.closed : !d.closed)));
      renderPreview();
    };
    const seg = ["영업", "쉬는 날"].map((label, i) =>
      el("button", { type: "button", text: label, onclick: () => ((d.closed = i === 1), update()) }),
    );
    open.addEventListener("change", update);
    close.addEventListener("change", update);
    const tr = el("tr", {}, [
      el("td", { class: `day-cell${d.weekday >= 6 ? " weekend" : ""}`, text: WEEKDAYS[d.weekday - 1] }),
      el("td", {}, el("div", { class: "seg", role: "group", "aria-label": `${WEEKDAYS[d.weekday - 1]}요일 영업` }, seg)),
      el("td", {}, el("div", { class: "time-pair" }, [open, el("span", { text: "–", "aria-hidden": "true" }), close, nextDay])),
    ]);
    update();
    return tr;
  });
  $("hours-rows").replaceChildren(...rows);
}

function hoursText(d) {
  if (d.closed) return "쉬는 날";
  if (d.open === "00:00" && d.close === "24:00") return "24시간";
  return `${d.open} – ${toMin(d.close) < toMin(d.open) ? "다음날 " : ""}${d.close}`;
}

function renderHoursSummary() {
  // 같은 시간인 요일끼리 묶는다. "월–금 10:00 – 22:00"
  const byText = new Map();
  for (const d of state.hoursDraft) {
    const t = hoursText(d);
    byText.set(t, [...(byText.get(t) || []), d.weekday]);
  }
  $("hours-summary").replaceChildren(
    ...[...byText].map(([text, days]) =>
      el("div", { class: "summary-row" }, [el("span", { class: "summary-days", text: daysLabel(days) }), el("span", { class: "summary-time", text })]),
    ),
  );
}

$("hours-edit").addEventListener("click", () => setHoursMode("edit"));
$("hours-cancel").addEventListener("click", () => loadHours());
$("hours-copy").addEventListener("click", () => {
  const mon = state.hoursDraft[0];
  state.hoursDraft.forEach((d) => Object.assign(d, { closed: mon.closed, open: mon.open, close: mon.close }));
  renderHoursRows();
});

$("hours-save").addEventListener("click", async () => {
  const err = $("hours-error");
  const same = state.hoursDraft.find((d) => !d.closed && d.open === d.close);
  if (same) {
    err.textContent = `${WEEKDAYS[same.weekday - 1]}요일 여는 시각과 닫는 시각이 같아요`;
    err.hidden = false;
    return;
  }
  err.hidden = true;
  const payload = state.hoursDraft.map((d) => ({
    weekday: d.weekday,
    is_closed: d.closed,
    open_time: d.closed ? null : d.open,
    close_time: d.closed ? null : d.close,
    closes_next_day: !d.closed && toMin(d.close) < toMin(d.open),
  }));
  const btn = $("hours-save");
  btn.disabled = true;
  const { error } = await db.rpc("set_place_hours", { p_place_id: state.place.id, p_hours: payload });
  btn.disabled = false;
  if (error) {
    console.warn("영업시간 저장 실패", error);
    err.textContent = "저장하지 못했어요. 잠시 뒤 다시 시도해 주세요";
    err.hidden = false;
    return;
  }
  toast("영업시간을 저장했어요. 사용자 앱에 바로 반영돼요");
  await loadHours();
});

/** 그 요일 영업시간 안인지. 영업시간을 아직 안 넣었으면 확인하지 않는다 */
function insideHours(weekday, start, end) {
  if (!state.hours) return null;
  const d = state.hoursDraft[weekday - 1];
  if (d.closed) return `${WEEKDAYS[weekday - 1]}요일은 쉬는 날로 저장돼 있어요`;
  const o = toMin(d.open);
  const c = toMin(d.close);
  const s = toMin(start);
  const e = toMin(end);
  const ok = c > o ? s >= o && e <= c : s >= o || e <= c;
  return ok ? null : `${WEEKDAYS[weekday - 1]}요일 영업시간(${hoursText(d)}) 안으로 정해 주세요`;
}

// ── 무던한 시간 ──────────────────────────────────────

const calmForm = { days: new Set(), actions: new Set() };

function buildCalmControls() {
  $("calm-days").replaceChildren(
    ...WEEKDAYS.map((w, i) =>
      el("button", {
        class: "chip day",
        type: "button",
        text: w,
        "aria-pressed": "false",
        "data-day": String(i + 1),
        onclick: (ev) => toggle(calmForm.days, i + 1, ev.currentTarget),
      }),
    ),
  );
  $("calm-actions").replaceChildren(
    ...ACTIONS.map((a) =>
      el("button", {
        class: "chip",
        type: "button",
        text: a.label,
        "aria-pressed": "false",
        "data-action": a.key,
        onclick: (ev) => toggle(calmForm.actions, a.key, ev.currentTarget),
      }),
    ),
  );
  optionList($("calm-start"), OPEN_TIMES, "10:00");
  optionList($("calm-end"), CLOSE_TIMES, "12:00");
  for (const id of ["calm-start", "calm-end", "calm-custom"]) $(id).addEventListener("input", renderPreview);
}

function toggle(set, value, button) {
  if (set.has(value)) set.delete(value);
  else set.add(value);
  button.setAttribute("aria-pressed", String(set.has(value)));
  renderPreview();
}

function syncChips() {
  document.querySelectorAll("#calm-days .chip").forEach((b) => b.setAttribute("aria-pressed", String(calmForm.days.has(Number(b.dataset.day)))));
  document.querySelectorAll("#calm-actions .chip").forEach((b) => b.setAttribute("aria-pressed", String(calmForm.actions.has(b.dataset.action))));
}

function resetCalmForm() {
  state.editing = null;
  calmForm.days.clear();
  calmForm.actions.clear();
  $("calm-start").value = "10:00";
  $("calm-end").value = "12:00";
  $("calm-custom").value = "";
  $("calm-title").textContent = "무던한 시간 등록하기";
  $("calm-save").textContent = "등록하기";
  $("calm-cancel").hidden = true;
  $("calm-error").hidden = true;
  syncChips();
}

async function loadCalm() {
  const { data, error } = await db
    .from("business_calm_hours")
    .select("id,weekday,start_time,end_time,actions,custom_action")
    .eq("place_id", state.place.id)
    .order("weekday")
    .order("start_time");
  if (error) {
    // '등록한 시간이 없어요' 로 보이면 같은 시간을 또 등록하게 된다
    console.warn("무던한 시간 읽기 실패", error);
    $("reg-list").replaceChildren(
      el("p", { class: "error", text: "등록된 시간을 불러오지 못했어요" }),
      el("button", { class: "btn btn-secondary btn-sm", type: "button", text: "다시 시도", style: "margin-top:10px", onclick: loadCalm }),
    );
    return;
  }
  state.calm = data || [];
  renderRegistered();
  renderPreview();
}

/** 같은 시간 · 같은 일을 하는 요일끼리 한 묶음. 앱 장소 상세도 이렇게 묶어 보여준다 */
function calmGroups(rows) {
  const groups = new Map();
  for (const r of rows) {
    const actions = [...(r.actions || [])].sort();
    const key = [r.start_time, r.end_time, actions.join(","), r.custom_action || ""].join("|");
    const g = groups.get(key) || { key, days: [], ids: [], start: r.start_time, end: r.end_time, actions, custom: r.custom_action };
    g.days.push(r.weekday);
    g.ids.push(r.id);
    groups.set(key, g);
  }
  return [...groups.values()].sort((a, b) => Math.min(...a.days) - Math.min(...b.days) || toMin(a.start) - toMin(b.start));
}

/** 하는 일 글자. 고른 순서와 상관없이 '음악 · 조명 · 방송' 순서로 */
const actionWords = (g) => [...ACTIONS.filter((a) => g.actions.includes(a.key)).map((a) => a.short), ...(g.custom ? [g.custom] : [])];

function renderRegistered() {
  const groups = calmGroups(state.calm);
  const list = $("reg-list");
  if (!groups.length) {
    list.replaceChildren(el("p", { class: "empty", text: "아직 등록한 시간이 없어요. 왼쪽에서 첫 시간을 등록해 보세요." }));
    return;
  }
  list.replaceChildren(
    ...groups.map((g) =>
      el("div", { class: `reg-item${state.editing === g.key ? " editing" : ""}` }, [
        el("p", { class: "reg-when", text: `${daysLabel(g.days)} ${hhmm(g.start)}–${hhmm(g.end)}` }),
        el("p", { class: "reg-what", text: actionWords(g).join(" · ") }),
        el("div", { class: "reg-actions" }, [
          el("button", { class: "btn btn-secondary btn-sm", type: "button", text: "수정", onclick: () => editGroup(g) }),
          el("button", { class: "btn btn-ghost btn-sm", type: "button", text: "삭제", onclick: () => deleteGroup(g) }),
        ]),
      ]),
    ),
  );
}

function editGroup(g) {
  state.editing = g.key;
  calmForm.days = new Set(g.days);
  calmForm.actions = new Set(g.actions);
  $("calm-start").value = hhmm(g.start);
  $("calm-end").value = hhmm(g.end);
  $("calm-custom").value = g.custom || "";
  $("calm-title").textContent = "무던한 시간 수정하기";
  $("calm-save").textContent = "수정 저장";
  $("calm-cancel").hidden = false;
  $("calm-error").hidden = true;
  syncChips();
  renderRegistered();
  renderPreview();
  $("calm-card").scrollIntoView({ behavior: "smooth", block: "start" });
}

async function deleteGroup(g) {
  const ok = await confirmDialog("삭제할까요?", `${daysLabel(g.days)} ${hhmm(g.start)}–${hhmm(g.end)} 무던한 시간을 지워요. 사용자 앱에서도 사라져요.`, "삭제");
  if (!ok) return;
  const { error } = await db.from("business_calm_hours").delete().in("id", g.ids);
  if (error) {
    console.warn("삭제 실패", error);
    toast("삭제하지 못했어요. 잠시 뒤 다시 시도해 주세요");
    return;
  }
  if (state.editing === g.key) resetCalmForm();
  toast("삭제했어요");
  await loadCalm();
}

$("calm-cancel").addEventListener("click", () => {
  resetCalmForm();
  renderRegistered();
  renderPreview();
});

$("calm-save").addEventListener("click", async () => {
  const err = $("calm-error");
  const start = $("calm-start").value;
  const end = $("calm-end").value;
  const custom = $("calm-custom").value.trim();
  const days = [...calmForm.days].sort((a, b) => a - b);
  const editingIds = calmGroups(state.calm).find((g) => g.key === state.editing)?.ids || [];

  // 저장 전에 확인 (docs/03 8번 '입력 확인')
  let problem = null;
  if (!days.length) problem = "요일을 하나 이상 골라 주세요";
  else if (toMin(end) <= toMin(start)) problem = "끝 시각이 시작 시각보다 뒤여야 해요";
  else if (!calmForm.actions.size && !custom) problem = "이 시간에 하는 일을 하나 이상 골라 주세요";
  if (!problem) {
    for (const d of days) {
      problem = insideHours(d, start, end);
      if (problem) break;
      const clash = state.calm.find(
        (r) => r.weekday === d && !editingIds.includes(r.id) && toMin(start) < toMin(r.end_time) && toMin(r.start_time) < toMin(end),
      );
      if (clash) {
        problem = `${WEEKDAYS[d - 1]}요일 ${hhmm(clash.start_time)}–${hhmm(clash.end_time)} 와 시간이 겹쳐요`;
        break;
      }
    }
  }
  if (problem) {
    err.textContent = problem;
    err.hidden = false;
    return;
  }
  err.hidden = true;

  const rows = days.map((d) => ({
    place_id: state.place.id,
    weekday: d,
    start_time: start,
    end_time: end,
    actions: calmForm.actions.size ? [...calmForm.actions] : null,
    custom_action: custom || null,
    updated_at: new Date().toISOString(),
  }));
  const btn = $("calm-save");
  btn.disabled = true;
  // 새 줄을 먼저 넣고 옛 줄을 지운다. 넣다가 실패하면 원래 시간이 그대로 남는다
  const ins = await db.from("business_calm_hours").insert(rows);
  let failed = ins.error;
  if (!failed && editingIds.length) failed = (await db.from("business_calm_hours").delete().in("id", editingIds)).error;
  btn.disabled = false;
  if (failed) {
    console.warn("무던한 시간 저장 실패", failed);
    err.textContent = "저장하지 못했어요. 잠시 뒤 다시 시도해 주세요";
    err.hidden = false;
    await loadCalm();
    return;
  }
  toast(editingIds.length ? "수정했어요. 사용자 앱 지도에 반영됐어요" : "등록했어요. 사용자 앱 지도에 반영됐어요");
  resetCalmForm();
  await loadCalm();
});

// ── 사용자 앱 미리보기 ───────────────────────────────
// 앱 지도 마커 · 장소 카드 · 상세 '사업장' 탭과 같은 말로 그린다

function openNowText() {
  if (!state.hours) return "영업시간 정보 없음";
  const { weekday, minute } = seoulNow();
  const today = state.hoursDraft[weekday - 1];
  const yesterday = state.hoursDraft[(weekday + 5) % 7];
  const o = toMin(today.open);
  const c = toMin(today.close);
  if (!today.closed && (c > o ? minute >= o && minute < c : minute >= o)) return "영업 중";
  if (!yesterday.closed && toMin(yesterday.close) < toMin(yesterday.open) && minute < toMin(yesterday.close)) return "영업 중";
  return "영업 종료";
}

/** 지금(서울)이 등록한 무던한 시간 안인지. 그러면 앱 지도 마커에 '무던한 시간' 이 붙는다 */
function calmNow(rows) {
  const { weekday, minute } = seoulNow();
  return rows.some((r) => r.weekday === weekday && minute >= toMin(r.start_time) && minute < toMin(r.end_time));
}

function renderPreview() {
  const box = $("preview");
  if (!state.place) return;
  let rows = state.calm;
  let draft = false;
  // 아직 등록한 게 없으면 입력 중인 값으로 "이렇게 보여요"
  if (!rows.length && calmForm.days.size) {
    draft = true;
    rows = [...calmForm.days].map((d) => ({
      weekday: d,
      start_time: $("calm-start").value + ":00",
      end_time: $("calm-end").value + ":00",
      actions: [...calmForm.actions],
      custom_action: $("calm-custom").value.trim() || null,
    }));
  }
  const groups = calmGroups(rows);
  const lines = groups.map((g) => `${daysLabel(g.days)} ${clockShort(g.start)}~${clockShort(g.end)}시`);
  const actions = [...new Set(groups.flatMap(actionWords))];
  const now = rows.length && calmNow(rows);

  const marker = el("div", { class: "map-strip", "aria-hidden": "true" }, [
    el("span", { class: "marker" }, [el("span", { class: "dot" }), el("span", { text: shortName(state.place.name) }), now ? el("span", { class: "tag", text: "무던한 시간" }) : null]),
  ]);
  const sheet = el("div", { class: "sheet" }, [
    el("p", { class: "sheet-name", text: state.place.name }),
    el("p", { class: "sheet-info", text: [state.place.category, openNowText()].join(" · ") }),
    lines.length
      ? el("div", { class: "biz-note" }, [
          el("span", { class: "biz-badge", text: "사업장 등록" }),
          el("span", { class: "biz-text", text: now ? "지금 무던한 시간 운영 중" : `${lines[0]} 무던한 시간 운영${lines.length > 1 ? ` 외 ${lines.length - 1}` : ""}` }),
        ])
      : el("p", { class: "sheet-info", style: "margin-top:12px", text: "등록한 무던한 시간이 여기에 보여요" }),
  ]);
  const card = lines.length
    ? el("div", { class: "biz-card" }, [
        el("p", { class: "biz-card-title", text: "사업장이 등록한 무던한 시간" }),
        el("p", { class: "biz-card-hours", text: lines.join("\n") }),
        actions.length ? el("div", { class: "action-chips" }, actions.map((a) => el("span", { text: a }))) : null,
      ])
    : null;
  const phone = el("div", { class: draft ? "preview-empty" : null }, [marker, sheet, card]);
  box.replaceChildren(
    phone,
    el("p", {
      class: "preview-foot",
      text: draft
        ? "등록하면 이렇게 보여요"
        : "지금이 등록한 시간이면 지도 마커에 '무던한 시간' 이 붙어요.",
    }),
  );
}

// ── 시작 ─────────────────────────────────────────────

async function init() {
  if (!db) {
    $("view-loading").textContent = "config.js 에 Supabase 주소와 키를 넣어 주세요 (config.example.js 참고)";
    return;
  }
  buildCalmControls();
  loadKakao();
  $("search").addEventListener("input", () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(runSearch, 300);
  });
  const { data, error } = await db.from("places").select(PLACE_COLUMNS).order("name");
  if (error) console.warn("장소 읽기 실패", error);
  state.places = data || [];

  // 주소에 ?place=13 이 있거나 전에 고른 가게가 있으면 바로 그 가게 화면. 사장님께 가게 바로가기 링크로 줄 수 있다
  let saved = Number(new URLSearchParams(location.search).get("place")) || null;
  try {
    saved = saved || Number(localStorage.getItem(STORE_KEY));
  } catch (_) {
    // 저장소를 못 쓰는 브라우저
  }
  const found = state.places.find((p) => p.id === saved);
  if (found) await openPlace(found);
  else {
    renderMyPlaces();
    show("find");
  }
}

init();
