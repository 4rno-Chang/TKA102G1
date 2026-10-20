/* =========================================================
   Bistroops 員工後台：活動管理（查詢、詳細、新增、修改、刪除活動）
   對應的 HTML（都在 templates/staff/promote/，兩頁共用這一個檔案）：
     promote_list.html   列表頁：查詢、卡片、分頁、「活動詳細」的對話框
     promote_form.html   表單頁：新增／修改活動的表單、「套用已有活動」的對話框
   對應的 CSS ：static/css/staff/promote.css
   要先載入 staff.js（左側選單、時鐘、「送出前先確認」等共用的功能在那裡）

   畫面上的資料都是 Controller 從資料庫查出來、用 Thymeleaf 顯示的，查詢、換頁、看詳細、套用都是連結或表單，
   不需要 JS。這個檔案只做「瀏覽器這一邊才做得到」的事：
     1. 送出前先確認（列表頁的「刪除」）──★ 程式不在這裡，在 staff.js，說明見下面
     2. 送出表單前先檢查一次，有問題就把訊息顯示在表單上方、不送出
     3. 選了圖片之後，檢查檔案並在預覽框顯示新選的那一張
     4. 日期選擇：自己做的月曆（只選年月日，不能選今天以前，結束日期最早是開始日期的隔天）
     5. 活動詳細的對話框：打開、關閉都不重新載入整頁（列表頁）
     6. 整單折數：畫面上打「幾折」，換算成百分比（10～90）送出（表單頁）
   2～4 是表單頁用的，列表頁用不到。頁面上沒有對應的東西時，那一段什麼都不會做。

   ★ 這些檢查只是方便使用者，可以被跳過，所以 Java（PromoteController、PromoteService）收到資料後都會再檢查一次。
     規則有改的話，這裡和 Java 兩邊要一起改。
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和其他檔案互相衝突
(() => {


/* ---------- 1. 送出前先確認（列表頁「活動詳細」的「刪除」） ----------
   ★ 這個功能的程式在 staff.js（A-7. 送出前先確認），後台每一頁都會載入它，所以這裡「不用、也不能」再寫一次：
     HTML 的表單寫了 data-confirm="要問的話"，按下去送出之前 staff.js 就會跳出確認的視窗，按「取消」就不送出。
     這裡如果再寫一段一樣的程式，同一次送出會被問兩次（之前確認的視窗跳兩次就是這個原因）。
   之後別的表單也想先確認再送出，在 <form> 加上 data-confirm 就可以了，JS 不用改。 */


/* ---------- 日期的小工具（2 的檢查和 4 的月曆都會用到） ----------
   活動的開始、結束只選「年月日」，不選幾點幾分。月曆和這裡的檢查用的日期是 '2026-10-15' 這種文字。
   文字的格式都一樣（年-月-日，月和日都補成兩位數），所以直接比文字的大小就等於比日期的先後。

   ★ 送到 Controller 的是「完整的時間」：開始是那一天的 00:00、結束是那一天的 23:59，
     文字長這樣：2026-10-15T00:00、2026-10-16T23:59（中間的 T 是日期和時間的分隔）。
     這樣 Controller 才能直接用 PromoteVO 接（VO 的開始、結束是 LocalDateTime）。
     所以一個日期欄位有兩個隱藏欄位：
       data-date-value   只有年月日，給月曆和檢查用，沒有 name，不會送出
       data-date-full    完整的時間，name 是 promoteBegin／promoteEnd，會送出；下面的 refreshDatePicker 會讓它跟著日期更新 */

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
// 一個日期欄位（picker）裡「只有年月日」的那個隱藏欄位（HTML 上有 data-date-value）
function dateInput(picker) {
  return picker.querySelector('[data-date-value]');
}
// 讀表單裡選好的日期：which 是 'begin'（開始）或 'end'（結束）。回傳 '2026-10-15'，還沒選是空字串
function getDate(form, which) {
  const picker = form.querySelector('[data-date-picker="' + which + '"]');
  return picker ? dateInput(picker).value : '';
}
// 開始日期是不是被鎖住（進行中的活動不能改開始日期，HTML 會在那一格加上 data-locked="yes"）
function isBeginLocked(form) {
  const picker = form.querySelector('[data-date-picker="begin"]');
  return Boolean(picker && picker.dataset.locked === 'yes');
}


