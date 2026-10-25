//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

final class RendererLauncher {
    private static final int MAX_DEPTH = 3;
    private static Process process;
    private static int restarts;
    private static Path lastExecutable;
    private static BridgeConfig lastConfig;

    private RendererLauncher() {
    }

    static void start(BridgeConfig config) {
        if (config.launch) {
            try {
                Path executable = find(config);
                if (executable == null) {
                    TmcgiUeBridgeClient.LOGGER.info("No packaged Unreal program next to the mod's jar: expecting one started by hand");
                    return;
                }

                launch(executable, config);
                Runtime.getRuntime().addShutdownHook(new Thread(RendererLauncher::stop, "TMCGI-Renderer-Stop"));
            } catch (IOException | RuntimeException var2) {
                Exception e = var2;
                TmcgiUeBridgeClient.LOGGER.warn("The TMCGI renderer could not be started", e);
            }

        }
    }

    static boolean restart() {
        Class var2 = RendererLauncher.class;
        Path executable;
        BridgeConfig config;
        synchronized(RendererLauncher.class) {
            executable = lastExecutable;
            config = lastConfig;
        }

        if (executable != null && config != null) {
            Thread thread = new Thread(() -> {
                stop();

                try {
                    launch(executable, config);
                } catch (IOException var3) {
                    IOException e = var3;
                    TmcgiUeBridgeClient.LOGGER.warn("The TMCGI renderer could not be started again", e);
                }

            }, "TMCGI-Renderer-Restart");
            thread.setDaemon(true);
            thread.start();
            return true;
        } else {
            return false;
        }
    }

    static synchronized void stop() {
        Process running = process;
        process = null;
        if (running != null && running.isAlive()) {
            running.destroy();

            try {
                if (!running.waitFor(4L, TimeUnit.SECONDS)) {
                    running.destroyForcibly();
                }
            } catch (InterruptedException var2) {
                running.destroyForcibly();
                Thread.currentThread().interrupt();
            }

            TmcgiUeBridgeClient.LOGGER.info("The TMCGI renderer was ended");
        }
    }

    private static synchronized void launch(Path executable, BridgeConfig config) throws IOException {
        List<String> command = new ArrayList();
        command.add(executable.toString());
        command.add("-TMCGIBridgeFrameShare");
        command.add("-TMCGIFrameFile=" + String.valueOf(Platform.frameFile()));
        command.add("-RenderOffScreen");
        command.add("-NoSound");
        command.add("-TMCGIParentPid=" + ProcessHandle.current().pid());
        if (!TmcgiSettings.hardwareRayTracing) {
            command.add("-noraytracing");
        }

        if (Platform.current() == Platform.MAC) {
            command.add("-ini:Engine:[SystemSettings]:Metal.Bindless.ResourceDescriptorHeapSize=16777216");
        }

        TmcgiSettings.launchedWithRayTracing = TmcgiSettings.hardwareRayTracing;
        lastExecutable = executable;
        lastConfig = config;
        String[] var3 = config.rendererArguments.split("\\s+");
        int var4 = var3.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String argument = var3[var5];
            if (!argument.isBlank()) {
                command.add(argument);
            }
        }

