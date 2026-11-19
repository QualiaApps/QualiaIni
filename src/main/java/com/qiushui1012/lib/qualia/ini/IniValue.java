package com.qiushui1012.lib.qualia.ini;

import java.math.BigDecimal;

/**
 * A single entry value inside a section.
 *
 * <p>Every value keeps the literal it is written with, so a parsed document
 * serializes back to the same text. The typed accessors convert that literal
 * and throw {@link IniException} when it cannot represent the requested type.
 */
public abstract class IniValue {
    private final String literal;

    protected IniValue(final String literal) {
        this.literal = literal;
    }

    /**
     * Returns the literal exactly as it appears in the document, without the
     * surrounding {@code key = } prefix.
     *
     * @return the serializable literal
     */
    public final String serialize() {
        return this.literal;
    }

    /**
     * Returns the kind of this value.
     *
     * @return the inferred type
     */
    public abstract IniType type();

    /**
     * Returns the value as text. Quoted strings are unescaped and lose their
     * quotes.
     *
     * @return the textual form
     */
    public String asString() {
        return this.literal;
    }

    /**
     * Returns the value as a {@code long}.
     *
     * @return the integer form
     * @throws IniException if the literal is not an integer
     */
    public long asLong() {
        try {
            return Long.parseLong(this.literal.trim());
        } catch (final NumberFormatException e) {
            throw new IniException("Not an integer literal: " + this.literal);
        }
    }

    /**
     * Returns the value as an {@code int}.
     *
     * @return the integer form
     * @throws IniException if the literal is not an integer or does not fit
     */
    public int asInt() {
        final long value = this.asLong();
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IniException("Integer literal out of range: " + this.literal);
        }
        return (int) value;
    }

    /**
     * Returns the value as a {@link BigDecimal}.
     *
     * @return the decimal form
     * @throws IniException if the literal is not a decimal
     */
    public BigDecimal asDecimal() {
        try {
            return new BigDecimal(this.literal.trim());
        } catch (final NumberFormatException e) {
            throw new IniException("Not a decimal literal: " + this.literal);
        }
    }

    /**
     * Returns the value as a {@code boolean}.
     *
     * @return the boolean form
     * @throws IniException if the literal is neither {@code true} nor {@code false}
     */
    public boolean asBoolean() {
        final String text = this.literal.trim();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new IniException("Not a boolean literal: " + this.literal);
    }

    @Override
    public String toString() {
        return this.literal;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IniValue)) {
            return false;
        }
        final IniValue that = (IniValue) other;
        return this.type() == that.type() && this.literal.equals(that.literal);
    }

    @Override
    public int hashCode() {
        return 31 * this.type().hashCode() + this.literal.hashCode();
    }
}
