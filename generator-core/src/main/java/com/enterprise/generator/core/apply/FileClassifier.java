package com.enterprise.generator.core.apply;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Decides whether a file should be treated as text (and therefore eligible for content
 * substitution) or as a binary blob (copied verbatim).
 *
 * <p>Primary signal is the lowercase extension. For files with no extension or an unknown one, the
 * classifier reads the first chunk and looks for NUL bytes: a file is treated as binary if it
 * contains any NUL byte in its first 8 KiB.
 */
public final class FileClassifier {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "java", "kt", "scala", "groovy",
            "xml", "pom", "yml", "yaml", "properties", "toml", "ini",
            "md", "txt", "adoc", "rst",
            "json", "jsonc",
            "html", "htm", "css", "svg",
            "js", "mjs", "ts", "tsx", "jsx",
            "sh", "bash", "zsh", "fish", "cmd", "bat", "ps1",
            "sql",
            "gradle", "gradle.kts",
            "conf", "cfg", "env",
            "gitignore", "gitattributes", "editorconfig", "dockerignore");

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            "class", "jar", "war", "ear", "zip", "tar", "gz", "tgz", "7z", "rar",
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "webp", "tif", "tiff",
            "pdf",
            "mp3", "mp4", "wav", "ogg", "flac", "mov", "avi", "mkv",
            "woff", "woff2", "ttf", "eot", "otf",
            "exe", "dll", "so", "dylib", "o", "a",
            "ks", "p12", "jks", "pfx");

    private static final int SNIFF_LIMIT = 8192;

    private FileClassifier() {
    }

    /** @return {@code true} if the file looks like text and may be transformed. */
    public static boolean isText(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase();
        String ext = extension(name);
        if (BINARY_EXTENSIONS.contains(ext)) {
            return false;
        }
        if (TEXT_EXTENSIONS.contains(ext)) {
            return true;
        }
        // Handle dot-prefixed files (e.g. ".gitignore") where our extension helper returns the
        // whole name.
        if (name.startsWith(".") && TEXT_EXTENSIONS.contains(name.substring(1))) {
            return true;
        }
        // Fall back to byte sniffing.
        return !containsNullByte(file);
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1);
    }

    private static boolean containsNullByte(Path file) throws IOException {
        try (var in = Files.newInputStream(file)) {
            byte[] buf = new byte[SNIFF_LIMIT];
            int read = in.read(buf);
            for (int i = 0; i < read; i++) {
                if (buf[i] == 0) {
                    return true;
                }
            }
            return false;
        }
    }
}
