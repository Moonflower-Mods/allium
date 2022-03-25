-- Hangman
-- By hugeblank - March 22, 2022
-- A game of hangman, played in the chat. Use !hangman to start a game, add a letter or word after to start guessing.
-- Derived from the original !hangman command in alpha, for allium-cc
-- Source: https://github.com/hugeblank/Alpha/blob/master/alpha.lua#L354

local words = require "words"
local CommandManager = java.import("CommandManager") -- We need the java command manager for creating commands.


local function text(str)
    return texts.toJson(texts.parseSafe(str))
end

local word
local guessed
local guesses = 0
local function parseGuess()
    -- Returns a string with underscores where a letter hasn't been guessed
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

local builder = CommandManager.literal("hangman") -- Create the builder for the hangman command

allium.onEvent("command_register", function(e, script, command, success)
    -- Let us know if the command was successfully registered
    if success then
        print(script, command, "success")
    else
        print(script, command, "failure")
    end
end)

builder:executes(function(context) -- The part of the command with no values attached
    if word ~= nil then -- If there's a game being played tell the player to guess
        commands.tellraw(context:getPlayer():getName():asString(), text("<red>No guess! Add a letter or word to guess</red>"))
        return 0
    else -- Start a game, since there's not one currently playing
        word = java.split(words[math.random(1, #words)], "")
        guessed = {} -- Create a table to mark guessed characters in the word
        for i = 1, #word do
        guessed[i] = false
        end
        guesses = 15 -- Give a generous 15 guesses. Could be reduced to decrease the difficulty.
        commands.tellraw("@a", text("Guess the word!"))
        commands.tellraw("@a", text("<bold>"..parseGuess().."</bold>"))
        commands.tellraw("@a", text("You have 10 guesses. Good luck!"))
        return 1
    end
end)

builder:m_then(CommandManager.argument("guess", argumentTypes.string.word())):executes(function(context)
    -- The part of the command that handles guesses
    local str = argumentTypes.string.getString(context, "guess") -- Get the guess from the command context
    if word ~= nil then -- If theres a game running
        commands.tellraw("@a", text(context:getPlayer():getName():asString().." guessed <bold>"..str.."</bold>"))
        if #str == 1 then -- If the guess is a letter
            local correct = false
            for i = 1, #word do -- Check the word for the letter
                if word[i] == str then -- If there's a match
                    correct = true -- Mark the guess as correct
                    guessed[i] = true -- Mark all letters in the word that match as guessed
                end
            end
            if correct then -- If the guess was marked as correct
                commands.tellraw("@a", text("<green>"..context:getPlayer():getName():asString().." guessed a letter correctly!</green>"))
            else
                commands.tellraw("@a", text("<red>"..context:getPlayer():getName():asString().." guessed a letter incorrectly!</red>"))
                guesses = guesses-1 -- Subtract a guess
            end
        else -- If the guess is a word
            if str == table.concat(word, "") then -- If the guessed word is an exact match
                commands.tellraw("@a", text("<green>"..context:getPlayer():getName():asString().." guessed the word! It was: <bold>"..table.concat(word, "").."</bold></green>"))
                word = nil -- Clear the word, ending the game
                guesses = 0
                return 1
            else -- Otherwise the guess is incorrect
                commands.tellraw("@a", text("<red>"..context:getPlayer():getName():asString().." guessed incorrectly! </red>"))
                guesses = guesses-1 -- Subtract a guess
            end
        end
        if guesses > 0 then -- So long as the game has guesses left
            commands.tellraw("@a", text("<bold>"..parseGuess().."</bold>"))
            local s = " guesses"
            if guesses == 1 then s = " guess" end -- Handle the English language
            commands.tellraw("@a", text(tostring(guesses)..s.." left"))
        else -- No guesses left, game over!
            commands.tellraw("@a", text("<red><bold>Game over!</bold> The word was: <bold>"..table.concat(word, "").."</bold></red>"))
            word = nil
        end
    end
    return 1
end)

allium.command(builder) -- Register the command