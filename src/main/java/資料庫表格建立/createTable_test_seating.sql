/* =========================================================
   入座管理測試用的建表 SQL（05 專用）
   = createTable_v1_4.sql（G1 原版，fee538f）＋ 入座管理「現在」的測試資料（19～27）
   和原版一樣：刪掉整個資料庫 → 建表 → 新增資料 → 修改測試資料 → 加外鍵，可以重複執行
   和原版不同的地方：
     · orders_details 多了 od_served_at（OrdersDetailsVO 有這個欄位，少了它讀訂單的頁面會出錯）
     · 最後多了 19～27 的測試資料，時間從「執行的當下」推算：請在 14:30～21:30 執行，執行完馬上測
   權限照 G1：員工 1（陳小心，密碼 0987878787）有 1～7 全部權限，3 = 入座管理
   測完要回到原版資料：重跑 createTable_v1_4.sql
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
	od_served_at DATETIME NULL COMMENT '送達時間',
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

-- 9. 插入義餐訂單明細 (orders_details)
INSERT INTO orders_details (orders_no, meal_no, promote_no, od_meal_num, od_discount_price, od_discount_total, od_actual_price, od_comment, od_status) VALUES 
-- 訂單 1 明細 (A01 桌)
(1, 1, null, 1, 160, 0, 160, '麵包要烤酥一點', '已送達'),
(1, 4, 1, 1, 380, 0, 380, '飯軟一點', '已送達'),
(1,8, null, 1, 130, 0, 130, '少冰', '已取消'),
(1,11, 2, 1, 160, 50, 110, '', '等待送餐'),
-- 訂單 2 明細 (B01 桌)
(2,3, null, 1, 200, 0, 200, '', '已送達'),
(2, 7, 1, 1, 680, 0, 680, '三分熟', '等待送餐'),
(2, 10, null, 1, 100, 0, 100, '加冰塊與檸檬片', '準備中'),
-- 訂單 3 明細 (C01 桌)
(3, 6, null, 1, 300, 0, 300, '起司多一點', '準備中'),
(3, 8, 1, 1, 130, 0, 130, '去冰', '未備料');

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

-- 18. QA (qa)
INSERT INTO qa (qa_title, qa_content) VALUES
('請問有提供素食餐點嗎？', '我們提供蛋奶素及全素的義大利麵與披薩，歡迎向服務人員諮詢。'),
('訂位最多可以接受幾人？', '線上訂位最多支援6人桌型，6人以上團體用餐請來電預約。');

/* =========================================================
   入座管理「現在」的測試資料（19～27）
   時間都從「執行這份 SQL 的當下」推算，請在 14:30～21:30 之間執行，執行完馬上測
   （時間軸才看得到今天稍早的訂單；過了一段時間再測，超時、保留的數字會跟著變）
   編號接在上面原始資料的後面：
     member 4～6、reservation_datetime 5～9、reservation 4～15、waiting 4～6、orders 4～17
   ========================================================= */

-- 以 Java 時鐘為主：NOW() 一律用台灣時間，跟入座管理（Java）的「現在」一致
SET time_zone = '+08:00';

-- 現在（去掉秒）；訂位時段再取到 5 分鐘的整數，畫面上比較好讀
SET @now = NOW() - INTERVAL SECOND(NOW()) SECOND;
SET @t0  = @now - INTERVAL MOD(MINUTE(@now), 5) MINUTE;

-- 19. 測試會員 (member 4～6)：前台登入的明碼是 Test0004～Test0006
INSERT INTO member (mem_no, mem_tel, mem_name, mem_account, mem_password, mem_mail, mem_barcode, mem_tag, mem_birth, mem_fb, mem_line, mem_goo, mem_x, mem_status) VALUES
(4, '0911000004', '黃大偉', 'huangdw04', '{bcrypt}$2a$10$UCU/PUmq7rIyr0fLtWD2teTjzAuaS1DMu.6JKqV83FsHPGsmclcrG', 'huangdw04@example.com', '/GH45678', NULL, '1990-01-20', NULL, NULL, NULL, NULL, '已驗證'),
(5, '0911000005', '吳佩琪', 'wupq05', '{bcrypt}$2a$10$ci7JLbzH5afudwz2Piy2KOqIBDoxI1obRbiAiOl9gcxf.L/DOLEK2', 'wupq05@example.com', '/IJ56789', '對海鮮過敏', '1993-05-02', NULL, NULL, NULL, NULL, '已驗證'),
(6, '0911000006', '周子翔', 'zhouzx06', '{bcrypt}$2a$10$/9NX/QXbmZBSV1S56p746O/TXDSFSkRCkBLbd14UWRAyWSwRwjR5y', 'zhouzx06@example.com', '/KL67890', NULL, '1988-09-14', NULL, NULL, NULL, NULL, '已驗證');

