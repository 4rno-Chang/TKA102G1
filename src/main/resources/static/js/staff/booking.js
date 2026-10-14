/* =========================================================
   Bistroops 員工後台：訂候位（訂位、候位兩個分頁）
   對應的 HTML：templates/staff/booking.html
   對應的 CSS ：static/css/staff/booking.css
   要先載入 staff.js（用到裡面的 escapeHtml、staffToast，以及「送出前先確認」）

   這個檔案分成兩區：
     A. 資料庫的畫面也會用到的程式（保留）：送出前先確認、自製的月曆
     B. 假資料：只控制 booking.html 裡標示【假資料】的部分（訂位的列表和對話框、候位的列表、叫號和對話框）

   【Thymeleaf 串接】資料庫的版本確認沒問題後：
     1. 刪掉 booking.html 裡用 ▼▼▼ 和 ▲▲▲ 框起來的 5 段【假資料】
     2. 刪掉這個檔案的 B 區（下面用 ▼▼▼ 和 ▲▲▲ 框起來的範圍；最後一行的 })(); 要留著）
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和其他檔案互相衝突
(() => {


/* =====================================================================
   A. 資料庫的畫面（booking.html 的【資料庫資料】）也會用到的程式 ── 保留
     A-1. 送出前先確認（取消訂位、過號）──★ 程式不在這裡，在 staff.js
     A-2. 日期選擇：自製的月曆（查詢的日期、新增／修改訂位的日期）
   資料庫版的對話框（新增／修改訂位、新增／修改候位）都是「連結到 Controller、重新載入頁面」打開的，這裡不需要寫程式。
   ===================================================================== */

/* ---------- A-1. 送出前先確認 ----------
   ★ 這個功能的程式在 staff.js（A-6. 送出前先確認），後台每一頁都會載入它，所以這裡「不用、也不能」再寫一次：
     HTML 的表單寫了 data-confirm="要問的話"，按下去送出之前 staff.js 就會跳出確認的視窗，按「取消」就不送出。
     這裡如果再寫一段一樣的程式，同一次送出會被問兩次。
   目前「取消訂位」和候位的「過號」有用到。之後別的表單也想先確認再送出，在 <form> 加上 data-confirm 就可以了，JS 不用改。 */


/* ---------- A-2. 日期選擇：自製的月曆 ----------
   不用瀏覽器內建的日期欄位（每個瀏覽器長得不一樣），改成自己畫的月曆，樣子和活動管理的月曆相同。
   月曆是「跳出來浮在畫面上」的小視窗：按日期的按鈕就出現在按鈕旁邊，選了日期、點到別的地方、捲動畫面時會自己關掉。
   ★ 月曆只能選年月日，沒有時間。訂位的時段是另外用一排可以點的時段來選（booking.html 的 .booking-slots）。

   HTML 的寫法（資料庫和假資料都一樣）：
     <div class="booking-date" data-date-picker>              ← 一個日期欄位
       <input type="hidden" name="date" value="2026-10-14">    ← 真正送出去的值，格式是 年-月-日
       <button type="button" class="booking-date-btn" data-date-toggle><span data-date-text></span>…</button>
       <div class="booking-date-panel" data-date-panel hidden></div>   ← 月曆，內容和位置由這裡決定
     </div>
   外層的 div 可以加的設定：
     data-min="today"             今天以前的日子不能選
     data-date-submit="按鈕的id"  選了日期之後，自動按下那顆按鈕（把表單送出去）
     data-date-clear="欄位的id"   選了日期之後，先把那個欄位清空
   選了日期之後，月曆所在的那個 div 會發出一個叫 datepick 的事件，想在選日期之後做別的事可以聽這個事件。 */
const WEEKDAYS = '日一二三四五六';

// Date 物件 → '2026-10-14'
function toDateText(date) {
  const two = n => String(n).padStart(2, '0');   // 個位數前面補 0
  return date.getFullYear() + '-' + two(date.getMonth() + 1) + '-' + two(date.getDate());
}
// 今天，格式 '2026-10-14'
function todayText() {
  return toDateText(new Date());
}
// '2026-10-14' → '2026/10/14（三）'
function dateLabel(dateText) {
  const [y, m, d] = dateText.split('-').map(Number);
  return dateText.split('-').join('/') + '（' + WEEKDAYS[new Date(y, m - 1, d).getDay()] + '）';
}

// 一個日期欄位裡的隱藏欄位（真正送出去的值放在這裡）
function dateInputOf(picker) {
  return picker.querySelector('input[type="hidden"]');
}
// 這個日期欄位最早可以選哪一天；沒有限制回傳空字串
function minDateOf(picker) {
  return picker.dataset.min === 'today' ? todayText() : '';
}

// 依照隱藏欄位的值，更新按鈕上的文字
function refreshDatePicker(picker) {
  const value = dateInputOf(picker).value;
  picker.querySelector('[data-date-text]').textContent = value ? dateLabel(value) : '請選擇日期';
  picker.classList.toggle('is-empty', !value);
}

// 用程式替某個日期欄位設定日期（例如打開對話框帶入原本的日期），並更新按鈕上的文字
function setPickerDate(picker, dateText) {
  dateInputOf(picker).value = dateText || '';
  refreshDatePicker(picker);
}

function closeDatePanel(picker) {
  picker.querySelector('[data-date-panel]').hidden = true;
  picker.classList.remove('is-open');
}
function closeAllDatePanels() {
  document.querySelectorAll('[data-date-picker].is-open').forEach(closeDatePanel);
}

// 決定跳出來的月曆要出現在畫面的哪裡：預設貼在日期按鈕的下面，下面放不下就改放上面。
// 月曆是 position: fixed（見 booking.css），所以位置是用「離畫面上緣、左緣多遠」來指定
function placeDatePanel(picker) {
  const panel = picker.querySelector('[data-date-panel]');
  // getBoundingClientRect() 會告訴我們按鈕現在在畫面上的位置（top、bottom、left 都是離畫面邊緣的距離）
  const button = picker.querySelector('[data-date-toggle]').getBoundingClientRect();
  const GAP = 6;       // 月曆和按鈕之間的距離
  const MARGIN = 10;   // 月曆離畫面邊緣至少要留多少

  let top = button.bottom + GAP;
  const fitsBelow = top + panel.offsetHeight <= window.innerHeight - MARGIN;
  const topIfAbove = button.top - GAP - panel.offsetHeight;
  if (!fitsBelow && topIfAbove >= MARGIN) top = topIfAbove;
  top = Math.max(MARGIN, Math.min(top, window.innerHeight - panel.offsetHeight - MARGIN));

  let left = Math.min(button.left, window.innerWidth - panel.offsetWidth - MARGIN);
  left = Math.max(MARGIN, left);

  panel.style.top = top + 'px';
  panel.style.left = left + 'px';
}

