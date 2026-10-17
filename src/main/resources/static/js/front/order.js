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
  ['dbCartSheet', 'dbOrderedSheet'].forEach(id => {
    const oldSheet = document.getElementById(id);
    const newSheet = doc.getElementById(id);
    if (!oldSheet || !newSheet) return;
    newSheet.hidden = oldSheet.hidden;
    newSheet.dataset.returnTo = oldSheet.dataset.returnTo || '';
    const oldBody = oldSheet.querySelector('.order-sheet-body');
    const newBody = newSheet.querySelector('.order-sheet-body');
    oldSheet.replaceWith(newSheet);
    if (oldBody && newBody) newBody.scrollTop = oldBody.scrollTop;
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

  cartBusy = true;
  try {
    const res = await fetch(action, { method: 'POST', body });
    const result = swapCartArea(await res.text());
    if (!result.ok) {
      location.reload();
      return;
    }
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

})();
