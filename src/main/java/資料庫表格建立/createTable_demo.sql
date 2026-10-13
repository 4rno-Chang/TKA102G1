/* =========================================================
   示範／測試用的建表 SQL（正式時段 ＋ 現在附近的時段：任何時間 demo 都看得到需保留、桌數不夠）
   = createTable_v1_4.sql（G1 原版，56c40fe；v1_4 最後修改 1286fd1）＋ 05 新增的欄位 ＋ 測試資料（19～27，壓力測試版）
   和原版一樣：刪掉整個資料庫 → 建表 → 新增資料 → 測試資料 → 加外鍵，可以重複執行；ALTER TABLE 只用來加外鍵
   05 新增的欄位直接寫在 CREATE TABLE 裡（旁邊註明「G1 v1_4 沒有，05 新增」）：
     qa.qa_clicks（常見問題點擊次數）
     waiting.waiting_date、waiting_take_time、waiting_ticket_seq（CJ 的 WaitingVO 有，v1_4 還沒有）
   19～27 的時間從「執行的當下」推算：整天任何時間都能執行，執行完馬上測（預期結果寫在 19 段上方）
   權限、員工照 G1：員工 1（陳小心，密碼 0987878787）有 1～7 全部權限，3 = 入座管理
   要回到 G1 原版資料：重跑 createTable_v1_4.sql（但 v1_4 沒有 qa_clicks，首頁常見問題會出錯，05 請用這一份）
   ========================================================= */
/*SHOW DATABASES;*/
DROP DATABASE IF EXISTS project;
CREATE DATABASE IF NOT EXISTS project;
USE project;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS orders_details;
DROP TABLE IF EXISTS meal;
DROP TABLE IF EXISTS meal_type;
DROP TABLE IF EXISTS promote;
DROP TABLE IF EXISTS promote_meal;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS emp_permission;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS seat_type;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS reservation_datetime;
DROP TABLE IF EXISTS waiting;
DROP TABLE IF EXISTS seat;
DROP TABLE IF EXISTS member;
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS qa;
DROP TABLE IF EXISTS announcement;

-- 訂單, orders
CREATE TABLE IF NOT EXISTS orders(
  orders_no INT UNSIGNED AUTO_INCREMENT COMMENT '訂單編號',
  seat_no CHAR(3) NOT NULL COMMENT '桌號',
  member_no INT UNSIGNED COMMENT '會員編號',
  orders_time DATETIME COMMENT '下單時間',
  orders_total INT COMMENT '總金額',
  orders_discount_total INT COMMENT '折扣金額',
  orders_actual_price INT COMMENT '實付金額',
  orders_pay VARCHAR(2) COMMENT '付款方式',
  PRIMARY KEY(orders_no)
) COMMENT '訂單';

-- 訂單明細, orders_details
CREATE TABLE IF NOT EXISTS orders_details(
	orders_details_no	 INT UNSIGNED AUTO_INCREMENT COMMENT'訂單明細編號',
  orders_no INT UNSIGNED NOT NULL COMMENT '訂單編號',
  meal_no INT UNSIGNED NOT NULL COMMENT '菜品編號',
  promote_no INT UNSIGNED COMMENT '活動編號',
  od_meal_num INT COMMENT '菜品數量',
  od_discount_price INT COMMENT '下單時菜品單價',
  od_discount_total INT COMMENT '折扣金額',
  od_actual_price INT COMMENT '小計',
  od_comment VARCHAR(30) COMMENT '備註',
  od_status VARCHAR(5) COMMENT '訂單狀態',
  od_served_at DATETIME NULL COMMENT'送達時間',
  PRIMARY KEY(orders_details_no)
) COMMENT '訂單明細';

-- 菜品, meal
CREATE TABLE IF NOT EXISTS meal(
  meal_no INT UNSIGNED AUTO_INCREMENT COMMENT '菜品編號',
  meal_type_no INT UNSIGNED COMMENT '類別編號',
  meal_name VARCHAR(255) NOT NULL COMMENT '名稱',
  meal_img MEDIUMBLOB COMMENT '圖片',
  meal_exp MEDIUMTEXT COMMENT '說明',
  meal_price INT NOT NULL COMMENT '價格',
--  meal_status_num ENUM('0', '1') COMMENT '菜品狀態', --
  meal_status_en ENUM('上架', '未上架') COMMENT '菜品狀態',
  PRIMARY KEY(meal_no)
) COMMENT '菜品';

-- 菜品類別, meal_type
CREATE TABLE IF NOT EXISTS meal_type(
  meal_type_no INT UNSIGNED AUTO_INCREMENT COMMENT '類別編號',
  meal_type_name VARCHAR(30) COMMENT '類別名稱',
  PRIMARY KEY(meal_type_no)
) COMMENT '菜品類別';

-- 活動, promote
CREATE TABLE IF NOT EXISTS promote(
  promote_no INT UNSIGNED AUTO_INCREMENT COMMENT '活動編號',
  promote_name VARCHAR(10) COMMENT '活動名稱',
  promote_begin DATETIME COMMENT '開始時間',
  promote_end DATETIME COMMENT '結束時間',
  promote_content MEDIUMTEXT COMMENT '活動內容',
  promote_img MEDIUMBLOB COMMENT '活動圖片',
  PRIMARY KEY(promote_no)
) COMMENT '活動';

-- 活動商品, promote_meal
CREATE TABLE IF NOT EXISTS promote_meal(
  promote_no INT UNSIGNED NOT NULL COMMENT '活動編號',
  meal_no INT UNSIGNED NOT NULL COMMENT '菜品編號',
  promote_discount INT COMMENT '折扣後金額',
  PRIMARY KEY (promote_no, meal_no)
) COMMENT '活動商品';

-- 員工, employee
CREATE TABLE IF NOT EXISTS employee(
  emp_no INT UNSIGNED AUTO_INCREMENT COMMENT '員工編號',
  emp_name VARCHAR(30) NOT NULL COMMENT '姓名',
  emp_password VARCHAR(100) NOT NULL COMMENT '密碼',
  emp_pwd_changed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已完成首次密碼修改',
  emp_tel VARCHAR(10) NOT NULL COMMENT '本人電話',
  emp_ice VARCHAR(30) COMMENT '緊急聯絡人',
  emp_icetel VARCHAR(10) COMMENT '緊急聯絡人電話',
  emp_add VARCHAR(100) COMMENT '地址',
  emp_sal INTEGER NOT NULL COMMENT '月薪',
  emp_status VARCHAR(2) NOT NULL COMMENT '員工狀態：1在職、2停用',
  emp_photo MEDIUMBLOB COMMENT '員工照片',
  PRIMARY KEY (emp_no)
) COMMENT '員工';

-- 員工權限對照, emp_permission
CREATE TABLE IF NOT EXISTS emp_permission(
  emp_no INT UNSIGNED NOT NULL COMMENT '員工編號',
  perm_no INT UNSIGNED NOT NULL COMMENT '權限編號',
  PRIMARY KEY (emp_no, perm_no)
) COMMENT '員工權限';

-- 系統權限, permission
CREATE TABLE IF NOT EXISTS permission(
  perm_no INT UNSIGNED AUTO_INCREMENT NOT NULL COMMENT '權限編號',
  perm_exp VARCHAR(30) NOT NULL COMMENT '權限描述',
  PRIMARY KEY (perm_no)
) COMMENT '系統權限';

-- 桌型, seat_type
CREATE TABLE IF NOT EXISTS seat_type (
  seat_type_no INT NOT NULL COMMENT '桌型編號',
  seat_type_num TINYINT COMMENT '桌數',
  seat_type_rsv_num TINYINT COMMENT '開放訂位桌數',
  seat_type_take_num INT COMMENT '取號',
  seat_type_call_num INT COMMENT '入座號',
  seat_type_pending INT COMMENT '目前可使用',
  PRIMARY KEY (seat_type_no)
) COMMENT '桌型';