// 畫出某一年某一月的月曆。month 是 0～11（JavaScript 的月份從 0 開始）
function drawDatePanel(picker, year, month) {
  const panel = picker.querySelector('[data-date-panel]');
  const value = dateInputOf(picker).value;
  const min = minDateOf(picker);
  const today = todayText();

  const firstWeekday = new Date(year, month, 1).getDay();      // 這個月 1 號是星期幾（0 是星期日）
  const daysInMonth = new Date(year, month + 1, 0).getDate();   // 這個月有幾天（下個月的第 0 天＝這個月最後一天）

  let days = '';
  for (let i = 0; i < firstWeekday; i++) days += '<span></span>';   // 1 號前面的空格
  for (let d = 1; d <= daysInMonth; d++) {
    const dateText = toDateText(new Date(year, month, d));
    const classes = ['booking-cal-day'];
    if (dateText === today) classes.push('is-today');
    if (dateText === value) classes.push('is-selected');
    // 有設定最早的日期時，比它早的日子不能按
    days += `<button type="button" class="${classes.join(' ')}" data-date-day="${dateText}"${min && dateText < min ? ' disabled' : ''}>${d}</button>`;
  }

  // 有限制最早的日期、而且上一個月的最後一天比它還早：上個月沒有可以選的日子，「上個月」不能按
  const prevDisabled = Boolean(min) && toDateText(new Date(year, month, 0)) < min;

  panel.dataset.year = year;
  panel.dataset.month = month;
  panel.innerHTML = `
    <div class="booking-cal-head">
      <button type="button" class="booking-cal-nav" data-date-nav="-1" aria-label="上個月"${prevDisabled ? ' disabled' : ''}>‹</button>
      <span class="booking-cal-title">${year} 年 ${month + 1} 月</span>
      <button type="button" class="booking-cal-nav" data-date-nav="1" aria-label="下個月">›</button>
    </div>
    <div class="booking-cal-week">${WEEKDAYS.split('').map(w => `<span>${w}</span>`).join('')}</div>
    <div class="booking-cal-days">${days}</div>
    <div class="booking-cal-foot">
      <button type="button" class="booking-cal-today" data-date-day="${today}">今天</button>
      ${min ? '<span class="booking-cal-hint">不能選今天以前的日期</span>' : ''}
    </div>`;

  // 月曆開著的時候（例如按了上個月、下個月），每個月的列數不一樣、高度會變，所以重新算一次位置
  if (!panel.hidden) placeDatePanel(picker);
}

// 打開月曆：顯示「已經選的那個月」；還沒選就顯示這個月
function openDatePanel(picker) {
  document.querySelectorAll('[data-date-picker]').forEach(other => { if (other !== picker) closeDatePanel(other); });
  const value = dateInputOf(picker).value || todayText();
  const [y, m] = value.split('-').map(Number);
  // 先讓月曆出現，再畫內容：要先出現，瀏覽器才量得到它的高度，位置才算得出來
  picker.querySelector('[data-date-panel]').hidden = false;
  picker.classList.add('is-open');
  drawDatePanel(picker, y, m - 1);
}

document.addEventListener('click', e => {
  // (1) 按日期的按鈕：打開或關掉月曆
  const toggle = e.target.closest('[data-date-toggle]');
  if (toggle) {
    const picker = toggle.closest('[data-date-picker]');
    if (picker.querySelector('[data-date-panel]').hidden) openDatePanel(picker);
    else closeDatePanel(picker);
    return;
  }

  // (2) 按「上個月」「下個月」
  const nav = e.target.closest('[data-date-nav]');
  if (nav) {
    const picker = nav.closest('[data-date-picker]');
    const panel = picker.querySelector('[data-date-panel]');
    // new Date 會自動處理跨年：12 月的下個月會變成隔年 1 月
    const target = new Date(Number(panel.dataset.year), Number(panel.dataset.month) + Number(nav.dataset.dateNav), 1);
    drawDatePanel(picker, target.getFullYear(), target.getMonth());
    return;
  }

  // (3) 選了某一天（或按月曆下面的「今天」）：把日期放進隱藏欄位、關掉月曆
  const day = e.target.closest('[data-date-day]');
  if (day) {
    const picker = day.closest('[data-date-picker]');
    setPickerDate(picker, day.dataset.dateDay);
    closeDatePanel(picker);

    // 告訴別人「這個日期欄位選好日期了」（假資料的 B 區會聽這個事件，重新查那一天的訂位）
    picker.dispatchEvent(new CustomEvent('datepick'));

    // HTML 有設定 data-date-clear：先把那個欄位清空（查詢表單用：選了日期就不要再用搜尋的文字）
    const clearTarget = picker.dataset.dateClear && document.getElementById(picker.dataset.dateClear);
    if (clearTarget) clearTarget.value = '';

    // HTML 有設定 data-date-submit：自動按下那顆按鈕，把表單送出去（重新向 Controller 要那一天的資料）。
    // requestSubmit(按鈕) 的效果和使用者自己按那顆按鈕一樣，會照按鈕上的 formaction、formmethod 送出；
    // 太舊的瀏覽器沒有 requestSubmit，就改用 click()
    const submitBtn = picker.dataset.dateSubmit && document.getElementById(picker.dataset.dateSubmit);
    if (submitBtn && submitBtn.form) {
      if (submitBtn.form.requestSubmit) submitBtn.form.requestSubmit(submitBtn);
      else submitBtn.click();
    }
    return;
  }

  // (4) 點到月曆以外的地方：把打開的月曆關掉
  if (!e.target.closest('[data-date-picker]')) closeAllDatePanels();
});

// 月曆是固定浮在畫面上的，按鈕的位置一變（捲動、平板轉向、視窗大小改變）就會和按鈕對不上，所以這些時候直接關掉。
// 第三個參數 true：頁面裡面「可以自己捲動的區塊」（例如對話框的內容）捲動時也收得到
window.addEventListener('scroll', closeAllDatePanels, true);
window.addEventListener('resize', closeAllDatePanels);
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') closeAllDatePanels();
});

// 頁面載入時：Thymeleaf 已經把日期放進隱藏欄位了，先把每個日期按鈕上的文字顯示出來
document.querySelectorAll('[data-date-picker]').forEach(refreshDatePicker);


// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
// ▼▼▼  【假資料・刪除範圍 開始】                                          ▼▼▼
// ▼▼▼   串接完成、確認資料庫的畫面沒問題後，從這一行開始刪，               ▼▼▼
// ▼▼▼   一直刪到下面 ▲▲▲ 框起來的「刪除範圍 結束」那一行（標記也一起刪掉）▼▼▼
// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
/* =====================================================================
   B. 假資料 ── 資料庫的版本確認沒問題後，從上面的 ▼▼▼ 到下面的 ▲▲▲ 都可以刪掉
      （最後一行的 })(); 要留著）
     B-1. 假資料 BookingDemo，以及兩個分頁都會用到的小工具
     B-2. 訂位：查詢（日期、午餐／晚餐、搜尋）、列表、新增／修改的對話框、取消訂位、刷新頁面
     B-3. 候位：左邊今天的候位列表（依桌型篩選、過號、編輯）、右邊每種桌型的叫號（叫下一號）、新增／修改的對話框、刷新頁面
   訂位和候位在同一個頁面的不同分頁，一次只會顯示一個，所以用「頁面上有沒有那個元素」決定要執行哪一段。

   兩個分頁共同的規定（資料庫的版本也一樣，寫在 booking.html 開頭和 StaffBookingController.java）：
     ・狀態是「取消」的訂位、候位不顯示（資料還在，只是不列出來）
     ・入座不在這一頁處理：訂位的「實到」、候位的「已入座」由負責入座管理的組員設定，這一頁選不到這兩個狀態
   ===================================================================== */
