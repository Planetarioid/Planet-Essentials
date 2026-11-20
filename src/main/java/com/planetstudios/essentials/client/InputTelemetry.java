package com.planetstudios.essentials.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Iterator;

/** Lightweight local input telemetry for the optional Keystrokes and CPS HUD widgets. */
public final class InputTelemetry {
    private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<>();
    private static final long WINDOW_MILLIS = 1_000L;

    private InputTelemetry() {}

    public static synchronized void recordMousePress(int button) {
        long now = System.currentTimeMillis();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) LEFT_CLICKS.addLast(now);
        else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) RIGHT_CLICKS.addLast(now);
        trim(LEFT_CLICKS, now);
        trim(RIGHT_CLICKS, now);
    }

    public static synchronized int leftCps() {
        trim(LEFT_CLICKS, System.currentTimeMillis());
        return LEFT_CLICKS.size();
    }

    public static synchronized int rightCps() {
        trim(RIGHT_CLICKS, System.currentTimeMillis());
        return RIGHT_CLICKS.size();
    }

    public static String keystrokes(Minecraft client) {
        long window = client.getWindow().getWindow();
        return key(window, GLFW.GLFW_KEY_W, "W") + " "
                + key(window, GLFW.GLFW_KEY_A, "A") + " "
                + key(window, GLFW.GLFW_KEY_S, "S") + " "
                + key(window, GLFW.GLFW_KEY_D, "D") + "   "
                + key(window, GLFW.GLFW_KEY_SPACE, "SPACE") + " "
                + key(window, GLFW.GLFW_KEY_LEFT_SHIFT, "SHIFT");
    }

    private static String key(long window, int key, String label) {
        boolean down = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
        return down ? "[" + label + "]" : label.toLowerCase(java.util.Locale.ROOT);
    }

    private static void trim(ArrayDeque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() >= WINDOW_MILLIS) clicks.removeFirst();
    }
}
