(() => {
  const form = document.querySelector('[data-rsv-manage]');

  if (!form) return;

  let submitting = false;

  const confirmMessages = {
    arrived: '確定將這筆訂位標記為實到？',
    'no-show': '確定將這筆訂位標記為未到？',
    cancel: '確定取消這筆訂位？'
  };

  form.addEventListener('submit', event => {
    if (submitting) {
      event.preventDefault();
      return;
    }

    const button = event.submitter;

    // 明確要求操作按鈕，避免沒有 operation 的送出。
    if (!button || button.name !== 'operation') {
      event.preventDefault();
      return;
    }

    const message = confirmMessages[button.value];

    if (message && !confirm(message)) {
      event.preventDefault();
      return;
    }

    submitting = true;
    form.setAttribute('aria-busy', 'true');

    // 保留 operation 按鈕的值，不能在送出前將它 disabled。
    // 以 submitting 旗標阻止重複送出。
  });

  window.addEventListener('pageshow', () => {
    submitting = false;
    form.removeAttribute('aria-busy');
  });
})();