/* =========================================================
   Bistroops 會員功能（登入／註冊／忘記密碼／會員中心）
   對應的 HTML：templates/member/member_modals.html
   對應的 CSS ：static/css/front/member.css

   ※ 目前畫面用的是假資料。Java 寫好後要改的地方都標了「Thymeleaf 串接」，
     用 Ctrl+F 搜尋這幾個字就能一個一個找到（這個檔案和 member_modals.html 都有）。
     整體做法先看「1. 後端串接點 memberApi」開頭的總覽。
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和 main.js 互相衝突
(() => {

/* ========== 0. 共用設定與小工具 ========== */
const PHONE_RE = /^09\d{8}$/;            // 手機：09 開頭共 10 碼
const PW_CHARSET_RE = /^[A-Za-z0-9]+$/;  // 密碼：只能有英文與數字
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PW_MIN_LENGTH = 8;
const RESEND_SECONDS = 30;               // 發送驗證碼後鎖定的秒數

const $ = id => document.getElementById(id);

// 把文字中的 < > & " ' 換掉，避免後端資料被當成 HTML 執行
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, ch => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
  ));
}

// 顯示提示訊息；type 可為 'ok'（綠）、'error'（紅）或空字串（灰）；text 為空則隱藏
function setMsg(el, text, type = '') {
  el.textContent = text || '';
  el.classList.remove('ok', 'error');
  if (type) el.classList.add(type);
  el.hidden = !text;
}

// '2026-09-20' → '2026年09月20日'
function formatDate(dateText) {
  const [y, m, d] = String(dateText).split('-');
  return `${y}年${m}月${d}日`;
}

// 送出期間先把送出按鈕鎖住，避免重複點擊
async function withSubmitLock(form, task) {
  const btn = form.querySelector('[type="submit"]');
  btn.disabled = true;
  try {
    return await task();
  } finally {
    btn.disabled = false;
  }
}

let toastTimer = null;
function showToast(text) {
  const toast = $('toast');
  toast.textContent = text;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 2600);
}


/* ========== 1. 後端串接點 memberApi ==========
   目前每個函式都回傳「示範用的假資料」，讓畫面可以先操作。
   示範帳號：手機 0912345678 ／ 密碼 abc12345 ／ 簡訊驗證碼 123456

   ---------------------------------------------------------
   【Thymeleaf 串接】總覽
   ---------------------------------------------------------
   我們不用 JSON，所以做法是「表單直接送到 Controller，Controller 再回傳頁面」：

     A. 送出資料：<form> 加上 th:action 和 method="post"，
                  JS 檢查完欄位後呼叫 form.submit() 把表單送出去
     B. 顯示資料：Controller 把資料放進 Model 或 Session，
                  HTML 用 th:text / th:value / th:each 顯示
     C. 送出後整個頁面會重新載入，對話框會自動關上。
        要讓它再打開（例如登入失敗要顯示錯誤），由 Controller 傳值給頁面，
        見這個檔案最下面的「7. 頁面載入後」

   Java 寫好一個功能，就把下面對應的 memberApi 函式刪掉。
   全部改完後，從 DEMO_LOGIN_KEY 到 memberApi 結尾（整個第 1 區的程式）都可以刪除。

     memberApi 函式                 改成什麼                     提示寫在
     ---------------------------   --------------------------   ---------------------------
     getSession                    HTML 直接判斷 session         5. 登入狀態
     newCaptcha                    <img> 指向 Controller         4. 登入 refreshCaptcha()
     login                         form.submit()                4. 登入
     logout                        連到 /member/logout           5. 登入狀態
     register                      form.submit()                4. 註冊
     verifyCode / resetPassword    form.submit()                4. 忘記密碼
     isPhoneRegistered             fetch 純文字（或直接拿掉）     4. 註冊 checkRegPhone()
     sendCode                      fetch 純文字                  3-3. setupSendCode()
     getProfile                    th:value                     6. 個人資料 loadProfile()
     updateProfile                 form.submit()                6. 個人資料
     changePassword                form.submit()                6. 更改密碼
     getOrders                     th:each                      6. 歷史訂單
     getReservations               th:each                      6. 預約紀錄
     cancelReservation             每筆訂位各一個小表單           6. 預約紀錄
     getCoupons                    th:each                      6. 優惠券

   Controller 和畫面之間約定好的名稱（可以自己改，但 Java、HTML、JS 三邊要一致）：

     Session  loginMember   登入中的會員物件（MemberVO），沒登入就是沒有這個屬性
     Model    loginError / regError / forgotCodeError / resetError / profileError / changePwError
                            各表單的錯誤訊息文字
     Model    startView     重新載入後要打開登入對話框的哪個畫面：login / register / forgot / reset
     Model    startTab      重新載入後要打開會員中心的哪個分頁：profile / password / orders / reservations / coupons
     Model    toastMsg      重新載入後要跳出的小提示文字，例如「登入成功」
     Model    orders / currentResvs / historyResvs / coupons   會員中心要顯示的清單

   小提醒：
     - Controller 用 redirect 的話，Model 的值要改用 RedirectAttributes 的 addFlashAttribute() 才帶得過去
     - 下面提示中的網址（/member/login 等）和 VO 欄位名稱（name、phone 等）都是範例，請換成你們實際的
     - JS 裡的網址直接寫 '/member/...' 即可（目前專案沒有設定 context-path）
   ================================================== */
const DEMO_LOGIN_KEY = 'demoLoginPhone';

