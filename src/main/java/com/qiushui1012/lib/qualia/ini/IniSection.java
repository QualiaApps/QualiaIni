package com.qiushui1012.lib.qualia.ini;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A named group of entries.
 *
 * <p>Entries keep insertion order and every mutation invalidates the text
 * cached by the owning {@link Ini}, so repeated serialization costs nothing
 * until the section changes.
 */
public final class IniSection {
    private final String name;
    private final Map<String, IniValue> entries = new LinkedHashMap<>();
    private Runnable listener = IniSection.noop();

    IniSection(final String name) {
        final String checked = IniSection.checkName(name);
        this.name = checked;
    }

    /**
     * Creates a section that is not attached to a document.
     *
     * @param name section name, written between square brackets
     * @return an empty section
     * @throws IniException if the name cannot be written back verbatim
     */
    public static IniSection of(final String name) {
        return new IniSection(name);
    }

    private static Runnable noop() {
        return new Runnable() {
            @Override
            public void run() {
            }
        };
    }

    private static String checkName(final String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (name.isEmpty()) {
            throw new IniException("Section name must not be empty");
        }
        if (name.indexOf(']') >= 0 || name.indexOf('[') >= 0 || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {
            throw new IniException("Section name contains a reserved character: " + name);
        }
        if (!name.equals(name.trim())) {
            throw new IniException("Section name must not have leading or trailing whitespace: " + name);
        }
        return name;
    }

    private String checkKey(final String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.isEmpty()) {
            throw new IniException("Entry key must not be empty in section '" + this.name + "'");
        }
        if (key.indexOf('=') >= 0 || key.indexOf('\n') >= 0 || key.indexOf('\r') >= 0) {
            throw new IniException("Entry key contains a reserved character: " + key);
        }
        if (!key.equals(key.trim())) {
            throw new IniException("Entry key must not have leading or trailing whitespace: " + key);
        }
        return key;
    }

    void attach(final Runnable listener) {
        this.listener = listener == null ? IniSection.noop() : listener;
    }

    private void modified() {
        this.listener.run();
    }

    /**
     * Returns the section name.
     *
     * @return the name written between square brackets
     */
    public String name() {
        return this.name;
    }

    /**
     * Stores an already typed value.
     *
     * @param key entry key
     * @param value value to store, replacing any previous one
     * @return this section
     */
    public IniSection put(final String key, final IniValue value) {
        final String checked = this.checkKey(key);
        if (value == null) {
            throw new NullPointerException("value");
        }
        this.entries.put(checked, value);
        this.modified();
        return this;
    }

    /**
     * Stores a text value, quoting it when needed.
     *
     * @param key entry key
     * @param value logical text
     * @return this section
     */
    public IniSection put(final String key, final String value) {
        return this.put(key, IniString.of(value));
    }

    /**
     * Stores an integral value.
     *
     * @param key entry key
     * @param value the number
     * @return this section
     */
    public IniSection put(final String key, final long value) {
        return this.put(key, IniInteger.of(value));
    }

    /**
     * Stores an integral value.
     *
     * @param key entry key
     * @param value the number
     * @return this section
     */
    public IniSection put(final String key, final int value) {
        return this.put(key, IniInteger.of(value));
    }

    /**
     * Stores a decimal value.
     *
     * @param key entry key
     * @param value the number
     * @return this section
     */
    public IniSection put(final String key, final BigDecimal value) {
        return this.put(key, IniDecimal.of(value));
    }

    /**
     * Stores a decimal value.
     *
     * @param key entry key
     * @param value the number
     * @return this section
     */
    public IniSection put(final String key, final double value) {
        return this.put(key, IniDecimal.of(value));
    }

    /**
     * Stores a boolean value.
     *
     * @param key entry key
     * @param value the flag
     * @return this section
     */
    public IniSection put(final String key, final boolean value) {
        return this.put(key, IniBoolean.of(value));
    }

    /**
     * Looks up an entry.
     *
     * @param key entry key
     * @return the stored value, or {@code null} when absent
     */
    public IniValue get(final String key) {
        return this.entries.get(key);
    }

