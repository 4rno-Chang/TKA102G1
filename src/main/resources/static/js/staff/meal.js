/* =========================================================
   Bistroops 員工後台：菜單管理（staff/meal/meal_list.html）
   要先載入 staff.js（escapeHtml、staffToast、送出前的 data-confirm 都在那裡）。

   內容：A-1. 小工具　A-2. 圖片載入失敗換插圖　A-3. 篩選　A-4. 統計　A-5. 菜品詳細
         A-6. 選圖片　A-7. 新增／修改的對話框　A-8. 類別（新增類別）　A-9. 活動價
         A-10. 數字欄位不用上下鍵和滾輪改數字　A-11. 編輯類別的對話框　A-12. 打開頁面時
   ========================================================= */

(() => {


/* ---------- A-1. 小工具 ---------- */
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
const money = n => 'NT$ ' + Number(n).toLocaleString('en-US');

// 客人點餐時的常用備註，跟前台 order.js A-5 的 NOTE_TAGS 一樣（那邊改了這裡也要改）
const NOTE_TAGS = {
  '前菜': ['人到齊再上', '需分食盤', '醬汁分開'],
  '主餐': ['不要蒜', '不要洋蔥', '不要辣'],
  '甜點': ['跟主餐一起上', '餐後上'],
  '飲料': ['去冰', '溫的', '餐前上', '餐後上']
};
const tagLabel = tag => (tag === '前菜' ? '開胃菜' : tag || '');   // 資料庫存「前菜」，畫面寫「開胃菜」
// 選了種類之後，在 box 裡顯示「客人點「主餐」類的菜時，常用備註會出現：…」；沒選就藏起來
function showTagPreview(box, tag) {
  box.hidden = !NOTE_TAGS[tag];
  if (NOTE_TAGS[tag]) {
    box.innerHTML = `客人點「${escapeHtml(tagLabel(tag))}」類的菜時，常用備註會出現：`
      + NOTE_TAGS[tag].map(t => `<i>${escapeHtml(t)}</i>`).join('');
  }
}
const MAX_IMG = 5 * 1024 * 1024;   // 跟 application.properties 的 max-file-size=5MB 一樣
const wide = window.matchMedia('(min-width: 1201px)');   // 寬螢幕：詳細固定在右邊；其他：從右邊滑出

// 類別的資料寫在每一組 .meal-group 的 data-* 上（類別名稱、標籤代號、插圖）
const typeInfo = typeNo => ($(`.meal-group[data-group-type="${typeNo}"]`) || {}).dataset || {};


/* ---------- A-2. 圖片載入失敗時，換成插圖（例如圖片被刪掉了） ---------- */
function watchImages(root = document) {
  $$('img[data-fallback]', root).forEach(img => {
    const useFallback = () => {
      img.src = img.dataset.fallback;
      img.removeAttribute('data-fallback');
      img.parentElement.classList.add('is-art');
    };
    if (img.complete && img.naturalWidth === 0 && img.currentSrc) useFallback();   // 已經載入失敗
    else img.addEventListener('error', useFallback, { once: true });
  });
}


/* ---------- A-3. 篩選：類別＋關鍵字＋四個格子（菜品都已經在頁面上，這裡只是藏起來不符合的） ----------
   三個條件可以一起用，例如「主餐」＋「牛」＋「上架中」＝主餐裡、名稱或說明有「牛」、上架中的菜。
   每次打開這一頁都從頭開始：全部類別、沒有關鍵字、「全部菜品」（不記住上一次的篩選） */
const filterType = $('#mealFilterType');
const filterQ = $('#mealFilterQ');
// 四個格子：全部菜品／上架中／未上架／參加活動，每一格怎麼判斷
const STATS = {
  all:   { label: '全部菜品', ok: () => true },
  on:    { label: '上架中',   ok: r => r.dataset.status === '上架' },
  off:   { label: '未上架',   ok: r => r.dataset.status !== '上架' },
  promo: { label: '參加活動', ok: r => !!r.dataset.promos }
};
let filterStat = 'all';   // 目前按的是哪一格

function applyFilter() {
  const type = filterType.value;
  const q = filterQ.value.trim().toLowerCase();
  const stat = STATS[filterStat];
  let shown = 0, total = 0;
  const counts = { all: 0, on: 0, off: 0, promo: 0 };   // 格子上的數字：符合類別＋關鍵字的有幾道

  $$('.meal-group').forEach(group => {
    const typeOk = !type || group.dataset.groupType === type;
    let groupShown = 0;
    $$('.meal-row', group).forEach(row => {
      total++;
      const text = (row.dataset.name + ' ' + (row.dataset.exp || '')).toLowerCase();
      const queryOk = typeOk && (!q || text.includes(q));
      if (queryOk) Object.keys(counts).forEach(k => { if (STATS[k].ok(row)) counts[k]++; });
      const ok = queryOk && stat.ok(row);
      row.parentElement.hidden = !ok;
      if (ok) groupShown++;
    });
    // 有打關鍵字或按了格子時，沒有符合的類別整組藏起來；只選類別時，空的類別也顯示（才看得到「還沒有菜品」）
    group.hidden = !typeOk || ((q !== '' || filterStat !== 'all') && groupShown === 0);
    shown += groupShown;
  });

  renderStats(counts);
  $('#mealFilterCount').textContent = (type || q || filterStat !== 'all') ? `顯示 ${shown} / ${total} 道` : `共 ${total} 道`;
  $('#mealNoResult').hidden = shown > 0 || (!q && filterStat === 'all');
}

// 回到一開始：全部類別、沒有關鍵字、全部菜品
//   （瀏覽器按「上一頁」回來時，可能會自己把上次打的字填回去，所以也要清掉）
function resetFilter() {
  filterType.value = '';
  filterQ.value = '';
  filterStat = 'all';
}

filterType.addEventListener('change', applyFilter);
filterQ.addEventListener('input', applyFilter);


/* ---------- A-4. 四個格子（還沒點任何菜時，在詳細的位置顯示；平板以下顯示在列表上面） ----------
   按一格就只顯示那一種菜，按中的那格會變色；格子上的數字會跟著類別、關鍵字變 */
function renderStats(counts) {
  const html = Object.entries(STATS).map(([k, s]) =>
    `<button type="button" class="meal-stat${k === filterStat ? ' is-active' : ''}" data-stat="${k}" aria-pressed="${k === filterStat}">
      <span>${s.label}</span><b>${counts[k]}</b></button>`).join('');
  $$('[data-stats]').forEach(box => { box.innerHTML = html; });
}

document.addEventListener('click', e => {
  const btn = e.target.closest('[data-stat]');
  if (!btn) return;
  filterStat = btn.dataset.stat;
  applyFilter();
});


/* ---------- A-5. 菜品詳細 ----------
   點列表的一道菜 → 用那一列的 data-* 填右邊的詳細；按「修改」打開對話框 */
const panel = $('#mealPanel');
const backdrop = $('#mealPanelBackdrop');
const views = { empty: $('[data-panel="empty"]', panel), view: $('[data-panel="view"]', panel) };
let current = null;   // 目前打開的那一列

function showPart(name) {
  Object.entries(views).forEach(([k, el]) => { el.hidden = k !== name; });
  panel.scrollTop = 0;
}

// 活動的名稱和狀態：從對話框裡的活動列表找（「活動編號」→ 名稱、進行中／未開始）
function promoInfo(no) {
  const row = $$('[data-promo-row]').find(r => $('input[name$=".promoteNo"]', r).value === no);
  return row ? { name: $('.meal-promo-name', row).textContent, state: $('.meal-tag', row).textContent } : { name: '活動 #' + no, state: '' };
}

function fillView(row) {
  const d = row.dataset;
  const t = typeInfo(d.type);
  const hasImg = d.hasImg === 'true';
  const on = d.status === '上架';
  const set = (key, text) => { $(`[data-view="${key}"]`, panel).textContent = text; };

  // 圖片：有圖片用列表那張的網址，沒有用類別的插圖；是代表圖的話左上角有標籤
  const photo = $('[data-view="photo"]', panel);
  const rowImg = $('.meal-row-thumb img', row);
  const img = $('img', photo);
  photo.classList.toggle('is-art', !hasImg || rowImg.parentElement.classList.contains('is-art'));
  img.src = hasImg ? rowImg.getAttribute('src') : t.art;
  img.alt = hasImg ? d.name : '';
  img.onerror = () => { img.onerror = null; img.src = t.art; photo.classList.add('is-art'); };
  $('[data-view="pill"]', panel).hidden = !d.cover;

  const label = tagLabel(t.tag);
  set('type', label && label !== t.typeName ? `${t.typeName}・${label}` : (t.typeName || ''));
  set('name', d.name);
  set('price', money(d.price));
  set('status', d.status);
  $('[data-view="status"]', panel).classList.toggle('is-off', !on);
  set('exp', d.exp || '');
  set('no', d.mealNo);
  set('promoOnly', d.promoOnly === 'true' ? '是' : '否');

  // 活動價：一個活動一行
  const promos = (d.promos || '').split(',').filter(Boolean).map(s => s.split(':'));
  $('[data-view="promos"]', panel).innerHTML = promos.length === 0 ? '沒有參加活動' : promos.map(([no, price]) => {
    const p = promoInfo(no);
    const text = !price ? '原價' : price === '0' ? `${money(0)}（贈送）` : money(price);   // 活動價空的＝用原價
    return `<span>${escapeHtml(p.name)} <small>${escapeHtml(p.state)}</small> ${text}</span>`;
  }).join('');

  // 代表圖的小字按鈕：有圖片、上架、還不是「設定的」代表圖才顯示「設為類別代表圖」
  const setForm = $('[data-cover-set]', panel);
  setForm.mealtypeNo.value = d.type;
  setForm.mealNo.value = d.mealNo;
  setForm.hidden = !(hasImg && on && d.cover !== 'custom');
}

function openRow(row, { scroll = false } = {}) {
  $$('.meal-row.active').forEach(r => r.classList.remove('active'));
  row.classList.add('active');
  current = row;
  fillView(row);
  showPart('view');
  if (!wide.matches) { panel.classList.add('open'); backdrop.hidden = false; document.body.style.overflow = 'hidden'; }
  if (scroll) row.scrollIntoView({ block: 'center' });
}

function closePanel() {
  panel.classList.remove('open');
  backdrop.hidden = true;
  document.body.style.overflow = '';
  $$('.meal-row.active').forEach(r => r.classList.remove('active'));
  current = null;
  showPart('empty');
}

document.addEventListener('click', e => {
  const row = e.target.closest('.meal-row');
  if (row) openRow(row);
  if (e.target.closest('[data-panel-close]')) closePanel();
  if (e.target.closest('[data-edit-open]') && current) openDialog({ row: current });
  // 點到菜品以外的地方（詳細的面板、對話框裡面除外）：關掉詳細，回到四個格子
  else if (current && !row && !e.target.closest('#mealPanel, #mealDialog, dialog')) closePanel();
});
backdrop.addEventListener('click', closePanel);
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && !wide.matches && panel.classList.contains('open') && dialog.hidden) closePanel();
});
// 平板轉成寬螢幕（或反過來）時，滑出用的設定跟著換
wide.addEventListener('change', () => {
  if (wide.matches) { panel.classList.remove('open'); backdrop.hidden = true; document.body.style.overflow = ''; }
  else if (current) { panel.classList.add('open'); backdrop.hidden = false; }
});