const demo = {
  smsCode: '123456',
  captcha: '',
  members: {
    '0912345678': { password: 'abc12345', name: '陳小姐', email: '', birthday: '1995-08-20' }
  },
  orders: [
    { id: 'O20260920001', date: '2026-09-20', time: '11:30 a.m', people: 3, type: '用餐',
      items: [{ name: '松露野菇濃湯', qty: 3, price: 180 }, { name: '香煎鴨胸', qty: 2, price: 580 }, { name: '焦糖布蕾', qty: 3, price: 160 }] },
    { id: 'O20260905002', date: '2026-09-05', time: '10:45 a.m', people: 4, type: '用餐',
      items: [{ name: '凱薩沙拉', qty: 2, price: 220 }, { name: '紅酒燉牛頰', qty: 4, price: 680 }] },
    { id: 'O20260818003', date: '2026-08-18', time: '07:15 p.m', people: 2, type: '用餐',
      items: [{ name: '生火腿拼盤', qty: 1, price: 420 }, { name: '香草烤春雞', qty: 2, price: 520 }, { name: '提拉米蘇', qty: 2, price: 180 }] }
  ],
  reservations: {
    current: [
      { id: 'R20261010001', date: '2026-10-10', time: '18:30', people: 4, note: '靠窗座位、慶生' }
    ],
    history: [
      { id: 'R20260920001', date: '2026-09-20', time: '11:30', people: 3, note: '' },
      { id: 'R20260818002', date: '2026-08-18', time: '19:00', people: 2, note: '需要兒童座椅' }
    ]
  },
  coupons: [
    { id: 'C001', title: '壽星招待甜點', desc: '生日當月用餐，招待主廚甜點一份。', expire: '2026-12-31', used: false },
    { id: 'C002', title: '平日午間 85 折', desc: '週一至週五 11:00–14:00 套餐適用。', expire: '2026-11-30', used: false },
    { id: 'C003', title: '新會員 100 元折抵', desc: '消費滿 1000 元可折抵 100 元。', expire: '2026-09-30', used: true }
  ]
};

// 模擬網路延遲後回傳結果
const fake = result => new Promise(resolve => setTimeout(() => resolve(result), 300));

function demoLoginPhone() {
  const phone = sessionStorage.getItem(DEMO_LOGIN_KEY);
  return demo.members[phone] ? phone : null;
}

const memberApi = {
  // 目前是否已登入。回傳：{ loggedIn: true/false, name: '陳小姐' }
  getSession() {
    const phone = demoLoginPhone();
    return fake(phone ? { loggedIn: true, name: demo.members[phone].name } : { loggedIn: false });
  },

  // 產生新的圖形驗證碼。回傳：驗證碼文字（示範用）
  // 後端若是輸出圖片，改成更換 <img> 的 src 即可，見「4. 登入」的 refreshCaptcha()
  newCaptcha() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    demo.captcha = Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
    return fake(demo.captcha);
  },

  // 登入。傳入：{ phone, password, captcha }　回傳：{ ok, name, message }
  login({ phone, password, captcha }) {
    if (captcha.toUpperCase() !== demo.captcha) return fake({ ok: false, message: '※驗證碼錯誤' });
    const member = demo.members[phone];
    if (!member || member.password !== password) return fake({ ok: false, message: '※帳號或密碼錯誤' });
    sessionStorage.setItem(DEMO_LOGIN_KEY, phone);
    return fake({ ok: true, name: member.name });
  },

  // 登出。回傳：{ ok }
  logout() {
    sessionStorage.removeItem(DEMO_LOGIN_KEY);
    return fake({ ok: true });
  },

  // 手機號碼是否已註冊。回傳：true（已被註冊）／false（可註冊）
  isPhoneRegistered(phone) {
    return fake(Boolean(demo.members[phone]));
  },

  // 發送簡訊驗證碼。purpose：'register' 註冊／'forgot' 忘記密碼／'change' 會員中心更改密碼
  // （'change' 時 phone 會是 null，請後端使用登入中會員的手機）
  // 回傳：{ ok, message, reason }　reason 為 'NOT_REGISTERED' 代表手機尚未註冊
  sendCode(phone, purpose) {
    const registered = Boolean(demo.members[phone]);
    if (purpose === 'register' && registered) return fake({ ok: false, message: '☒ 手機號碼已被註冊' });
    if (purpose === 'forgot' && !registered) return fake({ ok: false, reason: 'NOT_REGISTERED' });
    return fake({ ok: true, message: `驗證碼已發送（示範用驗證碼：${demo.smsCode}）` });
  },

  // 註冊。傳入：{ phone, code, password }　回傳：{ ok, message }
  register({ phone, code, password }) {
    if (code !== demo.smsCode || demo.members[phone]) return fake({ ok: false });
    demo.members[phone] = { password, name: '', email: '', birthday: '' };
    return fake({ ok: true });
  },

  // 忘記密碼步驟 1：確認驗證碼。傳入：{ phone, code }　回傳：{ ok }
  verifyCode({ phone, code }) {
    return fake({ ok: Boolean(demo.members[phone]) && code === demo.smsCode });
  },

  // 忘記密碼步驟 2：設定新密碼。傳入：{ phone, code, password }　回傳：{ ok, message }
  resetPassword({ phone, code, password }) {
    if (!demo.members[phone] || code !== demo.smsCode) return fake({ ok: false });
    demo.members[phone].password = password;
    return fake({ ok: true });
  },

  // 取得個人資料。回傳：{ name, email, phone, birthday: 'yyyy-MM-dd' 或空字串 }
  getProfile() {
    const phone = demoLoginPhone();
    const { name, email, birthday } = demo.members[phone];
    return fake({ name, email, phone, birthday });
  },

  // 更新個人資料。傳入：{ name, email, birthday }　回傳：{ ok, name, message }
  updateProfile({ name, email, birthday }) {
    Object.assign(demo.members[demoLoginPhone()], { name, email, birthday });
    return fake({ ok: true, name });
  },

  // 會員中心更改密碼。傳入：{ code, password }　回傳：{ ok, message }
  changePassword({ code, password }) {
    if (code !== demo.smsCode) return fake({ ok: false, message: '※驗證碼錯誤請重新輸入' });
    demo.members[demoLoginPhone()].password = password;
    return fake({ ok: true });
  },

  // 歷史訂單。回傳：[{ id, date, time, people, type, items: [{ name, qty, price }] }]
  getOrders() {
    return fake(demo.orders);
  },

  // 預約紀錄。回傳：{ current: [...], history: [...] }，每筆為 { id, date, time, people, note }
  getReservations() {
    return fake(demo.reservations);
  },

  // 取消訂位。傳入：訂位編號　回傳：{ ok, message }
  cancelReservation(id) {
    demo.reservations.current = demo.reservations.current.filter(r => r.id !== id);
    return fake({ ok: true });
  },

  // 優惠券。回傳：[{ id, title, desc, expire, used }]
  getCoupons() {
    return fake(demo.coupons);
  }
};