/* ---------- 2. 送出前的檢查 ---------- */

// 檢查表單的內容。沒問題回傳空字串，有問題回傳要顯示的訊息。
// 活動名稱用 name 取得（form.promoteName）；開始、結束日期用上面的 getDate 讀「只有年月日」的那個欄位
// ★ 這些規則 Java 存檔前也要再檢查一次（頁面上的檢查可以被跳過）
function checkPromoteForm(form) {
  const name = form.promoteName.value.trim();
  const begin = getDate(form, 'begin');   // 格式是 2026-10-15（只有年月日）
  const end = getDate(form, 'end');
  const today = todayText();
  if (!name) return '請輸入活動名稱';
  if (name.length > 10) return '活動名稱最多 10 個字';
  // 開始、結束日期都是必填：沒選的那一格標成紅框（promote.css 的 .promote-date.is-invalid），選了之後紅框會自己消失
  form.querySelectorAll('[data-date-picker]').forEach(picker => {
    picker.classList.toggle('is-invalid', !dateInput(picker).value);
  });
  if (!begin) return '請選擇開始日期';
  if (!end) return '請選擇結束日期';
  // 開始日期不能早於今天。進行中的活動開始日期是鎖住的（本來就在今天以前），所以不檢查
  if (!isBeginLocked(form) && begin < today) return '開始日期不能早於今天';
  if (end < today) return '結束日期不能早於今天';
  // 結束日期最早是開始日期的隔天（不能同一天）。addDays(begin, 1) 就是開始日期的隔天
  if (end < addDays(begin, 1)) return '結束日期最早是開始日期的隔天';
  // 圖片的大小：選檔案的時候已經檢查過一次（3），送出前再確認一次比較保險
  const imgInput = form.querySelector('[data-img-input]');
  const sizeError = imgInput && imgInput.files ? checkImgSize(imgInput.files[0]) : '';
  if (sizeError) return sizeError;
  // 整單優惠（6）：沒勾就不檢查；有勾就一定要填好折數（1～9 折或兩位數的 11～89 折）。有錯時把收合的區塊展開
  const discToggle = form.querySelector('[data-disc-toggle]');
  if (discToggle && discToggle.checked) {
    const disc = parseDisc(form.querySelector('[data-disc-input]').value);
    const discError = disc.error || (disc.value === null ? '請填寫整單折數，或取消勾選「有整單優惠」' : '');
    if (discError) {
      delete discField.dataset.folded;
      refreshDiscField();
      return discError;
    }
  }
  return '';
}

// 把訊息顯示在表單上方（HTML 裡有 data-form-error 的那一行）；text 是空的就把那一行藏起來
function showFormError(form, text) {
  const box = form.querySelector('[data-form-error]');
  if (!box) return;
  box.textContent = text;
  box.hidden = !text;
}

// 新增／修改的表單（promote_form.html 的 <form id="promoteForm">）：送出前先檢查，有問題就不送出。
// 列表頁沒有這個表單，promoteForm 會是 null，就什麼都不做
const promoteForm = document.getElementById('promoteForm');
if (promoteForm) {
  promoteForm.addEventListener('submit', e => {
    // 送出前再確認一次「完整的時間」和選的日期一致（平常選日期的時候就會更新，這裡是保險）
    promoteForm.querySelectorAll('[data-date-picker]').forEach(refreshDatePicker);
    const error = checkPromoteForm(promoteForm);
    showFormError(promoteForm, error);
    if (error) e.preventDefault();   // 取消這一次的送出
  });
}


/* ---------- 3. 活動圖片：選了圖片之後，檢查並顯示預覽 ----------
   活動圖片會顯示在首頁的輪播，輪播的框是 16:9，圖片用 CSS 拉開填滿（object-fit: fill）：
   其他比例的圖片會被拉成 16:9（會變形），不留白、不裁切。
   表單裡的預覽框用的是同樣的 CSS，所以選了圖片之後，在預覽框看到的就是它在首頁上的樣子。

   選到的檔案會先檢查兩件事，不通過就不接受、並在表單上方顯示警示：
     1. 是不是圖片
     2. 有沒有超過 5MB（後端最多收 10MB，前端設得小一點，見下面的 IMG_MAX_MB）
   通過之後才「把新選的圖片放進預覽框」。圖片檔案本身不會被改動：
   按「儲存」送到 Controller、存進資料庫的，是使用者選的原本的圖片。

   HTML 的寫法：
     預覽框   <div class="promote-img-box" data-img-preview>
     檔案欄位 <input type="file" name="upImg" data-img-input>
     兩個要放在同一個 <div class="promote-field"> 裡面，這裡才找得到對方。 */

