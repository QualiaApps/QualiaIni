package com.qiushui1012.lib.qualia.ini;

import java.math.BigDecimal;

/**
 * A decimal value. The literal is kept so that a parsed document serializes
 * back unchanged, including trailing zeros such as {@code 3.10}.
 */
public final class IniDecimal extends IniValue {
    private final BigDecimal value;

    IniDecimal(final BigDecimal value, final String literal) {
        super(literal);
        this.value = value;
    }

    /**
     * Creates a decimal value.
     *
     * @param value the number
     * @return a value that serializes to the plain form of {@code value}
     */
    public static IniDecimal of(final BigDecimal value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return new IniDecimal(value, value.toPlainString());
    }

    /**
     * Creates a decimal value from a {@code double}.
     *
     * @param value the number
     * @return a value backed by {@link BigDecimal#valueOf(double)}
     */
    public static IniDecimal of(final double value) {
        return IniDecimal.of(BigDecimal.valueOf(value));
    }

    @Override
    public IniType type() {
        return IniType.DECIMAL;
    }

    @Override
    public BigDecimal asDecimal() {
        return this.value;
    }

    @Override
    public String asString() {
        return this.value.toPlainString();
    }

    @Override
    public long asLong() {
        try {
            return this.value.longValueExact();
        } catch (final ArithmeticException e) {
            throw new IniException("Not an integral literal: " + this.serialize());
        }
    }

    @Override
    public int asInt() {
        try {
            return this.value.intValueExact();
        } catch (final ArithmeticException e) {
            throw new IniException("Not an integral literal: " + this.serialize());
        }
    }
}
