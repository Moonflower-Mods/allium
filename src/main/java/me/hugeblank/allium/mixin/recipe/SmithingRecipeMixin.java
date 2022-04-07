package me.hugeblank.allium.mixin.recipe;

import me.hugeblank.allium.lua.api.recipe.RecipeLib;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.SmithingRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.squiddev.cobalt.LuaError;

@Mixin(SmithingRecipe.class)
public class SmithingRecipeMixin {
    @Shadow @Mutable @Final ItemStack result;

    @Shadow @Mutable @Final Ingredient base;

    @Shadow @Mutable @Final Ingredient addition;

    // Renamed to `output` to match getOutput method
    public void allium$setOutput(ItemStack result) throws LuaError {
        RecipeLib.assertInModifyPhase();

        this.result = result;
    }

    public Ingredient allium$getBase() {
        return this.base;
    }

    public Ingredient allium$getAddition() {
        return this.addition;
    }

    public void allium$setBase(Ingredient base) throws LuaError {
        RecipeLib.assertInModifyPhase();

        this.base = base;
    }

    public void allium$setAddition(Ingredient addition) throws LuaError {
        RecipeLib.assertInModifyPhase();

        this.addition = addition;
    }
}