// 圖片檔案最大可以多大。
// 後端（application.properties 的 spring.servlet.multipart.max-file-size）設定的是 10MB，超過的話伺服器會直接出錯，
// 使用者只會看到錯誤頁。所以前端先擋，而且設得比後端小一點（5MB）留點空間：
// 表單送出時除了圖片還有其他欄位，整個請求會比圖片本身再大一些。
// ★ 這個數字和後端的設定要一起看：後端如果調小，這裡也要跟著調到比它更小。
const IMG_MAX_MB = 5;
const IMG_MAX_BYTES = IMG_MAX_MB * 1024 * 1024;   // 換算成位元組（1MB = 1024 × 1024 位元組），檔案的大小 file.size 是用位元組算的

// 檢查圖片檔案的大小。沒問題回傳空字串，太大回傳要顯示的訊息
function checkImgSize(file) {
  if (!file || file.size <= IMG_MAX_BYTES) return '';
  const sizeMB = (file.size / 1024 / 1024).toFixed(1);   // 這個檔案是幾 MB，取到小數一位
  return `圖片檔案太大（${sizeMB}MB），最大只能上傳 ${IMG_MAX_MB}MB，請換一張或先壓縮圖片`;
}

// 把預覽框的內容換成一張圖片；src 是空的就顯示「尚無圖片」
function showImgPreview(box, src) {
  box.textContent = '';
  if (!src) { box.textContent = '尚無圖片'; return; }
  const img = document.createElement('img');
  img.src = src;
  img.alt = '圖片預覽';
  box.appendChild(img);
}

// 頁面上每一個選擇圖片的欄位（有 data-img-input 的）都套用
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

    // 檔案太大（超過 5MB）：不接受這個檔案，顯示警示
    const sizeError = checkImgSize(file);
    if (sizeError) {
      input.value = '';                 // 清掉選到的檔案，這樣按「儲存」時不會把它送出去
      preview.innerHTML = original;
      if (input.form) showFormError(input.form, sizeError);
      return;
    }

    // URL.createObjectURL 會替這個檔案產生一個暫時的網址，<img> 用它就能顯示還沒上傳的圖片
    showImgPreview(preview, URL.createObjectURL(file));
    if (input.form) showFormError(input.form, '');
  });
});


/* ---------- 4. 日期選擇：自己做的月曆 ----------
   不用瀏覽器內建的日期欄位（每個瀏覽器長得不一樣，也不好限制），改成自己畫的月曆。
   月曆是「跳出來浮在畫面上」的小視窗，不佔版面的位置：按日期的按鈕就出現在按鈕旁邊，
   選了日期、點到別的地方、或捲動畫面時會自己關掉。

   HTML 的寫法：
     <div class="promote-date" data-date-picker="begin">      ← begin 是開始日期，end 是結束日期
       <input type="hidden" data-date-value>                  ← 選到的日期，格式 2026-10-15（不會送出）
       <input type="hidden" name="promoteBegin" data-date-full>   ← 真正送出去的值，格式 2026-10-15T00:00（結束是 T23:59）
       <button type="button" class="promote-date-btn" data-date-toggle><span data-date-text></span>…</button>
       <div class="promote-date-panel" data-date-panel hidden></div>   ← 月曆（跳出來的小視窗），內容和位置由這裡決定
     </div>

   可以選的範圍：
     開始日期：不能早於今天
     結束日期：不能早於今天，而且最早是開始日期的隔天（不能和開始日期同一天）
   選了開始日期之後，如果原本的結束日期變成不能選的，會把結束日期清掉，請使用者重選。
   進行中的活動不能改開始日期：那一格有 data-locked="yes"，按鈕會鎖住。

   ★ 這裡只是讓使用者「選不到」不對的日期。隱藏欄位的值還是可以被改，所以 Java 存檔前要用同樣的規則再檢查一次。
   ★ 送到 Controller 的時間是這裡補的（開始 00:00、結束 23:59），一樣可以被改；
     Java 存檔前要自己再把時間設成 00:00 和 23:59，不要直接相信送來的幾點幾分。 */
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
  const begin = form ? getDate(form, 'begin') : '';
  if (!begin) return today;
  const afterBegin = addDays(begin, 1);   // 開始日期的隔天
  return afterBegin > today ? afterBegin : today;
}

