package fr.iutgon.sae401.serverSide.server.runtime.config;

import fr.iutgon.sae401.serverSide.game.ServerGameConfig;

import java.util.Locale;

/**
 * @brief Minimal CLI argument parser for server runtime configuration.
 * <p>
 * Supported flags:
 * - --port <int>
 * - --udpPort <int>
 * - --maxClients <int>
 * - --tickHz <int>
 * - --netHz <int>
 */
public final class ArgsConfigLoader {
    private ArgsConfigLoader() {
    }

    public static ServerRuntimeConfig load(String[] args) {
        int port = 7777;
        int udpPort = 7777;
        boolean udpPortExplicit = false;
        int maxClients = 32;
        int tickHz = 60;
        int netHz = 20;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }
            String key = arg.substring(2).toLowerCase(Locale.ROOT);
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for --" + key);
            }
            String value = args[++i];

            switch (key) {
                case "port" -> port = parsePositiveInt("port", value);
                case "udpport" -> {
                    udpPort = parsePositiveInt("udpPort", value);
                    udpPortExplicit = true;
                }
                case "maxclients" -> maxClients = parsePositiveInt("maxClients", value);
                case "tickhz" -> tickHz = parsePositiveInt("tickHz", value);
                case "nethz" -> netHz = parsePositiveInt("netHz", value);
                default -> throw new IllegalArgumentException("Unknown flag: --" + key);
            }
        }

        if (!udpPortExplicit) {
            udpPort = port;
        }

        return new ServerRuntimeConfig(new fr.iutgon.sae401.serverSide.server.tcp.ServerConfig(port, udpPort, maxClients), new ServerGameConfig(tickHz, netHz));
    }

    private static int parsePositiveInt(String name, String raw) {
        try {
            int value = Integer.parseInt(raw);
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be > 0");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid int for " + name + ": " + raw);
        }
    }
}
