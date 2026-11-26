package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Where the project's data folders are, wherever the program is started from (the project folder, src, src/main ...).

public final class ProjectPaths {
    private ProjectPaths() {
    }

    // The folder that contains "resources" and "saves": src in the current layout, main in the older one.
    public static Path dataRoot() {
        Path here = Path.of("").toAbsolutePath();

        // 1) started from the project folder: <here>/src/main exists
        if (Files.isDirectory(here.resolve("src").resolve("main")))
            return here.resolve("src");

        // 2) started from inside src, or a folder below it (src/main, src/main/UI ...): walk up to the src folder
        for (Path p = here; p != null; p = p.getParent())
            if (p.getFileName() != null && p.getFileName().toString().equalsIgnoreCase("src")
                    && Files.isDirectory(p.resolve("main")))
                return p;

        // 3) older layout without a src folder: the main folder itself
        if (here.getFileName() != null && here.getFileName().toString().equalsIgnoreCase("main"))
            return here;
        if (Files.isDirectory(here.resolve("main")))
            return here.resolve("main");

        // 4) nothing recognisable: use a src folder below the current folder
        return here.resolve("src");
    }

    // src/resources: the pictures.
    public static Path resourcesDirectory() {
        return dataRoot().resolve("resources");
    }

    /*
    - src/saves: the saved players. If it does not exist yet, a "saves" folder left in an old location
    (next to src, or in a main folder next to src) is moved here once, so no player is lost.
    */
    public static Path savesDirectory() {
        Path target = dataRoot().resolve("saves");
        if (!Files.exists(target)) {
            Path here = Path.of("").toAbsolutePath();
            for (Path old : new Path[] { here.resolve("saves"), here.resolve("main").resolve("saves") }) {
                if (!old.equals(target) && Files.isDirectory(old)) {
                    try {
                        Files.createDirectories(target.getParent());
                        Files.move(old, target);
                    } catch (IOException e) {
                        // carry on with the new (empty) folder
                    }
                    break;
                }
            }
        }
        return target;
    }
}