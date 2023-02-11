import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm") version "1.8.0"
    idea
}

val projGroupId: String by project
val projArtifactId: String by project
val projName: String by project
val projVersion: String by project
val projVcs: String by project
val projBranch: String by project
val orgName: String by project
val orgUrl: String by project
val developers: String by project

val lwjglDepends = arrayOf("", "-glfw", "-opengl", "-stb", "-jemalloc")
val lwjglNatives =
    if (System.getProperty("project.bindAllNative").toBoolean())
        arrayOf(
            "linux-arm32", "linux-arm64", "linux",
            "macos-arm64", "macos",
            "windows-arm64", "windows-x86", "windows"
        )
    else System.getProperty("os.arch").let { osArch ->
        @Suppress("INACCESSIBLE_TYPE")
        when (OperatingSystem.current()) {
            OperatingSystem.LINUX -> arrayOf(
                if (osArch.startsWith("arm") || osArch.startsWith("aarch64"))
                    "linux-${if (osArch.contains("64") || osArch.startsWith("armv8")) "arm64" else "arm32"}"
                else "linux"
            )

            OperatingSystem.MAC_OS -> arrayOf(if (osArch.startsWith("aarch64")) "macos-arm64" else "macos")
            OperatingSystem.WINDOWS -> arrayOf(
                if (osArch.contains("64"))
                    "windows${if (osArch.startsWith("aarch64")) "-arm64" else ""}"
                else "windows-x86"
            )

            else -> {
                throw IllegalStateException("Unsupported system type ${OperatingSystem.current()}")
            }
        }
    }

group = projGroupId
version = projVersion

repositories {
    mavenCentral()
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    // temporary maven repositories
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases") }
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    api("org.joml:joml:1.10.5")
    api("org.slf4j:slf4j-api:2.0.6")
    api("io.github.over-run:bin-packing:0.2.0")

    compileOnly("org.jetbrains:annotations:24.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.6")

    api(platform("org.lwjgl:lwjgl-bom:3.3.1"))
    for (depend in lwjglDepends) {
        api("org.lwjgl:lwjgl$depend")
        for (platform in lwjglNatives) {
            testRuntimeOnly("org.lwjgl:lwjgl$depend::natives-$platform")
        }
    }
}

val targetJavaVersion = 17
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.named<Javadoc>("javadoc") {
    isFailOnError = false
    options {
        encoding = "UTF-8"
        locale = "en_US"
        windowTitle = "$projName $projVersion Javadoc"
        if (this is StandardJavadocDocletOptions) {
            charSet = "UTF-8"
            isAuthor = true
            links("https://docs.oracle.com/en/java/javase/$targetJavaVersion/docs/api/")
        }
    }
}

kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.named<Jar>("jar") {
    manifestContentCharset = "utf-8"
    setMetadataCharset("utf-8")
    from("LICENSE")
    manifest.attributes(
        "Specification-Title" to archiveBaseName,
        "Specification-Vendor" to orgName,
        "Specification-Version" to "0",
        "Implementation-Title" to archiveBaseName,
        "Implementation-Vendor" to orgName,
        "Implementation-Version" to archiveVersion
    )
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(tasks.named("classes"))
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource, "LICENSE")
}

tasks.named<Jar>("javadocJar") {
    val javadoc by tasks
    dependsOn(javadoc)
    archiveClassifier.set("javadoc")
    from(javadoc, "LICENSE")
}

tasks.withType<Jar> {
    archiveBaseName.set(projArtifactId)
}

artifacts {
    archives(tasks.named("javadocJar"))
    archives(tasks.named("sourcesJar"))
}

idea.module.inheritOutputDirs = true
