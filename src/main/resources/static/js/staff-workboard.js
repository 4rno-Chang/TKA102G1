/* =========================================================
   Bistroops 員工後台：工作面板（內場／外場／已完成訂單）
   對應的 HTML：templates/staff/workboard.html
   需要先載入 staff.js（訂單資料 StaffStore 在那裡）
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和其他檔案互相衝突
(() => {

const boardCards = document.getElementById('boardCards');
const boardHint = document.getElementById('boardHint');
const viewButtons = document.querySelectorAll('[data-board-view]');

// 三個畫面的說明文字
const VIEWS = {
  kitchen: { hint: '餐點做好後，點一下該道餐點的「出餐」，通知外場送餐。' },
  floor:   { hint: '黃框的餐點已經做好，送到桌上後點一下「確認送達」。' },
  done:    { hint: '餐點已全部送達、尚未結帳的桌子。右側是每道餐點的送達時間。' }
};

let currentView = 'kitchen';       // 目前顯示：kitchen 內場／floor 外場／done 已完成訂單
const collapsedIds = new Set();    // 被「縮小」的訂單編號


/* ========== 1. 產生畫面 ==========
   【Thymeleaf 串接】
     這一區之後改由 workboard.html 的 th:each 產生，可以整區刪掉。大致的寫法：

       <article class="table-card" th:each="order : ${orders}">
         ...
         <li th:each="item : ${order.items}">
           內場：item.status 是 COOKING 時，按鈕包在小表單裡
             <form th:action="@{/staff/workboard/toServe}" method="post">
               <input type="hidden" name="itemId" th:value="${item.id}">
               <button type="submit" class="dish">...出餐</button>
             </form>
           外場：item.status 是 TO_SERVE 時，表單送到 /staff/workboard/served
         </li>
       </article>

     完整的寫法（包含備註、已取消的餐點、「編輯訂單」的連結）在 workboard.html 的卡片區。

     Controller 依照畫面只放需要的訂單進 Model（內場、外場：還有餐點沒送達的；已完成：全部送達且未結帳的）。
     已結帳、已取消的訂單都不要放進來；被取消的餐點不算在「全部送達」的判斷裡。
     三個畫面可以用網址區分，例如 /staff/workboard?view=floor，左邊的按鈕就改成連結。
     並在 <head> 加上 <meta http-equiv="refresh" content="10"> 讓不同平板的畫面保持同步。 */

