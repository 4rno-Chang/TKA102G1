document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("memberSearchModal");
    const box = modal.querySelector(".employee-modal-content");
    const form = document.getElementById("memberSearchForm");
    const openBtn = document.getElementById("searchMemberBtn");
    const nameInput = document.getElementById("searchMemberName");
    const phoneInput = document.getElementById("searchMemberPhone");

    function closeSearch() {
        modal.hidden = true;
        box.classList.remove("is-shaking");
        openBtn.focus();
    }

    openBtn.addEventListener("click", () => {
        form.reset();
        modal.hidden = false;
        nameInput.focus();
    });
    document.getElementById("closeMemberSearchModal").addEventListener("click", closeSearch);
    document.getElementById("cancelMemberSearchBtn").addEventListener("click", closeSearch);
    document.getElementById("resetMemberSearchBtn").addEventListener("click", event => {
        window.location.assign(event.currentTarget.dataset.url);
    });
    modal.addEventListener("click", event => {
        if (event.target !== modal) return;
        box.classList.remove("is-shaking");
        void box.offsetWidth;
        box.classList.add("is-shaking");
    });
    modal.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            event.preventDefault();
            closeSearch();
        }
        if (event.key === "Tab") {
            const controls = Array.from(modal.querySelectorAll("button, input"));
            const first = controls[0];
            const last = controls[controls.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        }
    });
    form.addEventListener("submit", event => {
        nameInput.value = nameInput.value.trim();
        phoneInput.value = phoneInput.value.trim();
        if (!nameInput.value && !phoneInput.value) {
            event.preventDefault();
            staffToast("請輸入會員姓名或手機號碼");
            nameInput.focus();
        }
    });
});
