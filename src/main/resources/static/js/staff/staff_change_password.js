document.addEventListener("DOMContentLoaded", function () {

    // 取得首次登入修改密碼彈窗
    const modal = document.getElementById("firstPasswordModal");

    // 非首次登入，不需要執行
    if (!modal) {
        return;
    }

    const form = document.getElementById("firstPasswordForm");
    const message = document.getElementById("firstPasswordMessage");
    const submitButton = document.getElementById("firstPasswordSubmit");

    form.addEventListener("submit", async function (event) {

        event.preventDefault();

        const oldPassword =
            document.getElementById("firstOldPassword").value;

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
            message.textContent = "兩次輸入的新密碼不一致";
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
                    oldPassword: oldPassword,
                    newPassword: newPassword
                })
            });

            const result = await response.json();

            if (!response.ok) {
                message.textContent =
                    result.message || "密碼修改失敗";
                return;
            }

            // 5. 修改成功，重新載入後台首頁
            alert("密碼修改成功");

            window.location.href = "/staff";

        } catch (error) {
            message.textContent = "系統發生錯誤，請稍後再試";

        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "確認修改密碼";
        }
    });
});