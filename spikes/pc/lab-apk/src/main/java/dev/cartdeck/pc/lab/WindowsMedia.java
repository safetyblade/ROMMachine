package dev.cartdeck.pc.lab;

import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

/** Pure Java media-sealing contract; no runtime or production dependencies. */
final class WindowsMedia {
    private static final byte[] MARKER="CARTDECK_PC_LAB_WINDOWS_1\n".getBytes(StandardCharsets.US_ASCII);
    private static final String[] SOURCE={"WINCD.ISO","GAMECD.ISO","BOOT.IMG"};
    private static final String[] ENTRY={"01WIN.ISO","02GAME.ISO","BOOT.IMG"};
    static void markWorkspace(LabWorkspace w)throws IOException{
        File marker=new File(w.root,"windows-workspace.marker");
        if(!marker.exists())LabWorkspace.copy(new ByteArrayInputStream(MARKER),marker);
        if(!isWorkspace(w.root))throw new IOException("Invalid workspace marker");
    }
    static boolean isWorkspace(File root){
        File marker=new File(root,"windows-workspace.marker");
        try{return marker.isFile()&&marker.length()==MARKER.length&&java.util.Arrays.equals(MARKER,Files.readAllBytes(marker.toPath()));}
        catch(IOException e){return false;}
    }
    static File seal(LabWorkspace w)throws IOException{
        if(!isWorkspace(w.root))throw new IOException("Not a Windows workspace");
        File archive=new File(w.media,"guest.dosz");
        if(archive.isFile())return archive;
        File temp=File.createTempFile("guest-", ".stage",w.media);
        try{
            try(FileOutputStream raw=new FileOutputStream(temp);ZipOutputStream zip=new ZipOutputStream(raw)){
                long total=0;byte[] buffer=new byte[65536];
                for(int i=0;i<SOURCE.length;i++){
                    File f=new File(w.media,SOURCE[i]);if(!f.isFile())continue;
                    if(i<2&&!LabWorkspace.iso(f))throw new IOException("Not ISO9660: "+SOURCE[i]);
                    total+=f.length();if(total>LabWorkspace.MAX_MEDIA||temp.getUsableSpace()<f.length()+LabWorkspace.RESERVE)throw new IOException("Workspace storage limit");
                    CRC32 crc=new CRC32();try(InputStream in=new FileInputStream(f)){int n;while((n=in.read(buffer))!=-1)crc.update(buffer,0,n);}
                    ZipEntry entry=new ZipEntry(ENTRY[i]);entry.setMethod(ZipEntry.STORED);entry.setSize(f.length());entry.setCompressedSize(f.length());entry.setCrc(crc.getValue());zip.putNextEntry(entry);
                    try(InputStream in=new FileInputStream(f)){int n;while((n=in.read(buffer))!=-1){if(temp.getUsableSpace()<LabWorkspace.RESERVE)throw new IOException("Storage reserve");zip.write(buffer,0,n);}}
                    zip.closeEntry();
                }
                // No DOSBOX.BAT: allow the core's native installed-OS menu/automatic media mounting.
                zip.putNextEntry(new ZipEntry("README.TXT"));zip.write("CARTDECK PC Lab. Use the core operating-system menu.".getBytes(StandardCharsets.US_ASCII));zip.closeEntry();
                zip.finish();zip.flush();raw.getFD().sync();
            }
            if(!temp.setReadOnly())throw new IOException("Cannot seal archive");
            Files.move(temp.toPath(),archive.toPath());
            return archive;
        }finally{temp.delete();}
    }
}
