package com.qiushui1012.lib.qualia.ini;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Literal handling shared by the parser and the value types: quoting, escaping
 * and type inference.
 */
final class IniLiterals {
    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL =
        Pattern.compile("[+-]?(?:\\d+\\.\\d*|\\.\\d+|\\d+(?:\\.\\d*)?[eE][+-]?\\d+)");

    private IniLiterals() {
    }

    /**
     * Tells whether a literal is a double-quoted string.
     *
     * @param literal trimmed literal
     * @return true when the literal starts and ends with a double quote
     */
    static boolean isQuoted(final String literal) {
        return literal.length() >= 2 && literal.charAt(0) == '"' && literal.charAt(literal.length() - 1) == '"';
    }

    /**
     * Wraps a logical string into the literal that reproduces it on parse.
     *
     * @param value logical text
     * @return the literal to write after {@code key = }
     */
    static String quote(final String value) {
        if (!IniLiterals.needsQuote(value)) {
            return value;
        }
        final StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }
        builder.append('"');
        return builder.toString();
    }

    /**
     * Tells whether a logical string must be quoted to survive a parse.
     *
     * @param value logical text
     * @return true when writing it bare would lose information
     */
    static boolean needsQuote(final String value) {
        if (value.isEmpty()) {
            return true;
        }
        final char first = value.charAt(0);
        final char last = value.charAt(value.length() - 1);
        if (Character.isWhitespace(first) || Character.isWhitespace(last)) {
            return true;
        }
        if (first == '[' || first == '#' || first == ';') {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c == '#' || c == ';' || c == '"' || c == '\\' || c == '\n' || c == '\r' || c == '\t') {
                return true;
            }
        }
        return IniLiterals.infer(value).type() != IniType.STRING;
    }

    /**
     * Recovers the logical text of a quoted literal.
     *
     * @param literal trimmed literal
     * @return the unescaped text, or the literal itself when it is not quoted
     */
    static String unquote(final String literal) {
        if (!IniLiterals.isQuoted(literal)) {
            return literal;
        }
        final String body = literal.substring(1, literal.length() - 1);
        final StringBuilder builder = new StringBuilder(body.length());
        for (int i = 0; i < body.length(); i++) {
            final char c = body.charAt(i);
            if (c != '\\' || i + 1 >= body.length()) {
                builder.append(c);
                continue;
            }
            final char escaped = body.charAt(++i);
            switch (escaped) {
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                default:
                    builder.append(escaped);
                    break;
            }
        }
        return builder.toString();
    }

    /**
     * Infers the typed value of a literal.
     *
     * @param literal trimmed literal
     * @return the value carrying both the literal and its typed form
     */
    static IniValue infer(final String literal) {
        final String text = literal.trim();
        if (IniLiterals.isQuoted(text)) {
            return new IniString(IniLiterals.unquote(text), text);
        }
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return new IniBoolean(Boolean.parseBoolean(text), text);
        }
        if (IniLiterals.INTEGER.matcher(text).matches()) {
            try {
                return new IniInteger(Long.parseLong(text), text);
            } catch (final NumberFormatException ignored) {
                return new IniString(text, text);
            }
        }
        if (IniLiterals.DECIMAL.matcher(text).matches()) {
            try {
                return new IniDecimal(new BigDecimal(text), text);
            } catch (final NumberFormatException ignored) {
                return new IniString(text, text);
            }
        }
        return new IniString(text, text);
    }
}
