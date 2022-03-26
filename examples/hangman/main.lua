-- Hangman
-- By hugeblank - March 22, 2022
-- A game of hangman, played in the chat. Use !hangman to start a game, add a letter or word after to start guessing.
-- Derived from the original !hangman command in alpha, for allium-cc
-- Source: https://github.com/hugeblank/Alpha/blob/master/alpha.lua#L354

local words = require "words"
local CommandManager = java.import("CommandManager") -- We need the java command manager for creating commands.
local MessageType = java.import("network.MessageType") -- "net.minecraft." among other packages are auto-filled for you!
local Util = java.import("Util")
local arguments = command.arguments -- Create shortcut for command argument types

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

local function broadcast(context, text) -- easily broadcast a message to all players
    context
            :getSource()
            :getWorld()
            :getServer()
            :getPlayerManager()
            :broadcast(texts.parseSafe(text), MessageType.CHAT, Util.NIL_UUID)
end

local builder = CommandManager.literal("hangman") -- Create the builder for the hangman command

allium.onEvent("command_register", function(_, _, _, success)
    -- Let us know if the command was successfully registered
    if success then
        print("/hangman command registered!")
    else
        print("/hangman command failed to register!")
    end
end)

builder:executes(function(context) -- The part of the command with no values attached
    if word ~= nil then -- If there's a game being played tell the player to guess
        context:getSource():sendFeedback(texts.parseSafe("<red>No guess! Add a letter or word to guess</red>"), false)
        return 0 -- Execution handlers expect an integer return value.
        -- We use 0 to indicate error, and 1 to indicate success.
    else -- Start a game, since there's not one currently playing
        word = java.split(words[math.random(1, #words)], "")
        guessed = {} -- Create a table to mark guessed characters in the word
        for i = 1, #word do
        guessed[i] = false
        end
        guesses = 10 -- Give 10 guesses. Could be increased to reduce the difficulty.
        broadcast(context, string.format(
                "Guess the word!\n<bold>%s</bold>\nYou have %d guesses. Good luck!",
                parseGuess(),
                guesses
        ))
        return 1
    end
end)

builder:m_then(CommandManager.argument("guess", arguments.string.word()):executes(function(context)
    -- The part of the command that handles guesses
    local playerName = context:getSource():getPlayer():getName():asString()
    local broadcastWin = function() -- easily broadcast win message
        broadcast(context, string.format(
                        "<green>%s guessed the word! It was: <bold>%s</bold></green>",
                        playerName,
                        table.concat(word, "")
                ))
        word = nil -- Clear the word, ending the game
        guesses = 0
        return 1
    end
    local str = arguments.string.getString(context, "guess") -- Get the guess from the command context
    if word ~= nil then -- If theres a game running
        broadcast(context, playerName.." guessed <bold>"..str.."</bold>")
        if #str == 1 then -- If the guess is a letter
            local correct = false
            local total = 0 -- Keep track of the total number of letters guessed so far
            for i = 1, #word do -- Check the word for the letter
                if word[i] == str and not guessed[i] then -- If there's a new match
                    correct = true -- Mark the guess as correct
                    guessed[i] = true -- Mark all letters in the word that match as guessed
                end
                if guessed[i] then total = total + 1 end -- increment total if the current letter has been guessed
            end
            if total == #word then -- If all letters have been guessed
                return broadcastWin()
            elseif correct then -- If the guess was marked as correct
                broadcast(context, "<green>"..playerName.." guessed a letter correctly!</green>")
            else
                broadcast(context, "<red>"..playerName.." guessed a letter incorrectly!</red>")
                guesses = guesses-1 -- Subtract a guess
            end
        else -- If the guess is a word
            if str == table.concat(word, "") then -- If the guessed word is an exact match
                return broadcastWin()
            else -- Otherwise the guess is incorrect
                broadcast(context, "<red>"..playerName.." guessed incorrectly! </red>")
                guesses = guesses-1 -- Subtract a guess
            end
        end
        if guesses > 0 then -- So long as the game has guesses left
            broadcast(context, "<bold>"..parseGuess().."</bold>")
            local s = " guesses"
            if guesses == 1 then s = " guess" end -- Handle the English language
            broadcast(context, tostring(guesses)..s.." left")
        else -- No guesses left, game over!
            broadcast(context, string.format(
                    "<red><bold>Game over!</bold> The word was: <bold>%s</bold></red>",
                    table.concat(word, "")
            ))
            word = nil
        end
    else -- No game, tell player how to start one
        context:getSource():sendFeedback(
                texts.parseSafe("<red>No game! Use /hangman with no guess to start</red>"),
                false
        )
    end
    return 1
end))

command.register(builder) -- Register the command