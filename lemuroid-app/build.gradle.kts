import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlinx-serialization")
    id("androidx.baselineprofile")
    id("org.jetbrains.kotlin.plugin.compose")
}

// New hardware cores require the host's checked GLES negotiation and trigger protocol.
if (providers.gradleProperty("cartdeckNativeSystems").orNull?.isNotBlank() == true) {
    require(providers.gradleProperty("cartdeckLibretroDroidVersion").orNull == "0.14.0-cartdeck3") {
        "Native console APKs require the verified CARTDECK LibretroDroid host"
    }
}

android {
    defaultConfig {
        versionCode = 252
        versionName = "1.17.0" // Always remember to update Cores Tag!
        applicationId = "com.swordfish.lemuroid"

        // CARTDECK standalone preview: a distinct install from the inherited P0
        // test app, so old library data and saves remain untouched. This property
        // is used ONLY for an internal debug APK; it is not the public release ID
        // or permission to reuse inherited debug signing for production.
        if (providers.gradleProperty("cartdeck.dev.standaloneId").orNull == "true") {
            applicationId = "dev.prestwich.cartdeck"
        }

        if (providers.gradleProperty("cartdeck.ps2next.labId").orNull == "true") {
            applicationId = "dev.prestwich.cartdeck.ps2lab"
        }

        // P0 test-only overrides. No override means the original production configuration.
        // Keep the existing package ID and test-signing certificate between updates.
        providers.gradleProperty("cartdeck.p0.testVersionCode").orNull?.let { value ->
            val testCode = value.toIntOrNull()
            require(testCode != null && testCode > 252 && testCode <= 2100000000) {
                "cartdeck.p0.testVersionCode must be an integer above 252."
            }
            versionCode = testCode
        }
        if (providers.gradleProperty("cartdeck.p0.phoneArm64Only").orNull == "true") {
            ndk {
                abiFilters += "arm64-v8a"
            }
        }
    }
    flavorDimensions += listOf("opensource", "cores")

    if (usePlayDynamicFeatures()) {
        println("Building Google Play version. Bundling dynamic features.")
        dynamicFeatures.addAll(
            setOf(
                ":lemuroid_core_desmume",
                ":lemuroid_core_dosbox_pure",
                ":lemuroid_core_fbneo",
                ":lemuroid_core_fceumm",
                ":lemuroid_core_gambatte",
                ":lemuroid_core_genesis_plus_gx",
                ":lemuroid_core_handy",
                ":lemuroid_core_mame2003_plus",
                ":lemuroid_core_mednafen_ngp",
                ":lemuroid_core_mednafen_pce_fast",
                ":lemuroid_core_mednafen_wswan",
                ":lemuroid_core_melonds",
                ":lemuroid_core_mgba",
                ":lemuroid_core_mupen64plus_next_gles3",
                ":lemuroid_core_pcsx_rearmed",
                ":lemuroid_core_ppsspp",
                ":lemuroid_core_prosystem",
                ":lemuroid_core_snes9x",
                ":lemuroid_core_stella",
                ":lemuroid_core_citra",
            ),
        )
    }

    // Since some dependencies are closed source we make a completely free as in free speech variant.

    productFlavors {

        create("free") {
            dimension = "opensource"
        }

        create("play") {
            dimension = "opensource"
        }

        // Include cores in the final apk
        create("bundle") {
            dimension = "cores"
        }

        // Download cores on demand (from GooglePlay or GitHub)
        create("dynamic") {
            dimension = "cores"
        }
    }

    providers.gradleProperty("cartdeckNativeBundleDir").orNull?.let { bundle ->
        sourceSets.getByName("main") {
            jniLibs.srcDir("$bundle/jniLibs")
            assets.srcDir("$bundle/assets")
        }
    }

    packagingOptions {
        jniLibs {
            // Stripping created some issues with some libretro cores such as ppsspp
            keepDebugSymbols +=
                setOf(
                    "*/*/*_libretro_android.so", "**/liblibretrodroid.so",
                    "**/libhakuxcore.so", "**/libSDL3.so", "**/*hook*.so",
                )
            useLegacyPackaging = true
            if (providers.gradleProperty("cartdeck.ps2next.labId").orNull == "true") {
                // Private ARMSX2 APK verification binds every packaged ELF byte
                // to its source-build receipt, including crash symbols.
                keepDebugSymbols += "**/*.so"
            }
        }
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/library_release.kotlin_module")
        }
    }

    signingConfigs {
        maybeCreate("debug").apply {
            storeFile = file("$rootDir/debug.keystore")
        }

        maybeCreate("release").apply {
            storeFile = file("$rootDir/release.jks")
            keyAlias = "lemuroid"
            storePassword = "lemuroid"
            keyPassword = "lemuroid"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs["release"]
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            resValue("string", "lemuroid_name", "CARTDECK")
            resValue("string", "cartdeck_name", "CARTDECK")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            resValue("string", "lemuroid_name", "CARTDECK Debug")
            resValue("string", "cartdeck_name", "CARTDECK Debug")
        }
    }

    lint {
        disable += setOf("MissingTranslation", "ExtraTranslation", "EnsureInitializerMetadata")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { testTask ->
            testTask.maxHeapSize = "1536m"
            // Robolectric resolves Android SDK jars inside the forked JVM.
            // Preserve configured network/CA trust settings without disabling TLS.
            listOf(
                "http.proxyHost",
                "http.proxyPort",
                "http.nonProxyHosts",
                "https.proxyHost",
                "https.proxyPort",
                "https.nonProxyHosts",
                "javax.net.ssl.trustStore",
                "javax.net.ssl.trustStoreType",
                "maven.repo.local",
            ).forEach { property ->
                System.getProperty(property)?.let { value -> testTask.systemProperty(property, value) }
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = deps.versions.kotlinExtension
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    namespace = "com.swordfish.lemuroid"
}

// ASSET-H1 — generated, compile-time Android resource source set.
// Editable SVGs/edition JSON in design/ are the only design masters.
// The app does not use these new Drawables until separate P1 screen PRs.
abstract class GenerateCartdeckNativeAssets : Exec() {
    @get:OutputDirectory
    abstract val resourceDirectory: DirectoryProperty
}

androidComponents.onVariants { variant ->
    val capitalizedVariant = variant.name.replaceFirstChar { it.uppercase() }
    val generator =
        tasks.register<GenerateCartdeckNativeAssets>("generate${capitalizedVariant}CartdeckNativeAssets") {
            group = "cartdeck"
            description = "Generate Original/Pink/Green vectors and semantic colours for ${variant.name}"
            workingDir = rootProject.projectDir
            val stagingResources = layout.buildDirectory.dir("generated/cartdeck-native/${variant.name}/res")
            commandLine(
                providers.gradleProperty("cartdeck.assets.python").orElse("python3").get(),
                "scripts/generate_cartdeck_native_assets.py",
                "--output",
                stagingResources.get().asFile.absolutePath,
            )
            outputs.dir(layout.buildDirectory.dir("generated/cartdeck-native/${variant.name}"))
            // Keep the generator's dedicated /res safety guard while using AGP's managed output path.
            doLast {
                project.sync {
                    from(stagingResources)
                    into(resourceDirectory)
                }
            }
            inputs.file(rootProject.file("scripts/generate_cartdeck_native_assets.py"))
            inputs.files(
                fileTree(rootProject.file("design/cartdeck-v03")) { include("**/*.svg", "**/*.json", "**/*.py") },
                fileTree(rootProject.file("design/cartdeck-v04")) { include("**/*.svg", "**/*.json", "**/*.py") },
                fileTree(rootProject.file("design/cartdeck-v05")) { include("**/*.svg", "**/*.json", "**/*.py") },
            )
        }
    variant.sources.res?.addGeneratedSourceDirectory(generator) { it.resourceDirectory }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(platform(deps.libs.androidx.compose.composeBom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test:core:1.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation(project(":retrograde-util"))
    implementation(project(":retrograde-app-shared"))
    implementation(project(":lemuroid-metadata-libretro-db"))
    implementation(project(":lemuroid-touchinput"))

    "baselineProfile"(project(":baselineprofile"))
    implementation(deps.libs.androidx.profileInstaller)

    "bundleImplementation"(project(":bundled-cores"))

    "freeImplementation"(project(":lemuroid-app-ext-free"))
    "playImplementation"(project(":lemuroid-app-ext-play"))

    implementation(deps.libs.androidx.navigation.navigationFragment)
    implementation(deps.libs.androidx.navigation.navigationUi)
    implementation(deps.libs.androidx.navigation.compose)
    implementation(deps.libs.material)
    implementation(deps.libs.coil.coil)
    implementation(deps.libs.coil.coilCompose)
    implementation(deps.libs.androidx.appcompat.constraintLayout)
    implementation(deps.libs.androidx.activity.activity)
    implementation(deps.libs.androidx.activity.activityKtx)
    implementation(deps.libs.androidx.activity.compose)
    implementation(deps.libs.androidx.appcompat.appcompat)
    implementation(deps.libs.androidx.preferences.preferencesKtx)
    implementation(deps.libs.arch.work.runtime)
    implementation(deps.libs.arch.work.runtimeKtx)
    implementation(deps.libs.androidx.lifecycle.commonJava8)
    implementation(deps.libs.androidx.lifecycle.reactiveStreams)

    kapt(deps.libs.androidx.lifecycle.processor)

    implementation(deps.libs.androidx.leanback.leanback)
    implementation(deps.libs.androidx.leanback.leanbackPreference)
    implementation(deps.libs.androidx.leanback.leanbackPaging)

    implementation(deps.libs.androidx.appcompat.recyclerView)
    implementation(deps.libs.androidx.paging.common)
    implementation(deps.libs.androidx.paging.runtime)
    implementation(deps.libs.androidx.room.common)
    implementation(deps.libs.androidx.room.runtime)
    implementation(deps.libs.androidx.room.ktx)
    implementation(deps.libs.dagger.android.core)
    implementation(deps.libs.dagger.android.support)
    implementation(deps.libs.dagger.core)
    implementation(deps.libs.kotlinxCoroutinesAndroid)
    implementation(deps.libs.okHttp3)
    implementation(deps.libs.okio)
    implementation(deps.libs.retrofit)
    implementation(deps.libs.flowPreferences)
    implementation(deps.libs.guava)
    implementation(deps.libs.androidx.documentfile)
    implementation(deps.libs.androidx.leanback.tvProvider)
    implementation(deps.libs.harmony)
    implementation(deps.libs.startup)
    implementation(deps.libs.kotlin.serialization)
    implementation(deps.libs.kotlin.serializationJson)

    implementation(platform(deps.libs.androidx.compose.composeBom))
    implementation(deps.libs.androidx.compose.material3)
    implementation(deps.libs.androidx.compose.constraintLayout)
    debugImplementation(deps.libs.androidx.compose.tooling)
    implementation(deps.libs.androidx.compose.toolingPreview)
    implementation(deps.libs.androidx.compose.extendedIcons)
    implementation(deps.libs.androidx.compose.accompanist.systemUiController)
    implementation(deps.libs.androidx.compose.accompanist.navigationMaterial)
    implementation(deps.libs.androidx.compose.accompanist.drawablePainter)
    implementation(deps.libs.androidx.paging.compose)
    implementation(deps.libs.androidx.lifecycle.viewModelCompose)
    implementation(deps.libs.composeHtmlText)

    implementation(deps.libs.composeSettings.uiTiles)
    implementation(deps.libs.composeSettings.uiTilesExtended)
    implementation(deps.libs.composeSettings.diskStorage)
    implementation(deps.libs.composeSettings.memoryStorage)

    val libretroDroidVersion =
        providers.gradleProperty("cartdeckLibretroDroidVersion")
            .orElse(deps.versions.libretrodroid)
            .get()
    implementation("com.github.Swordfish90:LibretroDroid:$libretroDroidVersion")

    // Uncomment this when using a local aar file.
    // implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    kapt(deps.libs.dagger.android.processor)
    kapt(deps.libs.dagger.compiler)
}

fun usePlayDynamicFeatures(): Boolean {
    val task = gradle.startParameter.taskRequests.toString()
    return task.contains("Play") && task.contains("Dynamic")
}