-- 訂位, reservation
CREATE TABLE IF NOT EXISTS reservation (
  rsv_no INT UNSIGNED AUTO_INCREMENT COMMENT '訂位編號',
  mem_no INT UNSIGNED NOT NULL COMMENT '會員編號',
  rsv_dt_no INT UNSIGNED NOT NULL COMMENT '訂位時段編號',
  seat_type_no INT NOT NULL COMMENT '桌型編號',
  rsv_create_time DATETIME COMMENT '訂位成立時間',
  rsv_status VARCHAR(2) COMMENT '狀態',
  rsv_comment VARCHAR(50) COMMENT '備註',
  PRIMARY KEY (rsv_no)
) COMMENT '訂位';

-- 訂位時段, reservation_datetime
CREATE TABLE IF NOT EXISTS reservation_datetime (
  rsv_dt_no INT UNSIGNED AUTO_INCREMENT COMMENT '訂位時段編號',
  rsv_dt_datetime DATETIME COMMENT '訂位時段',
  PRIMARY KEY (rsv_dt_no)
) COMMENT '訂位時段';

-- 候位, waiting
CREATE TABLE IF NOT EXISTS waiting (
  waiting_no INT UNSIGNED AUTO_INCREMENT COMMENT '候位編號',
  seat_type_no INT NOT NULL COMMENT '桌型編號',
  mem_no INT UNSIGNED COMMENT '會員編號',
  waiting_tel CHAR(10) NOT NULL COMMENT '電話',
  waiting_name VARCHAR(30) COMMENT '姓名',
  waiting_comment VARCHAR(20) COMMENT '備註',
  waiting_status VARCHAR(3) COMMENT '狀態',
  waiting_notify_time TIME COMMENT '通知時間',
  waiting_date DATE NULL COMMENT '候位日期',                     -- G1 v1_4 沒有，05 新增（CJ 的 WaitingVO 有這三個欄位）
  waiting_take_time DATETIME NULL COMMENT '抽號時間',            -- 同上
  waiting_ticket_seq INT NULL COMMENT '當天這個桌型的號碼',      -- 同上
  PRIMARY KEY (waiting_no)
) COMMENT = '候位';

-- 桌位, seat
CREATE TABLE IF NOT EXISTS seat (
  seat_no CHAR(3) NOT NULL COMMENT '桌號',
  seat_type_no INT NOT NULL COMMENT '桌型編號',
  seat_status VARCHAR(5) COMMENT '桌位狀態',
  seat_time TIME COMMENT '入座時間',
  PRIMARY KEY (seat_no)
) COMMENT = '桌位';

-- 會員, member
CREATE TABLE IF NOT EXISTS member (
  mem_no INT UNSIGNED AUTO_INCREMENT COMMENT '會員編號',
  mem_tel CHAR(10) NOT NULL COMMENT '電話',
  mem_name VARCHAR(30) COMMENT '姓名',
  mem_account VARCHAR(16) COMMENT '帳號',
  mem_password VARCHAR(100) COMMENT '密碼',
  mem_mail VARCHAR(30) COMMENT 'Email',
  mem_barcode CHAR(8) COMMENT '載具條碼',
  mem_tag VARCHAR(50) COMMENT '客戶備註',
  mem_birth DATE COMMENT '生日',
  mem_fb VARCHAR(32) COMMENT '臉書',
  mem_line VARCHAR(32) COMMENT 'Line',
  mem_goo VARCHAR(32) COMMENT 'Google',
  mem_x VARCHAR(32) COMMENT 'X(推特)',
  mem_status VARCHAR(3) COMMENT '會員狀態',
  PRIMARY KEY(mem_no)
) COMMENT '會員';

-- 回饋, feedback
CREATE TABLE IF NOT EXISTS feedback(
  feedback_no INT UNSIGNED AUTO_INCREMENT COMMENT '回饋編號',
  orders_no INT UNSIGNED COMMENT '訂單編號',
  fee_customer VARCHAR(30) COMMENT '回饋人',
  fee_customer_tel CHAR(10) COMMENT '回饋人電話',
  fee_customer_time DATETIME COMMENT '用餐時間點',
  fee_time DATETIME COMMENT '回饋時間',
  fee_content VARCHAR(255) COMMENT '回饋內容',
  fee_rating TINYINT COMMENT '評分',
  PRIMARY KEY(feedback_no)
) COMMENT '回饋';

-- 常見問題, qa
CREATE TABLE IF NOT EXISTS qa(
  qa_no INT UNSIGNED AUTO_INCREMENT COMMENT '編號',
  qa_title VARCHAR(30) COMMENT '標題',
  qa_content MEDIUMTEXT COMMENT '內容',
  qa_clicks INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '點擊次數',   -- G1 v1_4 沒有，05 新增（首頁常見問題顯示點擊最多的 5 題）
  PRIMARY KEY(qa_no)
) COMMENT '常見問題';

-- 推播訊息, announcement
CREATE TABLE IF NOT EXISTS announcement(
  ann_no INT UNSIGNED AUTO_INCREMENT COMMENT '編號',
  ann_title VARCHAR(30) COMMENT '標題',
  ann_begin DATETIME COMMENT '公告時間',
  ann_img MEDIUMBLOB COMMENT '推播圖',
  ann_text MEDIUMTEXT COMMENT '文字內容',
  PRIMARY KEY(ann_no)
) COMMENT '推播訊息';


/*假資料, 測試資料*/
-- 1. 插入義式料理類別 (meal_type)
INSERT INTO meal_type (meal_type_name) VALUES 
('主打推薦'),
('開胃菜'),
('義大利麵'),
('燉飯'),
('經典排餐'),
('甜點'),
('飲料');


-- 2. 插入義式菜品資料 (meal)
INSERT INTO meal (meal_type_no, meal_name, meal_exp, meal_price, meal_status_en) VALUES
(2, '義式烤麵包 Bruschetta', '新鮮番茄粒搭配羅勒與初榨橄欖油', 160, '上架'),
(2, '酥炸墨魚圈', '附塔塔醬與新鮮黃檸檬塊', 220, '上架'),
(2, '義式卡布里沙拉', '莫札瑞拉起司搭配新鮮番茄與羅勒醬', 200, '上架'),
(2, '蒜味奶油烤野菇', '綜合野菇佐蒜味香草奶油醬（蛋奶素）', 180, '上架'),
(2, '帕瑪火腿哈密瓜卷', '經典鹹甜組合，嚴選帕瑪生火腿搭配甜哈密瓜', 280, '上架'),
(2, '松露起司薯條', '現炸金黃薯條刨上帕瑪森起司佐黑松露醬（蛋奶素）', 190, '上架'),
(3, '牛肝菌野菇義大利麵', '香濃牛肝菌醬汁配現削帕馬森起司', 320, '上架'),
(3, '粉紅醬焗烤筆管麵', '濃郁粉紅醬搭配多種時蔬菜彩（蛋奶素）', 280, '上架'),
(3, '熱那亞羅勒青醬細扁麵', '自製羅勒青醬搭配烤松子與帕瑪森起司（蛋奶素）', 270, '上架'),
(3, '經典波隆那肉醬麵', '慢火燉煮傳統牛肉豬肉綜合番茄肉醬', 300, '上架'),
(3, '奶油卡邦尼培根麵', '經典蛋黃奶油醬搭配香煎培根與黑胡椒', 290, '上架'),
(3, '威尼斯墨魚汁海鮮麵', '特調墨魚醬汁炒鮮蝦、蛤蜊與透抽', 380, '上架'),
(3, '香蒜辣味白酒蛤蜊麵', '新鮮蛤蜊搭配蒜片、乾辣椒與白酒香炒', 320, '上架'),
(4, '經典粉紅醬海鮮燉飯', '濃郁奶油番茄醬汁搭配鮮蝦與干貝', 380, '上架'),
(4, '松露野菇燉飯', '濃郁黑松露醬與綜合野菇義大利燉飯（蛋奶素）', 360, '上架'),
(4, '西西里番茄海鮮燉飯', '鮮甜番茄高湯燉煮多種時令海鮮', 390, '上架'),
(4, '拿坡里牛肝菌菇燉飯', '嚴選義大利牛肝菌菇高湯燉煮（蛋奶素）', 380, '上架'),
(5, '托斯卡尼獵人風燉雞', '慢火燉煮去骨雞腿肉，搭番茄、黑橄欖、彩椒與紅酒醬汁', 420, '上架'),
(5, '巴薩米克櫻桃鴨胸', '宜蘭櫻桃鴨胸香煎至粉紅熟度，淋上巴薩米克黑醋櫻桃醬', 580, '上架'),
(5, '羅馬香草烤豬腳', '外皮香酥大豬腳，搭配自製義式酸菜與粒醬芥末', 620, '上架'),
(5, '西西里香草烤嫩羊肩排', '紐西蘭羊肩排搭配新鮮薄荷青醬與香草麵包粉烘烤', 680, '上架'),
(5, '地中海煎烤海鱸魚排', '新鮮海鱸魚菲力香煎，搭配蒔蘿鮮蝦起司醬與烤時蔬', 520, '上架'),
(5, '波隆那香煎小牛肉佐帕瑪火腿', '嫩小牛肉片疊上帕瑪火腿與鼠尾草，以白酒黃油煎香', 560, '上架'),
(5, '拿坡里香辣奶油大蝦排', '野生大草蝦搭配蒜香辣椒奶油，附烤法式麵包沾醬', 480, '上架'),
(5, '主廚手作黑松露烤大褐菇排', '巨型大褐菇鑲黑松露時蔬內餡，覆蓋莫札瑞拉起司烘烤（蛋奶素）', 380, '上架'),
(5, '米蘭式巴薩米克燉小牛膝', '米蘭百年名菜 Ossobuco，牛膝燉至軟嫩搭義式香草檸檬醬', 720, '未上架'),
(6, '主廚手工提拉米蘇', '濃郁馬斯卡彭起司與浸泡咖啡酒的手指餅乾', 160, '未上架'),
(6, '義式香草奶酪', '附野生莓果醬', 120, '上架'),
(6, '香草熔岩巧克力蛋糕', '切開爆漿熱巧克力佐香草冰淇淋（蛋奶素）', 160, '上架'),
(6, '檸檬塔', '新鮮黃檸檬內餡與香酥塔皮（蛋奶素）', 140, '上架'),
(6, '義式香草冰淇淋', '傳統手工低脂香草冰淇淋雙球（蛋奶素）', 100, '上架'),
(6, '焦糖烤布蕾', '現烤香脆焦糖蓋上濃郁蛋黃布丁（蛋奶素）', 120, '上架'),
(6, '開心果巴斯克乳酪蛋糕', '濃郁開心果醬搭配綿密乳酪蛋糕（蛋奶素）', 170, '上架'),
(7, '綜合野莓派', '酸甜綜合野莓內餡香脆甜派（蛋奶素）', 150, '未上架'),
(7, '西西里檸檬氣泡咖啡', '現粹濃縮咖啡搭配西西里檸檬汁', 130, '未上架'),
(7, '義式濃縮咖啡', '雙份萃取，香濃苦甘', 90, '上架'),
(7, '氣泡礦泉水','原裝進口瓶裝氣泡水', 100, '上架');


