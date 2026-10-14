
/* ===== 員工權限清單 ===== */
const EMPLOYEE_PERMISSIONS = [
    { no: 1, name: "員工管理" },
    { no: 2, name: "會員管理" },
    { no: 3, name: "菜單管理" },
    { no: 4, name: "活動管理" },
    { no: 5, name: "公告設定" },
    { no: 6, name: "桌型管理" },
    { no: 7, name: "入座管理" },
    { no: 8, name: "訂單管理_廚師" },
    { no: 9, name: "訂單管理_菜口" },
    { no: 10, name: "訂單管理_外場" },
    { no: 11, name: "結帳" },
    { no: 12, name: "預約管理" },
    { no: 13, name: "候位管理" },
    { no: 14, name: "回饋表單管理" }
];


document.addEventListener("DOMContentLoaded", loadEmployees);

async function loadEmployees() {
    const tbody = document.getElementById("employeeTableBody");

    try {
        const response = await fetch("/api/employees");

        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }

        const employees = await response.json();
        tbody.replaceChildren();

        if (employees.length === 0) {
            const row = tbody.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 7;
            cell.textContent = "目前沒有員工資料";
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
            alert("讀取員工資料失敗，請稍後再試");
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
            alert("讀取員工權限失敗，請稍後再試");
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

        if (!confirm(`確定要停用員工 ${empNo} 嗎？`)) {
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

            alert("員工停用成功！");
            await loadEmployees();

        } catch (error) {
            console.error("停用員工失敗：", error);
            alert("停用員工失敗，請稍後再試");
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

        if (!confirm(`確定要取消停用員工 ${empNo}，恢復在職嗎？`)) {
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

            alert("員工已恢復在職！");
            await loadEmployees();

        } catch (error) {
            console.error("取消停用員工失敗：", error);
            alert("取消停用員工失敗，請稍後再試");
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

            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }

            alert("員工權限修改成功！");
            permissionModal.hidden = true;

        } catch (error) {
            console.error("儲存員工權限失敗：", error);
            alert("儲存員工權限失敗，請稍後再試");

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
    }

    closePermissionBtn.addEventListener("click", closePermissionModal);
    cancelPermissionBtn.addEventListener("click", closePermissionModal);

    permissionModal.addEventListener("click", (event) => {
        if (event.target === permissionModal) {
            closePermissionModal();
        }
    });


    // 關閉視窗
    function closeModal() {
        modal.hidden = true;
        photoPreview.hidden = true;
        photoPreview.removeAttribute("src");
    }

    closeBtn.addEventListener("click", closeModal);
    cancelBtn.addEventListener("click", closeModal);

    // 點擊視窗外側關閉
    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
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
            alert("請選擇 JPG、PNG、WebP，且小於 5 MB 的照片");
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

			// 顯示 Bistroops 後台成功提示
			const toast = document.createElement("div");
			toast.className = "staff-toast";
			toast.textContent = isEditing ? "✓ 員工修改成功" : "✓ 員工新增成功";

			document.body.appendChild(toast);

			// 淡入動畫
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
            closeModal();
            await loadEmployees();



        } catch (error) {
            console.error("員工資料儲存失敗：", error);

            if (createdEmpNo !== null) {
                alert(
                    `員工 ${createdEmpNo} 的基本資料已儲存，` +
                    `但後續操作失敗：${error.message}`
                );
                closeModal();
                await loadEmployees();
            } else {
                alert(
                    (editingEmpNo !== null ? "修改" : "新增") +
                    "員工失敗，請檢查資料或後端服務"
                );
            }

        } finally {
            submitBtn.disabled = false;
        }
    });
});



