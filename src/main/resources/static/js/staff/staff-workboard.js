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
  done:    { hint: '餐點已全部送達、尚未結帳的桌子。右側是每道餐點的送達時間。' }
};

let currentView = new URLSearchParams(location.search).get('view');
if (!VIEWS[currentView]) currentView = 'kitchen';   // 沒有帶 view，或帶了不認得的字，都當作內場

const boardHint = document.getElementById('boardHint');
if (boardHint) boardHint.textContent = VIEWS[currentView].hint;
document.querySelectorAll('[data-board-view]').forEach(link => {
  link.classList.toggle('active', link.dataset.boardView === currentView);
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


// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
// ▼▼▼  【假資料・刪除範圍 開始】                                          ▼▼▼
// ▼▼▼   串接完成、確認資料庫的畫面沒問題後，從這一行開始刪，               ▼▼▼
// ▼▼▼   一直刪到下面 ▲▲▲ 框起來的「刪除範圍 結束」那一行（標記也一起刪掉）▼▼▼
// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
/* =====================================================================
   B. 假資料 ── 資料庫的版本確認沒問題後，從這一行到檔案最後都可以刪掉
      （最後一行的 })(); 要留著）
   這一區只控制 workboard.html 裡標示【假資料】的卡片區和對話框：用 staff.js 的 StaffStore 產生卡片，
   出餐、送達、編輯訂單都是直接改瀏覽器裡的假資料，不會送到後端。
   ===================================================================== */
const boardCards = document.getElementById('boardCards');
// 假資料那一塊已經從 workboard.html 刪掉的話，下面的程式就不用執行了
if (!boardCards) return;

const collapsedIds = new Set();    // 被「縮小」的訂單編號


/* ========== B-1. 產生畫面（假資料） ==========
   資料庫的版本寫在 workboard.html 的【資料庫資料】：卡片用 th:each 產生，可以按的餐點包成小表單。
   下面「哪些訂單要出現、每道餐點顯示什麼、能不能按」的規則，可以當作寫 Service 與 Controller 的參考：
     內場、外場：還有餐點沒送達的未結帳訂單；已完成訂單：餐點全部送達、尚未結帳的訂單。
     已結帳、已取消的訂單都不出現；被取消的餐點不算在「全部送達」的判斷裡。 */

// 一道餐點右側的狀態文字，以及這道餐點在目前的畫面能不能點
function dishState(item) {
  // 被刪除的餐點：三個畫面都顯示「已取消」，讓內場知道不用做了
  if (item.status === '已取消') {
    return { label: '✕ 已取消', className: 'is-cancelled', clickable: false };
  }
  if (currentView === 'done') {
    return { label: `${item.servedAt} 送達`, className: 'is-served-time', clickable: false };
  }
  if (item.status === '已送達') {
    return { label: '✓ 已送達', className: 'is-served', clickable: false };
  }
  if (item.status === '等待送餐') {
    // 內場：誤觸出餐時可以再點一次退回製作中，所以也是可以點的
    return currentView === 'kitchen'
      ? { label: '● 等待送餐', className: 'is-to-serve', clickable: true }
      : { label: '確認送達 →', className: 'is-to-serve', clickable: true };
  }
  // 剩下的是 製作中
  return currentView === 'kitchen'
    ? { label: '出餐 →', className: 'is-cooking', clickable: true }
    : { label: '製作中', className: 'is-cooking', clickable: false };
}

function dishHtml(item, index) {
  const state = dishState(item);
  const qty = item.qty > 1 ? ` <span class="dish-qty">× ${escapeHtml(item.qty)}</span>` : '';
  // 這道餐點的備註（有填才顯示，在品名下面一行）
  const note = item.note ? `<small class="dish-note">${escapeHtml(item.note)}</small>` : '';
  return `
    <li>
      <button type="button" class="dish ${state.className}" data-item="${index}" ${state.clickable ? '' : 'disabled'}>
        <span class="dish-tag">${escapeHtml(item.mealtype)}</span>
        <span class="dish-name">${escapeHtml(item.name)}${qty}${note}</span>
        <span class="dish-state">${escapeHtml(state.label)}</span>
      </button>
    </li>`;
}

function cardHtml(order) {
  const collapsed = collapsedIds.has(order.id);
  // 進度只算沒有被刪除的餐點
  const activeItems = StaffStore.activeItems(order);
  const servedCount = activeItems.filter(item => item.status === '已送達').length;

  // 卡片最下面的按鈕：只有內場有，可以編輯訂單（填備註、刪除訂單，見 B-3）
  let footer = '';
  if (currentView === 'kitchen') {
    footer = `<button type="button" class="card-link" data-edit-order>編輯訂單</button>`;
  }

  return `
    <article class="table-card${collapsed ? ' collapsed' : ''}" data-order="${escapeHtml(order.id)}">
      <button type="button" class="table-card-head" data-toggle-card aria-expanded="${!collapsed}">
        <strong class="table-name">${escapeHtml(order.table)} 桌</strong>
        <span>${escapeHtml(order.time)}</span>
        <span class="table-progress">${servedCount}/${activeItems.length} 已送達</span>
        <span class="table-toggle">${collapsed ? '▼ 展開' : '▲ 縮小'}</span>
      </button>
      <div class="table-card-body">
        <ul class="dish-list">${order.items.map(dishHtml).join('')}</ul>
        ${footer ? `<div class="table-card-foot">${footer}</div>` : ''}
      </div>
    </article>`;
}

function render() {
  // 已結帳、已刪除的訂單不會出現在工作面板
  const unpaid = StaffStore.getOrders().filter(order => StaffStore.isOpen(order));
  // 已完成訂單：全部送達的；內場與外場：還有餐點沒送達的
  const orders = unpaid.filter(order => StaffStore.isAllServed(order) === (currentView === 'done'));
  // 先下單的排前面
  orders.sort((a, b) => a.time.localeCompare(b.time));

  if (orders.length === 0) {
    boardCards.innerHTML = `<p class="board-empty">${currentView === 'done' ? '目前沒有待結帳的桌子' : '目前沒有進行中的訂單'}</p>`;
    updateArrows();
    return;
  }

  // 畫面會整個重新產生，所以先記下每張卡片的餐點清單滑到哪裡，產生完再放回去
  // （不然在清單下面點了一道餐點，清單會跳回最上面）
  const scrollTops = {};
  boardCards.querySelectorAll('.table-card').forEach(card => {
    const list = card.querySelector('.dish-list');
    if (list) scrollTops[card.dataset.order] = list.scrollTop;
  });

  boardCards.innerHTML = orders.map(cardHtml).join('');

  boardCards.querySelectorAll('.table-card').forEach(card => {
    const list = card.querySelector('.dish-list');
    if (list && scrollTops[card.dataset.order]) list.scrollTop = scrollTops[card.dataset.order];
  });

  // 卡片的數量可能變了，重新判斷左右箭頭要不要顯示（函式在上面的 A-2）
  updateArrows();
}


/* ========== B-2. 操作（假資料） ==========
   資料庫的版本：出餐、退回製作中、確認送達都是小表單直接送到 Controller，「編輯訂單」是連結，
   寫在 workboard.html 的【資料庫資料】。 */
boardCards.addEventListener('click', e => {
  const card = e.target.closest('.table-card');
  if (!card) return;
  const orderId = card.dataset.order;

  // 點卡片上方的桌號那一列：縮小／展開
  if (e.target.closest('[data-toggle-card]')) {
    if (collapsedIds.has(orderId)) collapsedIds.delete(orderId);
    else collapsedIds.add(orderId);
    render();
    return;
  }

  // 點卡片下方的「編輯訂單」：打開對話框（B-3）
  if (e.target.closest('[data-edit-order]')) {
    openEdit(orderId);
    return;
  }

  // 點餐點（不能點的餐點是 disabled，不會進到這裡）
  const dish = e.target.closest('.dish');
  if (!dish) return;
  const itemIndex = Number(dish.dataset.item);

  if (currentView === 'kitchen') {
    // 先查出這道餐點現在的狀態，才知道這一下是「出餐」還是「退回」
    const current = StaffStore.getOrders().find(o => o.id === orderId);
    const item = current && current.items[itemIndex];
    if (!item) return;

    if (item.status === '製作中') {
      // 內場：製作中 → 等待送餐（點一下就生效）
      StaffStore.setItemStatus(orderId, itemIndex, '等待送餐');
    } else if (item.status === '等待送餐') {
      // 內場：等待送餐 → 退回製作中（誤觸出餐時使用）。
      // 多一個確認，避免不小心連點兩下，剛出餐又被退回去
      if (!confirm(`要把「${item.name}」退回製作中嗎？`)) return;
      StaffStore.setItemStatus(orderId, itemIndex, '製作中');
      staffToast(`「${item.name}」已退回製作中`);
    }
  } else if (currentView === 'floor') {
    // 外場：等待送餐 → 已送達
    StaffStore.setItemStatus(orderId, itemIndex, '已送達');
    const order = StaffStore.getOrders().find(o => o.id === orderId);
    if (order && StaffStore.isAllServed(order)) {
      staffToast(`${order.table} 桌的餐點已全部送達`);
    }
  }
  render();
});


/* ========== B-3. 編輯訂單（假資料；填寫備註、刪除單一道餐點、刪除整筆訂單） ==========
   對話框有兩個畫面：
     畫面一「填寫備註」：每道餐點的備註與「刪除」按鈕，按「儲存變更」才會存起來
     畫面二「確認刪除」：按了「刪除訂單」才出現，再按一次「確認刪除」才會真的刪除
   資料庫的版本是 workboard.html【資料庫資料】的對話框：兩個畫面各是一個表單，送到 Controller 處理。
   「常用備註」假資料和資料庫共用上面 A-4 的程式。 */
const editModal = document.getElementById('editModal');
const editItems = document.getElementById('editItems');
const editNotesView = document.getElementById('editNotesView');
const editNotesFoot = document.getElementById('editNotesFoot');
const editDeleteView = document.getElementById('editDeleteView');
const editDeleteFoot = document.getElementById('editDeleteFoot');
const editAskDelete = document.getElementById('editAskDelete');
const editConfirmDelete = document.getElementById('editConfirmDelete');

let editingId = null;        // 正在編輯的訂單編號

// 切換畫面一（填寫備註）和畫面二（確認刪除）
function showDeleteView(show) {
  editNotesView.hidden = show;
  editNotesFoot.hidden = show;
  editDeleteView.hidden = !show;
  editDeleteFoot.hidden = !show;
}

function openEdit(orderId) {
  const order = StaffStore.getOrders().find(o => o.id === orderId);
  if (!order) return;
  editingId = orderId;

  document.getElementById('editTitle').textContent = `${order.table} 桌`;
  document.getElementById('editMeta').textContent = `${order.time} 下單　訂單編號 ${order.id}`;

  // 每道餐點一列：品名、備註輸入框、刪除按鈕。
  // 被刪除的那一列會加上 is-removed 這個 class（品名劃線、輸入框鎖住、按鈕變成「復原」）
  editItems.innerHTML = order.items.map((item, index) => {
    const removed = item.status === '已取消';
    // 已經送到客人桌上的餐點不能刪除
    const served = item.status === '已送達' && !removed;
    return `
    <li class="edit-item${removed ? ' is-removed' : ''}">
      <span class="edit-item-name">
        <span class="dish-tag">${escapeHtml(item.mealtype)}</span>
        ${escapeHtml(item.name)}${item.qty > 1 ? ` <span class="dish-qty">× ${escapeHtml(item.qty)}</span>` : ''}
      </span>
      <input type="text" class="staff-input edit-input" data-item-note="${index}" maxlength="20"
        value="${escapeHtml(item.note || '')}" placeholder="備註" autocomplete="off"
        aria-label="${escapeHtml(item.name)}的備註" ${removed ? 'disabled' : ''}>
      <button type="button" class="staff-btn edit-remove" data-remove-item ${served ? 'disabled' : ''}>
        ${served ? '已送達' : (removed ? '復原' : '刪除')}
      </button>
    </li>`;
  }).join('');

  // 「刪除訂單」只有在每一道餐點都還沒出餐（都是 製作中）時才顯示；
  // 只要有一道已經出餐或送達，就不能刪除整筆訂單
  const nothingStarted = StaffStore.activeItems(order).every(item => item.status === '製作中');
  editAskDelete.hidden = !nothingStarted;

  showDeleteView(false);
  noteTarget = null;   // 還沒點任何備註欄位；要先點一道餐點的備註欄位，「常用備註」才知道要加到哪裡
  editModal.hidden = false;
}

function closeEdit() {
  editModal.hidden = true;
  editingId = null;
}

// 點背景、右上角的 ×、「取消」都會關閉
editModal.addEventListener('click', e => {
  if (e.target.closest('[data-edit-close]')) closeEdit();
});
// 按 Esc 關閉
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && !editModal.hidden) closeEdit();
});

