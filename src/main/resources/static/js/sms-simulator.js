(() => {
    'use strict';

    if (window.__bistroopsSmsSimulatorLoaded) return;

    const script = document.querySelector(
        'script[data-sms-simulator]'
    );

    if (!script) return;

    window.__bistroopsSmsSimulatorLoaded = true;

    const endpoint = script.dataset.inboxUrl;
    const cssUrl = script.dataset.cssUrl;
    const staff = script.dataset.staff === 'true';

    const storageKey = `bistroops-sms-preview:${endpoint}`;

    let remembered = { streamId: '', seen: [] };

    try {
        remembered = JSON.parse(
            sessionStorage.getItem(storageKey)
        ) || remembered;
    } catch (_) {
        // 瀏覽器停用儲存時，仍可在目前頁面顯示訊息。
    }

    let streamId = remembered.streamId || '';
    const seen = new Set(
        Array.isArray(remembered.seen) ? remembered.seen : []
    );

    let messages = [];
    let navigationPending = false;
    let stopped = false;

    const host = document.createElement('div');
    document.body.append(host);

    const root = host.attachShadow({ mode: 'open' });

    root.innerHTML = `
        <link rel="stylesheet">

        <button type="button"
                class="sms-launcher"
                aria-label="開啟模擬手機"
                hidden>
            模擬手機
        </button>

        <section class="sms-phone"
                 role="dialog"
                 aria-modal="false"
                 aria-label="模擬手機簡訊"
                 hidden>

            <div class="sms-status">
                <span>SMS DEMO</span>
                <span>模擬手機</span>
            </div>

            <header class="sms-header">
                <div class="sms-avatar">B</div>

                <div class="sms-heading">
                    <strong>Bistroops</strong>
                    <small>簡訊通知模擬</small>
                </div>

                <button type="button"
                        class="sms-close"
                        aria-label="收合模擬手機">×</button>
            </header>

            <label class="sms-recipient">
                收件電話
                <select aria-label="選擇收件電話"></select>
            </label>

            <div class="sms-chat"
                 role="log"
                 aria-live="polite"></div>

            <footer class="sms-footer">
                測試訊息，未實際發送至手機或 LINE
            </footer>

            <div class="sms-home-indicator"></div>
        </section>`;

    root.querySelector('link').href = cssUrl;

    const launcher = root.querySelector('.sms-launcher');
    const phone = root.querySelector('.sms-phone');
    const closeButton = root.querySelector('.sms-close');
    const recipient = root.querySelector('select');
    const chat = root.querySelector('.sms-chat');

    function openPhone() {
        phone.hidden = false;
        launcher.hidden = true;
    }

    function closePhone() {
        phone.hidden = true;
        launcher.hidden = messages.length === 0;
    }

    launcher.addEventListener('click', openPhone);
    closeButton.addEventListener('click', closePhone);

    function renderChat() {
        chat.replaceChildren();

        for (const item of messages) {
            if (item.phone !== recipient.value) continue;

            const block = document.createElement('article');
            block.className = 'sms-message';

            const title = document.createElement('div');
            title.className = 'sms-message-title';
            title.textContent = 'Bistroops';

            const bubble = document.createElement('div');
            bubble.className = 'sms-bubble';
            bubble.textContent = item.message;

            const time = document.createElement('div');
            time.className = 'sms-time';

            time.textContent = new Date(item.createdAt)
                .toLocaleString('zh-TW', {
                    timeZone: 'Asia/Taipei',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: false
                });

            block.append(title, bubble, time);
            chat.append(block);
        }

        chat.scrollTop = chat.scrollHeight;
    }

    recipient.addEventListener('change', renderChat);

    function updateMessages(data) {
        if (streamId !== data.streamId) {
            streamId = data.streamId;
            seen.clear();
        }

        messages = Array.isArray(data.messages)
            ? data.messages
            : [];

        const incoming = messages.filter(
            item => !seen.has(String(item.id))
        );

        for (const item of messages) {
            seen.add(String(item.id));
        }

        while (seen.size > 250) {
            seen.delete(seen.values().next().value);
        }

        try {
            sessionStorage.setItem(
                storageKey,
                JSON.stringify({
                    streamId,
                    seen: [...seen]
                })
            );
        } catch (_) {
            // 儲存失敗不影響訊息顯示。
        }

        const selectedPhone = incoming.length
            ? incoming[incoming.length - 1].phone
            : recipient.value;

        recipient.replaceChildren();

        for (const number of new Set(
            messages.map(item => item.phone)
        )) {
            recipient.add(new Option(number, number));
        }

        if ([...recipient.options].some(
            option => option.value === selectedPhone
        )) {
            recipient.value = selectedPhone;
        }

        renderChat();

        if (messages.length === 0) {
            phone.hidden = true;
            launcher.hidden = true;
            return;
        }

        if (phone.hidden) launcher.hidden = false;
        if (incoming.length) openPhone();
    }

    // 一般表單送出後等待重新導向，
    // 避免舊頁面先讀走訊息、下一頁卻不再自動展開。
    document.addEventListener('submit', event => {
        const form = event.target;

        if (form instanceof HTMLFormElement
            && form.method !== 'dialog'
            && !event.defaultPrevented) {

            navigationPending = true;
        }
    });

    window.addEventListener('pageshow', () => {
        navigationPending = false;
    });

    async function poll() {
        if (stopped) return;

        try {
            if (!document.hidden && !navigationPending) {
                const url = new URL(endpoint, window.location.href);
                url.searchParams.set('staff', String(staff));

                const response = await fetch(url, {
                    credentials: 'same-origin',
                    cache: 'no-store',
                    headers: { Accept: 'application/json' }
                });

                // 非模擬模式沒有此端點，停止顯示元件。
                if (response.status === 404) {
                    stopped = true;
                    host.remove();
                    return;
                }

                if (response.ok) {
                    const data = await response.json();

                    if (!navigationPending) {
                        updateMessages(data);
                    }
                }
            }
        } catch (_) {
            // 暫時斷線不干擾訂候位與會員頁面。
        } finally {
            if (!stopped) {
                window.setTimeout(poll, 3000);
            }
        }
    }

    poll();
})();