-- 20. 今天的訂位時段 (reservation_datetime 5～9)，都在「現在」前後
INSERT INTO reservation_datetime (rsv_dt_no, rsv_dt_datetime) VALUES
(5, @t0 - INTERVAL 40 MINUTE),    -- 已經來了（A02、B01）
(6, @t0 - INTERVAL 20 MINUTE),    -- 過了還沒來 → 逾時未到
(7, @t0 + INTERVAL 30 MINUTE),    -- 90 分鐘內 → 需保留
(8, @t0 + INTERVAL 60 MINUTE),    -- 90 分鐘內 → 需保留
(9, @t0 + INTERVAL 120 MINUTE);   -- 超過 90 分鐘 → 不算保留

-- 21. 今天的訂位 (reservation 4～15)
INSERT INTO reservation (rsv_no, mem_no, rsv_dt_no, seat_type_no, rsv_create_time, rsv_status, rsv_comment) VALUES
(4,  1, 5, 2, @now - INTERVAL 1 DAY, '實到', '2人用餐'),
(5,  2, 5, 4, @now - INTERVAL 2 DAY, '實到', '4人聚餐'),
(6,  3, 6, 2, @now - INTERVAL 1 DAY, '預約', '2人用餐'),          -- 逾時未到
(7,  4, 6, 4, @now - INTERVAL 3 DAY, '預約', '4人用餐'),          -- 逾時未到
(8,  5, 7, 2, @now - INTERVAL 1 DAY, '預約', '2人用餐'),
(9,  6, 7, 2, @now,                  '預約', '2人用餐'),
(10, 2, 8, 2, @now - INTERVAL 2 DAY, '預約', '2人約會'),
(11, 3, 7, 4, @now - INTERVAL 1 DAY, '預約', '3人用餐'),
(12, 4, 7, 6, @now - INTERVAL 4 DAY, '預約', '6人慶生'),
(13, 5, 7, 6, @now - INTERVAL 2 DAY, '預約', '5人家庭聚餐'),     -- 和 12 同一個時段：6 人桌不夠 → 時間軸紅色 !
(14, 6, 9, 4, @now - INTERVAL 1 DAY, '預約', '4人用餐'),          -- 超過 90 分鐘，不算保留
(15, 1, 7, 2, @now - INTERVAL 1 DAY, '取消', '臨時有事');         -- 已取消，不算

-- 22. 正在候位的客人 (waiting 4～6)
INSERT INTO waiting (waiting_no, seat_type_no, mem_no, waiting_tel, waiting_name, waiting_comment, waiting_status, waiting_notify_time) VALUES
(4, 2, NULL, '0912345678', '張先生', '2位',     '已通知', TIME(@now - INTERVAL 5 MINUTE)),
(5, 2, NULL, '0922333444', '李小姐', '2位',     '候位中', NULL),
(6, 6, NULL, '0955777888', '何小姐', '6位聚餐', '候位中', NULL);

-- 23. 桌位：原始資料的 A01、B01、C01 改成測試狀態
UPDATE seat SET seat_status = '可使用',     seat_time = NULL                            WHERE seat_no = 'A01';
UPDATE seat SET seat_status = '訂位者使用', seat_time = TIME(@t0 - INTERVAL 38 MINUTE) WHERE seat_no = 'B01';   -- 已結帳：一打開入座管理就自動變回可使用
UPDATE seat SET seat_status = '可使用',     seat_time = NULL                            WHERE seat_no = 'C01';

-- 24. 桌位：補齊到 17 桌（2 人桌 A01～A10、4 人桌 B01～B05、6 人桌 C01～C02）
INSERT INTO seat (seat_no, seat_type_no, seat_status, seat_time) VALUES
('A02', 2, '訂位者使用', TIME(@t0 - INTERVAL 37 MINUTE)),
('A03', 2, '其它者使用', TIME(@now - INTERVAL 95 MINUTE)),    -- 超時、未結帳
('A04', 2, '其它者使用', TIME(@now - INTERVAL 100 MINUTE)),   -- 超時、未結帳
('A05', 2, '其它者使用', TIME(@now - INTERVAL 30 MINUTE)),
('A06', 2, '可使用',     NULL),
('A07', 2, '可使用',     NULL),
('A08', 2, '其它者使用', TIME(@now - INTERVAL 15 MINUTE)),    -- 還沒點餐
('A09', 2, '其它者使用', TIME(@now + INTERVAL 20 MINUTE)),    -- 入座時間比現在晚 → 時間異常
('A10', 2, '停用',       NULL),
('B02', 4, '其它者使用', TIME(@now - INTERVAL 95 MINUTE)),    -- 超時、不影響（還有空桌）
('B03', 4, '可使用',     NULL),
('B04', 4, '可使用',     NULL),
('B05', 4, '其它者使用', TIME(@now - INTERVAL 10 MINUTE)),
('C02', 6, '其它者使用', TIME(@now - INTERVAL 50 MINUTE));    -- 預估現在 +40 分離開，趕不上 +30 分的訂位

