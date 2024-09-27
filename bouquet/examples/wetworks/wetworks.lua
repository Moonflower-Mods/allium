-- Wetworks
-- By hugeblank - March 22, 2022
-- Applies the 1.19 mud water bottle mechanic to concrete powder blocks
-- This file is marked as a dynamic entrypoint in the manifest.json.
-- Try modifying, and running /reload!

-- Import java classes
local Items = require("net.minecraft.item.Items")
local Registries = require("net.minecraft.registry.Registries")
local Identifier = require("net.minecraft.util.Identifier")
local SoundEvents = require("net.minecraft.sound.SoundEvents")
local SoundCategory = require("net.minecraft.sound.SoundCategory")
local EquipmentSlot = require("net.minecraft.entity.EquipmentSlot")

local DataComponentTypes = require("net.minecraft.component.DataComponentTypes")
local PotionContentsComponent = require("net.minecraft.component.type.PotionContentsComponent")
local Potions = require("net.minecraft.potion.Potions")
local ParticleTypes = require("net.minecraft.particle.ParticleTypes")

-- Return a function that we can modify while the game is playing
events.common.BLOCK_INTERACT:register(script, function(state, world, pos, player, hand, hitResult)
    local concrete = Registries.BLOCK:getId(state:getBlock()):getPath() -- Get the name of the block interacted with
    local mainHandStack = player:getEquippedStack(EquipmentSlot.MAINHAND) -- Get the main hand itemstack of the player
    -- Check if the block name has 'concrete_powder' in it, then check if the main hand is holding a water bottle
    if concrete:find("concrete_powder") and mainHandStack:getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT):matches(Potions.WATER) then
        -- Replace the powder block with the concrete variant
        world:setBlockState(pos, Registries.BLOCK:get(Identifier.of("minecraft:"..concrete:gsub("_powder", ""))):getDefaultState())
        -- Spawn some particles
        if world:isClient() then
            world:spawnParticles(ParticleTypes.SPLASH,
                    pos:getX() + world.random:nextDouble(),
                    pos:getY() + world.random:nextDouble(),
                    pos:getZ() + world.random:nextDouble(),
                    1, 0, 0, 0, 1
            )
        end
        -- Play sound effects
        world:playSound(nil, pos:getX(), pos:getY(), pos:getZ(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1, 1)
        world:playSound(nil, pos:getX(), pos:getY(), pos:getZ(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 1, 1)
        if (not player:isCreative()) then -- If the player isn't in creative
            mainHandStack:setCount(0) -- Remove the water bottle
            player:equipStack(EquipmentSlot.MAINHAND, Items.GLASS_BOTTLE:getDefaultStack()) -- Replace it with an empty glass bottle
        end
    end
end)