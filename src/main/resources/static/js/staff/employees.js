/* ===== 員工停用／恢復在職確認視窗 ===== */

function employeeConfirm(message) {
    return new Promise((resolve) => {
        const modal = document.getElementById("employeeConfirmModal");
        const box = modal.querySelector(".employee-confirm-box");
        const messageEl = document.getElementById("employeeConfirmMessage");
        const okBtn = document.getElementById("employeeConfirmOk");
        const cancelBtn = document.getElementById("employeeConfirmCancel");

        const previousFocus = document.activeElement;

        messageEl.textContent = message;
        modal.hidden = false;
        cancelBtn.focus();

        function finish(result) {
            modal.hidden = true;
            box.classList.remove("is-shaking");

            okBtn.removeEventListener("click", onOk);
            cancelBtn.removeEventListener("click", onCancel);
            modal.removeEventListener("click", onOverlayClick);
            document.removeEventListener("keydown", onKeydown, true);
            box.removeEventListener("animationend", onAnimationEnd);

            if (previousFocus && previousFocus.isConnected) {
                previousFocus.focus();
            }

            resolve(result);
        }

        function onOk() {
            finish(true);
        }

        function onCancel() {
            finish(false);
        }

        function onOverlayClick(event) {
            if (event.target !== modal) return;

            // 重新觸發震動動畫
            box.classList.remove("is-shaking");
            void box.offsetWidth;
            box.classList.add("is-shaking");
        }

        function onAnimationEnd() {
            box.classList.remove("is-shaking");
        }

        function onKeydown(event) {
            if (event.key === "Escape") {
                event.preventDefault();
                event.stopImmediatePropagation();
                return;
            }

            // 防止 Tab 移動到背景頁面
            if (event.key === "Tab") {
                event.preventDefault();
                if (document.activeElement === cancelBtn) {
                    okBtn.focus();
                } else {
                    cancelBtn.focus();
                }
            }
        }

        okBtn.addEventListener("click", onOk);
        cancelBtn.addEventListener("click", onCancel);
        modal.addEventListener("click", onOverlayClick);
        document.addEventListener("keydown", onKeydown, true);
        box.addEventListener("animationend", onAnimationEnd);
    });
}



/* ===== 員工權限清單 ===== */
const EMPLOYEE_PERMISSIONS = [
    { no: 1, name: "訂單管理_內場" },
    { no: 2, name: "訂單管理_外場" },
    { no: 3, name: "入座管理" },
    { no: 4, name: "訂候位管理" },
    { no: 5, name: "訂單管理" },
    { no: 6, name: "結帳" },
    { no: 7, name: "後臺管理" }
];


let employeeSearch = { name: "", no: "" };

function matchesEmployeeSearch(employee, criteria) {
    return (!criteria.name || String(employee.empName ?? "").includes(criteria.name))
        && (!criteria.no || Number(employee.empNo) === Number(criteria.no));
}

document.addEventListener("DOMContentLoaded", loadEmployees);

