/* =========================================================
   Bistroops 員工後台：訂單管理（查詢所有訂單）
   對應的 HTML：templates/staff/orders.html
   需要先載入 staff.js
   內容：展開／收合明細、日期的月曆
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和其他檔案互相衝突
(() => {



// A-1. 點某一筆訂單：展開／收合餐點明細
//      明細已經由 Thymeleaf 產生在頁面上，只是一開始加了 hidden 藏起來，這裡負責切換
const dbOrderList = document.getElementById('dbOrderList');
if (dbOrderList) {
  dbOrderList.addEventListener('click', e => {
    const row = e.target.closest('[data-toggle-order]');
    if (!row) return;
    const item = row.closest('.order-item');
    const detail = item.querySelector('.order-detail');
    detail.hidden = !detail.hidden;                       // 藏著就打開，開著就藏起來
    item.classList.toggle('open', !detail.hidden);        // open 這個 class 會讓右邊的 + 轉成 ×
    row.setAttribute('aria-expanded', !detail.hidden);
  });
}

// A-2. 日期：點中間的日期打開月曆；選了日期就把表單送出（網址會帶上新的日期，由 Controller 查那一天）
//      真正的日期欄位是透明的，蓋在顯示日期的文字上面，所以點文字其實是點到它。
//        平板：點到日期欄位，系統就會自己跳出月曆。
//        電腦：點日期欄位只會把游標放進去，所以另外呼叫瀏覽器提供的 showPicker() 把月曆打開
//      Controller 要自己再檢查一次日期：沒有帶日期或格式不對就用今天，日期在今天之後也改成今天。
const dbDateInput = document.getElementById('dbDateInput');
if (dbDateInput) {
  dbDateInput.addEventListener('click', () => {
    if (typeof dbDateInput.showPicker === 'function') {
      try { dbDateInput.showPicker(); } catch (e) { /* 打不開就算了，仍然可以用鍵盤輸入 */ }
    }
  });
  dbDateInput.addEventListener('change', () => {
    if (dbDateInput.value) dbDateInput.form.submit();     // 把日期清掉的話 value 是空的，這時不送出
  });
}



})();
