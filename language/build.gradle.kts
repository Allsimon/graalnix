plugins {
    `java-library`
}

val graalvmVersion: String by project

dependencies {
    api("org.graalvm.truffle:truffle-api:$graalvmVersion")
    annotationProcessor("org.graalvm.truffle:truffle-dsl-processor:$graalvmVersion")

    testImplementation("org.graalvm.polyglot:polyglot:$graalvmVersion")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Xss512m")
}

tasks.test {
    environment("TEST_VAR", "foo")
    environment("HOME", "/fake-home")
    environment("NIX_PATH", "")
    jvmArgs("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
    systemProperty("graalnix.tests", rootProject.file("tests").absolutePath)
}
