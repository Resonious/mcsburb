local function getGpuAndScreen()
  local screen = component.list('screen', true)()
  for address in component.list('screen', true) do
    if #component.invoke(address, 'getKeyboards') > 0 then
      screen = address
    end
  end
  local gpu = component.list('gpu', true)()
  return gpu, screen
end

local gpu, screen = getGpuAndScreen()

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
local function box(yStart, msg)
  if gpu and screen then
    component.invoke(gpu, 'setBackground', 0x11D635)
    component.invoke(gpu, 'fill', msgX-2, msgY + yStart, msgWidth+4, msgHeight, ' ')
    component.invoke(gpu, 'set', msgX - 1, msgY + yStart + 1, msg)
  end
end
local function listItem(yStart, msg)
  if gpu and screen then
    component.invoke(gpu, 'setBackground', 0x1900FA)
    component.invoke(gpu, 'fill', msgX-2, msgY - 3 + yStart, msgWidth+4, 1, ' ')
    component.invoke(gpu, 'set', msgX - 1, msgY - 3 + yStart, msg)
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
local keyC = 99
local keyQ = 113
local keyY = 121
local keyN = 110
local keyEnter = 13
local keySpace = 32
local function numOfKey(char)
  local num = char - 48
  if num >= 0 and num <= 9 then
    return num
  else
    return nil
  end
end

-- title("Press enter to begin")

-- boot address should be address of this sburb disc, right!?!?
local sburb = component.proxy(computer.getBootAddress())

local testState = {
  curPlayer = nil,

  open = function(self, player)
    self.curPlayer = player
  end,
  close = function(self)
  end,

  render = function(self)
    title('test!!!')
    status('unbelievable')
  end,

  key = function(self, player, char)
    print(player..' pressed '..char)
  end,

  touch = function(self, player, x, y, button)
    print(player..' touched at '..x..', '..y)
  end
}

withPlayer = function(self, player)
  self.curPlayer = player
  return self
end

serverListState = {
  curPlayer = 'unknown',
  with = withPlayer,
  curError = nil,
  servers = nil,
  refreshTimer = 2,

  open = function(self)
    self.servers, self.curError = sburb.listWaitingServers(self.curPlayer)
  end,

  render = function(self)
    if self.curError then
      title('Error: '..self.curError)
    elseif not self.servers then
      title('wtf i dont even know')
    elseif #self.servers == 0 then
      title('No servers currently available')
    else
      title('Press the number corrosponding to...')
      for i,name in ipairs(self.servers) do
        listItem(i, i..') '..name)
      end
    end
    component.invoke(gpu, 'setBackground', 0x8BAFE0)
    print("Press 'q' to cancel")
    -- print("also lol u cant choose yet")
  end,

  tick = function(self)
    self.refreshTimer = self.refreshTimer - 1
    if self.refreshTimer <= 0 then
      self:open()
      clear()
      self:render()

      self.refreshTimer = 2
    end
  end,

  key = function(self, player, char)
    if char == keyQ then
      switchTo(menuState:with(self.curPlayer))
    elseif self.servers and #self.servers > 0 then
      local num = numOfKey(char)
      if num and num > 0 and num <= #self.servers then
        switchTo(
          waitForServerConfirmState
            :with(self.curPlayer)
            :selecting(self.servers[num])
        )
      end
    end
  end
}

waitForServerConfirmState = {
  curPlayer = 'unknown',
  choice = 'unknown',
  curError = nil,

  with = withPlayer,
  selecting = function(self, choice)
    self.choice = choice
    return self
  end,

  open = function(self)
    self.curError = sburb.selectServer(self.curPlayer, self.choice)
  end,
  close = function(self)
    sburb.cancelSelection(self.curPlayer)
  end,

  render = function(self)
    if self.curError then
      title('error: '..self.curError)
    else
      title('Waiting for '..self.choice..'...')
      status("Press 'q' to cancel")
    end
  end,

  tick = function(self)
    if sburb.playerHasServer(self.curPlayer) then
      switchTo(menuState:with(self.curPlayer))
    else
      local hit, candidate = sburb.checkIfSelected(self.choice)
      if not hit or candidate ~= self.curPlayer then
        switchTo(serverListState:with(self.curPlayer))
      end
    end
  end,

  key = function(self, player, char)
    if char == keyQ then
      switchTo(serverListState:with(self.curPlayer))
    end
  end
}

