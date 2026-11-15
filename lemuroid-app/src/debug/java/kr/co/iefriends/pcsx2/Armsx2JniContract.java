package kr.co.iefriends.pcsx2;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** SDL native registration/callback contract from ARMSX2 7a999782377e61fb96eca65e476b5ec0d182058e.
 * Checked before System.load: RegisterNatives otherwise leaves a pending exception
 * and the next JNI call aborts the process instead of returning a Java load error.
 */
public final class Armsx2JniContract {
    private Armsx2JniContract() {}
    private static final String[] PINNED = {
        "J|I|org.libsdl.app.HIDDeviceManager|closeDevice|(I)V",
        "J|I|org.libsdl.app.HIDDeviceManager|initialize|(ZZ)Z",
        "J|I|org.libsdl.app.HIDDeviceManager|openDevice|(I)Z",
        "J|I|org.libsdl.app.HIDDeviceManager|readReport|(I[BZ)Z",
        "J|I|org.libsdl.app.HIDDeviceManager|writeReport|(I[BZ)I",
        "J|S|org.libsdl.app.SDLActivity|clipboardGetText|()Ljava/lang/String;",
        "J|S|org.libsdl.app.SDLActivity|clipboardHasText|()Z",
        "J|S|org.libsdl.app.SDLActivity|clipboardSetText|(Ljava/lang/String;)V",
        "J|S|org.libsdl.app.SDLActivity|createCustomCursor|([IIIII)I",
        "J|S|org.libsdl.app.SDLActivity|destroyCustomCursor|(I)V",
        "J|S|org.libsdl.app.SDLActivity|getContext|()Landroid/app/Activity;",
        "J|S|org.libsdl.app.SDLActivity|getManifestEnvironmentVariables|()Z",
        "J|S|org.libsdl.app.SDLActivity|getNativeSurface|()Landroid/view/Surface;",
        "J|S|org.libsdl.app.SDLActivity|getPreferredLocales|()Ljava/lang/String;",
        "J|S|org.libsdl.app.SDLActivity|initTouch|()V",
        "J|S|org.libsdl.app.SDLActivity|isAndroidTV|()Z",
        "J|S|org.libsdl.app.SDLActivity|isChromebook|()Z",
        "J|S|org.libsdl.app.SDLActivity|isDeXMode|()Z",
        "J|S|org.libsdl.app.SDLActivity|isTablet|()Z",
        "J|S|org.libsdl.app.SDLActivity|manualBackButton|()V",
        "J|S|org.libsdl.app.SDLActivity|minimizeWindow|()V",
        "J|S|org.libsdl.app.SDLActivity|openFileDescriptor|(Ljava/lang/String;Ljava/lang/String;)I",
        "J|S|org.libsdl.app.SDLActivity|openURL|(Ljava/lang/String;)Z",
        "J|S|org.libsdl.app.SDLActivity|requestPermission|(Ljava/lang/String;I)V",
        "J|S|org.libsdl.app.SDLActivity|sendMessage|(II)Z",
        "J|S|org.libsdl.app.SDLActivity|setActivityTitle|(Ljava/lang/String;)Z",
        "J|S|org.libsdl.app.SDLActivity|setCustomCursor|(I)Z",
        "J|S|org.libsdl.app.SDLActivity|setOrientation|(IIZLjava/lang/String;)V",
        "J|S|org.libsdl.app.SDLActivity|setRelativeMouseEnabled|(Z)Z",
        "J|S|org.libsdl.app.SDLActivity|setSystemCursor|(I)Z",
        "J|S|org.libsdl.app.SDLActivity|setWindowStyle|(Z)V",
        "J|S|org.libsdl.app.SDLActivity|shouldMinimizeOnFocusLoss|()Z",
        "J|S|org.libsdl.app.SDLActivity|showFileDialog|([Ljava/lang/String;ZZI)Z",
        "J|S|org.libsdl.app.SDLActivity|showTextInput|(IIIII)Z",
        "J|S|org.libsdl.app.SDLActivity|showToast|(Ljava/lang/String;IIII)Z",
        "J|S|org.libsdl.app.SDLActivity|supportsRelativeMouse|()Z",
        "J|S|org.libsdl.app.SDLAudioManager|audioSetThreadPriority|(ZI)V",
        "J|S|org.libsdl.app.SDLAudioManager|registerAudioDeviceCallback|()V",
        "J|S|org.libsdl.app.SDLAudioManager|unregisterAudioDeviceCallback|()V",
        "J|S|org.libsdl.app.SDLControllerManager|hapticRumble|(IFFI)V",
        "J|S|org.libsdl.app.SDLControllerManager|hapticRun|(IFI)V",
        "J|S|org.libsdl.app.SDLControllerManager|hapticStop|(I)V",
        "J|S|org.libsdl.app.SDLControllerManager|joystickSetLED|(IIII)V",
        "J|S|org.libsdl.app.SDLControllerManager|pollHapticDevices|()V",
        "J|S|org.libsdl.app.SDLControllerManager|pollInputDevices|()V",
        "N|I|org.libsdl.app.HIDDeviceManager|HIDDeviceConnected|(ILjava/lang/String;IILjava/lang/String;ILjava/lang/String;Ljava/lang/String;IIIIZ)V",
        "N|I|org.libsdl.app.HIDDeviceManager|HIDDeviceDisconnected|(I)V",
        "N|I|org.libsdl.app.HIDDeviceManager|HIDDeviceInputReport|(I[B)V",
        "N|I|org.libsdl.app.HIDDeviceManager|HIDDeviceOpenPending|(I)V",
        "N|I|org.libsdl.app.HIDDeviceManager|HIDDeviceOpenResult|(IZ)V",
        "N|I|org.libsdl.app.HIDDeviceManager|HIDDeviceRegisterCallback|()V",
        "N|I|org.libsdl.app.HIDDeviceManager|HIDDeviceReleaseCallback|()V",
        "N|I|org.libsdl.app.HIDDeviceManager|HIDDeviceReportResponse|(I[B)V",
        "N|S|org.libsdl.app.SDLActivity|nativeAddTouch|(ILjava/lang/String;)V",
        "N|S|org.libsdl.app.SDLActivity|nativeAllowRecreateActivity|()Z",
        "N|S|org.libsdl.app.SDLActivity|nativeCheckSDLThreadCounter|()I",
        "N|S|org.libsdl.app.SDLActivity|nativeCleanupMainThread|()V",
        "N|S|org.libsdl.app.SDLActivity|nativeFocusChanged|(Z)V",
        "N|S|org.libsdl.app.SDLActivity|nativeGetHintBoolean|(Ljava/lang/String;Z)Z",
        "N|S|org.libsdl.app.SDLActivity|nativeGetHint|(Ljava/lang/String;)Ljava/lang/String;",
        "N|S|org.libsdl.app.SDLActivity|nativeGetVersion|()Ljava/lang/String;",
        "N|S|org.libsdl.app.SDLActivity|nativeInitMainThread|()V",
        "N|S|org.libsdl.app.SDLActivity|nativeLowMemory|()V",
        "N|S|org.libsdl.app.SDLActivity|nativePause|()V",
        "N|S|org.libsdl.app.SDLActivity|nativePermissionResult|(IZ)V",
        "N|S|org.libsdl.app.SDLActivity|nativeQuit|()V",
        "N|S|org.libsdl.app.SDLActivity|nativeResume|()V",
        "N|S|org.libsdl.app.SDLActivity|nativeRunMain|(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)I",
        "N|S|org.libsdl.app.SDLActivity|nativeSendQuit|()V",
        "N|S|org.libsdl.app.SDLActivity|nativeSetNaturalOrientation|(I)V",
        "N|S|org.libsdl.app.SDLActivity|nativeSetScreenResolution|(IIIIFF)V",
        "N|S|org.libsdl.app.SDLActivity|nativeSetenv|(Ljava/lang/String;Ljava/lang/String;)V",
        "N|S|org.libsdl.app.SDLActivity|nativeSetupJNI|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativeAccel|(FFF)V",
        "N|S|org.libsdl.app.SDLActivity|onNativeClipboardChanged|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativeDarkModeChanged|(Z)V",
        "N|S|org.libsdl.app.SDLActivity|onNativeDropFile|(Ljava/lang/String;)V",
        "N|S|org.libsdl.app.SDLActivity|onNativeFileDialog|(I[Ljava/lang/String;I)V",
        "N|S|org.libsdl.app.SDLActivity|onNativeInsetsChanged|(IIII)V",
        "N|S|org.libsdl.app.SDLActivity|onNativeKeyDown|(I)V",
        "N|S|org.libsdl.app.SDLActivity|onNativeKeyUp|(I)V",
        "N|S|org.libsdl.app.SDLActivity|onNativeKeyboardFocusLost|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativeLocaleChanged|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativeMouse|(IIFFZ)V",
        "N|S|org.libsdl.app.SDLActivity|onNativePen|(IIIIFFF)V",
        "N|S|org.libsdl.app.SDLActivity|onNativePinchEnd|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativePinchStart|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativePinchUpdate|(F)V",
        "N|S|org.libsdl.app.SDLActivity|onNativeResize|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativeRotationChanged|(I)V",
        "N|S|org.libsdl.app.SDLActivity|onNativeScreenKeyboardHidden|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativeScreenKeyboardShown|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativeSoftReturnKey|()Z",
        "N|S|org.libsdl.app.SDLActivity|onNativeSurfaceChanged|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativeSurfaceCreated|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativeSurfaceDestroyed|()V",
        "N|S|org.libsdl.app.SDLActivity|onNativeTouch|(IIIFFF)V",
        "N|S|org.libsdl.app.SDLAudioManager|nativeAddAudioDevice|(ZLjava/lang/String;I)V",
        "N|S|org.libsdl.app.SDLAudioManager|nativeRemoveAudioDevice|(ZI)V",
        "N|S|org.libsdl.app.SDLAudioManager|nativeSetupJNI|()V",
        "N|S|org.libsdl.app.SDLControllerManager|nativeAddHaptic|(ILjava/lang/String;)V",
        "N|S|org.libsdl.app.SDLControllerManager|nativeAddJoystick|(ILjava/lang/String;Ljava/lang/String;IIIIIIZZ)V",
        "N|S|org.libsdl.app.SDLControllerManager|nativeRemoveHaptic|(I)V",
        "N|S|org.libsdl.app.SDLControllerManager|nativeRemoveJoystick|(I)V",
        "N|S|org.libsdl.app.SDLControllerManager|nativeSetupJNI|()V",
        "N|S|org.libsdl.app.SDLControllerManager|onNativeHat|(IIII)V",
        "N|S|org.libsdl.app.SDLControllerManager|onNativeJoy|(IIF)V",
        "N|S|org.libsdl.app.SDLControllerManager|onNativePadDown|(II)Z",
        "N|S|org.libsdl.app.SDLControllerManager|onNativePadUp|(II)Z",
        "N|S|org.libsdl.app.SDLInputConnection|nativeCommitText|(Ljava/lang/String;I)V",
        "N|S|org.libsdl.app.SDLInputConnection|nativeGenerateScancodeForUnichar|(C)V",
    };