-- 25. 今天的訂單 (orders 4～17)：orders_pay 是 NULL＝還沒結帳；金額先填 0，27 再從明細加總
--   4～11、17：現在在座這組的訂單（下單時間晚於入座時間）
--   12～16：今天稍早別組客人的訂單（已結帳），時間軸上的「稍早用過餐」
INSERT INTO orders (orders_no, seat_no, member_no, orders_time, orders_total, orders_discount_total, orders_actual_price, orders_pay) VALUES
(4,  'A02', 1,    @t0  - INTERVAL 30 MINUTE,  0, 0, 0, NULL),
(5,  'A03', NULL, @now - INTERVAL 90 MINUTE,  0, 0, 0, NULL),
(6,  'A04', NULL, @now - INTERVAL 95 MINUTE,  0, 0, 0, NULL),
(7,  'A05', NULL, @now - INTERVAL 25 MINUTE,  0, 0, 0, NULL),
(8,  'B01', 2,    @t0  - INTERVAL 32 MINUTE,  0, 0, 0, '刷卡'),
(9,  'B02', NULL, @now - INTERVAL 90 MINUTE,  0, 0, 0, NULL),
(10, 'B05', NULL, @now - INTERVAL 6 MINUTE,   0, 0, 0, NULL),
(11, 'C02', NULL, @now - INTERVAL 45 MINUTE,  0, 0, 0, NULL),
(12, 'A01', 3,    @now - INTERVAL 200 MINUTE, 0, 0, 0, '刷卡'),
(13, 'A06', NULL, @now - INTERVAL 180 MINUTE, 0, 0, 0, '現金'),
(14, 'B03', 4,    @now - INTERVAL 210 MINUTE, 0, 0, 0, '刷卡'),
(15, 'C01', NULL, @now - INTERVAL 170 MINUTE, 0, 0, 0, '現金'),
(16, 'A02', NULL, @now - INTERVAL 160 MINUTE, 0, 0, 0, '現金'),
(17, 'B05', NULL, @now - INTERVAL 3 MINUTE,   0, 0, 0, NULL);   -- 明細全部已取消

-- 26. 今天訂單的明細：單價從 meal 表讀
INSERT INTO orders_details (orders_no, meal_no, promote_no, od_meal_num, od_discount_price, od_discount_total, od_actual_price, od_comment, od_status)
SELECT d.orders_no, m.meal_no, NULL, d.qty, m.meal_price, 0, m.meal_price * d.qty, d.cmt, d.st
FROM (
            SELECT 4 AS orders_no, 1 AS meal_no, 1 AS qty, '' AS cmt, '已送達' AS st
  UNION ALL SELECT 4,  11, 1, '不要辣', '準備中'
  UNION ALL SELECT 5,  2,  1, '', '已送達'
  UNION ALL SELECT 5,  12, 1, '', '已送達'
  UNION ALL SELECT 6,  3,  2, '', '已送達'
  UNION ALL SELECT 6,  13, 1, '', '已送達'
  UNION ALL SELECT 7,  4,  1, '', '準備中'
  UNION ALL SELECT 8,  5,  2, '', '已送達'
  UNION ALL SELECT 8,  14, 2, '', '已送達'
  UNION ALL SELECT 9,  6,  1, '', '已送達'
  UNION ALL SELECT 9,  15, 2, '', '等待送餐'
  UNION ALL SELECT 10, 7,  2, '', '未備料'
  UNION ALL SELECT 11, 8,  3, '', '已送達'
  UNION ALL SELECT 11, 16, 2, '', '準備中'
  UNION ALL SELECT 12, 9,  1, '', '已送達'
  UNION ALL SELECT 13, 10, 2, '', '已送達'
  UNION ALL SELECT 14, 11, 2, '', '已送達'
  UNION ALL SELECT 15, 12, 3, '', '已送達'
  UNION ALL SELECT 16, 1,  1, '', '已送達'
  UNION ALL SELECT 17, 2,  1, '', '已取消'
) AS d
JOIN meal m ON m.meal_no = d.meal_no
ORDER BY d.orders_no, d.meal_no;

-- 27. 今天訂單的金額：從明細加總（只更新 4～17）
UPDATE orders o
JOIN (
  SELECT orders_no, SUM(od_actual_price) AS total
  FROM orders_details
  WHERE orders_no BETWEEN 4 AND 17
  GROUP BY orders_no
) t ON t.orders_no = o.orders_no
SET o.orders_total = t.total,
    o.orders_discount_total = 0,
    o.orders_actual_price = t.total
WHERE o.orders_no BETWEEN 4 AND 17;

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