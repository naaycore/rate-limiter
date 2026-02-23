local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local processRate = tonumber(ARGV[2])

local listExisits = redis.call("EXISTS", key)

if listExists == 0 then
	redis.call("RPUSH", key,  processRate)
end

local data = redis.call("LPOP", key)

return {data}
