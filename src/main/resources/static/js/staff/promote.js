
(() => {



document.addEventListener('submit', e => {
  const message = e.target.dataset.confirm;  
  if (message && !confirm(message)) e.preventDefault();
});


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

function dateInput(picker) {
  return picker.querySelector('[data-date-value]');
}

function getDate(form, which) {
  const picker = form.querySelector('[data-date-picker="' + which + '"]');
  return picker ? dateInput(picker).value : '';
}

function isBeginLocked(form) {
  const picker = form.querySelector('[data-date-picker="begin"]');
  return Boolean(picker && picker.dataset.locked === 'yes');
}


/* ---------- A-1. 送出前的檢查 ---------- */

function checkPromoteForm(form) {
  const name = form.promoteName.value.trim();
  const begin = getDate(form, 'begin');   // 格式是 2026-10-15（只有年月日）
  const end = getDate(form, 'end');
  const today = todayText();
  if (!name) return '請輸入活動名稱';
  if (name.length > 10) return '活動名稱最多 10 個字';
  form.querySelectorAll('[data-date-picker]').forEach(picker => {
    picker.classList.toggle('is-invalid', !dateInput(picker).value);
  });
  if (!begin) return '請選擇開始日期';
  if (!end) return '請選擇結束日期';

  if (!isBeginLocked(form) && begin < today) return '開始日期不能早於今天';
  if (end < today) return '結束日期不能早於今天';
 
  if (end < addDays(begin, 1)) return '結束日期最早是開始日期的隔天';

  const imgInput = form.querySelector('[data-img-input]');
  const sizeError = imgInput && imgInput.files ? checkImgSize(imgInput.files[0]) : '';
  if (sizeError) return sizeError;
  return '';
}


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

    dbPromoteForm.querySelectorAll('[data-date-picker]').forEach(refreshDatePicker);
    const error = checkPromoteForm(dbPromoteForm);
    showFormError(dbPromoteForm, error);
    if (error) e.preventDefault();   // 取消這一次的送出
  });
}


/* ---------- A-2. 活動圖片：選了圖片之後，顯示預覽 ----------*/

const IMG_MAX_MB = 5;
const IMG_MAX_BYTES = IMG_MAX_MB * 1024 * 1024; 

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

// 頁面上每一個選擇圖片的欄位
document.querySelectorAll('[data-img-input]').forEach(input => {
  const preview = input.closest('.promote-field').querySelector('[data-img-preview]');
  const original = preview.innerHTML;   // 預覽框一開始的內容（目前的圖片，或「尚無圖片」）

  input.addEventListener('change', () => {
    const file = input.files[0];

    // 打開選檔案的視窗又按了取消：預覽框回到一開始的內容
    if (!file) { preview.innerHTML = original; return; }


    if (!file.type.startsWith('image/')) {
      input.value = '';                 // 清掉選到的檔案
      preview.innerHTML = original;
      if (input.form) showFormError(input.form, '請選擇圖片檔案（JPG、PNG 等）');
      return;
    }

    // 檔案太大（超過 5MB）：不接受這個檔案，顯示警示
    const sizeError = checkImgSize(file);
    if (sizeError) {
      input.value = '';             
      preview.innerHTML = original;
      if (input.form) showFormError(input.form, sizeError);
      return;
    }

    showImgPreview(preview, URL.createObjectURL(file));
    if (input.form) showFormError(input.form, '');
  });
});


//製作月曆
const WEEKDAYS = '日一二三四五六';

// '2026-10-15' → '2026/10/15（四）'
function dateLabel(dateText) {
  const [y, m, d] = dateText.split('-').map(Number);
  return dateText.split('-').join('/') + '（' + WEEKDAYS[new Date(y, m - 1, d).getDay()] + '）';
}


function minDateOf(picker) {
  const today = todayText();
  if (picker.dataset.datePicker !== 'end') return today;   
  const form = picker.closest('form');
  const begin = form ? getDate(form, 'begin') : '';
  if (!begin) return today;
  const afterBegin = addDays(begin, 1);
  return afterBegin > today ? afterBegin : today;
}


function refreshDatePicker(picker) {
  const input = dateInput(picker);
  const button = picker.querySelector('[data-date-toggle]');


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


function placeDatePanel(picker) {
  const panel = picker.querySelector('[data-date-panel]');

  const button = picker.querySelector('[data-date-toggle]').getBoundingClientRect();
  const GAP = 6;    
  const MARGIN = 10; 

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


  if (!panel.hidden) placeDatePanel(picker);
}

function openDatePanel(picker) {
  document.querySelectorAll('[data-date-picker]').forEach(other => { if (other !== picker) closeDatePanel(other); });
  const value = dateInput(picker).value;
  const min = minDateOf(picker);
  const [y, m] = (value && value >= min ? value : min).split('-').map(Number);

  picker.querySelector('[data-date-panel]').hidden = false;
  picker.classList.add('is-open');
  drawDatePanel(picker, y, m - 1);
}


function closeAllDatePanels() {
  document.querySelectorAll('[data-date-picker].is-open').forEach(closeDatePanel);
}


function refreshDatePickers(root) {
  root.querySelectorAll('[data-date-picker]').forEach(picker => {
    closeDatePanel(picker);
    refreshDatePicker(picker);
  });
}

document.addEventListener('click', e => {

  const toggle = e.target.closest('[data-date-toggle]');
  if (toggle) {
    const picker = toggle.closest('[data-date-picker]');
    if (picker.querySelector('[data-date-panel]').hidden) openDatePanel(picker);
    else closeDatePanel(picker);
    return;
  }

  const nav = e.target.closest('[data-date-nav]');
  if (nav) {
    const picker = nav.closest('[data-date-picker]');
    const panel = picker.querySelector('[data-date-panel]');
    const target = new Date(Number(panel.dataset.year), Number(panel.dataset.month) + Number(nav.dataset.dateNav), 1);
    drawDatePanel(picker, target.getFullYear(), target.getMonth());
    return;
  }

  const day = e.target.closest('[data-date-day]');
  if (day) {
    const picker = day.closest('[data-date-picker]');
    const form = picker.closest('form');
    dateInput(picker).value = day.dataset.dateDay;
    closeDatePanel(picker);
    refreshDatePicker(picker);


    if (picker.dataset.datePicker === 'begin' && form) {
      const endPicker = form.querySelector('[data-date-picker="end"]');
      const endInput = dateInput(endPicker);
      if (endInput.value && endInput.value < minDateOf(endPicker)) endInput.value = '';
      refreshDatePicker(endPicker);
    }
    if (form) showFormError(form, '');
    return;
  }

  if (!e.target.closest('[data-date-picker]')) closeAllDatePanels();
});

// 月曆是固定浮在畫面上的，按鈕的位置一變（捲動頁面、平板轉向、視窗大小改變），

window.addEventListener('scroll', closeAllDatePanels, true);
window.addEventListener('resize', closeAllDatePanels);
// 按 Esc 也關掉月曆
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') closeAllDatePanels();
});

refreshDatePickers(document);



})();
