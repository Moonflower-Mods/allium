package me.hugeblank.allium.lua.api.recipe;

import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

import java.util.HashMap;
import java.util.Map;

@LuaWrapped
public class AddRecipesContext extends RecipeContext {
    public AddRecipesContext(Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipes, Map<Identifier, Recipe<?>> recipesById) {
        super(recipes, recipesById);
    }

    @LuaWrapped
    public void addRecipe(Identifier id, LuaValue value) throws LuaError {
        Recipe<?> recipe;

        if (value.isUserdata(Recipe.class))
            recipe = value.checkUserdata(Recipe.class);
        else
            recipe = RecipeLib.fromJson(id, value);

        if (recipesById.put(id, recipe) != null) {
            throw new LuaError("recipe '" + id + "' already exists");
        }

         recipes.computeIfAbsent(recipe.getType(), unused -> new HashMap<>()).put(id, recipe);
    }

    public interface Handler {
        void addRecipes(AddRecipesContext ctx);
    }
}
