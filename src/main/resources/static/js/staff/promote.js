/* =========================================================
   Bistroops 員工後台：活動管理（查詢、詳細、新增、修改活動；每個活動一張卡片）
   對應的 HTML：templates/staff/promote.html
   對應的 CSS ：static/css/staff/promote.css
   需要先載入 staff.js（用到裡面的 escapeHtml、staffToast）

   這個檔案分成兩區：
     A. 資料庫的畫面也會用到的程式（保留）：送出表單前的檢查、選了圖片之後顯示預覽、日期選擇
     B. 假資料：只控制 promote.html 裡標示【假資料】的卡片和對話框

   【Thymeleaf 串接】資料庫的版本確認沒問題後：
     1. 刪掉 promote.html 裡標示【假資料】的兩塊（工具列與卡片、對話框）
     2. 刪掉這個檔案的 B 區（下面用 ▼▼▼ 和 ▲▲▲ 框起來的範圍；最後一行的 })(); 要留著）
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和其他檔案互相衝突
(() => {


/* =====================================================================
   A. 資料庫的畫面（promote.html 的【資料庫資料】）也會用到的程式 ── 保留
   這一區和資料無關：
     A-1. 送出表單前先檢查一次，有問題就把訊息顯示在表單上方、不送出
     A-2. 選了圖片之後，在預覽框顯示新選的那一張
     A-3. 日期選擇：自己做的月曆（只選年月日，不能選今天以前，結束日期最早是開始日期的隔天）
   注意：這些只是方便使用者，Controller 收到資料後還是要自己再檢查一次。
   ===================================================================== */

/* ---------- 日期的小工具（A-1 的檢查和 A-3 的月曆都會用到） ----------
   活動的開始、結束只選「年月日」，不選幾點幾分。表單送出的日期是 '2026-10-15' 這種文字。
   文字的格式都一樣（年-月-日，月和日都補成兩位數），所以直接比文字的大小就等於比日期的先後。 */

// Date 物件 → '2026-10-15'
function toDateText(date) {
  const two = n => String(n).padStart(2, '0');   // 個位數前面補 0
  return date.getFullYear() + '-' + two(date.getMonth() + 1) + '-' + two(date.getDate());
}
// 今天，格式 '2026-10-15'
function todayText() {
  return toDateText(new Date());
}
// 把日期往後移幾天：addDays('2026-10-31', 1) → '2026-11-01'
function addDays(dateText, days) {
  const [y, m, d] = dateText.split('-').map(Number);
  return toDateText(new Date(y, m - 1, d + days));
}
// 開始日期是不是被鎖住（進行中的活動不能改開始日期，HTML 會在那一格加上 data-locked="yes"）
function isBeginLocked(form) {
  const picker = form.querySelector('[data-date-picker="begin"]');
  return Boolean(picker && picker.dataset.locked === 'yes');
}


/* ---------- A-1. 送出前的檢查 ---------- */

// 檢查表單的內容。沒問題回傳空字串，有問題回傳要顯示的訊息。
// form 裡的欄位用 name 取得：promoteName、promoteBegin、promoteEnd（和 PromoteVO 的屬性同名）
// ★ 這些規則 Java 存檔前也要再檢查一次（頁面上的檢查可以被跳過）
function checkPromoteForm(form) {
  const name = form.promoteName.value.trim();
  const begin = form.promoteBegin.value;   // 格式是 2026-10-15（只有年月日）
  const end = form.promoteEnd.value;
  const today = todayText();
  if (!name) return '請輸入活動名稱';
  if (name.length > 10) return '活動名稱最多 10 個字';
  if (!begin || !end) return '請選擇開始日期與結束日期';
  // 開始日期不能早於今天。進行中的活動開始日期是鎖住的（本來就在今天以前），所以不檢查
  if (!isBeginLocked(form) && begin < today) return '開始日期不能早於今天';
  if (end < today) return '結束日期不能早於今天';
  // 結束日期最早是開始日期的隔天（不能同一天）。addDays(begin, 1) 就是開始日期的隔天
  if (end < addDays(begin, 1)) return '結束日期最早是開始日期的隔天';
  return '';
}

// 把訊息顯示在表單上方（HTML 裡有 data-form-error 的那一行）；text 是空的就把那一行藏起來
function showFormError(form, text) {
  const box = form.querySelector('[data-form-error]');
  if (!box) return;
  box.textContent = text;
  box.hidden = !text;
}

// 資料庫的表單：送出前先檢查，有問題就不送出
const dbPromoteForm = document.getElementById('dbPromoteForm');
if (dbPromoteForm) {
  dbPromoteForm.addEventListener('submit', e => {
    const error = checkPromoteForm(dbPromoteForm);
    showFormError(dbPromoteForm, error);
    if (error) e.preventDefault();   // 取消這一次的送出
  });
}


/* ---------- A-2. 活動圖片：選了圖片之後，顯示預覽 ----------
   活動圖片會顯示在首頁的輪播，輪播的框是 16:9，圖片用 CSS 拉開填滿（object-fit: fill）：
   其他比例的圖片會被拉成 16:9（會變形），不留白、不裁切。
   表單裡的預覽框用的是同樣的 CSS，所以選了圖片之後，在預覽框看到的就是它在首頁上的樣子。

   這裡只負責「把新選的圖片放進預覽框」。圖片檔案本身不會被改動：
   按「儲存」送到 Controller、存進資料庫的，是使用者選的原本的圖片。

   HTML 的寫法（資料庫和假資料的表單都一樣）：
     預覽框   <div class="promote-img-box" data-img-preview>
     檔案欄位 <input type="file" name="upImg" data-img-input>
     兩個要放在同一個 <div class="promote-field"> 裡面，這裡才找得到對方。 */

// 把預覽框的內容換成一張圖片；src 是空的就顯示「尚無圖片」
function showImgPreview(box, src) {
  box.textContent = '';
  if (!src) { box.textContent = '尚無圖片'; return; }
  const img = document.createElement('img');
  img.src = src;
  img.alt = '圖片預覽';
  box.appendChild(img);
}

