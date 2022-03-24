-- Hangman 
-- By hugeblank - March 22, 2022
-- A game of hangman, played in the chat. Use !hangman to start a game, add a letter or word after to start guessing.
-- Derived from the original !hangman command in alpha, for allium-cc
-- Source: https://github.com/hugeblank/Alpha/blob/master/alpha.lua#L354

local words = require "words"

local function text(str)
    return texts.toJson(texts.parseSafe(str))
end

local word
local guessed
local guesses = 0
local function parseGuess()
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
    local strs = java.split(message, " ")
    if table.remove(strs, 1) == "!hangman" then
        if #strs == 0 and word ~= nil then
            commands.tellraw(player:getName():asString(), text("<red>No guess! Add a letter or word to guess</red>"))
            return
        elseif #strs == 0 then
            ingame = true
            word = java.split(words[math.random(1, #words)], "")
            guessed = {}
            for i = 1, #word do
                guessed[i] = false
            end
            guesses = 15
            commands.tellraw("@a", text("Guess the word!"))
            commands.tellraw("@a", text("<bold>"..parseGuess().."</bold>"))
            commands.tellraw("@a", text("You have 10 guesses. Good luck!"))
            return
        end
        if word ~= nil then
            commands.tellraw("@a", text(player:getName():asString().." guessed <bold>"..strs[1].."</bold>"))
            if #strs[1] == 1 then
                local correct = false
                for i = 1, #word do
                    if word[i] == strs[1] then
                        correct = true
                        guessed[i] = true
                    end
                end
                if correct then
                    commands.tellraw("@a", text("<green>"..player:getName():asString().." guessed a letter correctly!</green>"))
                    guesses = guesses-1
                else
                    commands.tellraw("@a", text("<red>"..player:getName():asString().." guessed a letter incorrectly!</red>"))
                end
            else
                if strs[1] == table.concat(word, "") then
                    commands.tellraw("@a", text("<green>"..player:getName():asString().." guessed the word! It was: <bold>"..table.concat(word, "").."</bold></green>"))
                    word = nil
                    guesses = 0
                    return
                else
                    commands.tellraw("@a", text("<red>"..player:getName():asString().." guessed incorrectly! </red>"))
                    guesses = guesses-1
                end
            end
            if guesses > 0 then
                commands.tellraw("@a", text("<bold>"..parseGuess().."</bold>"))
                local s = " guesses"
                if guesses == 1 then s = " guess" end
                commands.tellraw("@a", text(tostring(guesses)..s.." left"))
            else
                commands.tellraw("@a", text("<green>"..player:getName():asString().." guessed the word! </green>"))
                commands.tellraw("@a", text("<red><bold>Game over!</bold> The word was: <bold>"..table.concat(word, "").."</bold></red>"))
                word = nil
            end
        end
    end
end)