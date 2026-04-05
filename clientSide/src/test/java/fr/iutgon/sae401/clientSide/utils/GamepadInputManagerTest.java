package fr.iutgon.sae401.clientSide.utils;

import fr.iutgon.sae401.TestReflectionUtils;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.EventQueue;
import net.java.games.input.Rumbler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GamepadInputManagerTest {

    @Test
    void setSelectedController_withNullOrBlankOrNoneDisablesController() throws Exception {
        GamepadInputManager manager = new GamepadInputManager();

        manager.setSelectedController(null);
        assertEquals(GamepadInputManager.NO_GAMEPAD, manager.getSelectedControllerName());
        assertNull(TestReflectionUtils.getField(manager, "controller"));

        manager.setSelectedController(" ");
        assertEquals(GamepadInputManager.NO_GAMEPAD, manager.getSelectedControllerName());
        assertNull(TestReflectionUtils.getField(manager, "controller"));

        manager.setSelectedController(GamepadInputManager.NO_GAMEPAD);
        assertEquals(GamepadInputManager.NO_GAMEPAD, manager.getSelectedControllerName());
        assertNull(TestReflectionUtils.getField(manager, "controller"));
    }

    @Test
    void poll_returnsNoneWhenControllerMissingOrNotPolling() throws Exception {
        GamepadInputManager manager = new GamepadInputManager();
        assertEquals(GamepadInputManager.Action.NONE, manager.poll(1_000_000_000L));

        FakeController fake = new FakeController();
        fake.pollResult = false;
        TestReflectionUtils.setField(manager, "controller", fake);
        assertEquals(GamepadInputManager.Action.NONE, manager.poll(1_000_000_000L));
    }

    @Test
    void poll_detectsBombPressOnlyOnRisingEdge() throws Exception {
        GamepadInputManager manager = new GamepadInputManager();
        FakeController fake = new FakeController();
        fake.button0.value = 1.0f;
        TestReflectionUtils.setField(manager, "controller", fake);

        assertEquals(GamepadInputManager.Action.DROP_BOMB, manager.poll(1_000_000_000L));
        assertEquals(GamepadInputManager.Action.NONE, manager.poll(2_000_000_000L));

        fake.button0.value = 0.0f;
        assertEquals(GamepadInputManager.Action.NONE, manager.poll(3_000_000_000L));

        fake.button0.value = 1.0f;
        assertEquals(GamepadInputManager.Action.DROP_BOMB, manager.poll(4_000_000_000L));
    }

    @Test
    void poll_honorsMovementCooldown() throws Exception {
        GamepadInputManager manager = new GamepadInputManager();
        FakeController fake = new FakeController();
        fake.pov.value = Component.POV.UP;
        TestReflectionUtils.setField(manager, "controller", fake);
        TestReflectionUtils.setField(manager, "bombPressed", true);

        assertEquals(GamepadInputManager.Action.MOVE_UP, manager.poll(1_000_000_000L));
        assertEquals(GamepadInputManager.Action.NONE, manager.poll(1_050_000_000L));
    }

    @Test
    void poll_prefersPovDirectionsOverAxes() throws Exception {
        GamepadInputManager manager = new GamepadInputManager();
        FakeController fake = new FakeController();
        TestReflectionUtils.setField(manager, "controller", fake);

        fake.pov.value = Component.POV.UP_RIGHT;
        assertEquals(GamepadInputManager.Action.MOVE_UP, manager.poll(1_000_000_000L));

        fake.pov.value = Component.POV.DOWN_LEFT;
        assertEquals(GamepadInputManager.Action.MOVE_DOWN, manager.poll(2_000_000_000L));

        fake.pov.value = Component.POV.LEFT;
        assertEquals(GamepadInputManager.Action.MOVE_LEFT, manager.poll(3_000_000_000L));

        fake.pov.value = Component.POV.RIGHT;
        assertEquals(GamepadInputManager.Action.MOVE_RIGHT, manager.poll(4_000_000_000L));
    }

    @Test
    void poll_usesAxesWhenPovIsOff_andAppliesDeadzoneAndDominantAxis() throws Exception {
        GamepadInputManager manager = new GamepadInputManager();
        FakeController fake = new FakeController();
        TestReflectionUtils.setField(manager, "controller", fake);

        fake.pov.value = Component.POV.OFF;
        fake.axisX.value = 0.1f;
        fake.axisY.value = 0.1f;
        assertEquals(GamepadInputManager.Action.NONE, manager.poll(1_000_000_000L));

        fake.axisX.value = 0.2f;
        fake.axisY.value = -0.8f;
        assertEquals(GamepadInputManager.Action.MOVE_UP, manager.poll(2_000_000_000L));

        fake.axisX.value = 0.1f;
        fake.axisY.value = 0.9f;
        assertEquals(GamepadInputManager.Action.MOVE_DOWN, manager.poll(3_000_000_000L));

        fake.axisX.value = -0.9f;
        fake.axisY.value = 0.1f;
        assertEquals(GamepadInputManager.Action.MOVE_LEFT, manager.poll(4_000_000_000L));

        fake.axisX.value = 0.95f;
        fake.axisY.value = 0.2f;
        assertEquals(GamepadInputManager.Action.MOVE_RIGHT, manager.poll(5_000_000_000L));
    }

    private static final class FakeController implements Controller {
        private final FakeComponent axisX = new FakeComponent(Component.Identifier.Axis.X, "x", 0.0f);
        private final FakeComponent axisY = new FakeComponent(Component.Identifier.Axis.Y, "y", 0.0f);
        private final FakeComponent pov = new FakeComponent(Component.Identifier.Axis.POV, "pov", Component.POV.OFF);
        private final FakeComponent button0 = new FakeComponent(new Component.Identifier.Button("0"), "0", 0.0f);
        private boolean pollResult = true;

        @Override
        public Controller[] getControllers() {
            return new Controller[0];
        }

        @Override
        public Type getType() {
            return Type.GAMEPAD;
        }

        @Override
        public Component[] getComponents() {
            return new Component[]{axisX, axisY, pov, button0};
        }

        @Override
        public Component getComponent(Component.Identifier identifier) {
            for (Component c : getComponents()) {
                if (c.getIdentifier().equals(identifier)) {
                    return c;
                }
            }
            return null;
        }

        @Override
        public Rumbler[] getRumblers() {
            return new Rumbler[0];
        }

        @Override
        public boolean poll() {
            return pollResult;
        }

        @Override
        public void setEventQueueSize(int size) {
        }

        @Override
        public EventQueue getEventQueue() {
            return null;
        }

        @Override
        public PortType getPortType() {
            return PortType.UNKNOWN;
        }

        @Override
        public int getPortNumber() {
            return 0;
        }

        @Override
        public String getName() {
            return "fake-pad";
        }
    }

    private static final class FakeComponent implements Component {
        private final Identifier identifier;
        private final String name;
        private float value;

        private FakeComponent(Identifier identifier, String name, float value) {
            this.identifier = identifier;
            this.name = name;
            this.value = value;
        }

        @Override
        public Identifier getIdentifier() {
            return identifier;
        }

        @Override
        public boolean isRelative() {
            return false;
        }

        @Override
        public boolean isAnalog() {
            return true;
        }

        @Override
        public float getDeadZone() {
            return 0.0f;
        }

        @Override
        public float getPollData() {
            return value;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
