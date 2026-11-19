package com.swordfish.lemuroid.app.pc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.SystemClock;
import android.system.Os;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity;
import com.swordfish.lemuroid.app.shared.game.GameProcessLock;
import com.swordfish.lemuroid.app.shared.game.pc.PcManagedSession;
import com.swordfish.lemuroid.lib.library.db.entity.Game;
import com.swordfish.lemuroid.lib.library.pc.PcCartridgeStore;
import com.swordfish.lemuroid.lib.library.pc.PcWineCartridges;
import com.swordfish.lemuroid.lib.library.pc.PcWorkspace;
import org.winehq.wine.WineActivity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Source-built candidate only; Android package owns native executable mappings. */
public final class WineNativeSession {
    private static final String ASSETS = "cartdeck-wine/";
    private static final long MAX_TOTAL = 2L * 1024 * 1024 * 1024;
    private static final String NTDLL = "arm64-v8a/lib/wine/aarch64-unix/ntdll.so";
    public final String ntdll;
    public final String[] argv, env;
    private final Game game;
    private final File server, prefix, working;
    private final Map<String,String> environment;
    private final long start = SystemClock.elapsedRealtime();
    private volatile boolean closed;
    private View menu;

    private static final class Item {
        final String mode, hash, path, library;
        final long size;
        Item(String row) {
            String[] fields = row.split("\t", -1);
            if(fields.length!=5)throw new IllegalArgumentException("Invalid runtime manifest");
            mode=fields[0]; hash=fields[1]; path=fields[2]; library=fields[3]; size=Long.parseLong(fields[4]);
            if(!mode.matches("[ND]")||!hash.matches("[a-f0-9]{64}")||
                !path.matches("[A-Za-z0-9_.+/-]+")||path.startsWith("/")||size<0||size>MAX_TOTAL)
                throw new IllegalArgumentException("Invalid runtime record");
            for(String part:path.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))
                throw new IllegalArgumentException("Invalid runtime path");
            if(mode.equals("N")&&!library.matches("libcartdeckwine_[a-f0-9]{24}\\.so"))
                throw new IllegalArgumentException("Invalid native file name");
            if(mode.equals("D")&&!library.equals("-"))throw new IllegalArgumentException("Invalid data record");
        }
    }
    private static byte[] manifest(Activity activity) throws IOException {
        try(InputStream input=activity.getAssets().open(ASSETS+"runtime.tsv"); ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192]; int count;
            while((count=input.read(buffer))!=-1){out.write(buffer,0,count);if(out.size()>4*1024*1024)throw new IOException("Runtime manifest too large");}
            return out.toByteArray();
        }
    }
    private static String hex(byte[] bytes){
        StringBuilder out=new StringBuilder();
        for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));
        return out.toString();
    }
    private static String sha(File file) throws Exception {
        MessageDigest h=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)){
            byte[] buffer=new byte[128*1024];int n;
            while((n=in.read(buffer))!=-1)if(n>0)h.update(buffer,0,n);
        }
        return hex(h.digest());
    }
    private static File inside(File root,String name) throws IOException {
        File result=new File(root,name);
        File canonical=result.getCanonicalFile();
        if(!canonical.toPath().startsWith(root.getCanonicalFile().toPath()))
            throw new IOException("Unexpected private path");
        return result;
    }
    private static File directory(File file) throws IOException {
        if(Files.isSymbolicLink(file.toPath())||!(file.isDirectory()||file.mkdirs()))
            throw new IOException("Private directory unavailable");
        return file;
    }
    private static void verify(File file,Item item) throws Exception{
        if(!file.isFile()||file.length()!=item.size||!sha(file).equals(item.hash))
            throw new IOException("Runtime file verification failed: "+item.path);
    }
    private static Map<String,Item> parse(byte[] bytes) {
        Map<String,Item> items=new LinkedHashMap<>();
        long size=0;
        for(String row:new String(bytes,StandardCharsets.US_ASCII).split("\n")){
            Item item=new Item(row);
            if(items.put(item.path,item)!=null||items.size()>20000)
                throw new IllegalArgumentException("Duplicate or excessive runtime records");
            size=Math.addExact(size,item.size);
            if(size>MAX_TOTAL)throw new IllegalArgumentException("Runtime package too large");
        }
        for(String required:new String[]{"arm64-v8a/bin/wine","arm64-v8a/bin/wineserver",NTDLL,
                "arm64-v8a/lib/wine/aarch64-unix/wineandroid.so",
                "arm64-v8a/lib/wine/aarch64-windows/wowbox64.dll",
                "arm64-v8a/lib/wine/i386-windows/ntdll.dll"}){
            if(!items.containsKey(required))throw new IllegalArgumentException("Missing Wine component: "+required);
        }
        if(!items.get(NTDLL).mode.equals("N"))throw new IllegalArgumentException("Invalid Wine loader receipt");
        return items;
    }
    private static File nativeFile(Activity activity,Item item) throws Exception {
        File dir=new File(activity.getApplicationInfo().nativeLibraryDir).getCanonicalFile();
        File file=new File(dir,item.library);
        if(!file.getCanonicalFile().equals(file)||!file.canExecute())
            throw new IOException("Native APK component is not executable");
        verify(file,item);
        return file;
    }
    private static void checkTree(Activity activity,File root,Map<String,Item> items) throws Exception {
        for(Item item:items.values()){
            File file=new File(root,item.path);
            inside(root,item.path.substring(0,item.path.lastIndexOf('/')));
            if(item.mode.equals("N")){
                File source=nativeFile(activity,item);
                if(!Files.isSymbolicLink(file.toPath())||!file.getCanonicalFile().equals(source))
                    throw new IOException("Native runtime link mismatch: "+item.path);
            }else{
                if(Files.isSymbolicLink(file.toPath()))throw new IOException("Unexpected data link");
                verify(inside(root,item.path),item);
            }
        }
    }
    private static File installRuntime(Activity activity,Map<String,Item> items,byte[] bytes) throws Exception {
        File base=directory(new File(activity.getFilesDir(),"pc-wine-native"));
        String version=hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        File root=inside(base,version);
        if(root.exists()){checkTree(activity,root,items);return root;}
        File stage=inside(base,".stage-"+UUID.randomUUID());
        directory(stage);
        try{
            Set<String> received=new HashSet<>();
            try(ZipInputStream zip=new ZipInputStream(activity.getAssets().open(ASSETS+"runtime.zip"))){
                ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    Item item=items.get(entry.getName());
                    if(item==null||!item.mode.equals("D")||entry.isDirectory()||!received.add(item.path))
                        throw new IOException("Unexpected Wine package entry");
                    File target=inside(stage,item.path);
                    directory(target.getParentFile());
                    long count=0;
                    MessageDigest hash=MessageDigest.getInstance("SHA-256");
                    try(FileOutputStream out=new FileOutputStream(target)){
                        byte[] buffer=new byte[128*1024];int n;
                        while((n=zip.read(buffer))!=-1){
                            if(n==0)continue;
                            count+=n;
                            if(count>item.size||stage.getUsableSpace()<128L*1024*1024)throw new IOException("Wine package space/size limit");
                            out.write(buffer,0,n);hash.update(buffer,0,n);
                        }
                        out.getFD().sync();
                    }
                    if(count!=item.size||!hex(hash.digest()).equals(item.hash))throw new IOException("Wine package digest mismatch");
                    zip.closeEntry();
                }
            }
            for(Item item:items.values()){
                if(item.mode.equals("D")){
                    if(!received.contains(item.path))throw new IOException("Missing Wine data: "+item.path);
                }else{
                    File target=inside(stage,item.path);
                    directory(target.getParentFile());
                    Files.createSymbolicLink(target.toPath(),nativeFile(activity,item).toPath());
                }
            }
            checkTree(activity,stage,items);
            Files.move(stage.toPath(),root.toPath(),StandardCopyOption.ATOMIC_MOVE);
            return root;
        }finally{
            // Do not follow runtime links or touch a prior version.
            if(stage.isDirectory()){
                try(java.util.stream.Stream<java.nio.file.Path> walk=Files.walk(stage.toPath())){
                    walk.sorted(Comparator.reverseOrder()).forEach(x->{try{Files.deleteIfExists(x);}catch(IOException ignored){}});
                }
            }
        }
    }
    public static WineNativeSession prepare(WineActivity activity) throws Exception {
        Game game=(Game)activity.getIntent().getSerializableExtra("cartdeck.game");
        if(game==null||!PcManagedSession.INSTANCE.permits(PcManagedSession.INSTANCE.getLocalOwner(),false))
            throw new IOException("No permitted local cartridge");
        PcCartridgeStore.Entry entry=PcManagedSession.INSTANCE.resolve(activity,game);
        if(entry==null||!PcWineCartridges.INSTANCE.isWine(entry))throw new IOException("Not a managed Wine cartridge");
        GameProcessLock.INSTANCE.acquireExclusive(activity.getApplicationContext());
        {
            byte[] bytes=manifest(activity);
            Map<String,Item> items=parse(bytes);
            File runtime=installRuntime(activity,items,bytes);
            File loader=nativeFile(activity,items.get("arm64-v8a/bin/wine"));
            File server=nativeFile(activity,items.get("arm64-v8a/bin/wineserver"));
            File ntdll=nativeFile(activity,items.get(NTDLL));
            PcWorkspace workspace=new PcCartridgeStore(activity.getFilesDir()).workspace(entry);
            File executable=PcWineCartridges.INSTANCE.prepareExecutable(entry,workspace);
            File wineRoot=directory(workspace.getRuntime());
            File prefix=inside(wineRoot,"wineprefix");
            if(prefix.exists()&&(!prefix.isDirectory()||Files.isSymbolicLink(prefix.toPath())))
                throw new IOException("Unexpected Wine prefix");
            File home=directory(inside(wineRoot,"home"));
            File tmp=directory(inside(wineRoot,"tmp"));
            File working=executable.getParentFile().getCanonicalFile();
            Map<String,String> env=new LinkedHashMap<>();
            env.put("HOME",home.getPath()); env.put("USER","cartdeck"); env.put("TMPDIR",tmp.getPath());
            env.put("CARTDECK_WINE_ROOT",runtime.getPath()); env.put("WINEPREFIX",prefix.getPath());
            env.put("WINEARCH","win64"); env.put("WINELOADER",loader.getPath()); env.put("WINESERVER",server.getPath());
            env.put("WINEDLLPATH",new File(runtime,"arm64-v8a/lib/wine").getPath());
            env.put("LD_LIBRARY_PATH",new File(runtime,"arm64-v8a/lib").getPath()+":"+activity.getApplicationInfo().nativeLibraryDir);
            env.put("PATH",new File(runtime,"arm64-v8a/bin").getPath()+":/system/bin");
            env.put("WINEDEBUG","-all,err+all"); env.put("WINEDLLOVERRIDES","mscoree,mshtml=");
            env.put("WINEDEBUGLOG",new File(wineRoot,"wine-session.log").getPath());
            env.put("LANG","C.UTF-8");
            // Do not execute extracted files with Android execve or lower targetSdk.
            Os.chdir(working.getPath());
            String winPath="Z:"+executable.getAbsolutePath().replace('/','\\');
            String[] argv={loader.getPath(),"c:\\windows\\system32\\explorer.exe","/desktop=shell,,android",winPath};
            return new WineNativeSession(game,ntdll,argv,env,server,prefix,working);
        }
    }
    private WineNativeSession(Game game,File ntdll,String[] argv,Map<String,String> map,File server,File prefix,
                              File working){
        this.game=game;this.ntdll=ntdll.getPath();this.argv=argv;this.environment=new LinkedHashMap<>(map);
        this.env=new String[map.size()*2];int i=0;
        for(Map.Entry<String,String> e:map.entrySet()){env[i++]=e.getKey();env[i++]=e.getValue();}
        this.server=server;this.prefix=prefix;this.working=working;
    }
    public static void failure(Activity activity,Throwable error){
        if(activity.isFinishing())return;
        activity.setResult(BaseGameActivity.RESULT_ERROR,new Intent().putExtra(
            BaseGameActivity.PLAY_GAME_RESULT_ERROR,"Wine startup failed: "+error.getClass().getSimpleName()+". Existing data retained."));
        activity.finish();
    }
    public void fail(Activity activity,String message){failure(activity,new IOException(message));}
    private void hideMenu(WineActivity activity){
        if(menu!=null){
            if(menu.getParent() instanceof FrameLayout)((FrameLayout)menu.getParent()).removeView(menu);
            menu=null;
        }
    }
    public void menu(WineActivity activity,int hwnd){
        if(activity.isFinishing()||closed)return;
        if(menu!=null){hideMenu(activity);return;}
        FrameLayout root=activity.findViewById(android.R.id.content);
        if(root==null)return;
        menu=WineMenuOverlay.create(activity,game.displayName(),
            ()->hideMenu(activity),
            ()->{hideMenu(activity);keyboard(activity,hwnd);},
            ()->{hideMenu(activity);confirmExit(activity);});
        root.addView(menu,new FrameLayout.LayoutParams(-1,-1));
    }
    private void keyboard(WineActivity activity,int hwnd){
        if(hwnd==0){Toast.makeText(activity,"Windows display is not ready",Toast.LENGTH_SHORT).show();return;}
        EditText field=new EditText(activity);
        field.setSingleLine(true);
        new AlertDialog.Builder(activity).setTitle("Send keyboard text").setView(field).setNegativeButton("Cancel",null)
            .setPositiveButton("Send",(dialog,which)->{
                KeyEvent[] events=KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(field.getText().toString().toCharArray());
                if(events==null){Toast.makeText(activity,"Use a physical keyboard for this text",Toast.LENGTH_LONG).show();return;}
                for(KeyEvent event:events)activity.wine_keyboard_event(hwnd,event.getAction(),event.getKeyCode(),event.getMetaState());
            }).show();
    }
    private void confirmExit(WineActivity activity){
        new AlertDialog.Builder(activity).setTitle("Return to Shelf")
            .setMessage("Save and close your game in Windows first. Returning may stop any remaining Wine processes.")
            .setNegativeButton("Cancel",null).setPositiveButton("Return",(d,w)->{
                activity.setResult(Activity.RESULT_OK,new Intent()
                    .putExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME,game)
                    .putExtra(BaseGameActivity.PLAY_GAME_RESULT_LEANBACK,activity.getIntent().getBooleanExtra("cartdeck.leanback",false))
                    .putExtra(BaseGameActivity.PLAY_GAME_RESULT_SESSION_DURATION,SystemClock.elapsedRealtime()-start));
                activity.finish();
            }).show();
    }
    private void serverCommand(String arg) throws Exception{
        ProcessBuilder pb=new ProcessBuilder(server.getPath(),arg);
        pb.directory(working).redirectErrorStream(true).redirectOutput(new File(working,"wine-server-cleanup.log"));
        pb.environment().clear();pb.environment().putAll(environment);
        java.lang.Process process=pb.start();
        if(!process.waitFor(2,TimeUnit.SECONDS)){process.destroyForcibly();throw new IOException("Wine server did not stop");}
    }
    public synchronized void close(){
        if(closed)return;
        closed=true;
        // Prefix-specific server only; never kill all Wine/console processes.
        if(prefix.isDirectory()){
            try{serverCommand("-k");serverCommand("-w");}catch(Exception ignored){}
        }
        // The existing process initializer owns the lock, released on isolated process termination.
    }
}
