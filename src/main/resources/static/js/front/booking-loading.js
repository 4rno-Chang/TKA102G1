/* =========================================================
   Bistroops 前台：表單送出中（防止重複點擊）
   對應的 CSS：static/css/front/booking-loading.css

   送出後：
     1. 按下去的那一顆按鈕鎖住，裡面換成「餐盤蓋等待圖示＋處理中…」（寬度維持原本的，旁邊的東西不會跟著動）
     2. 表單所在的區塊蓋一層 20% 的白色，並加上 inert：滑鼠、手指、鍵盤都不能再改內容
        （不能用 disabled：disabled 的欄位不會被送出，下一頁會收不到資料；inert 不影響送出的內容）
     蓋住的區塊都不包含照片（點餐的菜品照片在表單外面）。

   用法
     A. 一般表單（送出後會換頁）：<form> 加上 data-loading
          data-loading                      蓋住表單所在的 .booking-card；不在卡片裡就蓋住表單本身
          data-loading=".order-sheet-panel" 蓋住離表單最近的那個區塊（寫 CSS 選擇器）
        用在：預約訂位（下一步、查詢訂位、確認送出訂位、儲存變更）、現場取號、查詢候位號碼、取消候位、
              候位管理（叫下一號、執行操作）、點餐的「送出訂單」、掃 QR 的用餐回饋
     B. 背景送出、不換頁的表單（點餐頁的購物車，order.js 的 A-8）：自己呼叫
          bookingLock(按鈕, 要蓋住的區塊, 文字)   文字給 '' 就只顯示圖示（例如數量的 −、＋）
          bookingUnlock(按鈕, 區塊)              伺服器回應之後解開

   各頁自己的檢查、確認視窗（時段已額滿、手機格式錯誤、按了「取消」…）擋下來的，不會進入送出中。
   ========================================================= */

const LOADER_ICON = '<span class="cloche-loader"><i></i></span>';

// 鎖住按鈕、蓋住區塊
function bookingLock(btn, area, text = '處理中…') {
  if (btn && !btn.classList.contains('is-loading')) {
    btn.dataset.html = btn.innerHTML;             // 記住原本的內容，解開時還原
    btn.style.width = btn.offsetWidth + 'px';     // 固定成原本的寬度
    btn.classList.add('is-loading');
    if (!text) btn.classList.add('is-icon-only');
    btn.innerHTML = LOADER_ICON + text;
  }
  if (area && !area.classList.contains('is-submitting')) {
    // 白色那一層要貼著這個區塊，區塊本身沒有定位的話先暫時加上 relative
    if (getComputedStyle(area).position === 'static') {
      area.style.position = 'relative';
      area.dataset.posAdded = 'yes';
    }
    area.classList.add('is-submitting');
    area.inert = true;
  }
}

// 解開（背景送出完成，或按「上一頁」回來時）
function bookingUnlock(btn, area) {
  if (btn && btn.classList.contains('is-loading')) {
    btn.innerHTML = btn.dataset.html;
    delete btn.dataset.html;
    btn.style.width = '';
    btn.classList.remove('is-loading', 'is-icon-only');
  }
  if (area && area.classList.contains('is-submitting')) {
    area.classList.remove('is-submitting');
    area.inert = false;
    if (area.dataset.posAdded) {
      area.style.position = '';
      delete area.dataset.posAdded;
    }
  }
}

(() => {
  // 監聽 window 而不是 form：排在各頁自己的檢查之後執行，被擋下來的 e.defaultPrevented 是 true
  window.addEventListener('submit', e => {
    const form = e.target;
    if (!form.matches('form[data-loading]') || e.defaultPrevented) return;

    if (form.dataset.submitting) {   // 已經送出了：再送一次直接擋掉
      e.preventDefault();
      return;
    }
    form.dataset.submitting = 'yes';

    const btn = e.submitter || form.querySelector('[type="submit"]');
    const selector = form.dataset.loading;
    const area = (selector && form.closest(selector)) || form.closest('.booking-card') || form;
    bookingLock(btn, area);
  });

  // 按「上一頁」回來時，瀏覽器可能直接拿出之前的畫面（還在處理中），要恢復成可以操作
  window.addEventListener('pageshow', e => {
    if (!e.persisted) return;
    document.querySelectorAll('form[data-submitting]').forEach(form => delete form.dataset.submitting);
    document.querySelectorAll('.is-loading').forEach(btn => bookingUnlock(btn, null));
    document.querySelectorAll('.is-submitting').forEach(area => bookingUnlock(null, area));
  });
})();
