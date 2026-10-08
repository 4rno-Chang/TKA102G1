(() => {
    const form = document.getElementById('resvForm');
    if (!form) return;

    const calendar = document.getElementById('calendar');
    const dateInput = document.getElementById('resvDate');
    const toggle = document.getElementById('calendarToggle');

    const openDates = new Set(
        (calendar.dataset.openDates || '').split(',').filter(Boolean)
    );

    const minDate = calendar.dataset.min;
    const maxDate = calendar.dataset.max;

    function monthIndex(dateText) {
        const [year, month] = dateText.split('-').map(Number);
        return year * 12 + month - 1;
    }

    const minMonth = monthIndex(minDate);
    const maxMonth = monthIndex(maxDate);

    let currentMonth = monthIndex(dateInput.value || minDate);
    let calendarOpen = !dateInput.value;

    function setText(id, text) {
        const element = document.getElementById(id);
        if (element) element.textContent = text;
    }

    function updateSummary() {
        const table = form.querySelector(
            'input[name="tableType"]:checked'
			
        );

        const slot = form.querySelector(
            'input[name="rsvDtNo"]:checked'
        );

        setText(
            'sumDate',
            dateInput.value
                ? dateInput.value.replaceAll('-', '/')
                : '尚未選擇'
        );

        setText('sumTable', `${table.value} 人桌`);
        setText('sumTime', slot ? slot.dataset.time : '尚未選擇');
		
		const nextButton = document.getElementById('resvNext');
		nextButton.disabled = !(dateInput.value && slot);
    }

    function updateCalendarVisibility() {
        toggle.hidden = !dateInput.value;
        toggle.setAttribute('aria-expanded', String(calendarOpen));

        const value = toggle.querySelector('.date-field-value');
        value.textContent = dateInput.value.replaceAll('-', '/');

        const action = toggle.querySelector('.date-field-action');
        action.textContent = calendarOpen ? '收起日曆 ▴' : '開啟日曆 ▾';

        calendar.classList.toggle('is-collapsed', !calendarOpen);
    }

    function renderCalendar() {
        const year = Math.floor(currentMonth / 12);
        const month = currentMonth % 12 + 1;

        const firstWeekday = new Date(year, month - 1, 1).getDay();
        const totalDays = new Date(year, month, 0).getDate();

        let days = '<span></span>'.repeat(firstWeekday);

        for (let day = 1;day <= totalDays;day++) {
            const dateText =
                `${year}-${String(month).padStart(2, '0')}` +
                `-${String(day).padStart(2, '0')}`;

            const selectable =
                openDates.has(dateText) &&
                dateText >= minDate &&
                dateText <= maxDate;

            const classes = ['cal-day'];

            if (selectable) classes.push('is-open');
            if (dateText === dateInput.value) classes.push('is-selected');
            if (dateText === minDate) classes.push('is-today');

            days += `
        <button type="button"
                class="${classes.join(' ')}"
                data-date="${dateText}"
                ${selectable ? '' : 'disabled'}>
          ${day}
        </button>
      `;
        }

        calendar.innerHTML = `
      <div class="cal-head">
        <button type="button" class="cal-nav"
                data-cal-move="-1" aria-label="上個月"
                ${currentMonth <= minMonth ? 'disabled' : ''}>
          ‹
        </button>

        <span class="cal-title">${year}年${month}月</span>

        <button type="button" class="cal-nav"
                data-cal-move="1" aria-label="下個月"
                ${currentMonth >= maxMonth ? 'disabled' : ''}>
          ›
        </button>
      </div>

      <div class="cal-week">
        <span>日</span><span>一</span><span>二</span>
        <span>三</span><span>四</span><span>五</span><span>六</span>
      </div>

      <div class="cal-grid">${days}</div>
    `;

        updateCalendarVisibility();
    }

    // 日期或桌型變更後，清掉原本的時段，再查詢後端。
    function querySlots() {
        form.querySelectorAll('input[name="rsvDtNo"]').forEach(input => {
            input.checked = false;
        });

        // 尚未選日期時，不送出空日期。
        dateInput.disabled = !dateInput.value;
        form.requestSubmit();
    }

    calendar.addEventListener('click', event => {
        const moveButton = event.target.closest('[data-cal-move]');

        if (moveButton && !moveButton.disabled) {
            const nextMonth =
                currentMonth + Number(moveButton.dataset.calMove);

            if (nextMonth >= minMonth && nextMonth <= maxMonth) {
                currentMonth = nextMonth;
                renderCalendar();
            }

            return;
        }

        const dayButton = event.target.closest('[data-date]');

        if (dayButton && !dayButton.disabled) {
            dateInput.value = dayButton.dataset.date;
            querySlots();
        }
    });

    toggle.addEventListener('click', () => {
        calendarOpen = !calendarOpen;
        updateCalendarVisibility();
    });

    form.addEventListener('change', event => {
        if (event.target.name === 'tableType') {
            querySlots();
        }

        if (event.target.name === 'rsvDtNo') {
            updateSummary();
        }
    });

    renderCalendar();
    updateSummary();
})();