/* ---------- A-6. 選圖片：顯示預覽；超過 5MB 不讓送出 ---------- */
const drop = $('[data-drop]');
const dropInput = $('input[type="file"]', drop);
const dropText = $('[data-drop-text]', drop);
const dropPreview = $('[data-drop-preview]', drop);
const DROP_TEXT = dropText.textContent;

// current：修改時原本的圖片網址（沒有就是 null）
function resetDrop(currentImg = null) {
  if (dropPreview.src.startsWith('blob:')) URL.revokeObjectURL(dropPreview.src);
  dropInput.value = '';
  dropPreview.hidden = !currentImg;
  if (currentImg) dropPreview.src = currentImg; else dropPreview.removeAttribute('src');
  dropText.textContent = currentImg ? '目前的圖片，點一下可以換新的（5MB 以內）' : DROP_TEXT;
}

dropInput.addEventListener('change', () => {
  const file = dropInput.files[0];
  if (!file) return;
  if (file.size > MAX_IMG) {
    dropInput.value = '';
    staffToast('圖片超過 5MB，請換一張比較小的');
    return;
  }
  if (dropPreview.src.startsWith('blob:')) URL.revokeObjectURL(dropPreview.src);
  dropPreview.src = URL.createObjectURL(file);
  dropPreview.hidden = false;
  dropText.textContent = file.name + '（再點一次可以換）';
  $('input', $('#formRemoveImg')).checked = false;   // 選了新圖片，就不需要「刪除目前的圖片」
});
['dragenter', 'dragover'].forEach(t => dropInput.addEventListener(t, () => drop.classList.add('is-over')));
['dragleave', 'drop'].forEach(t => dropInput.addEventListener(t, () => drop.classList.remove('is-over')));


