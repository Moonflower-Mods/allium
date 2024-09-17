print("I'm feeling lucky!")

local brewingRecipeRegistryMixinBuilder = mixin.asClass("net.minecraft.recipe.BrewingRecipeRegistry") -- For use later
local brewingRecipeRegistryInvokerBuilder = mixin.asInterface("net.minecraft.recipe.BrewingRecipeRegistry") -- And create an interface mixin for an invoker

-- define that invoker. As an exercise for the reader, create and find different uses for the other private methods in this class.
brewingRecipeRegistryInvokerBuilder:invoker({"registerPotionRecipe(Lnet/minecraft/potion/Potion;Lnet/minecraft/item/Item;Lnet/minecraft/potion/Potion;)V"})
local brewingRecipeRegistryInvokerHolder = brewingRecipeRegistryInvokerBuilder:build() -- build interface mixin first

brewingRecipeRegistryMixinBuilder -- Get the point at which potions should be registered.
        :inject("add_brewing_recipes", { at = "TAIL", method = "registerDefaults()V" })
        :register(script, function(ci)
            -- get the interface from the holder
            local brewingRecipeRegistryInvoker = brewingRecipeRegistryInvokerHolder:getInterface()
            local Items = java.import("Items") -- import classes here to not cause nasty crashes.
            local Potions = java.import("Potions")

            brewingRecipeRegistryInvoker.invokeRegisterPotionRecipe(Potions.AWKWARD, Items.GOLD_NUGGET, Potions.LUCK) -- Register our lucky little potion
        end)

brewingRecipeRegistryMixinBuilder:build() -- Build the injecting mixin.