/* ========== 2. 對話框開關 ========== */
const authModal = $('authModal');
const memberModal = $('memberModal');
let lastFocus = null;   // 開啟對話框前的焦點位置，關閉後還原

function openModal(modal) {
  if (!modal.hidden) return;   // 已經開著就不用再處理
  lastFocus = document.activeElement;
  modal.hidden = false;
  document.body.classList.add('modal-open');
  modal.querySelector('.modal-dialog').focus();
}

function closeModal(modal) {
  if (modal.hidden) return;
  modal.hidden = true;
  if (!document.querySelector('.modal:not([hidden])')) {
    document.body.classList.remove('modal-open');
    if (lastFocus) lastFocus.focus();
  }
}

// 點到蒙版（背景）或右上角 × 就關閉
document.addEventListener('click', e => {
  const closer = e.target.closest('[data-close-modal]');
  if (closer) closeModal(closer.closest('.modal'));
});

// 按 Esc 關閉
document.addEventListener('keydown', e => {
  if (e.key !== 'Escape') return;
  const opened = document.querySelector('.modal:not([hidden])');
  if (opened) closeModal(opened);
});


/* ========== 3. 共用表單行為 ========== */

// 3-1. 密碼顯示／隱藏（眼睛按鈕）
document.addEventListener('click', e => {
  const btn = e.target.closest('[data-pw-toggle]');
  if (!btn) return;
  const input = btn.parentElement.querySelector('input');
  const show = input.type === 'password';
  input.type = show ? 'text' : 'password';
  btn.classList.toggle('showing', show);
  btn.setAttribute('aria-label', show ? '隱藏密碼' : '顯示密碼');
});

// 3-2. 密碼條件即時檢查。回傳一個函式，呼叫後可得知密碼是否全部合格
function setupPasswordCheck(form) {
  const pw = form.querySelector('[data-pw]');
  const confirm = form.querySelector('[data-pw-confirm]');
  const ruleLength = form.querySelector('[data-rule="length"]');
  const ruleCharset = form.querySelector('[data-rule="charset"]');
  const mismatch = form.querySelector('[data-pw-mismatch]');

  // submitting 為 true 時，「再次輸入」留白也算不相符
  function check(submitting = false) {
    const lengthOk = pw.value.length >= PW_MIN_LENGTH;
    const charsetOk = PW_CHARSET_RE.test(pw.value);
    const same = confirm.value === pw.value;

    ruleLength.classList.toggle('ok', lengthOk);    // 符合條件 → 變綠色
    ruleCharset.classList.toggle('ok', charsetOk);
    mismatch.hidden = same || (!submitting && confirm.value === '');
    return lengthOk && charsetOk && same;
  }

  pw.addEventListener('input', () => check());
  confirm.addEventListener('input', () => check());
  // form.reset() 之後欄位才會清空，所以延後一點再重新檢查
  form.addEventListener('reset', () => setTimeout(() => {
    form.classList.remove('was-submitted');
    check();
  }));

  return () => {
    form.classList.add('was-submitted');
    return check(true);
  };
}

// 3-3.「發送驗證碼」按鈕：發送成功後倒數 30 秒並鎖定，倒數完改為「重新發送」
function setupSendCode({ button, getPhone, purpose, msgEl, onFail }) {
  let timer = null;

  function reset() {
    clearInterval(timer);
    button.disabled = false;
    button.textContent = '發送驗證碼';
  }

  button.addEventListener('click', async () => {
    const phone = getPhone();
    // getPhone 回傳 null 代表不需要輸入手機（會員中心更改密碼）
    if (phone !== null && !PHONE_RE.test(phone)) {
      setMsg(msgEl, '※請輸入正確的手機號碼（09 開頭共 10 碼）', 'error');
      return;
    }

    button.disabled = true;
    // 【Thymeleaf 串接】發送驗證碼
    //   這個動作不能讓頁面重新載入（使用者填到一半的資料會不見），所以要在背景送出請求。
    //   不用 JSON 也可以：Controller 方法加上 @ResponseBody 並回傳 String，JS 用 res.text() 接。
    //   把下面那行 memberApi.sendCode(...) 換成：
    //
    //     const text = await fetch('/member/sendCode', {
    //       method: 'POST',
    //       body: new URLSearchParams({ phone: phone ?? '', purpose })   // Controller 用 @RequestParam 接
    //     }).then(res => res.text());
    //     const result = text === 'OK'
    //       ? { ok: true }
    //       : { ok: false, message: text, reason: text };
    //
    //   Controller 回傳的文字：成功回 "OK"；失敗回要顯示的訊息（例如 "☒ 手機號碼已被註冊"）；
    //   忘記密碼時手機未註冊請回 "NOT_REGISTERED"（畫面會顯示「點選前往註冊」那一行）。
    //   purpose 是 'change' 時 phone 會是空字串，請改用 session 裡登入中會員的手機。
	const text = await fetch('/member/sendCode', {
	   method: 'POST',
	   body: new URLSearchParams({ phone: phone ?? '', purpose })   // Controller 用 @RequestParam 接
	}).then(res => res.text());
	const result = text === 'OK'
	   ? { ok: true }
	   : { ok: false, message: text, reason: text };

    if (!result.ok) {
      button.disabled = false;
      setMsg(msgEl, result.message, 'error');
      if (onFail) onFail(result);
      return;
    }

    setMsg(msgEl, result.message || '驗證碼已發送', 'ok');
    let seconds = RESEND_SECONDS;
    button.textContent = `${seconds} 秒後可重發`;
    timer = setInterval(() => {
      seconds -= 1;
      if (seconds > 0) {
        button.textContent = `${seconds} 秒後可重發`;
      } else {
        clearInterval(timer);
        button.disabled = false;
        button.textContent = '重新發送';
      }
    }, 1000);
  });

  return reset;
}


