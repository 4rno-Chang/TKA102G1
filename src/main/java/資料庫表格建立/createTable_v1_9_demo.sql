-- createTable_v1_9_demo.sql（05 用，全天任何時間都可以匯入；正常營業時段用 createTable_v1_9.sql）＝ G1 createTable_v1_8.sql（0667d58，含活動整單折扣欄位）＋ 05 全天 demo 資料（第 19～27 節，同 v1_8_demo）
--   ＋ 05 的簡化 demo 資料（第 19～27 節：會員 4～9、正式時段訂位、工作面板 6 張單、1 張待結帳的大單）
--   ＋ 05 修正：promote 1 中秋節活動日期 2025 → 2026（G1 6cf9022 寫成 2025，和 2026-10-01 訂單明細的 promote_no 1 對不上）
--   其餘（建表、1～18 節原始資料、外鍵）和 G1 v1_6 一樣；G1 改 v1_6 時，這份要照上面的規則重新產生
--   以下兩行是 v1_6 原本的說明：
-- createTable_v1_6.sql ＝ G1 createTable_v1_5.sql（4e97aef）＋ 修正 meal 的 meal_type_no（ishaya 拿掉「主打推薦」後要全部減 1）＋ qa 點擊次數改成 demo 用的數字
--   2026-10-15 再修正不合邏輯的資料：訂單 1～3 已結帳但餐點沒送完／金額不對／不在活動期間、回饋時間、16:30 時段、候位電話與號碼順序、訂位成立時間、訂單早於開店
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
  promote_no INT UNSIGNED COMMENT '活動編號',
  orders_time DATETIME COMMENT '下單時間',
  orders_total INT COMMENT '總金額',
  orders_discount_total INT COMMENT '折扣金額',
  orders_actual_price INT COMMENT '實付金額',
  orders_pay VARCHAR(2) COMMENT '付款方式',
  orders_promote_disc TINYINT UNSIGNED COMMENT '整單折數快照',
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
  meal_promo_only BOOLEAN NOT NULL DEFAULT FALSE COMMENT '活動限定菜品',
  PRIMARY KEY(meal_no)
) COMMENT '菜品';