/* ---------- A-7. 新增／修改的對話框 ----------
   只能按「取消」、「X」或送出離開：按旁邊會晃一下、按 Esc 沒有反應。
   打開時後面的頁面設成 inert（點不到、Tab 也不會跑過去） */
const dialog = $('#mealDialog');
const form = $('#mealForm');
const behind = [$('main'), $('.staff-side')].filter(Boolean);

// row：要修改的那一列（新增時不傳）；keep：保留表單上的內容（Controller 回傳錯誤時）
function openDialog({ row = null, keep = false } = {}) {
  if (!keep) form.reset();
  const editing = keep ? form.mealNo.value !== '' : row !== null;
  if (keep && editing) row = $(`.meal-row[data-meal-no="${CSS.escape(form.mealNo.value)}"]`);

  // 標題、按鈕文字（新增和修改都送到同一個網址，Java 看 mealNo 有沒有值來分）
  $$('[data-mode-text]', form).forEach(el => { el.textContent = el.dataset.modeText.split('|')[editing ? 1 : 0]; });
  if (editing && row) $('#mealDialogTitle').textContent += ` #${row.dataset.mealNo}`;

  if (row && !keep) {
    const d = row.dataset;
    form.mealNo.value = d.mealNo;
    form.mealtypeNo.value = d.type;
    form.mealName.value = d.name;
    form.mealPrice.value = d.price;
    form.mealExp.value = d.exp || '';
    $$('input[name="mealStatus"]', form).forEach(r => { r.checked = r.value === d.status; });
    $('#formPromoOnly').checked = d.promoOnly === 'true';
  }
  if (!editing) form.mealNo.value = '';

  // 刪除菜品：只有修改時出現，先把要刪的菜品編號和確認的話填好
  const delForm = $('#mealDeleteForm');
  $('#formMealDelete').hidden = !editing;
  delForm.mealNo.value = editing ? form.mealNo.value : '';
  delForm.dataset.confirm = `確定要刪除「${row ? row.dataset.name : form.mealName.value}」嗎？\n刪除後前台和後台都不會再顯示，\n以前的訂單不受影響。`;

  const hasImg = editing && row && row.dataset.hasImg === 'true';
  resetDrop(hasImg ? $('.meal-row-thumb img', row).getAttribute('src') : null);
  $('#formRemoveImg').hidden = !hasImg;

  fillPromos(keep ? form.dataset.oldPromos : (row && editing ? row.dataset.promos : ''));
  promoOnlyNote.hidden = true;   // 「已經改成未上架」的說明，只在這次勾選時才出現
  syncType();
  $$('button[type="submit"]', form).forEach(b => { b.disabled = false; });

  dialog.hidden = false;
  behind.forEach(el => { el.inert = true; });
  document.body.style.overflow = 'hidden';
  $('.meal-modal-body', dialog).scrollTop = 0;
  form.mealName.focus();
}

