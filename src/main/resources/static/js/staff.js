/* =========================================================
   Bistroops 員工後台：共用程式（每個後台頁面都要載入，而且要放在最前面）
   內容：1. 示範資料 StaffStore　2. 小工具　3. 小提示　4. 尚未開放的按鈕　5. 時鐘　6. 重設示範資料

   對應的 CSS：static/css/staff.css
   各頁面自己的程式：staff-workboard.js／staff-checkout.js／staff-orders.js

   ※ 目前畫面用的是假資料。Java 寫好後要改的地方都標了「Thymeleaf 串接」，
     用 Ctrl+F 搜尋這幾個字就能找到。
   ========================================================= */


/* ========== 1. 示範資料 StaffStore ==========
   所有後台頁面都透過 StaffStore 讀寫訂單，所以內場、外場、結帳、訂單管理看到的是同一份資料。
   資料暫存在瀏覽器的 localStorage（關掉分頁再打開還在，但只存在這台裝置）。

   【Thymeleaf 串接】總覽
     這一區是用來代替資料庫的。真正上線時，內場和外場是「不同的平板」，
     一定要透過伺服器的資料庫才能同步，瀏覽器暫存做不到。做法和會員功能相同：

       顯示資料：Controller 把訂單清單放進 Model，HTML 用 th:each 產生畫面
       修改資料：每個按鈕包成一個小表單（th:action + method="post"）送到 Controller，
                 Controller 改完資料庫後 redirect 回同一頁
       保持同步：在 <head> 加上 <meta http-equiv="refresh" content="10">，
                 頁面就會每 10 秒自動重新載入一次，看到別台平板的最新操作

     全部改完後，這個 StaffStore 和各頁 JS 裡「產生畫面」的程式都可以刪掉。

     提示寫在哪裡（每個檔案用 Ctrl+F 搜尋「Thymeleaf 串接」）：
       StaffPageController.java   每個網址要準備的資料、要新增哪些 @PostMapping（先看這裡的總覽）
       templates/staff/*.html     每一塊畫面要換成的 th:each／th:text／表單，可以直接照著改
       static/js/staff-*.js       哪些程式之後可以刪掉、哪些要保留

     下面「一筆訂單的格式」可以當作設計資料表欄位的參考：
     訂單一張表、餐點一張表（餐點用訂單編號連回訂單），狀態存成文字或數字都可以。

   一筆訂單的格式：
     id       訂單編號
     table    桌號
     people   人數
     time     下單時間
     paid     是否已結帳（true／false）
     payMethod／paidAt   付款方式（'刷卡'／'現金'）與結帳時間，未結帳時是空字串
     note     整桌的備註（內場的「編輯訂單」可以填），沒有就不用寫
     cancelled／cancelReason／cancelledAt
              訂單被刪除時才會有：是否已刪除、刪除原因、刪除時間。
              刪除的訂單不會真的消失，只是標記起來，在「訂單管理」仍然查得到
     items    餐點清單，每一道為：
              category  分類（前菜／主菜／湯品／甜品）
              name      品名
              qty       數量
              price     單價
              discount  折扣金額（沒有折扣是 0）
              status    'COOKING' 製作中／'TO_SERVE' 等待送餐／'SERVED' 已送達
              servedAt  送達時間，還沒送達是空字串
              note      這道餐點的備註，沒有就不用寫
              cancelled 這道餐點被刪除時是 true。和訂單一樣不會真的移除，只是標記起來：
                        卡片上顯示成「已取消」、不算進金額、也不用等它送達
   ================================================== */
