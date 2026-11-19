package com.qiushui1012.lib.qualia.ini;

/**
 * Kinds of value a parsed entry can hold.
 *
 * <p>The type is inferred from the literal written in the document: quoted text
 * is a string, {@code true} and {@code false} are booleans, digit-only text is
 * an integer, and text with a decimal point or exponent is a decimal.
 */
public enum IniType {
    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN
}
