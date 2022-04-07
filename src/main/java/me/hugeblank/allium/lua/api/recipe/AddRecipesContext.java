package me.hugeblank.allium.lua.api.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.hugeblank.allium.lua.api.JsonLib;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
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
    public void addRecipe(Identifier id, String json) throws LuaError {
        addRecipe(id, JsonParser.parseString(json).getAsJsonObject());
    }

    @LuaWrapped
    public void addRecipe(Identifier id, JsonObject el) throws LuaError {
        addRecipe(id, RecipeManager.deserialize(id, el));
    }

    @LuaWrapped
    public void addRecipe(Identifier id, LuaValue val) throws LuaError {
        addRecipe(id, JsonLib.toJsonElement(val).getAsJsonObject());
    }

    @LuaWrapped
    public void addRecipe(Identifier id, Recipe<?> recipe) throws LuaError {
        if (recipesById.put(id, recipe) != null) {
            throw new LuaError("recipe '" + id + "' already exists");
        }

         recipes.computeIfAbsent(recipe.getType(), unused -> new HashMap<>()).put(id, recipe);
    }

    public interface Handler {
        void addRecipes(AddRecipesContext ctx);
    }
}
