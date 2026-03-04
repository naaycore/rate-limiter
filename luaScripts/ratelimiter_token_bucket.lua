local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_tokens = tonumber(ARGV[2])
local refill_interval = tonumber(ARGV[3])
local cost = tonumber(ARGV[4])
local now_ms = tonumber(ARGV[5])

local data = redis.call("HMGET", key, "tokens", "last_refill")
local tokens = tonumber(data[1])
local last_refill = tonumber(data[2])

if tokens == nil then
	tokens = capacity
	last_refill = now_ms
else
	local delta = now_ms - last_refill
	if delta > 0 then
		local intervals = math.floor(delta / refill_interval)
		if intervals > 0 then
			local refill_amount = intervals * refill_tokens
			tokens = math.min(capacity, tokens + refill_amount)
			last_refill = last_refill + intervals * refill_interval
		end
	end
end

local allowed = 0

if tokens >= cost then
	tokens = tokens - cost
	allowed = 1
end

redis.call("HMSET", key, "tokens", tokens, "last_refill", last_refill)
redis.call("PEXPIRE", key, refill_interval * 2)

return {allowed, tokens, last_refill}