function closeDialog() {
  dialog.hidden = true;
  behind.forEach(el => { el.inert = false; });
  document.body.style.overflow = panel.classList.contains('open') ? 'hidden' : '';   // 後面還有滑出來的詳細就繼續鎖住捲動
}

function shake() {
  const box = $('.meal-modal-box', dialog);
  box.classList.remove('is-shake');
  void box.offsetWidth;   // 讓動畫可以重播
  box.classList.add('is-shake');
}

document.addEventListener('click', e => {
  if (e.target.closest('[data-add-open]')) openDialog();
  if (e.target.closest('[data-dialog-close]')) closeDialog();
});
dialog.addEventListener('click', e => { if (e.target === dialog) shake(); });   // 按到旁邊的暗色區域
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && !dialog.hidden) { e.preventDefault(); shake(); }
});

// 送出後把按鈕停用，避免按兩次新增兩道一樣的菜（再打開時恢復）
form.addEventListener('submit', () => {
  // 價格整理成一般的整數再送：數字欄位允許 1e3（＝1000）、300.0 這種寫法，但 Java 的 Integer 讀不懂，會出現英文的錯誤訊息
  if (form.mealPrice.value !== '') form.mealPrice.value = String(Number(form.mealPrice.value));
  $$('button[type="submit"]', form).forEach(b => { b.disabled = true; });
});
window.addEventListener('pageshow', () => $$('button[type="submit"]', form).forEach(b => { b.disabled = false; }));