// 依照選到的日期，更新按鈕上的文字、鎖住的狀態，以及要送出的「完整的時間」
function refreshDatePicker(picker) {
  const input = dateInput(picker);
  const button = picker.querySelector('[data-date-toggle]');

  // 要送出的完整時間：開始日期補 T00:00，結束日期補 T23:59；還沒選日期就是空的。
  // 鎖住的開始日期（進行中的活動）不動它，維持 Thymeleaf 放進去的原本的時間。
  // （HTML 裡萬一沒有這個欄位，full 會是 null，就不用處理）
  const full = picker.querySelector('[data-date-full]');
  if (full && picker.dataset.locked !== 'yes') {
    const time = picker.dataset.datePicker === 'end' ? 'T23:59' : 'T00:00';
    full.value = input.value ? input.value + time : '';
  }

  picker.querySelector('[data-date-text]').textContent = input.value ? dateLabel(input.value) : '請選擇日期';
  picker.classList.toggle('is-empty', !input.value);
  if (input.value) picker.classList.remove('is-invalid');   // 選好日期了，拿掉「必填沒選」的紅框
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
  const value = dateInput(picker).value;
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
  const value = dateInput(picker).value;
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
    dateInput(picker).value = day.dataset.dateDay;
    closeDatePanel(picker);
    refreshDatePicker(picker);

    // 改了開始日期：結束日期如果變成不能選的（比開始日期早），就清掉請使用者重選
    if (picker.dataset.datePicker === 'begin' && form) {
      const endPicker = form.querySelector('[data-date-picker="end"]');
      const endInput = dateInput(endPicker);
      if (endInput.value && endInput.value < minDateOf(endPicker)) endInput.value = '';
      refreshDatePicker(endPicker);
    }
    if (form) showFormError(form, '');
    return;
  }

  // (4) 點到月曆以外的地方：把打開的月曆關掉
  if (!e.target.closest('[data-date-picker]')) closeAllDatePanels();
});

// 月曆是固定浮在畫面上的，按鈕的位置一變（捲動頁面、平板轉向、視窗大小改變），
// 月曆就會和按鈕對不上，所以這些時候直接把它關掉，要選再按一次。
// 第三個參數 true：頁面裡面「可以自己捲動的區塊」捲動時也收得到（一般的寫法只收得到整個頁面的捲動）
window.addEventListener('scroll', closeAllDatePanels, true);
window.addEventListener('resize', closeAllDatePanels);
// 按 Esc 也關掉月曆
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') closeAllDatePanels();
});

// 頁面載入時：Thymeleaf 已經把日期放進隱藏欄位了，先把按鈕上的文字顯示出來，並把要送出的完整時間準備好
refreshDatePickers(document);


/* ---------- 6. 整單優惠：勾選後展開折數和條件、可以收合（表單頁） ----------
   ・沒勾「有整單優惠」：折數的隱藏欄位送空的、條件欄位鎖住不送 → Java 收到 null＝不是整單折扣活動
   ・折數：數字輸入框（type="number"），1～9、每次 0.1，可以打字或按上下箭頭；乘以 10 放進隱藏欄位 promoteDisc
     8＝8 折（存 80）、8.5＝85 折（存 85）、8.8＝88 折（存 88）、1＝1 折（存 10）。資料庫存 10～90
     習慣打「85」「88」的話，離開輸入框時會自動換成 8.5、8.8
   ・條件：折數填好才能寫（鎖住的欄位不會送出）；有折數可以沒有條件
   ・收合：只是把區塊摺起來、旁邊顯示摘要，填好的資料不會不見（收合的狀態記在 data-folded）
     收合或沒勾時，折數輸入框也鎖住：藏起來的欄位如果填錯，瀏覽器會擋住送出卻沒辦法提示
   ★ Java 存檔前也要再檢查一次（10～90 或 null），頁面上的檢查可以被跳過。 */
