/* =========================================================
   Bistroops 員工後台：訂單管理（查詢所有訂單）
   對應的 HTML：templates/staff/orders.html
   需要先載入 staff.js（假資料 StaffStore 在那裡）

   這個檔案分成兩區：
     A. 資料庫的清單也會用到的程式（保留）：展開／收合明細、日期的月曆
     B. 假資料：只控制 orders.html 裡標示【假資料】的那一塊

   【Thymeleaf 串接】資料庫的版本確認沒問題後：
     1. 刪掉 orders.html 裡標示【假資料】的那個 div
     2. 刪掉這個檔案的 B 區（從「B. 假資料」那一行到檔案最後；最後一行的 })(); 要留著）
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和其他檔案互相衝突
(() => {


/* =====================================================================
   A. 資料庫的清單（orders.html 的【資料庫資料】）也會用到的程式 ── 保留
   這一區和資料無關，只處理畫面上的互動。Controller 還沒把 orders 放進 Model 時，
   那一塊不在頁面上，下面的 if 不成立，這一區就不會做任何事。
   ===================================================================== */

// A-1. 點某一筆訂單：展開／收合餐點明細
//      明細已經由 Thymeleaf 產生在頁面上，只是一開始加了 hidden 藏起來，這裡負責切換
const dbOrderList = document.getElementById('dbOrderList');
if (dbOrderList) {
  dbOrderList.addEventListener('click', e => {
    const row = e.target.closest('[data-toggle-order]');
    if (!row) return;
    const item = row.closest('.order-item');
    const detail = item.querySelector('.order-detail');
    detail.hidden = !detail.hidden;                       // 藏著就打開，開著就藏起來
    item.classList.toggle('open', !detail.hidden);        // open 這個 class 會讓右邊的 + 轉成 ×
    row.setAttribute('aria-expanded', !detail.hidden);
  });
}

// A-2. 日期：點中間的日期打開月曆；選了日期就把表單送出（網址會帶上新的日期，由 Controller 查那一天）
//      真正的日期欄位是透明的，蓋在顯示日期的文字上面，所以點文字其實是點到它。
//        平板：點到日期欄位，系統就會自己跳出月曆。
//        電腦：點日期欄位只會把游標放進去，所以另外呼叫瀏覽器提供的 showPicker() 把月曆打開
//      Controller 要自己再檢查一次日期：沒有帶日期或格式不對就用今天，日期在今天之後也改成今天。
const dbDateInput = document.getElementById('dbDateInput');
if (dbDateInput) {
  dbDateInput.addEventListener('click', () => {
    if (typeof dbDateInput.showPicker === 'function') {
      try { dbDateInput.showPicker(); } catch (e) { /* 打不開就算了，仍然可以用鍵盤輸入 */ }
    }
  });
  dbDateInput.addEventListener('change', () => {
    if (dbDateInput.value) dbDateInput.form.submit();     // 把日期清掉的話 value 是空的，這時不送出
  });
}


// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
// ▼▼▼  【假資料・刪除範圍 開始】                                          ▼▼▼
// ▼▼▼   串接完成、確認資料庫的畫面沒問題後，從這一行開始刪，               ▼▼▼
// ▼▼▼   一直刪到下面 ▲▲▲ 框起來的「刪除範圍 結束」那一行（標記也一起刪掉）▼▼▼
// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
/* =====================================================================
   B. 假資料 ── 資料庫的版本確認沒問題後，從這一行到檔案最後都可以刪掉
      （最後一行的 })(); 要留著）
   這一區只控制 orders.html 裡標示【假資料】的那一塊：用 staff.js 的 StaffStore 產生清單，
   篩選、日期、搜尋都是直接在瀏覽器裡篩選，不會送到後端。
   ===================================================================== */
const orderList = document.getElementById('orderList');
// 假資料那一塊已經從 orders.html 刪掉的話，下面的程式就不用執行了
if (!orderList) return;

const orderSearch = document.getElementById('orderSearch');
const filterButtons = document.querySelectorAll('#orderFilters [data-filter]');

let currentFilter = 'ALL';        // ALL 全部／ACTIVE 用餐中／DONE 待結帳／PAID 已結帳／CANCELLED 已取消
const openedIds = new Set();      // 目前展開明細的訂單編號

