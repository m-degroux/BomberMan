package fr.iutgon.sae401.clientSide.utils;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import fr.iutgon.sae401.TestReflectionUtils;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GlobalKeyCaptureTest {

    @Test
    void getInstance_returnsSingleton() {
        GlobalKeyCapture a = GlobalKeyCapture.getInstance();
        GlobalKeyCapture b = GlobalKeyCapture.getInstance();
        assertSame(a, b);
    }

    @Test
    void startStop_shortCircuitWhenAlreadyInTargetState() throws Exception {
        GlobalKeyCapture capture = GlobalKeyCapture.getInstance();

        TestReflectionUtils.setField(capture, "isRegistered", false);
        capture.stop();

        TestReflectionUtils.setField(capture, "isRegistered", true);
        capture.start();
        assertEquals(true, TestReflectionUtils.getField(capture, "isRegistered"));
    }

    @Test
    void nativeKeyPressed_convertsManyNativeKeys_andSupportsAddRemoveListener() throws Exception {
        GlobalKeyCapture capture = GlobalKeyCapture.getInstance();

        @SuppressWarnings("unchecked")
        Map<Integer, java.util.function.Consumer<KeyCode>> listeners =
                (Map<Integer, java.util.function.Consumer<KeyCode>>) TestReflectionUtils.getField(capture, "keyListeners");
        listeners.clear();

        List<KeyCode> received = new ArrayList<>();
        java.util.function.Consumer<KeyCode> listener = received::add;
        capture.addKeyListener(listener);

        Map<Integer, KeyCode> expected = Map.ofEntries(
                Map.entry(NativeKeyEvent.VC_ESCAPE, KeyCode.ESCAPE),
                Map.entry(NativeKeyEvent.VC_SPACE, KeyCode.SPACE),
                Map.entry(NativeKeyEvent.VC_ENTER, KeyCode.ENTER),
                Map.entry(NativeKeyEvent.VC_UP, KeyCode.UP),
                Map.entry(NativeKeyEvent.VC_DOWN, KeyCode.DOWN),
                Map.entry(NativeKeyEvent.VC_LEFT, KeyCode.LEFT),
                Map.entry(NativeKeyEvent.VC_RIGHT, KeyCode.RIGHT),
                Map.entry(NativeKeyEvent.VC_A, KeyCode.A),
                Map.entry(NativeKeyEvent.VC_Z, KeyCode.Z),
                Map.entry(NativeKeyEvent.VC_0, KeyCode.DIGIT0),
                Map.entry(NativeKeyEvent.VC_9, KeyCode.DIGIT9),
                Map.entry(NativeKeyEvent.VC_SHIFT, KeyCode.SHIFT),
                Map.entry(NativeKeyEvent.VC_CONTROL, KeyCode.CONTROL),
                Map.entry(NativeKeyEvent.VC_ALT, KeyCode.ALT),
                Map.entry(NativeKeyEvent.VC_TAB, KeyCode.TAB),
                Map.entry(NativeKeyEvent.VC_BACKSPACE, KeyCode.BACK_SPACE)
        );

        for (Map.Entry<Integer, KeyCode> e : expected.entrySet()) {
            capture.nativeKeyPressed(event(e.getKey()));
        }
        assertEquals(expected.size(), received.size());
        for (KeyCode code : expected.values()) {
            org.junit.jupiter.api.Assertions.assertTrue(received.contains(code));
        }

        int beforeUnknown = received.size();
        capture.nativeKeyPressed(event(NativeKeyEvent.VC_UNDEFINED));
        assertEquals(beforeUnknown, received.size());

        capture.removeKeyListener(listener);
        capture.nativeKeyPressed(event(NativeKeyEvent.VC_A));
        assertEquals(beforeUnknown, received.size());
    }

    private static NativeKeyEvent event(int keyCode) {
        return new NativeKeyEvent(
                NativeKeyEvent.NATIVE_KEY_PRESSED,
                0,
                0,
                keyCode,
                NativeKeyEvent.CHAR_UNDEFINED
        );
    }
}