async function loadEmployees() {
    const tbody = document.getElementById("employeeTableBody");

    try {
        const response = await fetch("/api/employees");

        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }

        const allEmployees = await response.json();
        const employees = allEmployees.filter(employee => matchesEmployeeSearch(employee, employeeSearch));
        tbody.replaceChildren();

        if (employees.length === 0) {
            const row = tbody.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 7;
            cell.textContent = employeeSearch.name || employeeSearch.no
                ? "查無符合條件的員工" : "目前沒有員工資料";
            return;
        }


        employees.forEach(emp => {
            const row = tbody.insertRow();

            // 員工照片
            const photoCell = row.insertCell();
            const photo = document.createElement("img");

            photo.className = "employee-list-photo";
            photo.alt = "員工照片";
            photo.src = `/api/employees/${emp.empNo}/photo`;

            // 沒有照片時顯示預設頭像
            photo.onerror = () => {
                photo.onerror = null;
                photo.src = "/images/employee-default.svg";
            };

            photoCell.appendChild(photo);

            // 員工基本資料
            const values = [
                emp.empNo ?? "",
                emp.empName ?? "",
                emp.empTel ?? "",
                emp.empSal ?? "",
                emp.empStatus === "1" || emp.empStatus === "在職"
                    ? "在職"
                    : emp.empStatus === "2"
                        ? "停用"
                        : (emp.empStatus ?? "未設定")
            ];

            values.forEach(value => {
                const cell = row.insertCell();
                cell.textContent = String(value);
            });

            // 操作按鈕
            const actionCell = row.insertCell();
            actionCell.className = "employee-actions";

            const editBtn = document.createElement("button");
            editBtn.type = "button";
            editBtn.textContent = "編輯";
            editBtn.className = "employee-action-btn";
            editBtn.dataset.action = "edit";
            editBtn.dataset.empNo = emp.empNo;

            const permissionBtn = document.createElement("button");
            permissionBtn.type = "button";
            permissionBtn.textContent = "權限";
            permissionBtn.className = "employee-action-btn";
            permissionBtn.dataset.action = "permissions";
            permissionBtn.dataset.empNo = emp.empNo;

            const disableBtn = document.createElement("button");
            disableBtn.type = "button";
            disableBtn.textContent = "停用";
            disableBtn.className = "employee-action-btn employee-danger-btn";
			disableBtn.dataset.action = "disable";
			disableBtn.dataset.empNo = emp.empNo;
			disableBtn.dataset.empName = emp.empName ?? "";

            // 判斷員工是否已停用
            const isDisabled = emp.empStatus === "2";

            if (isDisabled) {

                // 停用員工不可編輯資料或修改權限
                editBtn.disabled = true;
                permissionBtn.disabled = true;

                // 將停用按鈕改為取消停用
                disableBtn.textContent = "取消停用";
                disableBtn.dataset.action = "enable";

            } else {

                // 在職員工維持原本功能
                disableBtn.textContent = "停用";
                disableBtn.dataset.action = "disable";
            }

            actionCell.append(editBtn, permissionBtn, disableBtn);
        });


    } catch (error) {
        console.error("員工資料載入失敗：", error);
        tbody.replaceChildren();

        const row = tbody.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 7;
        cell.textContent = "員工資料載入失敗，請確認 API 是否正常";
    }
}



/* ===== 查詢員工功能 ===== */
document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("employeeSearchModal");
    const box = modal.querySelector(".employee-modal-content");
    const form = document.getElementById("employeeSearchForm");
    const openBtn = document.getElementById("searchEmployeeBtn");
    const nameInput = document.getElementById("searchEmpName");
    const noInput = document.getElementById("searchEmpNo");

    function closeSearch() {
        modal.hidden = true;
        box.classList.remove("is-shaking");
        openBtn.focus();
    }

    openBtn.addEventListener("click", () => {
        nameInput.value = employeeSearch.name;
        noInput.value = employeeSearch.no;
        modal.hidden = false;
        nameInput.focus();
    });
    document.getElementById("closeEmployeeSearchModal").addEventListener("click", closeSearch);
    document.getElementById("cancelEmployeeSearchBtn").addEventListener("click", closeSearch);
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
        event.preventDefault();
        const name = nameInput.value.trim();
        const no = noInput.value.trim();
        if (!name && !no) {
            staffToast("請輸入員工姓名或員工編號");
            nameInput.focus();
            return;
        }
        employeeSearch = { name, no };
        closeSearch();
        loadEmployees();
    });
    document.getElementById("resetEmployeeSearchBtn").addEventListener("click", () => {
        employeeSearch = { name: "", no: "" };
        form.reset();
        loadEmployees();
    });
});

/* ===== 新增員工功能 ===== */