// 日期切換用在「全部」「已結帳」「已取消」：一次看一天，預設今天
// （「用餐中」「待結帳」只會是當天正在進行的訂單，所以不需要選日期）
const DATE_FILTERS = ['ALL', 'PAID', 'CANCELLED'];

// 一筆訂單算在哪一天：已結帳的看結帳日期，其他（用餐中、待結帳、已取消）看下單日期。
// （已取消的訂單沒有另外記取消的時間，所以用下單日期）
// 「全部」就是用這個日期篩選，所以某一天「全部」的筆數，會等於那一天各個分頁的筆數加起來
function dayOf(order) {
  if (order.paid) return order.paidDate;
  return order.date;
}
let currentDate = StaffStore.todayText();   // 目前看的是哪一天，格式 '2026-10-04'



/* ========== B-1. 產生畫面（假資料） ==========
   資料庫的版本寫在 orders.html 的【資料庫資料】：清單用 th:each 產生，
   篩選條件帶在網址上（/staff/orders?status=PAID&date=2026-10-04&keyword=A1），由 Controller 查資料庫。
   下面「怎麼篩選、怎麼算一筆訂單在哪一天」可以當作寫 Service 的參考。 */
function detailHtml(order) {
  const amount = StaffStore.amountOf(order);
  // 被刪除的餐點仍然列出來（整列劃線變淡），狀態顯示「已取消」
  const rows = order.items.map(item => item.status === '已取消' ? `
    <tr class="row-cancelled">
      <td><span class="dish-tag">${escapeHtml(item.mealtype)}</span> ${escapeHtml(item.name)}</td>
      <td class="num">× ${escapeHtml(item.qty)}</td>
      <td class="num">${formatMoney(item.price * item.qty)}</td>
      <td class="num"></td>
      <td>已取消</td>
    </tr>` : `
    <tr>
      <td><span class="dish-tag">${escapeHtml(item.mealtype)}</span> ${escapeHtml(item.name)}${item.note ? `<small class="dish-note">${escapeHtml(item.note)}</small>` : ''}</td>
      <td class="num">× ${escapeHtml(item.qty)}</td>
      <td class="num">${formatMoney(item.price * item.qty)}</td>
      <td class="num discount">${item.discount ? '-' + escapeHtml(item.discount) : ''}</td>
      <td>${escapeHtml(item.status)}${item.servedAt ? `（${escapeHtml(item.servedAt)}）` : ''}</td>
    </tr>`).join('');

  let payInfo = '尚未結帳';
  if (StaffStore.isCancelled(order)) {
    payInfo = '已刪除訂單（餐點全部取消）';
  } else if (order.paid) {
    payInfo = `${escapeHtml(order.paidAt)} 以${escapeHtml(order.payMethod)}結帳`;
  }

  return `
    <div class="order-detail">
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

  const useDate = DATE_FILTERS.includes(currentFilter);   // 這個分頁要不要依日期篩選
  updateDateNav(useDate);

  const orders = StaffStore.getOrders().filter(order => {
    if (currentFilter !== 'ALL' && StaffStore.statusOf(order) !== currentFilter) return false;
    // 全部、已結帳、已取消：只留目前這一天的（哪一筆算在哪一天，見上面的 dayOf）
    if (useDate && dayOf(order) !== currentDate) return false;
    // 沒有輸入關鍵字時 keyword 是空字串，每一筆都算符合
    return order.table.toLowerCase().includes(keyword) || order.id.toLowerCase().includes(keyword);
  });
  // 新的訂單排前面
  orders.sort((a, b) => b.id.localeCompare(a.id));

  if (orders.length === 0) {
    orderList.innerHTML = `<p class="staff-empty">${useDate ? '這一天沒有符合條件的訂單' : '沒有符合條件的訂單'}</p>`;
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
          <span>${escapeHtml(order.time)}</span>
          <span class="num order-amount">${formatMoney(StaffStore.amountOf(order).pay)}</span>
          <span><span class="tag ${status.className}">${status.text}</span></span>
          <span class="order-icon">+</span>
        </button>
        ${opened ? detailHtml(order) : ''}
      </div>`;
  }).join('');
}


