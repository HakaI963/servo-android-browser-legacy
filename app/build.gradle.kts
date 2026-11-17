import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.servodroid.browser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.servodroid.browser"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // The Servo engine is a prebuilt shared object. CI builds it for each ABI and drops it
    // into src/main/jniLibs/<abi>/ before assembleRelease runs.
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // CI builds are unsigned developer releases. Production builds should supply a
            // real keystore via the standard signingConfigs mechanism.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // libservoshell.so depends on libc++_shared.so shipped by the NDK. Keeping the
            // uncompressed native libs avoids extraction at install time on modern devices.
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.documentfile)
    debugImplementation(libs.androidx.ui.tooling)
}

/**
 * Optional: download a prebuilt Servo engine (.so or .aar) from `servo.engine.url` in
 * gradle.properties. When the property is empty the CI source build supplies the engine.
 */
abstract class FetchServoEngineTask : DefaultTask() {
    @get:Input
    abstract val engineUrl: Property<String>

    @get:OutputDirectory
    abstract val jniLibsDir: DirectoryProperty

    @TaskAction
    fun fetch() {
        val url = engineUrl.get().trim()
        if (url.isEmpty()) {
            logger.lifecycle("servo.engine.url is empty - using engine from jniLibs or CI source build.")
            return
        }
        val out = jniLibsDir.get().asFile
        out.mkdirs()
        val archive = File(project.buildDir, "servo-engine-download")
        archive.mkdirs()

        val fileName = url.substringAfterLast('/').substringBefore('?')
        val target = File(archive, fileName)
        logger.lifecycle("Fetching Servo engine from $url")
        URL(url).openStream().use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
            }
        }

        if (fileName.endsWith(".aar", ignoreCase = true)) {
            val abiMap = mapOf(
                "arm64-v8a" to "arm64-v8a",
                "armeabi-v7a" to "armeabi-v7a",
                "x86" to "x86",
                "x86_64" to "x86_64",
            )
            ZipFile(target).use { zip ->
                zip.entries().asSequence()
                    .filter { it.name.startsWith("jni/") && it.name.endsWith("libservoshell.so") }
                    .forEach { entry ->
                        val abi = entry.name.split('/').getOrNull(1)
                        val destAbi = abiMap[abi]
                        if (destAbi != null) {
                            val destDir = File(out, destAbi).also { it.mkdirs() }
                            val dest = File(destDir, "libservoshell.so")
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(dest).use { output -> input.copyTo(output) }
                            }
                            logger.lifecycle("  -> ${dest.relativeTo(project.rootDir)}")
                        }
                    }
            }
        } else if (fileName.endsWith(".so", ignoreCase = true)) {
            val destDir = File(out, "arm64-v8a").also { it.mkdirs() }
            target.copyTo(File(destDir, "libservoshell.so"), overwrite = true)
        } else {
            throw GradleException("Unsupported Servo engine artifact: $fileName")
        }
    }
}

val fetchServoEngine by tasks.registering(FetchServoEngineTask::class) {
    engineUrl.set(providers.gradleProperty("servo.engine.url").orElse(""))
    jniLibsDir.set(layout.projectDirectory.dir("src/main/jniLibs"))
    group = "servo"
    description = "Downloads a prebuilt Servo engine binary when servo.engine.url is set."
}

tasks.named("preBuild") {
    dependsOn(fetchServoEngine)
}
