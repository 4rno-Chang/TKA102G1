/* =========================================================
   Bistroops 預約訂位
   對應的 HTML：templates/front/reservation/reservation_date.html（步驟一）
                templates/front/reservation/reservation_contact.html（步驟二）
                templates/front/reservation/reservation_done.html（步驟三）
   對應的 CSS ：static/css/front/reservation.css

   ※ 目前畫面用的是假資料，全部集中在「1. 假資料 ReservationDemo」這一區。
     Java 寫好後要改的地方都標了「Thymeleaf 串接」，用 Ctrl+F 搜尋這幾個字就能找到
     （這個檔案、三個 HTML、BookingPageController.java 都有）。

   上面三個頁面都會載入這個檔案。每一區開頭都會先確認「這一頁有沒有那個元素」，沒有就跳過。
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和 member.js 互相衝突
(() => {

const $ = id => document.getElementById(id);

// 各頁面的網址（目前專案沒有設定 context-path，所以直接寫）
const URLS = {
  date:    '/bistroops/reservation',
  contact: '/bistroops/reservation/contact',
  done:    '/bistroops/reservation/done'
};


/* ========== 1. 假資料 ReservationDemo ==========
   ★ 這一整區都是假資料，用來代替資料庫，讓畫面在 Java 還沒寫好之前可以操作。★

   【Thymeleaf 串接】總覽
     接上資料庫後，這一區可以整個刪掉。資料改由 Controller 查出來放進 Model，
     HTML 用 th:each／th:text 顯示；送出改成表單（th:action + method="post"）送到 Controller。
     不需要 JSON。每一個函式之後由誰負責，寫在各函式上面的註解。

   假資料的規則（可再討論）：
     可預約的日期：明天起三個月內；假設每週一公休、每月 15 日已經訂滿
     每天的時段  ：11:00、12:30、17:00、18:30 寫死
     額滿的時段  ：假設週六的 18:30、以及 6 人桌的 12:30 都滿了
     訂位編號    ：依照資料庫流水號
     示範資料    ：手機 0912345678 已經有兩筆訂位，可以直接拿來試「查詢訂位」

   【Thymeleaf 串接】對應的資料表（依照 資料庫表格建立/createTable_v1_4.sql）
     訂位時段 reservation_datetime
       一列就是「某一天的某個時段」：rsv_dt_no 時段編號、rsv_dt_datetime 日期加時間（例如 2026-08-25 17:00:00）
       所以「可預約的日期」和「每天的時段」都是從這張表查出來的：
         可預約的日期 = rsv_dt_datetime 的日期部分（今天以後、不重複），而且那一天至少還有一個時段沒滿
         某一天的時段 = rsv_dt_datetime 是那一天的所有資料列，時間部分就是畫面上的 11:00、17:00
       這張表要有資料，月曆上才有日期可以點；要開放哪幾天、哪些時段，就是往這張表新增資料。
     訂位 reservation
       mem_no 訂位的會員、rsv_dt_no 訂的是哪一個時段、seat_type_no 桌型、rsv_status 狀態（預約／實到…）、rsv_comment 備註（最多 50 字）
       這張表「沒有」姓名、電話、Email 的欄位，只記會員編號 mem_no（必填）。所以訂位的人一定要是會員，見下面的說明。
     桌型 seat_type
       seat_type_no 就是畫面上的 2／4／6；seat_type_rsv_num 開放訂位的桌數
       某個時段某個桌型是否額滿 = reservation 裡同一個 rsv_dt_no、seat_type_no 而且沒取消的筆數 ≥ seat_type_rsv_num

   【Thymeleaf 串接】★ 訂位的人怎麼變成會員編號（組內決定的做法）★
     客人不用先登入也能訂位。送出訂位時，Controller／Service 拿客人填的電話去查「會員 member」（比對 mem_tel）：
       查得到 → 用那位會員的 mem_no
       查不到 → 先新增一位會員（mem_tel 電話、mem_name 姓名、mem_mail Email），再用新會員的 mem_no
     然後才新增「訂位 reservation」，存進去的一律是會員編號。
     已經登入的客人：直接用 session 裡會員的 mem_no，不用再查。
     要留意的地方：
       1. 這樣建立的會員沒有密碼（mem_password 是空的）。會員的註冊和登入要能處理這種會員：
          註冊時「手機已存在但還沒有密碼」要改成幫他設定密碼，而不是回覆已註冊；
          登入時密碼是空的要當作登入失敗，不能直接拿來比對（會出錯）。
       2. 查得到會員時，不要用客人這次填的姓名、Email 去覆蓋會員原本的資料
          （訂位不用登入，任何人都能填別人的電話）。
       3. mem_mail 最多 30 字，所以步驟二 Email 輸入框的 maxlength 是 30。
       4. 「查詢訂位」是用電話找到會員，再查那位會員的訂位；畫面上的姓名、電話來自會員資料。
   ================================================== */