    /**
     * Looks up an entry and fails when it is absent.
     *
     * @param key entry key
     * @return the stored value
     * @throws IniException if the key is absent
     */
    public IniValue require(final String key) {
        final IniValue value = this.entries.get(key);
        if (value == null) {
            throw new IniException("Missing '" + key + "' in section '" + this.name + "'");
        }
        return value;
    }

    /**
     * Tells whether an entry exists.
     *
     * @param key entry key
     * @return true when present
     */
    public boolean has(final String key) {
        return this.entries.containsKey(key);
    }

    /**
     * Removes an entry.
     *
     * @param key entry key
     * @return this section
     */
    public IniSection remove(final String key) {
        if (this.entries.remove(key) != null) {
            this.modified();
        }
        return this;
    }

    /**
     * Reads an entry as text.
     *
     * @param key entry key
     * @param fallback value used when the key is absent
     * @return the text form, or {@code fallback}
     */
    public String getString(final String key, final String fallback) {
        final IniValue value = this.entries.get(key);
        return value == null ? fallback : value.asString();
    }

    /**
     * Reads an entry as a {@code long}.
     *
     * @param key entry key
     * @param fallback value used when the key is absent
     * @return the integer form, or {@code fallback}
     */
    public long getLong(final String key, final long fallback) {
        final IniValue value = this.entries.get(key);
        return value == null ? fallback : value.asLong();
    }

    /**
     * Reads an entry as an {@code int}.
     *
     * @param key entry key
     * @param fallback value used when the key is absent
     * @return the integer form, or {@code fallback}
     */
    public int getInt(final String key, final int fallback) {
        final IniValue value = this.entries.get(key);
        return value == null ? fallback : value.asInt();
    }

    /**
     * Reads an entry as a decimal.
     *
     * @param key entry key
     * @param fallback value used when the key is absent
     * @return the decimal form, or {@code fallback}
     */
    public BigDecimal getDecimal(final String key, final BigDecimal fallback) {
        final IniValue value = this.entries.get(key);
        return value == null ? fallback : value.asDecimal();
    }

    /**
     * Reads an entry as a boolean.
     *
     * @param key entry key
     * @param fallback value used when the key is absent
     * @return the boolean form, or {@code fallback}
     */
    public boolean getBoolean(final String key, final boolean fallback) {
        final IniValue value = this.entries.get(key);
        return value == null ? fallback : value.asBoolean();
    }

    /**
     * Returns the entry keys in insertion order.
     *
     * @return an unmodifiable key list
     */
    public List<String> keys() {
        return Collections.unmodifiableList(new ArrayList<>(this.entries.keySet()));
    }

    /**
     * Returns the entry count.
     *
     * @return number of entries
     */
    public int size() {
        return this.entries.size();
    }

    /**
     * Tells whether the section holds no entry.
     *
     * @return true when empty
     */
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /**
     * Serializes this section, including its header and a trailing line break.
     *
     * @return the section text
     */
    public String serialize() {
        final StringBuilder builder = new StringBuilder();
        builder.append('[').append(this.name).append(']').append('\n');
        for (final Map.Entry<String, IniValue> entry : this.entries.entrySet()) {
            builder.append(entry.getKey()).append(" = ").append(entry.getValue().serialize()).append('\n');
        }
        return builder.toString();
    }

    /**
     * Writes this section without building the whole text in memory.
     *
     * @param writer destination, flushed but not closed
     * @throws IOException if the writer fails
     */
    public void serialize(final Writer writer) throws IOException {
        writer.write('[');
        writer.write(this.name);
        writer.write(']');
        writer.write('\n');
        for (final Map.Entry<String, IniValue> entry : this.entries.entrySet()) {
            writer.write(entry.getKey());
            writer.write(" = ");
            writer.write(entry.getValue().serialize());
            writer.write('\n');
        }
        writer.flush();
    }

    @Override
    public String toString() {
        return this.serialize();
    }
}