-- 3. 桌型 (seat_type)
INSERT INTO seat_type (
    seat_type_no,
    seat_type_num,
    seat_type_rsv_num,
    seat_type_take_num,
    seat_type_call_num,
    seat_type_pending
)
VALUES
    (2, 10, 5, 12, 8, 10),
    (4, 5, 3, 25, 20, 5),
    (6, 2, 1, 8, 5, 2);

-- 4. 桌位 (seat)
INSERT INTO seat (
    seat_no,
    seat_type_no,
    seat_status,
    seat_time
)
VALUES
    ('A01', 2, '可使用', NULL), 
    ('B01', 4, '訂位者使用', '18:30:00'),
    ('C01', 6, '其它者使用', '19:00:00');

-- 5. 會員 (member)
INSERT INTO member (
    mem_tel,
    mem_name,
    mem_account,
    mem_password,
    mem_mail,
    mem_barcode,
    mem_tag,
    mem_birth,
    mem_fb,
    mem_line,
    mem_goo,
    mem_x,
    mem_status
) VALUES
(
    '0912345678',
    '王小明',
    'wangming01',
    '{bcrypt}$2y$10$ESZlY8Y.MzLF766jXF.4CuB9qzoXm0/YDTdSpbQGom60rnTwKKDtO',
    'wangming01@example.com',
    '/AB12345',
    '喜歡靠窗座位',
    '1995-03-15',
    'wang.ming',
    'wangming01',
    'wangming01@gmail.com',
    'wangming01',
    '已驗證'
),
(
    '0985510798',
    '陳小華',
    'chenhua02',
    '{bcrypt}$2y$10$wluGpbXL8u4rOUKBLjjGU.BsiJURH6BK970uv2UlKBByQMx7H2862',
    'chenhua02@example.com',
    '/CD23456',
    '不吃香菜',
    '1998-07-22',
    NULL,
    'chenhua02',
    'chenhua02@gmail.com',
    NULL,
    '已驗證'
),
(
    '0911111111',
    '林小美',
    'linmei03',
    '{bcrypt}$2y$10$nd.4i4UlqHTSVHjsH604CegjjzH3EAPhRBOJyFb.4ox4SPMVwxpBq',
    'linmei03@example.com',
    '/EF34567',
    '生日用餐',
    '2000-11-08',
    'lin.meimei',
    NULL,
    'linmei03@gmail.com',
    'linmei03',
    '未驗證'
); 

-- 6. 插入義餐訂單主檔 (orders)
INSERT INTO orders (seat_no, member_no, orders_time, orders_total, orders_discount_total, orders_actual_price, orders_pay) VALUES 
('A01', 1, '2026-08-24 18:30:00', 820, 50, 770, '刷卡'),
('B01', 2, '2026-08-24 19:15:00', 1180, 100, 1080, '刷卡'),
('C01', 3, '2026-08-24 19:40:00', 430, 0, 430, '現金');

-- 7. 活動 (promote)
INSERT INTO promote (promote_name, promote_begin, promote_end, promote_content) VALUES
('中秋節活動', '2026-09-20 10:00:00', '2026-10-06 22:00:00',  '中秋節限定商品優惠'),
('2026生日禮', '2026-02-15 00:00:00', '2026-02-28 23:59:59', '出示身份證，當月生日享提拉米蘇一份。'),
('情人節套餐', '2026-03-10 11:00:00', '2026-03-15 22:00:00', '雙人浪漫限定套餐。'),
('平日午間優惠', '2027-01-01 00:00:00', '2027-12-31 23:59:59', '週一至週五 11:00–14:00，商業午餐 85 折。'),
('母親節感恩祭', '2026-05-01 00:00:00', '2026-05-15 23:59:59', '帶媽媽來店用餐，拍照打卡即享全單 85 折。'),
('畢業季優惠', '2026-06-01 00:00:00', '2026-06-30 23:59:59', '憑學生證或畢業證書，享滿千送百折價券。'),
('父親節特別獻禮', '2026-08-01 00:00:00', '2026-08-10 23:59:59', '爸爸專屬優惠，提供父親節特別套餐。'),
('中秋慶團圓', '2026-09-15 00:00:00', '2026-09-25 23:59:59', '中秋期間消費滿額，即贈送品牌限量紀念周邊。'),
('秋季溫暖祭', '2026-10-05 10:00:00', '2026-10-20 22:00:00', '點購指定湯品或熱飲，即贈送每日精選甜點乙份。'),
('雙十連假特惠', '2026-10-07 00:00:00', '2026-10-12 23:59:59', '連假期間兩人同行，第二件主餐享半價優惠。'),
('雙十節活動', '2026-10-01 10:00:00', '2026-10-31 22:00:00',  '雙十國慶期間限定活動，指定開胃菜半價'),
('跨年特別企劃', '2026-12-31 18:00:00', '2027-01-01 02:00:00', '跨年夜限定酒精飲品暢飲活動。'),
('春季新菜上市', '2027-03-01 00:00:00', '2027-03-31 23:59:59', '春季新品主餐登場，嘗鮮價享有 85 折優惠。'),
('週年慶活動', '2027-09-01 10:00:00', '2027-09-30 22:00:00',  '週年慶主廚推薦菜品享優惠活動');

-- 8. 活動商品 (promote_meal)
INSERT INTO promote_meal (promote_no, meal_no, promote_discount) VALUES
(1, 1, 120),
(1, 2, 150),
(1, 3, 150);

