package me.hugeblank.allium.mixin.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShapedRecipe.class)
public interface ShapedRecipeAccessor {
    @Accessor
    @Mutable
    void setGroup(String group);

    @Accessor
    @Mutable
    void setOutput(ItemStack stack);
}
