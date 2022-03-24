-- Wetworks
-- By hugeblank - March 22, 2022
-- Applies the 1.19 mud water bottle mechanic to concrete powder blocks
-- This file is marked as a dynamic entrypoint in the manifest.json.
-- Try modifying, and running /reload!

-- Import java classes
local Items = java.import("Items")
local Registry = java.import("Registry")
local Identifier = java.import("Identifier")
local SoundEvents = java.import("SoundEvents")
local SoundCategory = java.import("SoundCategory")
local EquipmentSlot = java.import("EquipmentSlot")

-- Return a function that we can modify while the game is playing
local listener = allium.onEvent("block_interact", function(e, state, world, pos, player, hand, hitResult)
    local concrete = Registry.BLOCK:getId(state:getBlock()):getPath() -- Get the name of the block interacted with
    local mainHand = player:getEquippedStack(EquipmentSlot.MAINHAND) -- Get the main hand itemstack of the player
    -- Check if the block name has 'concrete_powder' in it, then check if the main hand is holding a water bottle
    if concrete:find("concrete_powder") and mainHand:isItemEqual(Items.POTION:getDefaultStack()) then
        -- Replace the powder block with the concrete variant
        world:setBlockState(pos, Registry.BLOCK:get(Identifier("minecraft:"..concrete:gsub("_powder", ""))):getDefaultState())
        -- Play the water bottle emptying sound effect
        world:playSound(nil, pos:getX(), pos:getY(), pos:getZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1, 1)
        if (not player:isCreative()) then -- If the player isn't in creative
            mainHand:setCount(0) -- Remove the water bottle
            player:equipStack(EquipmentSlot.MAINHAND, Items.GLASS_BOTTLE:getDefaultStack()) -- Replace it with an empty glass bottle
        end
    end
end)