-- 9. 訂單明細 (orders_details)
-- 狀態統一用：製作中／等待送餐／已送達／已取消；還沒送達的送達時間填 NULL
INSERT INTO orders_details (orders_no, meal_no, promote_no, od_meal_num, od_discount_price, od_discount_total, od_actual_price, od_comment, od_status, od_served_at) VALUES
-- 訂單 1 (A01)：烤麵包套用中秋特價 120
(1,  1,    1, 1, 160, 40, 120, '麵包要烤酥一點', '已送達',   '2026-10-01 18:42:00'),
(1, 14, NULL, 1, 380,  0, 380, '飯軟一點',       '已送達',   '2026-10-01 18:55:00'),
(1, 37, NULL, 1, 100,  0, 100, '少冰',           '已取消',   NULL),
(1, 29, NULL, 1, 160,  0, 160, '',               '等待送餐', NULL),
-- 訂單 2 (B01)：卡布里沙拉套用中秋特價 150
(2,  3,    1, 1, 200, 50, 150, '',               '已送達',   '2026-10-01 19:27:00'),
(2, 21, NULL, 1, 680,  0, 680, '三分熟',         '等待送餐', NULL),
(2, 37, NULL, 1, 100,  0, 100, '加冰塊與檸檬片', '製作中',   NULL),
-- 訂單 3 (C01)：剛點完
(3, 10, NULL, 1, 300,  0, 300, '起司多一點',     '製作中',   NULL),
(3, 37, NULL, 1, 100,  0, 100, '去冰',           '製作中',   NULL);

-- 10. 候位 (waiting)
INSERT INTO waiting (
    seat_type_no,
    mem_no,
    waiting_tel,
    waiting_name,
    waiting_comment,
    waiting_status,
    waiting_notify_time
)
VALUES
    (2, NULL, '0985510798', '王小明', '靠窗座位', '已入座', '18:10:00'),
    (4, NULL, '0985510798', '陳美玲', '4位用餐', '取消', '18:20:00'),
    (6, NULL, '0985510798', '林志豪', '慶生聚餐', '已入座', '18:30:00');

-- 11. 訂位時段 (reservation_datetime)
INSERT INTO reservation_datetime (
    rsv_dt_datetime
)
VALUES
    ('2026-08-25 11:00:00'),
    ('2026-08-25 12:30:00'),
    ('2026-08-25 17:00:00'),
    ('2026-08-25 16:30:00');

-- 12. 訂位 (reservation)
INSERT INTO reservation (
    mem_no,
    rsv_dt_no,
    seat_type_no,
    rsv_create_time,
    rsv_status,
    rsv_comment
)
VALUES
    (1, 1, 2, '2026-08-24 20:40:00', '實到', '2人用餐'),
    (2, 2, 4, '2026-08-24 20:42:00', '實到', '4人家庭聚餐'),
    (3, 3, 6, '2026-08-24 20:45:00', '預約', '6人慶生聚餐');

-- 13. 員工 (employee)
INSERT INTO employee
(emp_name, emp_password, emp_tel, emp_ice,
 emp_icetel, emp_add, emp_sal, emp_status, emp_photo)
VALUES
('陳小心',
 '{bcrypt}$2y$10$6SYYr3G35GKj0Ekhw6UKd.y6sUTmCBp4/jWfykkSo0YHlJ0r1qa9q',
 '0987878787', '', '', '', 30000, '1', NULL),

('カイル',
 '{bcrypt}$2y$10$BEoKQ7peCLE37..771PRgece9Vv/bzU1jQXGoSvGphSzIJz/BJ11S',
 '0911223344', '愛蜜莉雅', '0987654321', '露格尼卡王國', 40000, '1', NULL),

('張學長',
 '{bcrypt}$2y$10$Nxb9YSwhmDWc58oUYd1KWumgnHseCQlDJgaLEMyVWpqprQ.rnuck.',
 '0987654321', '', '', '電腦市程式碼街最強社區666號', 100000, '2', NULL),

('李木子',
 '{bcrypt}$2y$10$OMPcRNLstLX4WZVQne5LcuXvTudI5K89ZRHW7lC5bremWmKZ5tod2',
 '0900111222', '', '', '快樂縣可愛鄉聰明路102號', 40000, '1', NULL),

('強滾滾',
 '{bcrypt}$2y$10$4hmVp4LxpKKQ.A5XlbhnIOTUfu9p4cmY.fGKpBLOJjtAgSR5YmiSK',
 '0933445566', '張學長', '0987654321',
 '電腦市程式碼街最強社區666號', 50000, '1', NULL);

-- 14. 權限 (permission)
INSERT INTO permission (perm_exp) VALUES
('訂單管理_內場'),
('訂單管理_外場'),
('入座管理'),
('訂候位管理'),
('訂單管理'),
('結帳'),
('後臺管理');

-- 15. 員工權限 (emp_permission)
INSERT INTO emp_permission (emp_no, perm_no) VALUES
(1, 1),
(1, 2),
(1, 3),
(1, 4),
(1, 5),
(1, 6),
(1, 7),
(2, 1),
(2, 3);

-- 16. 回饋 (feedback)
INSERT INTO feedback (orders_no, fee_customer, fee_customer_tel, fee_customer_time, fee_time, fee_content, fee_rating) VALUES 
(1, '顧客A', '0985510798', '2026-08-24 19:40:00', '2026-08-25 19:40:00', '餐點非常美味，服務親切！', 10),
(1, '顧客B', '0985510798', '2026-08-25 11:00:00', '2026-08-26 19:40:00', '氣氛很好，會再光顧。', 9),
(1, '顧客C', '0985510798', '2026-08-25 12:30:00', '2026-08-26 19:40:00', '出餐稍微慢了點。', 5),
(2, '顧客D', '0985510798', '2026-08-25 18:30:00', '2026-08-27 19:40:00', '牛排熟度稍微過熟。', 3),
(3, '顧客E', '0985510798', '2026-08-26 11:40:00', '2026-08-28 19:40:00', '濃湯冷掉了，希望改進。', 1);

-- 17. 推播 (announcement)
INSERT INTO announcement (ann_title, ann_begin, ann_img, ann_text) VALUES 
('歡慶開幕！全館飲品8折', '2026-08-01 10:00:00', NULL, '即日起至月底，內用點選任意主餐即享飲品加購8折優惠！');

-- 18. QA (qa)：1、2 是 G1 原本的題目，3～11 是 05 新增；qa_clicks 是示範用的點擊次數（首頁顯示最多的 5 題）
INSERT INTO qa (qa_title, qa_content, qa_clicks) VALUES
('請問有提供素食餐點嗎？', '我們提供蛋奶素及全素的義大利麵與披薩，歡迎向服務人員諮詢。', 52),
('訂位最多可以接受幾人？', '線上訂位最多支援6人桌型，6人以上團體用餐請來電預約。', 128),
('線上訂位需要先登入會員嗎？', '需要。請先登入會員再進行訂位；尚未註冊的顧客可以使用手機號碼註冊會員。', 64),
('可以預約多久以後的日期？', '開放預約含今日起算90天內的日期。', 41),
('有哪些用餐時段可以預約？', '午餐 11:00、12:30，晚餐 17:00、18:30。當天已經開始的時段無法預約。', 96),
('一次可以訂多張桌子嗎？', '每次限訂1桌（2人、4人或6人桌）；多桌或包場需求請來電洽詢。', 27),
('訂位後想更改或取消怎麼辦？', '請在「會員中心」取消尚未開始的訂位；日期、時段或桌型需要更換時，請取消後重新訂位。', 75),
('沒有訂位可以現場候位嗎？', '可以，請至現場領取號碼牌。上午最晚發放至12:30，下午最晚發放至18:30，叫號進度可以在「候位狀況」頁面查看。', 87),
('候位可以取消或更換桌型嗎？', '可以在「候位狀況」頁面的「查詢候位」輸入手機取消候位；桌型無法線上修改，如需更換請取消後重新取號。', 12),
('結帳可以使用哪些付款方式？', '目前提供現金及刷卡付款。', 19),
('用餐時間有限制嗎？', '每組用餐時間以90分鐘為原則，敬請配合。', 33);