// 頁面上每一個選擇圖片的欄位（資料庫的表單、假資料的表單）都套用
document.querySelectorAll('[data-img-input]').forEach(input => {
  const preview = input.closest('.promote-field').querySelector('[data-img-preview]');
  const original = preview.innerHTML;   // 預覽框一開始的內容（目前的圖片，或「尚無圖片」）

  input.addEventListener('change', () => {
    const file = input.files[0];

    // 打開選檔案的視窗又按了取消：預覽框回到一開始的內容
    if (!file) { preview.innerHTML = original; return; }

    // 選到的不是圖片（file.type 是檔案的種類，圖片都是 image/ 開頭，例如 image/png）
    if (!file.type.startsWith('image/')) {
      input.value = '';                 // 清掉選到的檔案
      preview.innerHTML = original;
      if (input.form) showFormError(input.form, '請選擇圖片檔案（JPG、PNG 等）');
      return;
    }

    // URL.createObjectURL 會替這個檔案產生一個暫時的網址，<img> 用它就能顯示還沒上傳的圖片
    showImgPreview(preview, URL.createObjectURL(file));
    if (input.form) showFormError(input.form, '');
  });
});


/* ---------- A-3. 日期選擇：自己做的月曆 ----------
   不用瀏覽器內建的日期欄位（每個瀏覽器長得不一樣，也不好限制），改成自己畫的月曆。
   月曆是「跳出來浮在畫面上」的小視窗，不佔版面的位置：按日期的按鈕就出現在按鈕旁邊，
   選了日期、點到別的地方、或捲動畫面時會自己關掉。

   HTML 的寫法（資料庫和假資料的表單都一樣）：
     <div class="promote-date" data-date-picker="begin">      ← begin 是開始日期，end 是結束日期
       <input type="hidden" name="promoteBegin">              ← 真正送出去的值，格式 2026-10-15
       <button type="button" class="promote-date-btn" data-date-toggle><span data-date-text></span>…</button>
       <div class="promote-date-panel" data-date-panel hidden></div>   ← 月曆（跳出來的小視窗），內容和位置由這裡決定
     </div>

   可以選的範圍：
     開始日期：不能早於今天
     結束日期：不能早於今天，而且最早是開始日期的隔天（不能和開始日期同一天）
   選了開始日期之後，如果原本的結束日期變成不能選的，會把結束日期清掉，請使用者重選。
   進行中的活動不能改開始日期：那一格有 data-locked="yes"，按鈕會鎖住。

   ★ 這裡只是讓使用者「選不到」不對的日期。隱藏欄位的值還是可以被改，所以 Java 存檔前要用同樣的規則再檢查一次。
   ★ 送到 Controller 的只有年月日；幾點幾分由 Java 補（開始 00:00、結束 23:59）。 */
const WEEKDAYS = '日一二三四五六';

// '2026-10-15' → '2026/10/15（四）'
function dateLabel(dateText) {
  const [y, m, d] = dateText.split('-').map(Number);
  return dateText.split('-').join('/') + '（' + WEEKDAYS[new Date(y, m - 1, d).getDay()] + '）';
}

// 這個日期欄位最早可以選哪一天
function minDateOf(picker) {
  const today = todayText();
  if (picker.dataset.datePicker !== 'end') return today;   // 開始日期：今天
  // 結束日期：今天，或「開始日期的隔天」，看哪一個比較晚
  const form = picker.closest('form');
  const begin = form ? form.promoteBegin.value : '';
  if (!begin) return today;
  const afterBegin = addDays(begin, 1);   // 開始日期的隔天
  return afterBegin > today ? afterBegin : today;
}

// 依照隱藏欄位的值，更新按鈕上的文字和鎖住的狀態
function refreshDatePicker(picker) {
  const input = picker.querySelector('input[type="hidden"]');
  const button = picker.querySelector('[data-date-toggle]');
  picker.querySelector('[data-date-text]').textContent = input.value ? dateLabel(input.value) : '請選擇日期';
  picker.classList.toggle('is-empty', !input.value);
  button.disabled = picker.dataset.locked === 'yes';
}

// 關掉月曆
function closeDatePanel(picker) {
  picker.querySelector('[data-date-panel]').hidden = true;
  picker.classList.remove('is-open');
}

// 決定跳出來的月曆要出現在畫面的哪裡：預設貼在日期按鈕的下面，下面放不下就改放上面。
// 月曆是 position: fixed（見 promote.css），所以位置是用「離畫面上緣、左緣多遠」來指定
function placeDatePanel(picker) {
  const panel = picker.querySelector('[data-date-panel]');
  // getBoundingClientRect() 會告訴我們按鈕現在在畫面上的位置（top、bottom、left 都是離畫面邊緣的距離）
  const button = picker.querySelector('[data-date-toggle]').getBoundingClientRect();
  const GAP = 6;       // 月曆和按鈕之間的距離
  const MARGIN = 10;   // 月曆離畫面邊緣至少要留多少

  // 上下：先試下面；下面會超出畫面、而且上面放得下，就放上面
  let top = button.bottom + GAP;
  const fitsBelow = top + panel.offsetHeight <= window.innerHeight - MARGIN;
  const topIfAbove = button.top - GAP - panel.offsetHeight;
  if (!fitsBelow && topIfAbove >= MARGIN) top = topIfAbove;
  // 上下都放不下（畫面很矮）：盡量留在畫面裡
  top = Math.max(MARGIN, Math.min(top, window.innerHeight - panel.offsetHeight - MARGIN));

  // 左右：左邊對齊按鈕；會超出畫面右邊就往左移
  let left = Math.min(button.left, window.innerWidth - panel.offsetWidth - MARGIN);
  left = Math.max(MARGIN, left);

  panel.style.top = top + 'px';
  panel.style.left = left + 'px';
}

