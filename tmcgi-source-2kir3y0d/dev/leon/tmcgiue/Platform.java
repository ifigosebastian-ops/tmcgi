//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.nio.file.Path;
import java.util.Locale;

enum Platform {
    MAC,
    WINDOWS,
    LINUX;

    private static final Platform CURRENT = detect();

    private Platform() {
    }

    private static Platform detect() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (name.contains("win")) {
            return WINDOWS;
        } else {
            return !name.contains("mac") && !name.contains("darwin") ? LINUX : MAC;
        }
    }

    static Platform current() {
        return CURRENT;
    }

    static Path frameFile() {
        if (CURRENT != WINDOWS) {
            return Path.of("/tmp/tmcgi_frame.bin");
        } else {
            String temp = System.getProperty("java.io.tmpdir", "");
            if (temp.isBlank() || temp.contains(" ")) {
                String publicFolder = System.getenv("PUBLIC");
                temp = publicFolder != null && !publicFolder.isBlank() ? publicFolder : "C:\\Users\\Public";
            }

            return Path.of(temp, "tmcgi_frame.bin");
        }
    }
}
