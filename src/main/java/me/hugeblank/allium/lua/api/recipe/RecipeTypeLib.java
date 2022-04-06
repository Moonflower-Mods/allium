package me.hugeblank.allium.lua.api.recipe;

import me.hugeblank.allium.lua.type.annotation.LuaIndex;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.squiddev.cobalt.LuaError;

public class RecipeTypeLib {
    @LuaIndex
    public RecipeType<?> index(String type) throws LuaError {
        Identifier id = new Identifier(type);

        return Registry.RECIPE_TYPE.getOrEmpty(id).orElse(null);
    }
}
