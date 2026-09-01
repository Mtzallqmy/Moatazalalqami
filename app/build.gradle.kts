import com.android.build.api.dsl.Packaging
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val releaseSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.isFile) {
        propertiesFile.inputStream().use(::load)
    }
}

fun releaseSigningValue(propertyName: String, environmentName: String): String? =
    releaseSigningProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }
        ?: System.getenv(environmentName)?.takeIf { it.isNotBlank() }

val releaseStoreFilePath = releaseSigningValue("storeFile", "ANDROID_KEYSTORE_FILE")
val releaseStorePassword = releaseSigningValue("storePassword", "ANDROID_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("keyAlias", "ANDROID_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("keyPassword", "ANDROID_KEY_PASSWORD")

android {
    namespace = "me.rerere.rikkahub"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.moatazalaqami.agent"
        minSdk = 26
        targetSdk = 37
        versionCode = 30000
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }


    signingConfigs {
        create("release") {
            if (releaseStoreFilePath != null && releaseStorePassword != null &&
                releaseKeyAlias != null && releaseKeyPassword != null
            ) {
                storeFile = rootProject.file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = true
            }
            buildConfigField("String", "VERSION_NAME", "\"${android.defaultConfig.versionName}\"")
            buildConfigField("String", "VERSION_CODE", "\"${android.defaultConfig.versionCode}\"")
            buildConfigField("String", "UPDATE_API_URL", "\"\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "VERSION_NAME", "\"${android.defaultConfig.versionName}\"")
            buildConfigField("String", "VERSION_CODE", "\"${android.defaultConfig.versionCode}\"")
            buildConfigField("String", "UPDATE_API_URL", "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
    androidResources {
        generateLocaleConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += "lib/*/libtermux.so"
        }
    }
    lint {
        // Keep the release gate strict for new findings while the inherited localization
        // and Compose-style debt is reduced incrementally. Runtime/API/permission findings
        // discovered during this production hardening pass are fixed in source, not baselined.
        baseline = file("lint-baseline.xml")
        disable.add("FullBackupContent")
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        compilerOptions.optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        compilerOptions.optIn.add("androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi")
        compilerOptions.optIn.add("androidx.compose.animation.ExperimentalAnimationApi")
        compilerOptions.optIn.add("androidx.compose.animation.ExperimentalSharedTransitionApi")
        compilerOptions.optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
        compilerOptions.optIn.add("androidx.compose.foundation.layout.ExperimentalLayoutApi")
        compilerOptions.optIn.add("kotlin.uuid.ExperimentalUuidApi")
        compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
        compilerOptions.optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(
        project.layout.projectDirectory.file("compose_compiler_config.conf")
    )
}

// Moataz Alaqami 3.0: embed an aarch64 Alpine Linux minirootfs in every APK.
// Download happens only at build time. The installed app never needs to fetch a rootfs.
val alpineVersion = "3.24.1"
val embeddedLinuxDir = layout.buildDirectory.dir("generated/moatazLinux")
val prepareEmbeddedLinuxRootfs by tasks.registering {
    val outDir = embeddedLinuxDir
    outputs.dir(outDir)
    doLast {
        val dir = outDir.get().asFile.apply { mkdirs() }
        val archiveName = "alpine-minirootfs-$alpineVersion-aarch64.tar.gz"
        val base = "https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64"
        // aapt treats .gz as a packaging directive and strips that suffix from the asset
        // path. Keep the gzip bytes under a neutral name so Assets.open() is deterministic.
        val archive = File(dir, "linux-rootfs.tar.gz.bin")
        File(dir, "linux-rootfs.tar.gz").delete()
        val checksum = File(dir, "$archiveName.sha256")
        if (!archive.exists()) {
            URI("$base/$archiveName").toURL().openStream().use { input ->
                archive.outputStream().use { output -> input.copyTo(output) }
            }
        }
        URI("$base/$archiveName.sha256").toURL().openStream().use { input ->
            checksum.outputStream().use { output -> input.copyTo(output) }
        }
        val expected = checksum.readText().trim().substringBefore(' ')
        val digest = MessageDigest.getInstance("SHA-256")
        val actual = archive.inputStream().use { stream ->
            val buf = ByteArray(1024 * 128)
            while (true) {
                val count = stream.read(buf)
                if (count < 0) break
                digest.update(buf, 0, count)
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }
        check(actual.equals(expected, ignoreCase = true)) {
            "Embedded Linux rootfs checksum mismatch: expected=$expected actual=$actual"
        }
    }
}

android.sourceSets.getByName("main").assets.directories.add(embeddedLinuxDir.get().asFile.absolutePath)
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }
    .configureEach { dependsOn(prepareEmbeddedLinuxRootfs) }
// AGP's lint model reads every declared asset directory directly instead of going
// through merge*Assets, so it also needs the producer edge for Gradle 9 validation.
tasks.matching { it.name.contains("Lint", ignoreCase = true) }
    .configureEach { dependsOn(prepareEmbeddedLinuxRootfs) }

tasks.register("buildAll") {
    dependsOn("assembleRelease", "bundleRelease")
    description = "Build both APK and AAB"
}

val verifyReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fail fast when a release would be unsigned or use a missing keystore."
    doLast {
        val missing = buildList {
            if (releaseStoreFilePath == null) add("storeFile/ANDROID_KEYSTORE_FILE")
            if (releaseStorePassword == null) add("storePassword/ANDROID_STORE_PASSWORD")
            if (releaseKeyAlias == null) add("keyAlias/ANDROID_KEY_ALIAS")
            if (releaseKeyPassword == null) add("keyPassword/ANDROID_KEY_PASSWORD")
        }
        require(missing.isEmpty()) {
            "Release signing is mandatory. Missing: ${missing.joinToString()}"
        }
        require(rootProject.file(requireNotNull(releaseStoreFilePath)).isFile) {
            "Release keystore does not exist: $releaseStoreFilePath"
        }
    }
}

tasks.matching {
    it.name == "assembleRelease" || it.name == "bundleRelease" || it.name == "packageRelease"
}.configureEach {
    dependsOn(verifyReleaseSigning)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.webkit)
    implementation(libs.termux.terminal.view)
    implementation(libs.guava.listenablefuture)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.material3.adaptive.layout)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Image metadata extractor
    implementation(libs.metadata.extractor)

    // Haze
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.haze.blur.material3)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.androidx.workmanager)

    implementation(libs.jetbrains.markdown)

    // HTTP
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.ucrop)
    implementation(libs.pebble)
    implementation(libs.diffutils)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.coil.cache.control)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.zxing.core)
    implementation(libs.quickie.bundled)
    implementation(libs.barcode.scanning)
    implementation(libs.androidx.camera.core)

    // Room / Paging
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.commons.text)
    implementation(libs.sonner)
    implementation(libs.reorderable)
    implementation(libs.lucide.icons)
    implementation(libs.huge.icons)
    implementation(libs.image.viewer)

    implementation(libs.jlatexmath)
    implementation(libs.jlatexmath.font.greek)
    implementation(libs.jlatexmath.font.cyrillic)

    implementation(libs.modelcontextprotocol.kotlin.sdk)
    implementation(libs.jmdns)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.android)
    implementation(libs.sqlite.android)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.media)
    implementation(libs.androidx.documentfile)

    // Project modules
    implementation(project(":ai"))
    implementation(project(":local-llm"))
    implementation(project(":llama-cpp"))
    implementation(project(":web"))
    implementation(project(":document"))
    implementation(project(":highlight"))
    implementation(project(":search"))
    implementation(project(":speech"))
    implementation(project(":common"))
    implementation(project(":material3"))
    implementation(project(":workspace"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation(kotlin("reflect"))

    implementation(libs.jsch)
    implementation(libs.cron.utils)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
