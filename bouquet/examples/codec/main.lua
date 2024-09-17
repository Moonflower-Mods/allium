local CommandRegistrationCallback = java.import("net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback")
local CommandManager = java.import("net.minecraft.server.command.CommandManager")
local Hand = java.import("net.minecraft.util.Hand")

CommandRegistrationCallback.EVENT:register(function(dispatcher, registryAccess, environment)
    dispatcher:register(CommandManager.literal("encode_held_item")
        :executes(function(ctx)
            local src = ctx:getSource()
            local player = src:getPlayerOrThrow();
            local heldStack = player:getStackInHand(Hand.MAIN_HAND);

            print(heldStack:getItem())
        end)));
end);