const ReservationDemo = (() => {
  // 訂位資料存在瀏覽器裡的名稱。假資料的格式有修改時，把最後的數字加 1，瀏覽器就會改用新的初始資料
  const RESV_KEY = 'bistroopsDemoResv2';

  // Date 物件 → '2026-10-03'
  function toDateText(date) {
    return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');
  }
  // '2026-10-03' → Date 物件
  function toDate(text) {
    const [y, m, d] = text.split('-').map(Number);
    return new Date(y, m - 1, d);
  }

  // 【假資料】可以預約的第一天（明天）與最後一天（三個月後）
  // 【Thymeleaf 串接】Controller 放進 Model：minDate、maxDate
  function minDate() {
    const day = new Date();
    day.setDate(day.getDate() + 1);
    return toDateText(day);
  }
  function maxDate() {
    const day = new Date();
    day.setMonth(day.getMonth() + 3);
    return toDateText(day);
  }

  // 【假資料】某個桌型可以預約的日期。回傳 Set，用 .has('2026-10-07') 判斷那一天能不能訂
  // 【Thymeleaf 串接】Controller 查「訂位時段 reservation_datetime」：取出今天以後有開放時段的日期，
  //   只留下那個桌型至少還有一個時段沒滿的，用逗號接成文字放進 Model：openDates
  function openDates(tableType) {
    const dates = new Set();
    const last = toDate(maxDate());
    for (let day = toDate(minDate()); day <= last; day.setDate(day.getDate() + 1)) {
      if (day.getDay() === 1) continue;       // 星期一公休（getDay：0 是星期日、1 是星期一）
      if (day.getDate() === 15) continue;     // 15 日已訂滿
      dates.add(toDateText(day));
    }
    return dates;
  }

  // 【假資料】某一天開放的時段
  // 【Thymeleaf 串接】Controller 查「訂位時段 reservation_datetime」裡 rsv_dt_datetime 是這一天的資料列，
  //   放進 Model：slots。每一個時段建議帶著它的編號 rsv_dt_no（送出訂位時要存的是這個編號，不是時間的文字）
  function slots(dateText) {
    return ['11:00', '12:30', '17:00', '18:30'];
  }

  // 【假資料】某一天、某個桌型已經額滿的時段
  // 【Thymeleaf 串接】Controller 算出來，放在每一個時段的 full（是否額滿）
  //   額滿 = 「訂位 reservation」裡同一個時段（rsv_dt_no）、同一個桌型（seat_type_no）而且沒取消的筆數
  //          已經達到「桌型 seat_type」的開放訂位桌數（seat_type_rsv_num）
  function fullSlots(dateText, tableType) {
    const full = [];
    if (toDate(dateText).getDay() === 6) full.push('18:30');   // 星期六
    if (Number(tableType) === 6) full.push('12:30');
    return full;
  }

  // 【假資料】訂位的清單，存在瀏覽器的 localStorage。
  //   一開始先放兩筆示範訂位（手機 0912345678），這樣不用先訂位也能試「查詢訂位」
  //   seq 是下一個訂位編號的流水號；list 裡每一筆的 status 是 'BOOKED' 已訂位／'CANCELLED' 已取消
  // 【Thymeleaf 串接】訂位改存在資料庫，這裡整段刪掉
  function loadReservations() {
    try {
      const saved = JSON.parse(localStorage.getItem(RESV_KEY));
      if (saved && Array.isArray(saved.list)) return saved;
    } catch (e) { /* 讀不到就用下面的示範資料 */ }
    const days = [...openDates(2)];   // 可以預約的日期，挑第 3 天和第 10 天當示範
    return {
      seq: 123,
      list: [
        { reservationNo: 'BR000121', date: days[2], tableType: 2, time: '12:30', name: '陳小姐', phone: '0912345678', email: '', note: '', status: 'BOOKED' },
        { reservationNo: 'BR000122', date: days[9], tableType: 4, time: '17:00', name: '陳小姐', phone: '0912345678', email: '', note: '需要兒童椅', status: 'BOOKED' }
      ]
    };
  }
  function saveReservations(data) {
    try { localStorage.setItem(RESV_KEY, JSON.stringify(data)); } catch (e) { /* 存不了就算了 */ }
  }

  // 【假資料】送出訂位：產生訂位編號，把這筆訂位記在瀏覽器裡
  // 【Thymeleaf 串接】改成表單送到 Controller（POST /bistroops/reservation/submit），
  //   由 Controller 檢查後，先用電話找出（或建立）會員拿到 mem_no（做法見第 1 區開頭的說明），
  //   再新增一筆「訂位 reservation」：mem_no 會員編號、
  //   rsv_dt_no 選到的時段編號、seat_type_no 桌型、rsv_create_time 現在時間、rsv_status 預約、rsv_comment 備註。
  //   姓名、電話、Email 不存在訂位裡（只在需要建立會員時存進「會員 member」）
  //   訂位編號就是資料庫自動產生的 rsv_no
  function createReservation(data) {
    const all = loadReservations();
    const result = Object.assign({}, data, { reservationNo: 'BR' + String(all.seq).padStart(6, '0'), status: 'BOOKED' });
    all.seq += 1;
    all.list.push(result);
    saveReservations(all);
    return result;
  }

  // 【假資料】用手機查詢訂位：今天以後的都列出來（包含已取消的），日期近的排前面
  // 【Thymeleaf 串接】Controller 先用手機查「會員 member」（mem_tel）拿到 mem_no，
  //   再查「訂位 reservation」裡那個 mem_no 今天以後的訂位，把清單放進 Model；查不到會員就是沒有訂位
  function findReservations(phone) {
    const today = toDateText(new Date());
    return loadReservations().list
      .filter(r => r.phone === phone && r.date >= today)
      .sort((a, b) => (a.date + a.time).localeCompare(b.date + b.time));
  }

  // 【假資料】取消訂位：不刪除，只把狀態改成已取消
  // 【Thymeleaf 串接】改成小表單送到 Controller（POST /bistroops/reservation/cancel）
  function cancelReservation(reservationNo) {
    const all = loadReservations();
    const target = all.list.find(r => r.reservationNo === reservationNo);
    if (target) target.status = 'CANCELLED';
    saveReservations(all);
  }

  return {
    toDateText, minDate, maxDate, openDates, slots, fullSlots,
    createReservation, findReservations, cancelReservation
  };
})();


