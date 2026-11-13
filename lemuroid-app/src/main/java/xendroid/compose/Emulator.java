// SPDX-License-Identifier: WTFPL
// Minimal upstream JNI ABI. CARTDECK owns the Activity, library, UI and storage.
package xendroid.compose;

import android.content.Context;

public final class Emulator extends xendroid.emulator.Emulator {
    public native void setup_context(Context context);
    public native void setup_launch_args(String[] args);
    public native void setup_uri_info_list_file(String path);
    public native String cartdeck_report();
}
