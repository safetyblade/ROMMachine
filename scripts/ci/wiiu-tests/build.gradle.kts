plugins { kotlin("jvm") version "2.0.21" }
repositories { mavenCentral(); google() }
val sdk = providers.environmentVariable("ANDROID_HOME").get()
val androidApi = files("$sdk/platforms/android-35/android.jar")
dependencies {
    compileOnly(androidApi)
    testImplementation(androidApi)
    testImplementation("junit:junit:4.13.2")
}
kotlin { jvmToolchain(17) }
sourceSets {
    main {
        java.srcDir("../../../lemuroid-app/build/generated/source/buildConfig/freeBundle/debug")
        java.include("com/swordfish/lemuroid/BuildConfig.java")
        kotlin.srcDir("../../../lemuroid-app/src/main/java")
        kotlin.include("**/wiiu/WiiuStorage.kt", "**/wiiu/WiiuEngine.kt", "**/wiiu/WiiuPad.kt", "**/wiiu/WiiuViewport.kt", "**/xbox/XboxEngineStore.kt")
    }
    test {
        kotlin.srcDir("../../../lemuroid-app/src/test/java")
        kotlin.include("**/wiiu/WiiuRuntimeTest.kt")
    }
}
tasks.test { testLogging { events("passed", "failed", "skipped") } }