const DISC_HINT = '1～9 折，每次 0.1（8.5＝85 折）';
const DISC_ERROR = '整單折數請輸入 1～9，小數點最多一位（例如 8、8.5、8.8）；也可以按右邊的上下箭頭';

// 把輸入框的數字換算成百分比：回傳 { value: 10～90 或 null（空白）, error: 有錯時的訊息 }
function parseDisc(text) {
  const t = String(text).trim();
  if (t === '') return { value: null, error: '' };
  const n = Number(t);
  // 1～9，而且小數點最多一位（乘以 10 是整數；用四捨五入比對，避免 8.3 × 10 算出 83.00000000000001 這種誤差）
  if (!(n >= 1 && n <= 9 && Math.abs(n * 10 - Math.round(n * 10)) < 1e-9)) return { value: null, error: DISC_ERROR };
  return { value: Math.round(n * 10), error: '' };   // 8.5 → 85
}
// 百分比換成台灣的說法（結果、摘要用）：80 → "8"、85 → "85"
const discText = v => String(v % 10 === 0 ? v / 10 : v);

const discField = document.querySelector('[data-disc-field]');

// 依目前的狀態更新整個區塊（有沒有勾、有沒有收合、折數對不對）。每次有變動都呼叫它
function refreshDiscField() {
  if (!discField) return;
  const toggle = discField.querySelector('[data-disc-toggle]');
  const input = discField.querySelector('[data-disc-input]');
  const rule = discField.querySelector('[data-rule-input]');
  const result = discField.querySelector('[data-disc-result]');
  const summary = discField.querySelector('[data-disc-summary]');
  const fold = discField.querySelector('[data-disc-fold]');

  const disc = parseDisc(input.value);
  const on = toggle.checked;
  const folded = discField.dataset.folded === 'yes';

  // 真正送出的折數：沒勾、或折數不對 → 空的
  discField.querySelector('[data-disc-value]').value = on && disc.value ? disc.value : '';
  // 折數輸入框：沒勾或收合時鎖住（理由見上面的說明）
  input.disabled = !on || folded;
  // 條件：有勾、而且折數填好才能寫
  rule.disabled = !(on && disc.value);
  rule.placeholder = rule.disabled ? '請先填寫整單折數' : '例如：消費滿 1000 元、出示學生證';

  result.classList.toggle('is-error', !!disc.error);
  result.textContent = disc.error ? DISC_ERROR : disc.value ? `＝ ${discText(disc.value)} 折` : DISC_HINT;

  // 展開、收合
  discField.querySelector('[data-disc-panel]').hidden = !on || folded;
  fold.hidden = !on;
  fold.textContent = folded ? '展開' : '收合';
  summary.hidden = !(on && folded);
  const ruleText = rule.value.trim() ? '條件：' + rule.value.trim() : '無條件';
  summary.textContent = disc.value ? `${discText(disc.value)} 折・${ruleText}` : '尚未填寫折數';
}

if (discField) {
  const discInput = discField.querySelector('[data-disc-input]');
  const discValue = discField.querySelector('[data-disc-value]');
  // 修改活動時：隱藏欄位已經有存的百分比（Thymeleaf 放的），除以 10 顯示在輸入框（85 → 8.5）
  if (discValue.value) discInput.value = Number(discValue.value) / 10;

  // 打字、按上下箭頭（折數、條件）、勾選
  discField.addEventListener('input', e => {
    if (e.target.matches('[data-disc-toggle]')) delete discField.dataset.folded;   // 打勾時一定是展開的
    refreshDiscField();
  });
  // 離開折數輸入框時：打了「85」「88」這種台灣說法，自動換成 8.5、8.8
  discInput.addEventListener('change', () => {
    const n = Number(discInput.value);
    if (n >= 10 && n <= 90 && Number.isInteger(n)) discInput.value = n / 10;
    refreshDiscField();
  });
  // 收合／展開
  discField.querySelector('[data-disc-fold]').addEventListener('click', () => {
    if (discField.dataset.folded === 'yes') delete discField.dataset.folded;
    else discField.dataset.folded = 'yes';
    refreshDiscField();
  });
  refreshDiscField();
}


