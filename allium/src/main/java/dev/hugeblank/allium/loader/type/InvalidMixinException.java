package dev.hugeblank.allium.loader.type;

public class InvalidMixinException extends Exception {
    public InvalidMixinException(Type type, String message) {
        super(switch (type) {
            case INVALID_DESCRIPTOR -> "Could not find method matching descriptor: "+message;
            case INVALID_CLASSTYPE -> "Attempt to use "+message+" method on non-"+message+" mixin.";
        });

    }

    public enum Type {
        INVALID_DESCRIPTOR,
        INVALID_CLASSTYPE,
    }
}