/* ========== 2. 頁面之間暫時傳遞資料（也是假資料的一部分） ==========
   目前每一步是不同的頁面，又沒有送到後端，所以選好的日期、填好的資料先存在瀏覽器的 sessionStorage，
   下一頁再讀出來（關掉分頁就會清掉）。

   【Thymeleaf 串接】接上資料庫後這一區可以刪掉：
     步驟一 → 步驟二：改成表單用 POST 把時段編號 rsvDtNo 和桌型 tableType 送到 Controller，
                       Controller 放進 Model，步驟二再用隱藏欄位帶著
     步驟二 → 步驟三：表單送到 Controller 存進資料庫，再用 RedirectAttributes 帶到結果頁 */
const TEMP_KEYS = { draft: 'bistroopsResvDraft', done: 'bistroopsResvDone' };

function saveTemp(key, data) {
  try { sessionStorage.setItem(key, JSON.stringify(data)); } catch (e) { /* 存不了就算了 */ }
}
function loadTemp(key) {
  try { return JSON.parse(sessionStorage.getItem(key)); } catch (e) { return null; }
}
function removeTemp(key) {
  try { sessionStorage.removeItem(key); } catch (e) { /* 沒關係 */ }
}


/* ========== 3. 小工具、頁首頁尾 ==========
   這一區和資料無關，接上資料庫後保留。
   頁首頁尾的部分和首頁 main.js 的第 1、4、6 區相同：首頁的 main.js 還包含輪播、常見問題等
   只有首頁才有的程式，在這幾頁載入會出錯，所以把需要的部分另外寫在這裡。 */

