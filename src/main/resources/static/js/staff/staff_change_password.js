document.addEventListener("DOMContentLoaded", function() {
	
	// 修改密碼成功後，顯示 Bistroops 後台 Toast
	if (sessionStorage.getItem("staffPasswordSuccess") === "true") {

	    // 清除紀錄，避免重新整理後再次出現
	    sessionStorage.removeItem("staffPasswordSuccess");

	    const toast = document.createElement("div");
	    toast.className = "staff-toast";
	    toast.textContent = "✓ 密碼修改成功";

	    document.body.appendChild(toast);

	    // 觸發淡入動畫
	    requestAnimationFrame(() => {
	        requestAnimationFrame(() => {
	            toast.classList.add("show");
	        });
	    });

	    // 3 秒後淡出並移除
	    setTimeout(() => {
	        toast.classList.remove("show");

	        setTimeout(() => {
	            toast.remove();
	        }, 300);
	    }, 3000);
	}

    // 取得首次登入修改密碼彈窗
    const modal = document.getElementById("firstPasswordModal");

    // 非首次登入，不需要執行
    if (!modal) {
        return;
    }

    const form = document.getElementById("firstPasswordForm");
    const message = document.getElementById("firstPasswordMessage");
    const submitButton = document.getElementById("firstPasswordSubmit");
    // 點擊眼睛，切換密碼顯示／隱藏
    modal.querySelectorAll(".first-password-eye").forEach(function(button) {

        button.addEventListener("click", function() {

            // 找到眼睛對應的密碼欄位
            const input = document.getElementById(button.dataset.target);

            // 切換密碼顯示狀態
            const showPassword = input.type === "password";

            input.type = showPassword ? "text" : "password";

            // 更新按鈕狀態
            button.setAttribute("aria-pressed", String(showPassword));
            button.setAttribute(
                "aria-label",
                showPassword ? "隱藏密碼" : "顯示密碼"
            );

            // 切換眼睛圖示
            const svg = button.querySelector("svg");

			if (svg) {
			    svg.innerHTML = showPassword
			        ? '<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>'
			        : '<path d="M3 3l18 18"/><path d="M10.6 6.1A11.6 11.6 0 0 1 12 6c6.5 0 10 6 10 6a15 15 0 0 1-3.1 3.5"/><path d="M6.2 6.2C3.5 8 2 12 2 12s3.5 6 10 6c1.4 0 2.7-.3 3.8-.8"/><path d="M10 10a3 3 0 0 0 4 4"/>';
			}
        });
    });

    // 取得密碼欄位與驗證提示
    const newPasswordInput = document.getElementById("firstNewPassword");
    const lengthRule = document.getElementById("firstPasswordLength");
    const formatRule = document.getElementById("firstPasswordFormat");

    // 取得確認新密碼欄位及提示
    const confirmPasswordInput =
        document.getElementById("firstConfirmPassword");

    const matchRule =
        document.getElementById("firstPasswordMatch");

		// 即時檢查兩次密碼是否一致
		function checkPasswordMatch() {

		    const newPassword = newPasswordInput.value;
		    const confirmPassword = confirmPasswordInput.value;

		    // 尚未輸入完成：不顯示提示
		    if (confirmPassword.length === 0 ||
		        confirmPassword.length < newPassword.length) {

		        matchRule.textContent = "";
		        return;
		    }

		    // 兩次密碼一致
		    if (newPassword === confirmPassword) {
		        matchRule.textContent = "✓ 兩次輸入的密碼一致";
		        matchRule.style.color = "#27834a";
		    } else {
		        // 長度足夠，但密碼不同
		        matchRule.textContent = "兩次輸入的密碼不一致";
		        matchRule.style.color = "#c0392b";
		    }
		}

    // 確認密碼輸入時，立即比對
    confirmPasswordInput.addEventListener("input", checkPasswordMatch);

    // 新密碼改變時，也重新比對
    newPasswordInput.addEventListener("input", checkPasswordMatch);
	
	// 離開確認新密碼欄位時，檢查兩次密碼是否一致
	confirmPasswordInput.addEventListener("blur", function() {

	    const newPassword = newPasswordInput.value;
	    const confirmPassword = confirmPasswordInput.value;

	    // 尚未輸入，不顯示提示
	    if (confirmPassword.length === 0) {
	        matchRule.textContent = "";
	        return;
	    }

	    // 已輸入但與新密碼不同，顯示紅字
	    if (newPassword !== confirmPassword) {
	        matchRule.textContent = "兩次輸入的密碼不一致";
	        matchRule.style.color = "#c0392b";
	    } else {
	        checkPasswordMatch();
	    }
	});

    // 即時檢查新密碼
    newPasswordInput.addEventListener("input", function() {

        const password = newPasswordInput.value;

        // 至少 8 個字元
        const validLength = password.length >= 8;

        // 只能使用英文、數字，且兩者都要有
        const validFormat =
            /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]+$/.test(password);

        // 更新長度提示
        lengthRule.textContent =
            (validLength ? "✓ " : "✕ ") + "密碼長度至少為 8 個字元";

        lengthRule.style.color = validLength ? "#27864b" : "#c4533b";

        // 更新格式提示
        formatRule.textContent =
            (validFormat ? "✓ " : "✕ ") +
            "僅能使用英文與數字，且必須同時包含兩者";

        formatRule.style.color = validFormat ? "#27864b" : "#c4533b";
    });

    form.addEventListener("submit", async function(event) {

        event.preventDefault();

        const newPassword =
            document.getElementById("firstNewPassword").value;

        const confirmPassword =
            document.getElementById("firstConfirmPassword").value;

        message.textContent = "";

        // 1. 檢查密碼格式：至少 8 碼，包含英文及數字
        const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/;


        if (!passwordRegex.test(newPassword)) {
            message.textContent = "新密碼至少 8 碼，且必須包含英文及數字";
            return;
        }

		// 2. 檢查兩次新密碼是否一致
		if (newPassword !== confirmPassword) {
		    confirmPasswordInput.focus();
		    return;
		}

        // 3. 避免重複送出
        submitButton.disabled = true;
        submitButton.textContent = "修改中...";

        try {
            // 4. 呼叫 Spring Boot 修改密碼 API
            const response = await fetch("/api/employees/change-password", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    newPassword: newPassword
                })
            });

            const result = await response.json();

            if (!response.ok) {
                message.textContent = result.message || "密碼修改失敗";
                return;
            }

            // 5. 修改成功，先記錄提示，再重新載入後台首頁
            sessionStorage.setItem("staffPasswordSuccess", "true");

            window.location.href = "/staff";

        } catch (error) {
            message.textContent = "系統發生錯誤，請稍後再試";

        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "確認修改密碼";
        }
    });
});