/* ---------- 5. 活動詳細的對話框：打開、關閉都不重新載入整頁（列表頁） ----------
   卡片的「詳細」（data-detail-open）：在背景向它的網址（/staff/promote/detail?promoteNo=…）要頁面，
     只取出裡面的對話框（.promote-modal）放到目前的畫面上，卡片和分頁都不動。
   對話框的 ×、「關閉」、背景（data-detail-close）、Esc：直接把對話框拿掉，不用連到伺服器。
   網址：打開時改成「詳細」的網址、關閉時改回列表的網址（history.pushState，只改網址、不重新載入），所以：
     開著對話框按 F5、把網址貼給別人，一樣會打開那個活動；按瀏覽器的「上一頁」就是關閉對話框。
   背景要不到（登入過期、斷線、活動已經被刪掉…）：照原本的連結整頁換過去，功能不會壞。
   對話框裡的「刪除」「修改」照原本的做法（表單送出、連結換頁）。
   只在列表頁（有「詳細」按鈕的頁面）啟用：表單頁的「套用已有活動」也是 .promote-modal，不能被這裡關掉。 */
const DETAIL_PATH = '/promote/detail';
const onListPage = document.querySelector('[data-detail-open]') !== null || location.pathname.includes(DETAIL_PATH);
let detailLoading = false;

// 一般的左鍵點擊才自己處理；按著 Ctrl／Shift 點（想開新分頁）就交給瀏覽器
const plainClick = e => e.button === 0 && !e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey;

function removeDetail() {
  const modal = document.querySelector('.promote-modal');
  if (modal) modal.remove();
}

// 在背景要「詳細」的頁面，取出對話框放上來；push 是 true 時順便把網址改成詳細的網址
async function showDetail(url, push) {
  if (detailLoading) return;
  detailLoading = true;
  try {
    const res = await fetch(url);
    // 被轉到別的頁面（res.url 不是詳細的網址）：例如登入過期到登入頁、活動不存在回到列表
    if (!res.ok || !res.url.includes(DETAIL_PATH)) throw new Error(res.status);
    const doc = new DOMParser().parseFromString(await res.text(), 'text/html');
    const modal = doc.querySelector('.promote-modal');
    if (!modal) throw new Error('沒有對話框');
    removeDetail();
    document.body.insertBefore(modal, document.getElementById('staffToast'));
    if (push) history.pushState({ promoteDetail: true }, '', url);
    const close = modal.querySelector('.promote-close');
    if (close) close.focus();   // 用鍵盤的人可以直接按 Enter 關閉
  } catch (err) {
    location.href = url;        // 背景要不到：照原本的連結整頁換過去
  } finally {
    detailLoading = false;
  }
}

// 關閉：如果是這次用 JS 打開的，就「上一頁」（網址回到列表，下面 popstate 會拿掉對話框）；
//       一開始就打開著的（例如 F5），就把網址改成列表的網址再拿掉
function closeDetail(listUrl) {
  if (!document.querySelector('.promote-modal')) return;
  if (history.state && history.state.promoteDetail) {
    history.back();
  } else {
    if (listUrl) history.pushState({}, '', listUrl);
    removeDetail();
  }
}

if (onListPage) {
document.addEventListener('click', e => {
  const opener = e.target.closest('[data-detail-open]');
  if (opener && plainClick(e)) {
    e.preventDefault();
    showDetail(opener.href, true);
    return;
  }
  const closer = e.target.closest('.promote-modal [data-detail-close]');
  if (closer && plainClick(e)) {
    e.preventDefault();
    closeDetail(closer.href);
  }
});

// Esc：關閉對話框（上面還開著確認刪除的小視窗時不處理，那個視窗自己會擋 Esc）
document.addEventListener('keydown', e => {
  if (e.key !== 'Escape' || document.querySelector('dialog[open]')) return;
  const closer = document.querySelector('.promote-modal [data-detail-close]');
  if (closer) closeDetail(closer.href);
});

// 瀏覽器的「上一頁」「下一頁」：網址是詳細的就打開那一筆，不是就關掉對話框
window.addEventListener('popstate', () => {
  if (location.pathname.includes(DETAIL_PATH)) showDetail(location.href, false);
  else removeDetail();
});
}


