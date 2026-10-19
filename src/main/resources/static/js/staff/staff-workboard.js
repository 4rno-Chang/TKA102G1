/* =========================================================
   Bistroops 員工後台：工作面板（內場／外場／已完成訂單）
   對應的 HTML：templates/staff/workboard.html
   需要先載入 staff.js（假資料 StaffStore 在那裡）

   這個檔案分成兩區：
     A. 資料庫的畫面也會用到的程式（保留）：
        目前是哪個畫面、左右箭頭、卡片的縮小／展開、編輯訂單對話框裡的互動
     B. 假資料：只控制 workboard.html 裡標示【假資料】的卡片區和對話框

   【Thymeleaf 串接】資料庫的版本確認沒問題後：
     1. 刪掉 workboard.html 裡標示【假資料】的兩塊（卡片區、編輯訂單的對話框）
     2. 刪掉這個檔案的 B 區（從「B. 假資料」那一行到檔案最後；最後一行的 })(); 要留著）
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和其他檔案互相衝突
(() => {


/* =====================================================================
   A. 資料庫的畫面（workboard.html 的【資料庫資料】）也會用到的程式 ── 保留
   這一區和資料無關，只處理畫面上的互動，所以假資料和資料庫的畫面都用同一份。
   ===================================================================== */

/* ---------- A-1. 目前是哪個畫面 ----------
   左邊三顆是連結，網址上的 view 決定目前看的是哪個畫面：
     /staff/workboard?view=kitchen 內場（沒有帶 view 也是內場）／floor 外場／done 已完成訂單
   這裡依照網址把對應的那一顆變成磚紅色，並顯示該畫面的說明文字。 */
const VIEWS = {
  kitchen: { hint: '餐點做好後，點一下該道餐點的「出餐」，通知外場送餐。未送達之餐點，再次點擊可退回製作中。' },
  floor:   { hint: '黃框的餐點已經做好，送到桌上後點一下「確認送達」。' },
  done:    { hint: '所有尚未結帳的桌子（只能查看，不能點選）。已送達的餐點右側是送達時間。' }
};

let currentView = new URLSearchParams(location.search).get('view');
if (!VIEWS[currentView]) currentView = 'kitchen';   // 沒有帶 view，或帶了不認得的字，都當作內場

const boardHint = document.getElementById('boardHint');
if (boardHint) boardHint.textContent = VIEWS[currentView].hint;
document.querySelectorAll('[data-board-view]').forEach(link => {
  link.classList.toggle('active', link.dataset.boardView === currentView);
});

// 沒有權限的按鈕：點擊時顯示提示，不進行跳轉
document.querySelectorAll('.board-btn.no-permission').forEach(link => {
    link.addEventListener('click', e => {
        e.preventDefault();
        staffToast('您沒有此功能的操作權限');
    });
});


/* ---------- A-2. 左右箭頭 ----------
   卡片排成一列超出畫面時，用箭頭一次移動兩張卡片。
   頁面上每一個卡片區（class 是 board-scroll 的 div）各自有一組箭頭，這裡替每一組都設定好。 */
const CARDS_PER_CLICK = 2;   // 想改成一次移動幾張，改這個數字
const arrowUpdaters = [];    // 每一個卡片區「重新判斷箭頭要不要顯示」的函式

document.querySelectorAll('.board-scroll').forEach(scrollBox => {
  const cards = scrollBox.querySelector('.board-cards');
  const prev = scrollBox.querySelector('.board-arrow-prev');
  const next = scrollBox.querySelector('.board-arrow-next');
  if (!cards || !prev || !next) return;

  // 左邊還有卡片才顯示左箭頭，右邊還有才顯示右箭頭
  const update = () => {
    // 最多可以往右捲多少；留 4px 的誤差，避免因為小數點而一直顯示
    const maxScroll = cards.scrollWidth - cards.clientWidth;
    prev.hidden = cards.scrollLeft <= 4;
    next.hidden = cards.scrollLeft >= maxScroll - 4;
  };

  // direction：-1 往左、1 往右。一張卡片的距離 = 卡片寬度 + 卡片之間 16px 的間距
  const move = direction => {
    const card = cards.querySelector('.table-card');
    const oneCard = card ? card.offsetWidth + 16 : 336;
    cards.scrollBy({ left: direction * oneCard * CARDS_PER_CLICK, behavior: 'smooth' });
  };

  prev.addEventListener('click', () => move(-1));
  next.addEventListener('click', () => move(1));
  cards.addEventListener('scroll', update);    // 用手指滑動時也要更新
  arrowUpdaters.push(update);
});