const demoRsvList = document.getElementById('demoRsvList');           // 訂位分頁：放假資料列表的地方
const demoWaitingList = document.getElementById('demoWaitingList');   // 候位分頁：放假資料列表的地方
// 假資料那幾塊已經從 booking.html 刪掉的話，下面的程式就不用執行了
if (!demoRsvList && !demoWaitingList) return;


/* ========== B-1. 假資料 BookingDemo ==========
   用來代替資料庫的訂位、候位、桌型三張表。資料暫存在瀏覽器的 localStorage（關掉分頁再打開還在，但只存在這台裝置）。
   ★ 示範資料是「以今天為準」產生的（今天有幾筆訂位、幾組候位）。換了一天再打開，會自動重新產生當天的示範資料，
     前一天自己新增、修改的假資料會不見。接上資料庫之後就沒有這件事了。

   一筆訂位（欄位名稱盡量和 VO 相同；資料庫裡姓名、電話在會員表，時段在訂位時段表，這裡為了簡單放在一起）：
     rsvNo        訂位編號（新增時自動往下編）
     memName      姓名            memTel   電話
     rsvDate      日期 '2026-10-14'        rsvTime  時段 '17:00'（只會是 RSV_TIMES 裡的那四個）
     seatTypeNo   桌型（2、4、6 人桌）
     rsvStatus    狀態：預約／未到／取消／實到（★ 範例，資料庫的狀態由組員決定）。
                  「取消訂位」就是把它改成「取消」，資料不會被刪掉，只是列表不再顯示；「實到」由入座管理設定，這一頁改不了
     rsvComment   備註

   一筆候位：
     waitingNo        候位編號（新增時自動往下編）
     waitingTicketSeq ★ 候位號碼的數字：今天這個桌型的第幾號，每種桌型各自從 1 開始（對應候位表的 waiting_ticket_seq）。
                      畫面上顯示的號碼是「字母＋兩位數」：2 人桌 A01、4 人桌 B01、6 人桌 C01（組法在 B-3 的 ticketLabel）
     seatTypeNo       桌型
     waitingName      姓名            waitingTel  電話（列表上不顯示，編輯的對話框裡才看得到）
     waitingComment   備註
     waitingStatus    狀態：候位中／已通知／已入座／取消／過號（和組員 WaitingService 定義的相同）
     waitingNotifyTime  通知時間 '18:20'，還沒通知是空字串
     notifiedAt       通知的時間點（毫秒），用來算「10 分鐘還剩多久」；資料庫的版本用 waitingNotifyTime 算就可以

   pending：每種桌型目前可以使用的桌數，例如 { 2: 3, 4: 1, 6: 0 }。這一頁只顯示、不會改它（入座管理才會改）。
            ★ Redis：資料庫的版本之後從 Redis 拿 */
const SEAT_TYPES = [2, 4, 6];   // 有哪些桌型（幾人桌）
const NOTIFY_MINUTES = 10;      // 通知之後要在幾分鐘內回來

/* 餐期和時段（★ 尚可再討論：營業時間先當作 11:00～14:00 和 17:00～21:00）
   RSV_TIMES     可以訂的時段，先固定這四個。要和 booking.html 資料庫版對話框裡寫死的時段相同
   MEAL_SPLIT    午餐和晚餐的分界：14:00（午餐結束）和 17:00（晚餐開始）的中間。
                 時段在這個時間以前算午餐，這個時間（含）以後算晚餐；
                 剛打開頁面時，現在的時間在它以前就先看午餐，之後就先看晚餐。
   文字的格式都是「時:分」各兩個數字，所以直接比文字的大小就等於比時間的早晚（'12:30' < '15:30'）。 */
const RSV_TIMES = ['11:00', '12:30', '17:00', '17:30'];
const MEAL_SPLIT = '15:30';
const MEAL_NAMES = { lunch: '午餐', dinner: '晚餐' };

// 一個時間（'12:30'）算哪一個餐期：回傳 'lunch' 或 'dinner'
function mealOf(time) {
  return time < MEAL_SPLIT ? 'lunch' : 'dinner';
}

// （toDateText、todayText 這兩個日期的小工具寫在 A-2，月曆也要用）
// 離今天幾天的日期：dayText(1) 是明天，dayText(-1) 是昨天
function dayText(offset) {
  const date = new Date();
  date.setDate(date.getDate() + offset);
  return toDateText(date);
}
// 時間，格式 '18:05'
function timeText(date) {
  const two = n => String(n).padStart(2, '0');
  return two(date.getHours()) + ':' + two(date.getMinutes());
}