/* ========== 4. 登入／註冊／忘記密碼 ========== */
const loginForm = $('loginForm');
const registerForm = $('registerForm');
const forgotForm = $('forgotForm');
const resetForm = $('resetForm');

// 忘記密碼步驟 1 通過後，暫存手機與驗證碼給步驟 2 使用
let forgotData = null;

// 切換對話框內的畫面：login / register / forgot / reset
function showAuthView(name) {
  authModal.querySelectorAll('.auth-view').forEach(view => {
    view.hidden = view.dataset.view !== name;
  });
  // 每次切換都把表單與提示訊息清乾淨
  [loginForm, registerForm, forgotForm, resetForm].forEach(form => form.reset());
  authModal.querySelectorAll('.form-msg').forEach(msg => { msg.hidden = true; });
  //resetRegSendCode();
  //resetForgotSendCode();
  if (name === 'login') refreshCaptcha();
  authModal.scrollTop = 0;
}

function openAuth(view = 'login') {
  showAuthView(view);
  openModal(authModal);
}

document.addEventListener('click', e => {
  // Header 的「登入 / 註冊」
  if (e.target.closest('[data-open-auth]')) {
    e.preventDefault();   // 取消連結原本的動作，避免 href="#" 讓頁面跳回頂端
    openAuth('login');
    return;
  }
  // 對話框內切換畫面的連結（忘記密碼？／註冊會員／返回會員登入）
  const switcher = e.target.closest('[data-auth-view]');
  if (switcher) showAuthView(switcher.dataset.authView);
});

// ---------- 登入 ----------
const captchaImg = $('captchaImg');
const loginError = $('loginError');

// 【Thymeleaf 串接】圖形驗證碼
//   Controller 寫一個輸出圖片的方法（例如 GET /member/captcha），並把正確答案存進 session。
//   HTML 把 captchaImg 從 <span> 換成 <img>（見 member_modals.html），
//   然後把函式內容換成下面這行；網址後面加時間是為了讓瀏覽器每次都重新抓圖：
//     captchaImg.src = '/member/captcha?t=' + Date.now();
function refreshCaptcha() {
  

  captchaImg.src = '/member/captcha?t=' + Date.now();
}

$('captchaRefresh').addEventListener('click', refreshCaptcha);



// ---------- 登入送出 ----------
loginForm.addEventListener('submit', async e => {
  e.preventDefault();

  // 先清除上一次錯誤訊息
  setMsg(loginError, '');

  const phone = loginForm.phone.value.trim();
  const password = loginForm.password.value;
  const captcha = loginForm.captcha.value.trim();

  // 手機格式檢查
  if (!PHONE_RE.test(phone)) {
    setMsg(
      loginError,
      '※請輸入正確的手機號碼（09 開頭共 10 碼）',
      'error'
    );
    return;
  }

  // 密碼沒輸入
  if (password === '') {
    setMsg(loginError, '※請輸入密碼', 'error');
    return;  
  }
  
  

  // 背景送到 Controller，不重新整理頁面
  const result = await fetch('/member/login', {
    method: 'POST',
	body: new URLSearchParams({phone: phone,password: password,captcha: captcha})}).then(res => res.text());

  // 登入成功
  if (result === 'OK') {
    window.location.href = '/bistroops';
    return;
  }
  
  // 圖片驗證碼錯誤
  if (result === 'CAPTCHA_ERROR') {
    loginForm.captcha.value = '';

    setMsg(
      loginError,
      '※驗證碼錯誤',
      'error'
    );

    refreshCaptcha();
    loginForm.captcha.focus();
    return;
  }

  // 登入失敗：手機保留、只清密碼
  // 帳號或密碼錯誤
  loginForm.password.value = '';
  loginForm.captcha.value = '';

  // 重新產生新的驗證碼
  refreshCaptcha();

  setMsg(
    loginError,
    '※帳號或密碼錯誤',
    'error'
  );

  loginForm.password.focus();
});


// 快速登入：後端完成後改成導向各平台的登入網址
// 例如 location.href = '/oauth2/authorization/' + provider;
authModal.querySelectorAll('[data-social]').forEach(btn => {
  btn.addEventListener('click', () => {
    const provider = btn.dataset.social;
    showToast(`尚未串接 ${provider.toUpperCase()} 快速登入`);
  });
});

// ---------- 註冊 ----------
const regPhoneMsg = $('regPhoneMsg');
const regError = $('regError');
const isRegPasswordValid = setupPasswordCheck(registerForm);

