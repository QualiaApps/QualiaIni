package com.qiushui1012.lib.qualia.ini;

/**
 * A boolean value. Only {@code true} and {@code false} are recognized, in any
 * letter case; the literal keeps the casing it was written with.
 */
public final class IniBoolean extends IniValue {
    private final boolean value;

    IniBoolean(final boolean value, final String literal) {
        super(literal);
        this.value = value;
    }

    /**
     * Creates a boolean value.
     *
     * @param value the flag
     * @return a value that serializes to {@code true} or {@code false}
     */
    public static IniBoolean of(final boolean value) {
        return new IniBoolean(value, Boolean.toString(value));
    }

    @Override
    public IniType type() {
        return IniType.BOOLEAN;
    }

    @Override
    public boolean asBoolean() {
        return this.value;
    }

    @Override
    public String asString() {
        return Boolean.toString(this.value);
    }

    @Override
    public long asLong() {
        throw new IniException("Not an integer literal: " + this.serialize());
    }

    @Override
    public int asInt() {
        throw new IniException("Not an integer literal: " + this.serialize());
    }

    @Override
    public java.math.BigDecimal asDecimal() {
        throw new IniException("Not a decimal literal: " + this.serialize());
    }
}