waitForClientState = {
  curPlayer = 'unknown',
  curError = nil,
  candidate = nil,

  open = function(self)
    self.curError = sburb.waitForClient(self.curPlayer)
  end,
  close = function(self)
    sburb.doneWaitingForClient(self.curPlayer)
  end,

  with = withPlayer,

  render = function(self)
    local boxY = 0
    if self.curError then
      title('Error: '..self.curError)

    elseif self.candidate then
      title('Accept '..self.candidate..' as client?')
      box(boxY, "'y' to accept, 'n' to reject")
      boxY = boxY + 4

    else
      title('Waiting for client connection...')
    end

    box(boxY, "Press 'q' to cancel.")
  end,

  tick = function(self)
    local hit, candidate = sburb.checkIfSelected(self.curPlayer)
    if hit then
      if candidate ~= self.candidate then
        self.candidate = candidate
        clear()
        self:render()
      end

    elseif self.candidate then
      self.candidate = nil
      clear()
      self:render()

    end
    if hit and not candidate then
      self.curError = hit
      clear()
      self:render()
    end
  end,

  key = function(self, player, char)
    if char == keyQ then
      switchTo(menuState:with(self.curPlayer))

    elseif self.candidate then
      if char == keyY then
        sburb.appointServer(self.candidate, self.curPlayer)
        switchTo(menuState:with(self.curPlayer))
      elseif char == keyN then
        sburb.cancelSelection(self.candidate)
        self.candidate = nil
        clear()
        self:render()
      end
    else
      self:render()
    end
  end
}

menuState = {
  curPlayer = 'unknown',

  with = withPlayer,

  render = function(self)
    title('Welcome, '..self.curPlayer)
    
    component.invoke(gpu, 'setBackground', 0x8BAFE0)
    print("Press 'q' to sign out")
    local serverName = sburb.serverPlayerOf(self.curPlayer)
    if serverName then print('Server: '..serverName) end
    local clientName = sburb.clientPlayerOf(self.curPlayer)
    if clientName then print('Client: '..clientName) end

    if sburb.playerHasGame(self.curPlayer) then
      local curY = 0
      if sburb.playerHasClient(self.curPlayer) then
        box(curY, "Press 's' to connect to your client.")
        curY = curY + 5
      else
        box(curY, "Press 's' to await client")
        curY = curY + 5
      end
      if not sburb.playerHasServer(self.curPlayer) then
        box(curY, "Press 'c' to connect to a server")
        curY = curY + 5
      end

    else
      status("You ain't even playin'...")
    end
  end,

  key = function(self, player, char)
    if char == keyQ then
      self.curPlayer = 'unknown'
      switchTo(loginState)
      return
    end

    if player ~= self.curPlayer then return end
    if not sburb.playerHasGame(player) then return end

    if char == keyS then
      if sburb.playerHasClient(player) then
        local err = sburb.toggleServerMode(player)
        if err then print(err) end

      else
        switchTo(waitForClientState:with(player))
      end

    elseif char == keyC then
      if not sburb.playerHasServer(player) then
        switchTo(serverListState:with(player))
      end

    else
      clear()
      self:render()
    end
  end,

  touch = function(self, player, x, y, button)
    self:render()
  end
}

loginState = {
  render = function(self)
    title('Press enter to begin')
  end,

  key = function(self, player, char)
    if char == keyEnter then
      sburb.clearStateFor(player)
      switchTo(menuState:with(player))

    elseif char == keySpace then
      switchTo(testState)
    end
  end,

  touch = function(self, player, x, y, button)
    self:render()
  end
}

local currentState = {}
function switchTo(state, player)
  if currentState.close then currentState:close() end
  clear()
  currentState = state
  if state.open then state:open(player) end
  if state.render then state:render() end
end

switchTo(loginState)

while true do
  local name, arg1, arg2, arg3, arg4, arg5 = computer.pullSignal(1)

  if name == nil then
    if currentState.tick then
      currentState:tick()
    end

  elseif name == 'key_down' then
    if currentState.key then
      currentState:key(arg4, arg2)
    end

  elseif name == 'touch' then
    if currentState.touch then
      currentState:touch(arg5, arg2, arg3, arg4)
    end

  elseif name == 'component_added' then
    if arg1 == 'screen' or arg1 == 'gpu' then
      gpu, screen = getGpuAndScreen()
      if currentState.render then currentState:render() end
    end
  end
end