// 活動價有錯（例如打完折跟原價一樣）還按了儲存：除了欄位下面的紅字，再跳出對話框說是哪個活動，按確認後跳到那一格
//   送出時瀏覽器會檢查每個欄位，有錯的欄位會收到 invalid 事件（true：在事件往下傳的時候就先接到）
form.addEventListener('invalid', e => {
  if (!e.target.matches('[data-promo-input]')) return;   // 其他欄位照舊用瀏覽器的小提示
  e.preventDefault();                                     // 活動價不用瀏覽器的小提示，改用對話框
  const first = $$('input, select, textarea', form).find(el => !el.validity.valid);
  if (first !== e.target) return;                         // 前面還有別的欄位沒填好：先讓瀏覽器提示那一格
  const name = $('.meal-promo-name', e.target.closest('[data-promo-row]')).textContent;
  staffAlert(`「${name}」的活動價：${e.target.validationMessage}`).then(() => e.target.focus());
}, true);


/* ---------- A-8. 類別 ----------
   選「＋ 新增類別」：顯示名稱和種類（設成必填）。這裡只能新增；改名、刪除在右上角的「編輯類別」
   下面顯示這個類別的常用備註 */
const typeSelect = $('#formType');

function syncType() {
  const option = typeSelect.selectedOptions[0];
  const isNew = typeSelect.value === '0';

  $('#formNewType').hidden = !isNew;
  form.newTypeName.required = isNew;
  $$('input[name="newTypeTag"]', form).forEach(r => { r.required = isNew; });

  const tag = isNew ? ($('input[name="newTypeTag"]:checked', form) || {}).value : option && option.dataset.tag;
  showTagPreview($('#formTagPreview'), tag);
}

typeSelect.addEventListener('change', syncType);
form.addEventListener('change', e => { if (e.target.name === 'newTypeTag') syncType(); });


/* ---------- A-9. 活動價 ----------
   每個活動一列：勾了才展開設定。「打折」或「直接打金額」：
     打折：照台灣說法打 1～99 的整數，一位數是「幾折」（9 → 9折），兩位數是「幾幾折」（85 → 85折）→ 原價 × 折數，四捨五入
     金額：直接打活動價
   算好的金額放進隱藏的 promos[i].price 送給 Java（Java 收到的一定是金額） */
const priceInput = $('#formPrice');
// 折數換成「原價的百分之幾」：9 → 90、85 → 85
const percentOf = v => (v < 10 ? v * 10 : v);
// 算活動價：全部用整數算，加 50 再除以 100 去掉小數＝四捨五入，不會有小數誤差
//   （直接乘小數會出錯：45 × 0.7 電腦算出來是 31.4999…，四捨五入變成 31，應該是 32）
const promoPrice = (base, v) => Math.floor((base * percentOf(v) + 50) / 100);
// 百分之幾換回台灣的說法：90 → "9"、85 → "85"
const zheText = p => String(p % 10 === 0 ? p / 10 : p);

// 有勾活動、卻沒填折數或金額時，按儲存跳出的提醒
const EMPTY_MSG = '請輸入活動價格，至少要大於 0';

// 折數、金額只能打數字：打字或貼上時，數字以外的（空白、小數點、負號、文字）都拿掉，開頭多的 0 也拿掉（007 → 7）
function onlyDigits(input) {
  const clean = input.value.replace(/\D/g, '').replace(/^0+(?=\d)/, '');
  if (clean !== input.value) input.value = clean;
}

