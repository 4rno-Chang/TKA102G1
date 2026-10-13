/* =========================================================
   Bistroops 前台：常見問題的點擊次數（首頁 frontend_index.html、常見問題頁 qa/listAll.html）
   展開一題時，送一個 POST 到那一題的 data-click-url（/bistroops/qa/{編號}/click），點擊次數 +1
   收起不算；每次展開都算。只送出次數，不改畫面；送出失敗也不影響展開。
   網址由 Thymeleaf 產生（th:attr="data-click-url=@{...}"），所以這裡不用寫死網址。
   ========================================================= */

function sendQaClick(item) {
  const url = item && item.dataset.clickUrl;
  if (!url) return;
  // keepalive：就算使用者馬上離開這一頁，請求也會送出
  fetch(url, { method: 'POST', keepalive: true }).catch(() => {});
}

/* ========== 1. 首頁：按鈕＋JS 展開（展開的程式在 frontend_index.js「3. 常見問題」） ==========
   frontend_index.js 的 listener 掛在 #faqList 上，比這裡（document）先執行，
   所以這裡看到的是點完之後的狀態：有 .open 就是剛展開 */
document.addEventListener('click', e => {
  const btn = e.target.closest('.faq-question');
  if (!btn) return;
  const item = btn.closest('.faq-item');
  if (item && item.classList.contains('open')) sendQaClick(item);
});

/* ========== 2. 常見問題頁：原生 <details> 展開 ==========
   toggle 事件不會往上傳，所以用 capture（第三個參數 true）在 document 接 */
document.addEventListener('toggle', e => {
  const item = e.target;
  if (item.matches && item.matches('details.qa-item') && item.open) sendQaClick(item);
}, true);
