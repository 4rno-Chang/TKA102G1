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

		<div class="sms-launcher-group" hidden>
		    <button type="button"
		            class="sms-launcher"
		            aria-label="開啟模擬手機">
		        模擬手機
		    </button>

		    <button type="button"
		            class="sms-dismiss"
		            aria-label="關閉模擬手機通知"
		            title="關閉，收到新簡訊時再次顯示">
		        ×
		    </button>
		</div>

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

	const launcherGroup = root.querySelector('.sms-launcher-group');
	const launcher = root.querySelector('.sms-launcher');
	const dismissButton = root.querySelector('.sms-dismiss');
	const phone = root.querySelector('.sms-phone');
	const closeButton = root.querySelector('.sms-close');
	const recipient = root.querySelector('select');
	const chat = root.querySelector('.sms-chat');

	const dismissedKey = `${storageKey}:dismissed`;
	let dismissed = false;

	try {
	    dismissed = sessionStorage.getItem(dismissedKey) === 'true';
	} catch (_) {
	    // 無法使用儲存空間時，仍可在目前頁面關閉。
	}

	function setDismissed(value) {
	    dismissed = value;

	    try {
	        sessionStorage.setItem(dismissedKey, String(value));
	    } catch (_) {
	        // 不影響目前頁面的操作。
	    }
	}

	function openPhone() {
	    phone.hidden = false;
	    launcherGroup.hidden = true;
	}

	function closePhone() {
	    phone.hidden = true;
	    launcherGroup.hidden = dismissed || messages.length === 0;
	}

	launcher.addEventListener('click', openPhone);
	closeButton.addEventListener('click', closePhone);

	dismissButton.addEventListener('click', () => {
	    setDismissed(true);
	    phone.hidden = true;
	    launcherGroup.hidden = true;
	});

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

			// 編號可能在重新啟動後重複，
			// 加上產生時間與電話，避免將新訊息當成已讀。
			const messageKey = item => JSON.stringify([
			    String(item.id),
			    String(item.createdAt),
			    String(item.phone)
			]);

			const incoming = messages.filter(
			    item => !seen.has(messageKey(item))
			);

			for (const item of messages) {
			    seen.add(messageKey(item));
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
		    launcherGroup.hidden = true;
		    return;
		}

		// 只有新訊息可以解除關閉狀態並自動展開。
		if (incoming.length) {
		    setDismissed(false);
		    openPhone();
		    return;
		}

		launcherGroup.hidden = dismissed || !phone.hidden;
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