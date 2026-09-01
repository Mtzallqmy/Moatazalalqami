import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    id("rikkahub.android.library")
}

val webUiDir = rootProject.layout.projectDirectory.dir("web-ui")
val webStaticResourcesDir = layout.projectDirectory.dir("src/main/resources/static")

// Install exactly the dependency graph recorded in the tracked pnpm lockfile.
// Keeping one package manager in the production path avoids lockfile migration
// and makes local and CI builds deterministic.
val installWebUiDeps = tasks.register<Exec>("installWebUiDeps") {
    group = "build"
    description = "Install locked web-ui dependencies via pnpm."

    workingDir = webUiDir.asFile
    commandLine("pnpm", "install", "--frozen-lockfile")

    inputs.files(
        webUiDir.file("package.json"),
        webUiDir.file("pnpm-lock.yaml")
    )
    outputs.dir(webUiDir.dir("node_modules"))
}

val buildWebUi = tasks.register<Exec>("buildWebUi") {
    group = "build"
    description = "Build web-ui and copy its static output into the web module resources."

    dependsOn(installWebUiDeps)

    workingDir = webUiDir.asFile
    when {
        Os.isFamily(Os.FAMILY_MAC) -> commandLine("zsh", "-ic", "pnpm run build")
        Os.isFamily(Os.FAMILY_WINDOWS) -> commandLine("cmd", "/c", "pnpm run build")
        else -> commandLine("pnpm", "run", "build")
    }

    inputs.files(
        webUiDir.file("package.json"),
        webUiDir.file("pnpm-lock.yaml"),
        webUiDir.file("components.json"),
        webUiDir.file("copy.mjs"),
        webUiDir.file("react-router.config.ts"),
        webUiDir.file("tsconfig.json"),
        webUiDir.file("vite.config.ts"),
        webUiDir.file("vite-env.d.ts")
    )
    inputs.dir(webUiDir.dir("app"))
    inputs.dir(webUiDir.dir("public"))
    outputs.dir(webStaticResourcesDir)
}

android {
    namespace = "me.rerere.rikkahub.web"
}

tasks.named("preBuild") {
    dependsOn(buildWebUi)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // ktor server
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.conditional.headers)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.cors)
    api(libs.ktor.server.auth)
    api(libs.ktor.server.auth.jwt)
    api(libs.ktor.server.core)
    implementation(libs.ktor.server.host.common)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.sse)
    api(libs.ktor.server.cio)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
