# 員工 Email 與臨時密碼：手動啟用說明

程式與 `createTable_v1_6.sql` 已調整；沒有連線修改資料庫。
建表檔已加入兩個欄位及五筆 Test1234 初始密碼；假資料信箱暫為 NULL，請補上真實信箱。
`spring.jpa.hibernate.ddl-auto=none` 明確停用 Hibernate 自動變更表格。
請先完成以下資料庫調整，再啟動新版程式；否則員工查詢／登入會因缺少欄位而失敗。

## 1. 現有資料庫增加欄位

以下 SQL 僅供你手動執行，本次未執行。已加過的欄位不需重複新增。

```sql
ALTER TABLE employee
  ADD COLUMN emp_email VARCHAR(254) NULL COMMENT '員工通知信箱' AFTER emp_tel,
  ADD COLUMN emp_temp_password_expires_at DATETIME(6) NULL
    COMMENT '臨時密碼到期時間；既有測試帳號可為NULL' AFTER emp_pwd_changed;
```

既有 `emp_pwd_changed` 欄位繼續使用，不必重建。新臨時密碼有效 24 小時；
修改為正式密碼後期限清空。既有假資料若期限為 NULL，仍可使用初始密碼登入。

## 2. 五位假資料設定 Test1234

下列雜湊已用專案的 BCrypt 驗證對應 `Test1234`。
先確認員工編號與姓名，再手動更新，避免影響其他正式帳號：

```sql
SELECT emp_no, emp_name, emp_status FROM employee WHERE emp_no IN (1, 2, 3, 4, 5);

UPDATE employee
SET emp_password = '{bcrypt}$2a$10$g2db.AEZVThNjtzx7H4Wn.yZ8Hd1.M3T6FtcxfVuEK5XR7sTFPjNK',
    emp_pwd_changed = FALSE,
    emp_temp_password_expires_at = NULL
WHERE (emp_no, emp_name) IN (
  (1, '陳小心'), (2, 'カイル'), (3, '張學長'), (4, '李木子'), (5, '強滾滾')
);
```

五位員工各自補上你能收信的 Email。測試時可使用同一個你掌控的信箱；
員工編號仍是登入帳號，Email 不作為登入帳號。

```sql
-- 請將右側範例換成真實可收信的信箱，勿直接使用 example.invalid。
UPDATE employee SET emp_email = 'your-address@example.invalid' WHERE emp_no = 1;
UPDATE employee SET emp_email = 'your-address@example.invalid' WHERE emp_no = 2;
UPDATE employee SET emp_email = 'your-address@example.invalid' WHERE emp_no = 3;
UPDATE employee SET emp_email = 'your-address@example.invalid' WHERE emp_no = 4;
UPDATE employee SET emp_email = 'your-address@example.invalid' WHERE emp_no = 5;
```

保留員工在職狀態與權限不變。張學長仍是停用，不能登入；
初始主管需保有後台管理權限（7）。在職員工以編號與 `Test1234` 登入後強制改密碼。

若之後重建資料庫，原建表 SQL 的欄位與 INSERT 已同步更新；
請將五筆假資料的 `emp_email` 從 NULL 改為真實測試信箱。
共用密碼僅限這五筆測試資料，畫面新增員工不使用 Test1234。

## 3. 寄信設定

Gmail 的主機、587 STARTTLS 連線已設為預設值，寄件地址預設與寄件帳號相同。
在 Eclipse 啟動設定的 Environment 只需先填入兩個環境變數：
`EMPLOYEE_MAIL_USERNAME`（寄件 Gmail）及 `EMPLOYEE_MAIL_PASSWORD`（Google 應用程式密碼）。
密碼直接在本機填入，勿貼到聊天或提交到 Git；程式會移除 Gmail 應用程式密碼中的分隔空白。

完整設定如下，其餘項目可視需要覆寫：

| 變數 | 內容 |
| --- | --- |
| EMPLOYEE_MAIL_HOST | 預設 smtp.gmail.com，不必另外填寫 |
| EMPLOYEE_MAIL_PORT | STARTTLS 通常 587；隱式 TLS 通常 465 |
| EMPLOYEE_MAIL_USERNAME | SMTP 登入帳號 |
| EMPLOYEE_MAIL_PASSWORD | SMTP 專用密碼或應用程式密碼 |
| EMPLOYEE_MAIL_FROM | 預設同 EMPLOYEE_MAIL_USERNAME，不必另外填寫 |
| EMPLOYEE_LOGIN_URL | 本機預設為 http://localhost:8080/staff/login |

不要將寄信密碼提交到 Git。設定完成後重新啟動專案。
本機網址僅能從執行專案的同一台電腦使用；部署後修改 EMPLOYEE_LOGIN_URL。
程式沿用專案既有 JavaMail 依賴，不需額外修改 pom.xml。

## 4. 功能與驗收

- 新增員工必填 Email。後端以 SecureRandom 產生 12 碼英數臨時密碼，
  僅保存 BCrypt 雜湊，交易提交成功後才寄信；API 不回傳明文密碼。
- 信中含員工登入網址、員工編號、臨時密碼與 24 小時期限說明。
- 編輯 Email 不會自動重設密碼或寄信。先儲存新 Email，再按左下角「忘記密碼」。
- 忘記密碼會立即使舊密碼失效，產生新密碼並寄到資料庫已儲存的 Email。
  每次重新寄送都會重設，只能使用最後一次產生的密碼。
- 若寄信失敗，帳號與新密碼仍已儲存。列表上方會保留失敗說明；
  修正 Email 或 SMTP 設定後，再按「忘記密碼」重新寄送，不要重複新增員工。
- 尚未設定 SMTP 時，程式不寄信，會回報通知失敗；五位已設定的測試帳號仍可登入。

程式驗證包含編譯、服務單元測試與前端操作邏輯檢查。
本次未實際寄送信件、未連線修改資料庫，也未驗證真實收信；
手動調整完成後，請測試收信、首次改密碼、重設與寄送失敗後重新寄送。