function calcPromo(row) {
  const join = $('[data-promo-join]', row);
  const input = $('[data-promo-input]', row);
  const hidden = $('[data-promo-price]', row);
  const result = $('[data-promo-result]', row);
  const byRate = $('[data-promo-mode]:checked', row).value === 'rate';

  $('.meal-promo-set', row).hidden = !join.checked;
  row.classList.toggle('is-joined', join.checked);
  $('[data-promo-unit]', row).textContent = byRate ? '折' : '元';
  input.placeholder = byRate ? '例如 9、85' : '例如 199，贈送打 0';
  input.maxLength = byRate ? 2 : 5;   // 折數最多 2 位（1～99）、金額最多 5 位（跟原價的上限 99999 一樣）

  const base = Number(priceInput.value);
  const v = Number(input.value);
  let price = null, msg = '';
  if (byRate) {
    if (!(base >= 1)) msg = '先填上面的價格';
    else if (input.value === '') msg = EMPTY_MSG;
    else if (!Number.isInteger(v)) msg = '請打整數：85折打 85';
    else if (v < 1 || v > 99) msg = '請打 1～99：9折打 9、85折打 85';
    else price = promoPrice(base, v);
    // 打折算出 0 元時不直接當成贈送，避免不小心送出免費：要贈送請改用直接打金額
    if (price === 0) { msg = '算出來是 0 元；要贈送請改用「直接打金額」打 0'; price = null; }
  } else if (input.value === '') {
    msg = EMPTY_MSG;
  } else if (!Number.isInteger(v) || v < 0) {
    msg = '請輸入活動價（0 以上的整數，0 元＝贈送）';
  } else {
    price = v;
  }
  if (price !== null && base >= 1 && price > base) { msg = '活動價比原價還高'; price = null; }
  // 打的折太少（便宜的菜打 99折），四捨五入後跟原價一樣：等於沒打折，擋下來
  if (price !== null && price === base) { msg = `跟原價一樣，請改${byRate ? "折數" : "金額"}`; price = null; }

  hidden.value = price ?? '';
  input.setCustomValidity(join.checked ? msg : '');
  result.classList.toggle('is-error', join.checked && price === null && input.value !== '');
  // 顯示的「幾折」就是電腦怎麼看員工打的數字：打 9 顯示 9折、打 85 顯示 85折，打錯一眼就看得出來
  const approx = base >= 1 ? Math.round(price / base * 100) : 0;   // 直接打金額時，大約是原價的百分之幾
  // 欄位是空的：下面的灰字顯示怎麼打（按儲存時跳出的提醒才用 EMPTY_MSG）
  if (price === null && msg === EMPTY_MSG) result.textContent = byRate ? '打幾折？9折打 9、85折打 85' : '請輸入活動價（0 元＝贈送）';
  else if (price === null) result.textContent = msg;
  else if (price === 0) result.innerHTML = `活動價 <b>${money(0)}</b>（贈送）`;
  else result.innerHTML = `活動價 <b>${money(price)}</b>`
    + (byRate ? `（${zheText(percentOf(v))}折）` : approx >= 1 && approx <= 99 ? `（約 ${zheText(approx)}折）` : '');
}

// 換「打折／金額」時，把目前算好的金額換算過去，不用重打（換成打折時，換算不到 1～99折就留空，例如 0 元）
function switchMode(row) {
  const input = $('[data-promo-input]', row);
  const saved = $('[data-promo-price]', row).value;
  const price = Number(saved);
  const base = Number(priceInput.value);
  const byRate = $('[data-promo-mode]:checked', row).value === 'rate';
  const p = base >= 1 ? Math.round(price / base * 100) : 0;   // 原價的百分之幾：90 → 打 9、85 → 打 85
  if (saved !== '') input.value = byRate ? (p >= 1 && p <= 99 ? zheText(p) : '') : price;
  calcPromo(row);
}

// 修改時：依「活動編號:活動價,…」勾選活動並填入金額（活動價是空的＝用原價）
function fillPromos(text) {
  const saved = new Map((text || '').split(',').filter(Boolean).map(s => s.split(':')));
  $$('[data-promo-row]').forEach(row => {
    const no = $('input[name$=".promoteNo"]', row).value;
    $('[data-promo-join]', row).checked = saved.has(no);
    $('[data-promo-mode][value="' + (saved.has(no) ? 'amount' : 'rate') + '"]', row).checked = true;
    $('[data-promo-input]', row).value = saved.has(no) ? (saved.get(no) || priceInput.value) : '';
    calcPromo(row);
  });
  syncClash();
  syncPromoOnly();
}

// 活動限定菜品：至少要勾一個活動，不然不能送出（活動都結束後才知道什麼時候下架）
const promoOnlyBox = $('#formPromoOnly');
const promoOnlyNote = $('#formPromoOnlyNote');
function syncPromoOnly() {
  const anyJoined = $$('[data-promo-join]').some(c => c.checked);
  promoOnlyBox.setCustomValidity(promoOnlyBox.checked && !anyJoined ? '活動限定菜品至少要參加一個活動' : '');
  if (!promoOnlyBox.checked) promoOnlyNote.hidden = true;
}

// 勾「活動限定」或改了勾的活動時：勾的活動都還沒開始 → 狀態切成「未上架」（活動開始那天半夜會自動上架）；
// 有活動已經在進行中 → 切成「上架」。之後要先上架或先下架，直接改狀態就好（只有勾選的那一下會自動切）
function applyPromoOnlyStatus() {
  if (!promoOnlyBox.checked) return;
  const joined = $$('[data-promo-row]').filter(r => $('[data-promo-join]', r).checked);
  if (joined.length === 0) return;
  const anyStarted = joined.some(r => $('.meal-tag.is-on', r));   // 活動列上有金色「進行中」標籤
  $$('input[name="mealStatus"]', form).forEach(r => { r.checked = r.value === (anyStarted ? '上架' : '未上架'); });
  promoOnlyNote.hidden = anyStarted;
}

