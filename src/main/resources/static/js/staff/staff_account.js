/* ========== 員工個人選單：展開與收合 ========== */

document.addEventListener('DOMContentLoaded', () => {

    // 1. 取得員工個人選單
    const accountGroup = document.querySelector('.staff-account-group');
    const accountToggle = document.querySelector('[data-account-toggle]');

    if (!accountGroup || !accountToggle) return;

    // 2. 預設收合
    accountGroup.classList.remove('open');
    accountToggle.setAttribute('aria-expanded', 'false');

    // 3. 點擊員工姓名，展開或收合
    accountToggle.addEventListener('click', () => {
        const isOpen = accountGroup.classList.toggle('open');
        accountToggle.setAttribute('aria-expanded', String(isOpen));
    });
	
	
	// 4. 員工登出
	const logoutButton = document.getElementById('staffLogoutButton');

	if (logoutButton) {
	    logoutButton.addEventListener('click', async () => {
	        try {
	            const response = await fetch('/api/employees/logout', {
	                method: 'POST'
	            });

	            if (!response.ok) {
	                throw new Error('登出失敗');
	            }

	            // 登出成功，返回員工登入頁
	            window.location.href = '/staff/login';

	        } catch (error) {
	            alert('登出失敗，請稍後再試');
	            console.error(error);
	        }
	    });
	}

});