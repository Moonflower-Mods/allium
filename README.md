# Allium
Lua script loader & toolchain for Java Minecraft.

Currently, only functioning and in development in fabric, with the intent of supporting 
forge/quilt later on down the road.

Lua scripts are stored in a directory named `allium` in the game directory root.  

## Demo
This is the test script I'm using, if you'd like to play/develop with Allium for yourself. 
Create a directory `test` in the `allium` directory.

This should be put in `manifest.json`
```json
{
  "id": "test",
  "version": "0.1.0",
  "name": "Allium test script"
}
```

And this should be pasted in your game directory, as `main.lua`
```lua
-- Allium Test Plugin
-- (c) hugeblank 2022
-- no rights reserved
-- Test script that demonstrates the capabilities of Allium.
allium.onEvent("chat_message", function(e, player, message)
    -- this gets logged under the "test" namespace because the anonymous function gets called after the script returns
    -- the information that registers it
    print(player:getName():asString().." said "..message)
end)

allium.onEvent("player_join", function(e, player)
    print(game.listPlayers()[1])
end)

local i = 0
allium.onEvent("player_tick", function(e, player)
    player:setExperienceLevel(i)
    i=(i+1)%40
    local world = player:getWorld()
    world:setBlockState(player:getBlockPos():down(), game.getBlock("minecraft:diorite"):getDefaultState())
end)

-- This doesn't get logged under the "test" namespace since Allium doesn't know what script is running until the return
print("Loading test script!\n", "Test", 1, 2, 3)
```

## But Allium is for ComputerCraft?
You're right! That's moved to [allium-cc](https://github.com/hugeblank/allium-cc). I think this 
project is going to be more important going forward, as it has serious utility, more so than its
dinky CC counterpart. Plus this is in active development, so stick around!