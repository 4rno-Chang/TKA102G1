(() => {
  const form = document.getElementById('resvForm');
  if (!form) return;

  const data = window.bistroopsReservationAvailability;

  const calendar = document.getElementById('calendar');
  const dateInput = document.getElementById('resvDate');
  const toggle = document.getElementById('calendarToggle');
  const slotGrid = document.getElementById('slotGrid');
  const slotEmpty = document.getElementById('slotEmpty');
  const nextButton = document.getElementById('resvNext');

  if (!data || !data.minDate || !data.maxDate) {
    slotEmpty.textContent = '訂位資料無法載入，請重新整理頁面。';
    nextButton.disabled = true;
    return;
  }

  const slotsByDate = data.slotsByDate || {};
  const editData = window.bistroopsReservationEdit;

  // 每個日期及桌型，各自記住最後選擇的時段。
  const selectedSlots = new Map();

  if (editData?.selectedDate && editData.selectedSlot != null) {
      const key =
          `${editData.selectedDate}|${editData.selectedTableType}`;

      selectedSlots.set(key, String(editData.selectedSlot));
  }
  
  
  const minDate = data.minDate;
  const maxDate = data.maxDate;

  const loadedAt = performance.now();

  function serverNow() {
    return Number(data.serverTime)
      + performance.now() - loadedAt;
  }

  function tableType() {
    return form.querySelector(
      'input[name="tableType"]:checked'
    )?.value || '2';
  }

  function slotsFor(date) {
    return slotsByDate[date]?.[tableType()] || [];
  }

  function available(slot) {
    return Number(slot.remaining) > 0
      && Number(slot.startAt) > serverNow();
  }

  function selectableDate(date) {
    return date >= minDate
      && date <= maxDate
      && slotsFor(date).some(available);
  }

  function monthIndex(date) {
    const [year, month] = date.split('-').map(Number);
    return year * 12 + month - 1;
  }

  const minMonth = monthIndex(minDate);
  const maxMonth = monthIndex(maxDate);

  if (dateInput.value
      && (dateInput.value < minDate || dateInput.value > maxDate)) {
    dateInput.value = '';
  }

  let currentMonth = monthIndex(dateInput.value || minDate);
  let calendarOpen = !dateInput.value;

  function setText(id, text) {
    const element = document.getElementById(id);
    if (element) element.textContent = text;
  }

  function updateSummary() {
    const selected = form.querySelector(
      'input[name="rsvDtNo"]:checked'
    );

    setText(
      'sumDate',
      dateInput.value
        ? dateInput.value.replaceAll('-', '/')
        : '尚未選擇'
    );

    setText('sumTable', `${tableType()} 人桌`);
    setText('sumTime', selected?.dataset.time || '尚未選擇');

    nextButton.disabled = !(
      dateInput.value && selected && !selected.disabled
    );
  }

  function updateCalendarVisibility() {
    toggle.hidden = !dateInput.value;
    toggle.setAttribute('aria-expanded', String(calendarOpen));

    toggle.querySelector('.date-field-value').textContent =
      dateInput.value.replaceAll('-', '/');

    toggle.querySelector('.date-field-action').textContent =
      calendarOpen ? '收起日曆 ▴' : '開啟日曆 ▾';

    calendar.classList.toggle('is-collapsed', !calendarOpen);
  }

  function renderCalendar() {
    const year = Math.floor(currentMonth / 12);
    const month = currentMonth % 12 + 1;

    const firstWeekday = new Date(year, month - 1, 1).getDay();
    const totalDays = new Date(year, month, 0).getDate();

    let days = '<span></span>'.repeat(firstWeekday);

    for (let day = 1; day <= totalDays; day++) {
      const date =
        `${year}-${String(month).padStart(2, '0')}`
        + `-${String(day).padStart(2, '0')}`;

      const enabled = selectableDate(date);
      const classes = ['cal-day'];

      if (enabled) classes.push('is-open');
      if (date === dateInput.value) classes.push('is-selected');
      if (date === minDate) classes.push('is-today');

      days += `
        <button type="button"
                class="${classes.join(' ')}"
                data-date="${date}"
                ${enabled ? '' : 'disabled'}>
          ${day}
        </button>
      `;
    }

    calendar.innerHTML = `
      <div class="cal-head">
        <button type="button"
                class="cal-nav"
                data-cal-move="-1"
                aria-label="上個月"
                ${currentMonth <= minMonth ? 'disabled' : ''}>‹</button>

        <span class="cal-title">${year}年${month}月</span>

        <button type="button"
                class="cal-nav"
                data-cal-move="1"
                aria-label="下個月"
                ${currentMonth >= maxMonth ? 'disabled' : ''}>›</button>
      </div>

      <div class="cal-week">
        <span>日</span><span>一</span><span>二</span>
        <span>三</span><span>四</span><span>五</span><span>六</span>
      </div>

      <div class="cal-grid">${days}</div>
    `;

    updateCalendarVisibility();
  }

  function renderSlots() {
      // 先保留目前畫面選到的時段，再重新產生下一組選項。
      const previousKey = slotGrid.dataset.selectionKey;

      const previousSelection = slotGrid.querySelector(
          'input[name="rsvDtNo"]:checked'
      );

      if (previousKey && previousSelection) {
          selectedSlots.set(previousKey, previousSelection.value);
      }

      slotGrid.replaceChildren();

      const selectionKey = `${dateInput.value}|${tableType()}`;
      slotGrid.dataset.selectionKey = selectionKey;

      if (!dateInput.value) {
          slotEmpty.hidden = false;
          slotEmpty.textContent = '請先選擇日期，以查看可預約的時段。';

          updateSummary();
          return;
      }

      const slots = slotsFor(dateInput.value);
      const selectedSlot = selectedSlots.get(selectionKey);

      slotEmpty.hidden = slots.length > 0;
      slotEmpty.textContent = '這一天沒有可選的時段，請選擇其他日期。';

      for (const slot of slots) {
          const future = Number(slot.startAt) > serverNow();

          const label = document.createElement('label');
          label.className = 'slot';

          const input = document.createElement('input');
          input.type = 'radio';
          input.name = 'rsvDtNo';
          input.value = String(slot.rsvDtNo);
          input.disabled = !available(slot);

          input.checked =
              !input.disabled
              && input.value === selectedSlot;

          input.dataset.time = slot.timeText;
          input.dataset.startAt = String(slot.startAt);

          const box = document.createElement('span');
          box.className = 'slot-box';

          const time = document.createElement('span');
          time.className = 'slot-time';
          time.textContent = slot.timeText;

          const state = document.createElement('span');
          state.className = 'slot-state';

          const original = editData?.original;

          const isOriginalSlot = original
              && dateInput.value === original.date
              && tableType() === String(original.tableType)
              && String(slot.rsvDtNo) === String(original.rsvDtNo);

          if (!future) {
              state.textContent = '已開始';

          } else if (!available(slot)) {
              state.textContent = '已額滿';

          } else if (isOriginalSlot) {
              state.textContent =
                  `可選 ${slot.remaining} 桌（含原訂位）`;

          } else {
              state.textContent = `剩餘 ${slot.remaining} 桌`;
          }

          box.append(time, state);
          label.append(input, box);
          slotGrid.append(label);
      }

      updateSummary();
  }

  calendar.addEventListener('click', event => {
    const move = event.target.closest('[data-cal-move]');

    if (move && !move.disabled) {
      const next = currentMonth + Number(move.dataset.calMove);

      if (next >= minMonth && next <= maxMonth) {
        currentMonth = next;
        renderCalendar();
      }

      return;
    }

    const day = event.target.closest('[data-date]');

    if (!day || day.disabled) return;

    if (!selectableDate(day.dataset.date)) {
      renderCalendar();
      return;
    }

    dateInput.value = day.dataset.date;
    calendarOpen = false;

    renderSlots();
    renderCalendar();
  });

  toggle.addEventListener('click', () => {
    calendarOpen = !calendarOpen;
    updateCalendarVisibility();
  });

  form.addEventListener('change', event => {
    if (event.target.name === 'tableType') {
      renderSlots();
      renderCalendar();
    }

    if (event.target.name === 'rsvDtNo') {
      updateSummary();
    }
  });

  form.addEventListener('submit', event => {
    const selected = form.querySelector(
      'input[name="rsvDtNo"]:checked'
    );

    const slot = slotsFor(dateInput.value).find(item =>
      String(item.rsvDtNo) === selected?.value
    );

    if (!slot || !available(slot)) {
      event.preventDefault();

      renderSlots();
      renderCalendar();

      alert('請重新選擇可預約的日期與時段。');
    }
  });

  function refreshTimeState() {
    slotGrid.querySelectorAll(
      'input[name="rsvDtNo"]'
    ).forEach(input => {
      if (Number(input.dataset.startAt) <= serverNow()) {
        input.checked = false;
        input.disabled = true;

        input.closest('.slot')
          .querySelector('.slot-state').textContent = '已開始';
      }
    });

    renderCalendar();
    updateSummary();
  }

  setInterval(refreshTimeState, 30000);
  window.addEventListener('pageshow', refreshTimeState);

  renderCalendar();
  renderSlots();
})();