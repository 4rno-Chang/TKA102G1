/* =========================================================
   Bistroops 員工後台：共用程式（每個後台頁面都要載入，而且要放在最前面）

   這個檔案分成兩區：
     A. 共用程式（保留）：小工具、小提示、尚未開放的按鈕、選單的摺疊、時鐘、對話框、送出前先確認、主頁的工作備忘錄、結帳頁的整單折扣
     B. 假資料：示範訂單 StaffStore、訂單狀態的對照表、主頁訂候位的示範數字

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

/* ========== A-3-1. 無權限的選單與按鈕 ========== */
document.addEventListener('click', e => {
    const link = e.target.closest('[data-permission-link].no-permission');

    if (!link) return;

    e.preventDefault();
    staffToast('您沒有此功能的操作權限');
});

/* ========== A-4. 選單的摺疊群組（後臺管理） ==========
   點「後臺管理」會展開或收起下面的項目。做法是替外層的 .staff-group 加上或拿掉 open 這個 class，
   顯示與隱藏由 staff.css 處理。
   展開的狀態會記在 sessionStorage（這個分頁關掉前都記得），換到別的後台頁面時選單不會自己收起來。
   這一區和資料庫無關，之後接上 Thymeleaf 也可以保留。 */
   const MENU_GROUP_KEY = 'bistroopsStaffMenuGroupOpen';

   function setMenuGroupOpen(group, open) {
     group.classList.toggle('open', open);
     group.querySelector('[data-menu-group]')
          .setAttribute('aria-expanded', String(open));
   }

   // 頁面載入時檢查權限
   document.querySelectorAll('.staff-group').forEach(group => {

     const canManage = group.dataset.canManage === 'true';

     if (!canManage) {
       setMenuGroupOpen(group, false);
       return;
     }

     let wasOpen = false;

     try {
       wasOpen = sessionStorage.getItem(MENU_GROUP_KEY) === 'yes';
     } catch (e) {}

     setMenuGroupOpen(group, wasOpen || group.classList.contains('open'));
   });

   // 點擊後臺管理
   document.addEventListener('click', e => {

     const toggle = e.target.closest('[data-menu-group]');
     if (!toggle) return;

     const group = toggle.closest('.staff-group');
     const canManage = group.dataset.canManage === 'true';

     if (!canManage) {
       setMenuGroupOpen(group, false);
       staffToast('您沒有此功能的操作權限');
       return;
     }

     const open = !group.classList.contains('open');

     setMenuGroupOpen(group, open);

     try {
       sessionStorage.setItem(MENU_GROUP_KEY, open ? 'yes' : 'no');
     } catch (e) {}
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


/* ========== A-6. 對話框（取代瀏覽器原本的 alert／confirm） ==========
   畫面中上方跳出對話框，一定要按按鈕才會關（按旁邊、按 Esc 都不會關）。樣式在 staff.css 的「對話框」。
     staffAlert('跟原價一樣，請改折數');                         → 只有「確認」，黃色驚嘆號
     staffConfirm('確定結帳嗎？').then(ok => { if (ok) … });     → 「取消」「確認」，紅色問號，按確認 ok 是 true
     staffConfirm('確定要刪除嗎？', true)                         → 刪除這種收不回來的事：紅色警告三角形 */
// 圖示（SVG）：驚嘆號＝一條直線＋一個點；問號＝一個彎鉤＋一個點
//   刪除的警告（danger）：圓角的實心三角形，中間的驚嘆號用卡片的底色畫，看起來像挖空的
const DIALOG_ICON = {
  warn: '<svg viewBox="0 0 24 24"><path d="M12 6v8" /><circle cx="12" cy="18" r="1.4" /></svg>',
  ask: '<svg viewBox="0 0 24 24"><path d="M9 9a3 3 0 1 1 4.5 2.6c-.9.5-1.5 1.2-1.5 2.2v.4" /><circle cx="12" cy="18" r="1.4" /></svg>',
  danger: '<svg viewBox="0 0 48 44"><path class="tri" d="M24 5 43.5 39.5h-39z" /><path class="mark" d="M24 16v11.5" /><circle class="mark" cx="24" cy="33.4" r="2.5" /></svg>'
};
function staffDialog(text, withCancel, warn) {
  return new Promise(resolve => {
    const box = document.createElement('dialog');
    box.className = 'staff-dialog' + (withCancel ? ' is-confirm' : '') + (withCancel && warn ? ' is-danger' : '');
    const icon = !withCancel ? DIALOG_ICON.warn : warn ? DIALOG_ICON.danger : DIALOG_ICON.ask;
    box.innerHTML = `<span class="staff-dialog-icon" aria-hidden="true">${icon}</span>
      <p class="staff-dialog-text"></p>
      <div class="staff-dialog-btns">
        ${withCancel ? '<button type="button" class="staff-btn" value="no">取消</button>' : ''}
        <button type="button" class="staff-btn btn-pay" value="ok">確認</button>
      </div>`;
    box.querySelector('.staff-dialog-text').textContent = text;   // 用 textContent，文字裡有 < > 也不會被當成 HTML
    box.addEventListener('cancel', e => e.preventDefault());       // 按 Esc 不會關
    box.addEventListener('keydown', e => { if (e.key === 'Escape') { e.preventDefault(); e.stopPropagation(); } });
    box.addEventListener('click', e => {
      const btn = e.target.closest('button');
      if (!btn) return;
      box.close();
      box.remove();
      resolve(btn.value === 'ok');
    });
    document.body.append(box);
    box.showModal();   // showModal：蓋在所有東西上面（包括菜品管理的對話框），後面的畫面點不到
    box.querySelector(withCancel ? '[value=no]' : '[value=ok]').focus();   // 要確認的事先停在「取消」，按 Enter 不會誤送
  });
}
const staffAlert = text => staffDialog(text, false);
const staffConfirm = (text, warn) => staffDialog(text, true, warn);

/* ========== A-7. 送出前先確認 ==========
   表單寫了 data-confirm="要顯示的問句" 的話，送出前會先跳出上面的對話框，按「取消」就不送出。
   用在資料庫版本的表單，例如結帳（checkout.html）、把餐點退回製作中（workboard.html）、刪除活動、刪除菜品類別：
     <form th:action="..." method="post" data-confirm="確定結帳嗎？">
   刪除這種收不回來的事，再加上 data-confirm-warn，圖示會換成紅色驚嘆號：
     <form th:action="..." method="post" data-confirm="確定要刪除嗎？" data-confirm-warn> */
document.addEventListener('submit', e => {
  const form = e.target;
  // 刷卡的付款視窗還開著（A-8 的 staffCardPaying）：先不要做其他付款，避免一邊刷卡一邊收現金
  if (form.dataset.paying) {
    e.preventDefault();
    e.stopImmediatePropagation();
    staffAlert('刷卡付款視窗還開著，請先完成付款或關閉付款視窗');
    return;
  }
  const question = form.dataset.confirm;
  if (question && !form.dataset.confirmed) {
    e.preventDefault();             // 先不送出，等員工按按鈕
    e.stopImmediatePropagation();   // 也先不跑其他「送出時」的程式（例如假資料的提示）
    staffConfirm(question, form.hasAttribute('data-confirm-warn')).then(ok => {
      if (!ok) return;              // 按「取消」就不送出
      form.dataset.confirmed = 'yes';            // 已經問過了：下面再送一次時不要再問
      form.requestSubmit(e.submitter || undefined);   // 用同一顆按鈕再送一次
      delete form.dataset.confirmed;
    });
    return;
  }
  // 按的是刷卡按鈕（formtarget="ecpayWin"）：確認後才開小視窗，表單結果會顯示在這個視窗裡
  if (e.submitter && e.submitter.formTarget === 'ecpayWin') {
    const win = window.open('', 'ecpayWin', 'width=500,height=700');
    staffCardPaying(form, e.submitter, win);   // A-8：付款視窗開著時，刷卡按鈕顯示「付款中…」
  }
});

/* ========== A-8. 送出中：防止重複點擊，在按下去的按鈕裡顯示等待圖示 ==========
   後台所有表單共用（結帳、工作面板…）。表單送出後，在換頁之前再按都不會重複送出。
     工作面板的餐點按鈕：右側的「出餐 →」等文字換成等待圖示
     其他按鈕（現金、儲存變更、會員的確認…）：換成「等待圖示＋處理中…」
   刷卡（formtarget="ecpayWin"）另外處理：結果開在綠界小視窗，這一頁不會換頁，所以改成看小視窗關了沒（staffCardPaying）。
   等待圖示是 staff.css 的 .cloche-loader。不是表單的按鈕（例如回饋 QR）直接呼叫 staffBtnLoading／staffBtnDone。
   註：監聽 window 而不是 document，是為了排在最後執行：
       前面的程式（確認視窗按了取消、會員手機格式錯誤）擋下來的，e.defaultPrevented 是 true，就不會轉圈卡住。 */
function staffBtnLoading(btn, text = '處理中…') {
  if (!btn || btn.classList.contains('is-loading')) return;
  btn.dataset.html = btn.innerHTML;   // 記住原本的內容，結束或上一頁回來時還原
  btn.classList.add('is-loading');
  const loader = '<span class="cloche-loader"><i></i></span>';   // 按鈕裡只能放 span 這類的行內元素，不能放 div
  const state = btn.querySelector('.dish-state');
  if (state) state.innerHTML = loader;
  else btn.innerHTML = loader + text;
}
function staffBtnDone(btn) {
  if (!btn || !btn.classList.contains('is-loading')) return;
  btn.innerHTML = btn.dataset.html;
  delete btn.dataset.html;
  btn.classList.remove('is-loading');
}

/* 刷卡：綠界的付款小視窗開著的時候
     刷卡按鈕顯示「付款中…」，點它會把付款視窗叫到最前面（小視窗很容易被結帳頁蓋到後面）
     同一張表單的現金先不能按（上面「送出前先確認」看 form.dataset.paying 擋下來）
   每 0.5 秒檢查一次小視窗關了沒：關掉（付款完成會自己關、或員工按 × 取消）就恢復。
   付款成功時，小視窗會把結帳頁換成新的頁面（OrdersController 的 ecpayResult），按鈕本來就會變回原狀。 */
function staffCardPaying(form, btn, win) {
  if (!btn || !win) return;   // 小視窗被瀏覽器擋掉了：不鎖
  staffBtnLoading(btn, '付款中…');
  btn.classList.add('is-paying');   // staff.css：付款中的按鈕還可以點（點了叫出小視窗）
  form.dataset.paying = 'yes';
  const focusWin = ev => { ev.preventDefault(); win.focus(); };   // preventDefault：不要再送出一次表單
  btn.addEventListener('click', focusWin);
  const timer = setInterval(() => {
    if (!win.closed) return;
    clearInterval(timer);
    btn.removeEventListener('click', focusWin);
    btn.classList.remove('is-paying');
    delete form.dataset.paying;
    staffBtnDone(btn);
  }, 500);
}

window.addEventListener('submit', e => {
  if (e.defaultPrevented) return;                        // 被擋下來的不用等待
  if (e.submitter && e.submitter.formTarget) return;     // 刷卡：送到別的視窗
  const form = e.target;
  if (form.method === 'dialog') return;                  // <form method="dialog">：只是關掉對話框，不會換頁（例如訂候位的簡訊選擇）
  if (form.dataset.submitting) {                         // 已經在送了：第二次點擊直接擋掉
    e.preventDefault();
    return;
  }
  form.dataset.submitting = 'yes';
  const btn = e.submitter || form.querySelector('[type="submit"]');
  if (form.dataset.loadingMask === 'center' && btn) {
    // 等待圖示放在蒙版中間（工作面板的餐點）：按鈕只鎖住，裡面的「出餐 →」「● 等待送餐」等文字和排列都不動
    btn.dataset.html = btn.innerHTML;   // 上一頁回來時 staffBtnDone 會用到
    btn.classList.add('is-loading');
  } else {
    staffBtnLoading(btn);
  }
  staffAreaLock(form, e.submitter);
});

/* 送出中的蒙版：<form> 寫了 data-loading 才會加。按下去的按鈕所在的區塊蓋一層 20% 的白色，並加上 inert
   （滑鼠、手指、鍵盤都不能再按同一區塊的其他按鈕、改欄位；inert 不影響送出的內容）。按鈕本身浮在白色那一層上面。
     data-loading=".meal-modal-box"   蓋住離按鈕最近的那個區塊（寫 CSS 選擇器）
     data-loading                     沒寫值：蓋住表單本身
   從按鈕往外找：刪除菜品這種按鈕在對話框裡、表單在對話框外面（form="..."），要蓋的是按鈕所在的對話框。
   不想要蒙版的表單，拿掉 data-loading 就好；按鈕的「處理中…」照常。
   data-loading-mask="center"：改成 60% 的白色，等待圖示放在蒙版正中間（工作面板的出餐、退回、確認送達：蓋住那一道菜）。 */
function staffAreaLock(form, submitter) {
  if (!form.hasAttribute('data-loading')) return;
  const selector = form.dataset.loading;
  const area = selector ? ((submitter && submitter.closest(selector)) || form.closest(selector)) : form;
  if (!area) return;
  if (getComputedStyle(area).position === 'static') {   // 白色那一層要貼著這個區塊
    area.style.position = 'relative';
    area.dataset.posAdded = 'yes';
  }
  area.classList.add('is-submitting');
  if (form.dataset.loadingMask === 'center') {
    area.classList.add('mask-center');
    area.insertAdjacentHTML('beforeend', '<span class="mask-loader"><span class="cloche-loader"><i></i></span></span>');
  }
  area.inert = true;
}
function staffAreaUnlock(area) {
  area.classList.remove('is-submitting', 'mask-center');
  area.querySelectorAll(':scope > .mask-loader').forEach(el => el.remove());
  area.inert = false;
  if (area.dataset.posAdded) {
    area.style.position = '';
    delete area.dataset.posAdded;
  }
}

// 按「上一頁」回來時，瀏覽器可能直接拿出之前的畫面（按鈕還在轉），要恢復
window.addEventListener('pageshow', e => {
  if (!e.persisted) return;
  document.querySelectorAll('form[data-submitting]').forEach(f => delete f.dataset.submitting);
  document.querySelectorAll('form[data-paying]').forEach(f => delete f.dataset.paying);
  document.querySelectorAll('.is-paying').forEach(btn => btn.classList.remove('is-paying'));
  document.querySelectorAll('.is-loading').forEach(staffBtnDone);
  document.querySelectorAll('.is-submitting').forEach(staffAreaUnlock);
});

  document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('btnFeedbackQr');
    const dialog = document.getElementById('qrDialog');
    if (!btn || !dialog) return;   // 沒選訂單時沒有按鈕

    btn.addEventListener('click', async () => {
      staffBtnLoading(btn);   // 等 QR 資料的時候：按鈕顯示等待圖示，也不能再按（A-8）
      try {
        const res = await fetch(btn.dataset.url, {
          method: 'POST',
          body: new URLSearchParams({ order: btn.dataset.order })
        });
        const data = await res.json();
        // 有錯誤（例如找不到訂單）→ 用 msgDialog 對話框顯示，取代原本的 alert
        if (data.error) {
          document.getElementById('msgDialogText').textContent = data.error;
          document.getElementById('msgDialog').showModal();
          return;
        }

        document.getElementById('qrOrder').textContent = data.orders;
        const memberTag = document.getElementById('qrMember');
        memberTag.textContent = data.member ? `會員：${data.customer}` : '非會員';

        const box = document.getElementById('qrBox');
        box.innerHTML = '';   // 清掉上一次的 QR
        new QRCode(box, { text: data.url, width: 220, height: 220 });
        dialog.showModal();
      } finally {
        staffBtnDone(btn);    // 成功、失敗都要把按鈕還原
      }
    });

    document.getElementById('btnQrClose').addEventListener('click', () => dialog.close());

    // 列印：目前沒有接印表機，按了就當作已經印好，直接關掉 QR 對話框並顯示提示
    // （之後要真的列印，在 dialog.close() 前面加 window.print(); 即可）
    document.getElementById('btnQrPrint').addEventListener('click', () => {
      dialog.close();
      staffToast('回饋 QR Code 已列印');   // staffToast 在 staff.js
    });
    document.getElementById('btnMsgClose').addEventListener('click',
      () => document.getElementById('msgDialog').close());
  });

  // 結帳頁「會員」：打開輸入手機號碼的 memberDialog（checkout.html）
  document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('btnMember');
    const dialog = document.getElementById('memberDialog');
    if (!btn || !dialog) return;   // 不是結帳頁或沒選訂單時沒有這個按鈕

    const form = document.getElementById('memberForm');
    const phone = document.getElementById('memberPhone');
    const error = document.getElementById('memberError');
    const result = document.getElementById('memberResult');
    const submitBtn = form.querySelector('.btn-member-submit');
    let lookupSeq = 0;   // 第幾次查詢：打字很快、連續查好幾次時，只採用最後一次的結果

    // 顯示查詢結果；className：'' 查詢中／'is-found' 找到／'is-error' 查不到或失敗
    function showResult(html, className) {
      result.innerHTML = html;
      result.className = 'member-result ' + className;
      result.hidden = false;
    }

    // 清掉查詢結果（號碼改了、還沒滿 10 碼時）
    function clearResult() {
      lookupSeq++;   // 還在路上的查詢回來也不要顯示
      result.hidden = true;
      submitBtn.disabled = false;
    }

    // 向 Java 查詢這個手機號碼是不是會員（OrdersController 的 /staff/checkout/member/lookup）
    async function lookupMember(tel) {
      const seq = ++lookupSeq;
      showResult('查詢中…', '');
      submitBtn.disabled = true;   // 查到會員之前先不能按確認
      try {
        const res = await fetch(form.dataset.lookupUrl + '?phone=' + encodeURIComponent(tel));
        const data = await res.json();
        if (seq !== lookupSeq) return;   // 號碼已經改了，這個結果作廢
        if (data.found) {
          // textContent 再放進 strong：會員名稱不會被當成 HTML
          const name = document.createElement('strong');
          name.textContent = data.name || '（未填姓名）';
          showResult('會員：', 'is-found');
          result.append(name);
          submitBtn.disabled = false;
        } else {
          showResult('查無此會員', 'is-error');
        }
      } catch (e) {
        if (seq !== lookupSeq) return;
        showResult('查詢失敗，請再試一次', 'is-error');   // 例如網路斷線、沒有權限被轉到登入頁
        submitBtn.disabled = false;   // 查不到結果時還是讓員工可以送出，交給 Java 判斷
      }
    }

    btn.addEventListener('click', () => {
      form.reset();
      error.hidden = true;
      clearResult();
      dialog.showModal();
      phone.focus();
    });

    // × 和「取消」都是關閉
    document.getElementById('btnMemberClose').addEventListener('click', () => dialog.close());
    document.getElementById('btnMemberCancel').addEventListener('click', () => dialog.close());

    // 只能輸入數字；輸入滿 10 碼而且格式正確，就查詢會員
    phone.addEventListener('input', () => {
      phone.value = phone.value.replace(/\D/g, '');
      error.hidden = true;
      if (/^09\d{8}$/.test(phone.value)) lookupMember(phone.value);
      else clearResult();
    });

    // 送出前檢查格式：09 開頭、10 碼（Java 那邊也要再檢查一次）
    form.addEventListener('submit', e => {
      if (!/^09\d{8}$/.test(phone.value)) {
        e.preventDefault();
        error.hidden = false;
        phone.focus();
      }
    });
  });





