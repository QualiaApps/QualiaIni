package com.qiushui1012.lib.qualia.ini;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An INI document: an ordered list of {@link IniSection} instances.
 *
 * <p>Sections are written as {@code [name]}, entries as {@code key = value}.
 * Lines starting with {@code #} are comments, and a {@code #} or {@code ;}
 * inside a value starts an inline comment unless it is escaped with a backslash
 * or sits inside a double-quoted literal.
 *
 * <p>The text form is built on first use and cached until the document changes,
 * so {@link #serialize()} is cheap to call repeatedly. The stream overloads
 * write straight to the destination without materializing the whole text.
 */
public final class Ini {
    private final Map<String, IniSection> sections = new LinkedHashMap<>();
    private String cache;

    Ini() {
    }

    /**
     * Creates an empty document.
     *
     * @return a document without sections
     */
    public static Ini empty() {
        return new Ini();
    }

    /**
     * Parses an INI file as UTF-8.
     *
     * @param path file to read
     * @return the parsed document
     * @throws IOException if the file cannot be read
     * @throws IniException if the document is structurally invalid
     */
    public static Ini parse(final Path path) throws IOException {
        return Ini.parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    /**
     * Parses INI content held in memory.
     *
     * @param content document text
     * @return the parsed document
     * @throws IniException if the document is structurally invalid
     */
    public static Ini parse(final String content) {
        if (content == null) {
            throw new NullPointerException("content");
        }
        return IniReader.parse(content);
    }

    /**
     * Parses INI content read from a stream, which is drained but not closed.
     *
     * @param source stream holding UTF-8 text
     * @return the parsed document
     * @throws IOException if the stream cannot be read
     * @throws IniException if the document is structurally invalid
     */
    public static Ini parse(final InputStream source) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] chunk = new byte[8192];
        int read;
        while ((read = source.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return Ini.parse(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
    }

    /**
     * Returns the named section, creating it when absent.
     *
     * @param name section name
     * @return the existing or newly created section
     * @throws IniException if the name cannot be written back verbatim
     */
    public IniSection section(final String name) {
        final IniSection existing = this.sections.get(name);
        if (existing != null) {
            return existing;
        }
        final IniSection created = new IniSection(name);
        created.attach(new Runnable() {
            @Override
            public void run() {
                Ini.this.markDirty();
            }
        });
        this.sections.put(name, created);
        this.markDirty();
        return created;
    }

    /**
     * Returns the named section without creating it.
     *
     * @param name section name
     * @return the section, or {@code null} when absent
     */
    public IniSection sectionOrNull(final String name) {
        return this.sections.get(name);
    }

    /**
     * Tells whether a section exists.
     *
     * @param name section name
     * @return true when present
     */
    public boolean hasSection(final String name) {
        return this.sections.containsKey(name);
    }

    /**
     * Removes a section.
     *
     * @param name section name
     * @return this document
     */
    public Ini removeSection(final String name) {
        if (this.sections.remove(name) != null) {
            this.markDirty();
        }
        return this;
    }

    /**
     * Returns the sections in document order.
     *
     * @return an unmodifiable section list
     */
    public List<IniSection> sections() {
        return Collections.unmodifiableList(new ArrayList<>(this.sections.values()));
    }

    /**
     * Returns the section names in document order.
     *
     * @return an unmodifiable name list
     */
    public List<String> names() {
        return Collections.unmodifiableList(new ArrayList<>(this.sections.keySet()));
    }

    /**
     * Returns the section count.
     *
     * @return number of sections
     */
    public int size() {
        return this.sections.size();
    }

    /**
     * Tells whether the document holds no section.
     *
     * @return true when empty
     */
    public boolean isEmpty() {
        return this.sections.isEmpty();
    }

    private void markDirty() {
        this.cache = null;
    }

    /**
     * Serializes the document, reusing the text built by the previous call
     * until the document changes again.
     *
     * @return the document text
     */
    public String serialize() {
        String text = this.cache;
        if (text == null) {
            final StringBuilder builder = new StringBuilder();
            for (final IniSection section : this.sections.values()) {
                builder.append(section.serialize());
            }
            text = builder.toString();
            this.cache = text;
        }
        return text;
    }

    /**
     * Writes the document without building the whole text in memory.
     *
     * @param writer destination, flushed but not closed
     * @throws IOException if the writer fails
     */
    public void serialize(final Writer writer) throws IOException {
        for (final IniSection section : this.sections.values()) {
            section.serialize(writer);
        }
        writer.flush();
    }

    /**
     * Writes the document as UTF-8, leaving the stream open.
     *
     * @param target destination, flushed but not closed
     * @throws IOException if the stream fails
     */
    public void serialize(final OutputStream target) throws IOException {
        final Writer writer = new OutputStreamWriter(target, StandardCharsets.UTF_8);
        this.serialize(writer);
        writer.flush();
    }

    /**
     * Writes the document to a file as UTF-8, creating parent directories.
     *
     * @param path destination file
     * @throws IOException if the file cannot be written
     */
    public void writeTo(final Path path) throws IOException {
        final Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        final OutputStream out = Files.newOutputStream(path);
        try {
            this.serialize(out);
        } finally {
            out.close();
        }
    }

    @Override
    public String toString() {
        return this.serialize();
    }
}
