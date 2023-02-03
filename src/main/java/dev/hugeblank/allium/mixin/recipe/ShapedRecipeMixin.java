package dev.hugeblank.allium.mixin.recipe;

import dev.hugeblank.allium.lua.api.recipe.RecipeLib;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.squiddev.cobalt.LuaError;

@Mixin(ShapedRecipe.class)
public class ShapedRecipeMixin {
    @Shadow @Mutable @Final String group;

    @Shadow @Mutable @Final ItemStack output;

    public void allium$setGroup(String group) throws LuaError {
        RecipeLib.assertInModifyPhase();

        this.group = group;
    }

    public void allium$setOutput(ItemStack output) throws LuaError {
        RecipeLib.assertInModifyPhase();

        this.output = output;
    }
}
