#!/usr/bin/env python3
"""PC lab only: VFS v3 directory discovery and safe optical callback ownership."""
from pathlib import Path
import sys

EDITS = {
    "vfs/vfs.h": [("static const int SUPPORTED_VERSION = 2;", "static const int SUPPORTED_VERSION = 3;")],
    "vfs/vfs.cpp": [
        ("return new retro_vfs_interface {", "static retro_vfs_interface interface = {"),
        ("        &VFS::truncate\n    };", """        &VFS::truncate,
        &retro_vfs_stat_impl,
        &retro_vfs_mkdir_impl,
        &retro_vfs_opendir_impl,
        &retro_vfs_readdir_impl,
        &retro_vfs_dirent_get_name_impl,
        &retro_vfs_dirent_is_dir_impl,
        &retro_vfs_closedir_impl
    };
    return &interface;""")
    ],
    "environment.h": [
        ("struct retro_disk_control_callback *retro_disk_control_callback = nullptr;",
         "struct retro_disk_control_callback pcDiskCallbacks{};\n    struct retro_disk_control_callback *retro_disk_control_callback = nullptr;")
    ],
    "environment.cpp": [
        ("""    if (!useVirtualFileSystem) {
        return false;
    }

    vfsInterfaceInfo->required_interface_version""",
         """    // The PC-only host uses private real paths as well as virtual files.
    if (!vfsInterfaceInfo || vfsInterfaceInfo->required_interface_version > libretrodroid::VFS::SUPPORTED_VERSION) {
        return false;
    }

    vfsInterfaceInfo->required_interface_version"""),
        ("retro_disk_control_callback = static_cast<struct retro_disk_control_callback*>(data);",
         """if (!data) { retro_disk_control_callback = nullptr; return false; }
            pcDiskCallbacks = *static_cast<struct retro_disk_control_callback*>(data);
            if (!pcDiskCallbacks.get_num_images || !pcDiskCallbacks.get_image_index ||
                !pcDiskCallbacks.set_image_index || !pcDiskCallbacks.set_eject_state ||
                !pcDiskCallbacks.get_eject_state) return false;
            retro_disk_control_callback = &pcDiskCallbacks;""")
    ],
    "libretrodroid.cpp": [
        ("""        Environment::getInstance().getRetroDiskControlCallback()->set_eject_state(true);
        Environment::getInstance().getRetroDiskControlCallback()->set_image_index((unsigned) index);
        Environment::getInstance().getRetroDiskControlCallback()->set_eject_state(false);""",
         """        auto* disk = Environment::getInstance().getRetroDiskControlCallback();
        if (!disk->set_eject_state(true)) return;
        if (!disk->set_image_index((unsigned) index)) {
            disk->set_eject_state(false);
            return;
        }
        disk->set_eject_state(false);""")
    ]
}

def apply(root):
    cpp = root / "libretrodroid/src/main/cpp"
    updates = {}
    for name, edits in EDITS.items():
        path = cpp / name
        text = path.read_text()
        for before, after in edits:
            if text.count(before) != 1 or after in text:
                raise ValueError(f"PC guest overlay mismatch: {name}: {before[:60]}")
            text = text.replace(before, after, 1)
        updates[path] = text
    for path, text in updates.items():
        path.write_text(text)

if __name__ == "__main__":
    apply(Path(sys.argv[1]).resolve())
