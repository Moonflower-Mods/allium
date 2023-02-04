package dev.hugeblank.allium.lua.api.recipe;

import dev.hugeblank.allium.lua.type.annotation.CoerceToNative;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.squiddev.cobalt.LuaError;

import java.util.HashMap;
import java.util.Map;

@LuaWrapped
public class ModifyRecipesContext extends RecipeContext {
    public ModifyRecipesContext(Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipes, Map<Identifier, Recipe<?>> recipesById) {
        super(recipes, recipesById);
    }

    @LuaWrapped
    public Recipe<?> getRecipe(Identifier id) {
        return recipesById.get(id);
    }

    @LuaWrapped
    public @CoerceToNative Map<Identifier, Recipe<?>> getRecipesOfType(RecipeType<?> type) {
        return recipes.get(type);
    }

    @LuaWrapped
    public void replaceRecipe(Identifier id, Recipe<?> newRecipe) throws LuaError {
        var oldRecipe = recipesById.put(id, newRecipe);

        if (oldRecipe == null)
            throw new LuaError("recipe '" + id + "' doesn't exist");
        else if (oldRecipe.getType() != newRecipe.getType())
            throw new LuaError("old recipe and new recipe's types don't match");

        recipes.computeIfAbsent(newRecipe.getType(), unused -> new HashMap<>()).put(id, newRecipe);
    }

    public interface Handler {
        void modifyRecipes(ModifyRecipesContext ctx);
    }
}
