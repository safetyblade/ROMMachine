package dev.cartdeck.pc.winelab;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Owner-only synthetic translator proof. No arbitrary EXE execution or OS assets. */
public final class WineLabActivity extends Activity {
    private static final String MARKER = "CARTDECK_BOX64_OK\n";
    private volatile Process child;
    private volatile boolean stopped;
    private TextView status;
    private Button test;
    private Thread worker;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24,24,24,24);
        TextView title = new TextView(this);
        title.setText("CARTDECK Wine Lab — ARM64 translator");
        title.setTextSize(22);
        layout.addView(title);
        test = new Button(this);
        test.setText("Run two independent translated guest sessions");
        layout.addView(test);
        status = new TextView(this);
        status.setText("Synthetic x86_64 guest only. No Windows installation, no console files, no game media.");
        status.setTextIsSelectable(true);
        layout.addView(status);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);setContentView(scroll);
        test.setOnClickListener(v -> {
            if(worker != null && worker.isAlive()) return;
            test.setEnabled(false);
            status.setText("Executing bundled x86_64 fixture via native Box64...");
            worker = new Thread(this::runProbe,"pc-translator");
            worker.start();
        });
    }
    private static String hash(File f) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=Files.newInputStream(f.toPath())){
            byte[] buffer=new byte[65536];int n;
            while((n=in.read(buffer))!=-1)if(n>0)digest.update(buffer,0,n);
        }
        StringBuilder out=new StringBuilder();
        for(byte v:digest.digest())out.append(String.format(java.util.Locale.ROOT,"%02x",v & 255));
        return out.toString();
    }
    private void runProbe(){
        String result;
        try{
            File runner=new File(getApplicationInfo().nativeLibraryDir,"libcartdeck_box64.so");
            if(!runner.isFile()||!runner.canExecute())throw new IllegalStateException("Packaged translator not executable");
            File work=new File(getFilesDir(),"probe-"+UUID.randomUUID());
            if(!work.mkdir())throw new IllegalStateException("Private workspace unavailable");
            File guest=new File(work,"probe-x64.elf");
            try(InputStream in=getAssets().open("probe-x64.elf"); FileOutputStream out=new FileOutputStream(guest)){
                byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)if(n>0)out.write(buf,0,n);
                out.getFD().sync();
            }
            String digest=hash(guest);
            for(int n=1;n<=2;n++){
                if(stopped)throw new InterruptedException();
                File log=new File(work,"session-"+n+".log");
                ProcessBuilder pb=new ProcessBuilder(runner.getAbsolutePath(),guest.getAbsolutePath());
                pb.directory(work).redirectErrorStream(true).redirectOutput(log);
                pb.environment().clear();
                pb.environment().put("HOME",work.getAbsolutePath());
                pb.environment().put("TMPDIR",work.getAbsolutePath());
                pb.environment().put("PATH","/system/bin");
                pb.environment().put("BOX64_DYNAREC","1");
                pb.environment().put("BOX64_LOG","1");
                Process launched=pb.start();
                child=launched;
                if(stopped)launched.destroyForcibly();
                if(!launched.waitFor(30,TimeUnit.SECONDS)){
                    launched.destroyForcibly();throw new IllegalStateException("Translator timed out");
                }
                int code=launched.exitValue();
                child=null;
                byte[] bytes=Files.readAllBytes(log.toPath());
                String output=new String(bytes,StandardCharsets.UTF_8);
                if(code!=37||!output.contains(MARKER.trim()))throw new IllegalStateException("Guest result "+code+"\n"+output.substring(Math.max(0,output.length()-2500)));
                byte[] contents=Files.readAllBytes(new File(work,"probe-progress.txt").toPath());
                String expected=n==1?MARKER:MARKER+MARKER;
                if(!Arrays.equals(contents,expected.getBytes(StandardCharsets.US_ASCII)))throw new IllegalStateException("Guest persistence mismatch");
            }
            if(!digest.equals(hash(guest)))throw new IllegalStateException("Fixture changed");
            result="PASS - Android ARM64 Box64\nTwo independent guest exits = 37\nGuest-written file retained across sessions (1 -> 2 records)\nFixture unchanged\nBox64 sha256="+hash(runner)+"\nWine / Windows execution NOT TESTED";
        }catch(Exception e){result="FAIL - "+e.getClass().getSimpleName()+": "+e.getMessage()+"\nWine / Windows execution NOT TESTED";}
        finally{Process p=child;child=null;if(p!=null&&p.isAlive())p.destroyForcibly();}
        final String report=result;
        runOnUiThread(()->{if(!stopped){status.setText(report);test.setEnabled(true);}});
    }
    @Override protected void onDestroy(){
        stopped=true;Process p=child;if(p!=null)p.destroyForcibly();
        if(worker!=null)worker.interrupt();super.onDestroy();
    }
}
