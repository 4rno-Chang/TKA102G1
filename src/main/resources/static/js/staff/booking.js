/*
 * Bistroops 後台訂候位頁面。
 * 資料由 Controller 提供，本檔負責查詢日期月曆。
 */
(() => {
    'use strict';

    const WEEKDAYS = '日一二三四五六';

    const taipeiDateFormatter = new Intl.DateTimeFormat('en-US', {
        timeZone: 'Asia/Taipei',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });

    function todayText() {
        const parts = Object.fromEntries(
            taipeiDateFormatter.formatToParts(new Date())
                .map(part => [part.type, part.value])
        );

        return `${parts.year}-${parts.month}-${parts.day}`;
    }

    function toDateText(date) {
        const two = number => String(number).padStart(2, '0');

        return [
            date.getFullYear(),
            two(date.getMonth() + 1),
            two(date.getDate())
        ].join('-');
    }

    function parseDate(value) {
        if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) {
            return null;
        }

        const [year, month, day] = value.split('-').map(Number);
        const date = new Date(year, month - 1, day);

        return toDateText(date) === value ? date : null;
    }

    function inputOf(picker) {
        return picker.querySelector('input[type="hidden"]');
    }

    function refreshLabel(picker) {
        const input = inputOf(picker);
        const label = picker.querySelector('[data-date-text]');

        if (!input || !label) return;

        const date = parseDate(input.value);

        label.textContent = date
            ? `${input.value.replaceAll('-', '/')}（${WEEKDAYS[date.getDay()]}）`
            : '請選擇日期';

        picker.classList.toggle('is-empty', !date);
    }

    function closePanel(picker) {
        const panel = picker.querySelector('[data-date-panel]');
        const button = picker.querySelector('[data-date-toggle]');

        if (panel) panel.hidden = true;
        if (button) button.setAttribute('aria-expanded', 'false');

        picker.classList.remove('is-open');
    }

    function closeAllPanels() {
        document.querySelectorAll('[data-date-picker].is-open')
            .forEach(closePanel);
    }

    function positionPanel(picker) {
        const panel = picker.querySelector('[data-date-panel]');
        const toggle = picker.querySelector('[data-date-toggle]');

        if (!panel || !toggle) return;

        const rect = toggle.getBoundingClientRect();
        const margin = 10;
        const gap = 6;

        let top = rect.bottom + gap;

        if (top + panel.offsetHeight > window.innerHeight - margin) {
            top = rect.top - panel.offsetHeight - gap;
        }

        top = Math.max(
            margin,
            Math.min(top, window.innerHeight - panel.offsetHeight - margin)
        );

        const left = Math.max(
            margin,
            Math.min(
                rect.left,
                window.innerWidth - panel.offsetWidth - margin
            )
        );

        panel.style.top = `${top}px`;
        panel.style.left = `${left}px`;
    }

    function drawPanel(picker, year, month) {
        const panel = picker.querySelector('[data-date-panel]');
        const input = inputOf(picker);

        if (!panel || !input) return;

        const today = todayText();
        const min = picker.dataset.min === 'today'
            ? today
            : (picker.dataset.min || '');

        const max = picker.dataset.max || '';

        function unavailable(value) {
            return Boolean(
                (min && value < min) || (max && value > max)
            );
        }

        const firstWeekday = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();

        let days = '<span></span>'.repeat(firstWeekday);

        for (let day = 1;day <= daysInMonth;day++) {
            const value = toDateText(new Date(year, month, day));
            const classes = ['booking-cal-day'];
            const selected = value === input.value;

            if (value === today) classes.push('is-today');
            if (selected) classes.push('is-selected');

            days += `
                <button type="button"
                        class="${classes.join(' ')}"
                        data-date-day="${value}"
                        aria-pressed="${selected}"
                        ${unavailable(value) ? 'disabled' : ''}>
                    ${day}
                </button>`;
        }

        const previousDisabled = Boolean(min)
            && toDateText(new Date(year, month, 0)) < min;

        const nextDisabled = Boolean(max)
            && toDateText(new Date(year, month + 1, 1)) > max;

        panel.dataset.year = String(year);
        panel.dataset.month = String(month);

        panel.innerHTML = `
            <div class="booking-cal-head">
                <button type="button"
                        class="booking-cal-nav"
                        data-date-nav="-1"
                        aria-label="上個月"
                        ${previousDisabled ? 'disabled' : ''}>
                    ‹
                </button>

                <span class="booking-cal-title">
                    ${year} 年 ${month + 1} 月
                </span>

                <button type="button"
                        class="booking-cal-nav"
                        data-date-nav="1"
                        aria-label="下個月"
                        ${nextDisabled ? 'disabled' : ''}>
                    ›
                </button>
            </div>

            <div class="booking-cal-week">
                ${[...WEEKDAYS].map(day => `<span>${day}</span>`).join('')}
            </div>

            <div class="booking-cal-days">${days}</div>

            <div class="booking-cal-foot">
                <button type="button"
                        class="booking-cal-today"
                        data-date-day="${today}"
                        ${unavailable(today) ? 'disabled' : ''}>
                    今天
                </button>
            </div>`;

        positionPanel(picker);
    }

    function openPanel(picker) {
        const panel = picker.querySelector('[data-date-panel]');
        const toggle = picker.querySelector('[data-date-toggle]');
        const input = inputOf(picker);

        if (!panel || !toggle || !input) return;

        closeAllPanels();

        const date = parseDate(input.value) || parseDate(todayText());

        panel.hidden = false;
        toggle.setAttribute('aria-expanded', 'true');
        picker.classList.add('is-open');

        drawPanel(picker, date.getFullYear(), date.getMonth());
    }

    document.addEventListener('click', event => {
        if (!(event.target instanceof Element)) return;

        const toggle = event.target.closest('[data-date-toggle]');

        if (toggle) {
            const picker = toggle.closest('[data-date-picker]');

            if (!picker) return;

            if (picker.classList.contains('is-open')) {
                closePanel(picker);
            } else {
                openPanel(picker);
            }

            return;
        }

        const nav = event.target.closest('[data-date-nav]');

        if (nav) {
            if (nav.disabled) return;

            const picker = nav.closest('[data-date-picker]');
            const panel = picker?.querySelector('[data-date-panel]');

            if (!picker || !panel) return;

            const target = new Date(
                Number(panel.dataset.year),
                Number(panel.dataset.month) + Number(nav.dataset.dateNav),
                1
            );

            drawPanel(
                picker,
                target.getFullYear(),
                target.getMonth()
            );

            return;
        }

        const day = event.target.closest('[data-date-day]');

        if (day) {
            if (day.disabled) return;

            const picker = day.closest('[data-date-picker]');
            const input = picker && inputOf(picker);

            if (!picker || !input || !parseDate(day.dataset.dateDay)) return;

            input.value = day.dataset.dateDay;
            refreshLabel(picker);
            closePanel(picker);

            picker.querySelector('[data-date-toggle]')?.focus();

            input.dispatchEvent(new Event('change', { bubbles: true }));
            picker.dispatchEvent(new CustomEvent('datepick', {
                bubbles: true,
                detail: { date: input.value }
            }));

            const submitButton = document.getElementById(
                picker.dataset.dateSubmit || ''
            );

            if (submitButton?.form && !submitButton.disabled) {
                submitButton.form.requestSubmit(submitButton);
            }

            return;
        }

        if (!event.target.closest('[data-date-picker]')) {
            closeAllPanels();
        }
    });

    document.addEventListener('keydown', event => {
        if (event.key !== 'Escape') return;

        const picker = document.querySelector(
            '[data-date-picker].is-open'
        );

        if (picker) {
            closePanel(picker);
            picker.querySelector('[data-date-toggle]')?.focus();
        }
    });

    window.addEventListener('resize', closeAllPanels);

    window.addEventListener('scroll', event => {
        if (event.target instanceof Element
            && event.target.closest('[data-date-panel]')) {
            return;
        }

        closeAllPanels();
    }, true);

    function initialize() {
        document.querySelectorAll('[data-date-picker]')
            .forEach(refreshLabel);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialize, {
            once: true
        });
    } else {
        initialize();
    }
})();
/*
 * 後台新增／編輯訂位。
 * 使用 Controller 一次載入的剩餘桌數資料。
 */
