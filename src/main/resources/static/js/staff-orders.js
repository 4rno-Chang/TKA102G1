/* =========================================================
   Bistroops 員工後台：訂單管理（查詢所有訂單）
   對應的 HTML：templates/staff/orders.html
   需要先載入 staff.js（訂單資料 StaffStore 在那裡）
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和其他檔案互相衝突
(() => {

const orderList = document.getElementById('orderList');
const orderSearch = document.getElementById('orderSearch');
const filterButtons = document.querySelectorAll('#orderFilters [data-filter]');

let currentFilter = 'ALL';        // ALL 全部／ACTIVE 用餐中／DONE 待結帳／PAID 已結帳
const openedIds = new Set();      // 目前展開明細的訂單編號

// 餐點狀態對應的中文
const ITEM_STATUS_TEXT = { COOKING: '製作中', TO_SERVE: '等待送餐', SERVED: '已送達' };


/* ========== 1. 產生畫面 ==========
   【Thymeleaf 串接】
     清單改由 orders.html 的 th:each 產生，可以整區刪掉。
     篩選與搜尋改成一個 method="get" 的表單，把條件帶在網址上，例如
       /staff/orders?status=PAID&keyword=A1
     Controller 用 @RequestParam 接 status 和 keyword，查出符合的訂單放進 Model。
     「展開明細」不需要後端，下面第 2 區的展開程式可以保留。 */
function detailHtml(order) {
  const amount = StaffStore.amountOf(order);
  // 被刪除的餐點仍然列出來（整列劃線變淡），狀態顯示「已取消」
  const rows = order.items.map(item => item.cancelled ? `
    <tr class="row-cancelled">
      <td><span class="dish-tag">${escapeHtml(item.category)}</span> ${escapeHtml(item.name)}</td>
      <td class="num">× ${escapeHtml(item.qty)}</td>
      <td class="num">${formatMoney(item.price * item.qty)}</td>
      <td class="num"></td>
      <td>已取消</td>
    </tr>` : `
    <tr>
      <td><span class="dish-tag">${escapeHtml(item.category)}</span> ${escapeHtml(item.name)}${item.note ? `<small class="dish-note">${escapeHtml(item.note)}</small>` : ''}</td>
      <td class="num">× ${escapeHtml(item.qty)}</td>
      <td class="num">${formatMoney(item.price * item.qty)}</td>
      <td class="num discount">${item.discount ? '-' + escapeHtml(item.discount) : ''}</td>
      <td>${ITEM_STATUS_TEXT[item.status]}${item.servedAt ? `（${escapeHtml(item.servedAt)}）` : ''}</td>
    </tr>`).join('');

  let payInfo = '尚未結帳';
  if (order.cancelled) {
    payInfo = `${escapeHtml(order.cancelledAt)} 已刪除訂單（原因：${escapeHtml(order.cancelReason)}）`;
  } else if (order.paid) {
    payInfo = `${escapeHtml(order.paidAt)} 以${escapeHtml(order.payMethod)}結帳`;
  }
  // 整桌的備註（有填才顯示）
  const orderNote = order.note ? `<p class="order-detail-note">備註：${escapeHtml(order.note)}</p>` : '';

  return `
    <div class="order-detail">
      ${orderNote}
      <table class="receipt-table">
        <thead><tr><th>品項</th><th class="num">數量</th><th class="num">小計</th><th class="num">折扣</th><th>狀態</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
      <p class="order-detail-sum">
        總價 ${formatMoney(amount.total)}　總折扣 ${formatMoney(amount.discount)}　實付金額 <strong>${formatMoney(amount.pay)}</strong>
        <span class="order-detail-pay">${payInfo}</span>
      </p>
    </div>`;
}

function render() {
  const keyword = orderSearch.value.trim().toLowerCase();

  const orders = StaffStore.getOrders().filter(order => {
    if (currentFilter !== 'ALL' && StaffStore.statusOf(order) !== currentFilter) return false;
    // 沒有輸入關鍵字時 keyword 是空字串，每一筆都算符合
    return order.table.toLowerCase().includes(keyword) || order.id.toLowerCase().includes(keyword);
  });
  // 新的訂單排前面
  orders.sort((a, b) => b.id.localeCompare(a.id));

  if (orders.length === 0) {
    orderList.innerHTML = '<p class="staff-empty">沒有符合條件的訂單</p>';
    return;
  }

  orderList.innerHTML = orders.map(order => {
    const status = ORDER_STATUS_TEXT[StaffStore.statusOf(order)];
    const opened = openedIds.has(order.id);
    return `
      <div class="order-item${opened ? ' open' : ''}">
        <button type="button" class="order-row" data-toggle-order="${escapeHtml(order.id)}" aria-expanded="${opened}">
          <span class="order-id">${escapeHtml(order.id)}</span>
          <span><span class="badge badge-table">${escapeHtml(order.table)}</span></span>
          <span>${escapeHtml(order.people)} 人</span>
          <span>${escapeHtml(order.time)}</span>
          <span class="num order-amount">${formatMoney(StaffStore.amountOf(order).pay)}</span>
          <span><span class="tag ${status.className}">${status.text}</span></span>
          <span class="order-icon">+</span>
        </button>
        ${opened ? detailHtml(order) : ''}
      </div>`;
  }).join('');
}


/* ========== 2. 操作 ========== */
// 切換狀態篩選
filterButtons.forEach(btn => {
  btn.addEventListener('click', () => {
    currentFilter = btn.dataset.filter;
    filterButtons.forEach(b => b.classList.toggle('active', b === btn));
    render();
  });
});

// 輸入關鍵字時即時篩選
orderSearch.addEventListener('input', render);

// 點某一筆訂單：展開／收合餐點明細
orderList.addEventListener('click', e => {
  const row = e.target.closest('[data-toggle-order]');
  if (!row) return;
  const id = row.dataset.toggleOrder;
  if (openedIds.has(id)) openedIds.delete(id);
  else openedIds.add(id);
  render();
});

// 另一個分頁改了資料時，這裡跟著更新
StaffStore.onChange(render);

render();

})();