/* =========================================================
   以下是 05 的測試資料（19～27），放在加外鍵之前，和原版一樣「資料放好再加外鍵」
   寫法：INSERT … VALUES、INSERT … SELECT … JOIN、UPDATE … JOIN
   JOIN 只在執行這份 SQL 的時候跑一次（Java 程式不會呼叫），用來模擬「已經發生過的操作」：
     客人到了沒（訂位狀態）、點了什麼（明細單價）、算好的金額；正式流程是組員的程式在操作當下寫進去的
   JOIN 要讀前面已經新增的資料，順序不能調換：20 → 21（時段 → 訂位）、25 → 26 → 27（訂單 → 明細 → 金額）
   ========================================================= */

/* =========================================================
   入座管理「現在」的測試資料（19～27，壓力測試版：正式時段 ＋ 現在附近的時段）
   另一份 createTable_demo_real.sql 只用正式時段（11:00、12:30、17:00、18:30），和 CJ 的訂位頁一致
   時間都從「執行這份 SQL 的當下」推算，整天任何時間都可以執行，執行完馬上測
   （接近半夜時，算出來落在昨天、明天的訂位和訂單不會出現；入座時間落在昨天的桌子改成可使用，所以數字會比下面少）
   編號接在上面原始資料的後面：
     member 4～16、reservation_datetime 5～22、reservation 4～44、waiting 4～12、orders 4～29（16、18 沒用到）
   新增的會員 7～16 前台登入密碼都是 Test0004（和會員 4 共用同一個雜湊）

   正式時段（今天 11:00、12:30、17:00、18:30）也有訂位：已經過了的是實到、未到、取消；還沒到的是預約。
   為了讓下面的數字固定，「現在 ～ 現在＋90 分」之間的正式時段訂位不寫入（需保留只算附近時段那幾筆）。
   ---------- 執行後馬上看，應該是這樣（T＝執行時間取到 5 分鐘；不含正式時段的訂位） ----------
   桌子：17 桌（2 人桌 10、4 人桌 5、6 人桌 2）
   一打開就自動清桌（已結帳＝空桌）：A06、B01
   平面圖
     可使用  A01 A06 A07、B01 B03、C01
     使用中  A02 A03 A04 A05 A08 A09、B02 B04 B05、C02（A09 是時間異常：算使用中，但不算用餐中）
     停用    A10
     超時    A03 95 分、A04 100 分、B02 95 分（B04 是 89 分，過 1 分鐘後也會變超時）
     時間異常 A09（入座時間比現在晚 20 分）
     小字    A02「訂位 N 分」、A05「現場 N 分」、A08 尚未點餐
   桌位卡
     A02：共 2 張訂單，1 張未結帳（加點那張已付）；客人王小明
     A05：客人鄭雅文，客戶備註很長（測試換行）
     B05：共 2 張訂單，1 張未結帳（1 張已取消）
     A07（空桌）：2 人桌 空桌 3、需保留 6、現場還能坐 0 → 其它者入座會先跳警告
       訂位清單：接下來 10 筆（T+15 ×2、T+30 ×2、T+60、T+85、T+95、T+120 ×2、T+150）＋ 還沒到的正式時段訂位；逾時未到 1 筆（T−20 林小美）
       候位清單：3 組（已通知的張先生排第一）
   時間軸上方卡片
     用餐中 9 / 16 桌｜90 分鐘內的預約 11 組（2 人 6、4 人 2、6 人 3）｜須注意 5 項（超時 3、時間異常 1、桌數不夠 1）｜本日訂單 21 張
   時間軸（現在前後各 4 小時）
     2 人桌・預約：需保留 6・逾時未到 1
       T−240 實到 1、T−120 實到 2（另 1 筆取消不顯示）、T−40 實到 1、T−20 林小美・逾時未到（另有未到 1）、
       T+15 ×2、T+30 ×2、T+60、T+85、T+95（90 分鐘外，不算保留）、T+120 ×2、T+150
     4 人桌・預約：需保留 2・逾時未到 1
     6 人桌・預約：需保留 3；T+30 兩筆有紅色 !（桌數不夠），T+75 沒有
     A01：兩段稍早用過餐，第二段是「2 張單」（加點）
     A06、B01：剛結帳變空桌，用餐那段變成稍早用過餐；B03：剛離開的那組畫到「現在」
     A07：100 分鐘前那組有畫；7 小時前的訂單在時間軸起點之前，不畫；全部取消的訂單也不畫
     昨天、明天的訂位和昨天的訂單：都不會出現
   卡片篩選
     用餐中 → 9 桌；90 分鐘內的預約 → 只剩 T+15～T+85 的預約；
     須注意 → A03 A04 A09 B02 ＋ 6 人桌 T+30 兩筆紅色 !
   平面圖說明列篩選：可使用 6、使用中 10、停用 1、超時 3、時間異常 1（可以和桌型疊加；「超時」＋「4 人桌」→ 只剩 B02）
   ========================================================= */

-- 以 Java 時鐘為主：NOW() 一律用台灣時間，跟入座管理（Java）的「現在」一致
SET time_zone = '+08:00';

-- 現在（去掉秒）；訂位時段再取到 5 分鐘的整數，畫面上比較好讀
SET @now = NOW() - INTERVAL SECOND(NOW()) SECOND;
SET @t0  = @now - INTERVAL MOD(MINUTE(@now), 5) MINUTE;
SET @pwd = '{bcrypt}$2a$10$UCU/PUmq7rIyr0fLtWD2teTjzAuaS1DMu.6JKqV83FsHPGsmclcrG';   -- 明碼 Test0004

-- 19. 測試會員 (member 4～16)
INSERT INTO member (mem_no, mem_tel, mem_name, mem_account, mem_password, mem_mail, mem_barcode, mem_tag, mem_birth, mem_fb, mem_line, mem_goo, mem_x, mem_status) VALUES
(4,  '0911000004', '黃大偉', 'huangdw04', @pwd, 'huangdw04@example.com', '/GH45678', NULL, '1990-01-20', NULL, NULL, NULL, NULL, '已驗證'),
(5,  '0911000005', '吳佩琪', 'wupq05', '{bcrypt}$2a$10$ci7JLbzH5afudwz2Piy2KOqIBDoxI1obRbiAiOl9gcxf.L/DOLEK2', 'wupq05@example.com', '/IJ56789', '對海鮮過敏', '1993-05-02', NULL, NULL, NULL, NULL, '已驗證'),
(6,  '0911000006', '周子翔', 'zhouzx06', '{bcrypt}$2a$10$/9NX/QXbmZBSV1S56p746O/TXDSFSkRCkBLbd14UWRAyWSwRwjR5y', 'zhouzx06@example.com', '/KL67890', NULL, '1988-09-14', NULL, NULL, NULL, NULL, '已驗證'),
(7,  '0911000007', '陳志明', 'chenzm07', @pwd, 'chenzm07@example.com', '/MN11111', NULL, '1985-02-11', NULL, NULL, NULL, NULL, '已驗證'),
(8,  '0911000008', '林雅婷', 'linyt08', @pwd, 'linyt08@example.com', '/MN22222', '素食（蛋奶素）', '1992-06-30', NULL, NULL, NULL, NULL, '已驗證'),
(9,  '0911000009', '張家豪', 'zhangjh09', @pwd, 'zhangjh09@example.com', '/MN33333', NULL, '1979-12-01', NULL, NULL, NULL, NULL, '已驗證'),
(10, '0911000010', '李怡君', 'liyj10', @pwd, 'liyj10@example.com', '/MN44444', '帶嬰兒車，需要寬一點的位子', '1990-08-08', NULL, NULL, NULL, NULL, '已驗證'),
(11, '0911000011', '王俊傑', 'wangjj11', @pwd, 'wangjj11@example.com', '/MN55555', NULL, '1995-03-21', NULL, NULL, NULL, NULL, '已驗證'),
(12, '0911000012', '劉淑芬', 'liusf12', @pwd, 'liusf12@example.com', '/MN66666', '對堅果過敏', '1983-10-17', NULL, NULL, NULL, NULL, '已驗證'),
(13, '0911000013', '鄭雅文', 'zhengyw13', @pwd, 'zhengyw13@example.com', '/MN77777', '喜歡安靜的位子；上次說燉飯太鹹，這次請廚房少放鹽；生日在這個月，結帳時可以詢問是否要招待甜點', '1996-12-30', NULL, NULL, NULL, NULL, '已驗證'),
(14, '0911000014', '蔡明哲', 'caimz14', @pwd, 'caimz14@example.com', '/MN88888', NULL, '1987-04-04', NULL, NULL, NULL, NULL, '已驗證'),
(15, '0911000015', '許芳瑜', 'xufy15', @pwd, 'xufy15@example.com', '/MN99999', NULL, '1999-09-09', NULL, NULL, NULL, NULL, '未驗證'),
(16, '0911000016', '歐陽靜', 'ouyj16', @pwd, 'ouyj16@example.com', '/MN00000', NULL, '1991-01-01', NULL, NULL, NULL, NULL, '已驗證');