// '2026-10-03' → '2026/10/03'（畫面上顯示用）
const showDate = text => text.split('-').join('/');
// 星期幾的中文，例如 '2026-10-10' → '六'
function weekdayText(dateText) {
  const [y, m, d] = dateText.split('-').map(Number);
  return '日一二三四五六'[new Date(y, m - 1, d).getDay()];
}
// 把文字放進某個元素；找不到那個元素就不做事
function setText(id, text) {
  const el = $(id);
  if (el) el.textContent = text;
}
// 把文字中的 < > & " ' 換掉，避免客人填的內容（姓名、備註）被當成 HTML 執行
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, ch => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
  ));
}

const menuToggle = $('menuToggle');
const mainNav = $('mainNav');
if (menuToggle && mainNav) {
  // 手機版的漢堡選單
  menuToggle.addEventListener('click', () => {
    const isOpen = mainNav.classList.toggle('open');
    menuToggle.classList.toggle('open', isOpen);
    menuToggle.setAttribute('aria-expanded', isOpen);
  });
}

const siteHeader = $('siteHeader');
const backToTop = $('backToTop');
window.addEventListener('scroll', () => {
  if (siteHeader) siteHeader.classList.toggle('scrolled', window.scrollY > 10);    // 捲動後頁首出現陰影
  if (backToTop) backToTop.classList.toggle('show', window.scrollY > 400);         // 捲動一段後出現「回到頂端」
});
if (backToTop) backToTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));

setText('year', new Date().getFullYear());


/* ========== 4. 訂位步驟一：選日期、桌型、時段 ==========
   【Thymeleaf 串接】接上資料庫後的做法（HTML 的寫法見 reservation_date.html）：
     選日期、換桌型 → 表單用 GET 送回同一頁，Controller 查出那一天的時段後重新顯示
     選時段        → 不用重新載入，只更新摘要
     按「下一步」   → 表單用 POST 送到 /bistroops/reservation/contact
   所以這一區會變得很短：月曆（renderCalendar）保留，但可預約的日期改成讀 HTML 上的 data-open-dates；
   產生時段的 renderSlots 可以刪掉（改由 th:each 產生）；
   點日期和換桌型的地方，改成「把日期填進隱藏欄位後呼叫 resvForm.submit()」。
   時段的選項送出的值會變成時段編號（rsvDtNo），所以選時段時，摘要要顯示的時間改成讀選項上的 data-time：
     setText('sumTime', e.target.dataset.time); */
