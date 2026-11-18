package dev.cartdeck.pc.lab;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.ComponentActivity;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.*;

/** Separate Windows setup path. Media becomes immutable at first launch; guest disk stays writable. */
public final class WindowsSetupActivity extends ComponentActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private LabWorkspace workspace;
    private String id;
    private TextView status;
    private boolean busy;
    private static final int WINDOWS=41,GAME=42,FLOPPY=43,PREPARED=44;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this);scroll.addView(panel);
        status=new TextView(this);panel.addView(status);
        button(panel,"New Windows workspace",this::newWorkspace);
        button(panel,"1. Select Windows 95 / 98 ISO",()->choose(WINDOWS));
        button(panel,"2. Select Shandalar / game ISO (optional)",()->choose(GAME));
        button(panel,"3. Select boot floppy IMG (if needed)",()->choose(FLOPPY));
        button(panel,"4. Import prepared Windows IMG (optional)",()->choose(PREPARED));
        button(panel,"5. Install / reopen Windows",this::openGuest);
        button(panel,"Refresh workspace status",this::refresh);
        button(panel,"Installation and game launch guide",this::guide);
        setContentView(scroll);
        id=getPreferences(0).getString("windows-workspace",null);
        if(id!=null)try{workspace=new LabWorkspace(getFilesDir(),id);}catch(IOException e){status.setText(e.getMessage());}
        refresh();
    }
    private void button(LinearLayout p,String text,Runnable action){
        Button b=new Button(this);b.setText(text);b.setOnClickListener(v->{if(!busy)action.run();});p.addView(b);
    }
    private void newWorkspace(){
        new AlertDialog.Builder(this).setMessage("Create a fresh isolated Windows installation? Existing lab files remain intact; reopen the previous workspace using its saved ID is not yet supported here.")
            .setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->{
                try{id=UUID.randomUUID().toString();workspace=new LabWorkspace(getFilesDir(),id);
                    if(!getPreferences(0).edit().putString("windows-workspace",id).commit())throw new IOException("Cannot persist workspace");refresh();
                }catch(IOException e){status.setText(e.getMessage());}
            }).show();
    }
    private boolean ready(){if(workspace!=null)return true;status.setText("Create a Windows workspace first.");return false;}
    private boolean sealed(){return new File(workspace.media,"guest.dosz").isFile();}
    private File item(int request){
        return new File(workspace.media,request==WINDOWS?"WINCD.ISO":request==GAME?"GAMECD.ISO":"BOOT.IMG");
    }
    private void choose(int request){
        if(!ready())return;
        if(request!=PREPARED&&sealed()){status.setText("Media is sealed. New media requires a new workspace; existing installation stays unchanged.");return;}
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,request);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null||!ready()||busy)return;
        if(request<WINDOWS||request>PREPARED)return;
        final Uri uri=data.getData();
        work(()->{
            if(request!=PREPARED&&sealed())throw new IOException("Media already sealed");
            File target=request==PREPARED?new File(workspace.system,"installed.img"):item(request);
            if(target.exists())throw new IOException("No overwrite: file already exists");
            if(request==PREPARED&&diskCount()>0)throw new IOException("Existing guest disk retained");
            File incoming=File.createTempFile("import-", ".stage",target.getParentFile());
            if(!incoming.delete())throw new IOException("Cannot prepare temporary media");
            try{
                try(InputStream src=getContentResolver().openInputStream(uri)){
                    if(src==null)throw new IOException("Media unavailable");LabWorkspace.copy(src,incoming);
                }
                if(request==WINDOWS||request==GAME){if(!LabWorkspace.iso(incoming))throw new IOException("Expected ISO9660 image");}
                else {
                    try(RandomAccessFile f=new RandomAccessFile(incoming,"r")){
                        long len=f.length();
                        if(request==FLOPPY && len!=1474560&&len!=737280&&len!=1228800&&len!=2949120)throw new IOException("Unsupported floppy image size");
                        if(request==PREPARED&&(len<7L*1024*1024||len%512!=0))throw new IOException("Expected raw bootable IMG (7MB minimum)");
                        f.seek(510);if(f.read()!=0x55||f.read()!=0xaa)throw new IOException("Missing boot signature");
                    }
                }
                if(request!=PREPARED&&!incoming.setReadOnly())throw new IOException("Cannot protect source media");
                Files.move(incoming.toPath(),target.toPath());
            }finally{incoming.delete();}
        });
    }
    private int diskCount(){
        File[] files=workspace.system.listFiles((dir,name)->name.toLowerCase(Locale.ROOT).matches(".*\\.(img|vhd|ima)"));
        return files==null?0:files.length;
    }
    private void refresh(){
        if(workspace==null){status.setText("No Windows workspace. Create one to begin.");return;}
        status.setText("Workspace: "+id+"\nWindows ISO: "+item(WINDOWS).isFile()+" | Game ISO: "+item(GAME).isFile()+
            "\nBoot floppy: "+item(FLOPPY).isFile()+" | Guest disks: "+diskCount()+"\nMedia sealed: "+sealed()+
            "\nDisk presence is not proof of Windows installation. Shut down in Windows before exit.");
    }
    private void openGuest(){
        if(!ready())return;
        if(!item(WINDOWS).isFile()&&diskCount()==0){status.setText("Select a Windows ISO or prepared guest disk first.");return;}
        new AlertDialog.Builder(this).setMessage("Seal the selected media for this installation. Include the Shandalar ISO and any required boot floppy before first launch. Guest writes remain in this workspace.")
            .setNegativeButton("Cancel",null).setPositiveButton("Launch",(d,w)->work(()->{
                seal();
                runOnUiThread(()->{if(!isFinishing()){
                    Intent i=new Intent(this,LabSessionActivity.class);i.putExtra("workspace",id);i.putExtra("windows",true);startActivity(i);
                }});
            })).show();
    }
    private void seal()throws IOException{
        File archive=new File(workspace.media,"guest.dosz");
        if(archive.isFile())return;
        File temp=File.createTempFile("guest-", ".stage",workspace.media);
        try{
            try(FileOutputStream raw=new FileOutputStream(temp);ZipOutputStream zip=new ZipOutputStream(raw)){
                long total=0;byte[] buffer=new byte[65536];
                for(int request:new int[]{WINDOWS,GAME,FLOPPY}){
                    File f=item(request);if(!f.isFile())continue;
                    total+=f.length();if(total>LabWorkspace.MAX_MEDIA||temp.getUsableSpace()<f.length()+LabWorkspace.RESERVE)throw new IOException("Workspace storage limit");
                    CRC32 crc=new CRC32();try(InputStream in=new FileInputStream(f)){int n;while((n=in.read(buffer))!=-1)crc.update(buffer,0,n);}
                    String name=request==WINDOWS?"01WIN.ISO":request==GAME?"02GAME.ISO":"BOOT.IMG";
                    ZipEntry entry=new ZipEntry(name);entry.setMethod(ZipEntry.STORED);entry.setSize(f.length());entry.setCompressedSize(f.length());entry.setCrc(crc.getValue());zip.putNextEntry(entry);
                    try(InputStream in=new FileInputStream(f)){int n;while((n=in.read(buffer))!=-1){if(temp.getUsableSpace()<LabWorkspace.RESERVE)throw new IOException("Storage reserve");zip.write(buffer,0,n);}}
                    zip.closeEntry();
                }
                // No DOSBOX.BAT: let the core present its OS installation/boot menu and mount media.
                zip.putNextEntry(new ZipEntry("README.TXT"));zip.write("CARTDECK PC Lab. Use the core operating-system menu.".getBytes(java.nio.charset.StandardCharsets.US_ASCII));zip.closeEntry();
                zip.finish();zip.flush();raw.getFD().sync();
            }
            if(!temp.setReadOnly())throw new IOException("Cannot seal archive");
            Files.move(temp.toPath(),archive.toPath());
        }finally{temp.delete();}
    }
    private void guide(){
        new AlertDialog.Builder(this).setTitle("Windows / Shandalar — owner acceptance").setMessage(
            "1. Supply lawful Windows media and optional boot floppy plus Shandalar CD before opening. No OS or game is bundled.\n\n"+
            "2. In the DOSBox Pure menu use Boot and Install New Operating System; choose a disk size and complete Windows Setup. For a prepared disk use Run Installed Operating System.\n\n"+
            "3. After installing, boot the installed OS. Switch to the game CD using the lab disc controls. In Windows open the CD, run the actual installer and launch the installed game from Windows Explorer or Start menu. Exact Shandalar executable must be confirmed inside the guest.\n\n"+
            "4. Save in game, shut down in Windows, exit and reopen this same workspace. Check that game progress remains. Direct host-to-EXE launch is not claimed.").setPositiveButton("OK",null).show();
    }
    private interface Work{void run()throws Exception;}
    private void work(Work task){if(busy)return;busy=true;status.setText("Preparing private Windows workspace…");
        io.execute(()->{String error=null;try{task.run();}catch(Exception e){error=e.getMessage();}final String failure=error;
            runOnUiThread(()->{busy=false;if(failure==null)refresh();else status.setText("Windows lab: "+failure);});});
    }
    @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
}
