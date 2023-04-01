print("temp started!")


local e = true
events.SERVER_PLAYER_TICK:register(script, function(player)
    if e then
        print(tostring(java.cast(later.AccessibleServerPlayerHolder:get(), player):getSpawnPointPosition()))
        e = false
    end
end)