package me.hugeblank.allium.lua.api.http;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import me.hugeblank.allium.lua.api.JsonLib;
import me.hugeblank.allium.lua.type.LuaWrapped;
import me.hugeblank.allium.lua.type.OptionalArg;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

import java.lang.ref.Cleaner;
import java.nio.charset.Charset;

@LuaWrapped
public class LuaByteBuf {
    private static final Cleaner CLEANER = Cleaner.create();

    private final ByteBuf raw;
    private Charset preferredCharset;

    public LuaByteBuf(ByteBuf raw, Charset preferredCharset) {
        this.raw = raw;
        this.preferredCharset = preferredCharset;

        CLEANER.register(this, new BufferCleanupAction(raw));
    }

    @LuaWrapped
    public LuaByteBuf writeBoolean(boolean value) {
        raw.writeBoolean(value);
        return this;
    }

    @LuaWrapped
    public boolean readBoolean() {
        return raw.readBoolean();
    }

    @LuaWrapped
    public LuaByteBuf writeByte(int value) {
        raw.writeByte(value);
        return this;
    }

    @LuaWrapped
    public byte readByte() {
        return raw.readByte();
    }

    @LuaWrapped
    public LuaByteBuf writeShort(int value, @OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            raw.writeShortLE(value);
        else
            raw.writeShort(value);

        return this;
    }

    @LuaWrapped
    public short readShort(@OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            return raw.readShortLE();
        else
            return raw.readShort();
    }

    @LuaWrapped
    public LuaByteBuf writeInt(int value, @OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            raw.writeIntLE(value);
        else
            raw.writeInt(value);

        return this;
    }

    @LuaWrapped
    public int readInt(@OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            return raw.readIntLE();
        else
            return raw.readInt();
    }

    @LuaWrapped
    public LuaByteBuf writeLong(long value, @OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            raw.writeLongLE(value);
        else
            raw.writeLong(value);

        return this;
    }

    @LuaWrapped
    public long readLong(@OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            return raw.readLongLE();
        else
            return raw.readLong();
    }

    @LuaWrapped
    public LuaByteBuf writeFloat(float value, @OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            raw.writeFloatLE(value);
        else
            raw.writeFloat(value);

        return this;
    }

    @LuaWrapped
    public float readFloat(@OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            return raw.readFloatLE();
        else
            return raw.readFloat();
    }

    @LuaWrapped
    public LuaByteBuf writeDouble(double value, @OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            raw.writeDoubleLE(value);
        else
            raw.writeDouble(value);

        return this;
    }

    @LuaWrapped
    public double readDouble(@OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            return raw.readDoubleLE();
        else
            return raw.readDouble();
    }

    @LuaWrapped
    public LuaByteBuf writeBytes(byte[] bytes) {
        raw.writeBytes(bytes);

        return this;
    }

    @LuaWrapped
    public byte[] readBytes(int length) {
        byte[] arr = new byte[length];
        raw.readBytes(arr);
        return arr;
    }

    @LuaWrapped
    public LuaByteBuf writeString(String text, @OptionalArg String charset) {
        raw.writeBytes(text.getBytes(getCharsetFor(charset)));
        return this;
    }

    @LuaWrapped
    public LuaByteBuf writeJson(LuaValue value, @OptionalArg String charset) throws LuaError {
        return writeString(JsonLib.toJson(value), charset);
    }

    @LuaWrapped
    public String readString(int binaryLength, @OptionalArg String charset) {
        return raw.readBytes(binaryLength).toString(getCharsetFor(charset));
    }

    @LuaWrapped
    public String asString(@OptionalArg String charset) {
        return raw.toString(getCharsetFor(charset));
    }

    @LuaWrapped
    public LuaValue asJson(@OptionalArg String charset) {
        return JsonLib.fromJson(raw.toString(getCharsetFor(charset)));
    }

    private Charset getCharsetFor(String name) {
        if (name == null) return preferredCharset;
        else return Charset.forName(name);
    }

    @LuaWrapped
    public LuaByteBuf setCharset(String name) {
        this.preferredCharset = Charset.forName(name);
        return this;
    }

    @LuaWrapped
    public String getCharset() {
        return this.preferredCharset.name();
    }

    @LuaWrapped
    public int readableBytes() {
        return raw.readableBytes();
    }

    @LuaWrapped
    public ByteBuf getRaw() {
        return raw;
    }

    private record BufferCleanupAction(ReferenceCounted obj) implements Runnable {
        @Override
        public void run() {
            this.obj.release();
        }
    }
}