(() => {
    'use strict';

    function initializeStaffReservationEditor() {
        const form = document.getElementById('staffReservationEditor');
        const config = window.staffBookingEditor;

        if (!form || !config?.availability) return;

        const dateInput = document.getElementById('staffRsvDate');
        const tableInput = document.getElementById('staffRsvTableType');
        const slotInput = document.getElementById('staffRsvSlot');
        const message = document.getElementById('staffRsvSlotMessage');
        const saveButton = document.getElementById('staffRsvSave');

        const availability = config.availability;

        function refreshSlots(preferredSlot = '') {
            const slots = availability.slotsByDate?.[
                dateInput.value
            ]?.[tableInput.value] || [];

            slotInput.replaceChildren(
                new Option('請選擇時段', '')
            );

            for (const slot of slots) {
                const remaining = Number(slot.remaining);
                const full = slot.full === true || remaining <= 0;

                const option = new Option(
                    `${slot.timeText}｜${full
                        ? '已額滿'
                        : `剩餘 ${remaining} 桌`}`,
                    String(slot.rsvDtNo)
                );

                option.disabled = full;

                if (!full
                    && String(slot.rsvDtNo) === String(preferredSlot)) {
                    option.selected = true;
                }

                slotInput.add(option);
            }

            const hasAvailableSlot = slots.some(
                slot => slot.full !== true
                    && Number(slot.remaining) > 0
            );

            slotInput.disabled = !hasAvailableSlot;
            saveButton.disabled = !hasAvailableSlot;

            message.textContent = hasAvailableSlot
                ? '實際名額會在儲存時再次確認。'
                : '這一天的所選桌型沒有可預約時段，請更換日期或桌型。';
        }

        dateInput.addEventListener('change', () => {
            refreshSlots();
        });

        tableInput.addEventListener('change', () => {
            refreshSlots();
        });

        form.addEventListener('submit', event => {
            if (!form.reportValidity()) {
                event.preventDefault();
                return;
            }

            saveButton.disabled = true;
            saveButton.textContent = '儲存中…';
        });

        window.addEventListener('pageshow', () => {
            saveButton.textContent = '儲存訂位';
            refreshSlots(slotInput.value);
        });

        refreshSlots(config.selectedSlot);
    }

    if (document.readyState === 'loading') {
        document.addEventListener(
            'DOMContentLoaded',
            initializeStaffReservationEditor,
            { once: true }
        );
    } else {
        initializeStaffReservationEditor();
    }
})();