// 離開手機欄位時，先檢查格式
registerForm.memTel.addEventListener('blur', () => {

  const phone = registerForm.memTel.value.trim();

  if (phone === '') {
    setMsg(regPhoneMsg, '');
    return;
  }

  if (!PHONE_RE.test(phone)) {
    setMsg(
      regPhoneMsg,
      '※請輸入正確的手機號碼（09 開頭共 10 碼）',
      'error'
    );
    return;
  }

  setMsg(regPhoneMsg, '☑ 手機號碼格式正確', 'ok');
});


// 註冊送出
registerForm.addEventListener('submit', async e => {

  // 先攔住，做前端檢查
  e.preventDefault();

  setMsg(regError, '');

  const phone = registerForm.memTel.value.trim();
  const password = registerForm.memPassword.value;
  const password2 = registerForm.password2.value;

  // 1. 手機
  if (phone === '') {
    setMsg(regPhoneMsg, '※請輸入手機號碼', 'error');
    return;
  }

  if (!PHONE_RE.test(phone)) {
    setMsg(
      regPhoneMsg,
      '※請輸入正確的手機號碼（09 開頭共 10 碼）',
      'error'
    );
    return;
  }

  // 2. 密碼規則
  if (!isRegPasswordValid()) {
    return;
  }

  // 3. 再次輸入密碼
  if (password !== password2) {
    setMsg(regError, '※兩次輸入的密碼不相同', 'error');
    return;
  }

  // 4. 到後端檢查手機號碼是否已註冊
  const result = await fetch(
    '/member/checkPhone?memTel=' + encodeURIComponent(phone)
  ).then(res => res.text());

  if (result === 'TAKEN') {
    setMsg(regError, '手機號碼已註冊', 'error');
    return;
  }

  // 手機號碼沒有被註冊，正式送出註冊表單
  registerForm.submit();
 	 });
// ---------- 忘記密碼：步驟 1 身分驗證 ----------
const forgotPhoneMsg = $('forgotPhoneMsg');
const forgotNotRegistered = $('forgotNotRegistered');
const forgotCodeError = $('forgotCodeError');

const resetForgotSendCode = setupSendCode({
  button: $('forgotSendCode'),
  getPhone: () => {
    forgotNotRegistered.hidden = true;
    return forgotForm.phone.value.trim();
  },
  purpose: 'forgot',
  msgEl: forgotPhoneMsg,
  // 手機未註冊：顯示帶有「點選前往註冊」連結的那一行
  onFail: result => {
    if (result.reason === 'NOT_REGISTERED') {
      setMsg(forgotPhoneMsg, '');
      forgotNotRegistered.hidden = false;
    }
  }
});

forgotForm.addEventListener('submit', async e => {

  e.preventDefault();
  setMsg(forgotCodeError, '');

  const phone = forgotForm.phone.value.trim();
  const code = forgotForm.code.value.trim();

  // 手機格式錯誤
  if (!PHONE_RE.test(phone)) {
    forgotNotRegistered.hidden = true;

    setMsg(
      forgotPhoneMsg,
      '※請輸入正確的手機號碼（09 開頭共 10 碼）',
      'error'
    );

    return;
  }

  // 沒有輸入驗證碼
  if (code === '') {
    setMsg(
      forgotCodeError,
      '※請輸入驗證碼',
      'error'
    );
    return;
  }

  // 到 Controller 驗證手機 + 驗證碼
  const result = await fetch('/member/verifyForgotCode', {
    method: 'POST',
    body: new URLSearchParams({
      phone: phone,
      code: code
    })
  }).then(res => res.text());

  // 驗證成功
  if (result === 'OK') {
    showAuthView('reset');
    return;
  }

  // 還沒有發送驗證碼
  if (result === 'NO_CODE') {
    forgotForm.code.value = '';

    setMsg(
      forgotCodeError,
      '※請先發送驗證碼',
      'error'
    );

    forgotForm.code.focus();
    return;
  }

  // 驗證碼錯誤
  forgotForm.code.value = '';

  setMsg(
    forgotCodeError,
    '※驗證碼錯誤請重新輸入',
    'error'
  );

  forgotForm.code.focus();
});

  // 【Thymeleaf 串接】忘記密碼步驟 1  已完成
  //   上面的欄位檢查保留。從這裡到函式結尾整段換成一行：
  //     forgotForm.submit();
  //   Controller 要做的事：
  //     驗證碼正確：把手機存進 session（例如 session.setAttribute("resetPhone", phone)），
  //                 帶 startView="reset" 回到頁面，畫面就會打開「設定新密碼」
  //     驗證碼錯誤：帶 forgotCodeError="※驗證碼錯誤請重新輸入" 和 startView="forgot" 回到頁面
  //   改完後，上面的 let forgotData = null; 和這裡的 forgotData 都用不到了，可以刪掉



  // ---------- 忘記密碼：步驟 2 設定新密碼 ----------
  const resetError = $('resetError');
  const isResetPasswordValid = setupPasswordCheck(resetForm);

  resetForm.addEventListener('submit', e => {

    e.preventDefault();

    setMsg(resetError, '');

    if (!isResetPasswordValid()) return;

    // 【Thymeleaf 串接】忘記密碼步驟 2
    //   從這裡到函式結尾整段換成一行：
    //     resetForm.submit();
    //   Controller 要做的事：從 session 取出步驟 1 存的 resetPhone，更新密碼後把它移除
    //     成功：帶 startView="login"、toastMsg="修改成功，請重新登入" 後 redirect 回頁面
    //     失敗（例如 session 裡沒有 resetPhone）：帶 resetError="修改失敗！請重新操作" 和 startView="reset"

    resetForm.submit();
  });