// 畫出某一年某一月的月曆。month 是 0～11（JavaScript 的月份從 0 開始）
function drawDatePanel(picker, year, month) {
  const panel = picker.querySelector('[data-date-panel]');
  const value = picker.querySelector('input[type="hidden"]').value;
  const min = minDateOf(picker);
  const today = todayText();

  const firstWeekday = new Date(year, month, 1).getDay();      // 這個月 1 號是星期幾（0 是星期日）
  const daysInMonth = new Date(year, month + 1, 0).getDate();   // 這個月有幾天（下個月的第 0 天＝這個月最後一天）

  // 1 號前面的空格
  let days = '';
  for (let i = 0; i < firstWeekday; i++) days += '<span></span>';
  // 每一天一顆按鈕；早於 min 的不能按
  for (let d = 1; d <= daysInMonth; d++) {
    const dateText = toDateText(new Date(year, month, d));
    const classes = ['promote-cal-day'];
    if (dateText === today) classes.push('is-today');
    if (dateText === value) classes.push('is-selected');
    days += `<button type="button" class="${classes.join(' ')}" data-date-day="${dateText}"${dateText < min ? ' disabled' : ''}>${d}</button>`;
  }

  // 上一個月的最後一天如果早於 min，就沒有可以選的日子，「上個月」不能按
  const prevDisabled = toDateText(new Date(year, month, 0)) < min;
  const hint = picker.dataset.datePicker === 'end'
    ? '結束日期至少需和開始日期相隔一天' : '開始日期不能早於今天';

  panel.dataset.year = year;
  panel.dataset.month = month;
  panel.innerHTML = `
    <div class="promote-cal-head">
      <button type="button" class="promote-cal-nav" data-date-nav="-1" aria-label="上個月"${prevDisabled ? ' disabled' : ''}>‹</button>
      <span class="promote-cal-title">${year} 年 ${month + 1} 月</span>
      <button type="button" class="promote-cal-nav" data-date-nav="1" aria-label="下個月">›</button>
    </div>
    <div class="promote-cal-week">${WEEKDAYS.split('').map(w => `<span>${w}</span>`).join('')}</div>
    <div class="promote-cal-days">${days}</div>
    <p class="promote-cal-hint">${hint}</p>`;

  // 月曆開著的時候（例如按了上個月、下個月），每個月的列數不一樣、高度會變，所以重新算一次位置
  if (!panel.hidden) placeDatePanel(picker);
}

// 打開月曆：顯示「已經選的那個月」；還沒選就顯示最早可以選的那個月
function openDatePanel(picker) {
  document.querySelectorAll('[data-date-picker]').forEach(other => { if (other !== picker) closeDatePanel(other); });
  const value = picker.querySelector('input[type="hidden"]').value;
  const min = minDateOf(picker);
  const [y, m] = (value && value >= min ? value : min).split('-').map(Number);
  // 先讓月曆出現，再畫內容：要先出現，瀏覽器才量得到它的高度，位置才算得出來（drawDatePanel 最後會算位置）
  picker.querySelector('[data-date-panel]').hidden = false;
  picker.classList.add('is-open');
  drawDatePanel(picker, y, m - 1);
}

// 把所有打開的月曆關掉
function closeAllDatePanels() {
  document.querySelectorAll('[data-date-picker].is-open').forEach(closeDatePanel);
}

// 更新一個表單裡所有的日期欄位（頁面載入時、或程式改了隱藏欄位的值之後呼叫）
function refreshDatePickers(root) {
  root.querySelectorAll('[data-date-picker]').forEach(picker => {
    closeDatePanel(picker);
    refreshDatePicker(picker);
  });
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

  // (3) 選了某一天：把日期放進隱藏欄位、關掉月曆
  const day = e.target.closest('[data-date-day]');
  if (day) {
    const picker = day.closest('[data-date-picker]');
    const form = picker.closest('form');
    picker.querySelector('input[type="hidden"]').value = day.dataset.dateDay;
    closeDatePanel(picker);
    refreshDatePicker(picker);

    // 改了開始日期：結束日期如果變成不能選的（比開始日期早），就清掉請使用者重選
    if (picker.dataset.datePicker === 'begin' && form) {
      const endPicker = form.querySelector('[data-date-picker="end"]');
      const endInput = endPicker.querySelector('input[type="hidden"]');
      if (endInput.value && endInput.value < minDateOf(endPicker)) endInput.value = '';
      refreshDatePicker(endPicker);
    }
    if (form) showFormError(form, '');
    return;
  }

  // (4) 點到月曆以外的地方：把打開的月曆關掉
  if (!e.target.closest('[data-date-picker]')) closeAllDatePanels();
});

// 月曆是固定浮在畫面上的，按鈕的位置一變（捲動對話框的內容、平板轉向、視窗大小改變），
// 月曆就會和按鈕對不上，所以這些時候直接把它關掉，要選再按一次。
// 第三個參數 true：連「對話框裡面那一塊」的捲動也收得到（一般的寫法只收得到整個頁面的捲動）
window.addEventListener('scroll', closeAllDatePanels, true);
window.addEventListener('resize', closeAllDatePanels);
// 按 Esc 也關掉月曆
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') closeAllDatePanels();
});

// 頁面載入時：資料庫的表單（Thymeleaf 已經把日期放進隱藏欄位了）先把按鈕上的文字顯示出來
refreshDatePickers(document);


// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
// ▼▼▼  【假資料・刪除範圍 開始】                                          ▼▼▼
// ▼▼▼   串接完成、確認資料庫的畫面沒問題後，從這一行開始刪，               ▼▼▼
// ▼▼▼   一直刪到下面 ▲▲▲ 框起來的「刪除範圍 結束」那一行（標記也一起刪掉）▼▼▼
// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
/* =====================================================================
   B. 假資料 ── 資料庫的版本確認沒問題後，從上面的 ▼▼▼ 到下面的 ▲▲▲ 都可以刪掉
      （最後一行的 })(); 要留著）
   這一區只控制 promote.html 裡標示【假資料】的那兩塊：用下面的 PromoteDemo 產生卡片，
   篩選、搜尋、新增、修改都是直接改瀏覽器裡的假資料，不會送到後端。
   ===================================================================== */
const promoteList = document.getElementById('promoteList');   // 放假資料卡片的地方
// 假資料那一塊已經從 promote.html 刪掉的話，下面的程式就不用執行了
if (!promoteList) return;