/* ========== A-9. 主頁的工作備忘錄（staff_index.html 的 #workNote） ==========
   每位員工各自一份，存在 Redis（EmployeeNoteController／EmployeeNoteRedisService），換一台平板登入也看得到。
     打開頁面：GET  data-url，拿到這位員工的備忘錄放進填寫框（拿到之前顯示「讀取中…」、不能打字）
     打字：停下來 2 秒後 POST data-url（欄位 note），右下角顯示「已儲存 14:32」（存好的時間）
     離開填寫框（點到別的地方）：有還沒存的就馬上存
     只有內容改過才會送出；打開頁面、點進填寫框但沒打字，都不會呼叫伺服器
     全部用 fetch 在背景送出，頁面不會重新整理
     離開頁面時還沒存到的：用 sendBeacon 送出最後一次（換頁時也送得出去）
   讀取或儲存失敗（例如登入過期、網路斷線）時，右下角顯示原因，頁面不會出錯。 */
const workNote = document.getElementById('workNote');
if (workNote && workNote.dataset.url) {
  const noteUrl = workNote.dataset.url;
  const noteStatus = document.getElementById('workNoteStatus');
  const setStatus = text => { if (noteStatus) noteStatus.textContent = text; };
  const noteBody = () => new URLSearchParams({ note: workNote.value });
  let noteDirty = false;   // 打過字、還沒存到
  let noteTimer = 0;

  // 讀取
  workNote.readOnly = true;
  setStatus('讀取中…');
  fetch(noteUrl)
    .then(res => {
      if (!res.ok || res.redirected) throw new Error(res.status);   // redirected：登入過期被導到登入頁
      return res.text();
    })
    .then(text => {
      workNote.value = text;
      setStatus('');
    })
    .catch(() => setStatus('讀取失敗，請重新整理頁面（或重新登入）'))
    .finally(() => { workNote.readOnly = false; });

  // 儲存
  async function saveWorkNote() {
    clearTimeout(noteTimer);
    const sending = workNote.value;
    try {
      const res = await fetch(noteUrl, { method: 'POST', body: new URLSearchParams({ note: sending }) });
      if (!res.ok || res.redirected) throw new Error(res.status);
      noteDirty = workNote.value !== sending;   // 送出去的途中又打了字：還沒存完，等下一次
      if (noteDirty) return;
      const now = new Date();
      setStatus(`已儲存 ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`);
    } catch (err) {
      setStatus('儲存失敗，請稍後再試（或重新登入）');
    }
  }
  // 打字：停下來 2 秒後存
  workNote.addEventListener('input', () => {
    noteDirty = true;
    setStatus('儲存中…');
    clearTimeout(noteTimer);
    noteTimer = setTimeout(saveWorkNote, 2000);
  });
  // 離開填寫框（點到別的地方）：有還沒存的就馬上存，不用等 2 秒
  workNote.addEventListener('blur', () => {
    if (noteDirty) saveWorkNote();
  });

  // 離開頁面：還有沒存到的就送出最後一次
  window.addEventListener('pagehide', () => {
    if (!noteDirty) return;
    clearTimeout(noteTimer);
    navigator.sendBeacon(noteUrl, noteBody());
  });
}


