package me.hugeblank.allium.mixin.recipe;

import me.hugeblank.allium.lua.api.recipe.RecipeLib;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapelessRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.squiddev.cobalt.LuaError;

@Mixin(ShapelessRecipe.class)
public class ShapelessRecipeMixin {
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
