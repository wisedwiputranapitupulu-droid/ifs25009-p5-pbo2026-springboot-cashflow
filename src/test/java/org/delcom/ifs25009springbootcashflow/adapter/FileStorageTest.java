package org.delcom.ifs25009springbootcashflow.adapter;

import org.delcom.ifs25009springbootcashflow.adapter.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageTest {

    @Test
    void storeSavesFileAndDeleteRemovesIt(@TempDir Path tempDir) throws Exception {
        FileStorage storage = new FileStorage(tempDir.toString());
        assertEquals(tempDir.toAbsolutePath().normalize(), storage.getRoot());

        String path = storage.store("proofs", "Bukti.PNG", new ByteArrayInputStream(new byte[]{1, 2, 3}));
        assertTrue(path.startsWith("proofs/") && path.endsWith(".png"));
        assertTrue(Files.exists(tempDir.resolve(path)));

        assertTrue(storage.delete(path));
        assertFalse(storage.delete(path));
    }

    @Test
    void storeWithoutFolderAndSanitizedFolder(@TempDir Path tempDir) throws Exception {
        FileStorage storage = new FileStorage(tempDir.toString());
        String flat = storage.store(null, "a.jpg", new ByteArrayInputStream(new byte[]{1}));
        assertFalse(flat.contains("/"));
        String sanitized = storage.store("../x", "a.jpeg", new ByteArrayInputStream(new byte[]{1}));
        assertTrue(sanitized.startsWith("x/"));
    }

    @Test
    void storeRejectsUnsupportedExtensions(@TempDir Path tempDir) {
        FileStorage storage = new FileStorage(tempDir.toString());
        assertThrows(IOException.class, () -> storage.store("p", "virus.exe", new ByteArrayInputStream(new byte[0])));
        assertThrows(IOException.class, () -> storage.store("p", "tanpa-ekstensi", new ByteArrayInputStream(new byte[0])));
        assertThrows(IOException.class, () -> storage.store("p", null, new ByteArrayInputStream(new byte[0])));
    }

    @Test
    void deleteIgnoresBlankAndPathTraversal(@TempDir Path tempDir) throws Exception {
        FileStorage storage = new FileStorage(tempDir.resolve("uploads").toString());
        assertFalse(storage.delete(null));
        assertFalse(storage.delete("  "));
        Files.writeString(tempDir.resolve("rahasia.txt"), "x");
        assertFalse(storage.delete("../rahasia.txt"));
        assertTrue(Files.exists(tempDir.resolve("rahasia.txt")));
    }
}
