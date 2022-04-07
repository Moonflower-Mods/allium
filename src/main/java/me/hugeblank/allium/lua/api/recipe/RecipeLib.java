package me.hugeblank.allium.lua.api.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.hugeblank.allium.lua.api.JsonLib;
import me.hugeblank.allium.lua.api.WrappedLuaLibrary;
import me.hugeblank.allium.lua.event.SimpleEventType;
import me.hugeblank.allium.lua.type.annotation.CoerceToBound;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

import java.util.Map;

@LuaWrapped(name = "recipe")
public class RecipeLib implements WrappedLuaLibrary {
    private static boolean IN_MODIFY_PHASE = false;

    @LuaWrapped
    public static final SimpleEventType<AddRecipesContext.Handler> ADD = new SimpleEventType<>(new Identifier("allium:recipe/adding_recipes"));

    @LuaWrapped
    public static final SimpleEventType<ModifyRecipesContext.Handler> MODIFY = new SimpleEventType<>(new Identifier("allium:recipe/modifying_recipes"));

    @LuaWrapped
    public static final SimpleEventType<RemoveRecipesContext.Handler> REMOVE = new SimpleEventType<>(new Identifier("allium:recipe/remove_recipes"));

    @LuaWrapped(name = "types")
    public static final @CoerceToBound RecipeTypeLib TYPES = new RecipeTypeLib();

    public static void assertInModifyPhase() throws LuaError {
        if (!IN_MODIFY_PHASE) {
            throw new LuaError("tried to modify recipe not in modify phase");
        }
    }

    public static void runRecipeEvents(Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipes, Map<Identifier, Recipe<?>> recipesById) {
        AddRecipesContext addCtx = new AddRecipesContext(recipes, recipesById);

        ADD.invoker().addRecipes(addCtx);

        ModifyRecipesContext modifyCtx = new ModifyRecipesContext(recipes, recipesById);

        IN_MODIFY_PHASE = true;
        MODIFY.invoker().modifyRecipes(modifyCtx);
        IN_MODIFY_PHASE = false;

        RemoveRecipesContext removeCtx = new RemoveRecipesContext(recipes, recipesById);

        REMOVE.invoker().removeRecipes(removeCtx);
    }
}
