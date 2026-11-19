package com.qiushui1012.lib.qualia.ini;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IniTest {

    @Test
    void parsesSectionsInDocumentOrder() {
        final Ini ini = Ini.parse("[alpha]\nfirst = 1\n\n[beta]\nsecond = 2\n");

        assertEquals(Arrays.asList("alpha", "beta"), ini.names());
        assertEquals(2, ini.size());
        assertTrue(ini.hasSection("alpha"));
        assertNotNull(ini.sectionOrNull("beta"));
        assertNull(ini.sectionOrNull("gamma"));
    }

    @Test
    void infersValueTypesFromLiterals() {
        final IniSection section = Ini.parse(
            "[s]\n"
                + "text = hello\n"
                + "number = -12\n"
                + "decimal = 3.10\n"
                + "flag = TRUE\n"
                + "quoted = \"42\"\n"
                + "empty =\n"
        ).section("s");

        assertEquals(IniType.STRING, section.require("text").type());
        assertEquals(IniType.INTEGER, section.require("number").type());
        assertEquals(IniType.DECIMAL, section.require("decimal").type());
        assertEquals(IniType.BOOLEAN, section.require("flag").type());
        assertEquals(IniType.STRING, section.require("quoted").type());
        assertEquals(IniType.STRING, section.require("empty").type());

        assertEquals(-12L, section.getLong("number", 0L));
        assertEquals(new BigDecimal("3.10"), section.getDecimal("decimal", BigDecimal.ZERO));
        assertTrue(section.getBoolean("flag", false));
        assertEquals("42", section.getString("quoted", ""));
        assertEquals("", section.getString("empty", "fallback"));
    }

    @Test
    void keepsLiteralSoParsedDocumentsSerializeUnchanged() {
        final String text = "[s]\nnumber = +007\ndecimal = 2.500\nflag = True\nquoted = \"a b\"\n";

        assertEquals(text, Ini.parse(text).serialize());
    }

    @Test
    void stripsInlineCommentsOutsideQuotes() {
        final IniSection section = Ini.parse(
            "[s]\n"
                + "plain = value # note\n"
                + "semicolon = value ; note\n"
                + "escaped = a\\#b\n"
                + "quoted = \"a # b\"\n"
        ).section("s");

        assertEquals("value", section.getString("plain", ""));
        assertEquals("value", section.getString("semicolon", ""));
        assertEquals("a\\#b", section.getString("escaped", ""));
        assertEquals("a # b", section.getString("quoted", ""));
    }

    @Test
    void skipsLinesThatAreNotEntries() {
        final Ini ini = Ini.parse("stray = before\nnothing here\n# comment\n[s]\nnot an entry\nkey = value\n");

        assertEquals(Collections.singletonList("s"), ini.names());
        assertEquals(1, ini.section("s").size());
        assertFalse(ini.section("s").has("not an entry"));
    }

    @Test
    void acceptsEmptySection() {
        final Ini ini = Ini.parse("[empty]\n");

        assertTrue(ini.section("empty").isEmpty());
        assertEquals("[empty]\n", ini.serialize());
    }

    @Test
    void rejectsDuplicateSection() {
        final IniException error = assertThrows(
            IniException.class,
            () -> Ini.parse("[s]\na = 1\n[s]\nb = 2\n")
        );

        assertTrue(error.getMessage().contains("duplicate"));
    }

    @Test
    void rejectsUnterminatedSectionHeader() {
        assertThrows(IniException.class, () -> Ini.parse("[broken\nkey = value\n"));
    }

    @Test
    void quotesValuesThatWouldOtherwiseChangeMeaning() {
        final IniSection section = Ini.empty().section("s");
        section.put("plain", "hello");
        section.put("numeric", "007");
        section.put("spaced", " padded ");
        section.put("commented", "a#b");
        section.put("quoted", "say \"hi\"");
        section.put("flag", "true");
        section.put("number", 12);
        section.put("decimal", new BigDecimal("1.50"));
        section.put("enabled", true);

        assertEquals(
            "[s]\n"
                + "plain = hello\n"
                + "numeric = \"007\"\n"
                + "spaced = \" padded \"\n"
                + "commented = \"a#b\"\n"
                + "quoted = \"say \\\"hi\\\"\"\n"
                + "flag = \"true\"\n"
                + "number = 12\n"
                + "decimal = 1.50\n"
                + "enabled = true\n",
            section.serialize()
        );
    }

    @Test
    void roundTripsProgrammaticValues() {
        final Ini ini = Ini.empty();
        ini.section("s")
            .put("plain", "hello")
            .put("numeric", "007")
            .put("spaced", " padded ")
            .put("commented", "a#b")
            .put("escaped", "back\\slash")
            .put("multiline", "a\nb")
            .put("number", 12L)
            .put("decimal", new BigDecimal("1.50"))
            .put("enabled", false);

        final IniSection reparsed = Ini.parse(ini.serialize()).section("s");

        assertEquals("hello", reparsed.getString("plain", ""));
        assertEquals("007", reparsed.getString("numeric", ""));
        assertEquals(" padded ", reparsed.getString("spaced", ""));
        assertEquals("a#b", reparsed.getString("commented", ""));
        assertEquals("back\\slash", reparsed.getString("escaped", ""));
        assertEquals("a\nb", reparsed.getString("multiline", ""));
        assertEquals(12L, reparsed.getLong("number", 0L));
        assertEquals(new BigDecimal("1.50"), reparsed.getDecimal("decimal", BigDecimal.ZERO));
        assertFalse(reparsed.getBoolean("enabled", true));
    }

    @Test
    void cachesTextUntilDocumentChanges() {
        final Ini ini = Ini.parse("[s]\nkey = value\n");
        final String first = ini.serialize();

        assertSame(first, ini.serialize());

        ini.section("s").put("extra", 1);

        assertEquals("[s]\nkey = value\nextra = 1\n", ini.serialize());
    }

    @Test
    void reusesCacheAfterSectionRemoval() {
        final Ini ini = Ini.empty();
        ini.section("a").put("key", 1);
        ini.section("b").put("key", 2);
        ini.serialize();

        ini.removeSection("a");

        assertEquals("[b]\nkey = 2\n", ini.serialize());
    }

    @Test
    void writesThroughStreamsAndWriters() throws IOException {
        final Ini ini = Ini.empty();
        ini.section("s").put("text", "值").put("number", 7);

        final StringWriter writer = new StringWriter();
        ini.serialize(writer);
        assertEquals(ini.serialize(), writer.toString());

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ini.serialize(bytes);
        assertEquals(ini.serialize(), new String(bytes.toByteArray(), StandardCharsets.UTF_8));

        final ByteArrayOutputStream wrapped = new ByteArrayOutputStream();
        final Writer streamWriter = new OutputStreamWriter(wrapped, StandardCharsets.UTF_8);
        ini.serialize(streamWriter);
        assertEquals(ini.serialize(), new String(wrapped.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void readsAndWritesFiles(@TempDir final Path dir) throws IOException {
        final Path file = dir.resolve("nested").resolve("config.ini");
        final Ini ini = Ini.empty();
        ini.section("s").put("key", "value");

        ini.writeTo(file);
        assertTrue(Files.exists(file));
        assertEquals(ini.serialize(), new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        assertEquals(ini.serialize(), Ini.parse(file).serialize());
    }

    @Test
    void reportsMissingEntries() {
        final IniSection section = Ini.parse("[s]\nkey = value\n").section("s");

        final IniException error = assertThrows(IniException.class, () -> section.require("absent"));
        assertTrue(error.getMessage().contains("absent"));

        assertEquals("fallback", section.getString("absent", "fallback"));
        assertEquals(5L, section.getLong("absent", 5L));
        assertEquals(5, section.getInt("absent", 5));
        assertEquals(BigDecimal.ONE, section.getDecimal("absent", BigDecimal.ONE));
        assertTrue(section.getBoolean("absent", true));
    }

    @Test
    void rejectsValuesThatCannotConvert() {
        final IniSection section = Ini.parse("[s]\ntext = hello\n").section("s");

        assertThrows(IniException.class, () -> section.getLong("text", 0L));
        assertThrows(IniException.class, () -> section.getBoolean("text", false));
        assertThrows(IniException.class, () -> section.getInt("text", 0));
    }

    @Test
    void rejectsNamesThatCannotBeWrittenBack() {
        assertThrows(IniException.class, () -> Ini.empty().section(""));
        assertThrows(IniException.class, () -> Ini.empty().section("bad]name"));
        assertThrows(IniException.class, () -> Ini.empty().section("s").put("", 1));
        assertThrows(IniException.class, () -> Ini.empty().section("s").put("bad=key", 1));
    }

    @Test
    void exposesSectionAndKeyOrder() {
        final IniSection section = Ini.parse("[s]\nb = 1\na = 2\n").section("s");

        assertEquals(Arrays.asList("b", "a"), section.keys());
        assertEquals(2, section.size());
    }

    @Test
    void removesEntries() {
        final IniSection section = Ini.parse("[s]\na = 1\nb = 2\n").section("s");

        section.remove("a");

        assertFalse(section.has("a"));
        assertEquals("[s]\nb = 2\n", section.serialize());
    }
}
