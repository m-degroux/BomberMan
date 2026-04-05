package fr.iutgon.sae401.launcher;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.serverSide.app.GameServerMain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launcher entry point used to orchestrate client-only, server-only or local (embedded server + client) starts.
 */
public final class LauncherMain {
    @FunctionalInterface
    interface Entrypoint {
        void run(String[] args);
    }

    private static Entrypoint serverEntrypoint = GameServerMain::main;
    private static Entrypoint clientEntrypoint = App::main;

    private LauncherMain() {
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();
        String[] tail = Arrays.copyOfRange(args, 1, args.length);

        switch (mode) {
            case "server" -> serverEntrypoint.run(tail);
            case "client" -> clientEntrypoint.run(tail);
            case "local" -> runLocal(tail);
            default -> printUsage();
        }
    }

    private static void runLocal(String[] args) {
        int port = parsePort(args);
        String portValue = Integer.toString(port);
        List<String> serverArgs = new ArrayList<>();
        serverArgs.add("--port");
        serverArgs.add(portValue);
        serverArgs.add("--udpPort");
        serverArgs.add(portValue);

        Thread serverThread = new Thread(() -> serverEntrypoint.run(serverArgs.toArray(String[]::new)), "embedded-local-server");
        serverThread.setDaemon(true);
        serverThread.start();

        System.setProperty("sae.server.host", "localhost");
        System.setProperty("sae.server.port", portValue);
        clientEntrypoint.run(new String[0]);
    }

    static void setEntrypointsForTest(Entrypoint server, Entrypoint client) {
        serverEntrypoint = server;
        clientEntrypoint = client;
    }

    static void resetEntrypointsForTest() {
        serverEntrypoint = GameServerMain::main;
        clientEntrypoint = App::main;
    }

    private static int parsePort(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equalsIgnoreCase(args[i])) {
                try {
                    int parsed = Integer.parseInt(args[i + 1]);
                    if (parsed > 0 && parsed <= 65535) {
                        return parsed;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 7777;
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  launcher server [--port N --udpPort N ...]");
        System.out.println("  launcher client");
        System.out.println("  launcher local [--port N]");
    }
}