const resvForm = $('resvForm');
if (resvForm) {
  const calendar = $('calendar');
  const calendarToggle = $('calendarToggle');
  const slotGrid = $('slotGrid');
  const slotEmpty = $('slotEmpty');
  const resvNext = $('resvNext');

  // 目前選到的內容。從步驟二按「上一步」回來時，先讀出之前選的
  const draft = loadTemp(TEMP_KEYS.draft) || {};
  const state = {
    date: draft.date || '',
    tableType: [2, 4, 6].includes(Number(draft.tableType)) ? Number(draft.tableType) : 2,
    time: draft.time || ''
  };

  const todayText = ReservationDemo.toDateText(new Date());
  // 月份用「年 × 12 + 月」換成一個數字，比較先後和加減都比較方便
  const monthIndex = text => Number(text.slice(0, 4)) * 12 + Number(text.slice(5, 7)) - 1;
  const minMonth = monthIndex(ReservationDemo.minDate());
  const maxMonth = monthIndex(ReservationDemo.maxDate());

  let openDates = ReservationDemo.openDates(state.tableType);   // 目前桌型可以預約的日期
  let showing = minMonth;                                        // 月曆目前顯示的月份
  let calendarOpen = true;                                       // 月曆是否展開

  // 之前選的日期、時段如果已經不能選了，就當作沒選
  function checkState() {
    if (state.date && !openDates.has(state.date)) state.date = '';
    if (!state.date) { state.time = ''; return; }
    const canPick = ReservationDemo.slots(state.date).includes(state.time)
      && !ReservationDemo.fullSlots(state.date, state.tableType).includes(state.time);
    if (!canPick) state.time = '';
  }

  // ---------- 月曆 ----------
  function renderCalendar() {
    const y = Math.floor(showing / 12);
    const m = showing % 12 + 1;                            // 1 到 12
    const firstWeekday = new Date(y, m - 1, 1).getDay();   // 這個月 1 號是星期幾（0 是星期日）
    const daysInMonth = new Date(y, m, 0).getDate();       // 這個月有幾天

    let cells = '<span></span>'.repeat(firstWeekday);      // 1 號前面的空格
    for (let d = 1; d <= daysInMonth; d++) {
      const text = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      const isOpen = openDates.has(text);
      const classes = ['cal-day'];
      if (isOpen) classes.push('is-open');
      if (text === state.date) classes.push('is-selected');
      if (text === todayText) classes.push('is-today');
      // 不能預約的日期加上 disabled，就點不了
      cells += `<button type="button" class="${classes.join(' ')}" data-date="${text}" ${isOpen ? '' : 'disabled'}>${d}</button>`;
    }

    calendar.innerHTML = `
      <div class="cal-head">
        <button type="button" class="cal-nav" data-cal-move="-1" aria-label="上個月" ${showing <= minMonth ? 'disabled' : ''}>‹</button>
        <span class="cal-title">${y} 年 ${m} 月</span>
        <button type="button" class="cal-nav" data-cal-move="1" aria-label="下個月" ${showing >= maxMonth ? 'disabled' : ''}>›</button>
      </div>
      <div class="cal-week"><span>日</span><span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span></div>
      <div class="cal-grid">${cells}</div>`;
  }

  // ---------- 時段 ----------
  function renderSlots() {
    // 還沒選日期：顯示提示文字，不顯示時段
    slotEmpty.hidden = Boolean(state.date);
    slotGrid.hidden = !state.date;
    if (!state.date) { slotGrid.innerHTML = ''; return; }

    const full = ReservationDemo.fullSlots(state.date, state.tableType);
    slotGrid.innerHTML = ReservationDemo.slots(state.date).map(slot => `
      <label class="slot">
        <input type="radio" name="time" value="${slot}" ${slot === state.time ? 'checked' : ''} ${full.includes(slot) ? 'disabled' : ''}>
        <span class="slot-box">
          <span class="slot-time">${slot}</span>
          <span class="slot-state">${full.includes(slot) ? '已額滿' : ''}</span>
        </span>
      </label>`).join('');
  }

  // ---------- 把目前選到的內容顯示在畫面上 ----------
  function updateView() {
    // 已選日期：顯示「日期 + 開啟日曆」那一列；沒選日期：月曆一定展開
    calendarToggle.hidden = !state.date;
    if (!state.date) calendarOpen = true;
    calendar.classList.toggle('is-collapsed', !calendarOpen);
    calendarToggle.setAttribute('aria-expanded', calendarOpen);
    calendarToggle.querySelector('.date-field-value').textContent = state.date ? showDate(state.date) : '';
    calendarToggle.querySelector('.date-field-action').textContent = calendarOpen ? '收起日曆 ▴' : '開啟日曆 ▾';

    setText('sumDate', state.date ? showDate(state.date) : '尚未選擇');
    setText('sumTable', `${state.tableType} 人桌`);
    setText('sumTime', state.time || '尚未選擇');

    resvNext.disabled = !(state.date && state.time);   // 日期和時段都選了才能按下一步
  }

  function renderAll() {
    checkState();
    renderCalendar();
    renderSlots();
    updateView();
  }

  // 點月曆
  calendar.addEventListener('click', e => {
    // 左右箭頭：換月份
    const move = e.target.closest('[data-cal-move]');
    if (move) {
      showing += Number(move.dataset.calMove);
      renderCalendar();
      return;
    }
    // 點日期：記下日期、清掉原本的時段、把月曆收起來
    const day = e.target.closest('.cal-day');
    if (day && !day.disabled) {
      state.date = day.dataset.date;
      state.time = '';
      calendarOpen = false;
      renderAll();
    }
  });

  // 「開啟日曆／收起日曆」
  calendarToggle.addEventListener('click', () => {
    calendarOpen = !calendarOpen;
    updateView();
  });

  resvForm.addEventListener('change', e => {
    // 換桌型：可預約的日期和額滿的時段都可能不同，所以整個重新產生
    if (e.target.name === 'tableType') {
      state.tableType = Number(e.target.value);
      openDates = ReservationDemo.openDates(state.tableType);
      renderAll();
    }
    // 選時段
    if (e.target.name === 'time') {
      state.time = e.target.value;
      updateView();
    }
  });

  // 下一步：把選好的內容暫存起來，前往步驟二
  resvNext.addEventListener('click', () => {
    if (!state.date || !state.time) return;
    saveTemp(TEMP_KEYS.draft, state);
    location.href = URLS.contact;
  });

  // 一開始：把之前選的桌型勾起來；有選日期就顯示那個月並把月曆收起來
  resvForm.querySelectorAll('input[name="tableType"]').forEach(input => {
    input.checked = Number(input.value) === state.tableType;
  });
  checkState();
  if (state.date) {
    showing = monthIndex(state.date);
    calendarOpen = false;
  }
  renderAll();
}


