package dev.cartdeck.pc.lab;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.ComponentActivity;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** PC-only launcher. No shared production data, account, or engine registration. */
public final class LabActivity extends ComponentActivity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout layout;
    private TextView status;
    private String selected;
    private boolean busy;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        selected = getPreferences(0).getString("workspace", null);
        layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        status = new TextView(this); status.setText("PC R03 laboratory — synthetic or your own lawful images. No production data.");
        layout.addView(status);
        button("Run synthetic probe", () -> work(() -> {
            LabWorkspace w = new LabWorkspace(getFilesDir(), "synthetic");
            File probe = new File(w.media,"probe.dosz");
            if (!probe.exists()) try(InputStream source=getAssets().open("probe.dosz")){
                LabWorkspace.copy(source,probe);
                if(!probe.setReadOnly())throw new IOException("Cannot protect probe");
            }
            File disk = new File(w.system,"probe.img");
            if(!disk.exists())try(InputStream source=getAssets().open("probe.img")){LabWorkspace.copy(source,disk);}
            launch("synthetic",true);
        }));
        button("Import new PC ISO(s)", () -> choose(1,true));
        button("Add prepared raw guest IMG", () -> {if(selected==null)status.setText("Import a PC workspace first");else choose(2,false);});
        button("Reopen last PC workspace", () -> {if(selected==null)status.setText("No imported workspace");else launch(selected,false);});
        button("Inspect synthetic disk and saves", () -> work(() -> {
            LabWorkspace w=new LabWorkspace(getFilesDir(),"synthetic");
            File disk=new File(w.system,"probe.img");String marker="not created";
            if(disk.isFile())try(RandomAccessFile f=new RandomAccessFile(disk,"r")){f.seek(512);byte[] m=new byte[8];f.readFully(m);marker=new String(m,java.nio.charset.StandardCharsets.US_ASCII).replaceAll("[^ -~]","?");}
            File[] files=w.saves.listFiles();final String result="Sector 2 marker: "+marker+"; save files: "+(files==null?0:files.length);
            runOnUiThread(()->status.setText(result));
        }));
        setContentView(layout);
    }
    private void button(String text,Runnable task){Button b=new Button(this);b.setText(text);b.setOnClickListener(v->{if(!busy)task.run();});layout.addView(b);}
    private void choose(int request,boolean multiple){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/octet-stream");
        i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,multiple);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,request);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;
        ArrayList<Uri> uris=new ArrayList<>();if(data.getClipData()!=null){for(int n=0;n<data.getClipData().getItemCount();n++)uris.add(data.getClipData().getItemAt(n).getUri());}
        else if(data.getData()!=null)uris.add(data.getData());
        if(request==1){
            if(uris.isEmpty()||uris.size()>2){status.setText("Select one or two ISO images");return;}
            work(()->{
                String id=UUID.randomUUID().toString();LabWorkspace w=new LabWorkspace(getFilesDir(),id);
                File[] discs=new File[uris.size()];
                for(int n=0;n<uris.size();n++){File f=new File(w.media,"original-"+n+".iso");try(InputStream in=getContentResolver().openInputStream(uris.get(n))){if(in==null)throw new IOException("Media unavailable");LabWorkspace.copy(in,f);}
                    if(!LabWorkspace.iso(f))throw new IOException("Not an ISO9660 image");if(!f.setReadOnly())throw new IOException("Cannot protect media");discs[n]=f;}
                w.archive(discs);runOnUiThread(()->{selected=id;getPreferences(0).edit().putString("workspace",id).apply();status.setText("ISO workspace ready. Add guest IMG or reopen for core installation.");});
            });
        }else if(request==2&&uris.size()==1&&selected!=null){
            final String id=selected;
            work(()->{
                LabWorkspace w=new LabWorkspace(getFilesDir(),id);
                File f=new File(w.system,"installed.img");
                File[] existing=w.system.listFiles((dir,name)-> name.endsWith(".img")||name.endsWith(".vhd")||name.endsWith(".ima"));
                if(existing!=null&&existing.length>0)throw new IOException("Guest disk already present; no overwrite");
                File incoming=new File(w.system,".guest-incoming");
                if(incoming.exists())throw new IOException("Incomplete prior import: create a new workspace");
                try{
                    try(InputStream in=getContentResolver().openInputStream(uris.get(0))){if(in==null)throw new IOException("Image unavailable");LabWorkspace.copy(in,incoming);}
                    try(RandomAccessFile disk=new RandomAccessFile(incoming,"r")){
                        if(disk.length()<7L*1024*1024||disk.length()%512!=0)throw new IOException("Invalid raw hard disk size");
                        disk.seek(510);if(disk.read()!=0x55||disk.read()!=0xaa)throw new IOException("Missing boot sector signature");
                    }
                    java.nio.file.Files.move(incoming.toPath(),f.toPath());
                }finally{incoming.delete();}
                runOnUiThread(()->status.setText("Guest disk copied. Original unchanged. Boot is not yet verified."));
            });
        }
    }
    private void launch(String id,boolean synthetic){runOnUiThread(()->{if(!isFinishing()){Intent i=new Intent(this,LabSessionActivity.class);i.putExtra("workspace",id);i.putExtra("synthetic",synthetic);startActivity(i);}});}
    private interface Work{void execute()throws Exception;}
    private void work(Work job){if(busy)return;busy=true;status.setText("Preparing private PC data…");io.execute(()->{try{job.execute();}catch(Exception e){runOnUiThread(()->status.setText("PC lab: "+e.getMessage()));}finally{runOnUiThread(()->busy=false);}});}
    @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
}