const StaffStore = (() => {
  // 示範訂單的內容有修改時，把最後的數字加 1，瀏覽器就會改用新的示範訂單（舊的暫存不再使用）
  const STORAGE_KEY = 'bistroopsStaffDemoOrders3';

  // 一開始的示範訂單
  function defaultOrders() {
    return [
      { id: '20261004-006', table: 'A1', people: 2, time: '12:30', paid: false, payMethod: '', paidAt: '',
        items: [
          { category: '前菜', name: '日式和風沙拉', qty: 1, price: 220, discount: 0, status: 'SERVED', servedAt: '12:41' },
          { category: '主菜', name: '熟成肋眼牛排', qty: 1, price: 980, discount: 0, status: 'TO_SERVE', servedAt: '' },
          { category: '主菜', name: '奶油干貝義大利麵', qty: 1, price: 420, discount: 0, status: 'SERVED', servedAt: '12:52' },
          { category: '湯品', name: '法式洋蔥濃湯', qty: 2, price: 160, discount: 0, status: 'SERVED', servedAt: '12:38' }
        ] },
      { id: '20261004-007', table: 'B2', people: 2, time: '12:38', paid: false, payMethod: '', paidAt: '',
        note: '客人趕時間，主菜請先出',
        items: [
          { category: '前菜', name: '蒜味奶油蘑菇', qty: 1, price: 240, discount: 0, status: 'SERVED', servedAt: '12:49' },
          { category: '主菜', name: '香煎鱸魚佐白醬', qty: 1, price: 560, discount: 0, status: 'COOKING', servedAt: '', note: '醬汁另外放' },
          { category: '主菜', name: '松露野菇燉飯', qty: 1, price: 460, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '湯品', name: '主廚當日例湯', qty: 2, price: 120, discount: 0, status: 'SERVED', servedAt: '12:45' }
        ] },
      { id: '20261004-009', table: 'C1', people: 3, time: '12:55', paid: false, payMethod: '', paidAt: '',
        items: [
          { category: '前菜', name: '義式卡布里沙拉', qty: 1, price: 280, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '主菜', name: '經典粉紅醬海鮮燉飯', qty: 1, price: 480, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '主菜', name: '牛肝菌野菇義大利麵', qty: 2, price: 420, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '甜品', name: '主廚手工提拉米蘇', qty: 3, price: 180, discount: 0, status: 'COOKING', servedAt: '' },
          // 這一桌特別多放幾道，用來示範「品項很多時在卡片內上下滑動」
          { category: '前菜', name: '炙燒干貝佐柚子醬', qty: 1, price: 360, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '前菜', name: '酥炸花枝圈', qty: 1, price: 260, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '湯品', name: '南瓜濃湯', qty: 3, price: 140, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '主菜', name: '爐烤德國豬腳', qty: 1, price: 720, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '主菜', name: '香料烤半雞', qty: 1, price: 520, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '主菜', name: '瑪格麗特披薩', qty: 1, price: 250, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '甜品', name: '焦糖布蕾', qty: 2, price: 160, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '飲品', name: '手沖咖啡', qty: 3, price: 150, discount: 0, status: 'COOKING', servedAt: '' }
        ] },
      { id: '20261004-008', table: 'B3', people: 2, time: '12:50', paid: false, payMethod: '', paidAt: '',
        items: [
          { category: '前菜', name: '凱薩沙拉', qty: 1, price: 220, discount: 0, status: 'TO_SERVE', servedAt: '' },
          { category: '主菜', name: '戰斧豬排', qty: 1, price: 620, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '主菜', name: '清炒海鮮義大利麵', qty: 1, price: 440, discount: 0, status: 'COOKING', servedAt: '' },
          { category: '湯品', name: '法式蘑菇濃湯', qty: 2, price: 160, discount: 0, status: 'COOKING', servedAt: '' }
        ] },
      // 餐點已全部送達、還沒結帳
      { id: '20261004-005', table: 'B1', people: 4, time: '12:10', paid: false, payMethod: '', paidAt: '',
        items: [
          { category: '主菜', name: '戰斧豬排', qty: 1, price: 620, discount: 0, status: 'SERVED', servedAt: '12:34' },
          { category: '前菜', name: '莫札瑞拉起司與新鮮羅勒', qty: 1, price: 320, discount: 64, status: 'SERVED', servedAt: '12:22' },
          { category: '主菜', name: '瑪格麗特披薩', qty: 1, price: 250, discount: 0, status: 'SERVED', servedAt: '12:36' },
          { category: '主菜', name: '拿坡里手工餅皮', qty: 1, price: 120, discount: 0, status: 'SERVED', servedAt: '12:36' }
        ] },
      // 已結帳
      { id: '20261004-003', table: 'A3', people: 2, time: '11:20', paid: true, payMethod: '現金', paidAt: '12:25',
        items: [
          { category: '主菜', name: '香草烤春雞', qty: 1, price: 520, discount: 0, status: 'SERVED', servedAt: '11:42' },
          { category: '主菜', name: '松露野菇燉飯', qty: 1, price: 460, discount: 0, status: 'SERVED', servedAt: '11:44' },
          { category: '甜品', name: '焦糖布蕾', qty: 2, price: 160, discount: 0, status: 'SERVED', servedAt: '12:05' }
        ] },
      { id: '20261004-004', table: 'C2', people: 3, time: '11:45', paid: true, payMethod: '刷卡', paidAt: '12:48',
        items: [
          { category: '前菜', name: '生火腿拼盤', qty: 1, price: 420, discount: 0, status: 'SERVED', servedAt: '11:56' },
          { category: '主菜', name: '紅酒燉牛頰', qty: 2, price: 680, discount: 136, status: 'SERVED', servedAt: '12:12' },
          { category: '主菜', name: '熟成肋眼牛排', qty: 1, price: 980, discount: 0, status: 'SERVED', servedAt: '12:14' }
        ] }
    ];
  }

  // 讀出目前的訂單；瀏覽器裡沒有存過（或存壞了）就用示範訂單
  function getOrders() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
      if (Array.isArray(saved)) return saved;
    } catch (e) { /* 讀不到就當作沒有存過 */ }
    return defaultOrders();
  }

  function saveOrders(orders) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(orders));
    } catch (e) { /* 瀏覽器不允許儲存時，這次的操作就不會被記住 */ }
  }

  // 現在的時間，格式 '12:05'
  function nowTime() {
    const now = new Date();
    return String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');
  }

  return {
    getOrders,
    nowTime,

    // 改變某一道餐點的狀態。itemIndex 是這道餐點在 items 裡的第幾個（從 0 開始）
    setItemStatus(orderId, itemIndex, status) {
      const orders = getOrders();
      const order = orders.find(o => o.id === orderId);
      if (!order || !order.items[itemIndex]) return;
      order.items[itemIndex].status = status;
      order.items[itemIndex].servedAt = status === 'SERVED' ? nowTime() : '';
      saveOrders(orders);
    },

    // 結帳。method 是 '刷卡' 或 '現金'
    pay(orderId, method) {
      const orders = getOrders();
      const order = orders.find(o => o.id === orderId);
      if (!order) return;
      order.paid = true;
      order.payMethod = method;
      order.paidAt = nowTime();
      saveOrders(orders);
    },

    // 儲存「編輯訂單」的修改。orderNote 是整桌的備註；itemNotes 是陣列，第幾個就是第幾道餐點的備註
    // itemCancelled 也是陣列，第幾個是 true 就代表第幾道餐點要刪除（標記成已取消）
    saveEdits(orderId, orderNote, itemNotes, itemCancelled) {
      const orders = getOrders();
      const order = orders.find(o => o.id === orderId);
      if (!order) return;
      order.note = orderNote;
      order.items.forEach((item, index) => {
        item.note = itemNotes[index] || '';
        item.cancelled = Boolean(itemCancelled[index]);
      });
      saveOrders(orders);
    },

    // 刪除訂單：不會真的移除，而是標記成「已取消」並記下原因與時間
    cancelOrder(orderId, reason) {
      const orders = getOrders();
      const order = orders.find(o => o.id === orderId);
      if (!order) return;
      order.cancelled = true;
      order.cancelReason = reason;
      order.cancelledAt = nowTime();
      saveOrders(orders);
    },

    // 這筆訂單是否還在進行中（沒有結帳、也沒有被刪除）。工作面板和結帳只顯示這種訂單
    isOpen(order) {
      return !order.paid && !order.cancelled;
    },

    // 回到一開始的示範訂單
    reset() {
      saveOrders(defaultOrders());
    },

    // 這筆訂單的餐點是否全部送達
    isAllServed(order) {
      return this.activeItems(order).every(item => item.status === 'SERVED');
    },

    // 這筆訂單裡沒有被刪除的餐點
    activeItems(order) {
      return order.items.filter(item => !item.cancelled);
    },

    // 訂單的狀態：'CANCELLED' 已取消／'PAID' 已結帳／'DONE' 餐點都送達了、等結帳／'ACTIVE' 用餐中
    statusOf(order) {
      if (order.cancelled) return 'CANCELLED';
      if (order.paid) return 'PAID';
      return this.isAllServed(order) ? 'DONE' : 'ACTIVE';
    },

    // 計算金額。回傳 { total 總價, discount 總折扣, pay 實付金額 }
    amountOf(order) {
      let total = 0;
      let discount = 0;
      // 被刪除的餐點不算錢
      this.activeItems(order).forEach(item => {
        total += item.price * item.qty;
        discount += item.discount;
      });
      return { total, discount, pay: total - discount };
    },

    // 別的分頁改了資料時通知這一頁（讓兩個分頁的畫面保持一致）
    onChange(callback) {
      window.addEventListener('storage', e => {
        if (e.key === STORAGE_KEY) callback();
      });
    }
  };
})();