-- 菜品類別, meal_type
CREATE TABLE IF NOT EXISTS meal_type(
  meal_type_no INT UNSIGNED AUTO_INCREMENT COMMENT '類別編號',
  meal_type_name VARCHAR(30) COMMENT '類別名稱',
  meal_type_hashtag VARCHAR(10) COMMENT '標籤代號',
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
  promote_rule VARCHAR(100) COMMENT '活動條件',
  promote_disc TINYINT UNSIGNED COMMENT '活動折扣',
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
  emp_temp_password_expires_at DATETIME(6) NULL COMMENT '臨時密碼到期時間',
  emp_tel VARCHAR(10) NOT NULL COMMENT '本人電話',
  emp_email VARCHAR(254) NULL COMMENT '員工通知信箱',
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
CREATE TABLE IF NOT EXISTS `reservation` (
    `rsv_no` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '訂位編號',
    `mem_no` INT UNSIGNED NOT NULL COMMENT '會員編號',
    `rsv_dt_no` INT UNSIGNED NOT NULL COMMENT '訂位時段編號',
    `seat_type_no` INT NOT NULL COMMENT '桌型編號',
    `rsv_name` VARCHAR(30) DEFAULT NULL COMMENT '訂位聯絡姓名',
    `rsv_create_time` DATETIME DEFAULT NULL COMMENT '訂位成立時間',
    `rsv_status` VARCHAR(2) DEFAULT NULL COMMENT '狀態',
    `rsv_comment` VARCHAR(50) DEFAULT NULL COMMENT '備註',

    PRIMARY KEY (`rsv_no`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='訂位';

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
CREATE TABLE IF NOT EXISTS `member` (
    `mem_no` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '會員編號',
    `mem_tel` CHAR(10) NOT NULL COMMENT '電話',
    `mem_name` VARCHAR(30) DEFAULT NULL COMMENT '姓名',
    `mem_account` VARCHAR(16) DEFAULT NULL COMMENT '帳號',
    `mem_password` VARCHAR(100) DEFAULT NULL COMMENT '密碼',
    `mem_mail` VARCHAR(30) DEFAULT NULL COMMENT 'Email',
    `mem_barcode` CHAR(8) DEFAULT NULL COMMENT '載具條碼',
    `mem_tag` VARCHAR(50) DEFAULT NULL COMMENT '客戶備註',
    `mem_birth` DATE DEFAULT NULL COMMENT '生日',
    `mem_fb` VARCHAR(32) DEFAULT NULL COMMENT '臉書',
    `mem_line` VARCHAR(32) DEFAULT NULL COMMENT 'Line',
    `mem_goo` VARCHAR(32) DEFAULT NULL COMMENT 'Google',
    `mem_x` VARCHAR(32) DEFAULT NULL COMMENT 'X（推特）',
    `mem_status` VARCHAR(3) DEFAULT NULL COMMENT '會員狀態',

    PRIMARY KEY (`mem_no`),
    UNIQUE KEY `uk_member_tel` (`mem_tel`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='會員';

-- 回饋, feedback
CREATE TABLE IF NOT EXISTS feedback(
  feedback_no INT UNSIGNED AUTO_INCREMENT COMMENT '回饋編號',
  orders_no INT UNSIGNED COMMENT '訂單編號',
  fee_customer VARCHAR(30) COMMENT '回饋人',
  fee_customer_tel CHAR(10) COMMENT '回饋人電話',
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
INSERT INTO meal_type (meal_type_name,meal_type_hashtag) VALUES 
('開胃菜','前菜'),
('義大利麵','主餐'),
('燉飯','主餐'),
('經典排餐','主餐'),
('甜點','甜點'),
('飲料','飲料');


-- 2. 插入義式菜品資料 (meal)
-- 05 修正：G1 v1_5 的 meal_type 拿掉了「主打推薦」（只剩 1～6），下面的 meal_type_no 原本是舊編號 2～7，這裡全部減 1；
--         等 G1 的 v1_5 修好後，這幾行就和 G1 一樣
INSERT INTO meal (meal_type_no, meal_name, meal_exp, meal_price, meal_status_en, meal_promo_only) VALUES
(1, '義式烤麵包 Bruschetta', '新鮮番茄粒搭配羅勒與初榨橄欖油', 160, '上架', FALSE),
(1, '酥炸墨魚圈', '附塔塔醬與新鮮黃檸檬塊', 220, '上架', FALSE),
(1, '義式卡布里沙拉', '莫札瑞拉起司搭配新鮮番茄與羅勒醬', 200, '上架', FALSE),
(1, '蒜味奶油烤野菇', '綜合野菇佐蒜味香草奶油醬（蛋奶素）', 180, '上架', FALSE),
(1, '帕瑪火腿哈密瓜卷', '經典鹹甜組合，嚴選帕瑪生火腿搭配甜哈密瓜', 280, '上架', FALSE),
(1, '松露起司薯條', '現炸金黃薯條刨上帕瑪森起司佐黑松露醬（蛋奶素）', 190, '上架', FALSE),
(2, '牛肝菌野菇義大利麵', '香濃牛肝菌醬汁配現削帕馬森起司', 320, '上架', FALSE),
(2, '粉紅醬焗烤筆管麵', '濃郁粉紅醬搭配多種時蔬菜彩（蛋奶素）', 280, '上架', FALSE),
(2, '熱那亞羅勒青醬細扁麵', '自製羅勒青醬搭配烤松子與帕瑪森起司（蛋奶素）', 270, '上架', FALSE),
(2, '經典波隆那肉醬麵', '慢火燉煮傳統牛肉豬肉綜合番茄肉醬', 300, '上架', FALSE),
(2, '奶油卡邦尼培根麵', '經典蛋黃奶油醬搭配香煎培根與黑胡椒', 290, '上架', FALSE),
(2, '威尼斯墨魚汁海鮮麵', '特調墨魚醬汁炒鮮蝦、蛤蜊與透抽', 380, '上架', FALSE),
(2, '香蒜辣味白酒蛤蜊麵', '新鮮蛤蜊搭配蒜片、乾辣椒與白酒香炒', 320, '上架', FALSE),
(3, '經典粉紅醬海鮮燉飯', '濃郁奶油番茄醬汁搭配鮮蝦與干貝', 380, '上架', FALSE),
(3, '松露野菇燉飯', '濃郁黑松露醬與綜合野菇義大利燉飯（蛋奶素）', 360, '上架', FALSE),
(3, '西西里番茄海鮮燉飯', '鮮甜番茄高湯燉煮多種時令海鮮', 390, '上架', FALSE),
(3, '拿坡里牛肝菌菇燉飯', '嚴選義大利牛肝菌菇高湯燉煮（蛋奶素）', 380, '上架', FALSE),
(4, '托斯卡尼獵人風燉雞', '慢火燉煮去骨雞腿肉，搭番茄、黑橄欖、彩椒與紅酒醬汁', 420, '上架', FALSE),
(4, '巴薩米克櫻桃鴨胸', '宜蘭櫻桃鴨胸香煎至粉紅熟度，淋上巴薩米克黑醋櫻桃醬', 580, '上架', FALSE),
(4, '羅馬香草烤豬腳', '外皮香酥大豬腳，搭配自製義式酸菜與粒醬芥末', 620, '上架', FALSE),
(4, '西西里香草烤嫩羊肩排', '紐西蘭羊肩排搭配新鮮薄荷青醬與香草麵包粉烘烤', 680, '上架', FALSE),
(4, '地中海煎烤海鱸魚排', '新鮮海鱸魚菲力香煎，搭配蒔蘿鮮蝦起司醬與烤時蔬', 520, '上架', FALSE),
(4, '波隆那香煎小牛肉佐帕瑪火腿', '嫩小牛肉片疊上帕瑪火腿與鼠尾草，以白酒黃油煎香', 560, '上架', FALSE),
(4, '拿坡里香辣奶油大蝦排', '野生大草蝦搭配蒜香辣椒奶油，附烤法式麵包沾醬', 480, '上架', FALSE),
(4, '主廚手作黑松露烤大褐菇排', '巨型大褐菇鑲黑松露時蔬內餡，覆蓋莫札瑞拉起司烘烤（蛋奶素）', 380, '上架', TRUE),
(4, '米蘭式巴薩米克燉小牛膝', '米蘭百年名菜 Ossobuco，牛膝燉至軟嫩搭義式香草檸檬醬', 720, '未上架', FALSE),
(5, '主廚手工提拉米蘇', '濃郁馬斯卡彭起司與浸泡咖啡酒的手指餅乾', 160, '上架', FALSE),
(5, '義式香草奶酪', '附野生莓果醬', 120, '上架', FALSE),
(5, '香草熔岩巧克力蛋糕', '切開爆漿熱巧克力佐香草冰淇淋（蛋奶素）', 160, '上架', FALSE),
(5, '檸檬塔', '新鮮黃檸檬內餡與香酥塔皮（蛋奶素）', 140, '上架', FALSE),
(5, '義式香草冰淇淋', '傳統手工低脂香草冰淇淋雙球（蛋奶素）', 100, '上架', FALSE),
(5, '焦糖烤布蕾', '現烤香脆焦糖蓋上濃郁蛋黃布丁（蛋奶素）', 120, '上架', FALSE),
(5, '開心果巴斯克乳酪蛋糕', '濃郁開心果醬搭配綿密乳酪蛋糕（蛋奶素）', 170, '上架', FALSE),
(5, '綜合野莓派', '酸甜綜合野莓內餡香脆甜派（蛋奶素）', 150, '未上架', FALSE),
(6, '西西里檸檬氣泡咖啡', '現粹濃縮咖啡搭配西西里檸檬汁', 130, '未上架', FALSE),
(6, '義式濃縮咖啡', '', 90, '上架', FALSE),
(6, '氣泡礦泉水', '原裝進口瓶裝氣泡水', 100, '上架', FALSE),
(6, '經典氣泡紅白酒', '義大利精選佐餐紅白酒（單杯）', 220, '上架', FALSE),
(6, '托斯卡尼精釀啤酒', '清爽麥香與微苦啤酒花香，適合搭配肉類', 180, '上架', FALSE),
(6, '義式微甜氣泡酒', '帶有花果香氣的 Moscato 微甜氣泡酒', 250, '上架', FALSE),
(4, '雙人浪漫限定套餐', '含開胃菜、主餐兩份、特製甜點與精選氣泡酒雙人份', 1980, '未上架', FALSE),
(4, '尊榮父親節特別套餐', '包含香草烤豬腳、羊肩排大拼盤與特調飲品雙人份', 2280, '未上架', FALSE),
(2, '粉紅清甜檸香鮮蝦細扁麵', '春季限定！自製粉紅醬搭配鮮甜白蝦與西西里檸檬微酸清爽感', 360, '未上架', TRUE),
(3, '綠竹筍嫩雞佐松露翡翠燉飯', '選用當季新鮮綠竹筍與去骨雞腿肉，搭配波菜翡翠高湯燉煮', 420, '未上架', TRUE);


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
('A01', 1, '2026-10-16 18:30:00', 700, 40, 660, '刷卡'),
('B01', 2, '2026-10-16 19:15:00', 980, 50, 930, '刷卡'),
('C01', 3, '2026-10-16 19:40:00', 400, 0, 400, '現金');

-- 7. 活動 (promote)
INSERT INTO promote (promote_name, promote_begin, promote_end, promote_content, promote_rule, promote_disc) VALUES
('中秋節活動', '2026-09-20 10:00:00', '2026-10-06 22:00:00',  '中秋節限定商品優惠，數量有限送完即止', null, null),
('2026生日禮', '2026-01-01 00:00:00', '2026-06-30 23:59:59', '2026年上半年限定，生日當月出示身份證，即可享當月生日禮提拉米蘇一份。', null, null),
('情人節套餐', '2026-03-10 11:00:00', '2026-03-15 22:00:00', '浪漫情人節，餐庭提供雙人浪漫限定套餐，伴隨燭光。讓您跟另一半獲得最好的體驗', null, null),
('母親節感恩祭', '2026-05-01 00:00:00', '2026-05-15 23:59:59', '溫馨母親節，全家帶媽媽來店用餐~來店拍照打卡即享全單 8 折。','和媽咪拍照打卡',80),
('畢業季優惠', '2026-06-01 00:00:00', '2026-06-30 23:59:59', '憑學生證或畢業證書，可獲得抽獎資格。將會在6月30日抽出1萬元現金，若無得獎也可憑抽獎券回來兌換安慰獎糖果一份', null, null),
('父親節特別獻禮', '2026-08-01 00:00:00', '2026-08-10 23:59:59', '爸爸節，辛苦爸爸感念爸爸！本店祭出父親專屬優惠，提供父親節特別套餐。不來可惜！', null, null),
('中秋慶團圓', '2026-09-15 00:00:00', '2026-09-25 23:59:59', '中秋節期間消費每滿500即贈品牌純手工限量月餅1份，可疊加。贈完為止。', null, null),
('秋季溫暖祭', '2026-10-05 10:00:00', '2026-10-20 22:00:00', '點購指定湯品或熱飲，即贈送每日精選甜點乙份。', null, null),
('雙十連假特惠', '2026-10-07 00:00:00', '2026-10-12 23:59:59', '連假期間來店贈送可愛小國旗，一起慶國慶。', null, null),
('雙十節活動', '2026-10-07 00:00:00', '2026-10-31 22:00:00', '雙十國慶期間限定活動，酥炸墨魚圈、卡布里沙拉半價優惠。一直以來都只吃主餐就走了嗎？不訪來吃吃看吧', null, null),
('跨年特別企劃', '2026-12-31 18:00:00', '2027-01-01 02:00:00', '跨年夜限定酒精飲品暢飲活動。跨年夜狂歡夜，讓我們今日不醉不歸', null, null),
('春季新菜上市', '2027-03-01 00:00:00', '2027-03-31 23:59:59', '春季新品主餐登場，嘗鮮價享有 85 折優惠，走過路過千萬別錯過。', null, null),
('週年慶活動', '2027-09-01 10:00:00', '2027-09-30 22:00:00',  '週年慶主廚推薦菜品享優惠活動，我們主廚有著多年老經驗，推薦菜品一等一，請務必要來嘗嘗', null, null),
('聖誕週活動', '2026-10-19 10:00:00', '2026-10-25 22:00:00',  '找不到地方聖誕節讓我們一起開心度過，來店和聖誕樹拍照打卡即可享有75折優惠！', '和店內聖誕樹拍照打卡上傳至任一平台', 75);

-- 8. 活動商品 (promote_meal)
INSERT INTO promote_meal (promote_no, meal_no, promote_discount) VALUES
(1, 1, 120),
(1, 3, 150),
(1, 19, 500),
(2, 27, 0),
(3, 41, NULL),
(4, 7, 272),
(4, 10, 255),
(4, 14, 323),
(4, 15, 306),
(6, 29, 0),
(7, 40, NULL),
(9, 18, 357),
(9, 22, 442),
(9, 23, 476),
(10, 2, 110),
(10, 3, 100),
(11, 39, 0),
(11, 40, 0),
(11, 38, 0),
(12, 43, 306),
(12, 44, 357),
(13, 6, 150),
(13, 27, null),
(13, 25, null);

-- 9. 訂單明細 (orders_details)
-- 狀態統一用：製作中／等待送餐／已送達／已取消；還沒送達的送達時間填 NULL
INSERT INTO orders_details (orders_no, meal_no, promote_no, od_meal_num, od_discount_price, od_discount_total, od_actual_price, od_comment, od_status, od_served_at) VALUES
-- 訂單 1 (A01)：烤麵包套用中秋特價 120
(1,  1,    1, 1, 160, 40, 120, '麵包要烤酥一點', '已送達',   '2026-10-01 18:42:00'),
(1, 14, NULL, 1, 380,  0, 380, '飯軟一點',       '已送達',   '2026-10-01 18:55:00'),
(1, 37, NULL, 1, 100,  0, 100, '少冰',           '已取消',   NULL),
(1, 29, NULL, 1, 160,  0, 160, '',               '已送達',   '2026-10-01 19:05:00'),
-- 訂單 2 (B01)：卡布里沙拉套用中秋特價 150
(2,  3,    1, 1, 200, 50, 150, '',               '已送達',   '2026-10-01 19:27:00'),
(2, 21, NULL, 1, 680,  0, 680, '三分熟',         '已送達',   '2026-10-01 19:45:00'),
(2, 37, NULL, 1, 100,  0, 100, '加冰塊與檸檬片', '已送達',   '2026-10-01 19:35:00'),
-- 訂單 3 (C01)：已結帳，餐點都送完了
(3, 10, NULL, 1, 300,  0, 300, '起司多一點',     '已送達',   '2026-10-01 19:58:00'),
(3, 37, NULL, 1, 100,  0, 100, '去冰',           '已送達',   '2026-10-01 19:50:00');

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
    (2, NULL, '0912345678', '王小明', '靠窗座位', '已入座', '18:10:00'),   -- 05 修正：電話改成王小明自己的（原本是陳小華的）
    (4, NULL, '0922100001', '陳美玲', '4位用餐', '取消', '18:20:00'),
    (6, NULL, '0922100002', '林志豪', '慶生聚餐', '已入座', '18:30:00');

-- 11. 訂位時段 (reservation_datetime)
INSERT INTO reservation_datetime (
    rsv_dt_datetime
)
VALUES
    ('2026-08-25 11:00:00'),
    ('2026-08-25 12:30:00'),
    ('2026-08-25 17:00:00'),
    ('2026-08-25 18:30:00');   -- 05 修正：原本 16:30 不是正式時段（11:00、12:30、17:00、18:30）

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
-- 五位測試員工皆模擬已設定正式密碼，測試密碼為 Test1234；在職者登入不須強制改密碼，3號仍停用不可登入。
-- カイル、張學長與李木子已填入通知信箱；其餘員工 emp_email 暫為 NULL，需補齊才能寄送重設通知。
INSERT INTO employee
(emp_name, emp_password, emp_tel, emp_email, emp_pwd_changed,
 emp_temp_password_expires_at, emp_ice,
 emp_icetel, emp_add, emp_sal, emp_status, emp_photo)
VALUES
('陳小心',
 '{bcrypt}$2a$10$g2db.AEZVThNjtzx7H4Wn.yZ8Hd1.M3T6FtcxfVuEK5XR7sTFPjNK',
 '0987878787', NULL, TRUE, NULL, '', '', '', 30000, '1', NULL),

('カイル',
 '{bcrypt}$2a$10$g2db.AEZVThNjtzx7H4Wn.yZ8Hd1.M3T6FtcxfVuEK5XR7sTFPjNK',
 '0911223344', 'kyle880729@gmail.com', TRUE, NULL, '愛蜜莉雅', '0987654321', '露格尼卡王國', 40000, '1', NULL),

('張學長',
 '{bcrypt}$2a$10$g2db.AEZVThNjtzx7H4Wn.yZ8Hd1.M3T6FtcxfVuEK5XR7sTFPjNK',
 '0987654321', 'kyle880729@gmail.com', TRUE, NULL, '', '', '電腦市程式碼街最強社區666號', 100000, '2', NULL),

('李木子',
 '{bcrypt}$2a$10$g2db.AEZVThNjtzx7H4Wn.yZ8Hd1.M3T6FtcxfVuEK5XR7sTFPjNK',
 '0900111222', 'simpson200023@gmail.com', TRUE, NULL, '', '', '快樂縣可愛鄉聰明路102號', 40000, '1', NULL),

('強滾滾',
 '{bcrypt}$2a$10$g2db.AEZVThNjtzx7H4Wn.yZ8Hd1.M3T6FtcxfVuEK5XR7sTFPjNK',
 '0933445566', NULL, TRUE, NULL, '張學長', '0987654321',
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
(2, 3),
(3, 2), 
(4, 3), 
(5, 6); 

-- 16. 回饋 (feedback)
-- 05 修正：用餐時間＝那張訂單的下單時間，回饋時間在用餐之後；A01 是 2 人桌，所以 3 筆回饋分給 3 張單；
--         內容對上那張單的菜（訂單 2 是羊排、訂單 3 是肉醬麵）
INSERT INTO feedback (orders_no, fee_customer, fee_customer_tel, fee_time, fee_content, fee_rating) VALUES
(1, '顧客A', '0985510798', '2026-10-01 21:10:00', '餐點非常美味，服務親切！', 4),
(2, '顧客B', '0985510798', '2026-10-02 10:20:00', '氣氛很好，會再光顧。', 2),
(3, '顧客C', '0985510798', '2026-10-02 12:05:00', '出餐稍微慢了點。', 5),
(2, '顧客D', '0985510798', '2026-10-02 19:40:00', '羊排熟度稍微過熟。', 3),
(3, '顧客E', '0985510798', '2026-10-03 19:40:00', '肉醬麵送來時有點冷掉，希望改進。', 1);

-- 17. 推播 (announcement)
INSERT INTO announcement (ann_title, ann_begin, ann_img, ann_text) VALUES
('每週一固定公休', '2026-09-01 10:00:00', NULL,
 '本店每週一固定公休，週二至週日營業。午餐 11:00～14:00、晚餐 17:00～21:00，最後點餐為打烊前 30 分鐘。'),
('雙十連假正常營業', '2026-10-05 10:00:00', NULL,
 '10/10～10/12 國慶連假期間正常營業，週一（10/12）公休順延至 10/13（二）。連假人潮較多，建議提前線上訂位或現場取號候位。'),
('跨年夜營業時間調整', '2026-12-01 10:00:00', NULL,
 '12/31 跨年夜延長營業至 01:30，晚餐時段僅接受訂位，不開放現場候位；1/1（五）元旦店休一天。');


-- 18. QA (qa)：1、2 是 G1 原本的題目，3～11 是 05 新增
--     qa_clicks 是 demo 用的點擊次數（首頁顯示點擊最多的 5 題）：1～6 題是 1～6 次；7～11 題都是 7 次，
--     次數一樣時新的（編號大）排前面 → 首頁是 11、10、9、8、7；展開第 7 題一次就會跳到第 1 名
INSERT INTO qa (qa_title, qa_content, qa_clicks) VALUES
('請問有提供素食餐點嗎？', '我們提供蛋奶素及全素的義大利麵與披薩，歡迎向服務人員諮詢。', 1),
('訂位最多可以接受幾人？', '線上訂位最多支援6人桌型，6人以上團體用餐請來電預約。', 2),
('線上訂位需要先登入會員嗎？', '需要。請先登入會員再進行訂位；尚未註冊的顧客可以使用手機號碼註冊會員。', 3),
('可以預約多久以後的日期？', '開放預約含今日起算90天內的日期。', 4),
('有哪些用餐時段可以預約？', '午餐 11:00、12:30，晚餐 17:00、18:30。當天已經開始的時段無法預約。', 5),
('一次可以訂多張桌子嗎？', '每次限訂1桌（2人、4人或6人桌）；多桌或包場需求請來電洽詢。', 6),
('訂位後想更改或取消怎麼辦？', '請在「會員中心」取消尚未開始的訂位；日期、時段或桌型需要更換時，請取消後重新訂位。', 7),
('沒有訂位可以現場候位嗎？', '可以，請至現場領取號碼牌。上午最晚發放至12:30，下午最晚發放至18:30，叫號進度可以在「候位狀況」頁面查看。', 7),
('候位可以取消或更換桌型嗎？', '可以在「候位狀況」頁面的「查詢候位」輸入手機取消候位；桌型無法線上修改，如需更換請取消後重新取號。', 7),
('結帳可以使用哪些付款方式？', '目前提供現金及刷卡付款。', 7),
('用餐時間有限制嗎？', '每組用餐時間以90分鐘為原則，敬請配合。', 7);

/* =========================================================
   以下是 05 的 demo 資料（19～27，簡化版：全組 demo 用）
   - 上面 1～18 節的原始資料不改，這裡只 INSERT；唯一的例外是第 23 節把原始的 A01、B01、C01 清成可使用
   - 時間從「執行這份 SQL 的當下」推算，任何時間都可以匯入（demo 前重新匯入一次）：
       營業中（11:00～21:00）：入座、下單照現在往前推，不早於今天 11:00
       營業時間外：使用中的桌子都是 5～25 分鐘前入座，不跨日、不會時間異常，前台可以點餐
       稍早已結帳的兩張單：開店 3 小時以上照現在往前推；否則放在最近一個營業日的 12:10、18:40
   - 編號接在原始資料後面：member 4～9、reservation_datetime 5～10、reservation 4～12、waiting 4～7、orders 4～12
   - 會員 4～9 前台登入密碼都是 Test0004
   - 寫法：INSERT … VALUES、INSERT … SELECT … JOIN、UPDATE … JOIN；JOIN 要讀前面新增的資料，順序不能調換
   ---------- 匯入後馬上看，應該是這樣 ----------
   工作面板（廚房）：6 張卡片（訂單 4～9，都有製作中的餐點），可以左右滑；訂單 6 有一道等待送餐
   結帳：訂單 10（C02，9 道菜，全部已送達）顯示「待結帳」；訂單 4～9 顯示「用餐中」
   入座管理：使用中 A02 A03 A04 A05 B02 B03 C02（A05 訂位者使用，其他其它者使用），其他 10 桌可使用
     → 在結帳頁把訂單 10 結帳，入座管理重新整理（或最慢 1 分鐘）C02 就自動變回可使用
   訂位：現在前後的時段（約 −90、−50、−20、+30、+60、+150 分）
     4 人桌逾時未到 1（訂位: 678）；2 人桌需保留 1（陳媽媽: 798）、6 人桌需保留 1（訂位: 009）；+150 分那筆不算保留
   ========================================================= */

-- 以 Java 時鐘為主：NOW() 一律用台灣時間，跟入座管理（Java）的「現在」一致
SET time_zone = '+08:00';

-- 現在（去掉秒）
SET @now = NOW() - INTERVAL SECOND(NOW()) SECOND;
-- 營業中（11:00～21:00）？入座、下單時間不能早於 @floor：營業中是今天 11:00，營業時間外是今天 00:00（不跨日）
SET @biz = (TIME(@now) >= '11:00:00' AND TIME(@now) < '21:00:00');
SET @floor = IF(@biz, TIMESTAMP(CURDATE(), '11:00:00'), TIMESTAMP(CURDATE(), '00:00:00'));
-- 稍早已結帳的兩張單：營業中且已開店 3 小時以上 → 照現在往前推（@recent）；
-- 否則放在最近一個營業日的 12:10、18:40（21:00 後算今天，其他算昨天）
SET @recent = (@biz AND CAST(@now AS DATETIME) - INTERVAL 180 MINUTE >= TIMESTAMP(CURDATE(), '11:00:00'));
SET @hist = IF(TIME(@now) >= '21:00:00', CURDATE(), CURDATE() - INTERVAL 1 DAY);
SET @pwd = '{bcrypt}$2a$10$UCU/PUmq7rIyr0fLtWD2teTjzAuaS1DMu.6JKqV83FsHPGsmclcrG';   -- 明碼 Test0004

-- 19. 測試會員 (member 4～9)；會員 9 只用手機註冊、沒填姓名
INSERT INTO member (mem_no, mem_tel, mem_name, mem_account, mem_password, mem_mail, mem_barcode, mem_tag, mem_birth, mem_fb, mem_line, mem_goo, mem_x, mem_status) VALUES
(4, '0911000004', '黃大偉', 'huangdw04', @pwd, 'huangdw04@example.com', '/GH45678', NULL,             '1990-01-20', NULL, NULL, NULL, NULL, '已驗證'),
(5, '0911000005', '吳佩琪', 'wupq05',    @pwd, 'wupq05@example.com',    '/IJ56789', '對海鮮過敏',     '1993-05-02', NULL, NULL, NULL, NULL, '已驗證'),
(6, '0911000006', '周子翔', 'zhouzx06',  @pwd, 'zhouzx06@example.com',  '/KL67890', NULL,             '1988-09-14', NULL, NULL, NULL, NULL, '已驗證'),
(7, '0911000007', '陳志明', 'chenzm07',  @pwd, 'chenzm07@example.com',  '/MN11111', NULL,             '1985-02-11', NULL, NULL, NULL, NULL, '已驗證'),
(8, '0911000008', '林雅婷', 'linyt08',   @pwd, 'linyt08@example.com',   '/MN22222', '素食（蛋奶素）', '1992-06-30', NULL, NULL, NULL, NULL, '已驗證'),
(9, '0911000009', NULL,     NULL,        @pwd, NULL,                    NULL,       NULL,             NULL,         NULL, NULL, NULL, NULL, '未驗證');

-- 20. 訂位時段 (reservation_datetime 5～10)：demo 版不用正式時段，改用「現在」前後的時段，任何時間匯入都看得到
--     逾時未到、需保留（90 分鐘內）、90 分鐘外；@t0＝現在取到 5 分鐘，時段一律落在今天 00:00～23:55（不跨日）
--     （正式時段 11:00、12:30、17:00、18:30 由程式啟動時自己補齊，前台訂位一樣只能選正式時段）
SET @t0 = CAST(@now AS DATETIME) - INTERVAL MOD(MINUTE(@now), 5) MINUTE;
INSERT INTO reservation_datetime (rsv_dt_no, rsv_dt_datetime)
SELECT s.no, LEAST(GREATEST(@t0 + INTERVAL s.mins MINUTE, TIMESTAMP(CURDATE(), '00:00:00')), TIMESTAMP(CURDATE(), '23:55:00'))
FROM (
            SELECT 5 AS no, -90 AS mins   -- 已經過了：實到、未到
  UNION ALL SELECT 6,  -50                -- 已經過了：實到、取消
  UNION ALL SELECT 7,  -20                -- 剛過：實到（坐在 A05）、逾時未到
  UNION ALL SELECT 8,   30                -- 90 分鐘內 → 需保留
  UNION ALL SELECT 9,   60                -- 90 分鐘內 → 需保留
  UNION ALL SELECT 10, 150                -- 90 分鐘外 → 不算保留
) AS s;

-- 21. 訂位 (reservation 4～12)：時段已經過了用 past_status、還沒到用 future_status
--     past_status 是「預約」＝時間過了還沒來 → 入座管理顯示「逾時未到」
--     成立時間一定早於時段（至少 1 小時前）
--     rsv_name＝訂位聯絡姓名（CJ 的前台訂位時填，可以不填）：CJ 的訂位清單只看它，沒填顯示「未填寫」；
--     入座管理顯示「聯絡姓名: 電話末 3 碼」（陳媽媽: 798），沒填顯示「訂位: 電話末 3 碼」。還沒到的 9、10、11 示範填了和沒填
INSERT INTO reservation (rsv_no, mem_no, rsv_dt_no, seat_type_no, rsv_create_time, rsv_status, rsv_comment, rsv_name)
SELECT r.rsv_no, r.mem_no, r.rsv_dt_no, r.seat_type_no,
       LEAST(CAST(@now AS DATETIME) - INTERVAL 1 DAY, d.rsv_dt_datetime - INTERVAL 1 HOUR),
       IF(d.rsv_dt_datetime <= CAST(@now AS DATETIME), r.past_status, r.future_status), r.rsv_comment, r.rsv_name
FROM (
            SELECT 4 AS rsv_no, 4 AS mem_no, 5 AS rsv_dt_no, 2 AS seat_type_no, '實到' AS past_status, '預約' AS future_status, '2人用餐' AS rsv_comment, '黃大偉' AS rsv_name
  UNION ALL SELECT 5,  5, 5, 4, '未到', '預約', '4人聚餐',         '吳佩琪'
  UNION ALL SELECT 6,  6, 6, 2, '實到', '預約', '2人用餐',         '周子翔'
  UNION ALL SELECT 7,  7, 6, 2, '取消', '取消', '臨時有事',        '陳志明'
  UNION ALL SELECT 8,  8, 7, 2, '實到', '預約', '2人用餐',         '林雅婷'   -- 實到 → 第 24 節 A05 是「訂位者使用」
  UNION ALL SELECT 9,  1, 7, 4, '預約', '預約', '4人用餐',         NULL       -- 逾時未到；沒填 → 入座管理顯示 訂位: 678（王小明的電話）
  UNION ALL SELECT 10, 2, 8, 2, '預約', '預約', '2人，幫媽媽訂位', '陳媽媽'   -- 需保留；入座管理顯示 陳媽媽: 798（陳小華的電話），CJ 的訂位清單顯示 陳媽媽
  UNION ALL SELECT 11, 9, 9, 6, '預約', '預約', '6人慶生',         NULL       -- 需保留；沒填 → 入座管理顯示 訂位: 009
  UNION ALL SELECT 12, 3, 10, 2, '預約', '預約', '2人用餐',        '林小美'   -- 90 分鐘外，不算需保留
) AS r
JOIN reservation_datetime d ON d.rsv_dt_no = r.rsv_dt_no;   -- JOIN：查時段時間，過了用 past_status、還沒到用 future_status

-- 22. 今天的候位 (waiting 4～7)：號碼照取號時間（最早取的是 1 號）
INSERT INTO waiting (waiting_no, seat_type_no, mem_no, waiting_tel, waiting_name, waiting_comment, waiting_status, waiting_notify_time, waiting_date, waiting_take_time, waiting_ticket_seq) VALUES
(4, 2, NULL, '0933111222', '吳先生', '2位',       '已入座', TIME(GREATEST(CAST(@now AS DATETIME) - INTERVAL 20 MINUTE, CAST(@floor AS DATETIME))), CURDATE(), GREATEST(CAST(@now AS DATETIME) - INTERVAL 25 MINUTE, CAST(@floor AS DATETIME)), 1),
(5, 2, NULL, '0912555666', '張先生', '2位',       '已通知', TIME(GREATEST(CAST(@now AS DATETIME) - INTERVAL 2 MINUTE,  CAST(@floor AS DATETIME))), CURDATE(), GREATEST(CAST(@now AS DATETIME) - INTERVAL 15 MINUTE, CAST(@floor AS DATETIME)), 2),
(6, 2, 5,    '0911000005', '吳佩琪', '2位，會員', '候位中', NULL,                                                                       CURDATE(), GREATEST(CAST(@now AS DATETIME) - INTERVAL 8 MINUTE,  CAST(@floor AS DATETIME)), 3),
(7, 4, NULL, '0933555666', '趙先生', '4位',       '候位中', NULL,                                                                       CURDATE(), GREATEST(CAST(@now AS DATETIME) - INTERVAL 5 MINUTE,  CAST(@floor AS DATETIME)), 1);

-- 23. 桌位：原始資料的 A01、B01、C01 清成可使用
--     B01、C01 的 seat_time 只有時間（18:30、19:00）沒有日期，白天 demo 時入座管理會判成「入座時間比現在還晚」
UPDATE seat SET seat_status = '可使用', seat_time = NULL WHERE seat_no IN ('A01', 'B01', 'C01');

-- 24. 桌位：補齊到平面圖上的 17 桌（2 人桌 A01～A10、4 人桌 B01～B05、6 人桌 C01～C02）
--     使用中的都是現場客（其它者使用）。入座時間：營業中往前推 biz_min 分、營業時間外只推 off_min 分，都不早於 @floor
--     demo_seat 是匯入時用的暫存表（第 25 節下單時間要用同一個入座時間），最後會刪掉
CREATE TEMPORARY TABLE demo_seat (seat_no CHAR(3) PRIMARY KEY, seat_type_no INT, seated_at DATETIME, order_after INT);
INSERT INTO demo_seat (seat_no, seat_type_no, seated_at, order_after)
SELECT s.seat_no, s.seat_type_no,
       GREATEST(CAST(@now AS DATETIME) - INTERVAL IF(@biz, s.biz_min, s.off_min) MINUTE, CAST(@floor AS DATETIME)),
       s.order_after
FROM (
            SELECT 'A02' AS seat_no, 2 AS seat_type_no, 40 AS biz_min, 20 AS off_min, 5 AS order_after
  UNION ALL SELECT 'A03', 2, 35, 18, 4
  UNION ALL SELECT 'A04', 2, 30, 15, 5
  UNION ALL SELECT 'A05', 2, 15, 5,  3
  UNION ALL SELECT 'B02', 4, 45, 22, 5
  UNION ALL SELECT 'B03', 4, 25, 10, 5
  UNION ALL SELECT 'C02', 6, 75, 25, 3
) AS s;

INSERT INTO seat (seat_no, seat_type_no, seat_status, seat_time)
SELECT seat_no, seat_type_no, IF(seat_no = 'A05', '訂位者使用', '其它者使用'), TIME(seated_at) FROM demo_seat;   -- A05＝訂位 #8 林雅婷（實到）
INSERT INTO seat (seat_no, seat_type_no, seat_status, seat_time) VALUES
('A06', 2, '可使用', NULL),
('A07', 2, '可使用', NULL),
('A08', 2, '可使用', NULL),
('A09', 2, '可使用', NULL),
('A10', 2, '可使用', NULL),
('B04', 4, '可使用', NULL),
('B05', 4, '可使用', NULL);

-- 25. 訂單 (orders 4～12)：orders_pay 是 NULL＝還沒結帳；金額先填 0，27 再從明細加總
--     4～10 的下單時間＝入座後 order_after 分（不晚於現在）
INSERT INTO orders (orders_no, seat_no, member_no, orders_time, orders_total, orders_discount_total, orders_actual_price, orders_pay)
SELECT o.orders_no, o.seat_no, o.member_no,
       LEAST(d.seated_at + INTERVAL d.order_after MINUTE, CAST(@now AS DATETIME)), 0, 0, 0, NULL
FROM (
  -- 工作面板的 6 張卡片：現在在座、還有餐點在製作中
            SELECT 4 AS orders_no, 'A02' AS seat_no, 1 AS member_no
  UNION ALL SELECT 5,  'A03', NULL
  UNION ALL SELECT 6,  'A04', NULL
  UNION ALL SELECT 7,  'B02', 2
  UNION ALL SELECT 8,  'B03', NULL
  UNION ALL SELECT 9,  'A05', NULL
  -- 明細最多的一張：餐點都送完了、還沒結帳 → 結帳頁「待結帳」，結帳後入座管理自動清桌
  UNION ALL SELECT 10, 'C02', NULL
) AS o
JOIN demo_seat d ON d.seat_no = o.seat_no;   -- JOIN：讀同一桌的入座時間

-- 稍早用過餐、已結帳（桌子已經空了）：@recent 時照現在往前推 180、150 分；否則放在 @hist 那天的 12:10、18:40
INSERT INTO orders (orders_no, seat_no, member_no, orders_time, orders_total, orders_discount_total, orders_actual_price, orders_pay) VALUES
(11, 'B04', 4,    IF(@recent, CAST(@now AS DATETIME) - INTERVAL 180 MINUTE, TIMESTAMP(@hist, '12:10:00')), 0, 0, 0, '刷卡'),
(12, 'A07', NULL, IF(@recent, CAST(@now AS DATETIME) - INTERVAL 150 MINUTE, TIMESTAMP(@hist, '18:40:00')), 0, 0, 0, '現金');

-- 26. 訂單明細：單價從 meal 表讀；狀態照 G1：製作中／等待送餐／已送達／已取消
--     只用上架、非活動限定的菜；已送達的送達時間＝下單後 mins 分鐘（不晚於現在），其他是 NULL
INSERT INTO orders_details (orders_no, meal_no, promote_no, od_meal_num, od_discount_price, od_discount_total, od_actual_price, od_comment, od_status, od_served_at)
SELECT d.orders_no, m.meal_no, NULL, d.qty, m.meal_price, 0, m.meal_price * d.qty, d.cmt, d.st,
       CASE WHEN d.st = '已送達' THEN LEAST(o.orders_time + INTERVAL d.mins MINUTE, CAST(@now AS DATETIME)) ELSE NULL END
FROM (
            SELECT 4 AS orders_no, 1 AS meal_no, 1 AS qty, '' AS cmt, '已送達' AS st, 10 AS mins
  UNION ALL SELECT 4,  11, 1, '',           '製作中',   0
  UNION ALL SELECT 5,  7,  1, '',           '製作中',   0
  UNION ALL SELECT 5,  15, 1, '少鹽',       '製作中',   0
  UNION ALL SELECT 6,  3,  1, '',           '等待送餐', 0
  UNION ALL SELECT 6,  18, 1, '',           '製作中',   0
  UNION ALL SELECT 7,  2,  1, '',           '已送達',   12
  UNION ALL SELECT 7,  37, 2, '',           '已送達',   5
  UNION ALL SELECT 7,  20, 1, '',           '製作中',   0
  UNION ALL SELECT 8,  8,  1, '',           '製作中',   0
  UNION ALL SELECT 8,  14, 1, '',           '製作中',   0
  UNION ALL SELECT 8,  22, 1, '醬汁另外放', '製作中',   0
  UNION ALL SELECT 9,  13, 1, '不要辣',     '製作中',   0
  -- 訂單 10（C02，6 人）：9 道菜，全部已送達
  UNION ALL SELECT 10, 1,  2, '',           '已送達',   8
  UNION ALL SELECT 10, 3,  1, '',           '已送達',   10
  UNION ALL SELECT 10, 6,  1, '',           '已送達',   12
  UNION ALL SELECT 10, 9,  1, '',           '已送達',   25
  UNION ALL SELECT 10, 12, 1, '',           '已送達',   27
  UNION ALL SELECT 10, 16, 1, '',           '已送達',   30
  UNION ALL SELECT 10, 21, 2, '一份五分熟', '已送達',   35
  UNION ALL SELECT 10, 29, 3, '',           '已送達',   50
  UNION ALL SELECT 10, 36, 3, '',           '已送達',   55
  -- 稍早已結帳的兩張
  UNION ALL SELECT 11, 4,  1, '',           '已送達',   10
  UNION ALL SELECT 11, 17, 2, '',           '已送達',   25
  UNION ALL SELECT 11, 33, 2, '',           '已送達',   50
  UNION ALL SELECT 12, 5,  1, '',           '已送達',   10
  UNION ALL SELECT 12, 19, 1, '',           '已送達',   25
) AS d
JOIN meal m ON m.meal_no = d.meal_no            -- JOIN：從 meal 讀單價（不寫死，G1 改價格也會跟著變）
JOIN orders o ON o.orders_no = d.orders_no      -- JOIN：讀下單時間，推算已送達的時間
ORDER BY d.orders_no, d.mins, d.meal_no;

-- 27. 訂單金額：從明細加總（只更新上面新增的 4～12；已取消的不算）
-- JOIN：把第 26 節的明細加總，寫回訂單的金額
UPDATE orders o
JOIN (
  SELECT orders_no, SUM(CASE WHEN od_status = '已取消' THEN 0 ELSE od_actual_price END) AS total
  FROM orders_details
  WHERE orders_no BETWEEN 4 AND 12
  GROUP BY orders_no
) t ON t.orders_no = o.orders_no
SET o.orders_total = t.total,
    o.orders_discount_total = 0,
    o.orders_actual_price = t.total
WHERE o.orders_no BETWEEN 4 AND 12;

DROP TEMPORARY TABLE demo_seat;

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

-- 訂單 → 桌位、會員、活動
ALTER TABLE orders ADD CONSTRAINT orders_seat_fk FOREIGN KEY (seat_no) REFERENCES seat(seat_no);
ALTER TABLE orders ADD CONSTRAINT orders_member_fk FOREIGN KEY (member_no) REFERENCES member(mem_no);
ALTER TABLE orders ADD CONSTRAINT orders_promote_fk FOREIGN KEY (promote_no) REFERENCES promote (promote_no);

-- 回饋 → 訂單
ALTER TABLE feedback ADD CONSTRAINT feedback_orders_fk FOREIGN KEY (orders_no) REFERENCES orders(orders_no);

-- 訂單明細 → 訂單、菜品
ALTER TABLE orders_details ADD CONSTRAINT orders_details_orders_fk FOREIGN KEY (orders_no) REFERENCES orders(orders_no);
ALTER TABLE orders_details ADD CONSTRAINT orders_details_meal_fk FOREIGN KEY (meal_no) REFERENCES meal(meal_no);


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