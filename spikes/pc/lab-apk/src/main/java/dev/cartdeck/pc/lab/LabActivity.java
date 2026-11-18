package dev.cartdeck.pc.lab;

import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import com.swordfish.libretrodroid.GLRetroView;
import com.swordfish.libretrodroid.GLRetroViewData;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class LabActivity extends ComponentActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            File guest = new File(getFilesDir(), "synthetic-guest");
            File saves = new File(getFilesDir(), "synthetic-saves");
            if ((!guest.isDirectory() && !guest.mkdirs()) || (!saves.isDirectory() && !saves.mkdirs())) {
                throw new IllegalStateException("Cannot create private PC laboratory storage");
            }
            File content = new File(getFilesDir(), "synthetic.dosz");
            if (!content.isFile()) {
                // Original tiny DOS COM program: wait for key using INT 16h, then return to DOS.
                try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(content))) {
                    out.putNextEntry(new ZipEntry("PCPROBE.COM"));
                    out.write(new byte[]{(byte)0xb4,0, (byte)0xcd,0x16, (byte)0xcd,0x20});
                    out.closeEntry();
                }
            }
            GLRetroViewData data = new GLRetroViewData(this);
            data.setCoreFilePath(new File(getApplicationInfo().nativeLibraryDir,
                    "libdosbox_pure_libretro_android.so").getAbsolutePath());
            data.setGameFilePath(content.getAbsolutePath());
            data.setSystemDirectory(guest.getAbsolutePath());
            data.setSavesDirectory(saves.getAbsolutePath());
            GLRetroView view = new GLRetroView(this, data);
            getLifecycle().addObserver(view);
            setContentView(view);
        } catch (Exception failure) {
            TextView message = new TextView(this);
            message.setText("PC laboratory initialization failed: " + failure.getClass().getSimpleName());
            setContentView(message);
        }
    }
}
