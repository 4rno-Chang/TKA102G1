// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
// ▼▼▼  【假資料・刪除範圍】這個檔案「從頭到尾」都是假資料。               ▼▼▼
// ▼▼▼   串接完成、確認資料庫的畫面沒問題後，整個檔案刪掉，                ▼▼▼
// ▼▼▼   並把 checkout.html 的 <head> 裡載入 checkout.js 的那一行也刪掉。  ▼▼▼
// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼

/* =========================================================
   Bistroops 員工後台：結帳
   對應的 HTML：templates/staff/checkout.html
   需要先載入 staff.js（假資料 StaffStore 在那裡）

   ★ 這個檔案「全部」都是假資料用的：它只控制 checkout.html 裡標示【假資料】的那兩塊。★
     checkout.html 裡【資料庫資料】的那兩塊是 Thymeleaf 直接產生的，不需要這個檔案
     （結帳前的確認視窗用的是 staff.js 的「送出前先確認」）。

   【Thymeleaf 串接】資料庫的版本確認沒問題後：
     1. 刪掉 checkout.html 裡標示【假資料】的兩個 section
     2. 刪掉這個檔案，以及 checkout.html 的 <head> 裡載入它的那一行
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和其他檔案互相衝突
(() => {

const $ = id => document.getElementById(id);

const checkoutList = $('checkoutList');
// 假資料那一塊已經從 checkout.html 刪掉的話，下面的程式就不用執行了
if (!checkoutList) return;

const receipt = $('receipt');
const receiptEmpty = $('receiptEmpty');
const receiptBody = $('receiptBody');

let selectedId = null;   // 目前在右邊顯示明細的訂單編號，沒有選就是 null


/* ========== 1. 左邊：未結帳的訂單（假資料） ==========
   資料庫的版本寫在 checkout.html 的【資料庫資料】：清單用 th:each 產生，
   每一列的「結帳」是連結（/staff/checkout?order=編號），Controller 收到後把那筆訂單放進 Model。 */
function renderList() {
  const orders = StaffStore.getOrders().filter(order => StaffStore.isOpen(order));
  // 餐點已全部送達的排前面，其次依下單時間
  orders.sort((a, b) =>
    (StaffStore.isAllServed(b) - StaffStore.isAllServed(a)) || a.time.localeCompare(b.time));

  if (orders.length === 0) {
    checkoutList.innerHTML = '<p class="staff-empty">目前沒有未結帳的訂單</p>';
    return;
  }

  checkoutList.innerHTML = orders.map(order => {
    const status = ORDER_STATUS_TEXT[StaffStore.statusOf(order)];
    return `
      <div class="checkout-row${order.id === selectedId ? ' selected' : ''}">
        <span class="badge badge-table">${escapeHtml(order.table)}</span>
        <span class="checkout-row-info">
          <span class="checkout-row-id">${escapeHtml(order.id)}</span>
          <span class="tag ${status.className}">${status.text}</span>
        </span>
        <span class="checkout-row-amount">${formatMoney(StaffStore.amountOf(order).pay)}</span>
        <button type="button" class="staff-btn btn-pay" data-open-receipt="${escapeHtml(order.id)}">結帳</button>
      </div>`;
  }).join('');
}


/* ========== 2. 右邊：餐點明細（假資料） ========== */
function renderReceipt() {
  const order = StaffStore.getOrders().find(o => o.id === selectedId && StaffStore.isOpen(o));

  // 沒有選訂單（或那筆訂單已經結帳了）：顯示「請從左邊選擇」
  if (!order) {
    selectedId = null;
    receiptBody.hidden = true;
    receiptEmpty.hidden = false;
    receipt.classList.remove('open');
    return;
  }

  $('receiptTable').textContent = order.table;
  $('receiptId').textContent = order.id;
  $('receiptTime').textContent = order.time;

  // 被刪除的餐點不列在明細上，也不算錢
  $('receiptItems').innerHTML = StaffStore.activeItems(order).map(item => `
    <tr>
      <td>${escapeHtml(item.name)}</td>
      <td class="num">${escapeHtml(item.qty)}</td>
      <td class="num">${escapeHtml(item.price)}</td>
      <td class="num discount">${item.discount ? '-' + escapeHtml(item.discount) : ''}</td>
    </tr>`).join('');

  const amount = StaffStore.amountOf(order);
  $('receiptDiscount').textContent = amount.discount;
  $('receiptTotal').textContent = amount.total;
  $('receiptPay').textContent = formatMoney(amount.pay);

  $('receiptWarn').hidden = StaffStore.isAllServed(order);

  receiptEmpty.hidden = true;
  receiptBody.hidden = false;
  receipt.classList.add('open');   // 手機寬度時，明細會蓋在清單上面
}

function render() {
  renderList();
  renderReceipt();
}


/* ========== 3. 操作 ========== */
// 點某一列的「結帳」：在右邊顯示那筆訂單的明細
checkoutList.addEventListener('click', e => {
  const btn = e.target.closest('[data-open-receipt]');
  if (!btn) return;
  selectedId = btn.dataset.openReceipt;
  render();
});

// 右上角的 ×：關閉明細
$('receiptClose').addEventListener('click', () => {
  selectedId = null;
  render();
});

// 刷卡／現金
// 資料庫的版本是 checkout.html【資料庫資料】裡的表單（送到 POST /staff/checkout/pay），
// 按哪一顆，Controller 的 method 參數就會收到那顆的 value
receiptBody.addEventListener('click', e => {
  const btn = e.target.closest('[data-pay]');
  if (!btn) return;
  const order = StaffStore.getOrders().find(o => o.id === selectedId);
  if (!order) return;

  const method = btn.dataset.pay;
  const amount = formatMoney(StaffStore.amountOf(order).pay);
  const warning = StaffStore.isAllServed(order) ? '' : '這桌還有餐點尚未送達。\n';
  if (!confirm(`${warning}${order.table} 桌以「${method}」收款 ${amount}，確定結帳嗎？`)) return;

  StaffStore.pay(order.id, method);
  selectedId = null;
  render();
  staffToast(`${order.table} 桌結帳完成（${method} ${amount}）`);
});

// 另一個分頁改了資料時，這裡跟著更新
StaffStore.onChange(render);

// 從工作面板的「前往結帳」過來時，網址會帶 ?order=訂單編號，直接打開那筆的明細
selectedId = new URLSearchParams(location.search).get('order');
render();

})();