/* ========== 5. 登入狀態（切換 Header 顯示） ========== */
const memberGreeting = $('memberGreeting');

// body 有 is-logged-in 這個 class 時，Header 會把「登入 / 註冊」換成「會員中心」
function setLoggedIn(name) {
  document.body.classList.add('is-logged-in');
  memberGreeting.textContent = `${name || '會員'} 您好～`;
}

function setLoggedOut() {
  document.body.classList.remove('is-logged-in');
}

// 【Thymeleaf 串接】登出  已完成
//   把 if 那行以下的四行換成一行，直接連到 Controller：
//     location.href = '/member/logout';
//   Controller 要做的事：session.removeAttribute("loginMember")（或 session.invalidate()），
//   帶 toastMsg="您已登出" 後 redirect 回頁面。改完後 setLoggedOut() 用不到了，可以刪掉
document.addEventListener('click', async e => {
  if (!e.target.closest('[data-logout]')) return;
  
location.href = '/member/logout';
});

// 【Thymeleaf 串接】判斷是否已登入
//   登入狀態改由 Thymeleaf 在產生頁面時就決定好，不用再問後端。
//   在每個有 Header 的頁面（例如 frontend_index.html）把 <body> 改成：
//     <body th:classappend="${session.loginMember != null} ? 'is-logged-in'">
//   然後把下面這三行（memberApi.getSession 開始）整段刪掉。
//   「會員 您好～」的姓名也改由 HTML 的 th:text 顯示（見 member_modals.html 的 memberGreeting），
//   所以 setLoggedIn() 之後也用不到，等所有呼叫它的地方都改完就可以刪掉
// 進入頁面時先確認是否已登入
memberApi.getSession().then(session => {
  if (session.loggedIn) setLoggedIn(session.name);
});


/* ========== 6. 會員中心 ========== */
const memberContent = memberModal.querySelector('.member-content');

// 切換分頁：profile / password / orders / reservations / coupons
function showMemberTab(name) {
  memberModal.querySelectorAll('.member-panel').forEach(panel => {
    panel.hidden = panel.dataset.panel !== name;
  });
  // 「更改密碼」屬於個人資料底下，所以左側仍標示在「個人資料」
  const navName = name === 'password' ? 'profile' : name;
  memberModal.querySelectorAll('.member-nav [data-member-tab]').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.memberTab === navName);
  });

  if (name === 'profile') loadProfile();
  if (name === 'password') loadChangePassword();
  if (name === 'orders') loadOrders();
  if (name === 'reservations') loadReservations();
  if (name === 'coupons') loadCoupons();
  memberModal.scrollTop = 0;
}

// Header 下拉選單與會員中心左側選單，都用 data-member-tab 指定要開哪個分頁
document.addEventListener('click', e => {
  const tabBtn = e.target.closest('[data-member-tab]');
  if (!tabBtn) return;
  e.preventDefault();
  openModal(memberModal);
  showMemberTab(tabBtn.dataset.memberTab);
});

// ---------- 個人資料 ----------
const profileForm = $('profileForm');
const profileError = $('profileError');
// 注意：不能寫 profileForm.name，那會取到表單自己的 name 屬性，所以姓名欄位用 id 取得
const profileName = $('profileName');
const birthYear = $('birthYear');
const birthMonth = $('birthMonth');
const birthDay = $('birthDay');

function fillSelect(select, from, to, keep) {
  const options = ['<option value="">--</option>'];
  const step = from <= to ? 1 : -1;
  for (let n = from; n !== to + step; n += step) options.push(`<option value="${n}">${n}</option>`);
  select.innerHTML = options.join('');
  if (keep && select.querySelector(`option[value="${keep}"]`)) select.value = keep;
}

// 依照選到的年、月決定該月有幾天（例如 2 月只有 28 或 29 天）
function fillDays() {
  const year = Number(birthYear.value) || 2000;
  const month = Number(birthMonth.value);
  const days = month ? new Date(year, month, 0).getDate() : 31;
  fillSelect(birthDay, 1, days, birthDay.value);
}

fillSelect(birthYear, new Date().getFullYear(), 1920);   // 年份由新到舊
fillSelect(birthMonth, 1, 12);
fillDays();
birthYear.addEventListener('change', fillDays);
birthMonth.addEventListener('change', fillDays);

// 【Thymeleaf 串接】顯示個人資料  已完成
//   姓名、Email、手機改由 HTML 的 th:value 直接帶出（見 member_modals.html 的 profileForm），
//   JS 只剩「生日下拉選單」要處理，因為選項是 JS 產生的。
//   HTML 會把生日放在表單的 data-birthday 屬性上，所以整個函式換成：
//
//     function loadProfile() {
//       const [y, m, d] = (profileForm.dataset.birthday || '').split('-').map(Number);
//       birthYear.value = y || '';
//       birthMonth.value = m || '';
//       fillDays();
//       birthDay.value = d || '';
//     }
function loadProfile() {
  setMsg(profileError, '');

  const [y, m, d] = (profileForm.dataset.birthday || '')
    .split('-')
    .map(Number);

  birthYear.value = y || '';
  birthMonth.value = m || '';

  fillDays();

  birthDay.value = d || '';
}

profileForm.addEventListener('submit', e => {
  e.preventDefault();
  setMsg(profileError, '');

  const email = profileForm.email.value.trim();

  // Email 有填才檢查格式
  if (email !== '' && !EMAIL_RE.test(email)) {
    setMsg(profileError, '※Email 格式不正確', 'error');
    return;
  }

  // 生日三個欄位要嘛都選、要嘛都不選
  const parts = [birthYear.value, birthMonth.value, birthDay.value];
  const filled = parts.filter(Boolean).length;

  if (filled !== 0 && filled !== 3) {
    setMsg(profileError, '※請完整選擇生日的年、月、日', 'error');
    return;
  }

  // 驗證通過，正式送到 Controller
  profileForm.submit();
});

