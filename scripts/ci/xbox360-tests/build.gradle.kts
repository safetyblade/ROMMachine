plugins {
    kotlin("jvm") version "2.0.21"
}
repositories { mavenCentral(); google() }
val androidHome = providers.environmentVariable("ANDROID_HOME").get()
val androidApi = files("$androidHome/platforms/android-35/android.jar")
val androidCompileAar by configurations.creating
val extractDocumentFile by tasks.registering(Sync::class) {
    from({ zipTree(androidCompileAar.singleFile) }) { include("classes.jar") }
    into(layout.buildDirectory.dir("documentfile"))
}
dependencies {
    androidCompileAar("androidx.documentfile:documentfile:1.0.1@aar")
    compileOnly(files(layout.buildDirectory.file("documentfile/classes.jar")).builtBy(extractDocumentFile))
    compileOnly(androidApi)
    testImplementation(androidApi)
    testImplementation("junit:junit:4.13.2")
}
kotlin { jvmToolchain(17) }
sourceSets {
    main {
        java.srcDir("../../../lemuroid-app/src/main/java")
        java.include("xendroid/**/*.java")
        kotlin.srcDirs("../../../lemuroid-app/src/main/java", "../../../retrograde-app-shared/src/main/java")
        kotlin.include(
            "**/gamemenu/GameMenuCapabilities.kt",
            "**/input/ControllerHardwareProfile.kt",
            "**/xbox/XboxPadState.kt", "**/xbox/XboxStorage.kt", "**/xbox/XboxEngineStore.kt",
            "**/xbox360/Xbox360Engine.kt", "**/xbox360/Xbox360Pad.kt", "**/xbox360/Xbox360Reports.kt", "**/xbox360/Xbox360Content.kt",
            "**/scanner/Xbox360ContentIdentity.kt", "**/scanner/NativeDiscFolderIdentity.kt",
            "**/library/SystemID.kt", "**/local/CachedRomStager.kt",
        )
    }
    test {
        kotlin.srcDir("../../../lemuroid-app/src/test/java")
        kotlin.include(
            "**/gamemenu/GameMenuCapabilitiesTest.kt", "**/xbox/XboxNativeContractTest.kt",
            "**/xbox/XboxEngineStoreTest.kt", "**/xbox360/*Test.kt",
        )
    }
}
tasks.test { testLogging { events("passed", "failed", "skipped") } }
