package com.example.iconrandomizer;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

@Plugin(
        id = "iconrandomizer",
        name = "IconRandomizer",
        version = "1.0.0",
        authors = {"e1ixyz"}
)
public final class IconRandomizer {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final Path serverRoot;

    private volatile List<Favicon> icons = Collections.emptyList();

    @Inject
    public IconRandomizer(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.serverRoot = resolveServerRoot(dataDirectory);
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        try {
            Files.createDirectories(dataDirectory);
            IconConfig config = loadConfig();
            icons = loadIcons(config);
            if (icons.isEmpty()) {
                logger.warn("No icons loaded. The default server icon will be used until you add icons.");
            } else {
                logger.info("Loaded {} icons. Rotating on each server list refresh.", icons.size());
            }
        } catch (IOException e) {
            logger.error("Failed to set up IconRandomizer data directory.", e);
        }
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        List<Favicon> currentIcons = icons;
        if (currentIcons.isEmpty()) {
            return;
        }

        Favicon favicon = currentIcons.get(ThreadLocalRandom.current().nextInt(currentIcons.size()));
        ServerPing ping = event.getPing();
        event.setPing(ping.asBuilder().favicon(favicon).build());
    }

    private IconConfig loadConfig() throws IOException {
        Path configPath = dataDirectory.resolve("config.properties");

        if (Files.notExists(configPath)) {
            writeDefaultConfig(configPath);
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            properties.load(in);
        }

        String folder = properties.getProperty("icon-folder", "icons").trim();
        String files = properties.getProperty("icon-files", "").trim();

        List<String> listedFiles = new ArrayList<>();
        if (!files.isBlank()) {
            for (String entry : files.split(",")) {
                String value = entry.trim();
                if (!value.isEmpty()) {
                    listedFiles.add(value);
                }
            }
        }

        return new IconConfig(folder.isBlank() ? "icons" : folder, listedFiles);
    }

    private void writeDefaultConfig(Path configPath) throws IOException {
        String contents = """
                # IconRandomizer configuration
                # icon-folder: folder (relative to your Velocity root) containing .png icons.
                # If icon-files is empty, all .png files in icon-folder will be used.
                icon-folder=icons

                # icon-files: optional comma-separated list of icon paths (relative or absolute).
                # When set, only these files are used and icon-folder is ignored.
                icon-files=
                """;
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, contents);
    }

    private List<Favicon> loadIcons(IconConfig config) {
        List<Path> paths = new ArrayList<>();

        if (!config.iconFiles().isEmpty()) {
            for (String configuredPath : config.iconFiles()) {
                paths.add(resolvePath(configuredPath));
            }
        } else {
            Path folder = resolvePath(config.iconFolder());
            if (Files.isDirectory(folder)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, this::isPng)) {
                    for (Path file : stream) {
                        if (Files.isRegularFile(file)) {
                            paths.add(file);
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Failed to read icons from folder {}: {}", folder, e.toString());
                }
            } else {
                logger.warn("Icon folder {} does not exist. Create it and place 64x64 PNG icons inside.", folder);
            }
        }

        List<Favicon> loaded = new ArrayList<>();
        for (Path path : paths) {
            try {
                loaded.add(Favicon.create(path));
            } catch (Exception e) {
                logger.warn("Skipping icon {}: {}", path, e.toString());
            }
        }

        return Collections.unmodifiableList(loaded);
    }

    private boolean isPng(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".png");
    }

    private Path resolvePath(String configured) {
        Path path = Paths.get(configured);
        if (!path.isAbsolute()) {
            if (serverRoot != null) {
                path = serverRoot.resolve(path);
            } else {
                path = dataDirectory.resolve(path);
            }
        }
        return path.normalize();
    }

    private Path resolveServerRoot(Path dataDirectory) {
        Path parent = dataDirectory.toAbsolutePath().getParent(); // plugins
        if (parent != null) {
            Path root = parent.getParent();
            if (root != null) {
                return root;
            }
            return parent;
        }
        return dataDirectory.toAbsolutePath();
    }

    private record IconConfig(String iconFolder, List<String> iconFiles) {}
}
