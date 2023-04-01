print("temp started!")


local e = true
events.SERVER_PLAYER_TICK:register(script, function(player)
    if e then
        print(java.cast(later.AccessibleServerPlayer, player):getSpawnPointPosition())
    end
end)