/* ========== 2. 小工具 ========== */
// 把文字中的 < > & " ' 換掉，避免資料被當成 HTML 執行
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, ch => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
  ));
}

// 1310 → '$1,310'
function formatMoney(amount) {
  return '$' + Number(amount).toLocaleString('en-US');
}

// 訂單狀態對應的中文與顏色（顏色的 class 定義在 staff.css）
const ORDER_STATUS_TEXT = {
  ACTIVE: { text: '用餐中', className: 'tag-yellow' },
  DONE:   { text: '待結帳', className: 'tag-red' },
  PAID:   { text: '已結帳', className: 'tag-green' },
  CANCELLED: { text: '已取消', className: 'tag-gray' }
};


/* ========== 3. 小提示 ========== */
let staffToastTimer = null;
function staffToast(text) {
  const toast = document.getElementById('staffToast');
  if (!toast) return;
  toast.textContent = text;
  toast.classList.add('show');
  clearTimeout(staffToastTimer);
  staffToastTimer = setTimeout(() => toast.classList.remove('show'), 2400);
}


/* ========== 4. 尚未開放的按鈕 ==========
   HTML 裡寫 data-coming-soon="名稱" 的按鈕，點了都會跳出提示。
   之後功能做好了，把按鈕上的 data-coming-soon 拿掉、換成真正的連結或動作即可。 */
document.addEventListener('click', e => {
  const btn = e.target.closest('[data-coming-soon]');
  if (btn) staffToast(`「${btn.dataset.comingSoon}」功能尚未開放`);
});


/* ========== 5. 右上角的日期與時間 ========== */
const staffClock = document.getElementById('staffClock');
if (staffClock) {
  const showClock = () => {
    const now = new Date();
    const weekday = '日一二三四五六'[now.getDay()];
    staffClock.textContent =
      `${now.getMonth() + 1} 月 ${now.getDate()} 日（${weekday}）${StaffStore.nowTime()}`;
  };
  showClock();
  setInterval(showClock, 30000);   // 每 30 秒更新一次
}


/* ========== 6. 主頁的「重設示範資料」 ========== */
const resetDemoBtn = document.getElementById('resetDemo');
if (resetDemoBtn) {
  resetDemoBtn.addEventListener('click', () => {
    if (!confirm('確定要把訂單回復成一開始的示範資料嗎？')) return;
    StaffStore.reset();
    staffToast('已重設示範資料');
  });
}