const BookingDemo = (() => {
  // 示範資料的內容有修改時，把最後的數字加 1，瀏覽器就會改用新的示範資料（舊的暫存不再使用）
  const STORAGE_KEY = 'bistroopsStaffDemoBooking3';

  // 一開始的示範資料（訂位的時段只用 RSV_TIMES 裡的那四個）
  function defaultData() {
    const now = Date.now();
    const minutesAgo = n => now - n * 60000;                 // n 分鐘前的時間點
    const clock = ms => timeText(new Date(ms));              // 時間點 → '18:05'
    return {
      seedDay: todayText(),   // 這份示範資料是哪一天產生的
      reservations: [
        { rsvNo: 1, memName: '林志豪', memTel: '0911222333', rsvDate: dayText(-2), rsvTime: '17:30', seatTypeNo: 6, rsvStatus: '實到', rsvComment: '慶生聚餐' },
        { rsvNo: 2, memName: '陳美玲', memTel: '0922333444', rsvDate: dayText(-1), rsvTime: '12:30', seatTypeNo: 4, rsvStatus: '實到', rsvComment: '' },
        { rsvNo: 3, memName: '黃國華', memTel: '0933444555', rsvDate: dayText(-1), rsvTime: '17:00', seatTypeNo: 2, rsvStatus: '未到', rsvComment: '' },
        { rsvNo: 4, memName: '王小明', memTel: '0912345678', rsvDate: dayText(0), rsvTime: '11:00', seatTypeNo: 2, rsvStatus: '實到', rsvComment: '靠窗座位' },
        { rsvNo: 5, memName: '張雅婷', memTel: '0955666777', rsvDate: dayText(0), rsvTime: '11:00', seatTypeNo: 4, rsvStatus: '預約', rsvComment: '需要兒童椅' },
        { rsvNo: 6, memName: '李大同', memTel: '0966777888', rsvDate: dayText(0), rsvTime: '12:30', seatTypeNo: 4, rsvStatus: '預約', rsvComment: '' },
        { rsvNo: 7, memName: '周杰', memTel: '0988999000', rsvDate: dayText(0), rsvTime: '12:30', seatTypeNo: 2, rsvStatus: '取消', rsvComment: '臨時有事' },
        { rsvNo: 8, memName: '吳淑芬', memTel: '0977888999', rsvDate: dayText(0), rsvTime: '17:00', seatTypeNo: 6, rsvStatus: '預約', rsvComment: '6 人家庭聚餐' },
        { rsvNo: 9, memName: '陳美玲', memTel: '0922333444', rsvDate: dayText(0), rsvTime: '17:30', seatTypeNo: 2, rsvStatus: '預約', rsvComment: '' },
        { rsvNo: 10, memName: '鄭家豪', memTel: '0930303030', rsvDate: dayText(0), rsvTime: '17:30', seatTypeNo: 4, rsvStatus: '預約', rsvComment: '素食一位' },
        { rsvNo: 11, memName: '王小明', memTel: '0912345678', rsvDate: dayText(1), rsvTime: '17:00', seatTypeNo: 4, rsvStatus: '預約', rsvComment: '' },
        { rsvNo: 12, memName: '許文雄', memTel: '0910101010', rsvDate: dayText(3), rsvTime: '12:30', seatTypeNo: 6, rsvStatus: '預約', rsvComment: '公司聚餐' }
      ],
      waitings: [
        { waitingNo: 1, waitingTicketSeq: 9, seatTypeNo: 2, waitingName: '蔡依婷', waitingTel: '0921000111', waitingComment: '', waitingStatus: '已入座', waitingNotifyTime: clock(minutesAgo(40)), notifiedAt: minutesAgo(40) },
        { waitingNo: 2, waitingTicketSeq: 10, seatTypeNo: 2, waitingName: '劉建宏', waitingTel: '0921000222', waitingComment: '', waitingStatus: '過號', waitingNotifyTime: clock(minutesAgo(30)), notifiedAt: minutesAgo(30) },
        { waitingNo: 3, waitingTicketSeq: 11, seatTypeNo: 2, waitingName: '鄭雅文', waitingTel: '0921000333', waitingComment: '靠窗', waitingStatus: '已通知', waitingNotifyTime: clock(minutesAgo(12)), notifiedAt: minutesAgo(12) },
        { waitingNo: 4, waitingTicketSeq: 12, seatTypeNo: 2, waitingName: '謝明哲', waitingTel: '0921000444', waitingComment: '', waitingStatus: '已通知', waitingNotifyTime: clock(minutesAgo(3)), notifiedAt: minutesAgo(3) },
        { waitingNo: 5, waitingTicketSeq: 13, seatTypeNo: 2, waitingName: '洪淑惠', waitingTel: '0921000555', waitingComment: '', waitingStatus: '候位中', waitingNotifyTime: '', notifiedAt: null },
        { waitingNo: 6, waitingTicketSeq: 14, seatTypeNo: 2, waitingName: '郭志明', waitingTel: '0921000666', waitingComment: '', waitingStatus: '候位中', waitingNotifyTime: '', notifiedAt: null },
        { waitingNo: 7, waitingTicketSeq: 5, seatTypeNo: 4, waitingName: '曾美玉', waitingTel: '0921000777', waitingComment: '兒童椅', waitingStatus: '已通知', waitingNotifyTime: clock(minutesAgo(5)), notifiedAt: minutesAgo(5) },
        { waitingNo: 8, waitingTicketSeq: 6, seatTypeNo: 4, waitingName: '廖俊傑', waitingTel: '0921000888', waitingComment: '', waitingStatus: '候位中', waitingNotifyTime: '', notifiedAt: null },
        { waitingNo: 9, waitingTicketSeq: 7, seatTypeNo: 4, waitingName: '賴怡君', waitingTel: '0921000999', waitingComment: '', waitingStatus: '取消', waitingNotifyTime: '', notifiedAt: null },
        { waitingNo: 10, waitingTicketSeq: 3, seatTypeNo: 6, waitingName: '徐家豪', waitingTel: '0921001000', waitingComment: '慶生', waitingStatus: '候位中', waitingNotifyTime: '', notifiedAt: null }
      ],
      pending: { 2: 2, 4: 1, 6: 0 }
    };
  }

  // 從瀏覽器讀出目前的資料；沒有存過、讀不到、或不是今天產生的，就用今天的示範資料
  function load() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
      if (saved && saved.seedDay === todayText()) return saved;
    } catch (e) { /* 讀不到就用下面的示範資料 */ }
    return defaultData();
  }

  // 存進瀏覽器。存不進去（例如瀏覽器不給存）也不讓頁面壞掉，只是刷新頁面後會回到示範資料
  function save(data) {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(data)); } catch (e) { /* 存不了就算了 */ }
  }

  return { load, save };
})();

// 這一頁目前的假資料。改了之後要呼叫 saveDemo() 存起來
let demo = BookingDemo.load();
function saveDemo() {
  BookingDemo.save(demo);
}
// 「刷新頁面」用：重新從瀏覽器讀一次資料。同一台裝置開了兩個分頁時，另一個分頁新增、取消的才看得到。
// 資料庫的版本：「刷新頁面」是一個連結，重新載入頁面、Controller 重新向資料庫查一次
function reloadDemo() {
  demo = BookingDemo.load();
}

// 網址上的參數。例如網址是 /staff/booking?date=2026-10-20，urlParams.get('date') 就是 '2026-10-20'
const urlParams = new URLSearchParams(location.search);

// 檢查電話是不是 10 個數字
function isTel(text) {
  return /^[0-9]{10}$/.test(text);
}

// 一組清單裡最大的數字再加 1（清單是空的就回傳 1），用來編新的編號
function nextNumber(numbers) {
  return numbers.reduce((max, n) => Math.max(max, n), 0) + 1;
}

// 把狀態的下拉選單換成指定的那幾個選項，並選好目前的狀態。
// fixed 是「這一頁不能設定、但這筆資料已經是」的狀態（實到、已入座）：資料目前是它的話，多一個選起來的選項，
// 這樣只改別的欄位的時候，存檔才不會把狀態改掉
function fillStatusOptions(select, options, current, fixed) {
  let html = options.map(o => `<option value="${o}">${o}</option>`).join('');
  if (current === fixed) html += `<option value="${fixed}">${fixed}（由入座管理設定）</option>`;
  select.innerHTML = html;
  select.value = current;
}


/* ========== B-2. 訂位（假資料） ==========
   資料庫的版本：Controller 依照網址上的 date、meal、keyword 去資料庫查，列表用 th:each 產生
   （/staff/booking?date=2026-10-20&meal=dinner，或 /staff/booking?keyword=王）。

   查詢的規則（Service 要做的事和下面的 searchReservations 一樣，可以對照著寫）：
     狀態是「取消」的一律不顯示
     有搜尋文字 → 找「所有日期」裡姓名有這些字、或電話有這些數字的訂位（可以查到過往的；不看日期和餐期）
     沒有       → 找指定日期、指定餐期的訂位（沒有指定就是今天、依現在的時間決定午餐或晚餐）
     結果都依日期、時段排序（早的在前面）
   畫面這邊照後端的做法：打字的時候不會查，按了「查詢」才會查；在月曆上選日期、按「午餐」「晚餐」則是馬上查
   （資料庫的版本選了日期會自動送出表單、午餐晚餐是連結，效果相同）。 */
