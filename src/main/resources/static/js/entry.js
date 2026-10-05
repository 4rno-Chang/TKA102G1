// 入口選擇頁
// 1. 依照現在的時間顯示問候語
// 2. 按鍵盤 1 / 2 直接進入前台 / 後台

document.addEventListener('DOMContentLoaded', () => {

  // ===== 1. 問候語 =====
  const greeting = document.getElementById('entryGreeting');
  const hour = new Date().getHours();
  if (hour < 12) {
    greeting.textContent = 'Good Morning';
  } else if (hour < 18) {
    greeting.textContent = 'Good Afternoon';
  } else {
    greeting.textContent = 'Good Evening';
  }

  // ===== 2. 鍵盤快捷鍵 =====
  document.addEventListener('keydown', (e) => {
    // 按著 Ctrl / Cmd / Alt 時不處理，避免和瀏覽器快捷鍵衝突
    if (e.ctrlKey || e.metaKey || e.altKey) return;

    const card = document.querySelector(`.entry-card[data-key="${e.key}"]`);
    if (!card) return;

    card.classList.add('is-pressed'); // 先亮一下金線，再跳轉
    setTimeout(() => { window.location.href = card.href; }, 150);
  });

});