promoOnlyBox.addEventListener('change', () => { syncPromoOnly(); applyPromoOnlyStatus(); });

// 同一道菜「可以」參加時間重疊的活動：重疊的期間，前台用比較低的價格（PromoteService.getActivePrices 取低價）
//   勾了兩個時間重疊的活動，兩個活動下面都會出現提醒，讓員工知道會取低價（不擋，可以照樣儲存）
//   重疊：A 開始 ≤ B 結束，而且 B 開始 ≤ A 結束（只要有一點點時間一樣就算）
//   時間是 data-begin／data-end 的文字（yyyy-MM-dd HH:mm:ss），格式一樣，可以直接用文字比大小
const overlap = (a, b) => a.dataset.begin <= b.dataset.end && b.dataset.begin <= a.dataset.end;
const promoName = row => '「' + $('.meal-promo-name', row).textContent + '」';

function syncClash() {
  const rows = $$('[data-promo-row]');
  const joined = rows.filter(r => $('[data-promo-join]', r).checked);
  rows.forEach(row => {
    const hint = $('[data-promo-clash]', row);
    // 只提醒「有勾、而且跟另一個有勾的活動重疊」的；沒勾的不用提醒
    const clash = $('[data-promo-join]', row).checked ? joined.filter(other => other !== row && overlap(row, other)) : [];
    hint.textContent = clash.length ? `跟${clash.map(promoName).join('、')}時間重疊，重疊的期間前台會用比較低的價格` : '';
    hint.hidden = clash.length === 0;
  });
}

form.addEventListener('change', e => {
  const row = e.target.closest('[data-promo-row]');
  if (row && e.target.matches('[data-promo-mode]')) switchMode(row);
  else if (row) calcPromo(row);
  if (row && e.target.matches('[data-promo-join]')) { syncClash(); syncPromoOnly(); applyPromoOnlyStatus(); }
});
form.addEventListener('input', e => {
  const row = e.target.closest('[data-promo-row]');
  if (e.target.matches('[data-promo-input]')) onlyDigits(e.target);
  if (row && !e.target.matches('[data-promo-mode]')) calcPromo(row);   // 換模式交給上面的 change（要先換算才重算）
  if (e.target === priceInput) $$('[data-promo-row]').forEach(calcPromo);   // 原價改了，打折的活動價跟著重算
});


/* ---------- A-10. 數字欄位（價格、折數、金額）：鍵盤上下鍵、滑鼠滾輪不改數字 ----------
   滾輪照樣捲動對話框，只是不會讓數字跟著變 */
dialog.addEventListener('keydown', e => {
  if (e.target.matches('input[type="number"]') && (e.key === 'ArrowUp' || e.key === 'ArrowDown')) e.preventDefault();
});
dialog.addEventListener('wheel', e => {
  if (!e.target.matches('input[type="number"]') || document.activeElement !== e.target) return;
  e.preventDefault();
  $('.meal-modal-body', dialog).scrollTop += e.deltaY;
}, { passive: false });


/* ---------- A-11. 編輯類別的對話框 ----------
   左邊點一個類別（或「＋ 新增類別」），右邊就編輯它：名稱、種類，選種類時顯示常用備註。
   跟菜品的對話框一樣：只能按「關閉」或「X」離開（按旁邊會晃一下、按 Esc 沒有反應）。
   改了還沒儲存就換類別、或按關閉，會先問一次。修改時沒改任何東西，「儲存」不能按。 */
const typeDialog = $('#typeDialog');
const typeForm = $('#typeForm');
let typePicked = null;   // 左邊目前選的那一顆
const typeKind = () => ($('input[name="mealtypeHashtag"]:checked', typeForm) || {}).value || '';

// 有沒有改過（跟選的那一顆原本的名稱、種類比）
function typeDirty() {
  if (!typePicked) return false;
  const d = typePicked.dataset;
  return typeForm.mealtypeName.value.trim() !== (d.name || '') || typeKind() !== (d.tag || '');
}

function syncTypeForm() {
  showTagPreview($('#typeTagPreview'), typeKind());
  const isNew = !typePicked || !typePicked.dataset.no;
  $('#typeSaveBtn').disabled = !isNew && !typeDirty();   // 新增：一定能按（沒填會被 required 擋）
}

