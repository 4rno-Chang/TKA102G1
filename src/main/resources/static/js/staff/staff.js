/* =========================================================
   Bistroops 員工後台：共用程式（每個後台頁面都要載入，而且要放在最前面）

   這個檔案分成兩區：
     A. 共用程式（保留）：小工具、小提示、尚未開放的按鈕、選單的摺疊、時鐘、送出前先確認
     B. 假資料：示範訂單 StaffStore、訂單狀態的對照表、重設示範資料、主頁訂候位的示範數字

   對應的 CSS：static/css/staff/staff.css（共用）＋各頁自己的 CSS（checkout.css、orders.css、workboard.css、staff_index.css）
   各頁面自己的程式：staff-workboard.js／checkout.js／orders.js

   ※ 目前畫面用的是假資料。資料庫的版本已經用 Thymeleaf 寫在各頁 HTML 的【資料庫資料】，
     Controller 把資料放進 Model 之後就會和假資料同時出現。做法見下面 B-1 開頭的總覽。
   ========================================================= */


/* =====================================================================
   A. 共用程式 ── 保留
   這一區和資料無關，接上資料庫之後也要留著（公告管理等其他後台頁面也在用）。
   ===================================================================== */

/* ========== A-1. 小工具 ========== */
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


/* ========== A-2. 小提示 ========== */
let staffToastTimer = null;
function staffToast(text) {
  const toast = document.getElementById('staffToast');
  if (!toast) return;
  toast.textContent = text;
  toast.classList.add('show');
  clearTimeout(staffToastTimer);
  staffToastTimer = setTimeout(() => toast.classList.remove('show'), 2400);
}


/* ========== A-3. 尚未開放的按鈕 ==========
   HTML 裡寫 data-coming-soon="名稱" 的按鈕，點了都會跳出提示。
   之後功能做好了，把按鈕上的 data-coming-soon 拿掉、換成真正的連結或動作即可。 */
document.addEventListener('click', e => {
  const btn = e.target.closest('[data-coming-soon]');
  if (btn) staffToast(`「${btn.dataset.comingSoon}」功能尚未開放`);
});


/* ========== A-4. 選單的摺疊群組（後臺管理） ==========
   點「後臺管理」會展開或收起下面的項目。做法是替外層的 .staff-group 加上或拿掉 open 這個 class，
   顯示與隱藏由 staff.css 處理。
   展開的狀態會記在 sessionStorage（這個分頁關掉前都記得），換到別的後台頁面時選單不會自己收起來。
   這一區和資料庫無關，之後接上 Thymeleaf 也可以保留。 */
const MENU_GROUP_KEY = 'bistroopsStaffMenuGroupOpen';

function setMenuGroupOpen(group, open) {
  group.classList.toggle('open', open);   // 第二個參數是 true 就加上、false 就拿掉
  group.querySelector('[data-menu-group]').setAttribute('aria-expanded', open);
}

// 頁面載入時：上次是展開的，就先展開
document.querySelectorAll('.staff-group').forEach(group => {
  let wasOpen = false;
  try { wasOpen = sessionStorage.getItem(MENU_GROUP_KEY) === 'yes'; } catch (e) { /* 讀不到就當作收起 */ }
  // HTML 已經寫了 open（之後由 Thymeleaf 加上）的話也維持展開
  setMenuGroupOpen(group, wasOpen || group.classList.contains('open'));
});

document.addEventListener('click', e => {
  const toggle = e.target.closest('[data-menu-group]');
  if (!toggle) return;
  const group = toggle.closest('.staff-group');
  const open = !group.classList.contains('open');
  setMenuGroupOpen(group, open);
  try { sessionStorage.setItem(MENU_GROUP_KEY, open ? 'yes' : 'no'); } catch (e) { /* 存不了就算了 */ }
});


/* ========== A-5. 右上角的日期與時間 ========== */
const staffClock = document.getElementById('staffClock');
if (staffClock) {
  const showClock = () => {
    const now = new Date();
    const weekday = '日一二三四五六'[now.getDay()];
    const time = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');
    staffClock.textContent =
      `${now.getMonth() + 1} 月 ${now.getDate()} 日（${weekday}）${time}`;
  };
  showClock();
  setInterval(showClock, 30000);   // 每 30 秒更新一次
}