        Path logFile = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("tmcgi-renderer.log");
        Files.createDirectories(logFile.getParent());
        ProcessBuilder builder = (new ProcessBuilder(command)).directory(executable.getParent().toFile()).redirectErrorStream(true).redirectOutput(Redirect.to(logFile.toFile()));
        process = builder.start();
        Process started = process;
        TmcgiUeBridgeClient.LOGGER.info("Started the TMCGI renderer {} (process {}), its output is in {}", new Object[]{executable, started.pid(), logFile});
        started.onExit().thenAccept((finished) -> {
            Class var4 = RendererLauncher.class;
            synchronized(RendererLauncher.class) {
                if (process == started) {
                    process = null;
                    TmcgiUeBridgeClient.LOGGER.warn("The TMCGI renderer ended on its own (exit code {})", finished.exitValue());
                    if (finished.exitValue() != 0 && restarts < 2 && ProcessHandle.current().isAlive()) {
                        ++restarts;

                        try {
                            Thread.sleep(2000L);
                            launch(executable, config);
                        } catch (InterruptedException | IOException var7) {
                            Exception e = var7;
                            TmcgiUeBridgeClient.LOGGER.warn("The TMCGI renderer could not be started again", e);
                        }
                    }

                }
            }
        });
    }

    private static Path find(BridgeConfig config) throws IOException {
        Path configured;
        if (!config.rendererPath.isBlank()) {
            configured = Path.of(config.rendererPath.trim());
            if (!configured.isAbsolute()) {
                Path folder = jarFolder();
                if (folder == null) {
                    return null;
                }

                configured = folder.resolve(configured);
            }

            return executableOf(configured);
        } else {
            configured = jarFolder();
            if (configured == null) {
                return null;
            } else {
                Stream<Path> walk = Files.walk(configured, 3, new FileVisitOption[0]);

                Path var7;
                label62: {
                    try {
                        List<Path> candidates = walk.filter(RendererLauncher::isUnrealProgram).sorted(Comparator.comparing((path) -> {
                            return !path.getFileName().toString().toLowerCase(Locale.ROOT).contains("tmcgi");
                        }).thenComparing(Comparator.naturalOrder())).toList();
                        Iterator var4 = candidates.iterator();

                        while(var4.hasNext()) {
                            Path candidate = (Path)var4.next();
                            Path executable = executableOf(candidate);
                            if (executable != null) {
                                var7 = executable;
                                break label62;
                            }
                        }
                    } catch (Throwable var9) {
                        if (walk != null) {
                            try {
                                walk.close();
                            } catch (Throwable var8) {
                                var9.addSuppressed(var8);
                            }
                        }

                        throw var9;
                    }

                    if (walk != null) {
                        walk.close();
                    }

                    return null;
                }

                if (walk != null) {
                    walk.close();
                }

                return var7;
            }
        }
    }

    private static boolean isUnrealProgram(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.contains("crash") && !name.contains("cmd")) {
            boolean var10000;
            switch (Platform.current()) {
                case MAC -> var10000 = name.endsWith(".app") && Files.isDirectory(path.resolve("Contents").resolve("UE"), new LinkOption[0]);
                case WINDOWS -> var10000 = name.endsWith(".exe") && Files.isRegularFile(path, new LinkOption[0]) && Files.isDirectory(path.resolveSibling("Engine"), new LinkOption[0]);
                case LINUX -> var10000 = false;
                default -> throw new MatchException((String)null, (Throwable)null);
            }

            return var10000;
        } else {
            return false;
        }
    }

    private static Path executableOf(Path path) throws IOException {
        if (path != null && Files.exists(path, new LinkOption[0])) {
            String name = path.getFileName().toString();
            Path binaries;
            if (name.toLowerCase(Locale.ROOT).endsWith(".app") && Files.isDirectory(path, new LinkOption[0])) {
                Path macos = path.resolve("Contents").resolve("MacOS");
                if (!Files.isDirectory(macos, new LinkOption[0])) {
                    return null;
                } else {
                    Stream<Path> files = Files.list(macos);

                    try {
                        Optional<Path> program = files.filter((x$0) -> {
                            return Files.isRegularFile(x$0, new LinkOption[0]);
                        }).filter(Files::isExecutable).findFirst();
                        binaries = (Path)program.orElse((Object)null);
                    } catch (Throwable var12) {
                        if (files != null) {
                            try {
                                files.close();
                            } catch (Throwable var10) {
                                var12.addSuppressed(var10);
                            }
                        }

                        throw var12;
                    }

                    if (files != null) {
                        files.close();
                    }

                    return binaries;
                }
            } else {
                String lower = name.toLowerCase(Locale.ROOT);
                boolean windows = lower.endsWith(".exe");
                if ((windows || lower.endsWith(".sh")) && Files.isRegularFile(path, new LinkOption[0])) {
                    String project = name.substring(0, name.lastIndexOf(46));
                    binaries = path.resolveSibling(project).resolve("Binaries").resolve(windows ? "Win64" : "Linux");
                    Path real = binaries.resolve(windows ? project + ".exe" : project);
                    if (Files.isRegularFile(real, new LinkOption[0]) && Files.isExecutable(real)) {
                        return real;
                    }

                    if (Files.isDirectory(binaries, new LinkOption[0])) {
                        Stream<Path> files = Files.list(binaries);

                        Path var9;
                        label133: {
                            try {
                                Optional<Path> configured = files.filter((x$0) -> {
                                    return Files.isRegularFile(x$0, new LinkOption[0]);
                                }).filter(Files::isExecutable).filter((file) -> {
                                    return file.getFileName().toString().startsWith(project + "-" + (windows ? "Win64" : "Linux") + "-");
                                }).filter((file) -> {
                                    return !windows || file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe");
                                }).sorted().findFirst();
                                if (configured.isPresent()) {
                                    var9 = (Path)configured.get();
                                    break label133;
                                }
                            } catch (Throwable var13) {
                                if (files != null) {
                                    try {
                                        files.close();
                                    } catch (Throwable var11) {
                                        var13.addSuppressed(var11);
                                    }
                                }

                                throw var13;
                            }

                            if (files != null) {
                                files.close();
                            }

                            return Files.isRegularFile(path, new LinkOption[0]) && Files.isExecutable(path) ? path : null;
                        }

                        if (files != null) {
                            files.close();
                        }

                        return var9;
                    }
                }

                return Files.isRegularFile(path, new LinkOption[0]) && Files.isExecutable(path) ? path : null;
            }
        } else {
            return null;
        }
    }

    private static Path jarFolder() {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer("tmcgi_ue_bridge");
        if (container.isEmpty()) {
            return null;
        } else {
            Iterator var1 = ((ModContainer)container.get()).getOrigin().getPaths().iterator();

            Path origin;
            File file;
            do {
                if (!var1.hasNext()) {
                    return null;
                }

                origin = (Path)var1.next();
                file = origin.toFile();
            } while(!file.isFile());

            return origin.toAbsolutePath().getParent();
        }
    }
}