// 刪除單一道餐點：點「刪除」先在對話框裡標記起來（還可以按「復原」），按「儲存變更」才會真的生效
editItems.addEventListener('click', e => {
  const btn = e.target.closest('[data-remove-item]');
  if (!btn) return;
  const row = btn.closest('.edit-item');
  const removed = row.classList.toggle('is-removed');   // 有 → 拿掉，沒有 → 加上；回傳現在有沒有
  btn.textContent = removed ? '復原' : '刪除';
  row.querySelector('.edit-input').disabled = removed;   // 被刪除的餐點不用填備註
});

// 儲存變更（備註 + 刪除的餐點）
document.getElementById('editSave').addEventListener('click', () => {
  const rows = [...editItems.querySelectorAll('.edit-item')];
  // 依照順序收集每道餐點的備註，以及有沒有被標記成刪除
  const itemNotes = rows.map(row => row.querySelector('.edit-input').value.trim());
  const itemCancelled = rows.map(row => row.classList.contains('is-removed'));

  // 不能把每一道都刪掉。可以刪除整筆訂單時，提示改用「刪除訂單」
  if (itemCancelled.every(Boolean)) {
    staffToast(editAskDelete.hidden ? '至少要保留一道餐點' : '全部餐點都要刪除的話，請改用「刪除訂單」');
    return;
  }

  StaffStore.saveEdits(editingId, itemNotes, itemCancelled);
  closeEdit();
  render();
  staffToast('已儲存變更');
});

// 刪除訂單：先換到確認的畫面
editAskDelete.addEventListener('click', () => showDeleteView(true));
document.getElementById('editBackToNotes').addEventListener('click', () => showDeleteView(false));

// 確認刪除：把這筆訂單所有餐點的狀態都改成「已取消」
editConfirmDelete.addEventListener('click', () => {
  const order = StaffStore.getOrders().find(o => o.id === editingId);
  if (!order) return;
  StaffStore.cancelOrder(order.id);
  closeEdit();
  render();
  staffToast(`已刪除 ${order.table} 桌的訂單`);
});


// 另一個分頁（例如另一個視窗開著外場）改了資料時，這裡跟著更新
StaffStore.onChange(render);

render();


// ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
// ▲▲▲  【假資料・刪除範圍 結束】刪到這一行為止（這一行也刪掉）。           ▲▲▲
// ▲▲▲   下面的 })(); 不是假資料，要留著。                                ▲▲▲
// ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

})();
