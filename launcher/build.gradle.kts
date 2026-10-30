plugins {
    application
    id("org.graalvm.buildtools.native") version "0.11.5"
}

val graalvmVersion: String by project

dependencies {
    implementation("org.graalvm.polyglot:polyglot:$graalvmVersion")
    runtimeOnly(project(":language"))
    runtimeOnly("org.graalvm.truffle:truffle-runtime:$graalvmVersion")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

application {
    mainClass = "org.graalnix.launcher.Main"
    applicationName = "graalnix"
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--sun-misc-unsafe-memory-access=allow",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+EnableJVMCI",
    )
}

graalvmNative {
    toolchainDetection = false
    testSupport = false
    metadataRepository {
        enabled = false
    }
    binaries.named("main") {
        imageName = "graalnix"
        buildArgs.addAll("-R:StackSize=512m", "--static", "--libc=musl")
    }
}