/* ========== A-6. 送出前先確認 ==========
   表單寫了 data-confirm="要顯示的問句" 的話，送出前會先跳出確認視窗，按「取消」就不送出。
   用在資料庫版本的表單，例如結帳（checkout.html）、把餐點退回製作中（workboard.html）：
     <form th:action="..." method="post" data-confirm="確定結帳嗎？"> */
document.addEventListener('submit', e => {
  const question = e.target.dataset.confirm;
  if (question && !confirm(question)) e.preventDefault();
});


// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
// ▼▼▼  【假資料・刪除範圍 開始】                                          ▼▼▼
// ▼▼▼   串接完成、確認資料庫的畫面沒問題後，從這一行開始刪，               ▼▼▼
// ▼▼▼   一直刪到下面 ▲▲▲ 框起來的「刪除範圍 結束」那一行（標記也一起刪掉）▼▼▼
// ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
/* =====================================================================
   B. 假資料 ── 四個後台頁面的假資料都刪掉之後，從這一行到檔案最後都可以刪掉
   在那之前要留著：各頁 JS 的 B 區都是用這裡的 StaffStore 讀寫示範訂單。
   ===================================================================== */

/* ========== B-1. 示範資料 StaffStore ==========
   所有後台頁面都透過 StaffStore 讀寫訂單，所以內場、外場、結帳、訂單管理看到的是同一份資料。
   資料暫存在瀏覽器的 localStorage（關掉分頁再打開還在，但只存在這台裝置）。

   【Thymeleaf 串接】總覽
     這一區是用來代替資料庫的。真正上線時，內場和外場是「不同的平板」，
     一定要透過伺服器的資料庫才能同步，瀏覽器暫存做不到。做法和會員功能相同，不用 JSON：

       顯示資料：Controller 把訂單清單放進 Model，HTML 用 th:each 產生畫面
       修改資料：每個按鈕包成一個小表單（th:action + method="post"）送到 Controller，
                 Controller 改完資料庫後 redirect 回同一頁
       保持同步：在 <head> 加上 <meta http-equiv="refresh" content="10">，
                 頁面就會每 10 秒自動重新載入一次，看到別台平板的最新操作

     HTML 裡「資料庫的版本」都已經用 Thymeleaf 寫好了，和假資料的版本放在同一頁：
       【資料庫資料】Controller 把資料放進 Model 之後才會出現（還沒放的時候不顯示）
       【假資料】    這裡的 StaffStore 和各頁 JS 的 B 區產生的，目前看到的就是這一份
     所以串接時可以一頁一頁來：Controller 放了資料，兩份就會同時出現在頁面上，方便對照；
     確認資料庫的版本沒問題，再刪掉那一頁的假資料。

     提示寫在哪裡（每個檔案用 Ctrl+F 搜尋「Thymeleaf 串接」或「假資料」）：
       StaffPageController.java   每個網址要準備的資料、要新增哪些 @PostMapping（先看這裡的總覽）改完後記得領回自己的java寫
       templates/staff/*.html     每一頁開頭有串接的步驟；【資料庫資料】每一塊上面寫了 Controller 要放什麼
       static/js/staff/ 各頁的 JS A 區保留、B 區是假資料（刪除的方法寫在各檔案開頭）

     四個頁面的假資料都刪掉之後，這個檔案的 B 區（從「B. 假資料」那一行到檔案最後）也整個刪掉。

     ★★ 欄位名稱目前都是「範例」★★
     下面假資料的欄位名稱（id、table、items、mealtype 等）是寫假資料時先取的；
     HTML【資料庫資料】裡的名稱也是照這裡取的。VO 的設計和屬性名稱請負責的組員照自己的想法決定，
     決定後把 HTML 裡 ${...} 用到的名稱改成 VO 實際的名稱即可（假資料這邊不用跟著改）。

     下面「一筆訂單的格式」可以當作設計資料表欄位的參考：
     訂單一張表、餐點一張表（餐點用訂單編號連回訂單），狀態存成文字或數字都可以。

   一筆訂單的格式：
     id       訂單編號
     table    桌號
     time     下單時間
     paid     是否已結帳（true／false）
     date     下單的日期（2026-10-04）
     payMethod／paidAt   付款方式（'刷卡'／'現金'）與結帳時間，未結帳時是空字串
     paidDate 結帳的日期；訂單管理的「已結帳」用這個日期分天。未結帳時沒有
     訂單本身「沒有」備註欄位（備註寫在每一道餐點上），也「沒有」狀態欄位：刪除訂單時，是把下面每一道餐點的 status 都改成「已取消」，
              所以「餐點全部是已取消」的訂單就是已取消的訂單，在「訂單管理」仍然查得到
     items    餐點清單，每一道為：
              mealtype  分類（前菜／主菜／湯品／甜品／飲品）
              name      品名
              qty       數量
              price     單價
              discount  折扣金額取出菜品價格剪掉活動菜品價格*數量
              status    製作中／等待送餐／ 已送達／ 已取消
              servedAt  送達時間(目前資料庫沒有可以再討論是否需要增加)
              note      這道餐點的備註，沒有就不用寫
                        狀態直接用中文，和資料庫訂單明細 orders_details 的 od_status 相同。
                        「已取消」是被刪除的餐點：不會真的移除，卡片上顯示成已取消、不算進金額、也不用等它送達。
                        已取消的餐點如果被復原，會回到「製作中」
   ================================================== */
