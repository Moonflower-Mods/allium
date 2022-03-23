-- Play around with the returned function, run !reload in game when you want to test it

-- Import java classes
local Blocks = java.import("Blocks")
local Items = java.import("Items")
local Registry = java.import("Registry")
local Identifier = java.import("Identifier")
local SoundEvents = java.import("SoundEvents")
local SoundCategory = java.import("SoundCategory")
local EquipmentSlot = java.import("EquipmentSlot")

-- Return a function that we can modify while the game is playing
return function(e, state, world, pos, player, hand, hitResult)
    local concrete = Registry.BLOCK:getId(state:getBlock()):getPath() -- Get the name of the block interacted with
    local mainHand = player:getEquippedStack(EquipmentSlot.MAINHAND) -- Get the main hand itemstack of the player
    -- Check if the block name has 'concrete_powder' in it, then check if the main hand is holding a water bottle
    if concrete:find("concrete_powder") and mainHand:isItemEqual(Items.POTION:getDefaultStack()) then 
        -- Replace the powder block with the concrete variant
        local state = Registry.BLOCK:get(Identifier("minecraft:"..concrete:gsub("_powder", ""))):getDefaultState()
        world:setBlockState(pos, state)
        -- Play the water bottle emptying sound effect
        world:playSound(nil, pos:getX(), pos:getY(), pos:getZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1, 1)
        mainHand:setCount(0) -- Remove the water bottle
        player:equipStack(EquipmentSlot.MAINHAND, Items.GLASS_BOTTLE:getDefaultStack()) -- Replace it with an empty glass bottle
    end
end