/* ========== B-1. 假資料 PromoteDemo ==========
   用來代替資料庫的「活動 promote」資料表。資料暫存在瀏覽器的 localStorage
   （關掉分頁再打開還在，但只存在這台裝置）。

   一筆活動的格式（欄位名稱和 PromoteVO 的屬性相同）：
     promoteNo       活動編號（新增時自動往下編）
     promoteName     活動名稱（最多 10 個字）
     promoteBegin    開始時間，格式 '2026-10-01T00:00'（畫面只選年月日，時間固定是那一天的 00:00）
     promoteEnd      結束時間，格式 '2026-10-10T23:59'（時間固定是那一天的 23:59）
                     資料庫的版本也是這樣：表單只送年月日，由 Java 補上 00:00 和 23:59 再存。
     promoteImg      活動圖片。假資料沒有資料庫可以存檔案，所以存成一段很長的文字（data URL，
                     用 "data:image/jpeg;base64," 開頭），<img> 的 src 可以直接用它顯示。沒有圖片是空字串。
                     資料庫的版本存的是檔案的內容（byte[]），不會用到這種文字。
     （沒有活動狀態的欄位：狀態是用開始、結束時間和現在的時間比出來的，見 B-2 的 statusOf）
     promoteContent  活動內容

   資料庫的版本：活動由 Controller 查資料庫後放進 Model（名稱 promotes），
   新增、修改是表單送到 POST /staff/promote/save。寫在 promote.html 的【資料庫資料】。 */
const PromoteDemo = (() => {
  // 示範資料的內容有修改時，把最後的數字加 1，瀏覽器就會改用新的示範資料（舊的暫存不再使用）
  const STORAGE_KEY = 'bistroopsStaffDemoPromotes6';

  // 一開始的示範活動（12 筆，一頁 8 筆，所以會有兩頁）。狀態會跟著今天的日期變：
  // 結束時間已經過了的是「已結束」，還沒到開始時間的是「未開始」，其他是「進行中」
  function defaultPromotes() {
    return [
      { promoteNo: 1, promoteName: '週年慶活動', promoteBegin: '2026-09-01T00:00', promoteEnd: '2026-09-30T23:59',
        promoteContent: '週年慶期間，指定主餐享優惠價。' },
      { promoteNo: 2, promoteName: '中秋節活動', promoteBegin: '2026-09-20T00:00', promoteEnd: '2026-10-06T23:59',
        promoteContent: '中秋節限定套餐，四人同行招待甜點一份。' },
      { promoteNo: 3, promoteName: '雙十節活動', promoteBegin: '2026-10-01T00:00', promoteEnd: '2026-10-10T23:59',
        promoteContent: '雙十國慶期間，指定前菜與飲品第二件半價。' },
      { promoteNo: 4, promoteName: '平日午間優惠', promoteBegin: '2026-10-01T00:00', promoteEnd: '2026-12-31T23:59',
        promoteContent: '週一至週五 11:00–14:00，商業午餐 85 折。' },
      { promoteNo: 5, promoteName: '夏季消暑活動', promoteBegin: '2026-07-01T00:00', promoteEnd: '2026-08-31T23:59',
        promoteContent: '夏季限定冰品與冷湯，第二份八折。' },
      { promoteNo: 6, promoteName: '聖誕節活動', promoteBegin: '2026-12-18T00:00', promoteEnd: '2026-12-25T23:59',
        promoteContent: '聖誕限定雙人套餐，需提前訂位。' },
      { promoteNo: 7, promoteName: '開幕慶活動', promoteBegin: '2026-03-01T00:00', promoteEnd: '2026-03-31T23:59',
        promoteContent: '開幕期間全品項九折。' },
      { promoteNo: 8, promoteName: '母親節活動', promoteBegin: '2026-05-01T00:00', promoteEnd: '2026-05-10T23:59',
        promoteContent: '母親節當週，媽媽用餐招待甜點。' },
      { promoteNo: 9, promoteName: '父親節活動', promoteBegin: '2026-08-01T00:00', promoteEnd: '2026-08-08T23:59',
        promoteContent: '父親節限定牛排套餐。' },
      { promoteNo: 10, promoteName: '萬聖節活動', promoteBegin: '2026-10-25T00:00', promoteEnd: '2026-10-31T23:59',
        promoteContent: '變裝入場招待特調飲品一杯。' },
      { promoteNo: 11, promoteName: '感恩節活動', promoteBegin: '2026-11-20T00:00', promoteEnd: '2026-11-26T23:59',
        promoteContent: '感恩節烤雞分享餐，需提前三天預訂。' },
      { promoteNo: 12, promoteName: '跨年活動', promoteBegin: '2026-12-31T00:00', promoteEnd: '2027-01-01T23:59',
        promoteContent: '跨年夜限定套餐，含香檳一杯。' }
    ];
  }

  // 從瀏覽器讀出目前的活動；沒有存過（或讀不到）就用一開始的示範活動
  function getAll() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
      if (Array.isArray(saved)) return saved;
    } catch (e) { /* 讀不到就用下面的示範資料 */ }
    return defaultPromotes();
  }

  // 存進瀏覽器。成功回傳 true；存不進去（例如圖片太多，超過瀏覽器給的空間）回傳 false
  function saveAll(promotes) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(promotes));
      return true;
    } catch (e) {
      return false;
    }
  }

  return {
    getAll,

    // 用活動編號找一筆活動；找不到回傳 undefined
    find(promoteNo) {
      return getAll().find(p => p.promoteNo === promoteNo);
    },

    // 儲存一筆活動。data.promoteNo 是 null → 新增（編號用目前最大的編號加 1）；有值 → 修改那一筆。
    // 回傳存好的那一筆；存不進去回傳 null
    save(data) {
      const promotes = getAll();
      if (data.promoteNo === null) {
        const maxNo = promotes.reduce((max, p) => Math.max(max, p.promoteNo), 0);
        const created = Object.assign({}, data, { promoteNo: maxNo + 1 });
        promotes.push(created);
        return saveAll(promotes) ? created : null;
      }
      const target = promotes.find(p => p.promoteNo === data.promoteNo);
      if (!target) return null;
      Object.assign(target, data);
      return saveAll(promotes) ? target : null;
    },

    // 別的分頁改了資料時通知這一頁（讓兩個分頁的畫面保持一致）
    onChange(callback) {
      window.addEventListener('storage', e => {
        if (e.key === STORAGE_KEY) callback();
      });
    }
  };
})();