-- 20. 訂位時段 (reservation_datetime 5～18)：今天在「現在」前後，另外各一個昨天、明天的時段（不應出現）
INSERT INTO reservation_datetime (rsv_dt_no, rsv_dt_datetime) VALUES
(5,  @t0 - INTERVAL 240 MINUTE),
(6,  @t0 - INTERVAL 120 MINUTE),
(7,  @t0 - INTERVAL 40 MINUTE),   -- 已經來了（A02、B01）
(8,  @t0 - INTERVAL 20 MINUTE),   -- 過了還沒來 → 逾時未到
(9,  @t0 + INTERVAL 15 MINUTE),   -- 9～13：90 分鐘內 → 需保留
(10, @t0 + INTERVAL 30 MINUTE),
(11, @t0 + INTERVAL 60 MINUTE),
(12, @t0 + INTERVAL 75 MINUTE),
(13, @t0 + INTERVAL 85 MINUTE),
(14, @t0 + INTERVAL 95 MINUTE),   -- 剛好超過 90 分鐘 → 不算保留
(15, @t0 + INTERVAL 120 MINUTE),
(16, @t0 + INTERVAL 150 MINUTE),
(17, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '18:30:00')),   -- 昨天
(18, TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '18:30:00')),   -- 明天
(19, TIMESTAMP(CURDATE(), '11:00:00')),                   -- 19～22：今天的正式時段
(20, TIMESTAMP(CURDATE(), '12:30:00')),
(21, TIMESTAMP(CURDATE(), '17:00:00')),
(22, TIMESTAMP(CURDATE(), '18:30:00'));

-- 21. 訂位 (reservation 4～32)
INSERT INTO reservation (rsv_no, mem_no, rsv_dt_no, seat_type_no, rsv_create_time, rsv_status, rsv_comment) VALUES
-- 2 人桌
(4,  7,  5,  2, @now - INTERVAL 3 DAY, '實到', '2人用餐'),
(5,  8,  6,  2, @now - INTERVAL 2 DAY, '實到', '2人用餐'),
(6,  9,  6,  2, @now - INTERVAL 2 DAY, '實到', '2人約會'),
(7,  10, 6,  2, @now - INTERVAL 1 DAY, '取消', '臨時有事'),
(8,  1,  7,  2, @now - INTERVAL 1 DAY, '實到', '2人用餐'),          -- 坐在 A02
(9,  3,  8,  2, @now - INTERVAL 1 DAY, '預約', '2人用餐'),          -- 逾時未到
(10, 11, 8,  2, @now - INTERVAL 1 DAY, '未到', '2人用餐'),          -- 訂位管理已標成未到
(11, 5,  9,  2, @now - INTERVAL 1 DAY, '預約', '2人用餐'),
(12, 12, 9,  2, @now,                  '預約', '2人，靠窗'),
(13, 6,  10, 2, @now,                  '預約', '2人用餐'),
(14, 13, 10, 2, @now - INTERVAL 5 DAY, '預約', '2人，慶祝生日'),
(15, 1,  10, 2, @now - INTERVAL 1 DAY, '取消', '改期'),
(16, 2,  11, 2, @now - INTERVAL 2 DAY, '預約', '2人約會'),
(17, 14, 13, 2, @now - INTERVAL 1 DAY, '預約', '2人用餐'),
(18, 15, 14, 2, @now - INTERVAL 1 DAY, '預約', '2人用餐'),          -- 剛好超過 90 分鐘
(19, 16, 15, 2, @now - INTERVAL 1 DAY, '預約', '2人用餐'),
(20, 7,  15, 2, @now - INTERVAL 1 DAY, '預約', '2人用餐'),
(21, 8,  16, 2, @now - INTERVAL 1 DAY, '預約', '2人用餐'),
(22, 13, 17, 2, @now - INTERVAL 3 DAY, '預約', '昨天的訂位'),        -- 不應出現
-- 4 人桌
(23, 2,  7,  4, @now - INTERVAL 2 DAY, '實到', '4人聚餐'),          -- 坐在 B01（已結帳）
(24, 4,  8,  4, @now - INTERVAL 3 DAY, '預約', '4人用餐'),          -- 逾時未到
(25, 9,  10, 4, @now - INTERVAL 1 DAY, '預約', '3人用餐'),
(26, 10, 11, 4, @now - INTERVAL 1 DAY, '預約', '4人，有嬰兒車'),
(27, 6,  15, 4, @now - INTERVAL 1 DAY, '預約', '4人用餐'),
(28, 14, 18, 4, @now,                  '預約', '明天的訂位'),        -- 不應出現
-- 6 人桌
(29, 11, 6,  6, @now - INTERVAL 4 DAY, '實到', '6人聚餐'),
(30, 4,  10, 6, @now - INTERVAL 4 DAY, '預約', '6人慶生'),
(31, 5,  10, 6, @now - INTERVAL 2 DAY, '預約', '5人家庭聚餐'),      -- 和 30 同一個時段：6 人桌不夠 → 紅色 !
(32, 12, 12, 6, @now - INTERVAL 1 DAY, '預約', '6人公司聚餐');

-- 21-1. 正式時段的訂位 (reservation 33～44)：時段過了用 past_status、還沒到用 future_status
--       「現在 ～ 現在＋90 分」之間的不寫入，讓需保留的數字固定（只有附近時段那幾筆）
INSERT INTO reservation (rsv_no, mem_no, rsv_dt_no, seat_type_no, rsv_create_time, rsv_status, rsv_comment)
SELECT r.rsv_no, r.mem_no, r.rsv_dt_no, r.seat_type_no, @now - INTERVAL 2 DAY,
       IF(d.rsv_dt_datetime <= @now, r.past_status, r.future_status), r.rsv_comment
FROM (
            SELECT 33 AS rsv_no, 9 AS mem_no, 19 AS rsv_dt_no, 2 AS seat_type_no, '實到' AS past_status, '預約' AS future_status, '2人用餐' AS rsv_comment
  UNION ALL SELECT 34, 10, 19, 2, '未到', '預約', '2人用餐'
  UNION ALL SELECT 35, 2,  19, 4, '實到', '預約', '4人聚餐'
  UNION ALL SELECT 36, 11, 20, 2, '實到', '預約', '2人午餐'
  UNION ALL SELECT 37, 12, 20, 2, '取消', '取消', '臨時有事'
  UNION ALL SELECT 38, 6,  20, 6, '實到', '預約', '6人聚餐'
  UNION ALL SELECT 39, 13, 21, 2, '實到', '預約', '2人晚餐'
  UNION ALL SELECT 40, 14, 21, 2, '實到', '預約', '2人晚餐'
  UNION ALL SELECT 41, 3,  21, 4, '未到', '預約', '4人用餐'
  UNION ALL SELECT 42, 15, 22, 2, '實到', '預約', '2人晚餐'
  UNION ALL SELECT 43, 16, 22, 2, '未到', '預約', '2人晚餐'
  UNION ALL SELECT 44, 4,  22, 4, '實到', '預約', '4人晚餐'
) AS r
JOIN reservation_datetime d ON d.rsv_dt_no = r.rsv_dt_no   -- JOIN：查時段時間，過了用 past_status、還沒到用 future_status
-- 正式時段在「現在～現在＋90 分」之內的不寫，讓上面的預期數字固定
WHERE NOT (d.rsv_dt_datetime > @now AND d.rsv_dt_datetime < @now + INTERVAL 90 MINUTE);

