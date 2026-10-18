(() => {
// A-1. 面板的開關
function syncScrollLock() {
  const anyOpen = document.querySelector('.order-sheet:not([hidden])') !== null;
  document.body.classList.toggle('order-lock', anyOpen);
}

function openSheet(sheet, returnTo) {
  if (!sheet) return;
  document.querySelectorAll('.order-sheet').forEach(other => { if (other !== sheet) other.hidden = true; });
  sheet.dataset.returnTo = returnTo || '';
  sheet.hidden = false;
  syncScrollLock();
}

function closeSheets() {
  let returnTo = '';
  document.querySelectorAll('.order-sheet').forEach(sheet => {
    if (!sheet.hidden && sheet.dataset.returnTo) returnTo = sheet.dataset.returnTo;
    sheet.hidden = true;
  });
  if (returnTo) openSheet(document.getElementById(returnTo));
  else syncScrollLock();
}
document.addEventListener('click', e => {
  const opener = e.target.closest('[data-sheet-open]');
  if (opener) {
    openSheet(document.getElementById(opener.dataset.sheetOpen));
    return;
  }
  if (e.target.closest('[data-sheet-close]')) closeSheets();
});
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') closeSheets();
});
syncScrollLock();

// A-2. 數量的 −、＋
document.addEventListener('click', e => {
  const stepBtn = e.target.closest('[data-stepper] [data-step]');
  if (!stepBtn) return;
  const input = stepBtn.closest('[data-stepper]').querySelector('input[type="number"]');
  const min = Number(input.min) || 1;
  const max = Number(input.max) || 10;   // 每一品項最多 10 份（跟 CartRedisService.MAX_NUM 一樣）
  const current = Number(input.value) || min;

  input.value = Math.max(min, Math.min(max, current + Number(stepBtn.dataset.step)));

  const sheet = stepBtn.closest('.order-sheet-item');
  if (sheet) updateItemTotal(sheet);
});

// A-3. 送出前先確認：表單寫了 data-confirm="要問的話"，送出前跳出自己的對話框（取代瀏覽器原本的 confirm）
//   一定要按「取消」或「確認」才會關（按旁邊、按 Esc 都不會關）。樣式在 order.css 的「確認對話框」
function orderConfirm(text) {
  return new Promise(resolve => {
    const box = document.createElement('dialog');
    box.className = 'order-confirm';
    box.innerHTML = `<p class="order-confirm-label">CONFIRM</p>
      <p class="order-confirm-text"></p>
      <div class="order-confirm-btns">
        <button type="button" class="order-confirm-btn is-ok" value="ok">確認</button>
        <button type="button" class="order-confirm-btn" value="no">取消</button>
      </div>`;
    box.querySelector('.order-confirm-text').textContent = text;   // 用 textContent，文字裡有 < > 也不會被當成 HTML
    box.addEventListener('cancel', e => e.preventDefault());       // 按 Esc 不會關
    box.addEventListener('click', e => {
      const btn = e.target.closest('button');
      if (!btn) return;
      box.close();
      box.remove();
      resolve(btn.value === 'ok');
    });
    document.body.append(box);
    box.showModal();                             // 蓋在所有東西上面，後面的畫面點不到
    box.querySelector('[value=no]').focus();     // 先停在「取消」，不小心按 Enter 不會送出
  });
}

document.addEventListener('submit', e => {
  const form = e.target;
  const message = form.dataset.confirm;
  if (!message || form.dataset.confirmed) return;
  e.preventDefault();             // 先不送出，等客人按按鈕
  e.stopImmediatePropagation();   // 也先不跑下面 A-8 的「背景送出」
  orderConfirm(message).then(ok => {
    if (!ok) return;              // 按「取消」就不送出
    form.dataset.confirmed = 'yes';                // 已經問過了：再送一次時不要再問
    form.requestSubmit(e.submitter || undefined);  // 用同一顆按鈕再送一次（清空會走 A-8 的背景送出）
    delete form.dataset.confirmed;
  });
});

// A-4. 菜品的對話框

function updateItemTotal(sheet) {
  const num = Math.max(1, Math.floor(Number(sheet.querySelector('[data-item-input="num"]').value)) || 1);
  sheet.querySelector('[data-item-field="total"]').textContent = Number(sheet.dataset.price) * num;
}

