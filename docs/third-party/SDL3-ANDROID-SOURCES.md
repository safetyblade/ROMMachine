# SDL3 Android adapter

`lemuroid-app/src/main/java/org/libsdl/app` is vendored from SDL source
`829a65d769d935c4852f8159e964312c0957260a`, directory
`android-project/app/src/main/java/org/libsdl/app`, matching the packaged SDL binary. These files implement the official
SDL Android Java/JNI surface, controller, audio and HID glue. They retain the
`org.libsdl.app` namespace required by SDL's JNI registration. No launcher UI or
separate library is included.

Copyright SDL contributors. See [SDL3-LICENSE.txt](SDL3-LICENSE.txt) (zlib licence).
The corresponding native SDL revision and dependency hashes are recorded by the
pinned hakuX native package provenance. SDL performs its own Java/native version
match before creating a surface. CARTDECK adapter sources are separate in
`com.rfandango.haku_x` and are GPL-3.0-or-later.

The only SDL Java modifications are plainly marked in `SDLActivity.java`: its superclass is AndroidX `ComponentActivity` for the existing CARTDECK Compose shell lifecycle owners, and fatal library-load dialogs use the CARTDECK product title. Upstream hakuX-specific Java UI modifications were not retained.