-- 22. 今天的候位 (waiting 4～12)：候位中、已通知會出現；已入座、過號、取消不會
INSERT INTO waiting (waiting_no, seat_type_no, mem_no, waiting_tel, waiting_name, waiting_comment, waiting_status, waiting_notify_time, waiting_date, waiting_take_time, waiting_ticket_seq) VALUES
(4,  2, NULL, '0912345678', '張先生', '2位',       '已通知', TIME(@now - INTERVAL 5 MINUTE), CURDATE(), @now - INTERVAL 40 MINUTE, 3),
(5,  2, NULL, '0922333444', '李小姐', '2位',       '候位中', NULL, CURDATE(), @now - INTERVAL 25 MINUTE, 4),
(6,  2, 15,   '0911000015', '許芳瑜', '2位，會員', '候位中', NULL, CURDATE(), @now - INTERVAL 10 MINUTE, 5),
(7,  2, NULL, '0933111222', '吳先生', '2位',       '已入座', TIME(@now - INTERVAL 50 MINUTE), CURDATE(), @now - INTERVAL 70 MINUTE, 1),
(8,  2, NULL, '0933222333', '陳小姐', '2位',       '過號',   TIME(@now - INTERVAL 30 MINUTE), CURDATE(), @now - INTERVAL 60 MINUTE, 2),
(9,  4, NULL, '0933555666', '趙先生', '4位',       '候位中', NULL, CURDATE(), @now - INTERVAL 15 MINUTE, 1),
(10, 6, NULL, '0955777888', '何小姐', '6位聚餐',   '候位中', NULL, CURDATE(), @now - INTERVAL 20 MINUTE, 1),
(11, 6, NULL, '0955888999', '孫先生', '5位',       '候位中', NULL, CURDATE(), @now - INTERVAL 5 MINUTE,  2),
(12, 6, NULL, '0955999000', '鄭小姐', '6位',       '取消',   NULL, CURDATE(), @now - INTERVAL 35 MINUTE, 3);

-- 23. 桌位：原始資料的 A01、B01、C01 改成測試狀態
--     入座時間算出來不是今天（例如半夜執行）的桌子改成「可使用」，不然入座管理會當成時間異常
UPDATE seat SET seat_status = '可使用', seat_time = NULL WHERE seat_no IN ('A01', 'C01');
UPDATE seat SET seat_status = IF(DATE(@t0 - INTERVAL 38 MINUTE) = CURDATE(), '訂位者使用', '可使用'),
                seat_time   = IF(DATE(@t0 - INTERVAL 38 MINUTE) = CURDATE(), TIME(@t0 - INTERVAL 38 MINUTE), NULL)
WHERE seat_no = 'B01';   -- 已結帳：一打開入座管理就自動變回可使用

-- 24. 桌位：補齊到平面圖上的 17 桌（2 人桌 A01～A10、4 人桌 B01～B05、6 人桌 C01～C02）
INSERT INTO seat (seat_no, seat_type_no, seat_status, seat_time)
SELECT s.seat_no, s.seat_type_no,
       CASE WHEN s.seated_at IS NULL OR DATE(s.seated_at) = CURDATE() THEN s.seat_status ELSE '可使用' END,
       CASE WHEN s.seated_at IS NOT NULL AND DATE(s.seated_at) = CURDATE() THEN TIME(s.seated_at) END
FROM (
            SELECT 'A02' AS seat_no, 2 AS seat_type_no, '訂位者使用' AS seat_status, @t0 - INTERVAL 37 MINUTE AS seated_at
  UNION ALL SELECT 'A03', 2, '其它者使用', @now - INTERVAL 95 MINUTE    -- 超時
  UNION ALL SELECT 'A04', 2, '其它者使用', @now - INTERVAL 100 MINUTE   -- 超時
  UNION ALL SELECT 'A05', 2, '其它者使用', @now - INTERVAL 30 MINUTE
  UNION ALL SELECT 'A06', 2, '其它者使用', @now - INTERVAL 50 MINUTE    -- 已結帳：一打開就自動清桌
  UNION ALL SELECT 'A07', 2, '可使用',     NULL
  UNION ALL SELECT 'A08', 2, '其它者使用', @now - INTERVAL 15 MINUTE    -- 還沒點餐
  UNION ALL SELECT 'A09', 2, '其它者使用', @now + INTERVAL 20 MINUTE    -- 入座時間比現在晚 → 時間異常
  UNION ALL SELECT 'A10', 2, '停用',       NULL
  UNION ALL SELECT 'B02', 4, '其它者使用', @now - INTERVAL 95 MINUTE    -- 超時
  UNION ALL SELECT 'B03', 4, '可使用',     NULL
  UNION ALL SELECT 'B04', 4, '訂位者使用', @now - INTERVAL 89 MINUTE    -- 差 1 分鐘超時
  UNION ALL SELECT 'B05', 4, '其它者使用', @now - INTERVAL 10 MINUTE
  UNION ALL SELECT 'C02', 6, '其它者使用', @now - INTERVAL 50 MINUTE    -- 預估現在 +40 分離開，趕不上 T+30 的訂位
) AS s;

-- 25. 訂單 (orders 4～29)：orders_pay 是 NULL＝還沒結帳；金額先填 0，27 再從明細加總
INSERT INTO orders (orders_no, seat_no, member_no, orders_time, orders_total, orders_discount_total, orders_actual_price, orders_pay) VALUES
-- 現在在座這組的訂單（下單時間晚於入座時間）
(4,  'A02', 1,    @t0  - INTERVAL 30 MINUTE,  0, 0, 0, NULL),
(5,  'A02', NULL, @t0  - INTERVAL 10 MINUTE,  0, 0, 0, '現金'),   -- 加點，已付 → A02 還有 1 張未結帳
(6,  'A03', NULL, @now - INTERVAL 90 MINUTE,  0, 0, 0, NULL),
(7,  'A03', NULL, @now - INTERVAL 40 MINUTE,  0, 0, 0, NULL),     -- 加點
(8,  'A04', NULL, @now - INTERVAL 95 MINUTE,  0, 0, 0, NULL),
(9,  'A05', 13,   @now - INTERVAL 25 MINUTE,  0, 0, 0, NULL),     -- 客戶備註很長
(10, 'A06', NULL, @now - INTERVAL 45 MINUTE,  0, 0, 0, '刷卡'),   -- 全部已付 → 自動清桌
(11, 'B04', 14,   @now - INTERVAL 85 MINUTE,  0, 0, 0, NULL),
(12, 'B01', 2,    @t0  - INTERVAL 32 MINUTE,  0, 0, 0, '刷卡'),   -- 全部已付 → 自動清桌
(13, 'B02', NULL, @now - INTERVAL 90 MINUTE,  0, 0, 0, NULL),
(14, 'B05', NULL, @now - INTERVAL 6 MINUTE,   0, 0, 0, NULL),
(15, 'B05', NULL, @now - INTERVAL 3 MINUTE,   0, 0, 0, NULL),     -- 明細全部已取消
(17, 'C02', NULL, @now - INTERVAL 45 MINUTE,  0, 0, 0, NULL),
-- 今天稍早別組客人的訂單（已結帳）：時間軸上的「稍早用過餐」
(19, 'A01', 3,    @now - INTERVAL 230 MINUTE, 0, 0, 0, '刷卡'),
(20, 'A01', NULL, @now - INTERVAL 120 MINUTE, 0, 0, 0, '現金'),   -- 和 19 相隔 110 分鐘 → 另一組客人
(21, 'A01', NULL, @now - INTERVAL 100 MINUTE, 0, 0, 0, '現金'),   -- 和 20 同一組（加點）→「2 張單」
(22, 'A07', NULL, @now - INTERVAL 420 MINUTE, 0, 0, 0, '現金'),   -- 時間軸起點之前，不畫（但算在本日訂單）
(23, 'B03', 4,    @now - INTERVAL 200 MINUTE, 0, 0, 0, '刷卡'),
(24, 'C01', NULL, @now - INTERVAL 170 MINUTE, 0, 0, 0, '現金'),
(25, 'A02', NULL, @now - INTERVAL 160 MINUTE, 0, 0, 0, '現金'),
(26, 'A07', NULL, @now - INTERVAL 100 MINUTE, 0, 0, 0, '現金'),
(27, 'B03', NULL, @now - INTERVAL 60 MINUTE,  0, 0, 0, '刷卡'),   -- 剛離開
-- 不應出現的訂單
(28, 'A05', NULL, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '19:00:00'), 0, 0, 0, '現金'),   -- 昨天
(29, 'A07', NULL, @now - INTERVAL 100 MINUTE, 0, 0, 0, NULL);     -- 明細全部已取消

