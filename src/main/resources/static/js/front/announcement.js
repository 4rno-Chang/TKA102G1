/* =========================================================
   Bistroops 前台：最新消息（listAll.html、detail.html）
   內容：1. 手機版漢堡選單　2. Header 陰影 + 回到頂端　3. 頁尾年份　4. 圖片載入失敗時顯示佔位

   對應的 CSS：static/css/front/announcement.css
   1～3 和 main.js 的版型功能相同；main.js 會抓首頁才有的元素，所以這兩頁不載入它，改用這支。
   ========================================================= */


/* ========== 1. 手機版漢堡選單 ========== */
const menuToggle = document.getElementById('menuToggle');
const mainNav = document.getElementById('mainNav');

if (menuToggle && mainNav) {
  menuToggle.addEventListener('click', () => {
    const isOpen = mainNav.classList.toggle('open');
    menuToggle.classList.toggle('open', isOpen);
    menuToggle.setAttribute('aria-expanded', isOpen);
  });
}


/* ========== 2. Header 陰影 + 回到頂端按鈕 ========== */
const siteHeader = document.getElementById('siteHeader');
const backToTop = document.getElementById('backToTop');

window.addEventListener('scroll', () => {
  if (siteHeader) siteHeader.classList.toggle('scrolled', window.scrollY > 10);
  if (backToTop) backToTop.classList.toggle('show', window.scrollY > 400);
});
if (backToTop) {
  backToTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
}


/* ========== 3. 頁尾年份 ========== */
const year = document.getElementById('year');
if (year) year.textContent = new Date().getFullYear();


/* ========== 4. 圖片載入失敗時顯示佔位 ==========
   例如圖片資料損毀、或伺服器回 404 時，列表卡片換成 logo 佔位，內文頁直接隱藏圖片區 */
// 圖片可能在這支程式執行前就已經載入失敗，所以先檢查一次，再監聽之後的 error
function onImageError(img, handler) {
  if (img.complete && img.naturalWidth === 0) handler();
  else img.addEventListener('error', handler, { once: true });
}

const logoSrc = document.querySelector('link[rel="icon"]').href;

document.querySelectorAll('.ann-thumb > img').forEach(img => {
  onImageError(img, () => {
    const empty = document.createElement('div');
    empty.className = 'ann-thumb-empty';
    empty.innerHTML = `<img src="${logoSrc}" alt="">`;
    img.replaceWith(empty);
  });
});

document.querySelectorAll('.ann-article-img img').forEach(img => {
  onImageError(img, () => img.closest('.ann-article-img').remove());
});
