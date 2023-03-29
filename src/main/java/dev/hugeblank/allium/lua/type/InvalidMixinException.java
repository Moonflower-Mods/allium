package dev.hugeblank.allium.lua.type;

public class InvalidMixinException extends Exception {
    public InvalidMixinException(Type type, String message) {
        super(switch (type) {
            case INVALID_METHOD -> "Could not find method matching descriptor: "+message;
            case MISSING_TARGET -> "Missing injection target (key 'at')";
            case INVALID_TARGET -> "Invalid injection target: "+message;
        });

    }

    public enum Type {
        INVALID_METHOD,
        MISSING_TARGET,
        INVALID_TARGET,
    }
}