function startDemoRsv() {
  const dateInput = document.getElementById('demoRsvDate');            // 查詢的日期（月曆裡的隱藏欄位）
  const datePicker = document.getElementById('demoRsvDatePicker');     // 查詢的日期那個月曆
  const mealBox = document.getElementById('demoRsvMeals');             // 「午餐」「晚餐」兩顆按鈕
  const keywordInput = document.getElementById('demoRsvKeyword');
  const summary = document.getElementById('demoRsvSummary');

  const DATE_FORMAT = /^\d{4}-\d{2}-\d{2}$/;   // 日期的格式：四個數字-兩個數字-兩個數字

  // 目前的查詢條件（資料庫的版本，這三個是放在網址上的 date、meal、keyword）
  // 日期：網址有帶就用網址的，沒有就今天
  // 餐期：網址有帶就用網址的，沒有就看現在的時間比較接近午餐還是晚餐（15:30 以前是午餐）
  let currentDate = DATE_FORMAT.test(urlParams.get('date') || '') ? urlParams.get('date') : todayText();
  let currentMeal = MEAL_NAMES[urlParams.get('meal')] ? urlParams.get('meal') : mealOf(timeText(new Date()));
  let currentKeyword = '';

  // '2026-10-14' → '10/14'
  function shortDate(dateText) {
    return dateText.slice(5).replace('-', '/');
  }

  // 狀態標籤的顏色：預約是黃色、實到是綠色、其他（未到）是灰色
  function rsvStatusClass(status) {
    if (status === '預約') return 'tag-yellow';
    if (status === '實到') return 'tag-green';
    return 'tag-gray';
  }

  // 模擬後端的查詢。回傳符合條件的訂位（已經排好順序）
  function searchReservations(date, meal, keyword) {
    const word = keyword.trim().toLowerCase();
    const matched = demo.reservations.filter(r => {
      if (r.rsvStatus === '取消') return false;                                              // 取消的不顯示
      if (word) return r.memName.toLowerCase().includes(word) || r.memTel.includes(word);   // 有搜尋文字：不看日期和餐期
      return r.rsvDate === date && mealOf(r.rsvTime) === meal;
    });
    // 文字的格式都一樣（年-月-日、時:分），所以直接比文字的大小就等於比先後
    return matched.sort((a, b) => (a.rsvDate + a.rsvTime).localeCompare(b.rsvDate + b.rsvTime));
  }

  function render() {
    const searching = Boolean(currentKeyword.trim());   // 是不是正在看搜尋的結果
    const list = searchReservations(currentDate, currentMeal, currentKeyword);
    setPickerDate(datePicker, currentDate);   // 月曆的按鈕顯示目前看的那一天（A-2）
    keywordInput.value = currentKeyword;
    // 選取中的餐期變成深色；正在看搜尋結果的時候兩顆都不選
    mealBox.querySelectorAll('[data-meal]').forEach(btn => {
      btn.classList.toggle('active', !searching && btn.dataset.meal === currentMeal);
    });

    summary.innerHTML = searching
      ? `搜尋「<b>${escapeHtml(currentKeyword.trim())}</b>」的結果（所有日期），共 <b>${list.length}</b> 筆`
      : `<b>${escapeHtml(currentDate.split('-').join('/'))}</b> <b>${MEAL_NAMES[currentMeal]}</b> 的訂位，共 <b>${list.length}</b> 筆`;

    if (list.length === 0) {
      demoRsvList.innerHTML = '<tr><td colspan="7" class="staff-empty">沒有符合條件的訂位</td></tr>';
      return;
    }
    demoRsvList.innerHTML = list.map(r => `
      <tr>
        <td class="booking-time">${escapeHtml(shortDate(r.rsvDate))} ${escapeHtml(r.rsvTime)}</td>
        <td>${escapeHtml(r.memName)}</td>
        <td>${escapeHtml(r.memTel)}</td>
        <td>${escapeHtml(r.seatTypeNo)} 人桌</td>
        <td><span class="tag ${rsvStatusClass(r.rsvStatus)}">${escapeHtml(r.rsvStatus)}</span></td>
        <td class="booking-note"><span title="${escapeHtml(r.rsvComment || '')}">${escapeHtml(r.rsvComment || '')}</span></td>
        <td class="booking-col-act">
          <div class="booking-actions">
            <button type="button" class="staff-btn booking-btn-sm" data-rsv-edit="${escapeHtml(r.rsvNo)}">編輯</button>
            ${r.rsvStatus !== '實到' ? `<button type="button" class="staff-btn btn-danger-outline booking-btn-sm" data-rsv-cancel="${escapeHtml(r.rsvNo)}">取消訂位</button>` : ''}
          </div>
        </td>
      </tr>`).join('');
  }

  // 在月曆上選了日期：馬上查那一天（餐期不變），並清掉搜尋的文字（有搜尋文字的話是不看日期的）
  datePicker.addEventListener('datepick', () => {
    currentDate = dateInput.value;
    currentKeyword = '';
    render();
  });

  // 按「午餐」「晚餐」：馬上換餐期，並清掉搜尋的文字
  mealBox.addEventListener('click', e => {
    const btn = e.target.closest('[data-meal]');
    if (!btn) return;
    currentMeal = btn.dataset.meal;
    currentKeyword = '';
    render();
  });

  // 按「查詢」（或在輸入框按 Enter）才會用搜尋的文字查
  document.getElementById('demoRsvTools').addEventListener('submit', e => {
    e.preventDefault();   // 假資料不用把表單送到後端
    currentDate = dateInput.value || todayText();   // 日期被清空就當作今天
    currentKeyword = keywordInput.value;
    render();
  });

  // 「刷新頁面」：重新讀一次資料，查詢的條件（日期、餐期、搜尋文字）不變
  document.getElementById('demoRsvRefresh').addEventListener('click', () => {
    reloadDemo();
    render();
    staffToast('已更新訂位的資料');
  });


  /* ---------- 新增／修改訂位的對話框 ----------
     資料庫的版本：「＋ 新增訂位」「編輯」都是連結，Controller 把 rsvForm 放進 Model 後對話框就會出現。
     日期用自製的月曆（A-2）選，月曆沒有時間；時段是另外一排可以點的按鈕（其實是一組單選的欄位，name 是 rsvTime），
     固定是 RSV_TIMES 那四個。 */
  const modal = document.getElementById('demoRsvModal');
  const form = document.getElementById('demoRsvForm');
  const errorBox = document.getElementById('demoRsvError');
  const formDatePicker = document.getElementById('demoRsvFormDatePicker');   // 對話框裡的月曆
  const slotsBox = document.getElementById('demoRsvSlots');                   // 放那一排時段的地方
  let editingNo = null;   // 正在修改的訂位編號；null 代表正在新增

  // 產生那一排時段：午餐的排前面、晚餐的排後面，各有一個小標題。selected 是要先選起來的時間（例如 '17:00'），沒有就都不選
  function renderSlots(selected) {
    const slot = t => `
      <label class="booking-slot">
        <input type="radio" name="rsvTime" value="${t}"${t === selected ? ' checked' : ''}>
        <span>${t}</span>
      </label>`;
    slotsBox.innerHTML = Object.keys(MEAL_NAMES).map(meal =>
      `<span class="booking-slot-group">${MEAL_NAMES[meal]}</span>` + RSV_TIMES.filter(t => mealOf(t) === meal).map(slot).join('')
    ).join('');
  }

  function showError(text) {
    errorBox.textContent = text;
    errorBox.hidden = !text;
  }

  // 目前選到的時段（例如 '17:00'）；還沒選是空字串。input:checked 是「被選起來的那一個」
  function selectedTime() {
    const picked = slotsBox.querySelector('input:checked');
    return picked ? picked.value : '';
  }

  // 打開對話框。reservation 是要修改的那一筆；沒有傳就是新增
  function openForm(reservation) {
    editingNo = reservation ? reservation.rsvNo : null;
    document.getElementById('demoRsvTitle').textContent = reservation ? '修改訂位' : '新增訂位';

    form.memName.value = reservation ? reservation.memName : '';
    form.memTel.value = reservation ? reservation.memTel : '';
    // 日期：新增時不能選今天以前（data-min="today"），而且先選好目前看的那一天（那一天如果已經過了就選今天）；
    //       修改過往的訂位不限制，才改得了狀態
    if (reservation) delete formDatePicker.dataset.min;
    else formDatePicker.dataset.min = 'today';
    setPickerDate(formDatePicker, reservation ? reservation.rsvDate : (currentDate < todayText() ? todayText() : currentDate));
    // 時段：修改時把原本的時段先選起來
    renderSlots(reservation ? reservation.rsvTime : '');
    form.seatTypeNo.value = reservation ? reservation.seatTypeNo : '';
    form.rsvComment.value = reservation ? (reservation.rsvComment || '') : '';
    // 狀態：修改時才能改，而且只能選「預約」「未到」。
    //       「實到」由入座管理設定（已經實到的會多一個固定的選項）；要取消請按列表上的「取消訂位」
    document.getElementById('demoRsvStatusField').hidden = !reservation;
    fillStatusOptions(form.rsvStatus, ['預約', '未到'], reservation ? reservation.rsvStatus : '預約', '實到');

    showError('');
    modal.hidden = false;
    form.memName.focus();
  }

  function closeForm() {
    modal.hidden = true;
    editingNo = null;
  }

  document.getElementById('demoRsvAdd').addEventListener('click', () => openForm(null));
  modal.addEventListener('click', e => {
    if (e.target.closest('[data-demo-close]')) closeForm();
  });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && !modal.hidden) closeForm();
  });

  // 列表上的「編輯」「取消訂位」
  demoRsvList.addEventListener('click', e => {
    const editBtn = e.target.closest('[data-rsv-edit]');
    if (editBtn) {
      const reservation = demo.reservations.find(r => r.rsvNo === Number(editBtn.dataset.rsvEdit));
      if (reservation) openForm(reservation);
      return;
    }
    // 取消訂位：不是把資料刪掉，是把狀態改成「取消」（紀錄留著）。取消之後列表就不再顯示這一筆。
    // 資料庫的版本：表單送到 POST /staff/booking/rsv/cancel，Java 做同樣的事
    const cancelBtn = e.target.closest('[data-rsv-cancel]');
    if (cancelBtn) {
      const reservation = demo.reservations.find(r => r.rsvNo === Number(cancelBtn.dataset.rsvCancel));
      if (!reservation || reservation.rsvStatus === '取消' || reservation.rsvStatus === '實到') return;
      if (!confirm(`確定要取消 ${reservation.memName} 的訂位嗎？`)) return;
      reservation.rsvStatus = '取消';
      saveDemo();
      render();
      staffToast(`已取消 ${reservation.memName} 的訂位`);
    }
  });

  // 「儲存」：檢查 → 存進假資料 → 關閉對話框並更新列表
  // ★ 這些檢查 Java 存檔前也要再做一次（頁面上的檢查可以被跳過）
  form.addEventListener('submit', e => {
    e.preventDefault();   // 假資料不用真的把表單送到後端

    const name = form.memName.value.trim();
    const tel = form.memTel.value.trim();
    let error = '';
    if (!name) error = '請輸入姓名';
    else if (!isTel(tel)) error = '電話請輸入 10 個數字';
    else if (!form.formDate.value) error = '請選擇日期';
    else if (!selectedTime()) error = '請選擇訂位時段';
    else if (!form.seatTypeNo.value) error = '請選擇桌型';
    // 新增的訂位不能訂今天以前的日期（修改過往的訂位不檢查，才能補登狀態）
    else if (editingNo === null && form.formDate.value < todayText()) error = '不能新增今天以前的訂位';
    showError(error);
    if (error) return;

    const data = {
      memName: name,
      memTel: tel,
      rsvDate: form.formDate.value,
      rsvTime: selectedTime(),
      seatTypeNo: Number(form.seatTypeNo.value),
      rsvStatus: editingNo === null ? '預約' : form.rsvStatus.value,   // 新增固定是「預約」
      rsvComment: form.rsvComment.value.trim()
    };
    const isNew = editingNo === null;
    if (isNew) {
      demo.reservations.push(Object.assign({ rsvNo: nextNumber(demo.reservations.map(r => r.rsvNo)) }, data));
    } else {
      Object.assign(demo.reservations.find(r => r.rsvNo === editingNo), data);
    }
    saveDemo();

    // 存好之後，列表換到這筆訂位的那一天、那個餐期，才看得到剛剛存的
    currentDate = data.rsvDate;
    currentMeal = mealOf(data.rsvTime);
    currentKeyword = '';
    closeForm();
    render();
    staffToast(isNew ? `已新增 ${data.memName} 的訂位` : `已修改 ${data.memName} 的訂位`);
  });

  render();
}


