-- KEYS[1]：每日序號 String
-- KEYS[2]：號碼牌 Hash
-- KEYS[3]：有效電話 Hash
-- KEYS[4]：有效隊伍 Sorted Set
--
-- ARGV[1]：操作
-- ARGV[2]：日期
-- ARGV[3]：桌型
-- ARGV[4]：當日開始時間，秒
-- ARGV[5]：隔天午夜時間，秒
-- ARGV[6]：號碼牌識別碼
-- ARGV[7]：操作憑證
-- ARGV[8]：電話
-- ARGV[9]：姓名
-- ARGV[10]：備註

local operation = ARGV[1]
local ticketId = ARGV[6]
local token = ARGV[7]

local allowedOperations = {
    TAKE = true,
    LIST = true,
    GET = true,
    NOTIFY = true,
    MISS = true,
    SEAT = true,
    CANCEL_STAFF = true,
    CANCEL_CLIENT = true,
    COMMENT = true
}

if not allowedOperations[operation] then
    return "INVALID_OPERATION"
end

local startAt = tonumber(ARGV[4])
local expiresAt = tonumber(ARGV[5])

if not startAt or not expiresAt or expiresAt <= startAt then
    return "INVALID_TIME"
end

-- 使用 Redis 時鐘檢查日期，避免跨午夜的舊請求繼續取號。
local serverTime = redis.call("TIME")
local nowSeconds = tonumber(serverTime[1])
local nowMillis = nowSeconds * 1000
    + math.floor(tonumber(serverTime[2]) / 1000)

if nowSeconds < startAt or nowSeconds >= expiresAt then
    return "DATE_CHANGED"
end

-- 寫入前確認 Redis 資料型別。
local expectedTypes = {
    "string", "hash", "hash", "zset"
}

for i = 1, 4 do
    local actualType = redis.call("TYPE", KEYS[i]).ok

    if actualType ~= "none"
        and actualType ~= expectedTypes[i] then

        return "INVALID_KEY_TYPE"
    end
end

local function readTicket(id)
    local raw = redis.call("HGET", KEYS[2], id)

    if not raw then
        return nil
    end

    local success, ticket = pcall(cjson.decode, raw)

    if not success
        or type(ticket) ~= "table"
        or ticket.ticketId ~= id
        or type(ticket.phone) ~= "string"
        or type(ticket.status) ~= "string"
        or type(ticket.accessToken) ~= "string" then

        error("Invalid waiting ticket data")
    end

    return ticket
end

local function expireKeys()
    for i = 1, 4 do
        redis.call("EXPIREAT", KEYS[i], expiresAt)
    end
end

local function saveTicket(ticket)
    local raw = cjson.encode(ticket)

    redis.call(
        "HSET", KEYS[2], ticket.ticketId, raw)

    redis.call("EXPIREAT", KEYS[2], expiresAt)

    return raw
end

-- 結束候位：移出隊伍並解除電話限制。
-- 當日紀錄保留至午夜，序號不回收。
local function endTicket(ticket, status)
    ticket.status = status

    local raw = cjson.encode(ticket)

    redis.call(
        "HSET", KEYS[2], ticket.ticketId, raw)

    redis.call(
        "ZREM", KEYS[4], ticket.ticketId)

    local mappedId = redis.call(
        "HGET", KEYS[3], ticket.phone)

    if mappedId == ticket.ticketId then
        redis.call("HDEL", KEYS[3], ticket.phone)
    end

    expireKeys()

    return raw
end

-- 原子取號。
if operation == "TAKE" then
    local existingId = redis.call(
        "HGET", KEYS[3], ARGV[8])

    if existingId then
        return "DUPLICATE"
    end

    local counterText = redis.call("GET", KEYS[1])
    local currentSequence = 0

    if counterText then
        if not string.match(counterText, "^%d+$") then
            return "INVALID_COUNTER"
        end

        currentSequence = tonumber(counterText)
    end

    if not currentSequence
        or currentSequence >= 2147483647 then

        return "COUNTER_LIMIT"
    end

    local nextSequence = currentSequence + 1

    local ticket = {
        ticketId = ticketId,
        date = ARGV[2],
        tableType = tonumber(ARGV[3]),
        sequence = nextSequence,
        phone = ARGV[8],
        name = ARGV[9],
        note = ARGV[10],
        status = "WAITING",
        takenAt = nowMillis,
        notifiedAt = 0,
        notifyDeadline = 0,
        expiresAt = expiresAt,
        accessToken = token
    }

    local raw = cjson.encode(ticket)

    redis.call(
        "SET", KEYS[1], tostring(nextSequence))

    redis.call(
        "HSET", KEYS[2], ticketId, raw)

    redis.call(
        "HSET", KEYS[3], ARGV[8], ticketId)

    redis.call(
        "ZADD", KEYS[4], nextSequence, ticketId)

    expireKeys()

    return "OK|" .. raw
end

-- 取得今日全部紀錄，包含今日已結束的號碼牌。
if operation == "LIST" then
    local rows = redis.call("HVALS", KEYS[2])

    return "OK|[" .. table.concat(rows, ",") .. "]"
end

-- 人工叫號，按照取號順序找到尚未通知的第一組。
if operation == "NOTIFY" then
    local queueIds = redis.call(
        "ZRANGE", KEYS[4], 0, -1)

    local nextTicket = nil

    for _, id in ipairs(queueIds) do
        local candidate = readTicket(id)

        if candidate and candidate.status == "WAITING" then
            nextTicket = candidate
            break
        end
    end

    if not nextTicket then
        return "NO_WAITING"
    end

    -- 從卡片叫號時，也必須符合順序。
    if ticketId ~= ""
        and nextTicket.ticketId ~= ticketId then

        return "OUT_OF_ORDER"
    end

    nextTicket.status = "NOTIFIED"
    nextTicket.notifiedAt = nowMillis

    -- 不設定自動過號期限。
    nextTicket.notifyDeadline = 0

    local raw = saveTicket(nextTicket)

    expireKeys()

    return "OK|" .. raw
end

local ticket = readTicket(ticketId)

if not ticket then
    return "NOT_FOUND"
end

-- 客人查看與取消都需要號碼牌憑證。
if operation == "GET" or operation == "CANCEL_CLIENT" then
    if token == ""
        or ticket.accessToken ~= token then

        return "FORBIDDEN"
    end
end

if operation == "GET" then
    return "OK|" .. cjson.encode(ticket)
end

local active = ticket.status == "WAITING"
    or ticket.status == "NOTIFIED"

if not active then
    return "ENDED"
end

-- 人工過號：必須先叫號。
if operation == "MISS" then
    if ticket.status ~= "NOTIFIED" then
        return "NOT_NOTIFIED"
    end

    return "OK|" .. endTicket(ticket, "MISSED")
end

if operation == "SEAT" then
    return "OK|" .. endTicket(ticket, "SEATED")
end

if operation == "CANCEL_STAFF"
    or operation == "CANCEL_CLIENT" then

    return "OK|" .. endTicket(ticket, "CANCELLED")
end

if operation == "COMMENT" then
    ticket.note = ARGV[10]

    return "OK|" .. saveTicket(ticket)
end

return "INVALID_OPERATION"