/* ========== A-10. 結帳頁的整單折扣（checkout.html 的「整單折扣」那一行和 #promoteDialog） ==========
   1. 「加入折扣」打開對話框；×、「取消」關閉
   2. 加入、取消之後的結果訊息（Controller 的 flash：promoteMsg）用小提示顯示
   3. 加入、取消活動折扣不整頁重新整理（表單有 data-promote-ajax）：
      在背景用 fetch 送出 → Controller 照常 redirect 回 /staff/checkout?order=…，fetch 會跟著拿到那一頁的 HTML
      → 只把會變的幾塊換成新的：左邊的訂單清單（金額）、明細表格、活動折扣那一行、總計、結帳確認的金額
      會員、刷卡、回饋 QR 的按鈕不換（它們的程式是直接綁在按鈕上的，換掉就失效）。Java 不用改。
   用 (() => { ... })() 包起來：裡面的變數名稱（dialog…）不會和這個檔案其他地方衝突。 */
(() => {

// 3. 背景送出（監聽 document、寫在 A-7 後面：A-7 的「確定要取消嗎？」按了確認之後才會跑到這裡）
const SWAP_PARTS = ['.checkout-list', '.receipt-table', '[data-promote-line]', '.receipt-sum'];
document.addEventListener('submit', async e => {
  const form = e.target;
  if (!form.matches('[data-promote-ajax]')) return;
  e.preventDefault();                        // 不要整頁送出（A-8 看到也就不會再鎖一次）
  if (form.dataset.submitting) return;       // 送出中再按：不理它
  form.dataset.submitting = 'yes';
  const btn = e.submitter || form.querySelector('[type="submit"]');
  staffBtnLoading(btn);                      // 按鈕轉圈＋區塊蓋白色（A-8）
  staffAreaLock(form, e.submitter);
  try {
    const res = await fetch(form.action, { method: 'POST', body: new FormData(form) });
    const page = new DOMParser().parseFromString(await res.text(), 'text/html');
    if (!page.querySelector('[data-promote-line]')) {   // 拿到的不是結帳頁（例如登入過期被轉到登入頁）：整頁換過去
      location.href = res.url;
      return;
    }
    SWAP_PARTS.forEach(sel => {
      const now = document.querySelector(sel), fresh = page.querySelector(sel);
      if (now && fresh) now.replaceWith(fresh);
    });
    // 結帳表單的確認文字裡有實付金額，也要跟著換
    const payForm = document.querySelector('.receipt-actions'), freshPay = page.querySelector('.receipt-actions');
    if (payForm && freshPay) payForm.dataset.confirm = freshPay.dataset.confirm;

    const dialog = document.getElementById('promoteDialog');
    if (dialog && dialog.open) dialog.close();
    if (dialog) dialog.querySelector('[data-promote-form]').reset();
    const msg = document.querySelector('[data-promote-line]').dataset.promoteMsg;
    if (msg) staffToast(msg);
  } catch (err) {
    staffAlert('連線失敗，請再試一次');
  } finally {
    delete form.dataset.submitting;
    staffBtnDone(btn);
    document.querySelectorAll('.is-submitting').forEach(staffAreaUnlock);
  }
});

// 2. 結果訊息
const line = document.querySelector('[data-promote-line]');
if (line && line.dataset.promoteMsg) staffToast(line.dataset.promoteMsg);

const dialog = document.getElementById('promoteDialog');
if (!dialog) return;   // 沒選訂單時沒有對話框

// 1. 打開、關閉
document.addEventListener('click', e => {
  if (e.target.closest('[data-promote-open]')) {
    dialog.showModal();
    return;
  }
  if (e.target.closest('[data-promote-close]')) dialog.close();
});

})();


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
