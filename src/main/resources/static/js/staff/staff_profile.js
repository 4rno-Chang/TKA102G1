
document.addEventListener('DOMContentLoaded', () => {

    // 取得個人資料彈窗與按鈕
    const profileButton = document.getElementById('staffProfileButton');
    const profileModal = document.getElementById('staffProfileModal');
    const closeButton = document.getElementById('staffProfileClose');

    if (!profileButton || !profileModal || !closeButton) {
        return;
    }

    // 開啟個人資料彈窗
    profileButton.addEventListener('click', () => {
        profileModal.hidden = false;
    });

    // 關閉個人資料彈窗
    closeButton.addEventListener('click', () => {
        profileModal.hidden = true;
    });

    // 點擊彈窗外面的黑色背景時關閉
    profileModal.addEventListener('click', (event) => {
        if (event.target === profileModal) {
            profileModal.hidden = true;
        }
    });

    // 按 ESC 關閉彈窗
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !profileModal.hidden) {
            profileModal.hidden = true;
        }
    });

});
