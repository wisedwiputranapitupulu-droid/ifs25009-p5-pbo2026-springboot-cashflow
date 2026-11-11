package org.delcom.ifs25009springbootcashflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DotEnvTest {

    private static final String KEY = "APP_PORT_TEST_CASHFLOW";
    private static final String QUOTED = "APP_PORT_QUOTED_CASHFLOW";
    private static final String SINGLE = "APP_PORT_SINGLE_CASHFLOW";

    @AfterEach
    void clearProperties() {
        System.clearProperty(KEY);
        System.clearProperty(QUOTED);
        System.clearProperty(SINGLE);
    }

    @Test
    void loadReadsEnvFile(@TempDir Path tempDir) throws Exception {
        Path env = tempDir.resolve(".env");
        Files.writeString(env, """
                # komentar
                %s=9090

                %s="7070"
                %s='6060'
                =invalid
                NOEQUALS
                """.formatted(KEY, QUOTED, SINGLE));

        DotEnv.load(env);

        assertEquals("9090", System.getProperty(KEY));
        assertEquals("7070", System.getProperty(QUOTED));
        assertEquals("6060", System.getProperty(SINGLE));
    }

    @Test
    void loadIgnoresMissingFileAndExistingProperty(@TempDir Path tempDir) throws Exception {
        DotEnv.load(tempDir.resolve("tidak-ada.env"));
        assertNull(System.getProperty(KEY));

        System.setProperty(KEY, "1111");
        Path env = tempDir.resolve(".env");
        Files.writeString(env, KEY + "=2222\n");
        DotEnv.load(env);

        assertEquals("1111", System.getProperty(KEY));
    }

    @Test
    void loadWithoutArgumentUsesWorkingDirectory() throws Exception {
        DotEnv.load();
    }

    @Test
    void quoteStrippingOnlyAppliesToMatchingPairs() {
        DotEnv.applyLine(KEY + "=\"abc'");
        assertEquals("\"abc'", System.getProperty(KEY));
        System.clearProperty(KEY);
        DotEnv.applyLine(KEY + "=x");
        assertEquals("x", System.getProperty(KEY));
    }

    @Test
    void privateConstructorIsReachableOnlyByReflection() throws Exception {
        Constructor<DotEnv> constructor = DotEnv.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
