local key = KEYS[1] -- Redis Key
local sessionId = ARGV[1] -- 세션 ID
local now = tonumber(ARGV[2]) -- 현재 시간
local expireAt = tonumber(ARGV[3]) -- 만료 시간
local maxSize = tonumber(ARGV[4]) -- 허용 세션 수
local ttl = tonumber(ARGV[5]) -- TTL

-- 만료 데이터 정리
redis.call('zremrangebyscore', key, '-inf', now)

-- 세션 추가 / 갱신
redis.call('zadd', key, expireAt, sessionId)

-- 총 세션 개수
local len = redis.call('zcard', key)

-- 가장 오래된 세션 삭제
if len > maxSize then
    redis.call('zremrangebyrank', key, 0, len - maxSize - 1)
end

-- ttl 설정
redis.call('expire', key, ttl)