function openItemSheet(opener) {
  const sheet = document.getElementById(opener.dataset.itemOpen);
  if (!sheet) return;
  const isEdit = opener.dataset.itemEdit !== undefined;

  let card = opener;
  if (isEdit) {
    card = document.querySelector('[data-item-card="' + sheet.id + '"][data-meal-no="' + opener.dataset.mealNo + '"]');
  }

  const data = card ? card.dataset : opener.dataset;
  const price = Number(data.price);
  const origPrice = Number(data.origPrice || data.price);

  const field = name => sheet.querySelector('[data-item-field="' + name + '"]');
  field('name').textContent = data.name;
  field('exp').textContent = data.exp || '';
  field('promo').textContent = data.promoName || '';
  field('promo').hidden = !data.promoName;
  field('orig').textContent = 'NT$ ' + origPrice;
  field('orig').hidden = origPrice === price;
  field('price').textContent = 'NT$ ' + price;
  field('submit').textContent = isEdit ? '更新購物車' : '加入購物車';

  const cardImg = card ? card.querySelector('img') : null;
  if (cardImg) sheet.querySelector('[data-item-img]').src = cardImg.src;

  const input = name => sheet.querySelector('[data-item-input="' + name + '"]');
  input('mealNo').value = opener.dataset.mealNo;
  input('num').value = isEdit ? opener.dataset.num : 1;
  input('comment').value = isEdit ? (opener.dataset.comment || '') : '';

  const oldComment = input('oldComment');
  if (oldComment) oldComment.value = isEdit ? (opener.dataset.comment || '') : '';

  sheet.dataset.price = price;
  sheet.querySelector('[data-item-remove]').hidden = !isEdit;
  updateItemTotal(sheet);
  renderNoteTags(sheet, data.tags);

  const form = sheet.querySelector('form[data-add-action]');
  if (form) form.action = isEdit ? form.dataset.editAction : form.dataset.addAction;

  const from = opener.closest('.order-sheet');
  openSheet(sheet, from ? from.id : '');
}

document.addEventListener('click', e => {
  const opener = e.target.closest('[data-item-open]');
  if (opener) openItemSheet(opener);
});

document.addEventListener('keydown', e => {
  if (e.key !== 'Enter' && e.key !== ' ') return;
  const card = e.target.closest('[data-item-open][role="button"]');
  if (!card || e.target !== card) return;
  e.preventDefault();
  openItemSheet(card);
});

document.addEventListener('input', e => {
  const sheet = e.target.closest('.order-sheet-item');
  if (sheet && e.target.matches('[data-item-input="num"]')) updateItemTotal(sheet);
});

// A-5. 常用備註（依類別的標籤代號顯示不同的標籤）
const NOTE_TAGS = {
  '前菜': ['人到齊再上', '需分食盤', '醬汁分開'],
  '主餐': ['不要蒜', '不要洋蔥', '不要辣'],
  '甜點': ['跟主餐一起上', '餐後上'],
  '飲料': ['去冰', '溫的', '餐前上', '餐後上']
};

const NOTE_TAG_GROUPS = [
  ['餐前上', '餐後上', '跟主餐一起上']
];

function sameGroupTags(tag) {
  const group = NOTE_TAG_GROUPS.find(g => g.includes(tag));
  return group ? group.filter(t => t !== tag) : [];
}

function renderNoteTags(sheet, code) {
  const row = sheet.querySelector('[data-item-tags-row]');
  if (!row) return;
  const tags = NOTE_TAGS[code] || [];

  sheet.querySelector('[data-item-tags]').innerHTML = tags.map(tag =>
    '<button type="button" class="order-tag" data-note-tag="' + tag + '">' + tag + '</button>').join('');
  sheet.querySelector('[data-item-tags-hint]').hidden = true;
  row.hidden = tags.length === 0;
}

document.addEventListener('click', e => {
  const tagBtn = e.target.closest('[data-note-tag]');
  if (!tagBtn) return;
  const sheet = tagBtn.closest('.order-sheet-item');
  const input = sheet.querySelector('[data-item-input="comment"]');
  const hint = sheet.querySelector('[data-item-tags-hint]');
  const text = tagBtn.dataset.noteTag;

  hint.hidden = true;

  const parts = input.value.trim().split('、').map(p => p.trim()).filter(p => p);
  if (parts.includes(text)) return;

  const others = sameGroupTags(text);
  const kept = parts.filter(p => !others.includes(p));
  kept.push(text);
  const next = kept.join('、');

  if (input.maxLength > 0 && next.length > input.maxLength) {
    hint.hidden = false;
    return;
  }
  input.value = next;
});

// A-6. 頁首：往下捲動之後出現陰影
const siteHeader = document.getElementById('siteHeader');
if (siteHeader) {
  const onScroll = () => siteHeader.classList.toggle('scrolled', window.scrollY > 10);
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
}

// A-7. 同桌的手機購物車互通（WebSocket）

