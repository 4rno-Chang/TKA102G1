/* =========================================================
   入座管理（staff/seating.html）
   畫面資料都由 Thymeleaf 產生，這支只做兩件小事，不讀資料、不產生畫面：
   1. 每 60 秒重新整理，更新用餐分鐘數和桌位狀態。
      正在展開「訂位者入座／候位入座」清單時先不重新整理，免得點到一半畫面跳掉。
   2. 桌位卡視窗（平面圖、時間軸都有）開著時，按 Esc 等於點「✕ 關閉」。
   ========================================================= */
(() => {
  const RELOAD_MS = 60 * 1000;

  setInterval(() => {
    const busy = document.querySelector('.pick-box[open]');
    if (!busy && document.visibilityState === 'visible') location.reload();
  }, RELOAD_MS);

  document.addEventListener('keydown', e => {
    const close = document.querySelector('.seat-modal-close');
    if (e.key === 'Escape' && close) close.click();
  });
})();
