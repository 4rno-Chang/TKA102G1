(() => {
    const byId = id => document.getElementById(id);

    function setText(id, value) {
        const element = byId(id);
        if (element) element.textContent = value || '未填寫';
    }

    // 取號表單摘要。
    document.querySelectorAll('[data-summary]').forEach(input => {
        const update = () => {
            setText(input.dataset.summary, input.value.trim());
        };

        input.addEventListener('input', update);
        update();
    });

    const joinForm = byId('joinForm');

    if (joinForm) {
        const updateTable = () => {
            const selected = joinForm.querySelector(
                'input[name="tableType"]:checked'
            );

            if (!selected) return;

            setText('sumTable', `${selected.value} 人桌`);
            setText('sumWaiting', `${selected.dataset.waiting} 組`);
            setText('sumEstimate', selected.dataset.estimate);
        };

        joinForm.addEventListener('change', updateTable);
        updateTable();

        let submitting = false;

        joinForm.addEventListener('submit', event => {
            const phone =
                joinForm.elements.namedItem('phone').value.trim();

            const name =
                joinForm.elements.namedItem('name').value.trim();

            const selected = joinForm.querySelector(
                'input[name="tableType"]:checked'
            );

            let message = '';

            if (!selected) {
                message = '請選擇桌型';
            } else if (!/^09[0-9]{8}$/.test(phone)) {
                message = '請輸入09開頭的10碼手機號碼';
            } else if (Array.from(name).length > 30) {
                message = '姓名最多30個字元';
            }

            const error = byId('contactError');

            if (error) {
                error.textContent = message;
                error.hidden = message === '';
            }

            if (message || submitting) {
                event.preventDefault();
                return;
            }

            submitting = true;
            joinForm.setAttribute('aria-busy', 'true');
        });

        window.addEventListener('pageshow', () => {
            submitting = false;
            joinForm.removeAttribute('aria-busy');
        });
    }

    // 一般表單送出，沒有 API 呼叫。
    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', event => {
            if (!confirm(form.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    // 狀況页及有效號碼牌每10秒重新載入。
    // 正在操作表單時暫停，避免清掉輸入內容。
    if (document.querySelector('[data-auto-refresh="true"]')) {
        let dirty = false;

        document.addEventListener('input', () => {
            dirty = true;
        });

        document.addEventListener('change', () => {
            dirty = true;
        });

        setInterval(() => {
            const active = document.activeElement;

            const editing = active
                && active.matches('input, textarea, select');

            if (!dirty && !editing && !document.hidden) {
                window.location.reload();
            }
        }, 10000);
    }
})();