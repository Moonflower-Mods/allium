-- don't mind this! templating a documentation scheme.

--- A block is a voxel in a {@linkplain World world}. {@link AbstractBlock},
-- this class, and its subclasses define all logic for those voxels.
--
-- <p>There is exactly one instance for every type of block. Every stone
-- block for example in a world shares the same block instance. Each block
-- instance is registered under {@link net.minecraft.util.registry.Registry#BLOCK}.
-- See {@link Blocks} for examples of block instances.
--
-- <p>An item corresponding to a block is not automatically created. You
-- may create your own {@link net.minecraft.item.BlockItem} and register it
-- under {@link net.minecraft.util.registry.Registry#ITEM}.
--
-- <p>The translation key for the block name is determined by {@link
-- #getTranslationKey}.
--
-- <p>In the world, the actual voxels are not stored as blocks, but as
-- {@linkplain BlockState block states}. The possible states of the block
-- are defined by {@link #appendProperties}.
--
-- @see AbstractBlock
-- @see BlockState
--
-- @module net.minecraft.block.Block
-- @alias Block
local Block = {}

-- @classobject
local object = {}

    --- Replaces the {@code state} with the {@code newState} at the {@code pos}.
    --
    -- <p>If the two state objects are identical, this method does nothing.
    --
    -- <p>If the new state {@linkplain BlockState#isAir() is air},
    -- breaks the block at the position instead.
    --
    -- @tparam net.minecraft.block.BlockState newState the new block state
    -- @tparam net.minecraft.world.WorldAccess world the world
    -- @tparam pos the position of the replaced block state
    -- @tparam int flags the bitwise flags for {@link net.minecraft.world.ModifiableWorld#setBlockState(BlockPos, BlockState, int, int)}
    -- @tparam net.minecraft.block.BlockState state the existing block state
    function Block.replace(state, newState, world, pos, flags)
        Block.replace(state, newState, world, pos, flags, 512);
    end