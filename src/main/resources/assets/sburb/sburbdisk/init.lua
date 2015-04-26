local screen = component.list('screen', true)()
for address in component.list('screen', true) do
  if #component.invoke(address, 'getKeyboards') > 0 then
    screen = address
  end
end

local gpu = component.list('gpu', true)()
local w, h, msgX, msgY, msgWidth, msgHeight
msgWidth = 36
msgHeight = 3
if gpu and screen then
  component.invoke(gpu, 'bind', screen)
  w, h = component.invoke(gpu, 'getResolution')
  msgX = w / 2 - msgWidth / 2
  msgY = h / 2
  component.invoke(gpu, 'setResolution', w, h)
  component.invoke(gpu, 'setBackground', 0x8BAFE0)
  component.invoke(gpu, 'setForeground', 0xFFFFFF)
  component.invoke(gpu, 'fill', 1, 1, w, h, ' ')
end
local y = 1
local function print(msg)
  if gpu and screen then
    component.invoke(gpu, 'setForeground', 0x0057A3)
    component.invoke(gpu, 'set', 1, y, msg)
    if y == h then
      component.invoke(gpu, 'copy', 1, 2, w, h - 1, 0, -1)
      component.invoke(gpu, 'fill', 1, h, w, 1, ' ')
    else
      y = y + 1
    end
  end
end
local function status(msg)
  if gpu and screen then
    component.invoke(gpu, 'setBackground', 0x11D635)
    component.invoke(gpu, 'fill', msgX, msgY,        msgWidth, msgHeight, ' ')
    component.invoke(gpu, 'set', msgX + 1, msgY + 1, msg)
  end
end
local function title(msg)
  if gpu and screen then
    component.invoke(gpu, 'setBackground', 0xFFFFFF)
    component.invoke(gpu, 'setForeground', 0x0057A3)
    component.invoke(gpu, 'fill', msgX, msgY - 5, msgWidth, msgHeight, ' ')
    component.invoke(gpu, 'set', msgX + 1, msgY - 4, msg)
    component.invoke(gpu, 'setForeground', 0xFFFFFF)
  end
end
local function clear()
  if gpu and screen then
    y = 1
    component.invoke(gpu, 'setBackground', 0x8BAFE0)
    component.invoke(gpu, 'setForeground', 0xFFFFFF)
    component.invoke(gpu, 'fill', 1, 1, w, h, ' ')
  end
end

local rom = {}
function rom.invoke(method, ...)
  return component.invoke(computer.getBootAddress(), method, ...)
end
function rom.open(file) return rom.invoke("open", file) end
function rom.read(handle) return rom.invoke("read", handle, math.huge) end
function rom.close(handle) return rom.invoke("close", handle) end
function rom.inits() return ipairs(rom.invoke("list", "boot")) end
function rom.isDirectory(path) return rom.invoke("isDirectory", path) end
-- function rom.sburbTest(str) return rom.invoke("sburbTest", arg) end

function sleep(seconds)
  checkArg(1, seconds, "number", "nil")
  local deadline = computer.uptime() + (seconds or 0)
  repeat
    computer.pullSignal(deadline - computer.uptime())
  until computer.uptime() >= deadline
end

local loaded = false
local function load()
  local handle, reason = rom.open('statuses.txt')
  if not handle then
    error(reason)
  end

  repeat
    local data, reason = rom.read(handle)
    if not data and reason then
      error(reason)
    end
    if data then
      for d in string.gmatch(data, "[%w -]+|\n") do
        status(d:gsub('|.*', ''))
        sleep(math.random() * 0.75)
      end
    end
  until not data

  clear()
  title('Sburb (nothing happens yet)')
  loaded = true
end


local keyW = 119
local keyA = 97
local keyS = 115
local keyD = 100
local keyEnter = 13
local keySpace = 32

title("Press enter to begin")

-- boot address should be address of this sburb disc, right!?!?
local sburb = computer.getBootAddress()

while true do
  local name, arg1, arg2, arg3, arg4, arg5 = computer.pullSignal()
  -- print("gave me "..name.." signal")
  if name == 'key_down' then
    local char = arg2
    local player = arg4

    -- TODO check if player has started sburb already
    if not loaded and char == keyEnter then
      title('Welcome, '..player)
      load()

    -- NOTE test...
    elseif char == keySpace then
      local response = component.invoke(sburb, 'sburbTest', player..' is bad')
      status(response)
    elseif char == keyW then
      print('---'..component.invoke(sburb, 'getLabel')..'---')
      for key,value in pairs(component.methods(sburb)) do
        local valueText = '---'
        if value then valueText = 'direct' else valueText = 'synchronized' end
        print(key..' - '..valueText)
      end
    end

    -- status("pressed "..char)

  elseif name == 'clipboard' then
    computer.shutdown()
  end
end