/* ========== B-2. 查詢與產生卡片（假資料） ==========
   資料庫的版本：Controller 依照網址上的 status、keyword、page 去資料庫查「那一頁」的 8 筆
   （/staff/promote?status=進行中&keyword=節&page=2），卡片用 th:each 產生（一個活動一張）。

   ★ 搜尋、狀態、分頁、排序都應該由後端查資料庫，不是在瀏覽器裡篩選。
     假資料沒有後端，所以用下面的 searchPromotes 來「模擬後端的查詢」：
       給它 狀態、關鍵字、第幾頁 → 它回傳 那一頁的活動（最多 8 筆）、目前第幾頁、總共幾頁、總共幾筆。
     Service（或 Repository）要做的事和它一樣，可以對照著寫；差別是資料庫用分頁的查詢，
     只會讀出那一頁的 8 筆，不會像這裡一樣先拿到全部再切。
     畫面這邊也照後端的做法：打字的時候不會查，按了「搜尋」（或 Enter）、換狀態、換頁才會查。

   ★ 活動狀態不存在資料裡，是用時間判斷的（下面的 statusOf）。
     資料庫的版本在 promote.html 用 Thymeleaf 寫了同樣的判斷式（th:with 裡的 statusText）；
     依狀態查詢時，Controller 要把狀態換成時間的條件去查（寫在 promote.html 和 StaffPageController 的說明裡）。 */
const promoteSearch = document.getElementById('promoteSearch');
const filterButtons = document.querySelectorAll('#promoteFilters [data-filter]');

const PAGE_SIZE = 8;      // 一頁顯示幾張卡片

// 目前的查詢條件（資料庫的版本，這三個是放在網址上的 status、keyword、page）
let currentStatus = '';   // 狀態：''（全部）／'未開始'／'進行中'／'已結束'
let currentKeyword = '';  // 關鍵字：按了「搜尋」才會更新
let currentPage = 1;      // 第幾頁（從 1 開始）

// 現在的時間，格式和活動的時間相同：'2026-10-07T15:30'
function nowText() {
  const now = new Date();
  const two = n => String(n).padStart(2, '0');   // 個位數前面補 0
  return toDateText(now) + 'T' + two(now.getHours()) + ':' + two(now.getMinutes());
}

// ★ 判斷一筆活動的狀態：用開始、結束時間和現在的時間比較
//   時間的文字格式都一樣（年-月-日T時:分），所以直接比文字的大小就等於比時間的先後
function statusOf(promote) {
  const now = nowText();
  if (now < promote.promoteBegin) return '未開始';   // 現在 早於 開始時間
  if (now > promote.promoteEnd) return '已結束';     // 現在 晚於 結束時間
  return '進行中';                                    // 其他：在開始和結束之間
}

// 狀態標籤的顏色：進行中是綠色、未開始是黃色、其他（已結束）是灰色
function statusClass(status) {
  if (status === '進行中') return 'tag-green';
  if (status === '未開始') return 'tag-yellow';
  return 'tag-gray';
}

// 畫面上只顯示年月日：'2026-10-01T00:00' → '2026/10/01'
// （slice(0, 10) 是拿前面 10 個字，也就是「年-月-日」的部分）
function showDate(text) {
  return String(text || '').slice(0, 10).split('-').join('/');
}

// 模擬後端的查詢：依狀態、關鍵字篩選 → 依結束時間排序 → 只拿第 page 頁的 8 筆。
// 回傳 { list 那一頁的活動, page 第幾頁, totalPages 總共幾頁, total 總共幾筆 }
function searchPromotes(status, keyword, page) {
  const word = keyword.trim().toLowerCase();

  // 1. 篩選：狀態（用時間判斷）和關鍵字（活動名稱裡有這些字）都要符合
  const matched = PromoteDemo.getAll().filter(p => {
    if (status && statusOf(p) !== status) return false;
    return p.promoteName.toLowerCase().includes(word);   // word 是空字串時，每一筆都算符合
  });

  // 2. 排序：依結束時間，晚的排前面（還沒結束的在前面，已經結束很久的在最後）。
  //    想改成「早的排前面」，把 b 和 a 對調：a.promoteEnd.localeCompare(b.promoteEnd)
  //    結束時間一樣的話，編號大的（比較新的）排前面
  matched.sort((a, b) => b.promoteEnd.localeCompare(a.promoteEnd) || b.promoteNo - a.promoteNo);

  // 3. 分頁：總共幾頁（至少 1 頁）；page 超出範圍就拉回來
  const totalPages = Math.max(1, Math.ceil(matched.length / PAGE_SIZE));
  const safePage = Math.min(Math.max(1, page), totalPages);
  const start = (safePage - 1) * PAGE_SIZE;   // 這一頁的第一筆是全部裡的第幾筆（從 0 開始算）

  return { list: matched.slice(start, start + PAGE_SIZE), page: safePage, totalPages, total: matched.length };
}

const promotePrev = document.getElementById('promotePrev');
const promoteNext = document.getElementById('promoteNext');

function render() {
  const result = searchPromotes(currentStatus, currentKeyword, currentPage);
  currentPage = result.page;   // 例如刪減資料後頁數變少，目前的頁數會被拉回最後一頁

  // 右下角的分頁
  document.getElementById('promoteTotal').textContent = `共 ${result.total} 筆`;
  document.getElementById('promotePageText').textContent = `第 ${result.page} / ${result.totalPages} 頁`;
  promotePrev.disabled = result.page <= 1;                    // 已經是第一頁，不能再往前
  promoteNext.disabled = result.page >= result.totalPages;    // 已經是最後一頁，不能再往後

  if (result.list.length === 0) {
    promoteList.innerHTML = '<p class="staff-empty">沒有符合條件的活動</p>';
    return;
  }

  // 每一筆活動一張卡片：上面是滿版的 16:9 圖片，下面是名稱、期間，最下面一排是狀態和「詳細」
  promoteList.innerHTML = result.list.map(p => `
    <article class="promote-card">
      <div class="promote-card-img">${p.promoteImg ? `<img src="${escapeHtml(p.promoteImg)}" alt="">` : '尚無圖片'}</div>
      <div class="promote-card-body">
        <h3 class="promote-card-name">${escapeHtml(p.promoteName)}<span class="promote-card-no">編號 ${escapeHtml(p.promoteNo)}</span></h3>
        <p class="promote-card-period">
          ${escapeHtml(showDate(p.promoteBegin))} <small>起</small><br>
          ${escapeHtml(showDate(p.promoteEnd))} <small>止</small>
        </p>
        <div class="promote-card-foot">
          <span class="tag ${statusClass(statusOf(p))}">${escapeHtml(statusOf(p))}</span>
          <button type="button" class="staff-btn" data-detail-promote="${escapeHtml(p.promoteNo)}">詳細</button>
        </div>
      </div>
    </article>`).join('');
}

