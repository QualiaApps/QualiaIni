package com.qiushui1012.lib.qualia.ini;

/**
 * A text value. The literal is quoted and escaped whenever writing it back
 * would otherwise lose information or change the inferred type.
 */
public final class IniString extends IniValue {
    private final String value;

    IniString(final String value, final String literal) {
        super(literal);
        this.value = value;
    }

    /**
     * Creates a text value from an arbitrary string.
     *
     * @param value the logical text, stored without quotes
     * @return a value that serializes to a literal preserving {@code value}
     */
    public static IniString of(final String value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return new IniString(value, IniLiterals.quote(value));
    }

    @Override
    public IniType type() {
        return IniType.STRING;
    }

    @Override
    public String asString() {
        return this.value;
    }
}
