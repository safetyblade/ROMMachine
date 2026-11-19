package dev.cartdeck.pc.lab;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.*;

/** Only private laboratory data. Never receives production app directories. */
final class LabWorkspace {
    static final long MAX_MEDIA = 2L * 1024 * 1024 * 1024;
    static final long RESERVE = 128L * 1024 * 1024;
    final File root, system, saves, media;
    LabWorkspace(File files, String id) throws IOException {
        if (id == null || !id.matches("synthetic|synthetic-boot|[0-9a-f-]{36}")) throw new IOException("Invalid workspace");
        root = new File(files, "pc-r03/" + id);
        system = new File(root, "system");
        saves = new File(root, "saves");
        media = new File(root, "media");
        for (File dir : new File[]{system, saves, media}) {
            if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create workspace");
        }
    }

    static void copy(InputStream source, File target) throws IOException {
        if (target.exists()) throw new IOException("Refusing to overwrite media or guest disk");
        File temp = File.createTempFile(".incoming-", ".tmp", target.getParentFile());
        try {
            try (FileOutputStream out = new FileOutputStream(temp)) {
                long total = 0;
                byte[] buffer = new byte[65536];
                int count;
                while ((count = source.read(buffer)) != -1) {
                    if (count == 0) continue;
                    total += count;
                    if (total > MAX_MEDIA || temp.getUsableSpace() < RESERVE) throw new IOException("Media/storage limit");
                    out.write(buffer, 0, count);
                }
                out.getFD().sync();
            }
            if (temp.length() == 0) throw new IOException("Empty media");
            Files.move(temp.toPath(), target.toPath());
        } finally { temp.delete(); }
    }

    static boolean iso(File file) throws IOException {
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            if (input.length() < 32774) return false;
            input.seek(32769);
            return input.readByte() == 'C' && input.readByte() == 'D' && input.readByte() == '0'
                && input.readByte() == '0' && input.readByte() == '1';
        }
    }

    File archive(File[] discs) throws IOException {
        File out = new File(media, "guest.dosz");
        if (out.exists()) return out;
        File temp = File.createTempFile(".archive-", ".tmp", media);
        try {
            long total = 0;
            try (FileOutputStream raw = new FileOutputStream(temp); ZipOutputStream zip = new ZipOutputStream(raw)) {
                for (int i = 0; i < discs.length; i++) {
                    File disc = discs[i];
                    if (!iso(disc)) throw new IOException("Invalid ISO");
                    total += disc.length();
                    if (total > MAX_MEDIA || temp.getUsableSpace() < total + RESERVE) throw new IOException("Archive/storage limit");
                    ZipEntry entry = new ZipEntry(String.format(java.util.Locale.ROOT, "%02d-disc.iso", i));
                    CRC32 crc = new CRC32();
                    byte[] buf = new byte[65536];
                    try (InputStream in = new FileInputStream(disc)) {
                        int n; while ((n = in.read(buf)) != -1) crc.update(buf, 0, n);
                    }
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(disc.length());
                    entry.setCompressedSize(disc.length());
                    entry.setCrc(crc.getValue());
                    zip.putNextEntry(entry);
                    try (InputStream in = new FileInputStream(disc)) {
                        int n; while ((n = in.read(buf)) != -1) {
                            if (temp.getUsableSpace() < RESERVE) throw new IOException("Storage limit");
                            zip.write(buf, 0, n);
                        }
                    }
                    zip.closeEntry();
                }
                zip.finish(); zip.flush(); raw.getFD().sync();
            }
            if (!temp.setReadOnly()) throw new IOException("Cannot protect original archive");
            Files.move(temp.toPath(), out.toPath());
            return out;
        } finally { temp.delete(); }
    }
}