    public static String[] pinnedEntries() { return PINNED.clone(); }
    public static void verify() { verify(PINNED); }

    public static void verify(String[] entries) {
        Map<String, Map<String, Method>> classes = new HashMap<>();
        ArrayList<String> missing = new ArrayList<>();
        for (String entry : entries) {
            String[] fields = entry.split("\\|", -1);
            if (fields.length != 5) throw new IllegalArgumentException("Invalid JNI contract entry");
            try {
                Map<String, Method> methods = classes.get(fields[2]);
                if (methods == null) {
                    methods = new HashMap<>();
                    Class<?> type = Class.forName(fields[2], false, Armsx2JniContract.class.getClassLoader());
                    for (Method method : type.getDeclaredMethods()) methods.put(method.getName() + descriptor(method), method);
                    classes.put(fields[2], methods);
                }
                Method method = methods.get(fields[3] + fields[4]);
                if (method == null || Modifier.isStatic(method.getModifiers()) != fields[1].equals("S") ||
                        (fields[0].equals("N") && !Modifier.isNative(method.getModifiers()))) missing.add(entry);
            } catch (ReflectiveOperationException | LinkageError error) {
                missing.add(entry + " [" + error.getClass().getSimpleName() + "]");
            }
        }
        if (!missing.isEmpty()) throw new IllegalStateException("ARMSX2 SDL/JNI mismatch: " + String.join(", ", missing));
    }

    private static String descriptor(Method method) {
        StringBuilder value = new StringBuilder("(");
        for (Class<?> type : method.getParameterTypes()) value.append(descriptor(type));
        return value.append(')').append(descriptor(method.getReturnType())).toString();
    }
    private static String descriptor(Class<?> type) {
        if (type.isArray()) return type.getName().replace('.', '/');
        if (!type.isPrimitive()) return "L" + type.getName().replace('.', '/') + ";";
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        throw new IllegalArgumentException(type.getName());
    }
}