// 換狀態：回到第 1 頁重新查
filterButtons.forEach(btn => {
  btn.addEventListener('click', () => {
    currentStatus = btn.dataset.filter;
    currentPage = 1;
    filterButtons.forEach(b => b.classList.toggle('active', b === btn));
    render();
  });
});

// 搜尋：按「搜尋」按鈕或在搜尋框按 Enter 才會查（打字的時候不會查），查的時候回到第 1 頁
document.getElementById('promoteSearchForm').addEventListener('submit', e => {
  e.preventDefault();   // 假資料不用把表單送到後端
  currentKeyword = promoteSearch.value;
  currentPage = 1;
  render();
});

// 上一頁、下一頁
promotePrev.addEventListener('click', () => { currentPage -= 1; render(); });
promoteNext.addEventListener('click', () => { currentPage += 1; render(); });


/* ========== B-3. 活動的對話框（假資料）：活動詳細、新增／修改的表單、套用已有活動 ==========
   對話框裡有三個畫面，一次只顯示一個：
     畫面一「活動詳細」：按卡片上的「詳細」打開，顯示這筆活動全部的資訊，下方有「修改」
                         （已經結束的活動不能修改，沒有「修改」這顆按鈕）
     畫面二「表單」    ：按「＋ 新增活動」打開，或在畫面一按「修改」換過來
     畫面三「套用已有活動」：在新增的表單按「套用已有活動」打開，挑一個已結束的活動，
                             把它的名稱、內容、圖片帶進表單（程式在 B-4）
   資料庫的版本是 promote.html【資料庫資料】的兩個對話框：
   「詳細」「＋ 新增活動」「修改」都是連結，Controller 把 promoteDetail 或 promoteForm 放進 Model 後
   對應的對話框就會打開；「儲存」把表單送到 POST /staff/promote/save。 */
const promoteModal = document.getElementById('promoteModal');
const promoteForm = document.getElementById('promoteForm');
const promoteDetailView = document.getElementById('promoteDetailView');
const promotePickView = document.getElementById('promotePickView');

const promoteImgFile = document.getElementById('promoteImgFile');       // 假資料表單裡選擇圖片的欄位
const promoteImgPreview = document.getElementById('promoteImgPreview'); // 假資料表單裡的預覽框

let editingNo = null;   // 正在修改的活動編號；null 代表正在新增
let viewingNo = null;   // 正在看詳細的活動編號
let appliedImg = '';    // 新增時「套用已有活動」帶進來的圖片；沒有套用（或那個活動沒有圖片）是空字串

// ---- 假資料的圖片 ----
// 資料庫的版本是把檔案直接送到 Controller 存起來；假資料沒有後端，只能把圖片變成文字存進瀏覽器。
// 瀏覽器給的空間很小，所以先把圖片等比例縮小（最長的那一邊縮到 800，比例不變、不會變形）再存。
// 這一段只有假資料需要，接上資料庫後整個刪掉。
const DEMO_IMG_MAX = 800;   // 縮小之後，最長的那一邊是幾個像素

let pendingImg = null;      // 這次在表單裡新選的圖片（已經變成文字）；null 代表沒有選新的

// 把圖片檔案等比例縮小，變成一段文字（data URL）。回傳 Promise
function fileToSmallDataUrl(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      // 縮小的倍率：最長邊超過 800 才縮，不會把小圖放大
      const scale = Math.min(1, DEMO_IMG_MAX / Math.max(img.naturalWidth, img.naturalHeight));
      const canvas = document.createElement('canvas');
      canvas.width = Math.round(img.naturalWidth * scale);
      canvas.height = Math.round(img.naturalHeight * scale);
      const ctx = canvas.getContext('2d');
      ctx.fillStyle = '#fff';   // 先鋪白底：JPEG 沒有透明，透明的地方不鋪底色會變成黑色
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(url);
      resolve(canvas.toDataURL('image/jpeg', 0.8));
    };
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('不是圖片')); };
    img.src = url;
  });
}

// 沒有選新的圖片檔案時要用的圖片：
//   修改 → 那筆活動原本的圖片
//   新增 → 「套用已有活動」帶進來的圖片（沒有套用就是空字串）
function currentDemoImg() {
  if (editingNo === null) return appliedImg;
  const promote = PromoteDemo.find(editingNo);
  return promote ? (promote.promoteImg || '') : '';
}

// 假資料的表單選了圖片：預覽由 A-2 處理，這裡負責把圖片變成等一下要存的文字
promoteImgFile.addEventListener('change', () => {
  const file = promoteImgFile.files[0];
  pendingImg = null;
  // 沒有選（按了取消），或選到的不是圖片（A-2 已經顯示錯誤訊息並清掉）：預覽框顯示這筆活動原本的圖片
  if (!file || !file.type.startsWith('image/')) {
    showImgPreview(promoteImgPreview, currentDemoImg());
    return;
  }
  promoteImgFile.dataset.busy = 'yes';   // 處理中的記號，按「儲存」時會檢查
  fileToSmallDataUrl(file).then(dataUrl => {
    pendingImg = dataUrl;
    delete promoteImgFile.dataset.busy;
  }).catch(() => {
    promoteImgFile.value = '';
    delete promoteImgFile.dataset.busy;
    showImgPreview(promoteImgPreview, currentDemoImg());
    showFormError(promoteForm, '這個檔案不是可以使用的圖片，請重新選擇');
  });
});

