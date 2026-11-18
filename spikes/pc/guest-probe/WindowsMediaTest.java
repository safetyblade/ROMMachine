package dev.cartdeck.pc.lab;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Host JVM fixture: verifies installation workspace/media invariants, not Windows boot. */
public final class WindowsMediaTest {
    private static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);}
    private static void iso(File target,byte marker)throws IOException{
        try(RandomAccessFile f=new RandomAccessFile(target,"rw")){f.setLength(40960);f.seek(32769);f.writeBytes("CD001");f.seek(0);f.write(marker);}
    }
    public static void main(String[] args)throws Exception{
        File root=Files.createTempDirectory("windows-media-test").toFile();
        LabWorkspace w=new LabWorkspace(root,UUID.randomUUID().toString());
        check(!WindowsMedia.isWorkspace(w.root),"unmarked workspace not Windows");
        WindowsMedia.markWorkspace(w);
        check(WindowsMedia.isWorkspace(w.root),"marked workspace");
        File win=new File(w.media,"WINCD.ISO"),game=new File(w.media,"GAMECD.ISO");
        iso(win,(byte)11);iso(game,(byte)22);
        byte[] original=Files.readAllBytes(win.toPath());
        File disk=new File(w.system,"installed.img");
        Files.write(disk.toPath(),new byte[]{4,5,6});
        File archive=WindowsMedia.seal(w);
        byte[] first=Files.readAllBytes(archive.toPath());
        try(ZipFile z=new ZipFile(archive)){
            check(z.getEntry("01WIN.ISO")!=null&&z.getEntry("02GAME.ISO")!=null,"disc identity/order");
            check(z.getEntry("DOSBOX.BAT")==null,"native OS menu preserved");
            try(InputStream in=z.getInputStream(z.getEntry("01WIN.ISO"))){check(in.read()==11,"Windows content");}
            try(InputStream in=z.getInputStream(z.getEntry("02GAME.ISO"))){check(in.read()==22,"game content");}
        }
        LabWorkspace reopened=new LabWorkspace(root,w.root.getName());
        check(WindowsMedia.isWorkspace(reopened.root),"workspace identity");
        check(Arrays.equals(first,Files.readAllBytes(WindowsMedia.seal(reopened).toPath())),"sealed media unchanged");
        check(Arrays.equals(original,Files.readAllBytes(win.toPath())),"source unchanged");
        check(Arrays.equals(new byte[]{4,5,6},Files.readAllBytes(disk.toPath())),"guest disk unchanged by media seal/reopen");
        System.out.println("PASS: Windows workspace marker, media sealing, original and guest protection, reopen identity");
    }
}