// 點了左邊的一顆：右邊填上它的資料
function pickType(btn) {
  typePicked = btn;
  $$('[data-type-pick]', typeDialog).forEach(b => b.classList.toggle('is-active', b === btn));
  const d = btn.dataset;
  const isNew = !d.no;
  $('#typeFormTitle').textContent = isNew ? '新增類別' : '修改類別';
  $('#typeSaveBtn').textContent = isNew ? '新增' : '儲存';
  typeForm.mealtypeNo.value = d.no || '';
  typeForm.mealtypeName.value = d.name || '';
  $$('input[name="mealtypeHashtag"]', typeForm).forEach(r => { r.checked = r.value === d.tag; });

  // 刪除：只有底下沒有菜的類別才出現
  const del = $('#typeDeleteForm');
  $('#typeDeleteBtn').hidden = isNew || d.deletable !== 'true';
  // 左下角的紅色小字：沒有菜 → 旁邊有刪除按鈕，寫「可刪除」；有菜 → 沒有按鈕，寫「無法刪除」；新增類別 → 不顯示
  const hint = $('#typeDeleteHint');
  hint.hidden = isNew;
  hint.textContent = d.deletable === 'true' ? '※此類別內無菜品，可刪除' : '※有餐點歸類於此類別，無法刪除';
  del.mealtypeNo.value = d.no || '';
  del.dataset.confirm = `確定要刪除「${d.name}」這個類別嗎？`;
  syncTypeForm();
}

function openTypes() {
  pickType($('[data-type-pick]', typeDialog));   // 先選第一個類別（一個都沒有就是「＋ 新增類別」）
  typeDialog.hidden = false;
  behind.forEach(el => { el.inert = true; });
  document.body.style.overflow = 'hidden';
  $('.meal-modal-body', typeDialog).scrollTop = 0;
  $('.meal-modal-x', typeDialog).focus();
}

async function closeTypes() {
  if (typeDirty() && !(await staffConfirm('類別改了還沒儲存，確定要關閉嗎？'))) return;
  typeDialog.hidden = true;
  behind.forEach(el => { el.inert = false; });
  document.body.style.overflow = panel.classList.contains('open') ? 'hidden' : '';
}

typeDialog.addEventListener('click', async e => {
  const btn = e.target.closest('[data-type-pick]');
  if (!btn || btn === typePicked) return;
  if (typeDirty() && !(await staffConfirm('這個類別改了還沒儲存，確定要換到別的類別嗎？'))) return;
  pickType(btn);
  if (!btn.dataset.no) typeForm.mealtypeName.focus();   // 新增：直接可以打名稱
});
typeForm.addEventListener('input', syncTypeForm);
typeForm.addEventListener('change', syncTypeForm);
// 類別名稱前後的空白先去掉再送（Java 也會再 trim 一次）
typeForm.addEventListener('submit', () => { typeForm.mealtypeName.value = typeForm.mealtypeName.value.trim(); }, true);

document.addEventListener('click', e => {
  if (e.target.closest('[data-types-open]')) openTypes();
  if (e.target.closest('[data-types-close]')) closeTypes();
});
typeDialog.addEventListener('click', e => {   // 按到旁邊的暗色區域：晃一下
  if (e.target !== typeDialog) return;
  const box = $('.meal-modal-box', typeDialog);
  box.classList.remove('is-shake');
  void box.offsetWidth;
  box.classList.add('is-shake');
});
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && !typeDialog.hidden) e.preventDefault();
});


/* ---------- A-12. 打開頁面時 ---------- */
function start() {
  watchImages();
  resetFilter();
  applyFilter();   // 裡面會順便畫四個格子
  showPart('empty');

  // 新增或修改失敗（Controller 放了 formError）：對話框直接打開，保留剛剛填的內容
  if (dialog.dataset.open === 'true') openDialog({ keep: true });

  // 編輯類別之後（Controller 放了 typeMessage 或 typeError）：類別的對話框直接打開，看得到剛剛的結果
  if (typeDialog.dataset.open === 'true' && dialog.hidden) openTypes();

  // 網址有 mealNo（例如剛新增、修改完）：打開那道菜的詳細（篩選每次都從頭開始，一定看得到那道菜）
  const no = new URLSearchParams(location.search).get('mealNo');
  const row = no && $(`.meal-row[data-meal-no="${CSS.escape(no)}"]`);
  if (row && dialog.hidden && typeDialog.hidden) openRow(row, { scroll: true });
}


start();

})();