// 切換對話框裡的畫面：name 是 'detail'（活動詳細）、'form'（表單）或 'pick'（套用已有活動），其他兩個會被藏起來
function showView(name) {
  promoteDetailView.hidden = name !== 'detail';
  promoteForm.hidden = name !== 'form';
  promotePickView.hidden = name !== 'pick';
}

// 打開「活動詳細」
function openDetail(promote) {
  viewingNo = promote.promoteNo;
  const status = statusOf(promote);   // 狀態用時間判斷（B-2）

  document.getElementById('promoteModalTitle').textContent = '活動詳細';
  document.getElementById('detailNo').textContent = promote.promoteNo;
  document.getElementById('detailName').textContent = promote.promoteName;
  document.getElementById('detailBegin').textContent = showDate(promote.promoteBegin);
  document.getElementById('detailEnd').textContent = showDate(promote.promoteEnd);
  document.getElementById('detailContent').textContent = promote.promoteContent || '（沒有填寫）';
  const tag = document.getElementById('detailStatus');
  tag.textContent = status;
  tag.className = 'tag ' + statusClass(status);   // 標籤的顏色跟著狀態換
  // 已經結束的活動不能修改：把「修改」藏起來，只能看和關閉
  document.getElementById('promoteToEdit').hidden = status === '已結束';
  showImgPreview(document.getElementById('detailImg'), promote.promoteImg);   // 有圖片就顯示，沒有就是「尚無圖片」

  showView('detail');
  promoteModal.hidden = false;
}

// 打開表單。promote 是要修改的那一筆；沒有傳就是新增
function openPromote(promote) {
  editingNo = promote ? promote.promoteNo : null;

  document.getElementById('promoteModalTitle').textContent = promote ? '修改活動' : '新增活動';
  document.getElementById('promoteNoField').hidden = !promote;
  document.getElementById('promoteNoText').textContent = promote ? promote.promoteNo : '';

  promoteForm.promoteName.value = promote ? promote.promoteName : '';
  // 日期：表單只用年月日（slice(0, 10) 拿掉後面的時間）
  promoteForm.promoteBegin.value = promote ? promote.promoteBegin.slice(0, 10) : '';
  promoteForm.promoteEnd.value = promote ? promote.promoteEnd.slice(0, 10) : '';
  // 進行中的活動不能修改開始日期：把開始日期那一格鎖住，並顯示說明
  const beginLocked = Boolean(promote) && statusOf(promote) === '進行中';
  const beginPicker = promoteForm.querySelector('[data-date-picker="begin"]');
  if (beginLocked) beginPicker.dataset.locked = 'yes';
  else delete beginPicker.dataset.locked;
  document.getElementById('promoteBeginLockNote').hidden = !beginLocked;
  refreshDatePickers(promoteForm);   // 把按鈕上的文字更新成上面設定的日期（A-3）
  promoteForm.promoteContent.value = promote ? promote.promoteContent : '';

  // 圖片：清掉上一次選的檔案和上一次套用的圖片，預覽框顯示這筆活動目前的圖片
  promoteImgFile.value = '';
  pendingImg = null;
  appliedImg = '';
  showImgPreview(promoteImgPreview, promote ? promote.promoteImg : '');

  // 修改時顯示「活動狀態」，新增時顯示「套用已有活動」
  document.getElementById('promoteStatusField').hidden = !promote;
  document.getElementById('promoteApplyField').hidden = Boolean(promote);
  document.getElementById('promoteApplyNote').textContent = APPLY_NOTE;

  showFormError(promoteForm, '');
  showView('form');
  promoteModal.hidden = false;
  promoteForm.promoteName.focus();
}

function closePromote() {
  promoteModal.hidden = true;
  editingNo = null;
  viewingNo = null;
}

// 「＋ 新增活動」
document.getElementById('promoteAdd').addEventListener('click', () => openPromote(null));

// 卡片上的「詳細」：先打開「活動詳細」
promoteList.addEventListener('click', e => {
  const btn = e.target.closest('[data-detail-promote]');
  if (!btn) return;
  const promote = PromoteDemo.find(Number(btn.dataset.detailPromote));
  if (promote) openDetail(promote);
});

// 「活動詳細」下方的「修改」：換成表單，並帶入這筆活動目前的內容
// （已結束的活動這顆按鈕是藏起來的；這裡再檢查一次，資料庫的版本 Controller 也要做同樣的檢查）
document.getElementById('promoteToEdit').addEventListener('click', () => {
  const promote = PromoteDemo.find(viewingNo);
  if (promote && statusOf(promote) !== '已結束') openPromote(promote);
});

// 點背景、右上角的 ×、「取消」都會關閉
promoteModal.addEventListener('click', e => {
  if (e.target.closest('[data-promote-close]')) closePromote();
});
// 按 Esc 關閉
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && !promoteModal.hidden) closePromote();
});

// 「儲存」：檢查 → 存進假資料 → 關閉對話框並更新卡片
promoteForm.addEventListener('submit', e => {
  e.preventDefault();   // 假資料不用真的把表單送到後端

  let error = checkPromoteForm(promoteForm);   // 檢查的規則和資料庫的表單共用（A 區）
  // 假資料的圖片還在變成文字（通常不到一秒）：這時存檔會漏掉圖片，請使用者再按一次
  if (!error && promoteImgFile.dataset.busy) error = '圖片還在處理，請稍候再按一次儲存';
  showFormError(promoteForm, error);
  if (error) return;

  const isNew = editingNo === null;
  const original = isNew ? null : PromoteDemo.find(editingNo);
  // 表單只有年月日，存的時候補上時間：開始是那一天的 00:00，結束是那一天的 23:59（Java 存檔時也要這樣補）。
  // 進行中的活動不能改開始時間：不管表單送什麼，一律保留原本的（Java 也要這樣做，因為表單的值可以被改）
  const keepBegin = original && statusOf(original) === '進行中';
  const saved = PromoteDemo.save({
    promoteNo: editingNo,
    promoteName: promoteForm.promoteName.value.trim(),
    promoteBegin: keepBegin ? original.promoteBegin : promoteForm.promoteBegin.value + 'T00:00',
    promoteEnd: promoteForm.promoteEnd.value + 'T23:59',
    promoteContent: promoteForm.promoteContent.value.trim(),
    // 有選新的圖片就用新的；沒有選：修改時保留原本的，新增時用「套用已有活動」帶進來的（沒有套用就是沒有圖片）
    promoteImg: pendingImg !== null ? pendingImg : currentDemoImg()
  });

  // 存不進去：通常是圖片太多，超過瀏覽器給假資料的空間（接上資料庫後不會有這個問題）
  if (!saved) {
    showFormError(promoteForm, '儲存失敗：瀏覽器的暫存空間不夠，請不要放圖片，或把其他活動的圖片換掉');
    return;
  }

  closePromote();
  render();
  staffToast(isNew ? `已新增活動「${saved.promoteName}」` : `已修改活動「${saved.promoteName}」`);
});