/* ========== B-2. 操作（假資料） ========== */
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


/* ========== B-3. 日期切換（假資料；在「全部」「已結帳」「已取消」出現） ==========
   一次看一天，預設今天。左右箭頭切換前一天、後一天；點中間的日期會跳出裝置內建的月曆，可以直接跳到某一天。
   不能選未來的日期：右箭頭到今天就不能再按，月曆也選不到今天以後。
   「已結帳」依結帳的日期分，「已取消」依下單的日期分（沒有另外記取消的時間）；「全部」則是都算：
   已結帳的看結帳日期，其他的看下單日期。

   資料庫的版本：日期放在網址上，左右箭頭是連結（網址換成前一天、後一天），顯示的文字用 th:text，
   都寫在 orders.html 的【資料庫資料】；它需要的 JS（打開月曆、選了日期就送出表單）在上面的 A-2。

   目前「沒有」顯示當天的小計（筆數與金額）。之後想加的話，Controller 算好放進 Model，
   在 orders.html 的日期旁邊或清單表頭用 th:text 顯示即可。 */
const dateNav = document.getElementById('orderDateNav');
const datePrev = document.getElementById('datePrev');
const dateNext = document.getElementById('dateNext');
const dateLabel = document.getElementById('dateLabel');
const dateInput = document.getElementById('dateInput');

// '2026-10-04' → '10/04（日）'；不是今年的日期前面多顯示年份
function dateLabelText(dateText) {
  const [y, m, d] = dateText.split('-').map(Number);
  const weekday = '日一二三四五六'[new Date(y, m - 1, d).getDay()];
  const monthDay = String(m).padStart(2, '0') + '/' + String(d).padStart(2, '0');
  const thisYear = Number(StaffStore.todayText().slice(0, 4));
  return (y === thisYear ? '' : y + '/') + monthDay + '（' + weekday + '）';
}

// 顯示或隱藏整組日期切換，並更新上面的文字。show 是 false 時整組隱藏
function updateDateNav(show) {
  dateNav.hidden = !show;
  if (!show) return;
  const today = StaffStore.todayText();
  dateLabel.textContent = dateLabelText(currentDate);
  dateInput.value = currentDate;
  dateInput.max = today;                      // 月曆不能選今天以後
  dateNext.disabled = currentDate >= today;   // 已經是今天，就不能再往後
}

// 換到某一天。超過今天就停在今天
function goToDate(dateText) {
  const today = StaffStore.todayText();
  currentDate = dateText > today ? today : dateText;
  openedIds.clear();   // 換了一天，原本展開的明細不用留著
  render();
}

datePrev.addEventListener('click', () => goToDate(StaffStore.shiftDate(currentDate, -1)));
dateNext.addEventListener('click', () => goToDate(StaffStore.shiftDate(currentDate, 1)));

// 點中間的日期：打開裝置內建的月曆。
// 做法：真正的日期欄位（dateInput）是透明的，剛好蓋在顯示日期的文字上面，所以點文字其實是點到它。
//   平板：點到日期欄位，系統就會自己跳出月曆，不需要程式。
//   電腦：點日期欄位只會把游標放進去，所以另外呼叫瀏覽器提供的 showPicker() 把月曆打開
//         （比較舊的瀏覽器沒有這個功能，就維持可以用鍵盤輸入日期）。
dateInput.addEventListener('click', () => {
  if (typeof dateInput.showPicker === 'function') {
    try { dateInput.showPicker(); } catch (e) { /* 打不開就算了，仍然可以用鍵盤輸入 */ }
  }
});

// 在月曆選了日期（把日期清掉的話 value 會是空的，這時不做事）
dateInput.addEventListener('change', () => {
  if (dateInput.value) goToDate(dateInput.value);
});

// 另一個分頁改了資料時，這裡跟著更新
StaffStore.onChange(render);

render();


// ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
// ▲▲▲  【假資料・刪除範圍 結束】刪到這一行為止（這一行也刪掉）。           ▲▲▲
// ▲▲▲   下面的 })(); 不是假資料，要留著。                                ▲▲▲
// ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

})();
