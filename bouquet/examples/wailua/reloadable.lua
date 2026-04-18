-- WAILUA
-- By hugeblank - Jul 9, 2022
-- WAILA-like (What Am I Looking At) script exclusively for the client-side.
-- This is a demonstration of how allium is not just for server sided use cases.

if allium.environment() ~= "client" then return end

local events = require("bouquet").events
local Component = require("net.minecraft.network.chat.Component")
local BuiltInRegistries = require("net.minecraft.core.registries.BuiltInRegistries")
local Type = require("net.minecraft.world.phys.HitResult.Type")

events.client.guiRenderTail:register(script, function(gui, minecraft, graphics, deltaTracker, shouldRenderLevel, resourcesLoaded)
    if minecraft.hitResult and minecraft.hitResult:getType() == Type.BLOCK then
        -- Finally, use the block to get the identifier of the block
        local identifier = BuiltInRegistries.BLOCK:getKey(
            -- Use the position to get the state, then the block attributed to that state
            minecraft.level:getBlockState(
                -- Get the block position the player is looking at <- START HERE read UP ^
                minecraft.hitResult:getBlockPos()
            ):getBlock()
        )
        local namespace, path = identifier:getNamespace(),  identifier:getPath()
        if not (namespace == "minecraft" and path == "air") then -- If we're just looking at air, don't write text.
            graphics:centeredText(
                minecraft.font,
                Component.translatable("block."..namespace.."."..path),
                graphics:guiWidth()/2,
                5,
                0xffffffff
            )
            -- The position 5 was arbitrarily chosen, and was the first value I picked just to test. It worked perfectly.
            -- Exercise for the reader - Create a background frame behind the text, so it can be viewed on white backgrounds.
            -- Note that while text rendering uses RGB, background rendering uses ARGB.
        end
    end
end)