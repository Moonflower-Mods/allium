-- Wetworks
-- By hugeblank - March 22, 2022
-- Applys the 1.19 mud water bottle mechanic to concrete powder blocks

local function require(modname) 
    -- A modified version of require that deletes the cached result in package.loaded
    local out = _G.require(modname)
    _G.package.loaded[modname] = nil
    return out
end

-- Initialize event
local listener = allium.onEvent("block_interact", require("wetworks"))

allium.onEvent("chat_message", function(e, player, message)
    if message:find("!reload") then -- If the player (you) decides to reload
        allium.removeListener(listener) -- Remove the listener from the event
        listener = allium.onEvent("block_interact", require("wetworks")) -- Re-apply the new listener
        commands.tell("@a", "Reloaded!") -- Notify players (you)
    end 
end)