// 重新判斷所有卡片區的箭頭（卡片的數量或寬度改變之後呼叫）
function updateArrows() {
  arrowUpdaters.forEach(update => update());
}
window.addEventListener('resize', updateArrows);   // 平板轉向、視窗大小改變時
updateArrows();


/* ---------- A-3. 資料庫的卡片：點桌號那一列縮小／展開 ----------
   卡片已經由 Thymeleaf 產生在頁面上，這裡只是替它加上或拿掉 collapsed 這個 class。
   之後頁面如果每 10 秒自動重新載入，縮小的卡片會恢復展開；想讓它留著，
   可以把縮小的訂單編號存進 sessionStorage，頁面載入時再把對應的卡片加上 collapsed。 */
const dbBoardCards = document.getElementById('dbBoardCards');
if (dbBoardCards) {
  dbBoardCards.addEventListener('click', e => {
    const head = e.target.closest('[data-toggle-card]');
    if (!head) return;
    const collapsed = head.closest('.table-card').classList.toggle('collapsed');   // 回傳現在是不是縮小的
    head.setAttribute('aria-expanded', !collapsed);
    head.querySelector('.table-toggle').textContent = collapsed ? '▼ 展開' : '▲ 縮小';
    updateArrows();
  });
}


/* ---------- A-4. 編輯訂單的對話框：常用備註 ----------
   先點要填的備註欄位，再點「常用備註」的按鈕，文字就會加進那個欄位。 */
let noteTarget = null;       // 最後點到的備註欄位，「常用備註」會加到這個欄位

// 記住最後點到的是哪一個備註欄位
document.addEventListener('focusin', e => {
  if (e.target.matches('.edit-modal .edit-input')) noteTarget = e.target;
});

// 把文字加到最後點到的欄位。欄位裡已經有字就用「、」接在後面
document.addEventListener('click', e => {
  const chip = e.target.closest('[data-note-chip]');
  // noteTarget.isConnected 是 false 代表那個欄位已經不在畫面上了；鎖住的欄位（被刪除的餐點）也不加
  if (!chip || !noteTarget || !noteTarget.isConnected || noteTarget.disabled || noteTarget.readOnly) return;
  const text = chip.dataset.noteChip;
  const current = noteTarget.value.trim();
  if (current.split('、').includes(text)) return;   // 已經有這個備註就不重複加
  // slice 是為了不超過輸入框的字數上限（maxlength）
  noteTarget.value = (current ? `${current}、${text}` : text).slice(0, noteTarget.maxLength);
  noteTarget.focus();
});


/* ---------- A-5. 資料庫的對話框：刪除／復原單一道餐點、切換到「確認刪除」的畫面 ----------
   對話框由 Thymeleaf 產生（Controller 有放 editOrder 時才會在頁面上），這裡只處理按鈕按下去的變化，
   真正存進資料庫是按「儲存變更」或「確認刪除」把表單送出之後，由 Controller 處理。 */
document.addEventListener('click', e => {
  // 每一列最右邊的「刪除／復原」：先在畫面上標記起來，按「儲存變更」才會真的生效
  const removeBtn = e.target.closest('[data-db-remove]');
  if (removeBtn) {
    const row = removeBtn.closest('.edit-item');
    const removed = row.classList.toggle('is-removed');   // 有 → 拿掉，沒有 → 加上；回傳現在有沒有
    removeBtn.textContent = removed ? '復原' : '刪除';
    row.querySelector('.edit-input').readOnly = removed;   // 被刪除的餐點不用填備註
    // 這一列藏著的 cancelledItemIds：要刪除時拿掉 disabled（才會被送出），復原時加回去
    row.querySelector('[name="cancelledItemIds"]').disabled = !removed;
    return;
  }

  // 「刪除訂單」：換到確認的畫面；「← 返回」：換回填寫備註的畫面
  const askDelete = e.target.closest('[data-edit-ask-delete]');
  const back = e.target.closest('[data-edit-back]');
  if (askDelete || back) {
    const dialog = (askDelete || back).closest('.edit-dialog');
    dialog.querySelector('[data-edit-view="notes"]').hidden = Boolean(askDelete);
    dialog.querySelector('[data-edit-view="delete"]').hidden = !askDelete;
  }
});