function connectCartSocket() {
  const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
  const socket = new WebSocket(protocol + location.host + '/bistroops/order/ws');

  socket.addEventListener('message', async e => {
    if (e.data !== 'cart-changed') return;
    if (cartBusy || Date.now() - lastCartAction < 2000) return;
    try {
      const res = await fetch(location.pathname);
      const result = swapCartArea(await res.text());
      if (!result.ok) {
        location.reload();
        return;
      }
      showToast('同桌的人更新了購物車');
    } catch (err) {
      // 網路暫時有問題：下一次有通知時再更新
    }
  });

  socket.addEventListener('close', () => setTimeout(connectCartSocket, 3000));
}

connectCartSocket();

// A-8. 購物車的動作不重新載入頁面
let cartBusy = false;
let lastCartAction = 0;
let toastTimer = null;

function showToast(text) {
  const toast = document.getElementById('orderToast');
  if (!toast || !text) return;
  toast.textContent = text;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 2000);
}

function swapCartArea(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const newBar = doc.querySelector('.order-bar');
  const oldBar = document.querySelector('.order-bar');
  if (!newBar || !oldBar || !doc.getElementById('dbCartSheet')) return { ok: false };

  oldBar.replaceWith(newBar);
  // 購物車、已點餐點：面板本身留著，只換裡面的內容（標題列、品項列表、下方的合計和按鈕）。
  //   整個面板換掉的話，order.css「從右邊滑進來」的動畫會再播一次，看起來像收起來又展開。
  //   品項列表那一層（.order-sheet-body）是捲動的那一層，也留著、只換裡面的東西，捲到哪裡就停在哪裡。
  ['dbCartSheet', 'dbOrderedSheet'].forEach(id => {
    const oldSheet = document.getElementById(id);
    const newSheet = doc.getElementById(id);
    if (!oldSheet || !newSheet) return;
    ['.order-sheet-head', '.order-sheet-foot'].forEach(part => {
      const oldPart = oldSheet.querySelector(part);
      const newPart = newSheet.querySelector(part);
      if (oldPart && newPart) oldPart.replaceWith(newPart);
    });
    const oldBody = oldSheet.querySelector('.order-sheet-body');
    const newBody = newSheet.querySelector('.order-sheet-body');
    if (oldBody && newBody) oldBody.replaceChildren(...newBody.childNodes);
  });
  syncScrollLock();

  const alert = doc.querySelector('.order-alert');
  return { ok: true, message: alert ? alert.textContent.trim() : '' };
}

document.addEventListener('submit', async e => {
  if (e.defaultPrevented) return;
  const form = e.target;
  const submitter = e.submitter;
  const action = submitter && submitter.hasAttribute('formaction') ? submitter.formAction : form.action;
  if (!action.includes('/order/cart/')) return;

  e.preventDefault();
  const body = new URLSearchParams(new FormData(form));
  if (submitter && submitter.name) body.append(submitter.name, submitter.value);

  // 送出中：鎖住按下去的按鈕，蓋上 20% 白色防止重複點擊（booking-loading.js 的 bookingLock）
  //   菜品的對話框：只蓋住填寫的表單（照片在表單外面，不會被蓋到）；購物車：蓋住整個購物車
  //   購物車裡數量的 −、＋：按鈕維持原樣（不顯示等待圖示），只蓋住那一個品項（含「編輯」），其他品項照常可以按
  let lockBtn = submitter || form.querySelector('[type="submit"]');
  let lockArea = form.closest('.order-sheet-item') ? form : form.closest('.order-sheet-panel');
  if (lockBtn && lockBtn.closest('.order-stepper')) {
    lockBtn = null;
    lockArea = form;   // form.order-cart-item：一個品項
  }
  bookingLock(lockBtn, lockArea);

  cartBusy = true;
  try {
    const res = await fetch(action, { method: 'POST', body });
    const result = swapCartArea(await res.text());
    if (!result.ok) {
      location.reload();
      return;
    }
    bookingUnlock(lockBtn, lockArea);   // 購物車整塊已經換成新的；菜品的對話框還是同一個，要自己解開
    if (form.closest('.order-sheet-item')) closeSheets();
    showToast(result.message);
  } catch (err) {
    if (submitter && submitter.name) {
      const hidden = document.createElement('input');
      hidden.type = 'hidden';
      hidden.name = submitter.name;
      hidden.value = submitter.value;
      form.appendChild(hidden);
    }
    form.action = action;
    form.submit();
  } finally {
    cartBusy = false;
    lastCartAction = Date.now();
  }
});

