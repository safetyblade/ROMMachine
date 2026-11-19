#!/usr/bin/env python3
"""Anchor-exact private runtime wiring; retain upstream surface/input implementation and JNI name."""
from pathlib import Path
import sys

root, java = map(Path, sys.argv[1:])
def edit_file(path, old, new):
    text = path.read_text()
    if text.count(old) != 1:
        raise RuntimeError("Upstream anchor did not match: " + str(path) + ": " + old[:60])
    path.write_text(text.replace(old, new))

loader = root / "dlls/ntdll/unix/loader.c"
edit_file(loader, '    if (!(build_dir = remove_tail( ntdll_dir, "/dlls/ntdll" )))',
'''#ifdef __ANDROID__
    if ((env = getenv( "CARTDECK_WINE_ROOT" )) && env[0] == '/')
    {
        dll_dir = build_path( env, "arm64-v8a/lib/wine" );
        bin_dir = build_path( env, "arm64-v8a/bin" );
        data_dir = build_path( env, "share/wine" );
        wineloader = build_path( bin_dir, "wine" );
    }
    else
#endif
    if (!(build_dir = remove_tail( ntdll_dir, "/dlls/ntdll" )))''')
edit_file(root / "dlls/wow64/syscall.c",
          'native_machine == IMAGE_FILE_MACHINE_ARM64 ? L"xtajit.dll" : L"wow64cpu.dll"',
          'native_machine == IMAGE_FILE_MACHINE_ARM64 ? L"wowbox64.dll" : L"wow64cpu.dll"')
source = (root / "dlls/wineandroid.drv/WineActivity.java").read_text()
old = 'public class WineActivity extends Activity'
assert source.count(old) == 1
source = source.replace(old, 'public class WineActivity extends androidx.activity.ComponentActivity')
start = source.index('    private void loadWine( String cmdline )')
end = source.index('    private void createProgressDialog(', start)
source = source[:start] + '''    private com.swordfish.lemuroid.app.pc.WineNativeSession cartdeckSession;
    private void loadWine( String unused )
    {
        try {
            cartdeckSession = com.swordfish.lemuroid.app.pc.WineNativeSession.prepare(this);
            runOnUiThread(() -> createProgressDialog(0, "Preparing CARTDECK Windows environment..."));
            System.load(cartdeckSession.ntdll);
            String err = wine_init(cartdeckSession.argv, cartdeckSession.env);
            runOnUiThread(() -> cartdeckSession.fail(this, "Wine initialization returned: " + err));
        } catch (Throwable error) {
            runOnUiThread(() -> com.swordfish.lemuroid.app.pc.WineNativeSession.failure(this, error));
        }
    }

    @Override public void onBackPressed()
    {
        if (cartdeckSession != null) cartdeckSession.menu(this, desktop_window == null ? 0 : desktop_window.hwnd);
        else super.onBackPressed();
    }

    @Override public void onDestroy()
    {
        if (cartdeckSession != null) cartdeckSession.close();
        super.onDestroy();
        // Wine's process globals cannot be safely reused after the Android host exits.
        android.os.Process.killProcess(android.os.Process.myPid());
    }

''' + source[end:]
# Drop old asset installer entirely; no global shared prefix, unchecked file extraction, or executable chmod.
start = source.index('    private final boolean isFileWanted(')
end = source.index('    // Generic Wine window class', start)
source = source[:start] + '    //\\n' + source[end:]
old = '        setContentView( new TopView( this, hwnd ));'
assert source.count(old) == 1
source = source.replace(old, '''        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        frame.addView(new TopView(this, hwnd), new android.widget.FrameLayout.LayoutParams(-1, -1));
        android.widget.Button menu = new android.widget.Button(this);
        menu.setText("CARTDECK");
        menu.setOnClickListener(v -> onBackPressed());
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(-2, -2, android.view.Gravity.TOP | android.view.Gravity.RIGHT);
        frame.addView(menu, params);
        setContentView(frame);''')
old = '        progress_dialog.dismiss();'
assert source.count(old) == 1
source = source.replace(old, '        if (progress_dialog != null) progress_dialog.dismiss();')
path = java / "org/winehq/wine/WineActivity.java"
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(source)
