# Dolphin engine and Sys rollback

GameCube engine versions retain their matching `dolphin-emu/Sys` tree under the system directory's `engine-versions/<source12>-<core-sha12>/` directory. Before launch, CARTDECK verifies the selected core's full SHA-256, its recorded source revision, the retained resource index and every indexed Sys file. The selected version's directory becomes the Libretro system directory. Memory cards remain in the existing stable `saves/native/gc/User` directory and are never copied, converted or deleted by asset installation or rollback.

An older engine without retained, verified matching resources fails closed. CARTDECK does not pair it with the current APK's Sys tree or infer compatibility from directory names. This is the expected boundary for engines installed before versioned asset provisioning; native GameCube v1 has not been publicly shipped. Subsequent engine updates retain each successfully provisioned version's resources for rollback. Existing unversioned Sys files are not treated as proof of a matching old bundle.

The JVM regression covers distinct-source old/new installation, rollback using the old retained Sys without opening the new APK archive, unchanged memory-card contents, missing or damaged retained resources, and mismatched engine bytes. Android gameplay and device save/resume acceptance remain separate checks.
