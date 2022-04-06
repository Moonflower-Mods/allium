package me.hugeblank.allium.mixin.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractCookingRecipe.class)
public interface AbstractCookingRecipeAccessor {
    @Accessor
    void setGroup(String group);

    @Accessor
    Ingredient getInput();

    @Accessor
    void setInput(Ingredient input);

    @Accessor
    void setOutput(ItemStack stack);

    @Accessor
    void setExperience(float experience);

    @Accessor
    void setCookTime(int cookTime);
}