/* ========== B-3. 候位（假資料） ==========
   資料庫的版本：Controller 把 waitingList（今天的候位）和 seatBoards（每種桌型的叫號）放進 Model，
   過號、叫下一號、儲存候位都是 method="post" 的表單，做完再 redirect 回這一頁；新增、編輯的對話框是連結到 Controller 打開的。

   這一頁會改的狀態（和組員的 WaitingService 相同）：
     候位中 ──叫下一號──→ 已通知 ──過號──→ 過號（通知後 10 分鐘內沒有回來；過號要重新填候位表單）
     編輯的對話框可以把狀態改成：候位中、已通知、過號、取消
     ★ 已入座不在這一頁：由負責入座管理的組員在安排入座時設定
   「叫號中」＝ 狀態是「已通知」的候位號碼。同一種桌型可能同時有好幾個（都還在 10 分鐘內等客人回來）；
   號碼的顏色：紅底＝已叫號（還在 10 分鐘內），綠底＝逾時（超過 10 分鐘還沒回來，提醒工作人員可以按「過號」了）。 */
function startDemoWaiting() {
  const boards = document.getElementById('demoBoards');
  const summary = document.getElementById('demoWaitingSummary');
  const filterBox = document.getElementById('demoWaitingFilter');   // 放桌型篩選按鈕的地方

  // 目前篩選的桌型（資料庫的版本，這是放在網址上的 seatType）。null 代表看全部
  let currentSeatType = null;

  /* ★ 候位號碼：畫面上顯示的號碼 ＝ 字母 ＋ 兩位數
       字母   由桌型決定：2 人桌 A、4 人桌 B、6 人桌 C
       數字   waitingTicketSeq（今天這個桌型的第幾號），不足兩位前面補 0 → A01、B12；超過 99 會照實顯示三位（A100）
     資料庫的版本：頁面用 Thymeleaf 組（booking.html 候位那一塊的說明），Java 的訊息裡也要用同樣的規則。 */
  const TICKET_LETTERS = { 2: 'A', 4: 'B', 6: 'C' };
  function ticketLabel(w) {
    return (TICKET_LETTERS[w.seatTypeNo] || '') + String(w.waitingTicketSeq).padStart(2, '0');
  }

  // 傳給客人的簡訊內容。假資料不會真的傳，只會在畫面下方顯示「已通知」
  // ★ 資料庫的版本：這段文字由 Java 傳簡訊時使用（寫在 StaffBookingController.java 的說明裡），這裡刪掉就好
  const SMS_TEXT = `【Bistroops】您的候位已到號，請在 ${NOTIFY_MINUTES} 分鐘內回到店門口；逾時未到將視為過號，過號需重新填寫候位表單。`;

  // 狀態標籤的顏色：候位中黃、已通知紅（要注意）、已入座綠、其他（過號）灰
  function waitingStatusClass(status) {
    if (status === '候位中') return 'tag-yellow';
    if (status === '已通知') return 'tag-red';
    if (status === '已入座') return 'tag-green';
    return 'tag-gray';
  }

  // 已通知的候位，10 分鐘還剩幾分鐘。回傳負數代表已經超過了
  function minutesLeft(waiting) {
    const passed = Math.floor((Date.now() - waiting.notifiedAt) / 60000);   // 通知之後過了幾分鐘
    return NOTIFY_MINUTES - passed;
  }

  // 某一種桌型裡，符合某個狀態的候位（依號碼排序）
  function waitingsOf(seatTypeNo, status) {
    return demo.waitings
      .filter(w => w.seatTypeNo === seatTypeNo && w.waitingStatus === status)
      .sort((a, b) => a.waitingTicketSeq - b.waitingTicketSeq);
  }

  function render() {
    // 右邊：每種桌型一張小卡
    boards.innerHTML = SEAT_TYPES.map(type => {
      const calling = waitingsOf(type, '已通知');
      const waiting = waitingsOf(type, '候位中');
      // 叫號中的號碼：紅底是已叫號；超過 10 分鐘的加上 is-late，變成綠底（逾時）
      const callingHtml = calling.length
        ? calling.map(w => `<span class="booking-call${minutesLeft(w) < 0 ? ' is-late' : ''}">${escapeHtml(ticketLabel(w))}</span>`).join('')
        : '<span class="booking-call-none">尚無叫號</span>';
      return `
        <article class="booking-board">
          <div class="booking-board-head">
            <h3 class="booking-board-title">${type} 人桌</h3>
            <button type="button" class="staff-btn btn-card booking-btn-sm" data-call-next="${type}">叫下一號</button>
          </div>
          <p class="booking-board-meta">可用 <b>${escapeHtml(demo.pending[type])}</b> 桌<i>・</i>等候 <b>${waiting.length}</b> 組</p>
          <p class="booking-calls-label">目前叫號</p>
          <p class="booking-calls">${callingHtml}</p>
        </article>`;
    }).join('');

    // 左邊：今天的候位。取消的不顯示；還在處理中的（已通知、候位中）排前面，再依桌型、號碼排
    const ORDER = { '已通知': 0, '候位中': 1, '已入座': 2, '過號': 3 };
    // 依桌型篩選的按鈕：「全部」加上每一種桌型，選取中的那一顆是深色（右邊的叫號不受篩選影響，上面已經產生好了）
    filterBox.innerHTML =
      `<button type="button" class="chip${currentSeatType === null ? ' active' : ''}" data-seat-filter="">全部</button>` +
      SEAT_TYPES.map(type => `<button type="button" class="chip${currentSeatType === type ? ' active' : ''}" data-seat-filter="${type}">${type} 人桌</button>`).join('');

    // 有選桌型的話只留那個桌型的
    const list = demo.waitings
      .filter(w => w.waitingStatus !== '取消' && (currentSeatType === null || w.seatTypeNo === currentSeatType))
      .sort((a, b) => ORDER[a.waitingStatus] - ORDER[b.waitingStatus] || a.seatTypeNo - b.seatTypeNo || a.waitingTicketSeq - b.waitingTicketSeq);

    // 按鈕後面的說明：看全部是「今日候位，全部共 9 組」；選了桌型是「今日候位，2人桌共 6 組」
    summary.innerHTML = `今日候位，${currentSeatType === null ? '全部' : `${currentSeatType}人桌`}共 <b>${list.length}</b> 組`;

    if (list.length === 0) {
      demoWaitingList.innerHTML = `<tr><td colspan="7" class="staff-empty">${currentSeatType === null ? '目前沒有候位' : '這個桌型目前沒有候位'}</td></tr>`;
      return;
    }
    demoWaitingList.innerHTML = list.map(w => {
      // 通知時間：已通知的多顯示還剩幾分鐘，超過 10 分鐘用紅字提醒
      let notify = w.waitingNotifyTime ? escapeHtml(w.waitingNotifyTime) : '—';
      if (w.waitingStatus === '已通知') {
        const left = minutesLeft(w);
        notify += left >= 0
          ? ` <small class="booking-left">剩 ${left} 分</small>`
          : ` <small class="booking-left is-late">逾時</small>`;
      }
      // 操作：已通知的才有「過號」；每一筆都有「編輯」（電話不顯示在列表上，要看、要改都按編輯）
      const no = escapeHtml(w.waitingNo);
      const missBtn = w.waitingStatus === '已通知'
        ? `<button type="button" class="staff-btn btn-danger-outline booking-btn-sm" data-act="miss" data-no="${no}">過號</button>` : '';
      return `
        <tr>
          <td class="booking-no">${escapeHtml(ticketLabel(w))}</td>
          <td>${escapeHtml(w.waitingName)}</td>
          <td>${escapeHtml(w.seatTypeNo)} 人桌</td>
          <td><span class="tag ${waitingStatusClass(w.waitingStatus)}">${escapeHtml(w.waitingStatus)}</span></td>
          <td>${notify}</td>
          <td class="booking-note"><span title="${escapeHtml(w.waitingComment || '')}">${escapeHtml(w.waitingComment || '')}</span></td>
          <td class="booking-col-act">
            <div class="booking-actions">
              ${missBtn}
              <button type="button" class="staff-btn booking-btn-sm" data-act="edit" data-no="${no}">編輯</button>
            </div>
          </td>
        </tr>`;
    }).join('');
  }

  // 「叫下一號」：通知這個桌型「候位中」裡號碼最小的那一位（狀態改成「已通知」、記下通知時間，並「傳簡訊」）
  // 資料庫的版本：表單送到 POST /staff/booking/waiting/call，Java 在這一步真的把 SMS_TEXT 那段文字傳到客人的手機
  boards.addEventListener('click', e => {
    const btn = e.target.closest('[data-call-next]');
    if (!btn) return;
    const next = waitingsOf(Number(btn.dataset.callNext), '候位中')[0];
    if (!next) {
      staffToast(`${btn.dataset.callNext} 人桌目前沒有候位中的客人`);
      return;
    }
    next.waitingStatus = '已通知';
    next.notifiedAt = Date.now();
    next.waitingNotifyTime = timeText(new Date());
    saveDemo();
    render();
    staffToast(`已傳簡訊通知 ${ticketLabel(next)}（${next.waitingName}）`);
    console.log('（假資料）傳給 ' + next.waitingTel + ' 的簡訊內容：' + SMS_TEXT);   // 按 F12 的 Console 可以看到簡訊的內容
  });

  // 依桌型篩選：按「全部」或某一種桌型，馬上換成那個桌型的候位
  // 資料庫的版本：那幾顆是連結（/staff/booking?tab=waiting&seatType=2），由 Controller 查出那個桌型的候位
  filterBox.addEventListener('click', e => {
    const btn = e.target.closest('[data-seat-filter]');
    if (!btn) return;
    currentSeatType = btn.dataset.seatFilter === '' ? null : Number(btn.dataset.seatFilter);   // 空字串是「全部」
    render();
  });

  // 「刷新頁面」：重新讀一次資料，並重新算「剩幾分鐘」（篩選的桌型不變）
  document.getElementById('demoWaitingRefresh').addEventListener('click', () => {
    reloadDemo();
    render();
    staffToast('已更新候位的資料');
  });


  /* ---------- 新增／修改候位的對話框 ----------
     資料庫的版本：「＋ 新增候位」「編輯」都是連結，Controller 把 waitingForm 放進 Model 後對話框就會出現；
     表單送到 POST /staff/booking/waiting/save。新增是替不會用網路的客人取號，Java 替這個桌型取下一個號碼。 */
  const modal = document.getElementById('demoWaitingModal');
  const form = document.getElementById('demoWaitingForm');
  const errorBox = document.getElementById('demoWaitingError');
  let editingNo = null;   // 正在修改的候位編號；null 代表正在新增

  function showError(text) {
    errorBox.textContent = text;
    errorBox.hidden = !text;
  }

  // 打開對話框。waiting 是要修改的那一筆；沒有傳就是新增
  function openForm(waiting) {
    editingNo = waiting ? waiting.waitingNo : null;
    document.getElementById('demoWaitingTitle').textContent = waiting ? '修改候位' : '新增候位';
    document.getElementById('demoWaitingSubmit').textContent = waiting ? '儲存' : '取號';

    form.waitingName.value = waiting ? waiting.waitingName : '';
    form.waitingTel.value = waiting ? waiting.waitingTel : '';
    form.seatTypeNo.value = waiting ? waiting.seatTypeNo : '';
    form.waitingComment.value = waiting ? (waiting.waitingComment || '') : '';
    // 狀態：修改時才能改。「已入座」由入座管理設定（已經入座的會多一個固定的選項）
    document.getElementById('demoWaitingStatusField').hidden = !waiting;
    fillStatusOptions(form.waitingStatus, ['候位中', '已通知', '過號', '取消'], waiting ? waiting.waitingStatus : '候位中', '已入座');

    showError('');
    modal.hidden = false;
    form.waitingName.focus();
  }

  function closeForm() {
    modal.hidden = true;
    editingNo = null;
  }

  document.getElementById('demoWaitingAdd').addEventListener('click', () => openForm(null));
  modal.addEventListener('click', e => {
    if (e.target.closest('[data-demo-close]')) closeForm();
  });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && !modal.hidden) closeForm();
  });

  // 列表上的「過號」「編輯」
  demoWaitingList.addEventListener('click', e => {
    const btn = e.target.closest('[data-act]');
    if (!btn) return;
    const waiting = demo.waitings.find(w => w.waitingNo === Number(btn.dataset.no));
    if (!waiting) return;

    if (btn.dataset.act === 'edit') {
      openForm(waiting);
      return;
    }
    // 過號：已通知的客人沒有回來。資料庫的版本：表單送到 POST /staff/booking/waiting/miss
    if (btn.dataset.act === 'miss') {
      if (waiting.waitingStatus !== '已通知') return;
      const label = `${ticketLabel(waiting)}（${waiting.waitingName}）`;
      if (!confirm(`確定要把 ${label} 設為過號嗎？`)) return;
      waiting.waitingStatus = '過號';
      saveDemo();
      render();
      staffToast(`${label} 已過號`);
    }
  });

  // 「取號」／「儲存」：檢查 → 存進假資料 → 關閉對話框並更新畫面
  // ★ 這些檢查 Java 存檔前也要再做一次
  form.addEventListener('submit', e => {
    e.preventDefault();

    const name = form.waitingName.value.trim();
    const tel = form.waitingTel.value.trim();
    const seatTypeNo = Number(form.seatTypeNo.value);
    const isNew = editingNo === null;
    const status = isNew ? '候位中' : form.waitingStatus.value;   // 新增固定是「候位中」
    const isActive = s => s === '候位中' || s === '已通知';        // 還在等的狀態
    let error = '';
    if (!name) error = '請輸入姓名';
    else if (!isTel(tel)) error = '電話請輸入 10 個數字';
    else if (!seatTypeNo) error = '請選擇桌型';
    // 同一支電話在同一種桌型已經在候位了（候位中或已通知），不能重複（組員的 WaitingService 也有這個檢查）。
    // 修改的時候要排除自己這一筆，不然會被自己擋住
    else if (isActive(status) && demo.waitings.some(w => w.waitingNo !== editingNo && w.waitingTel === tel
        && w.seatTypeNo === seatTypeNo && isActive(w.waitingStatus))) error = '這支電話已經在這個桌型候位了';
    showError(error);
    if (error) return;

    // ★ 候位號碼：這個桌型目前最大的號碼加 1
    const takeNumber = () => nextNumber(demo.waitings.filter(w => w.seatTypeNo === seatTypeNo).map(w => w.waitingTicketSeq));
    let waiting;
    if (isNew) {
      waiting = {
        waitingNo: nextNumber(demo.waitings.map(w => w.waitingNo)),
        waitingTicketSeq: takeNumber(),
        seatTypeNo,
        waitingName: name,
        waitingTel: tel,
        waitingComment: form.waitingComment.value.trim(),
        waitingStatus: '候位中',
        waitingNotifyTime: '',
        notifiedAt: null
      };
      demo.waitings.push(waiting);
    } else {
      waiting = demo.waitings.find(w => w.waitingNo === editingNo);
      // 換了桌型：號碼是每種桌型各自編的，所以在新的桌型重新取一個號碼（★ 資料庫的版本要怎麼做請和組員討論）
      if (waiting.seatTypeNo !== seatTypeNo) {
        waiting.waitingTicketSeq = takeNumber();
        waiting.seatTypeNo = seatTypeNo;
      }
      waiting.waitingName = name;
      waiting.waitingTel = tel;
      waiting.waitingComment = form.waitingComment.value.trim();
      // 狀態改成「已通知」：記下通知時間（這裡不會傳簡訊，要通知客人請按「叫下一號」）；改回「候位中」：把通知時間清掉
      if (status === '已通知' && waiting.waitingStatus !== '已通知') {
        waiting.notifiedAt = Date.now();
        waiting.waitingNotifyTime = timeText(new Date());
      }
      if (status === '候位中') {
        waiting.notifiedAt = null;
        waiting.waitingNotifyTime = '';
      }
      waiting.waitingStatus = status;
    }
    saveDemo();
    closeForm();
    render();
    const label = `${ticketLabel(waiting)}（${waiting.waitingName}）`;
    staffToast(isNew ? `已新增候位：${label}` : `已修改候位：${label}`);
  });

  render();
  // 每 30 秒重新產生一次畫面，「剩幾分鐘」「逾時」才會跟著時間變
  setInterval(render, 30000);
}


// 這一頁目前顯示哪一個分頁，就執行哪一段
if (demoRsvList) startDemoRsv();
if (demoWaitingList) startDemoWaiting();

// ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
// ▲▲▲  【假資料・刪除範圍 結束】刪到這一行為止（這一行也刪掉）。           ▲▲▲
// ▲▲▲   下面的 })(); 不是假資料，要留著。                                ▲▲▲
// ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

})();
