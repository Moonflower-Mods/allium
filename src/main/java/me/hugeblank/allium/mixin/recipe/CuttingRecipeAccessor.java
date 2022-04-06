package me.hugeblank.allium.mixin.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CuttingRecipe;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CuttingRecipe.class)
public interface CuttingRecipeAccessor {
    @Accessor
    Ingredient getInput();

    @Accessor
    void setInput(Ingredient input);

    @Accessor
    void setOutput(ItemStack output);

    @Accessor
    void setGroup(String group);
}
