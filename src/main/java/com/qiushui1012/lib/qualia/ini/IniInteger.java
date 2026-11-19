package com.qiushui1012.lib.qualia.ini;

import java.math.BigDecimal;

/**
 * An integral value. The literal is kept so that a parsed document serializes
 * back unchanged, including forms such as {@code +007}.
 */
public final class IniInteger extends IniValue {
    private final long value;

    IniInteger(final long value, final String literal) {
        super(literal);
        this.value = value;
    }

    /**
     * Creates an integral value.
     *
     * @param value the number
     * @return a value that serializes to the decimal form of {@code value}
     */
    public static IniInteger of(final long value) {
        return new IniInteger(value, Long.toString(value));
    }

    @Override
    public IniType type() {
        return IniType.INTEGER;
    }

    @Override
    public long asLong() {
        return this.value;
    }

    @Override
    public int asInt() {
        if (this.value < Integer.MIN_VALUE || this.value > Integer.MAX_VALUE) {
            throw new IniException("Integer literal out of range: " + this.serialize());
        }
        return (int) this.value;
    }

    @Override
    public BigDecimal asDecimal() {
        return BigDecimal.valueOf(this.value);
    }

    @Override
    public String asString() {
        return Long.toString(this.value);
    }
}