/* ========== B-4. 套用已有活動（假資料） ==========
   新增活動時，可以從「已經結束」的活動裡挑一個，把它的活動名稱、活動內容、活動圖片帶進表單，
   再選開始、結束日期就可以儲存（會是一筆新的活動，不會改到原本那一個）。

   畫面的流程：表單的「套用已有活動」→ 畫面三（一開始是空的）→ 輸入活動名稱按「搜尋」→ 顯示結果
               → 按「套用」→ 回到表單，三個欄位已經帶好。

   ★【Thymeleaf 串接】這個功能請放到「最後再做」：其他的串接都完成、確認沒問題之後再回來做。
     資料庫的做法寫在 promote.html【資料庫資料】新增／修改表單裡，用 ▼▼▼▲▲▲ 框起來的「最後再做」那一段，
     以及 StaffPageController.java 的步驟 7。
     和這裡不一樣的地方：資料庫的版本每一步都要重新載入頁面（挑選是連結、套用也是連結），
     圖片則是「記住要從哪個活動複製」，儲存時由 Java 複製，而不是像這裡直接把圖片帶進表單。 */
const APPLY_NOTE = '可從已經結束的活動套用其活動名稱、活動內容和活動圖片。';
const promotePickList = document.getElementById('promotePickList');
const promotePickSearch = document.getElementById('promotePickSearch');

// 還沒搜尋時顯示的提示（一打開挑選的畫面，不會先列出任何活動）
const PICK_HINT = '<li class="staff-empty">請輸入活動名稱，按「搜尋」後才會列出已結束的活動</li>';

// 顯示搜尋結果：只有「已結束」的活動，名稱裡有關鍵字的，依結束時間排序（最近結束的在最上面）。
// 沒有輸入關鍵字就不查，只顯示提示。
// 資料庫的版本：有關鍵字才去查，用「已結束」的那個查詢（結束時間 早於 現在），一樣只拿前面幾筆
function renderPickList() {
  const word = promotePickSearch.value.trim().toLowerCase();
  if (!word) {
    promotePickList.innerHTML = PICK_HINT;
    return;
  }
  const ended = PromoteDemo.getAll()
    .filter(p => statusOf(p) === '已結束' && p.promoteName.toLowerCase().includes(word))
    .sort((a, b) => b.promoteEnd.localeCompare(a.promoteEnd));

  if (ended.length === 0) {
    promotePickList.innerHTML = '<li class="staff-empty">沒有符合條件的已結束活動</li>';
    return;
  }
  promotePickList.innerHTML = ended.map(p => `
    <li class="promote-pick-item">
      <span class="promote-pick-name">${escapeHtml(p.promoteName)}</span>
      <span class="promote-pick-end">${escapeHtml(showDate(p.promoteEnd))} 結束</span>
      <button type="button" class="staff-btn" data-apply-promote="${escapeHtml(p.promoteNo)}">套用</button>
    </li>`).join('');
}

// 表單的「套用已有活動」：換到畫面三。一開始搜尋框是空的，不列出任何活動，等使用者搜尋
document.getElementById('promoteApplyBtn').addEventListener('click', () => {
  promotePickSearch.value = '';
  promotePickList.innerHTML = PICK_HINT;
  document.getElementById('promoteModalTitle').textContent = '套用已有活動';
  showView('pick');
});

// 畫面三的搜尋：按「搜尋」或 Enter 才會查，並顯示結果
document.getElementById('promotePickForm').addEventListener('submit', e => {
  e.preventDefault();
  renderPickList();
});

// 回到新增的表單（什麼都不套用）
function backToAddForm() {
  document.getElementById('promoteModalTitle').textContent = '新增活動';
  showView('form');
}
document.getElementById('promotePickBack').addEventListener('click', backToAddForm);

// 按某一筆的「套用」：把名稱、內容、圖片帶進表單。開始、結束日期不帶（那是新活動自己的）
promotePickList.addEventListener('click', e => {
  const btn = e.target.closest('[data-apply-promote]');
  if (!btn) return;
  const source = PromoteDemo.find(Number(btn.dataset.applyPromote));
  if (!source) return;

  promoteForm.promoteName.value = source.promoteName;
  promoteForm.promoteContent.value = source.promoteContent || '';

  // 圖片：用那個活動的圖片（它沒有圖片就是沒有）；如果剛剛已經選了檔案，以套用的為準
  promoteImgFile.value = '';
  pendingImg = null;
  appliedImg = source.promoteImg || '';
  showImgPreview(promoteImgPreview, appliedImg);

  document.getElementById('promoteApplyNote').textContent =
    `已套用「${source.promoteName}」的活動名稱、活動內容${appliedImg ? '和活動圖片' : '（這個活動沒有圖片）'}，請再選擇開始與結束日期。`;
  showFormError(promoteForm, '');
  backToAddForm();
});


// 另一個分頁改了資料時，這裡跟著更新
PromoteDemo.onChange(render);

render();

// ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
// ▲▲▲  【假資料・刪除範圍 結束】刪到這一行為止（這一行也刪掉）。           ▲▲▲
// ▲▲▲   下面的 })(); 不是假資料，要留著。                                ▲▲▲
// ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

})();