// 一道餐點右側的狀態文字，以及這道餐點在目前的畫面能不能點
function dishState(item) {
  // 被刪除的餐點：三個畫面都顯示「已取消」，讓內場知道不用做了
  if (item.cancelled) {
    return { label: '✕ 已取消', className: 'is-cancelled', clickable: false };
  }
  if (currentView === 'done') {
    return { label: `${item.servedAt} 送達`, className: 'is-served-time', clickable: false };
  }
  if (item.status === 'SERVED') {
    return { label: '✓ 已送達', className: 'is-served', clickable: false };
  }
  if (item.status === 'TO_SERVE') {
    return currentView === 'kitchen'
      ? { label: '● 等待送餐', className: 'is-to-serve', clickable: false }
      : { label: '確認送達 →', className: 'is-to-serve', clickable: true };
  }
  // 剩下的是 COOKING
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
        <span class="dish-tag">${escapeHtml(item.category)}</span>
        <span class="dish-name">${escapeHtml(item.name)}${qty}${note}</span>
        <span class="dish-state">${escapeHtml(state.label)}</span>
      </button>
    </li>`;
}

function cardHtml(order) {
  const collapsed = collapsedIds.has(order.id);
  // 進度只算沒有被刪除的餐點
  const activeItems = StaffStore.activeItems(order);
  const servedCount = activeItems.filter(item => item.status === 'SERVED').length;

  // 卡片最下面的按鈕：只有內場有，可以編輯訂單（填備註、刪除訂單，見第 4 區）
  let footer = '';
  if (currentView === 'kitchen') {
    footer = `<button type="button" class="card-link" data-edit-order>編輯訂單</button>`;
  }
  // 整桌的備註（有填才顯示，在餐點清單上面）
  const orderNote = order.note ? `<p class="table-note">${escapeHtml(order.note)}</p>` : '';

  return `
    <article class="table-card${collapsed ? ' collapsed' : ''}" data-order="${escapeHtml(order.id)}">
      <button type="button" class="table-card-head" data-toggle-card aria-expanded="${!collapsed}">
        <strong class="table-name">${escapeHtml(order.table)} 桌</strong>
        <span>${escapeHtml(order.people)} 人</span>
        <span>${escapeHtml(order.time)}</span>
        <span class="table-progress">${servedCount}/${activeItems.length} 已送達</span>
        <span class="table-toggle">${collapsed ? '▼ 展開' : '▲ 縮小'}</span>
      </button>
      <div class="table-card-body">
        ${orderNote}
        <ul class="dish-list">${order.items.map(dishHtml).join('')}</ul>
        ${footer ? `<div class="table-card-foot">${footer}</div>` : ''}
      </div>
    </article>`;
}

function render() {
  boardHint.textContent = VIEWS[currentView].hint;
  viewButtons.forEach(btn => btn.classList.toggle('active', btn.dataset.boardView === currentView));

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

  // 卡片的數量可能變了，重新判斷左右箭頭要不要顯示（函式在下面的第 3 區）
  updateArrows();
}


/* ========== 2. 操作 ==========
   【Thymeleaf 串接】接上資料庫後，這一區大部分可以刪掉：
     切換畫面        → 三顆按鈕變成連結（/staff/workboard?view=...），下面 viewButtons 那段刪掉
     出餐、確認送達  → 餐點按鈕變成小表單直接送到 Controller，下面 StaffStore.setItemStatus 那段刪掉
     編輯訂單        → 變成連結（/staff/workboard/edit?orderId=...），下面 openEdit 那段刪掉
     縮小／展開      → 和資料無關，要保留。但畫面不再由 render() 產生，所以改成直接切換 class：
                         card.classList.toggle('collapsed');
                       頁面每 10 秒會重新載入，想讓縮小的狀態留著，可以把縮小的訂單編號存進 sessionStorage，
                       頁面載入時再把對應的卡片加上 collapsed。 */
// 切換 內場／外場／已完成訂單
viewButtons.forEach(btn => {
  btn.addEventListener('click', () => {
    currentView = btn.dataset.boardView;
    render();
  });
});

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

  // 點卡片下方的「編輯訂單」：打開對話框（第 4 區）
  if (e.target.closest('[data-edit-order]')) {
    openEdit(orderId);
    return;
  }

  // 點餐點（不能點的餐點是 disabled，不會進到這裡）
  const dish = e.target.closest('.dish');
  if (!dish) return;
  const itemIndex = Number(dish.dataset.item);

  if (currentView === 'kitchen') {
    // 內場：製作中 → 等待送餐
    StaffStore.setItemStatus(orderId, itemIndex, 'TO_SERVE');
  } else if (currentView === 'floor') {
    // 外場：等待送餐 → 已送達
    StaffStore.setItemStatus(orderId, itemIndex, 'SERVED');
    const order = StaffStore.getOrders().find(o => o.id === orderId);
    if (order && StaffStore.isAllServed(order)) {
      staffToast(`${order.table} 桌的餐點已全部送達`);
    }
  }
  render();
});


/* ========== 3. 左右箭頭 ==========
   卡片排成一列超出畫面時，用箭頭一次移動兩張卡片。
   這一區和資料無關，之後接上 Thymeleaf 也可以保留。 */
const boardPrev = document.getElementById('boardPrev');
const boardNext = document.getElementById('boardNext');

// 左邊還有卡片才顯示左箭頭，右邊還有才顯示右箭頭
function updateArrows() {
  // 最多可以往右捲多少；留 4px 的誤差，避免因為小數點而一直顯示
  const maxScroll = boardCards.scrollWidth - boardCards.clientWidth;
  boardPrev.hidden = boardCards.scrollLeft <= 4;
  boardNext.hidden = boardCards.scrollLeft >= maxScroll - 4;
}

// direction：-1 往左、1 往右。一次移動「兩張」卡片的距離（一張 = 卡片寬度 + 卡片之間 16px 的間距）
const CARDS_PER_CLICK = 2;   // 想改成一次移動幾張，改這個數字
function scrollCards(direction) {
  const card = boardCards.querySelector('.table-card');
  const oneCard = card ? card.offsetWidth + 16 : 336;
  boardCards.scrollBy({ left: direction * oneCard * CARDS_PER_CLICK, behavior: 'smooth' });
}

boardPrev.addEventListener('click', () => scrollCards(-1));
boardNext.addEventListener('click', () => scrollCards(1));
boardCards.addEventListener('scroll', updateArrows);    // 用手指滑動時也要更新
window.addEventListener('resize', updateArrows);        // 平板轉向、視窗大小改變時


/* ========== 4. 編輯訂單（填寫備註、刪除單一道餐點、刪除整筆訂單） ==========
   對話框有兩個畫面：
     畫面一「填寫備註」：整桌備註 + 每道餐點的備註與「刪除」按鈕，按「儲存變更」才會存起來
     畫面二「確認刪除」：按了「刪除訂單」才出現，要先選原因才能按「確認刪除」

   【Thymeleaf 串接】
     儲存備註：把畫面一包成一個表單送到 Controller，例如
         <form th:action="@{/staff/workboard/notes}" method="post">
           <input type="hidden" name="orderId" ...>
           <input name="orderNote" ...>
           每道餐點的輸入框都取同一個名字 name="itemNotes"，
           Controller 用 @RequestParam List<String> itemNotes 就能依順序收到全部。
           要刪除的餐點：在那一列放 <input type="hidden" name="cancelledItemIds" th:value="${item.id}">，
           沒有要刪除的列就不要放（或用 JS 把它 disabled），Controller 收到的就是要刪除的餐點編號
         </form>
     刪除訂單：另一個表單送到 /staff/workboard/cancel，帶 orderId 和 reason。
       建議和這裡一樣不要真的從資料庫刪掉，而是把訂單的狀態改成「已取消」並記下原因，
       之後才查得到是誰、為什麼刪的。
     下面「打開／關閉對話框」「常用備註」「切換兩個畫面」的程式和資料無關，可以保留。 */
const editModal = document.getElementById('editModal');
const editOrderNote = document.getElementById('editOrderNote');
const editItems = document.getElementById('editItems');
const editNotesView = document.getElementById('editNotesView');
const editNotesFoot = document.getElementById('editNotesFoot');
const editDeleteView = document.getElementById('editDeleteView');
const editDeleteFoot = document.getElementById('editDeleteFoot');
const editAskDelete = document.getElementById('editAskDelete');
const editConfirmDelete = document.getElementById('editConfirmDelete');
const deleteReasonButtons = document.querySelectorAll('[data-delete-reason]');

let editingId = null;        // 正在編輯的訂單編號
let noteTarget = null;       // 最後點到的備註欄位，「常用備註」會加到這個欄位
let deleteReason = '';       // 選到的刪除原因

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
  document.getElementById('editMeta').textContent = `${order.people} 人　${order.time} 下單　訂單編號 ${order.id}`;

  editOrderNote.value = order.note || '';
  // 每道餐點一列：品名、備註輸入框、刪除按鈕。
  // 被刪除的那一列會加上 is-removed 這個 class（品名劃線、輸入框鎖住、按鈕變成「復原」）
  editItems.innerHTML = order.items.map((item, index) => {
    const removed = Boolean(item.cancelled);
    // 已經送到客人桌上的餐點不能刪除
    const served = item.status === 'SERVED' && !removed;
    return `
    <li class="edit-item${removed ? ' is-removed' : ''}">
      <span class="edit-item-name">
        <span class="dish-tag">${escapeHtml(item.category)}</span>
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

  // 刪除畫面：回到還沒選原因的狀態
  deleteReason = '';
  deleteReasonButtons.forEach(btn => btn.classList.remove('active'));
  editConfirmDelete.disabled = true;
  // 「刪除訂單」只有在每一道餐點都還沒出餐（都是 COOKING）時才顯示；
  // 只要有一道已經出餐或送達，就不能刪除整筆訂單
  const nothingStarted = StaffStore.activeItems(order).every(item => item.status === 'COOKING');
  editAskDelete.hidden = !nothingStarted;

  showDeleteView(false);
  noteTarget = editOrderNote;   // 一開始「常用備註」預設加到整桌備註
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

// 記住最後點到的是哪一個備註欄位
editNotesView.addEventListener('focusin', e => {
  if (e.target.matches('.edit-input')) noteTarget = e.target;
});

// 常用備註：把文字加到最後點到的欄位。欄位裡已經有字就用「、」接在後面
document.getElementById('noteChips').addEventListener('click', e => {
  const chip = e.target.closest('[data-note-chip]');
  // noteTarget.isConnected 是 false 代表那個欄位已經不在畫面上了
  if (!chip || !noteTarget || !noteTarget.isConnected) return;
  const text = chip.dataset.noteChip;
  const current = noteTarget.value.trim();
  if (current.split('、').includes(text)) return;   // 已經有這個備註就不重複加
  // slice 是為了不超過輸入框的字數上限（maxlength）
  noteTarget.value = (current ? `${current}、${text}` : text).slice(0, noteTarget.maxLength);
  noteTarget.focus();
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

  // 不能把每一道都刪掉。可以刪除整筆訂單時，提示改用「刪除訂單」（才會留下刪除原因）
  if (itemCancelled.every(Boolean)) {
    staffToast(editAskDelete.hidden ? '至少要保留一道餐點' : '全部餐點都要刪除的話，請改用「刪除訂單」');
    return;
  }

  StaffStore.saveEdits(editingId, editOrderNote.value.trim(), itemNotes, itemCancelled);
  closeEdit();
  render();
  staffToast('已儲存變更');
});

// 刪除訂單：先換到確認的畫面
editAskDelete.addEventListener('click', () => showDeleteView(true));
document.getElementById('editBackToNotes').addEventListener('click', () => showDeleteView(false));

// 選刪除原因：選了之後「確認刪除」才能按
deleteReasonButtons.forEach(btn => {
  btn.addEventListener('click', () => {
    deleteReason = btn.dataset.deleteReason;
    deleteReasonButtons.forEach(b => b.classList.toggle('active', b === btn));
    editConfirmDelete.disabled = false;
  });
});

editConfirmDelete.addEventListener('click', () => {
  const order = StaffStore.getOrders().find(o => o.id === editingId);
  if (!order || !deleteReason) return;
  StaffStore.cancelOrder(order.id, deleteReason);
  closeEdit();
  render();
  staffToast(`已刪除 ${order.table} 桌的訂單（${deleteReason}）`);
});


// 另一個分頁（例如另一個視窗開著外場）改了資料時，這裡跟著更新
StaffStore.onChange(render);

render();

})();
