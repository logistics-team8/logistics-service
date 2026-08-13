local key = KEYS[1] -- Redis Key
local oldRefreshToken = ARGV[1] -- 기존 Refresh Token
local newRefreshToken = ARGV[2] -- 새로 생성된 Refresh Token
local ttl = tonumber(ARGV[3]) -- TTL

-- 기존 리프레시 토큰 확인
local savedRefreshToken = redis.call('get', key)

-- 리프레시 토큰이 존재하지 않거나 사용자의 리프레시 토큰이 일치하지 않으면 0을 반환
if not savedRefreshToken or savedRefreshToken ~= oldRefreshToken then
	return 0
end

-- 문제 없을 시 리프레시 리프레시 토큰 교체 후 1을 반환
redis.call('set', key, newRefreshToken, 'EX', ttl)
return 1
