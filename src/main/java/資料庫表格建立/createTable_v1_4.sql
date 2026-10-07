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
  emp_password VARCHAR(16) NOT NULL COMMENT '密碼',
  emp_tel VARCHAR(10) NOT NULL COMMENT '本人電話',
  emp_ice VARCHAR(30) COMMENT '緊急聯絡人',
  emp_icetel VARCHAR(10) COMMENT '緊急聯絡人電話',
  emp_add VARCHAR(100) COMMENT '地址',
  emp_sal INTEGER NOT NULL COMMENT '月薪',
  emp_status VARCHAR(2) NOT NULL COMMENT '員工狀態',
  emp_pic MEDIUMBLOB COMMENT '員工照片',
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
  mem_password VARCHAR(16) COMMENT '密碼',
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
    '0985510798',
    '王小明',
    'wangming01',
    'Test1234',
    'wangming01@example.com',
    'AB123456',
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
    'Test5678',
    'chenhua02@example.com',
    'CD234567',
    '不吃香菜',
    '1998-07-22',
    NULL,
    'chenhua02',
    'chenhua02@gmail.com',
    NULL,
    '已驗證'
),
(
    '0985510798',
    '林小美',
    'linmei03',
    'Test9012',
    'linmei03@example.com',
    'EF345678',
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
INSERT INTO promote (promote_name, promote_begin, promote_end, promote_content, promote_img) VALUES
('中秋節活動', '2026-09-20 10:00:00', '2026-10-06 22:00:00',  '中秋節限定商品優惠', NULL),
('雙十節活動', '2026-10-01 10:00:00', '2026-10-31 22:00:00',  '雙十國慶期間限定活動', NULL),
('週年慶活動', '2027-09-01 10:00:00', '2027-09-30 22:00:00',  '週年慶全館商品優惠活動', NULL);

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
INSERT INTO employee (emp_name, emp_password, emp_tel, emp_ice, emp_icetel, emp_add, emp_sal, emp_status, emp_pic) VALUES
('陳小心', '12345678', '0987878787', '', '', '', 30000,'在職',null),
('カイル', '910111213', '0911223344', '愛蜜莉雅', '0987654321', '露格尼卡王國', 40000,'在職',null),
('張學長', '11111111', '0987654321', '', '', '電腦市程式碼街最強社區666號', 100000,'離職',null),
('李木子', 'abcdefgh', '0900111222', '', '', '快樂縣可愛鄉聰明路102號', 40000,'在職',null),
('強滾滾', 'poiuytrew', '0933445566', '張學長', '0987654321', '電腦市程式碼街最強社區666號', 50000, '在職',null);

-- 14. 權限 (permission)
INSERT INTO permission (perm_exp) VALUES
('員工管理'),
('會員管理'),
('菜單管理'),
('活動管理'),
('公告設定'),
('桌型管理'),
('入座管理'),
('訂單管理_廚師'),
('訂單管理_菜口'),
('訂單管理_外場'),
('結帳'),
('預約管理'),
('候位管理'),
('回饋表單管理');

-- 15. 員工權限 (emp_permission)
INSERT INTO emp_permission (emp_no, perm_no) VALUES
(1, 1),
(1, 2),
(1, 3),
(1, 4),
(2, 1),
(3, 2),
(4, 5),
(5, 1);

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