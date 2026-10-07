/* 公告新增／修改頁（insertAnnPage.html、updateAnnPage.html）
   公告開始時間不能選過往日期；修改頁如果沒動到原本的時間（data-original）就放行。
   後端 AnnouncementController 也會再檢查一次。 */
document.addEventListener('DOMContentLoaded', () => {
  const input = document.getElementById('annBegin');
  if (!input) return;
  const form = input.form;
  const original = input.dataset.original || '';
  const MSG = '請勿設定過往日期';

  // 現在時間轉成 datetime-local 的格式：yyyy-MM-ddTHH:mm（本地時間）
  const nowValue = () => {
    const d = new Date();
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
  };

  // 紅字：後端有輸出就沿用（th:if），沒有就自己建一個
  const errorEl = () => {
    let el = document.getElementById('annBeginError');
    if (!el) {
      el = document.createElement('p');
      el.id = 'annBeginError';
      el.className = 'ann-field-error';
      el.setAttribute('role', 'alert');
      input.insertAdjacentElement('afterend', el);
    }
    return el;
  };

  const isPast = () => input.value !== '' && input.value !== original && input.value < nowValue();

  const check = () => {
    if (isPast()) {
      const el = errorEl();
      el.textContent = MSG;
      el.hidden = false;
      return false;
    }
    const el = document.getElementById('annBeginError');
    if (el) el.hidden = true;
    return true;
  };

  // 日期選擇器最早只能選到現在（修改頁原本就是過去時間的話，不設 min，避免原值被擋）
  if (!original || original >= nowValue()) input.min = nowValue();

  input.addEventListener('change', check);
  form.addEventListener('submit', (e) => {
    if (!check()) {
      e.preventDefault();
      input.focus();
    }
  });
});
