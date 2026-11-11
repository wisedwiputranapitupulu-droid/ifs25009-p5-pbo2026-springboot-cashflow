package org.delcom.ifs25009springbootcashflow.adapter.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Menyimpan file unggahan (foto profil dan bukti transaksi) ke folder UPLOAD_DIR. */
public class FileStorage {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");

    private final Path root;

    public FileStorage(String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public Path getRoot() {
        return root;
    }

    /** Menyimpan stream sebagai {folder}/{uuid}.{ext} lalu mengembalikan path relatif tersebut. */
    public String store(String folder, String originalFilename, InputStream content) throws IOException {
        String extension = extensionOf(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("Format file tidak didukung (gunakan PNG, JPG, JPEG, WEBP, atau GIF)");
        }
        String cleanFolder = folder == null ? "" : folder.replaceAll("[^A-Za-z0-9_-]", "");
        Path directory = cleanFolder.isEmpty() ? root : root.resolve(cleanFolder);
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + "." + extension;
        Files.copy(content, directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return cleanFolder.isEmpty() ? filename : cleanFolder + "/" + filename;
    }

    /** Menghapus file bila ada dan berada di dalam folder upload. */
    public boolean delete(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            return false;
        }
        return Files.deleteIfExists(target);
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
