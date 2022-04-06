package me.hugeblank.allium.lua.api.recipe;

import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.squiddev.cobalt.LuaError;

import java.util.Map;

@LuaWrapped
public class RemoveRecipesContext extends RecipeContext {
    public RemoveRecipesContext(Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipes, Map<Identifier, Recipe<?>> recipesById) {
        super(recipes, recipesById);
    }

    @LuaWrapped
    public void removeRecipe(Identifier id) throws LuaError {
        var oldRecipe = recipesById.remove(id);

        if (oldRecipe == null)
            throw new LuaError("recipe '" + id + "' doesn't exist");

        recipes.get(oldRecipe.getType()).remove(id);
    }

    @LuaWrapped
    public void removeRecipe(Recipe<?> recipe) throws LuaError {
        removeRecipe(recipe.getId());
    }

    public interface Handler {
        void removeRecipes(RemoveRecipesContext ctx);
    }
}
