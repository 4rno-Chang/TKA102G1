/* =========================================================
   Bistroops 前台：菜單（front/menu/menu.html）
   對應的 CSS：static/css/front/menu.css
   漢堡選單、header 陰影、回到頂端、頁尾年份由 announcement.js 處理（前台內頁共用）。

   A-1. 代表圖載入失敗時，換成插圖　A-2. 捲動時，上方的類別捷徑標出目前看到的類別
   ========================================================= */

(() => {

/* ---------- A-1. 代表圖載入失敗時，換成插圖 ----------
   例如那道菜的圖片被刪掉了（伺服器回 404）。插圖的網址寫在 <img> 的 data-fallback */
function useFallback(img) {
  const fallback = img.dataset.fallback;
  if (!fallback) return;
  img.removeAttribute('data-fallback');
  img.src = fallback;
  img.closest('.menu-cat-photo').classList.add('is-art');
}
function watchPhotos() {
  document.querySelectorAll('.menu-cat-photo img[data-fallback]').forEach(img => {
    if (img.complete && img.naturalWidth === 0) useFallback(img);
    else img.addEventListener('error', () => useFallback(img), { once: true });
  });
}

/* ---------- A-2. 捲動時，上方的類別捷徑標出目前看到的類別 ---------- */
function watchCategories() {
  const links = [...document.querySelectorAll('.menu-cats a')];
  if (links.length === 0) return;
  const byId = new Map(links.map(a => [a.getAttribute('href').slice(1), a]));
  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (!entry.isIntersecting) return;
      links.forEach(a => a.classList.remove('active'));
      const link = byId.get(entry.target.id);
      if (link) {
        link.classList.add('active');
        link.scrollIntoView({ block: 'nearest', inline: 'center' });   // 手機上捷徑可以左右滑，讓選取中的跟著出現
      }
    });
  }, { rootMargin: '-40% 0px -55% 0px' });                           // 類別標題經過畫面中間偏上的位置時算「目前看到」
  document.querySelectorAll('.menu-cat').forEach(section => observer.observe(section));
}

watchPhotos();
watchCategories();

})();
