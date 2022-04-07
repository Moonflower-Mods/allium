package me.hugeblank.allium.mixin.recipe;

import me.hugeblank.allium.lua.api.recipe.RecipeLib;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.*;
import org.squiddev.cobalt.LuaError;

@Mixin(AbstractCookingRecipe.class)
public class AbstractCookingRecipeMixin {
    @Shadow @Mutable @Final protected String group;

    @Shadow @Mutable @Final protected Ingredient input;

    @Shadow @Mutable @Final protected ItemStack output;

    @Shadow @Mutable @Final protected float experience;

    @Shadow @Mutable @Final protected int cookTime;

    public Ingredient allium$getInput() {
        return input;
    }

    public void allium$setGroup(String group) throws LuaError {
        RecipeLib.assertInModifyPhase();

        this.group = group;
    }

    public void allium$setInput(Ingredient input) throws LuaError {
        RecipeLib.assertInModifyPhase();

        this.input = input;
    }

    public void allium$setOutput(ItemStack output) throws LuaError {
        RecipeLib.assertInModifyPhase();

        this.output = output;
    }

    public void allium$setExperience(float experience) throws LuaError {
        RecipeLib.assertInModifyPhase();

        this.experience = experience;
    }

    public void allium$setCookTime(int cookTime) throws LuaError {
        RecipeLib.assertInModifyPhase();

        this.cookTime = cookTime;
    }
}