/* ========== 5. 訂位步驟二：填寫聯絡資料 ==========
   【Thymeleaf 串接】接上資料庫後：
     日期、桌型、時段由 Controller 放進 Model，HTML 用 th:text 顯示，下面讀暫存資料的幾行刪掉。
     送出改成讓表單正常送到 Controller：submit 事件裡只保留「檢查不通過就 e.preventDefault()」，
     檢查通過就什麼都不做（讓表單自己送出），createReservation 之後的幾行刪掉。

   送出前的檢查（checkContact）要保留，讓客人不用等頁面重新載入就能看到錯誤；
   但 Controller 也要用同樣的規則再檢查一次，因為瀏覽器的檢查是可以被跳過的。 */
const PHONE_RE = /^09\d{8}$/;                     // 手機：09 開頭共 10 碼
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// 沒問題回傳空字串，有問題回傳要顯示的錯誤訊息
function checkContact(form) {
  // namedItem('name') 是取出表單裡 name="name" 的那個欄位
  const name = form.elements.namedItem('name');
  const phone = form.elements.namedItem('phone');
  const email = form.elements.namedItem('email');
  const agree = form.elements.namedItem('agree');

  if (name.value.trim() === '') return '請輸入聯絡人姓名';
  if (!PHONE_RE.test(phone.value.trim())) return '請輸入正確的手機號碼（09 開頭共 10 碼）';
  if (email.value.trim() !== '' && !EMAIL_RE.test(email.value.trim())) return '電子郵件格式不正確';
  if (!agree.checked) return '請先勾選「我已閱讀訂位須知及個人資料使用說明」';
  return '';
}

// 摘要跟著輸入的內容更新：輸入框上寫 data-summary="sumName"，內容就會同步顯示到 id 是 sumName 的元素
document.querySelectorAll('[data-summary]').forEach(input => {
  const show = () => setText(input.dataset.summary, input.value.trim());
  input.addEventListener('input', show);
  show();   // 一進來先顯示一次（有登入會員時，輸入框已經有預設值）
});

const contactForm = $('contactForm');
if (contactForm) {
  const draft = loadTemp(TEMP_KEYS.draft);
  if (!draft || !draft.date || !draft.time) {
    // 沒有選好的日期（例如直接打網址進來）：回到步驟一
    location.replace(URLS.date);
  } else {
    setText('sumDate', showDate(draft.date));
    setText('sumTable', `${draft.tableType} 人桌`);
    setText('sumTime', draft.time);

    contactForm.addEventListener('submit', e => {
      e.preventDefault();
      const message = checkContact(contactForm);
      const errorBox = $('contactError');
      errorBox.textContent = message;
      errorBox.hidden = message === '';
      if (message !== '') return;

      const result = ReservationDemo.createReservation({
        date: draft.date,
        tableType: draft.tableType,
        time: draft.time,
        name: contactForm.elements.namedItem('name').value.trim(),
        phone: contactForm.elements.namedItem('phone').value.trim(),
        email: contactForm.elements.namedItem('email').value.trim(),
        note: contactForm.elements.namedItem('note').value.trim()
      });
      saveTemp(TEMP_KEYS.done, result);
      removeTemp(TEMP_KEYS.draft);   // 訂位完成，之前選的內容不用留了
      location.href = URLS.done;
    });
  }
}


/* ========== 6. 訂位步驟三：訂位成功 ==========
   【Thymeleaf 串接】接上資料庫後這一區整個刪掉，HTML 改用 th:text 顯示 Controller 帶過來的資料。 */
const resvDone = $('resvDone');
if (resvDone) {
  const done = loadTemp(TEMP_KEYS.done);
  if (!done) {
    location.replace(URLS.date);
  } else {
    setText('doneNo', done.reservationNo);
    setText('doneDate', showDate(done.date));
    setText('doneTable', `${done.tableType} 人桌`);
    setText('doneTime', done.time);
    setText('doneName', done.name);
    setText('donePhone', done.phone);
    setText('doneEmail', done.email || '未填寫');
    setText('doneNote', done.note || '無');
  }
}