/* 後端擋下操作時的提示：訊息由 Controller 用 addFlashAttribute("error", ...) 帶過來，
   HTML 寫在 #staffToast 的 data-error 上，頁面載入完成後顯示。 */
window.addEventListener('load', function () {
  const toast = document.getElementById('staffToast');
  if (toast && toast.dataset.error) staffToast(toast.dataset.error);//沒有訊息時它是空的
});
/* ---------- A-6. 即時更新（WebSocket） ----------
   連上 /staff/workboard/ws（Java：WorkboardWebSocketHandler），收到 "workboard-changed" 就重新整理。
   重新整理前記住「卡片區捲到哪裡」「哪些卡片縮小」，載入後還原，員工才不會每次都要重新找卡片。
   這些時候先不重新整理：
     ・編輯訂單的對話框開著（關掉對話框時頁面本來就會重新載入）
     ・分頁在背景（切回來時再整理） */
	 const BOARD_STATE_KEY = 'workboardState';
	 let reloadTimer = null;
	 let needRefresh = false;   // 在背景時收到通知，切回來要整理

	 // 重新整理前：記住捲動位置和縮小的卡片
	 function saveBoardState() {
	   if (!dbBoardCards) return;
	   const collapsed = [...dbBoardCards.querySelectorAll('.table-card.collapsed')]
	     .map(card => card.dataset.order);
	   try {
	     sessionStorage.setItem(BOARD_STATE_KEY, JSON.stringify({
	       page:  currentView,   // 只在同一個畫面還原（內場的狀態不要套到外場）
	       scroll: dbBoardCards.scrollLeft,
	       collapsed
	     }));
	   } catch (e) { /* 無痕模式等情況存不了就算了 */ }
	 }

	 // 載入後：還原捲動位置和縮小的卡片
	 function restoreBoardState() {
	   if (!dbBoardCards) return;
	   let state = null;
	   try {
	     state = JSON.parse(sessionStorage.getItem(BOARD_STATE_KEY));
	     sessionStorage.removeItem(BOARD_STATE_KEY);
	   } catch (e) { return; }
	   if (!state || state.page !== currentView) return;

	   state.collapsed.forEach(id => {
	     const head = dbBoardCards.querySelector(`.table-card[data-order="${id}"] [data-toggle-card]`);
	     if (head) head.click();   // 用 A-3 原本的縮小功能，文字和箭頭都會跟著更新
	   });
	   dbBoardCards.scrollLeft = state.scroll;
	   updateArrows();
	 }
	 restoreBoardState();

	 // 離開這一頁前（送出出餐、送達的表單、重新整理、點編輯訂單）都先記住位置
	 //頁面要被換掉之前，瀏覽器一定會觸發這個事件。如果改成監聽表單的 submit，有些情況會漏掉。例如「退回製作中」有 data-confirm，staff.js 確認後是用程式送出表單，這種送法不會觸發 submit 事件。
	 // pagehide 不管怎麼離開都會觸發。
	 window.addEventListener('pagehide', saveBoardState);

	 // 收到通知：等 1 秒再整理（連續按好幾道出餐時，只整理一次）
	 function refreshBoard() {
	   if (document.querySelector('.edit-modal:not([hidden])')) return;   // 只看「正在顯示」的編輯對話框（假資料那個平常是 hidden）
	   if (document.visibilityState !== 'visible') { needRefresh = true; return; }
	   clearTimeout(reloadTimer);
	   reloadTimer = setTimeout(() => {
	     location.reload();
	   }, 1000);
	 }

	 // 從背景切回來時（螢幕打開、切回這個分頁），如果中間有收到通知就整理
	 document.addEventListener('visibilitychange', () => {
	   if (needRefresh && document.visibilityState === 'visible') {
	     needRefresh = false;
	     refreshBoard();
	   }
	 });

	 // 連線（寫法參考 front/order.js）；斷線 3 秒後自動重連
	 function connectBoardSocket(isReconnect) {
	   const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
	   const socket = new WebSocket(protocol + location.host + '/staff/workboard/ws');

	   // 重連成功：斷線期間可能漏掉通知，整理一次
	   socket.addEventListener('open', () => { if (isReconnect) refreshBoard(); });
	   socket.addEventListener('message', e => {
	     if (e.data === 'workboard-changed') refreshBoard();
	   });
	   socket.addEventListener('close', () => setTimeout(() => connectBoardSocket(true), 3000));
	 }
	 if (dbBoardCards) connectBoardSocket(false);

})();