-- 26. 訂單明細：單價從 meal 表讀；狀態照 G1：製作中／等待送餐／已送達／已取消
--     已送達的送達時間＝下單後 15 分鐘（不晚於現在），其他是 NULL
INSERT INTO orders_details (orders_no, meal_no, promote_no, od_meal_num, od_discount_price, od_discount_total, od_actual_price, od_comment, od_status, od_served_at)
SELECT d.orders_no, m.meal_no, NULL, d.qty, m.meal_price, 0, m.meal_price * d.qty, d.cmt, d.st,
       CASE WHEN d.st = '已送達' THEN LEAST(o.orders_time + INTERVAL 15 MINUTE, @now) ELSE NULL END
FROM (
            SELECT 4 AS orders_no, 1 AS meal_no, 1 AS qty, '' AS cmt, '已送達' AS st
  UNION ALL SELECT 4,  11, 1, '不要辣', '製作中'
  UNION ALL SELECT 5,  37, 2, '', '已送達'
  UNION ALL SELECT 6,  2,  1, '', '已送達'
  UNION ALL SELECT 6,  12, 1, '', '已送達'
  UNION ALL SELECT 7,  20, 1, '', '等待送餐'
  UNION ALL SELECT 8,  3,  2, '', '已送達'
  UNION ALL SELECT 8,  13, 1, '', '已送達'
  UNION ALL SELECT 9,  4,  1, '少鹽', '製作中'
  UNION ALL SELECT 9,  14, 1, '', '製作中'
  UNION ALL SELECT 10, 5,  2, '', '已送達'
  UNION ALL SELECT 11, 6,  1, '', '已送達'
  UNION ALL SELECT 11, 15, 1, '', '已送達'
  UNION ALL SELECT 12, 7,  2, '', '已送達'
  UNION ALL SELECT 12, 16, 2, '', '已送達'
  UNION ALL SELECT 13, 8,  2, '', '已送達'
  UNION ALL SELECT 13, 17, 2, '', '等待送餐'
  UNION ALL SELECT 14, 9,  2, '', '製作中'
  UNION ALL SELECT 15, 10, 1, '', '已取消'
  UNION ALL SELECT 15, 37, 1, '', '已取消'
  UNION ALL SELECT 17, 19, 3, '', '已送達'
  UNION ALL SELECT 17, 21, 3, '', '製作中'
  UNION ALL SELECT 19, 9,  1, '', '已送達'
  UNION ALL SELECT 20, 10, 2, '', '已送達'
  UNION ALL SELECT 21, 37, 2, '', '已送達'
  UNION ALL SELECT 22, 11, 1, '', '已送達'
  UNION ALL SELECT 23, 12, 3, '', '已送達'
  UNION ALL SELECT 24, 13, 4, '', '已送達'
  UNION ALL SELECT 25, 1,  1, '', '已送達'
  UNION ALL SELECT 26, 2,  1, '', '已送達'
  UNION ALL SELECT 27, 3,  3, '', '已送達'
  UNION ALL SELECT 28, 4,  1, '', '已送達'
  UNION ALL SELECT 29, 5,  1, '', '已取消'
) AS d
JOIN meal m ON m.meal_no = d.meal_no            -- JOIN：從 meal 讀單價（不寫死，G1 改價格也會跟著變）
JOIN orders o ON o.orders_no = d.orders_no      -- JOIN：讀下單時間，推算已送達的時間
ORDER BY d.orders_no, d.meal_no;

-- 27. 訂單金額：從明細加總（只更新 4～29；全部取消的單金額是 0）
-- JOIN：把第 26 節的明細加總，寫回訂單的金額
UPDATE orders o
JOIN (
  SELECT orders_no, SUM(CASE WHEN od_status = '已取消' THEN 0 ELSE od_actual_price END) AS total
  FROM orders_details
  WHERE orders_no BETWEEN 4 AND 29
  GROUP BY orders_no
) t ON t.orders_no = o.orders_no
SET o.orders_total = t.total,
    o.orders_discount_total = 0,
    o.orders_actual_price = t.total
WHERE o.orders_no BETWEEN 4 AND 29;

/*
ALTER TABLE meal_type AUTO_INCREMENT = 1;
ALTER TABLE meal AUTO_INCREMENT = 1;
ALTER TABLE member AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE promote AUTO_INCREMENT = 1;
ALTER TABLE waiting AUTO_INCREMENT = 1;
ALTER TABLE reservation AUTO_INCREMENT = 1;
ALTER TABLE reservation_datetime AUTO_INCREMENT = 1;
ALTER TABLE employee AUTO_INCREMENT = 1;
ALTER TABLE permission AUTO_INCREMENT = 1;
ALTER TABLE feedback AUTO_INCREMENT = 1;
ALTER TABLE announcement AUTO_INCREMENT = 1;
ALTER TABLE qa AUTO_INCREMENT = 1;
*/

-- 添加外鍵 (Foreign Keys)
-- 桌位 → 桌型
ALTER TABLE seat ADD CONSTRAINT seat_seat_type_fk FOREIGN KEY (seat_type_no) REFERENCES seat_type(seat_type_no);

-- 訂單 → 桌位、會員
ALTER TABLE orders ADD CONSTRAINT orders_seat_fk FOREIGN KEY (seat_no) REFERENCES seat(seat_no);
ALTER TABLE orders ADD CONSTRAINT orders_member_fk FOREIGN KEY (member_no) REFERENCES member(mem_no);

-- 回饋 → 訂單
ALTER TABLE feedback ADD CONSTRAINT feedback_orders_fk FOREIGN KEY (orders_no) REFERENCES orders(orders_no);

-- 訂單明細 → 訂單、菜品、活動
ALTER TABLE orders_details ADD CONSTRAINT orders_details_orders_fk FOREIGN KEY (orders_no) REFERENCES orders(orders_no);
ALTER TABLE orders_details ADD CONSTRAINT orders_details_meal_fk FOREIGN KEY (meal_no) REFERENCES meal(meal_no);
ALTER TABLE orders_details ADD CONSTRAINT orders_details_promote_fk FOREIGN KEY (promote_no) REFERENCES promote(promote_no);

-- 菜品 → 菜品類別
ALTER TABLE meal ADD CONSTRAINT meal_meal_type_fk FOREIGN KEY (meal_type_no) REFERENCES meal_type(meal_type_no);

-- 活動商品 → 活動、菜品
ALTER TABLE promote_meal ADD CONSTRAINT promote_meal_promote_fk FOREIGN KEY (promote_no) REFERENCES promote(promote_no);
ALTER TABLE promote_meal ADD CONSTRAINT promote_meal_meal_fk FOREIGN KEY (meal_no) REFERENCES meal(meal_no);

-- 訂位 → 會員、訂位時段、桌型
ALTER TABLE reservation ADD CONSTRAINT reservation_member_fk FOREIGN KEY (mem_no) REFERENCES member(mem_no);
ALTER TABLE reservation ADD CONSTRAINT reservation_rsv_dt_fk FOREIGN KEY (rsv_dt_no) REFERENCES reservation_datetime(rsv_dt_no);
ALTER TABLE reservation ADD CONSTRAINT reservation_seat_type_fk FOREIGN KEY (seat_type_no) REFERENCES seat_type(seat_type_no);

-- 候位 → 會員、桌型
ALTER TABLE waiting ADD CONSTRAINT waiting_member_fk FOREIGN KEY (mem_no) REFERENCES member(mem_no);
ALTER TABLE waiting ADD CONSTRAINT waiting_seat_type_fk FOREIGN KEY (seat_type_no) REFERENCES seat_type(seat_type_no);

-- 員工權限 → 員工、系統權限
ALTER TABLE emp_permission ADD CONSTRAINT emp_permission_employee_fk FOREIGN KEY (emp_no) REFERENCES employee(emp_no);
ALTER TABLE emp_permission ADD CONSTRAINT emp_permission_permission_fk FOREIGN KEY (perm_no) REFERENCES permission(perm_no);

SET FOREIGN_KEY_CHECKS = 1;