/* ========== 7. 查詢訂位（訂位步驟一的頁面） ==========
   輸入手機 → 列出那支手機今天以後的所有訂位 → 每一筆可以取消，但不能修改。

   【Thymeleaf 串接】接上資料庫後（不用 JSON 的做法，HTML 的寫法見 reservation_date.html）：
     查詢：表單用 GET 把手機送回同一頁（/bistroops/reservation?lookupPhone=09...），
           Controller 查出訂位放進 Model，頁面重新載入後對話框直接是打開的、清單用 th:each 產生。
     取消：每一筆的「取消訂位」是一個小表單，POST 到 /bistroops/reservation/cancel，
           Controller 取消後 redirect 回 /bistroops/reservation?lookupPhone=同一支手機。
   改完後，這一區只需要留下對話框的開關，和「取消前跳確認視窗」：
     list.addEventListener('submit', e => { if (!confirm('確定要取消這筆訂位嗎？')) e.preventDefault(); });
   renderResvList、送出查詢時呼叫 ReservationDemo 的部分都刪掉。 */
const resvLookup = $('resvLookup');
if (resvLookup) {
  const form = $('resvLookupForm');
  const list = $('resvLookupList');
  const error = $('resvLookupError');
  let searchedPhone = '';   // 目前清單顯示的是哪一支手機的訂位

  function renderResvList() {
    const found = ReservationDemo.findReservations(searchedPhone);
    if (found.length === 0) {
      list.innerHTML = '<p class="lookup-empty">查無這支手機的訂位紀錄。</p>';
      return;
    }
    list.innerHTML = found.map(r => {
      const cancelled = r.status === 'CANCELLED';
      return `
        <article class="lookup-item${cancelled ? ' is-cancelled' : ''}">
          <div class="lookup-item-main">
            <p class="lookup-item-title">${showDate(r.date)}（${weekdayText(r.date)}）${escapeHtml(r.time)}</p>
            <p class="lookup-item-info">${escapeHtml(r.tableType)} 人桌　訂位編號 ${escapeHtml(r.reservationNo)}　${escapeHtml(r.name)}</p>
            ${r.note ? `<p class="lookup-item-info">備註：${escapeHtml(r.note)}</p>` : ''}
          </div>
          ${cancelled
            ? '<span class="lookup-tag">已取消</span>'
            : `<button type="button" class="btn-small danger" data-cancel-resv="${escapeHtml(r.reservationNo)}">取消訂位</button>`}
        </article>`;
    }).join('');
  }

  form.addEventListener('submit', e => {
    e.preventDefault();
    const phone = form.elements.namedItem('phone').value.trim();
    const ok = PHONE_RE.test(phone);
    error.textContent = ok ? '' : '請輸入正確的手機號碼（09 開頭共 10 碼）';
    error.hidden = ok;
    if (!ok) { list.innerHTML = ''; return; }
    searchedPhone = phone;
    renderResvList();
  });

  // 取消訂位：先確認，再把那一筆標記成已取消
  list.addEventListener('click', e => {
    const btn = e.target.closest('[data-cancel-resv]');
    if (!btn) return;
    if (!confirm('確定要取消這筆訂位嗎？取消後無法復原。')) return;
    ReservationDemo.cancelReservation(btn.dataset.cancelResv);
    renderResvList();
  });

  // ---------- 對話框的開關（和資料無關，接上資料庫後保留） ----------
  //   按鈕上寫 data-open-lookup="resvLookup" → 點了打開對話框
  //   對話框裡寫 data-lookup-close 的元素（背景、右上角的 ×）→ 點了關閉；按 Esc 也會關閉
  function openLookup() {
    resvLookup.hidden = false;
    document.body.classList.add('modal-open');   // 對話框打開時，後面的頁面不能捲動（樣式在 member.css）
    form.elements.namedItem('phone').focus();
  }
  function closeLookup() {
    if (resvLookup.hidden) return;
    resvLookup.hidden = true;
    document.body.classList.remove('modal-open');
  }
  document.addEventListener('click', e => {
    const opener = e.target.closest('[data-open-lookup]');
    if (opener && opener.dataset.openLookup === resvLookup.id) openLookup();
  });
  resvLookup.addEventListener('click', e => {
    if (e.target.closest('[data-lookup-close]')) closeLookup();
  });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeLookup();
  });
}

})();