document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("employeeModal");
    const form = document.getElementById("employeeForm");
    const photoInput = document.getElementById("empPhoto");
    const photoPreview = document.getElementById("employeePhotoPreview");
	
	const deletePhotoBtn = document.getElementById("deleteEmployeePhotoBtn");
	const deletePhotoInput = document.getElementById("deleteEmployeePhoto");

	// 點擊刪除照片：先切換成預設灰色人像
	deletePhotoBtn.addEventListener("click", () => {
	    // 清除目前選擇的照片
	    photoInput.value = "";

	    // 記錄使用者要求刪除照片
	    deletePhotoInput.value = "true";

	    // 顯示預設灰色人像
	    photoPreview.onerror = null;
	    photoPreview.src = "/images/employee-default.svg";
	    photoPreview.hidden = false;
	});

    const openBtn = document.getElementById("addEmployeeBtn");
    const closeBtn = document.getElementById("closeEmployeeModal");
    const cancelBtn = document.getElementById("cancelEmployeeBtn");
    const modalTitle = document.getElementById("employeeModalTitle");
    const tbody = document.getElementById("employeeTableBody");

    // null 代表新增，數字代表編輯指定員工
    let editingEmpNo = null;

    // 開啟新增員工視窗
    openBtn.addEventListener("click", () => {
        editingEmpNo = null;
        modalTitle.textContent = "新增員工";

        form.reset();
        photoPreview.hidden = true;
        photoPreview.removeAttribute("src");

        modal.hidden = false;
    });


    // 點擊員工列表的編輯按鈕
    tbody.addEventListener("click", async (event) => {

        const editBtn = event.target.closest(
            'button[data-action="edit"]'
        );

        if (!editBtn) {
            return;
        }

        const empNo = Number(editBtn.dataset.empNo);

        try {
            const response = await fetch(`/api/employees/${empNo}`);

            if (!response.ok) {
                throw new Error("無法取得員工資料");
            }

            const emp = await response.json();

            // 設定目前為編輯模式
            editingEmpNo = empNo;
            modalTitle.textContent = "編輯員工";

            // 帶入原本資料
            form.reset();

            document.getElementById("empName").value = emp.empName ?? "";
            document.getElementById("empTel").value = emp.empTel ?? "";
            document.getElementById("empIce").value = emp.empIce ?? "";
            document.getElementById("empIcetel").value = emp.empIcetel ?? "";
            document.getElementById("empAdd").value = emp.empAdd ?? "";
            document.getElementById("empSal").value = emp.empSal ?? "";

            // 顯示原本照片，沒有照片則顯示預設頭像
            photoPreview.onerror = () => {
                photoPreview.onerror = null;
                photoPreview.src = "/images/employee-default.svg";
            };

            photoPreview.src = `/api/employees/${empNo}/photo`;
            photoPreview.hidden = false;

            modal.hidden = false;

        } catch (error) {
            console.error("讀取員工資料失敗：", error);
            staffToast("讀取員工資料失敗，請稍後再試");
        }
    });


    // 點擊權限按鈕，開啟員工權限設定視窗
    tbody.addEventListener("click", async (event) => {
        const permissionBtn = event.target.closest(
            'button[data-action="permissions"]'
        );

        if (!permissionBtn) {
            return;
        }

        const empNo = Number(permissionBtn.dataset.empNo);
        const permissionModal = document.getElementById("employeePermissionModal");
        const permissionTitle = document.getElementById("employeePermissionTitle");
        const permissionList = document.getElementById("employeePermissionList");

        permissionList.textContent = "正在載入權限資料...";
        permissionTitle.textContent = `員工 ${empNo} 權限設定`;

        try {
            const response = await fetch(
                `/api/employees/${empNo}/permissions`
            );

			if (response.status === 403) {
			    staffToast("權限不足");
			    return;
			}

			if (response.status === 401) {
			    staffToast("請先登入");
			    return;
			}

			if (!response.ok) {
			    throw new Error("HTTP " + response.status);
			}

            const employeePermissions = await response.json();
            const selectedPermissions = new Set(employeePermissions.map(Number));

            permissionList.replaceChildren();

            EMPLOYEE_PERMISSIONS.forEach(permission => {
                const label = document.createElement("label");
                label.className = "employee-permission-item";

                const checkbox = document.createElement("input");
                checkbox.type = "checkbox";
                checkbox.name = "permissions";
                checkbox.value = String(permission.no);
                checkbox.checked = selectedPermissions.has(permission.no);

                label.append(checkbox, document.createTextNode(" " + permission.name));
                permissionList.appendChild(label);
            });

            permissionModal.dataset.empNo = String(empNo);
            permissionModal.hidden = false;

        } catch (error) {
            console.error("讀取員工權限失敗：", error);
            staffToast("讀取員工權限失敗，請稍後再試");
        }
    });


    // 點擊停用員工按鈕
    tbody.addEventListener("click", async (event) => {
        const disableBtn = event.target.closest(
            'button[data-action="disable"]'
        );

        if (!disableBtn || disableBtn.disabled) {
            return;
        }



		const empNo = Number(disableBtn.dataset.empNo);
		const empName = disableBtn.dataset.empName;

		if (!await employeeConfirm(
		    `確定要停用員工 ${empNo}－${empName} 嗎？`
		)) {
		    return;
		}

        disableBtn.disabled = true;

        try {
            const response = await fetch(
                `/api/employees/${empNo}/disable`,
                { method: "PATCH" }
            );

            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }

            staffToast("員工停用成功！");
            await loadEmployees();

        } catch (error) {
            console.error("停用員工失敗：", error);
            staffToast("停用員工失敗，請稍後再試");
            disableBtn.disabled = false;
        }
    });

    // 點擊取消停用按鈕，恢復員工在職
    tbody.addEventListener("click", async (event) => {

        const enableBtn = event.target.closest(
            'button[data-action="enable"]'
        );

        if (!enableBtn || enableBtn.disabled) {
            return;
        }

		const empNo = Number(enableBtn.dataset.empNo);
		const empName = enableBtn.dataset.empName;

		if (!await employeeConfirm(
		    `確定要取消停用員工 ${empNo}－${empName}，恢復在職嗎？`
		)) {
		    return;
		}

        enableBtn.disabled = true;

        try {
            const response = await fetch(
                `/api/employees/${empNo}/enable`,
                { method: "PATCH" }
            );

            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }

            staffToast("員工已恢復在職！");
            await loadEmployees();

        } catch (error) {
            console.error("取消停用員工失敗：", error);
            staffToast("取消停用員工失敗，請稍後再試");
            enableBtn.disabled = false;
        }
    });

    // 儲存員工權限
    const permissionForm = document.getElementById("employeePermissionForm");

    permissionForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const permissionModal = document.getElementById("employeePermissionModal");
        const empNo = Number(permissionModal.dataset.empNo);

        const selectedPermissions = Array.from(
            permissionForm.querySelectorAll('input[name="permissions"]:checked')
        ).map(checkbox => Number(checkbox.value));

        const submitBtn = permissionForm.querySelector('button[type="submit"]');
        submitBtn.disabled = true;

        try {
            const response = await fetch(
                `/api/employees/${empNo}/permissions`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(selectedPermissions)
                }
            );

			if (response.status === 403) {
			    staffToast("權限不足");
			    return;
			}

			if (response.status === 401) {
			    staffToast("請先登入");
			    return;
			}

			if (!response.ok) {
			    throw new Error("HTTP " + response.status);
			}
            staffToast("員工權限修改成功！");
            permissionModal.hidden = true;

        } catch (error) {
            console.error("儲存員工權限失敗：", error);
            staffToast("儲存員工權限失敗，請稍後再試");

        } finally {
            submitBtn.disabled = false;
        }
    });


    // 關閉員工權限設定視窗
    const permissionModal = document.getElementById("employeePermissionModal");
    const closePermissionBtn = document.getElementById("closePermissionModal");
    const cancelPermissionBtn = document.getElementById("cancelPermissionBtn");

	function closePermissionModal() {
	    permissionModal.hidden = true;
	    permissionModal.querySelector(".employee-modal-content")
	        .classList.remove("is-shaking");
	}
    closePermissionBtn.addEventListener("click", closePermissionModal);
    cancelPermissionBtn.addEventListener("click", closePermissionModal);

	// 點擊權限視窗外側時，震動但不關閉
	permissionModal.addEventListener("click", (event) => {
	    if (event.target === permissionModal) {
	        const box = permissionModal.querySelector(".employee-modal-content");

	        box.classList.remove("is-shaking");
	        void box.offsetWidth;
	        box.classList.add("is-shaking");
	    }
	});


    // 關閉視窗
	function closeModal() {
	    modal.hidden = true;
	    modal.querySelector(".employee-modal-content")
	        .classList.remove("is-shaking");

	    photoPreview.hidden = true;
	    photoPreview.removeAttribute("src");
	}

    closeBtn.addEventListener("click", closeModal);
    cancelBtn.addEventListener("click", closeModal);

	// 點擊背景時，視窗震動但不關閉
	modal.addEventListener("click", (event) => {
	    if (event.target === modal) {
	        const box = modal.querySelector(".employee-modal-content");

	        box.classList.remove("is-shaking");
	        void box.offsetWidth;
	        box.classList.add("is-shaking");
	    }
	});
	
	// ===== 新增、編輯、權限視窗：按 Esc 關閉 =====
	document.addEventListener("keydown", (event) => {
	    if (event.key !== "Escape") {
	        return;
	    }

	    // 權限設定視窗
	    if (!permissionModal.hidden) {
	        event.preventDefault();
	        closePermissionModal();
	        return;
	    }

	    // 新增／編輯員工視窗
	    if (!modal.hidden) {
	        event.preventDefault();
	        closeModal();
	    }
	});

	// 選擇照片後預覽
	photoInput.addEventListener("change", () => {
	    const file = photoInput.files[0];

	    // 重新選擇照片時，取消刪除狀態
	    if (file) {
	        deletePhotoInput.value = "false";
	    }

	    if (!file) {
            photoPreview.hidden = true;
            photoPreview.removeAttribute("src");
            return;
        }

        const allowedTypes = ["image/jpeg", "image/png", "image/webp"];

        if (!allowedTypes.includes(file.type) || file.size > 5 * 1024 * 1024) {
            staffToast("請選擇 JPG、PNG、WebP，且小於 5 MB 的照片");
            photoInput.value = "";
            photoPreview.hidden = true;
            photoPreview.removeAttribute("src");
            return;
        }

        const reader = new FileReader();
        reader.onload = () => {
            photoPreview.src = reader.result;
            photoPreview.hidden = false;
        };
        reader.readAsDataURL(file);
    });

    // 送出新增員工
    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const submitBtn = form.querySelector('button[type="submit"]');
        submitBtn.disabled = true;

        const employee = {
            empName: document.getElementById("empName").value.trim(),
            empTel: document.getElementById("empTel").value.trim(),
            empIce: document.getElementById("empIce").value.trim(),
            empIcetel: document.getElementById("empIcetel").value.trim(),
            empAdd: document.getElementById("empAdd").value.trim(),
            empSal: Number(document.getElementById("empSal").value)
        };

        let createdEmpNo = null;

        try {
            // 1. 判斷新增或修改員工
            const isEditing = editingEmpNo !== null;

            const response = await fetch(
                isEditing
                    ? `/api/employees/${editingEmpNo}`
                    : "/api/employees",
                {
                    method: isEditing ? "PUT" : "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(employee)
                }
            );

            if (!response.ok) {
                throw new Error(
                    (isEditing ? "修改" : "新增") +
                    "員工失敗，HTTP " + response.status
                );
            }

            // 新增時取得新編號，修改時沿用原編號
            createdEmpNo = isEditing
                ? editingEmpNo
                : await response.json();


            // 2. 如果有選擇照片，再上傳照片
			// 2. 處理員工照片：上傳或刪除
			const file = photoInput.files[0];
			const shouldDeletePhoto = deletePhotoInput.value === "true";

			if (file) {

			    // 有選擇新照片：上傳或更換照片
			    const formData = new FormData();
			    formData.append("photo", file);

			    const photoResponse = await fetch(
			        `/api/employees/${createdEmpNo}/photo`,
			        {
			            method: "POST",
			            body: formData
			        }
			    );

			    if (!photoResponse.ok) {
			        throw new Error("員工資料已儲存，但照片上傳失敗");
			    }

			} else if (isEditing && shouldDeletePhoto) {

			    // 編輯員工時有點擊刪除照片：呼叫 DELETE API
			    const photoResponse = await fetch(
			        `/api/employees/${createdEmpNo}/photo`,
			        {
			            method: "DELETE"
			        }
			    );

			    if (!photoResponse.ok) {
			        throw new Error("員工資料已儲存，但照片刪除失敗");
			    }
			}

			// 統一使用 Bistroops 後台 Toast
			staffToast(
			    isEditing ? "✓ 員工修改成功" : "✓ 員工新增成功"
			);
            closeModal();
            await loadEmployees();



        } catch (error) {
            console.error("員工資料儲存失敗：", error);

            if (createdEmpNo !== null) {
				staffToast(
				    `員工 ${createdEmpNo} 的基本資料已儲存，` +
				    `但後續操作失敗：${error.message}`
				);
                closeModal();
                await loadEmployees();
            } else {
				staffToast(
				    (editingEmpNo !== null ? "修改" : "新增") +
				    "員工失敗，請檢查資料或後端服務"
				);
            }

        } finally {
            submitBtn.disabled = false;
        }
    });
});