const StaffStore = (() => {
  // 示範訂單的內容有修改時，把最後的數字加 1，瀏覽器就會改用新的示範訂單（舊的暫存不再使用）
  const STORAGE_KEY = 'bistroopsStaffDemoOrders7';

  // Date 物件 → '2026-10-04'
  function toDateText(date) {
    return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');
  }
  // 把日期往前或往後移幾天。shiftDate('2026-10-04', -1) → '2026-10-03'
  function shiftDate(dateText, days) {
    const [y, m, d] = dateText.split('-').map(Number);
    return toDateText(new Date(y, m - 1, d + days));
  }

  // 一開始的示範訂單。
  // 日期都用「今天、昨天、前天」來算，所以不管哪一天打開，今天都會有進行中和已結帳的訂單可以看
  function defaultOrders() {
    const today = toDateText(new Date());
    const day1 = shiftDate(today, -1);   // 昨天
    const day2 = shiftDate(today, -2);   // 前天
    // 訂單編號：依照資料庫的自增數
    const idOf = (date, seq) => date.split('-').join('') + '-' + seq;
    return [
      { id: idOf(today, '006'), date: today, table: 'A1', time: '12:30', paid: false, payMethod: '', paidAt: '',
        items: [
          { mealtype: '前菜', name: '日式和風沙拉', qty: 1, price: 220, discount: 0, status: '已送達', servedAt: '12:41' },
          { mealtype: '主菜', name: '熟成肋眼牛排', qty: 1, price: 980, discount: 0, status: '等待送餐', servedAt: '' },
          { mealtype: '主菜', name: '奶油干貝義大利麵', qty: 1, price: 420, discount: 0, status: '已送達', servedAt: '12:52' },
          { mealtype: '湯品', name: '法式洋蔥濃湯', qty: 2, price: 160, discount: 0, status: '已送達', servedAt: '12:38' }
        ] },
      { id: idOf(today, '007'), date: today, table: 'B2', time: '12:38', paid: false, payMethod: '', paidAt: '',
        items: [
          { mealtype: '前菜', name: '蒜味奶油蘑菇', qty: 1, price: 240, discount: 0, status: '已送達', servedAt: '12:49' },
          { mealtype: '主菜', name: '香煎鱸魚佐白醬', qty: 1, price: 560, discount: 0, status: '製作中', servedAt: '', note: '醬汁另外放' },
          { mealtype: '主菜', name: '松露野菇燉飯', qty: 1, price: 460, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '湯品', name: '主廚當日例湯', qty: 2, price: 120, discount: 0, status: '已送達', servedAt: '12:45' }
        ] },
      { id: idOf(today, '009'), date: today, table: 'C1', time: '12:55', paid: false, payMethod: '', paidAt: '',
        items: [
          { mealtype: '前菜', name: '義式卡布里沙拉', qty: 1, price: 280, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '主菜', name: '經典粉紅醬海鮮燉飯', qty: 1, price: 480, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '主菜', name: '牛肝菌野菇義大利麵', qty: 2, price: 420, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '甜品', name: '主廚手工提拉米蘇', qty: 3, price: 180, discount: 0, status: '製作中', servedAt: '' },
          // 這一桌特別多放幾道，用來示範「品項很多時在卡片內上下滑動」
          { mealtype: '前菜', name: '炙燒干貝佐柚子醬', qty: 1, price: 360, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '前菜', name: '酥炸花枝圈', qty: 1, price: 260, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '湯品', name: '南瓜濃湯', qty: 3, price: 140, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '主菜', name: '爐烤德國豬腳', qty: 1, price: 720, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '主菜', name: '香料烤半雞', qty: 1, price: 520, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '主菜', name: '瑪格麗特披薩', qty: 1, price: 250, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '甜品', name: '焦糖布蕾', qty: 2, price: 160, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '飲品', name: '手沖咖啡', qty: 3, price: 150, discount: 0, status: '製作中', servedAt: '' }
        ] },
      { id: idOf(today, '008'), date: today, table: 'B3', time: '12:50', paid: false, payMethod: '', paidAt: '',
        items: [
          { mealtype: '前菜', name: '凱薩沙拉', qty: 1, price: 220, discount: 0, status: '等待送餐', servedAt: '' },
          { mealtype: '主菜', name: '戰斧豬排', qty: 1, price: 620, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '主菜', name: '清炒海鮮義大利麵', qty: 1, price: 440, discount: 0, status: '製作中', servedAt: '' },
          { mealtype: '湯品', name: '法式蘑菇濃湯', qty: 2, price: 160, discount: 0, status: '製作中', servedAt: '' }
        ] },
      // 餐點已全部送達、還沒結帳
      { id: idOf(today, '005'), date: today, table: 'B1', time: '12:10', paid: false, payMethod: '', paidAt: '',
        items: [
          { mealtype: '主菜', name: '戰斧豬排', qty: 1, price: 620, discount: 0, status: '已送達', servedAt: '12:34' },
          { mealtype: '前菜', name: '莫札瑞拉起司與新鮮羅勒', qty: 1, price: 320, discount: 64, status: '已送達', servedAt: '12:22' },
          { mealtype: '主菜', name: '瑪格麗特披薩', qty: 1, price: 250, discount: 0, status: '已送達', servedAt: '12:36' },
          { mealtype: '主菜', name: '拿坡里手工餅皮', qty: 1, price: 120, discount: 0, status: '已送達', servedAt: '12:36' }
        ] },
      // 已結帳
      { id: idOf(today, '003'), date: today, table: 'A3', time: '11:20', paid: true, payMethod: '現金', paidAt: '12:25', paidDate: today,
        items: [
          { mealtype: '主菜', name: '香草烤春雞', qty: 1, price: 520, discount: 0, status: '已送達', servedAt: '11:42' },
          { mealtype: '主菜', name: '松露野菇燉飯', qty: 1, price: 460, discount: 0, status: '已送達', servedAt: '11:44' },
          { mealtype: '甜品', name: '焦糖布蕾', qty: 2, price: 160, discount: 0, status: '已送達', servedAt: '12:05' }
        ] },
      { id: idOf(today, '004'), date: today, table: 'C2', time: '11:45', paid: true, payMethod: '刷卡', paidAt: '12:48', paidDate: today,
        items: [
          { mealtype: '前菜', name: '生火腿拼盤', qty: 1, price: 420, discount: 0, status: '已送達', servedAt: '11:56' },
          { mealtype: '主菜', name: '紅酒燉牛頰', qty: 2, price: 680, discount: 136, status: '已送達', servedAt: '12:12' },
          { mealtype: '主菜', name: '熟成肋眼牛排', qty: 1, price: 980, discount: 0, status: '已送達', servedAt: '12:14' }
        ] },

      // ---------- 昨天：三筆已結帳、一筆已取消（用來示範訂單管理的日期切換） ----------
      { id: idOf(day1, '011'), date: day1, table: 'A2', time: '18:10', paid: true, payMethod: '刷卡', paidAt: '19:32', paidDate: day1,
        items: [
          { mealtype: '前菜', name: '凱薩沙拉', qty: 1, price: 220, discount: 0, status: '已送達', servedAt: '18:22' },
          { mealtype: '主菜', name: '香煎鱸魚佐白醬', qty: 2, price: 560, discount: 0, status: '已送達', servedAt: '18:40' }
        ] },
      { id: idOf(day1, '012'), date: day1, table: 'B1', time: '18:30', paid: true, payMethod: '現金', paidAt: '20:05', paidDate: day1,
        items: [
          { mealtype: '主菜', name: '戰斧豬排', qty: 2, price: 620, discount: 0, status: '已送達', servedAt: '19:02' },
          { mealtype: '主菜', name: '瑪格麗特披薩', qty: 1, price: 250, discount: 0, status: '已送達', servedAt: '18:55' },
          { mealtype: '甜品', name: '主廚手工提拉米蘇', qty: 4, price: 180, discount: 72, status: '已送達', servedAt: '19:40' }
        ] },
      { id: idOf(day1, '013'), date: day1, table: 'C1', time: '19:00', paid: true, payMethod: '刷卡', paidAt: '20:41', paidDate: day1,
        items: [
          { mealtype: '湯品', name: '南瓜濃湯', qty: 3, price: 140, discount: 0, status: '已送達', servedAt: '19:12' },
          { mealtype: '主菜', name: '爐烤德國豬腳', qty: 1, price: 720, discount: 0, status: '已送達', servedAt: '19:38' }
        ] },
      // 這一筆是被刪除的訂單：每一道餐點的狀態都是「已取消」
      { id: idOf(day1, '014'), date: day1, table: 'A1', time: '19:20', paid: false, payMethod: '', paidAt: '',
        items: [
          { mealtype: '主菜', name: '松露野菇燉飯', qty: 2, price: 460, discount: 0, status: '已取消', servedAt: '' }
        ] },

      // ---------- 前天：兩筆已結帳 ----------
      { id: idOf(day2, '008'), date: day2, table: 'B2', time: '12:15', paid: true, payMethod: '現金', paidAt: '13:20', paidDate: day2,
        items: [
          { mealtype: '主菜', name: '奶油干貝義大利麵', qty: 2, price: 420, discount: 0, status: '已送達', servedAt: '12:36' },
          { mealtype: '飲品', name: '手沖咖啡', qty: 2, price: 150, discount: 0, status: '已送達', servedAt: '12:58' }
        ] },
      { id: idOf(day2, '009'), date: day2, table: 'C2', time: '18:45', paid: true, payMethod: '刷卡', paidAt: '20:50', paidDate: day2,
        items: [
          { mealtype: '前菜', name: '生火腿拼盤', qty: 2, price: 420, discount: 0, status: '已送達', servedAt: '18:58' },
          { mealtype: '主菜', name: '熟成肋眼牛排', qty: 3, price: 980, discount: 294, status: '已送達', servedAt: '19:30' },
          { mealtype: '主菜', name: '紅酒燉牛頰', qty: 3, price: 680, discount: 0, status: '已送達', servedAt: '19:34' }
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
    shiftDate,

    // 今天的日期，格式 '2026-10-04'
    todayText() {
      return toDateText(new Date());
    },

    // 改變某一道餐點的狀態。itemIndex 是這道餐點在 items 裡的第幾個（從 0 開始）
    setItemStatus(orderId, itemIndex, status) {
      const orders = getOrders();
      const order = orders.find(o => o.id === orderId);
      if (!order || !order.items[itemIndex]) return;
      order.items[itemIndex].status = status;
      order.items[itemIndex].servedAt = status === '已送達' ? nowTime() : '';
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
      order.paidDate = toDateText(new Date());   // 結帳的日期（訂單管理用這個日期分天）
      saveOrders(orders);
    },

    // 儲存「編輯訂單」的修改。itemNotes 是陣列，第幾個就是第幾道餐點的備註
    // itemCancelled 也是陣列，第幾個是 true 就代表第幾道餐點要刪除（狀態改成「已取消」）
    saveEdits(orderId, itemNotes, itemCancelled) {
      const orders = getOrders();
      const order = orders.find(o => o.id === orderId);
      if (!order) return;
      order.items.forEach((item, index) => {
        item.note = itemNotes[index] || '';
        if (itemCancelled[index]) {
          // 刪除這道餐點：狀態改成「已取消」
          item.status = '已取消';
          item.servedAt = '';
        } else if (item.status === '已取消') {
          // 復原被刪除的餐點：原本的狀態已經不知道了，一律回到「製作中」重新做
          item.status = '製作中';
        }
      });
      saveOrders(orders);
    },

    // 刪除訂單：不會真的移除，而是把這筆訂單「每一道餐點」的狀態都改成「已取消」。
    // 訂單本身沒有另外的狀態欄位（資料庫的訂單 orders 也沒有），所以「餐點全部已取消」的訂單就算是已取消
    cancelOrder(orderId) {
      const orders = getOrders();
      const order = orders.find(o => o.id === orderId);
      if (!order) return;
      order.items.forEach(item => {
        item.status = '已取消';
        item.servedAt = '';
      });
      saveOrders(orders);
    },

    // 這筆訂單是不是已取消：每一道餐點都是「已取消」
    isCancelled(order) {
      return order.items.every(item => item.status === '已取消');
    },

    // 這筆訂單是否還在進行中（沒有結帳、也沒有被刪除）。工作面板和結帳只顯示這種訂單
    isOpen(order) {
      return !order.paid && !this.isCancelled(order);
    },

    // 回到一開始的示範訂單
    reset() {
      saveOrders(defaultOrders());
    },

    // 這筆訂單的餐點是否全部送達
    isAllServed(order) {
      return this.activeItems(order).every(item => item.status === '已送達');
    },

    // 這筆訂單裡沒有被刪除的餐點（狀態不是「已取消」的）
    activeItems(order) {
      return order.items.filter(item => item.status !== '已取消');
    },

    // 訂單的狀態：'CANCELLED' 已取消／'PAID' 已結帳／'DONE' 餐點都送達了、等結帳／'ACTIVE' 用餐中
    statusOf(order) {
      if (this.isCancelled(order)) return 'CANCELLED';
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


/* ========== B-2. 訂單狀態的對照表（各頁 JS 的 B 區用來顯示狀態標籤） ========== */
// 訂單狀態對應的中文與顏色（顏色的 class 定義在 staff.css）
const ORDER_STATUS_TEXT = {
  ACTIVE: { text: '用餐中', className: 'tag-yellow' },
  DONE:   { text: '待結帳', className: 'tag-red' },
  PAID:   { text: '已結帳', className: 'tag-green' },
  CANCELLED: { text: '已取消', className: 'tag-gray' }
};


/* ========== B-3. 主頁的「重設示範資料」 ========== */
const resetDemoBtn = document.getElementById('resetDemo');
if (resetDemoBtn) {
  resetDemoBtn.addEventListener('click', () => {
    if (!confirm('確定要把訂單回復成一開始的示範資料嗎？')) return;
    StaffStore.reset();
    staffToast('已重設示範資料');
  });
}


/* ========== B-4. 主頁「訂候位管理」長條的示範數字 ==========
   staff_index.html 的【假資料】長條裡，寫了 data-demo-home="名稱" 的地方會填入下面對應的數字。
   資料庫的版本是同一頁的【資料庫資料】長條，數字由 Controller 放進 Model（名稱和下面相同）。 */
const HOME_DEMO = {
  nextRoundTime: '18:00',   // 最近一輪訂位的時間
  nextRoundCount: 6,        // 那一輪共幾組
  waitingCount: 3,          // 目前候位幾組
  callingNumber: 12         // 目前叫到幾號
};
document.querySelectorAll('[data-demo-home]').forEach(el => {
  el.textContent = HOME_DEMO[el.dataset.demoHome];
});

// ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
// ▲▲▲  【假資料・刪除範圍 結束】刪到這一行為止（這一行也刪掉）。           ▲▲▲
// ▲▲▲   注意：要等「四個後台頁面」的假資料都刪掉之後，才能刪這一區。      ▲▲▲
// ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
