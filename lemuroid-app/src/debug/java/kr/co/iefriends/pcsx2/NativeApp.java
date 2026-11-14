package kr.co.iefriends.pcsx2;

import android.os.Build;
import android.system.Os;
import android.system.OsConstants;
import android.view.Surface;
import java.io.File;
import java.io.IOException;

/**
 * Debug-only CARTDECK JNI adapter for ARMSX2's pinned Android native exports.
 * Not an external ARMSX2 app, Play! replacement, or a proof of working gameplay.
 * Intentionally fails closed on unsupported document-provider callbacks.
 */
public final class NativeApp {
    private static volatile boolean loaded;
    private static volatile String loadStatus = "NOT_REQUESTED";
    private static volatile File allowedRoot;

    private NativeApp() {}

    public static synchronized void allowPrivateRoot(File root) throws IOException {
        allowedRoot = root.getCanonicalFile();
    }

    public static synchronized boolean tryLoad() {
        if (loaded) return true;
        if (Build.VERSION.SDK_INT < 29 || !Build.SUPPORTED_ABIS[0].equals("arm64-v8a")) {
            loadStatus = "UNSUPPORTED_ANDROID_ABI";
            return false;
        }
        final String name = pageSize() >= 16384 ? "emucore_16k" : "emucore_4k";
        try {
            System.loadLibrary(name);
            loaded = true;
            loadStatus = "NATIVE_LIBRARY_LOADED_ONLY";
        } catch (UnsatisfiedLinkError error) {
            loadStatus = "NATIVE_LIBRARY_MISSING_OR_DEPENDENCY_UNAVAILABLE";
        }
        return loaded;
    }

    private static long pageSize() {
        try {
            long size = Os.sysconf(OsConstants._SC_PAGESIZE);
            return size > 0 ? size : 4096;
        } catch (Throwable ignored) {
            return 4096;
        }
    }

    public static String loadStatus() {
        return loadStatus;
    }

    public static native void initialize(String dataRoot, String biosRoot, int androidApi);
    public static native void onNativeSurfaceCreated();
    public static native void onNativeSurfaceChanged(Surface surface, int width, int height);
    public static native void onNativeSurfaceDestroyed();
    public static native void setDisplayRefreshRate(float hertz);
    public static native boolean runVMThread(String discPath);
    public static native void pause();
    public static native void resume();
    public static native void shutdown();
    public static native void setPadButton(int index, int range, boolean pressed);

    // JNI callbacks cached by pinned native-lib.cpp during initialise.
    public static void vmSetPaused(boolean paused) {
        // The debug host has no UI-controlled pause overlay.
    }

    public static void onPadRumble(int port, int largeMotor, int smallMotor) {
        // Deliberately no rumble until controller ownership and device tests pass.
    }

    public static void playSound(String path) {
        // Optional achievement jingles are disabled; emulation audio is native.
    }

    public static void onAutoTuneCommitted(
            String serial, String section, String key, String value) {
        // ARMSX2 writes native config under isolated game data root.
    }

    public static void onAutoTuneDisplayScale(int scale) {
        // Debug host renders the native surface without external rescaling.
    }

    public static String[] findSiblingChds(String path) {
        // Only a single, app-private lawful test image is supported in this probe.
        return new String[0];
    }

    public static int openContentUri(String uri) {
        // Never grant arbitrary document provider access to an unverified engine.
        return -1;
    }

    private static boolean insideSandbox(String path) {
        try {
            final File root = allowedRoot;
            if (root == null || path == null) return false;
            final String canonical = new File(path).getCanonicalPath();
            return canonical.startsWith(root.getCanonicalPath() + File.separator);
        } catch (IOException error) {
            return false;
        }
    }

    public static boolean createDirectoryPath(String path) {
        return insideSandbox(path) && (new File(path).isDirectory() || new File(path).mkdirs());
    }

    public static boolean createFilePath(String path) {
        if (!insideSandbox(path)) return false;
        try {
            File target = new File(path);
            return target.isFile() || target.createNewFile();
        } catch (IOException error) {
            return false;
        }
    }
}
