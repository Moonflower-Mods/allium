-- WAILUA
-- By hugeblank - Jul 9, 2022
-- WAILA-like (What Am I Looking At) script exclusively for the client-side.
-- This is a demonstration of how allium is not just for server sided use cases.
local Text = require("net.minecraft.text.Text")
local BlockPos = require("net.minecraft.util.math.BlockPos")
local Registries = require("net.minecraft.registry.Registries")

local renderText -- The text to be shared between the render event and tick event

events.client.GUI_RENDER_TAIL:register(script, function(client, context, hud)
    if renderText then -- If there's text, then draw it at the top center
        context:drawCenteredTextWithShadow(hud:getTextRenderer(), renderText, context:getScaledWindowWidth()/2, 5, 0xffffff)
        -- The position 5 was arbitrarily chosen, and was the first value I picked just to test. It worked perfectly.
        -- Exercise for the reader - Create a background frame behind the text, so it can be viewed on white backgrounds.
    end
end)

events.common.PLAYER_TICK:register(script, function(player)
    if player:getWorld():isClient() then
        -- Finally, use the block to get the identifier of the block
        local identifier = Registries.BLOCK:getId(
        -- Use the position to get the state, then the block attributed to that state
                player.world:getBlockState(
                -- Convert position to an actual BlockPos
                        BlockPos(
                        -- Get the block position the player is looking at <- START HERE read UP ^
                                player:raycast(5, 0, false):getBlockPos()
                        )
                ):getBlock()
        )
        local namespace, path = identifier:getNamespace(),  identifier:getPath()
        if namespace == "minecraft" and path == "air" then -- If we're just looking at air, don't create a text object.
            renderText = nil
        else -- Otherwise, pull the name of the block from the language.
            renderText = Text.translatable("block."..namespace.."."..path)
        end
    end
end)