(() => {
	
	const createForm = document.querySelector('[data-rsv-create]');
	const createPostForm = document.querySelector('[data-rsv-create-post]');

	if (createForm && createPostForm) {
	  let createSubmitting = false;

	  const dateInput = createForm.querySelector('[name="date"]');
	  const tableInput = createForm.querySelector('[name="tableType"]');
	  const slotInput = createForm.querySelector('[name="rsvDtNo"]');

	  const loadedDate =
	    createPostForm.querySelector('[name="date"]').value;

	  const loadedTableType =
	    createPostForm.querySelector('[name="tableType"]').value;

	  function invalidateSlots() {
	    slotInput.value = '';
	    slotInput.disabled = true;
	  }

	  dateInput.addEventListener('change', invalidateSlots);
	  tableInput.addEventListener('change', invalidateSlots);

	  createPostForm.addEventListener('submit', event => {
	    if (createSubmitting) {
	      event.preventDefault();
	      return;
	    }

	    const phone =
	      createForm.querySelector('[name="phone"]').value.trim();

	    const note =
	      createForm.querySelector('[name="note"]').value;

	    if (!/^09[0-9]{8}$/.test(phone)) {
	      event.preventDefault();
	      alert('請輸入09開頭的10碼手機號碼');
	      return;
	    }

	    if (dateInput.value !== loadedDate
	        || tableInput.value !== loadedTableType) {
	      event.preventDefault();
	      alert('日期或桌型已變更，請先按「查詢可訂時段」');
	      return;
	    }

	    if (!slotInput.value || slotInput.disabled) {
	      event.preventDefault();
	      alert('請先查詢並選擇可訂時段');
	      return;
	    }

	    createPostForm.querySelector('[name="phone"]').value = phone;
	    createPostForm.querySelector('[name="note"]').value = note;
	    createPostForm.querySelector('[name="rsvDtNo"]').value =
	      slotInput.value;

	    createSubmitting = true;
	  });

	  window.addEventListener('pageshow', () => {
	    createSubmitting = false;
	  });
	}
	
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