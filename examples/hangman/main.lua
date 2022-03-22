-- Hangman for Allium
-- by hugeblank - March 22, 2022
-- Derived from the original !hangman command in alpha, for allium-cc
-- Source: https://github.com/hugeblank/Alpha/blob/master/alpha.lua#L354

local words = require "words"

local function text(str)
    -- Parse & convert tagged text to formatted text
    return texts.toJson(texts.parseSafe(str))
end

local word
local guessed
local guesses = 0
local function parseGuess()
    --[[ Returns a string where all guessed letters are exposed,
     and all unknown letters are obscured with an underscore ]]
    local out = ""
    for i = 1, #word do
        if guessed[i] then
            out = out..word[i]
        else
            out = out.."_"
        end
        if i < #word then
            out = out.." "
        end
    end
    return out
end

allium.onEvent("chat_message", function(e, player, message)
    local strs = java.split(message, " ") -- Split the string into a table using java magic
    if table.remove(strs, 1) == "!hangman" then -- Check if this is for us
        if #strs == 0 and word ~= nil then -- If a game is ongoing, and the user tries to start a game
            commands.tellraw(player:getName():asString(), text("<red>No guess! Add a letter or word to guess</red>"))
            return
        elseif #strs == 0 then -- Initialize the game!
            ingame = true
            word = java.split(words[math.random(1, #words)], "")
            guessed = {}
            for i = 1, #word do -- Create a table that keeps track of the letters correctly guessed by players
                guessed[i] = false
            end
            guesses = 15 -- A lenient amount guesses
            commands.tellraw("@a", text("Guess the word!"))
            commands.tellraw("@a", text("<bold>"..parseGuess().."</bold>"))
            commands.tellraw("@a", text("You have 10 guesses. Good luck!"))
            return
        end
        if word ~= nil then -- Handle guesses if a game is being played
            commands.tellraw("@a", text(player:getName():asString().." guessed <bold>"..strs[1].."</bold>"))
            if #strs[1] == 1 then -- If the player is only guessing a letter
                local correct = false
                for i = 1, #word do -- Check each letter
                    if word[i] == strs[1] then -- If the guessed letter exists in the word
                        correct = true -- Flag to tell chat the player guessed correctly
                        guessed[i] = true -- Flag that this letter has been guessed in the word, for parsing the guess
                    end
                end
                if correct then
                    commands.tellraw("@a", text("<green>"..player:getName():asString().." guessed a letter correctly!</green>"))
                else
                    commands.tellraw("@a", text("<red>"..player:getName():asString().." guessed a letter incorrectly!</red>"))
                    guesses = guesses-1 -- Subtract a guess only if the guess is incorrect
                end
            else -- The player is guessing more than one letter
                if strs[1] == table.concat(word, "") then -- if it's correct
                    commands.tellraw("@a", text("<green>"..player:getName():asString().." guessed the word! It was: <bold>"..table.concat(word, "").."</bold></green>"))
                    word = nil
                    guesses = 0
                    return
                else -- If it's incorrect
                    commands.tellraw("@a", text("<red>"..player:getName():asString().." guessed the word incorrectly! </red>"))
                    guesses = guesses-1
                end
            end
            if guesses > 0 then -- The game goes on
                commands.tellraw("@a", text("<bold>"..parseGuess().."</bold>"))
                local s = " guesses"
                if guesses == 1 then s = " guess" end -- handle the english language
                commands.tellraw("@a", text(tostring(guesses)..s.." left"))
            else
                commands.tellraw("@a", text("<red><bold>Game over!</bold> The word was: <bold>"..table.concat(word, "").."</bold></red>"))
                word = nil -- Out of guesses, end the game
            end
        end
    end
end)