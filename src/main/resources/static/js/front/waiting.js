/* =========================================================
   Bistroops 現場候位
   對應的 HTML：templates/front/waiting/waiting_status.html（候位狀況）
                templates/front/waiting/waiting_join.html（現場取號，給客人掃 QR code 開啟）
                templates/front/waiting/waiting_ticket.html（號碼牌）
   對應的 CSS ：static/css/front/waiting.css

   ※ 目前畫面用的是假資料，全部集中在「1. 假資料 WaitingDemo」這一區。
     Java 寫好後要改的地方都標了「Thymeleaf 串接」，用 Ctrl+F 搜尋這幾個字就能找到
     （這個檔案、三個 HTML、BookingPageController.java 都有）。

   上面三個頁面都會載入這個檔案。每一區開頭都會先確認「這一頁有沒有那個元素」，沒有就跳過。
   ========================================================= */

// 用 (() => { ... })() 包起來，避免這裡的變數名稱和 member.js 互相衝突
(() => {

    const $ = id => document.getElementById(id);

    // 各頁面的網址（目前專案沒有設定 context-path，所以直接寫）
    const URLS = {
        status: '/bistroops/waiting',
        join: '/bistroops/waiting/join',
        ticket: '/bistroops/waiting/ticket'
    };


    /* ========== 1. 假資料 WaitingDemo ==========
       ★ 這一整區都是假資料，用來代替資料庫，讓畫面在 Java 還沒寫好之前可以操作。★
    
       【Thymeleaf 串接】總覽
         接上資料庫後，這一區可以整個刪掉。資料改由 Controller 查出來放進 Model，
         HTML 用 th:each／th:text 顯示；送出改成表單（th:action + method="post"）送到 Controller。
         不需要 JSON。每一個函式之後由誰負責，寫在各函式上面的註解。
    
       假資料的規則（可再討論如何顯示，粗估等候時間我還不知道怎麼算）：
         候位號碼    ：2 人桌 A 開頭、4 人桌 B、6 人桌 C，加三位數字
         一開始的狀況：2 人桌叫到 A023、發到 A028；4 人桌叫到 B015、發到 B019；6 人桌叫到 C008、發到 C011
         預估等候    ：每一組抓 4 到 7 分鐘
         示範資料    ：手機 0912345678 已經有一張號碼牌 A026，可以直接拿來試「查詢候位」
         只算當日    ：換了一天，資料會回到一開始的狀況
    
       【Thymeleaf 串接】對應的資料表（依照 資料庫表格建立/createTable_v1_4.sql）
         桌型 seat_type
           seat_type_no 就是畫面上的 2／4／6
           seat_type_take_num 取號（最後發出的號碼）、seat_type_call_num 入座號（目前叫到的號碼）
           等待組數 = 取號 − 入座號（再扣掉中間已經取消的）
         候位 waiting
           seat_type_no 桌型、waiting_tel 手機、waiting_name 姓名、waiting_comment 備註（最多 20 字）、
           waiting_status 狀態；有登入會員時再存 mem_no
    
       【Thymeleaf 串接】★ 提醒：請記得建立「存放號碼牌號碼」的資料表 ★
         目前的資料庫沒有地方記「這位客人拿到幾號」：
           seat_type_take_num 只記每個桌型最後發到幾號，「候位 waiting」的資料列裡沒有號碼。
         沒有這個號碼，「查詢候位」查到資料後不知道他是幾號，「前面還有幾組」也沒辦法算。
         組內已決定另外建立資料庫的表格來存，請負責的組員記得：
           1. 在 資料庫表格建立 的 SQL 檔加上建立的語法（和範例資料），並通知大家重新建立資料庫
           2. 至少要能查出：哪一筆候位（waiting_no）、哪個桌型、號碼是幾號
           3. 寫好之後，把這個檔案和 HTML 裡提到號碼的提示（ticketNo、number）改成實際的表格與欄位名稱
         這個檔案的假資料是把號碼直接記在每一張號碼牌的 number（見下面的 loadWaiting）。
    
       【Thymeleaf 串接】★ 提醒：後臺要寫一個「每日刷新候位資料」的排程 ★
         候位只算當日，但「候位 waiting」沒有日期欄位，分不出哪些是昨天的資料，
         所以每天要在固定的時間（例如凌晨 0 點，實際時間請組內決定）把候位資料刷新一次：
           1. 清空「候位 waiting」，讓候位編號 waiting_no 從 1 重新開始
              （要用 TRUNCATE TABLE waiting；用 DELETE 只會刪資料，編號會接著昨天的繼續加）
           2. 把「桌型 seat_type」的 seat_type_take_num（取號）、seat_type_call_num（入座號）歸零
         寫法可以用 Spring 的排程：啟動類別加 @EnableScheduling，
           再寫一個方法加上 @Scheduled(cron = "0 0 0 * * *")（每天 0 點執行）。
         要注意：排程只有在程式開著的時候才會執行，那個時間如果程式沒開，昨天的候位就會留到隔天，
           建議員工後臺另外留一個「清空候位」的按鈕，可以手動刷新。
         這個檔案的假資料是用 day 記住日期、換了一天就重來（見下面的 loadWaiting），就是在模擬這個排程。
       ================================================== */
    const WaitingDemo = (() => {
        // 候位資料存在瀏覽器裡的名稱。假資料的格式有修改時，把最後的數字加 1，瀏覽器就會改用新的初始資料
        const WAITING_KEY = 'bistroopsDemoWaiting3';

        // Date 物件 → '2026-10-03'
        function toDateText(date) {
            return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');
        }

        // 【假資料】候位的資料，存在瀏覽器的 localStorage。三個位置依序是 2 人桌、4 人桌、6 人桌
        //   calling 目前叫到的號碼；issued 最後發出去的號碼
        //   tickets 有留手機的號碼牌（才查得到），status 是 'WAITING' 等候中／'CANCELLED' 已取消
        //           一開始先放一張示範的號碼牌 A026（手機 0912345678），方便試「查詢候位」
        //   day     這份資料是哪一天的；換了一天就全部重來（候位只算當日）
        // 【Thymeleaf 串接】這些都改由資料庫記錄：
        //   calling 對應「桌型 seat_type」的 seat_type_call_num（入座號），issued 對應 seat_type_take_num（取號）；
        //   tickets 對應「候位 waiting」的資料列
        const TABLE_TYPES = [2, 4, 6];
        function loadWaiting() {
            const today = toDateText(new Date());
            try {
                const saved = JSON.parse(localStorage.getItem(WAITING_KEY));
                if (saved && saved.day === today && Array.isArray(saved.tickets)) return saved;
            } catch (e) { /* 讀不到就用下面的初始值 */ }
            return {
                day: today,
                calling: [23, 15, 8],
                issued: [28, 19, 11],
                tickets: [
                    { ticketNo: 'A026', tableType: 2, number: 26, name: '陳小姐', phone: '0912345678', note: '需要兒童椅', takenTime: '17:42', status: 'WAITING' }
                ]
            };
        }
        function saveWaiting(waiting) {
            try { localStorage.setItem(WAITING_KEY, JSON.stringify(waiting)); } catch (e) { /* 存不了就算了 */ }
        }

        // 號碼的寫法：A023
        function ticketNo(index, number) {
            return 'ABC'[index] + String(number).padStart(3, '0');
        }
        // 【假資料】預估等候時間：每一組抓 4 到 7 分鐘
        // 【Thymeleaf 串接】由 Controller（或 Service）算好文字，放在 queues 每一項和號碼牌的 estimate。
        //   怎麼估可以自己決定，例如用最近幾組從取號到入座實際花的時間來算
        function estimate(groups) {
            return groups <= 0 ? '不需等候' : `約 ${groups * 4}-${groups * 7} 分鐘`;
        }

        // 某個桌型裡，號碼介於「目前叫號」和 upTo 之間、而且還在等的有幾組（已取消的不算）
        function countWaiting(waiting, index, upTo) {
            const cancelled = waiting.tickets.filter(t =>
                t.status === 'CANCELLED' && t.tableType === TABLE_TYPES[index]
                && t.number > waiting.calling[index] && t.number <= upTo).length;
            return upTo - waiting.calling[index] - cancelled;
        }

        // 【假資料】目前是否開放取號（想看「已停止取號」的畫面，把 true 改成 false）
        // 【Thymeleaf 串接】Controller 依照營業時間判斷後放進 Model：open
        function isWaitingOpen() {
            return true;
        }

        // 【假資料】三個桌型目前的叫號狀況
        // 【Thymeleaf 串接】Controller 查資料庫放進 Model：queues（每一個有 tableType、callingNo、waitingCount、estimate）
        function queues() {
            const waiting = loadWaiting();
            return TABLE_TYPES.map((tableType, i) => {
                const count = countWaiting(waiting, i, waiting.issued[i]);
                return { tableType, callingNo: ticketNo(i, waiting.calling[i]), waitingCount: count, estimate: estimate(count) };
            });
        }

        // 把一張號碼牌加上「前面還有幾組」和「預估等候」（這兩個會隨時間變，所以每次用到都重新算）
        function withProgress(waiting, ticket) {
            const i = TABLE_TYPES.indexOf(ticket.tableType);
            const ahead = countWaiting(waiting, i, ticket.number) - 1;   // 扣掉自己
            return Object.assign({}, ticket, { ahead, estimate: estimate(ahead + 1) });
        }

        // 【假資料】取號：把那個桌型最後發出的號碼加 1，當作這位客人的號碼，並記下來
        // 【Thymeleaf 串接】改成表單送到 Controller（POST /bistroops/waiting/join），由 Controller：
        //   把那個桌型的 seat_type_take_num 加 1 當作號碼，並新增一筆「候位 waiting」
        //   （seat_type_no、waiting_tel、waiting_name、waiting_comment、waiting_status；有登入會員再存 mem_no）
        function joinWaiting(tableType, name, phone, note) {
            const waiting = loadWaiting();
            const i = Math.max(0, TABLE_TYPES.indexOf(Number(tableType)));
            waiting.issued[i] += 1;
            const now = new Date();
            const ticket = {
                ticketNo: ticketNo(i, waiting.issued[i]),
                tableType: TABLE_TYPES[i],
                number: waiting.issued[i],
                name, phone, note,
                takenTime: String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0'),
                status: 'WAITING'
            };
            waiting.tickets.push(ticket);
            saveWaiting(waiting);
            return withProgress(waiting, ticket);
        }

        // 【假資料】用手機查詢今天的候位：只列出還在等候的號碼牌（已取消、已經叫過號的不列）
        // 【Thymeleaf 串接】Controller 用手機查資料庫（當日、等候中），把清單放進 Model
        function findTickets(phone) {
            const waiting = loadWaiting();
            return waiting.tickets
                .filter(t => t.phone === phone && t.status === 'WAITING'
                    && t.number > waiting.calling[TABLE_TYPES.indexOf(t.tableType)])
                .map(t => withProgress(waiting, t));
        }

        // 【假資料】取消候位：不刪除，只把狀態改成已取消（後面的人「前面還有幾組」會少一組）
        // 【Thymeleaf 串接】改成小表單送到 Controller（POST /bistroops/waiting/cancel）
        function cancelTicket(number) {
            const waiting = loadWaiting();
            const target = waiting.tickets.find(t => t.ticketNo === number);
            if (target) target.status = 'CANCELLED';
            saveWaiting(waiting);
        }

        return { isWaitingOpen, queues, joinWaiting, findTickets, cancelTicket };
    })();


    /* ========== 2. 頁面之間暫時傳遞資料（也是假資料的一部分） ==========
       取號之後要換到「號碼牌」那一頁，但目前沒有送到後端，所以號碼牌的內容先存在瀏覽器的 sessionStorage，
       下一頁再讀出來（關掉分頁就會清掉）。
    
       【Thymeleaf 串接】接上資料庫後這一區可以刪掉：
         取號的表單送到 Controller 存進資料庫，再 redirect 到號碼牌的頁面；
         號碼牌的頁面由 Controller 用手機或號碼從資料庫查出來顯示。 */
    const TICKET_KEY = 'bistroopsWaitingTicket';

    function saveTemp(key, data) {
        try { sessionStorage.setItem(key, JSON.stringify(data)); } catch (e) { /* 存不了就算了 */ }
    }
    function loadTemp(key) {
        try { return JSON.parse(sessionStorage.getItem(key)); } catch (e) { return null; }
    }


    /* ========== 3. 小工具、頁首頁尾 ==========
       這一區和資料無關，接上資料庫後保留。
       頁首頁尾的部分和首頁 main.js 的第 1、4、6 區相同：首頁的 main.js 還包含輪播、常見問題等
       只有首頁才有的程式，在這幾頁載入會出錯，所以把需要的部分另外寫在這裡。 */

    // 把文字放進某個元素；找不到那個元素就不做事
    function setText(id, text) {
        const el = $(id);
        if (el) el.textContent = text;
    }
    // 把文字中的 < > & " ' 換掉，避免客人填的內容（姓名、備註）被當成 HTML 執行
    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>"']/g, ch => (
            { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
        ));
    }

    const menuToggle = $('menuToggle');
    const mainNav = $('mainNav');
    if (menuToggle && mainNav) {
        // 手機版的漢堡選單
        menuToggle.addEventListener('click', () => {
            const isOpen = mainNav.classList.toggle('open');
            menuToggle.classList.toggle('open', isOpen);
            menuToggle.setAttribute('aria-expanded', isOpen);
        });
    }

    const siteHeader = $('siteHeader');
    const backToTop = $('backToTop');
    window.addEventListener('scroll', () => {
        if (siteHeader) siteHeader.classList.toggle('scrolled', window.scrollY > 10);    // 捲動後頁首出現陰影
        if (backToTop) backToTop.classList.toggle('show', window.scrollY > 400);         // 捲動一段後出現「回到頂端」
    });
    if (backToTop) backToTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));

    setText('year', new Date().getFullYear());


    /* ========== 4. 候位狀況 ==========
       【Thymeleaf 串接】接上資料庫後，叫號卡片改用 th:each 產生（見 waiting_status.html），renderQueues 可以刪掉。
         但「每 60 秒自動刷新」的程式（startAutoRefresh、stopAutoRefresh）要保留：
         頁面每次重新載入，看到的就是當時資料庫的狀況；查詢候位的對話框打開時則要先停止刷新。 */
    const queueGrid = $('queueGrid');

    // 產生三種桌型的叫號卡片（取消候位之後等待組數會變，所以寫成函式方便重新產生）
    function renderQueues() {
        queueGrid.innerHTML = WaitingDemo.queues().map(queue => `
    <article class="queue-card">
      <h2 class="queue-head">${queue.tableType} 人桌</h2>
      <div class="queue-body">
        <p class="queue-label">目前叫號</p>
        <p class="queue-no">${queue.callingNo}</p>
        <p class="queue-info">等待組數 <strong>${queue.waitingCount}</strong> 組</p>
        <p class="queue-info">預估等候 <strong>${queue.estimate}</strong></p>
      </div>
    </article>`).join('');
    }

    // 每 60 秒重新載入頁面一次。用計時器（而不是 <meta http-equiv="refresh">），
    // 是因為計時器可以隨時停止：查詢候位的對話框打開時要先停下來，否則客人打字打到一半頁面就被刷新了
    const REFRESH_SECONDS = 60;
    let refreshTimer = null;
    function startAutoRefresh() {
        stopAutoRefresh();
        refreshTimer = setTimeout(() => location.reload(), REFRESH_SECONDS * 1000);
    }
    function stopAutoRefresh() {
        clearTimeout(refreshTimer);
        refreshTimer = null;
    }

    if (queueGrid) {
        const open = WaitingDemo.isWaitingOpen();
        $('waitingBar').classList.toggle('is-closed', !open);
        setText('waitingTitle', open ? '目前開放取號' : '目前已停止取號');

        const now = new Date();
        setText('refreshTime', [now.getHours(), now.getMinutes(), now.getSeconds()].map(n => String(n).padStart(2, '0')).join(':'));

        renderQueues();
        startAutoRefresh();
    }


    /* ========== 5. 現場取號的表單 ==========
       【Thymeleaf 串接】接上資料庫後：
         桌型的選項改用 th:each 產生（見 waiting_join.html），下面產生選項的程式刪掉；
         「更新摘要」的 showQueue 保留（它讀的是選項上的 data-waiting、data-estimate）；
         送出改成讓表單正常送到 Controller，只保留「檢查不通過就 e.preventDefault()」。
    
       送出前的檢查（checkContact）要保留，讓客人不用等頁面重新載入就能看到錯誤；
       但 Controller 也要用同樣的規則再檢查一次，因為瀏覽器的檢查是可以被跳過的。 */
    const PHONE_RE = /^09\d{8}$/;   // 手機：09 開頭共 10 碼

    // 沒問題回傳空字串，有問題回傳要顯示的錯誤訊息
    function checkContact(form) {
        // namedItem('name') 是取出表單裡 name="name" 的那個欄位
        const name = form.elements.namedItem('name');
        const phone = form.elements.namedItem('phone');

        if (name.value.trim() === '') return '請輸入聯絡人姓名';
        if (!PHONE_RE.test(phone.value.trim())) return '請輸入正確的手機號碼（09 開頭共 10 碼）';
        return '';
    }

    // 摘要跟著輸入的內容更新：輸入框上寫 data-summary="sumName"，內容就會同步顯示到 id 是 sumName 的元素
    document.querySelectorAll('[data-summary]').forEach(input => {
        const show = () => setText(input.dataset.summary, input.value.trim());
        input.addEventListener('input', show);
        show();   // 一進來先顯示一次（有登入會員時，輸入框已經有預設值）
    });

    const joinForm = $('joinForm');

    if (joinForm) {
        const showQueue = () => {
            const picked =
                joinForm.querySelector('input[name="tableType"]:checked');

            if (!picked) return;

            setText('sumTable', `${picked.value} 人桌`);
            setText('sumWaiting', `${picked.dataset.waiting} 組`);
            setText('sumEstimate', picked.dataset.estimate);
        };

        joinForm.addEventListener('change', e => {
            if (e.target.name === 'tableType') showQueue();
        });

        showQueue();

        joinForm.addEventListener('submit', e => {
            let message = checkContact(joinForm);

            if (!joinForm.querySelector('input[name="tableType"]:checked')) {
                message = '請選擇桌型';
            }

            const errorBox = $('contactError');
            errorBox.textContent = message;
            errorBox.hidden = message === '';

            // 有錯誤才阻止送出；通過時讓表單正常 POST 到 Java。
            if (message !== '') {
                e.preventDefault();
            }
        });
    }

    /* ========== 6. 號碼牌 ==========
       【Thymeleaf 串接】接上資料庫後這一區整個刪掉，HTML 改用 th:text 顯示 Controller 查出來的資料。 */
    


    /* ========== 7. 查詢候位（候位狀況的頁面） ==========
       輸入手機 → 顯示今天還在等候的號碼、前面還有幾組、預估等候 → 可以取消候位，但不能修改。
       對話框打開時停止自動刷新，關閉後恢復。
    
       【Thymeleaf 串接】接上資料庫後（HTML 的寫法見 waiting_status.html）：
         查詢：表單用 GET 把手機送回同一頁（/bistroops/waiting?lookupPhone=09...），
               Controller 查出號碼牌放進 Model，頁面重新載入後對話框直接是打開的。
         取消：小表單 POST 到 /bistroops/waiting/cancel，Controller 取消後 redirect 回同一個查詢網址。
         自動刷新：頁面載入時如果對話框是打開的（Controller 有回傳查詢結果），就不要啟動自動刷新。
                   把第 4 區最後的 startAutoRefresh() 改成 if (waitLookup.hidden) startAutoRefresh();
                   （waitLookup 是在這一區才取得的，所以那一行要移到這一區的最後面）
       改完後這一區只留下對話框的開關（含停止與恢復自動刷新），和「取消前跳確認視窗」：
         list.addEventListener('submit', e => { if (!confirm('確定要取消候位嗎？')) e.preventDefault(); });
       renderTicketList、送出查詢時呼叫 WaitingDemo 的部分都刪掉。 */
    const waitLookup = $('waitLookup');
    if (waitLookup) {
        const form = $('waitLookupForm');
        const list = $('waitLookupList');
        const error = $('waitLookupError');
        let searchedPhone = '';   // 目前清單顯示的是哪一支手機的號碼牌

        function renderTicketList() {
            const found = WaitingDemo.findTickets(searchedPhone);
            if (found.length === 0) {
                list.innerHTML = '<p class="lookup-empty">這支手機今天沒有等候中的號碼。</p>';
                return;
            }
            // 每張號碼牌：左邊大字的號碼，中間第一行是桌型，下面幾行小字（前面幾組、預估等候、取號時間、備註）各自一行
            list.innerHTML = found.map(t => `
      <article class="lookup-item">
        <p class="lookup-ticket-no">${escapeHtml(t.ticketNo)}</p>
        <div class="lookup-item-main">
          <p class="lookup-item-title">${escapeHtml(t.tableType)} 人桌</p>
          <p class="lookup-item-info">前面還有 ${escapeHtml(t.ahead)} 組</p>
          <p class="lookup-item-info">預估等候 ${escapeHtml(t.estimate)}</p>
          <p class="lookup-item-info">取號時間 ${escapeHtml(t.takenTime)}</p>
          ${t.note ? `<p class="lookup-item-info">備註：${escapeHtml(t.note)}</p>` : ''}
        </div>
        <button type="button" class="btn-small danger" data-cancel-ticket="${escapeHtml(t.ticketNo)}">取消候位</button>
      </article>`).join('');
        }

        form.addEventListener('submit', e => {
            e.preventDefault();
            const phone = form.elements.namedItem('phone').value.trim();
            const ok = PHONE_RE.test(phone);
            error.textContent = ok ? '' : '請輸入正確的手機號碼（09 開頭共 10 碼）';
            error.hidden = ok;
            if (!ok) { list.innerHTML = ''; return; }
            searchedPhone = phone;
            renderTicketList();
        });

        // 取消候位：先確認，再標記成已取消；後面頁面上的等待組數也要跟著更新
        list.addEventListener('click', e => {
            const btn = e.target.closest('[data-cancel-ticket]');
            if (!btn) return;
            if (!confirm(`確定要取消 ${btn.dataset.cancelTicket} 的候位嗎？取消後需要重新取號。`)) return;
            WaitingDemo.cancelTicket(btn.dataset.cancelTicket);
            renderTicketList();
            renderQueues();
        });

        // ---------- 對話框的開關（和資料無關，接上資料庫後保留） ----------
        //   按鈕上寫 data-open-lookup="waitLookup" → 點了打開對話框
        //   對話框裡寫 data-lookup-close 的元素（背景、右上角的 ×）→ 點了關閉；按 Esc 也會關閉
        function openLookup() {
            waitLookup.hidden = false;
            document.body.classList.add('modal-open');   // 對話框打開時，後面的頁面不能捲動（樣式在 member.css）
            stopAutoRefresh();                           // 對話框打開：停止自動刷新
            form.elements.namedItem('phone').focus();
        }
        function closeLookup() {
            if (waitLookup.hidden) return;
            waitLookup.hidden = true;
            document.body.classList.remove('modal-open');
            startAutoRefresh();                          // 對話框關閉：重新開始計時
        }
        document.addEventListener('click', e => {
            const opener = e.target.closest('[data-open-lookup]');
            if (opener && opener.dataset.openLookup === waitLookup.id) openLookup();
        });
        waitLookup.addEventListener('click', e => {
            if (e.target.closest('[data-lookup-close]')) closeLookup();
        });
        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') closeLookup();
        });
    }

})();
