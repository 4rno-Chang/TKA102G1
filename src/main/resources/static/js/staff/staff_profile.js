document.addEventListener('DOMContentLoaded', () => {

    // 取得個人資料彈窗與按鈕
    const profileButton = document.getElementById('staffProfileButton');
    const profileModal = document.getElementById('staffProfileModal');
    const closeButton = document.getElementById('staffProfileClose');

    if (!profileButton || !profileModal || !closeButton) {
        return;
    }

    // 將彈窗移到 body，避免被側邊欄裁切
    document.body.appendChild(profileModal);

    // 新密碼：點擊眼睛切換顯示／隱藏
    const newPasswordInput = document.getElementById('staffNormalNewPassword');
    const newPasswordEye = document.getElementById('staffNormalNewPasswordEye');

    if (newPasswordInput && newPasswordEye) {
        newPasswordEye.addEventListener('click', () => {
            const isHidden = newPasswordInput.type === 'password';

            newPasswordInput.type = isHidden ? 'text' : 'password';

            newPasswordEye.setAttribute(
                'aria-label',
                isHidden ? '隱藏新密碼' : '顯示新密碼'
            );

            // 切換眼睛圖示：顯示密碼為睜眼，隱藏密碼為閉眼
            const svg = newPasswordEye.querySelector('svg');

            if (svg) {
                svg.innerHTML = isHidden
                    ? '<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>'
                    : '<path d="M3 3l18 18"/><path d="M10.6 6.1A11.6 11.6 0 0 1 12 6c6.5 0 10 6 10 6a15 15 0 0 1-3.1 3.5"/><path d="M6.2 6.2C3.5 8 2 12 2 12s3.5 6 10 6c1.4 0 2.7-.3 3.8-.8"/><path d="M10 10a3 3 0 0 0 4 4"/>';
            }
        });
    }

    // 即時檢查新密碼是否至少 8 個字元
    const passwordLengthRule = document.getElementById('staffNormalPasswordLength');

    if (newPasswordInput && passwordLengthRule) {
        newPasswordInput.addEventListener('input', () => {
            const isValid = newPasswordInput.value.length >= 8;

            passwordLengthRule.textContent = isValid
                ? '✓ 密碼長度至少為 8 個字元'
                : '✕ 密碼長度至少為 8 個字元';

            passwordLengthRule.style.color = isValid
                ? '#27864b'
                : '#c4533b';
        });
    }

    // 確認新密碼：點擊眼睛切換顯示／隱藏
    const confirmPasswordInput = document.getElementById('staffNormalConfirmPassword');
    const confirmPasswordEye = document.getElementById('staffNormalConfirmPasswordEye');

    if (confirmPasswordInput && confirmPasswordEye) {
        confirmPasswordEye.addEventListener('click', () => {
            const isHidden = confirmPasswordInput.type === 'password';

            confirmPasswordInput.type = isHidden ? 'text' : 'password';

            confirmPasswordEye.setAttribute(
                'aria-label',
                isHidden ? '隱藏確認新密碼' : '顯示確認新密碼'
            );
			
			// 切換眼睛圖示：顯示密碼為睜眼，隱藏密碼為閉眼
			const svg = confirmPasswordEye.querySelector('svg');

			if (svg) {
			    svg.innerHTML = isHidden
			        ? '<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>'
			        : '<path d="M3 3l18 18"/><path d="M10.6 6.1A11.6 11.6 0 0 1 12 6c6.5 0 10 6 10 6a15 15 0 0 1-3.1 3.5"/><path d="M6.2 6.2C3.5 8 2 12 2 12s3.5 6 10 6c1.4 0 2.7-.3 3.8-.8"/><path d="M10 10a3 3 0 0 0 4 4"/>';
			}
        });
    }

    // 即時檢查密碼是否只包含英文與數字，且兩者都有
    const passwordFormatRule = document.getElementById('staffNormalPasswordFormat');

    if (newPasswordInput && passwordFormatRule) {
        newPasswordInput.addEventListener('input', () => {
            const isValid = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]+$/.test(newPasswordInput.value);

            passwordFormatRule.textContent = isValid
                ? '✓ 僅能使用英文與數字，且必須同時包含兩者'
                : '✕ 僅能使用英文與數字，且必須同時包含兩者';

            passwordFormatRule.style.color = isValid
                ? '#27864b'
                : '#c4533b';
        });
    }

    // 即時檢查「確認新密碼」是否與新密碼一致
    const passwordMatchRule = document.getElementById('staffNormalPasswordMatch');

    function checkPasswordMatch() {
        if (!newPasswordInput || !confirmPasswordInput || !passwordMatchRule) return;

        const newPassword = newPasswordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        // 尚未輸入到新密碼的長度，不顯示提示
        if (confirmPassword.length === 0 ||
            confirmPassword.length < newPassword.length) {
            passwordMatchRule.textContent = '';
            return;
        }

        if (newPassword === confirmPassword) {
            // 密碼一致：綠色勾勾
            passwordMatchRule.textContent = '✓ 兩次輸入的密碼一致';
            passwordMatchRule.style.color = '#27834a';
        } else {
            // 長度已達到，但密碼不同：紅字
            passwordMatchRule.textContent = '兩次輸入的密碼不一致';
            passwordMatchRule.style.color = '#c0392b';
        }
    }

    if (newPasswordInput && confirmPasswordInput && passwordMatchRule) {
        newPasswordInput.addEventListener('input', checkPasswordMatch);
        confirmPasswordInput.addEventListener('input', checkPasswordMatch);
    }

    // 離開確認密碼欄位時，檢查是否輸入完整
    if (newPasswordInput && confirmPasswordInput && passwordMatchRule) {
        confirmPasswordInput.addEventListener('blur', () => {
            const newPassword = newPasswordInput.value;
            const confirmPassword = confirmPasswordInput.value;

            if (confirmPassword.length === 0) return;

            if (confirmPassword.length < newPassword.length) {
                passwordMatchRule.textContent = '兩次輸入的密碼不一致';
                passwordMatchRule.style.color = '#c0392b';
            } else {
                checkPasswordMatch();
            }
        });
    }

    // 一般修改密碼：送出前完整驗證
    const normalPasswordForm = document.getElementById('staffNormalPasswordForm');
    const normalPasswordMessage = document.getElementById('staffNormalPasswordMessage');

    if (normalPasswordForm && newPasswordInput && confirmPasswordInput && normalPasswordMessage) {

        normalPasswordForm.addEventListener('submit', async (event) => {
            event.preventDefault();

            const newPassword = newPasswordInput.value;
            const confirmPassword = confirmPasswordInput.value;

            // 至少 8 個字元，僅限英文與數字，且兩者都要有
            const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/;

            if (!passwordRegex.test(newPassword)) {
                normalPasswordMessage.textContent = '密碼必須至少 8 個字元，且同時包含英文與數字';
                return;
            }

            if (newPassword !== confirmPassword) {
                normalPasswordMessage.textContent = '';
                confirmPasswordInput.focus();
                return;
            }

            // 驗證通過，呼叫後端修改密碼 API
            normalPasswordMessage.textContent = '';

            const submitButton = document.getElementById('staffNormalPasswordSubmit');
            submitButton.disabled = true;

            try {
                const response = await fetch('/api/employees/change-password', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        newPassword: newPassword
                    })
                });

                if (response.status === 401) {
                    window.location.href = '/staff/login';
                    return;
                }

                if (!response.ok) {
                    const result = await response.json();
                    throw new Error(result.message || '密碼修改失敗');
                }

                // 修改成功，下一步處理關閉彈窗與 Toast
                // 修改成功後重設表單
                resetNormalPasswordForm();

                // 關閉修改密碼彈窗，回到原本後台頁面
                document.getElementById('staffNormalPasswordModal').style.display = 'none';
                document.getElementById('staffProfileModal').hidden = true;

                // 顯示修改成功 Toast
                const toast = document.createElement('div');
                toast.className = 'staff-toast';
                toast.textContent = '✓ 密碼修改成功';
                document.body.appendChild(toast);

                requestAnimationFrame(() => {
                    requestAnimationFrame(() => toast.classList.add('show'));
                });

                // 3 秒後消失
                setTimeout(() => {
                    toast.classList.remove('show');
                    setTimeout(() => toast.remove(), 300);
                }, 3000);

            } catch (error) {
                normalPasswordMessage.style.color = '#c0392b';
                normalPasswordMessage.textContent = error.message || '密碼修改失敗';
            } finally {
                submitButton.disabled = false;
            }
        });
    }

    // 重設一般修改密碼表單與驗證提示
    function resetNormalPasswordForm() {
        if (!normalPasswordForm) return;

        // 清空兩個密碼欄位
        normalPasswordForm.reset();

        // 恢復密碼隱藏狀態
        newPasswordInput.type = 'password';
        confirmPasswordInput.type = 'password';
		
		// 恢復兩個眼睛的預設閉眼圖示
		[newPasswordEye, confirmPasswordEye].forEach((button) => {
		    if (!button) return;

		    const svg = button.querySelector('svg');

		    if (svg) {
		        svg.innerHTML =
		            '<path d="M3 3l18 18"/>' +
		            '<path d="M10.6 6.1A11.6 11.6 0 0 1 12 6c6.5 0 10 6 10 6a15 15 0 0 1-3.1 3.5"/>' +
		            '<path d="M6.2 6.2C3.5 8 2 12 2 12s3.5 6 10 6c1.4 0 2.7-.3 3.8-.8"/>' +
		            '<path d="M10 10a3 3 0 0 0 4 4"/>';
		    }

		    button.setAttribute('aria-pressed', 'false');
		});

		newPasswordEye?.setAttribute('aria-label', '顯示新密碼');
		confirmPasswordEye?.setAttribute('aria-label', '顯示確認新密碼');

        // 重設驗證提示
        passwordLengthRule.textContent = '○ 密碼長度至少為 8 個字元';
        passwordFormatRule.textContent = '○ 僅能使用英文與數字，且必須同時包含兩者';
        passwordMatchRule.textContent = '';

        passwordLengthRule.style.color = '#8c7d70';
        passwordFormatRule.style.color = '#8c7d70';
        passwordMatchRule.style.color = '#8c7d70';

        // 清除錯誤訊息
        normalPasswordMessage.textContent = '';
        normalPasswordMessage.style.color = '#c0392b';
    }

    // 一般員工修改密碼彈窗
    const changePasswordButton = document.getElementById('staffProfileChangePassword');
    const passwordModal = document.getElementById('staffNormalPasswordModal');
    const passwordCloseButton = document.getElementById('staffNormalPasswordClose');

    if (changePasswordButton && passwordModal && passwordCloseButton) {

        // 移到 body，避免被側邊欄裁切
        document.body.appendChild(passwordModal);

        // 點擊「修改密碼」
        changePasswordButton.addEventListener('click', () => {
            profileModal.hidden = true;
            passwordModal.style.display = 'flex';
        });

        // 點擊右上角 ×，重設密碼並返回個人資料
        passwordCloseButton.addEventListener('click', () => {
            resetNormalPasswordForm();

            passwordModal.style.display = 'none';
            profileModal.hidden = false;
        });

        // 點擊「取消」，重設密碼並返回個人資料
        const passwordCancelButton = document.getElementById('staffNormalPasswordCancel');

        passwordCancelButton.addEventListener('click', () => {
            resetNormalPasswordForm();

            passwordModal.style.display = 'none';
            profileModal.hidden = false;
        });

        // 點擊黑色背景時，修改密碼彈窗震動提醒，不關閉
        passwordModal.addEventListener('click', (event) => {
            if (event.target === passwordModal) {
                const dialog = passwordModal.querySelector('.staff-profile-dialog');

                dialog.classList.remove('password-shake');

                // 重新觸發動畫，連續點擊也能震動
                void dialog.offsetWidth;

                dialog.classList.add('password-shake');
            }
        });
    }

    // 取得個人資料欄位
    const empNo = document.getElementById('staffProfileEmpNo');
    const empName = document.getElementById('staffProfileEmpName');
    const empTel = document.getElementById('staffProfileEmpTel');
    const empIce = document.getElementById('staffProfileEmpIce');
    const empIcetel = document.getElementById('staffProfileEmpIcetel');
    const empAdd = document.getElementById('staffProfileEmpAdd');

    const photo = document.getElementById('staffProfilePhoto');
    const noPhoto = document.getElementById('staffProfileNoPhoto');

    // 開啟個人資料彈窗
    profileButton.addEventListener('click', async () => {

        try {
            // 從後端取得目前登入員工的資料
            const response = await fetch('/api/employees/me');

            if (response.status === 401) {
                window.location.href = '/staff/login';
                return;
            }

            if (!response.ok) {
                throw new Error('無法取得員工個人資料');
            }

            const data = await response.json();

            // 填入個人資料
            empNo.value = data.empNo ?? '';
            empName.value = data.empName ?? '';
            empTel.value = data.empTel ?? '';
            empIce.value = data.empIce ?? '';
            empIcetel.value = data.empIcetel ?? '';
            empAdd.value = data.empAdd ?? '';

            // 顯示員工照片；沒有照片時顯示預設圖片
            if (data.hasPhoto) {
                photo.src = `/api/employees/${data.empNo}/photo`;
                photo.hidden = false;
                noPhoto.hidden = true;
            } else {
                photo.hidden = true;
                noPhoto.hidden = false;
            }

            // 資料載入成功後才開啟彈窗
            profileModal.hidden = false;

        } catch (error) {
            console.error('讀取員工個人資料失敗：', error);
            alert('個人資料讀取失敗，請稍後再試');
        }
    });

    // 儲存員工個人資料
    const saveButton = document.getElementById('staffProfileSave');

    if (saveButton) {
        saveButton.addEventListener('click', async (event) => {
            event.preventDefault();

            // 只傳送允許修改的三個欄位
            const data = {
                empIce: empIce.value.trim(),
                empIcetel: empIcetel.value.trim(),
                empAdd: empAdd.value.trim()
            };

            // 避免重複點擊
            saveButton.disabled = true;

            try {
                const response = await fetch('/api/employees/me', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(data)
                });

                if (response.status === 401) {
                    window.location.href = '/staff/login';
                    return;
                }

                const result = await response.json();

                if (!response.ok) {
                    throw new Error(result.message || '個人資料修改失敗');
                }

                // 修改成功後關閉個人資料視窗
                profileModal.hidden = true;

                // 使用 Bistroops 後台共用的提示樣式
                const toast = document.createElement('div');
                toast.className = 'staff-toast';
                toast.textContent = '✓ 個人資料修改成功';

                document.body.appendChild(toast);

                // 觸發淡入動畫
                requestAnimationFrame(() => {
                    requestAnimationFrame(() => {
                        toast.classList.add('show');
                    });
                });

                // 3 秒後淡出並移除
                setTimeout(() => {
                    toast.classList.remove('show');

                    setTimeout(() => {
                        toast.remove();
                    }, 300);
                }, 3000);

            } catch (error) {
                console.error('儲存個人資料失敗：', error);
                alert(error.message || '個人資料修改失敗');

            } finally {
                saveButton.disabled = false;
            }
        });
    }

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
        if (event.key !== 'Escape') return;

        // 一般修改密碼彈窗開啟時，ESC 返回個人資料
        const passwordModal = document.getElementById('staffNormalPasswordModal');

        if (passwordModal && passwordModal.style.display === 'flex') {

            resetNormalPasswordForm();

            passwordModal.style.display = 'none';
            profileModal.hidden = false;
            return;
        }

        // 個人資料彈窗開啟時，ESC 關閉
        if (!profileModal.hidden) {
            profileModal.hidden = true;
        }
    });

});