// ---------- 更改密碼 ----------
const changePwForm = $('changePwForm');
const changePwError = $('changePwError');
const changePwPhoneMsg = $('changePwPhoneMsg');

const changePwCode = $('changePwCode');
const changePwCodeMsg = $('changePwCodeMsg');
let changePwCodeVerified = false;

const isChangePasswordValid = setupPasswordCheck(changePwForm);

changePwCode.addEventListener('input', async () => {
	
  changePwCodeVerified = false;

  const code = changePwCode.value.trim();

  // 還沒輸入滿 6 碼，不驗證
  if (code.length !== 6) {
    setMsg(changePwCodeMsg, '');
    return;
  }

  // 呼叫後端驗證
  const result = await fetch('/member/verifyCode', {
    method: 'POST',
    body: new URLSearchParams({ code })
  }).then(res => res.text());

  if (result === 'OK') {
	
	changePwCodeVerified = true;
    setMsg(changePwCodeMsg,'✓ 驗證成功','ok');

  } else if (result === 'NO_CODE') {

    setMsg(changePwCodeMsg,'※請先發送驗證碼','error');

  } else {
	
    setMsg(changePwCodeMsg,'※驗證碼錯誤','error');
  }
});

const resetChangePwSendCode = setupSendCode({
  button: $('changePwSendCode'),
  getPhone: () => null,   // 不用輸入手機，由後端發送到登入中會員的手機
  purpose: 'change',
  msgEl: changePwPhoneMsg
});

async function loadChangePassword() {
  
  setMsg(changePwPhoneMsg, '');
  resetChangePwSendCode();
  
  }
  //已完成
  // 【Thymeleaf 串接】手機號碼改由 HTML 的 th:text 顯示（見 member_modals.html 的 changePwPhone），
  //   下面這兩行直接刪掉。
  //   另外 Controller 回報 changePwError 時，這個函式開頭的 changePwForm.reset() 和
  //   setMsg(changePwError, '') 會把錯誤訊息清掉，所以那兩行也要一起刪掉


changePwForm.addEventListener('submit',  e => {
  e.preventDefault();
  setMsg(changePwError, '');

  const passwordValid = isChangePasswordValid();
  const code = changePwForm.code.value.trim();
  
  if (code === '') { setMsg(changePwError, '※請輸入驗證碼', 'error'); return; }
  
  // 驗證碼尚未通過即時驗證
  if (!changePwCodeVerified) {setMsg(changePwCodeMsg, '※請先完成驗證碼驗證', 'error');return;}
  if (!passwordValid) return;
  
  // 驗證通過，正式送到 Controller
    changePwForm.submit();
  });

  // 【Thymeleaf 串接】會員中心更改密碼
  //   上面的欄位檢查保留。從這裡到函式結尾整段換成一行：
  //     changePwForm.submit();
  //   Controller 會收到 code、password、password2，手機請從 session 的 loginMember 取得
  //     成功：帶 startTab="profile"、toastMsg="修改成功" 後 redirect 回頁面
  //     失敗：帶 changePwError="※驗證碼錯誤請重新輸入" 和 startTab="password" 回到頁面


// ---------- 歷史訂單 ----------
const ordersList = $('ordersList');

// 【Thymeleaf 串接】歷史訂單
//   清單改由 HTML 的 th:each 產生（寫法見 member_modals.html 的 ordersList），
//   JS 就不用再組畫面了，把整個函式換成空的：
//     function loadOrders() {}
//   下面「點 + 展開／收合」的程式不用動，th:each 產生的畫面一樣能用
async function loadOrders() {
  const orders = await memberApi.getOrders();
  if (orders.length === 0) {
    ordersList.innerHTML = '<p class="empty-text">目前沒有訂單紀錄</p>';
    return;
  }

  ordersList.innerHTML = orders.map(order => {
    const total = order.items.reduce((sum, item) => sum + item.price * item.qty, 0);
    const rows = order.items.map(item => `
      <tr>
        <td>${escapeHtml(item.name)}</td>
        <td class="num">× ${escapeHtml(item.qty)}</td>
        <td class="num">$${escapeHtml(item.price * item.qty)}</td>
      </tr>`).join('');

    return `
      <div class="record-item">
        <button type="button" class="record-head" data-toggle-record aria-expanded="false">
          <span class="record-info">
            <span>${escapeHtml(formatDate(order.date))} ${escapeHtml(order.time)}</span>
            <span>${escapeHtml(order.people)} 人</span>
            <span>${escapeHtml(order.type)}</span>
          </span>
          <span class="record-icon">+</span>
        </button>
        <div class="record-body" hidden>
          <table class="order-table">
            <thead><tr><th>品項</th><th class="num">數量</th><th class="num">小計</th></tr></thead>
            <tbody>${rows}</tbody>
          </table>
          <p class="order-total">合計　$${escapeHtml(total)}</p>
        </div>
      </div>`;
  }).join('');
}

// 點「+」或「訂位明細」展開／收合下方內容（歷史訂單與預約紀錄共用）
memberContent.addEventListener('click', e => {
  const toggle = e.target.closest('[data-toggle-record]');
  if (!toggle) return;
  const item = toggle.closest('.record-item');
  const body = item.querySelector('.record-body');
  body.hidden = !body.hidden;
  item.classList.toggle('open', !body.hidden);
  toggle.setAttribute('aria-expanded', !body.hidden);
});

