package fr.iutgon.sae401.launcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LauncherMainTest {
    private String previousServerHost;
    private String previousServerPort;

    @BeforeEach
    void setUp() {
        previousServerHost = System.getProperty("sae.server.host");
        previousServerPort = System.getProperty("sae.server.port");
        LauncherMain.resetEntrypointsForTest();
    }

    @AfterEach
    void tearDown() {
        LauncherMain.resetEntrypointsForTest();
        restoreSystemProperty("sae.server.host", previousServerHost);
        restoreSystemProperty("sae.server.port", previousServerPort);
    }

    @Test
    void mainPrintsUsageWhenArgsAreNull() {
        String out = captureStdout(() -> LauncherMain.main(null));
        assertTrue(out.contains("Usage:"));
    }

    @Test
    void mainPrintsUsageWhenArgsAreEmpty() {
        String out = captureStdout(() -> LauncherMain.main(new String[0]));
        assertTrue(out.contains("launcher local [--port N]"));
    }

    @Test
    void mainPrintsUsageWhenModeIsUnknown() {
        String out = captureStdout(() -> LauncherMain.main(new String[]{"unknown"}));
        assertTrue(out.contains("launcher server"));
    }

    @Test
    void mainDelegatesServerModeToServerEntrypoint() {
        AtomicReference<String[]> serverArgs = new AtomicReference<>();
        LauncherMain.setEntrypointsForTest(serverArgs::set, args -> {
        });

        LauncherMain.main(new String[]{"server", "--port", "9000"});

        assertArrayEquals(new String[]{"--port", "9000"}, serverArgs.get());
    }

    @Test
    void mainDelegatesClientModeToClientEntrypoint() {
        AtomicReference<String[]> clientArgs = new AtomicReference<>();
        LauncherMain.setEntrypointsForTest(args -> {
        }, clientArgs::set);

        LauncherMain.main(new String[]{"client", "--foo", "bar"});

        assertArrayEquals(new String[]{"--foo", "bar"}, clientArgs.get());
    }

    @Test
    void mainDelegatesLocalModeAndSetsServerProperties() throws Exception {
        AtomicReference<String[]> serverArgs = new AtomicReference<>();
        AtomicReference<String[]> clientArgs = new AtomicReference<>();
        CountDownLatch serverCalled = new CountDownLatch(1);

        LauncherMain.setEntrypointsForTest(args -> {
            serverArgs.set(args);
            serverCalled.countDown();
        }, clientArgs::set);

        LauncherMain.main(new String[]{"local", "--port", "8123"});

        assertTrue(serverCalled.await(2, TimeUnit.SECONDS));
        assertArrayEquals(new String[]{"--port", "8123", "--udpPort", "8123"}, serverArgs.get());
        assertArrayEquals(new String[0], clientArgs.get());
        assertEquals("localhost", System.getProperty("sae.server.host"));
        assertEquals("8123", System.getProperty("sae.server.port"));
    }

    @Test
    void parsePortReturnsDefaultWhenNoPortOption() throws Exception {
        assertEquals(7777, invokeParsePort(new String[]{"--udpPort", "9001"}));
    }

    @Test
    void parsePortAcceptsCaseInsensitiveValidPort() throws Exception {
        assertEquals(5050, invokeParsePort(new String[]{"--PORT", "5050"}));
    }

    @Test
    void parsePortReturnsDefaultWhenValueIsNotNumeric() throws Exception {
        assertEquals(7777, invokeParsePort(new String[]{"--port", "abc"}));
    }

    @Test
    void parsePortReturnsDefaultWhenValueIsOutOfRange() throws Exception {
        assertEquals(7777, invokeParsePort(new String[]{"--port", "0"}));
        assertEquals(7777, invokeParsePort(new String[]{"--port", "70000"}));
    }

    @Test
    void parsePortReturnsDefaultWhenPortFlagHasNoValue() throws Exception {
        assertEquals(7777, invokeParsePort(new String[]{"--port"}));
    }

    private static int invokeParsePort(String[] args) throws Exception {
        Method method = LauncherMain.class.getDeclaredMethod("parsePort", String[].class);
        method.setAccessible(true);
        return (int) method.invoke(null, (Object) args);
    }

    private static String captureStdout(Runnable runnable) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream replacement = new PrintStream(output)) {
            System.setOut(replacement);
            runnable.run();
            replacement.flush();
        } finally {
            System.setOut(original);
        }
        return output.toString();
    }

    private static void restoreSystemProperty(String name, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previousValue);
        }
    }
}
