plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Paper 26.2 and platform-paper are compiled for Java 25; --release 21 cannot read them.
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked", "-parameters"))
}

configurations.runtimeClasspath {
    // Paper provides these. Jedis drags in slf4j-api 1.7.x, which inside the plugin jar would
    // shadow the server's 2.x and break logging.
    exclude(group = "com.google.code.gson")
    exclude(group = "org.slf4j", module = "slf4j-api")
    exclude(group = "net.kyori")
}

dependencies {
    compileOnly(libs.paper.api)

    implementation(libs.platform.api)
    implementation(libs.platform.common)
    implementation(libs.platform.config)
    implementation(libs.platform.database)
    implementation(libs.platform.messaging)
    implementation(libs.platform.paper)

    // The platform ships no JDBC driver on purpose; the plugin picks the one it uses.
    runtimeOnly(libs.h2)

    // PacketEvents installs itself as its own Paper plugin, so it is never shaded here.
    compileOnly(libs.packetevents.spigot)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.paper.api)
    testImplementation(libs.slf4j.api)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.slf4j.simple)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.processResources {
    // paper-plugin.yml is hand-written but takes its version from gradle.properties, so the
    // descriptor and the build cannot disagree.
    val properties = mapOf("version" to project.version)
    inputs.properties(properties)
    filesMatching("paper-plugin.yml") {
        expand(properties)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.shadowJar {
    archiveFileName = "landmc-lobby.jar"

    // Relocated: libraries another plugin on the same server might also shade at a different
    // version. Not relocated: pl.landmc.platform, which only LandMC plugins load, and nothing
    // Paper already provides - those are excluded above instead.
    val shaded = "pl.landmc.lobby.libs"
    listOf(
        "eu.okaeri",
        "dev.rollczi.litecommands",
        "com.eternalcode.multification",
        "com.zaxxer.hikari",
        "com.j256.ormlite",
        "redis.clients",
        "org.json",
        "org.apache.commons.pool2",
        "org.h2",
        "org.yaml.snakeyaml",
    ).forEach { relocate(it, "$shaded.$it") }

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/**/module-info.class")
    exclude("org/jetbrains/annotations/**", "org/intellij/lang/**")

    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
