package com.qiushui1012.lib.qualia.ini;

/**
 * Line scanner turning document text into {@link Ini} instances.
 *
 * <p>Blank lines, comment lines and lines that neither open a section nor carry
 * an {@code =} separator are ignored, so hand-edited files with stray text still
 * load. A duplicate section name and an unterminated section header are
 * reported as {@link IniException}.
 */
final class IniReader {
    private IniReader() {
    }

    /**
     * Parses document text.
     *
     * @param content document text
     * @return the parsed document, empty when it holds no section
     */
    static Ini parse(final String content) {
        final Ini ini = new Ini();
        IniSection current = null;

        for (final String rawLine : content.split("\r\n|\r|\n", -1)) {
            final String line = rawLine.trim();
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }

            if (line.charAt(0) == '[') {
                final int close = line.indexOf(']');
                if (close < 0) {
                    throw new IniException("Unterminated section header: " + line);
                }
                final String name = line.substring(1, close).trim();
                if (ini.hasSection(name)) {
                    throw new IniException("Unexpected duplicate name '" + name + "'");
                }
                current = ini.section(name);
                continue;
            }

            if (current == null) {
                continue;
            }
            final int separator = line.indexOf('=');
            if (separator < 0) {
                continue;
            }
            final String key = line.substring(0, separator).trim();
            if (key.isEmpty()) {
                continue;
            }
            final String literal = IniReader.stripComment(line.substring(separator + 1).trim());
            current.put(key, IniLiterals.infer(literal));
        }

        return ini;
    }

    /**
     * Removes a trailing comment that sits outside quotes and is not escaped.
     *
     * @param value trimmed literal, possibly followed by a comment
     * @return the literal without the comment
     */
    private static String stripComment(final String value) {
        boolean quoted = false;
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (c == '"') {
                quoted = !quoted;
                continue;
            }
            if (!quoted && (c == '#' || c == ';')) {
                return value.substring(0, i).trim();
            }
        }
        return value;
    }
}
