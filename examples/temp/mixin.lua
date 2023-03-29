print("it's mixin' time!")
-- We have a very magic library that converts java classes into what is called "userdata" in lua.
-- This is accursed Lua.

-- Define a mixin class on ServerPlayerEntity.
local ServerPlayerEntityMixin = mixin.ofClass("net.minecraft.server.network.ServerPlayerEntity")

local SERVER_PLAYER_TICK = ServerPlayerEntityMixin:inject("server_player_tick", {
    at = {"HEAD"},
    method = "playerTick()V"
})

ServerPlayerEntityMixin:build()

-- Create events AFTER mixin has been built.

-- Pass in some code that does something
local i = 1
SERVER_PLAYER_TICK:register(script, function()
    -- code
    print("tick "..tostring(i))
    i = i+1
end, false)
-- NOTE: For a simple implementation like this, we can compact things:
--[[mixin
        .ofClass(ServerPlayerEntity)
        .inject({ at = {"HEAD"}, method = "playerTick()V" })
        .create(Identifier("demo", "on_server_player_tick"))
        .register()
]]
-- This looks much nicer, and a lot more like the java implementation, right!?