// A-9. 捲動時，類別按鈕標出目前看到的類別（加上 is-current，顏色跟點下去時一樣，樣式在 order.css）
//   判定線：往下捲和往上捲用不同的線，換的時機才自然
//     往下捲：類別標題被「點類別跳過來」時停的位置（.order-type 的 scroll-margin-top，也就是頁首＋上方類別那一排的下方）。
//             標題往上捲過這條線，就換成那個類別。
//     往上捲：畫面正中間。目前類別的標題往下退到畫面一半，才換回上一個類別（畫面上半部已經大多是上一個類別了）。
//     往下捲時不會換回前面的類別、往上捲時不會跳到後面的類別（兩條線中間的那一段，維持原本的不變）。
//   捲到頁面最底：直接標最後一個類別（最後一個類別比較短時，標題可能到不了判定線）。
//   點類別跳過去：點下去的那顆立刻亮，頁面滑動停下來之後才恢復自動判斷（不然滑過去的途中會一路閃過中間的類別）。
//   手機的類別是上方一排、可以左右滑：選取中的那顆被擠到外面時，那一排自動橫向滑過去讓它露出來。
const catNav = document.querySelector('.order-cats');
const catLinks = catNav ? [...catNav.querySelectorAll('.order-cat')] : [];
const catSections = catLinks.map(a => document.getElementById(a.getAttribute('href').slice(1)));

function setCurrentCat(link) {
  if (!link || link.classList.contains('is-current')) return;
  catLinks.forEach(a => {
    a.classList.remove('is-current');
    a.removeAttribute('aria-current');
  });
  link.classList.add('is-current');
  link.setAttribute('aria-current', 'true');

  // 類別那一排放不下、可以左右滑的時候（手機），把這顆滑到中間；電腦版是直排，全部看得到，不用滑
  if (catNav.scrollWidth > catNav.clientWidth) {
    const navBox = catNav.getBoundingClientRect();
    const linkBox = link.getBoundingClientRect();
    catNav.scrollTo({
      left: catNav.scrollLeft + (linkBox.left - navBox.left) - (navBox.width - linkBox.width) / 2,
      behavior: 'smooth'
    });
  }
}

// dir：'down' 往下捲、'up' 往上捲；沒有給（剛打開、改變視窗大小、點類別跳完）就用往下捲的線直接算
function updateCurrentCat(dir) {
  if (catLinks.length === 0) return;
  const atBottom = window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2;
  if (atBottom) {
    setCurrentCat(catLinks[catLinks.length - 1]);
    return;
  }
  let index = 0;   // 還沒捲到第一個類別時，標第一個
  catSections.forEach((section, i) => {
    if (!section) return;
    const line = (dir === 'up')
      ? window.innerHeight / 2                                          // 往上捲：畫面正中間
      : parseFloat(getComputedStyle(section).scrollMarginTop) + 2;      // 往下捲：頁首（＋類別那一排）的下方
    if (section.getBoundingClientRect().top <= line) index = i;
  });
  const nowIndex = catLinks.findIndex(a => a.classList.contains('is-current'));
  if (nowIndex >= 0) {
    if (dir === 'down') index = Math.max(index, nowIndex);   // 往下捲：不換回前面的類別
    if (dir === 'up') index = Math.min(index, nowIndex);     // 往上捲：不跳到後面的類別
  }
  setCurrentCat(catLinks[index]);
}

let catJumping = false;   // 點類別跳過去的途中：先不要自動判斷
let catTimer = 0;
function waitScrollEnd() {   // 頁面停止捲動 150 毫秒後，算「滑動停下來了」
  clearTimeout(catTimer);
  catTimer = setTimeout(() => {
    catJumping = false;
    updateCurrentCat();
  }, 150);
}

if (catLinks.length > 0) {
  let ticking = false;
  let lastY = window.scrollY;   // 上一次算的時候捲到哪裡：跟現在比，就知道是往上還是往下
  window.addEventListener('scroll', () => {
    if (catJumping) {
      lastY = window.scrollY;
      waitScrollEnd();
      return;
    }
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(() => {   // 一個畫面只算一次，捲動時才不會卡
      ticking = false;
      const y = window.scrollY;
      if (y === lastY) return;
      updateCurrentCat(y > lastY ? 'down' : 'up');
      lastY = y;
    });
  }, { passive: true });

  catNav.addEventListener('click', e => {
    const link = e.target.closest('.order-cat');
    if (!link) return;
    catJumping = true;
    setCurrentCat(link);
    waitScrollEnd();   // 已經在那個位置、頁面不用滑的時候，也會在 150 毫秒後恢復
  });

  window.addEventListener('resize', updateCurrentCat);
  updateCurrentCat();
}

})();