/* ---------- 7. 套用已有活動：打開、搜尋、套用、關閉都不重新載入整頁（表單頁） ----------
   promote_form.html 的連結和表單本來都會換頁（沒有 JS 也能用），這裡把它們攔下來改成在背景做，頁面位置不動：
     「套用已有活動」（data-pick-open）、搜尋（data-pick-search）：在背景要那個網址的頁面，只取出對話框（.promote-modal）放上來
     ×、背景、← 返回（data-pick-close）、Esc：直接拿掉對話框
     「套用」（data-pick-apply）：在背景要 copyFrom 的網址（Controller 已經把來源活動放進表單），
        只把活動名稱、活動內容、整單優惠（折數和條件）填進目前的表單；使用者選好的日期、圖片都不動
   背景要不到（登入過期、斷線…）：照原本的連結整頁換過去，功能不會壞。
   ★ 整單優惠要 PromoteController.addPromote 也把來源活動的折數、條件放進 promoteForm 才會帶過來 */
if (document.querySelector('[data-pick-open]')) {
let pickLoading = false;

// 在背景要頁面，回傳整理好的 HTML；被轉到別的頁面（例如登入頁）就當作失敗
async function fetchFormPage(url) {
  const res = await fetch(url);
  if (!res.ok || !res.url.includes('/promote/add')) throw new Error(res.status);
  return new DOMParser().parseFromString(await res.text(), 'text/html');
}

const closePick = () => {
  const modal = document.querySelector('.promote-modal');
  if (modal) modal.remove();
};

// 打開或搜尋：換上新的對話框
async function showPick(url) {
  if (pickLoading) return;
  pickLoading = true;
  try {
    const modal = (await fetchFormPage(url)).querySelector('.promote-modal');
    if (!modal) throw new Error('沒有對話框');
    closePick();
    document.body.insertBefore(modal, document.getElementById('staffToast'));
    const input = modal.querySelector('[name="pickKeyword"]');
    if (input) {
      input.focus({ preventScroll: true });                     // 游標放在搜尋框，畫面不要跟著捲動
      input.setSelectionRange(input.value.length, input.value.length);
    }
  } catch (err) {
    location.href = url;
  } finally {
    pickLoading = false;
  }
}

// 套用：把來源活動的名稱、內容、整單優惠填進目前的表單
async function applyPick(url) {
  if (pickLoading) return;
  pickLoading = true;
  try {
    const src = (await fetchFormPage(url)).getElementById('promoteForm');
    if (!src) throw new Error('沒有表單');
    const form = document.getElementById('promoteForm');
    const name = src.querySelector('#promoteName').value;
    form.querySelector('#promoteName').value = name;
    form.querySelector('#promoteContent').value = src.querySelector('#promoteContent').value;
    // 整單優惠：來源有折數就打勾並帶入（百分比除以 10 放進輸入框），沒有就取消勾選
    const disc = src.querySelector('[data-disc-value]').value;
    form.querySelector('[data-disc-toggle]').checked = disc !== '';
    form.querySelector('[data-disc-input]').value = disc ? Number(disc) / 10 : '';
    form.querySelector('[data-rule-input]').value = src.querySelector('[data-rule-input]').value;
    delete discField.dataset.folded;
    refreshDiscField();
    closePick();
    staffToast(`已套用「${name}」`);
  } catch (err) {
    location.href = url;
  } finally {
    pickLoading = false;
  }
}

document.addEventListener('click', e => {
  const link = e.target.closest('[data-pick-open], [data-pick-close], [data-pick-apply]');
  if (!link || !plainClick(e)) return;
  e.preventDefault();
  if (link.matches('[data-pick-open]')) showPick(link.href);
  else if (link.matches('[data-pick-close]')) closePick();
  else applyPick(link.href);
});

// 搜尋：用表單的欄位組出網址，在背景要
document.addEventListener('submit', e => {
  const form = e.target.closest('[data-pick-search]');
  if (!form) return;
  e.preventDefault();
  showPick(form.action + '?' + new URLSearchParams(new FormData(form)));
});

// Esc：關閉對話框（有其他小視窗開著時不處理）
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && !document.querySelector('dialog[open]')) closePick();
});
}

})();