// ---------- 預約紀錄 ----------
const resvList = $('resvList');
const resvTabs = memberModal.querySelectorAll('[data-resv-tab]');
let resvTab = 'current';   // 目前顯示：current 當前訂位／history 歷史訂位

// 【Thymeleaf 串接】預約紀錄
//   「當前訂位」和「歷史訂位」兩份清單都由 HTML 的 th:each 先產生好
//   （見 member_modals.html 的 resvList），JS 只負責切換要顯示哪一份。
//   把整個函式換成：
//
//     function loadReservations() {
//       resvTabs.forEach(btn => btn.classList.toggle('active', btn.dataset.resvTab === resvTab));
//       $('resvCurrent').hidden = resvTab !== 'current';
//       $('resvHistory').hidden = resvTab !== 'history';
//     }
async function loadReservations() {
  const reservations = await memberApi.getReservations();
  const list = reservations[resvTab];
  resvTabs.forEach(btn => btn.classList.toggle('active', btn.dataset.resvTab === resvTab));

  if (list.length === 0) {
    resvList.innerHTML = `<p class="empty-text">目前沒有${resvTab === 'current' ? '進行中的' : '歷史'}訂位</p>`;
    return;
  }

  resvList.innerHTML = list.map(resv => `
    <div class="record-item">
      <div class="record-head">
        <span class="record-info">
          <span>${escapeHtml(formatDate(resv.date))}</span>
          <span>${escapeHtml(resv.time)}</span>
          <span>${escapeHtml(resv.people)} 人</span>
        </span>
        <span class="record-actions">
          <button type="button" class="btn-small" data-toggle-record aria-expanded="false">訂位明細</button>
          ${resvTab === 'current'
            ? `<button type="button" class="btn-small danger" data-cancel-resv="${escapeHtml(resv.id)}">取消訂位</button>`
            : ''}
        </span>
      </div>
      <div class="record-body" hidden>
        <p>訂位編號：${escapeHtml(resv.id)}</p>
        <p>用餐時間：${escapeHtml(formatDate(resv.date))} ${escapeHtml(resv.time)}</p>
        <p>用餐人數：${escapeHtml(resv.people)} 人</p>
        <p>備註：${escapeHtml(resv.note || '無')}</p>
      </div>
    </div>`).join('');
}

resvTabs.forEach(btn => {
  btn.addEventListener('click', () => {
    resvTab = btn.dataset.resvTab;
    loadReservations();
  });
});

// 【Thymeleaf 串接】取消訂位
//   HTML 裡每筆訂位的「取消訂位」按鈕會各自包在一個小表單內（見 member_modals.html 的 resvList），
//   按下去就直接送到 Controller。JS 只要在送出前跳確認視窗，把下面整段換成：
//
//     resvList.addEventListener('submit', e => {
//       if (!confirm('確定要取消這筆訂位嗎？')) e.preventDefault();   // 按「取消」就不送出
//     });
//
//   Controller 要做的事：取消訂位後，帶 startTab="reservations"、toastMsg="已取消訂位" redirect 回頁面
resvList.addEventListener('click', async e => {
  const btn = e.target.closest('[data-cancel-resv]');
  if (!btn) return;
  if (!confirm('確定要取消這筆訂位嗎？')) return;

  btn.disabled = true;
  const result = await memberApi.cancelReservation(btn.dataset.cancelResv);
  if (!result.ok) {
    btn.disabled = false;
    showToast(result.message || '取消失敗，請稍後再試');
    return;
  }
  await loadReservations();
  showToast('已取消訂位');
});

// ---------- 優惠券 ----------
const couponList = $('couponList');

// 【Thymeleaf 串接】優惠券
//   清單改由 HTML 的 th:each 產生（見 member_modals.html 的 couponList），
//   把整個函式換成空的：
//     function loadCoupons() {}
async function loadCoupons() {
  const coupons = await memberApi.getCoupons();
  if (coupons.length === 0) {
    couponList.innerHTML = '<p class="empty-text">目前沒有優惠券</p>';
    return;
  }

  couponList.innerHTML = coupons.map(coupon => `
    <div class="coupon${coupon.used ? ' used' : ''}">
      <h4>${escapeHtml(coupon.title)}</h4>
      <p>${escapeHtml(coupon.desc)}</p>
      <p class="coupon-expire">${coupon.used ? '已使用' : `使用期限：${escapeHtml(formatDate(coupon.expire))}`}</p>
    </div>`).join('');
}


 /*========== 7. 頁面載入後 ==========
   【Thymeleaf 串接】表單送出後頁面會重新載入，對話框會關上。
   Controller 用 startView / startTab / toastMsg 告訴畫面「載入後要打開什麼」，
   HTML 會把這三個值寫在最外層 div 的 data- 屬性上（見 member_modals.html 最上面的提示），
   由下面這段程式讀出來處理。開始串接第一個表單時，把這段的註解拿掉即可：
   
   注意：showMemberTab('profile') 會呼叫 loadProfile()，而 loadProfile() 目前第一行的
      setMsg(profileError, '') 會把 Controller 送來的 profileError 清掉，
      所以 loadProfile() 要先照它上面的提示改好。
      ==================================== */

     const startInfo = authModal.parentElement.dataset;   // 就是 th:fragment="modals" 那個 div

     if (startInfo.startView) {
       // 這裡不呼叫 showAuthView()，因為它會把 Controller 送來的錯誤訊息清掉
       authModal.querySelectorAll('.auth-view').forEach(view => {
         view.hidden = view.dataset.view !== startInfo.startView;
       });
       openModal(authModal);
     }
     if (startInfo.startTab) {
       openModal(memberModal);
       showMemberTab(startInfo.startTab);
     }
     if (startInfo.toast) showToast(startInfo.toast);
   
   
})();
