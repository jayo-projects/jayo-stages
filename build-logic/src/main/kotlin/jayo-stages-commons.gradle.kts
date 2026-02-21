import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import kotlin.jvm.optionals.getOrNull

plugins {
    `java-library`
    `maven-publish`
}

val versionCatalog: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
fun catalogVersion(lib: String) =
    versionCatalog.findVersion(lib).getOrNull()?.requiredVersion
        ?: throw GradleException("Version '$lib' is not specified in the toml version catalog")

val javaVersion = catalogVersion("java").toInt()

repositories {
    mavenCentral()
}

dependencies {
    api("org.jspecify:jspecify:${catalogVersion("jspecify")}")

    testImplementation("org.junit.jupiter:junit-jupiter:${catalogVersion("junit")}")
    testImplementation("org.assertj:assertj-core:${catalogVersion("assertj")}")
    testImplementation("org.mockito:mockito-junit-jupiter:${catalogVersion("mockito")}")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${catalogVersion("junit")}")
    testRuntimeOnly("org.slf4j:slf4j-jdk-platform-logging:${catalogVersion("slf4j")}")
    testRuntimeOnly("ch.qos.logback:logback-classic:${catalogVersion("logback")}")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks {
    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    val testJavaVersion = System.getProperty("test.java.version", "").toIntOrNull()
    withType<Test> {
        if (testJavaVersion != null) {
            javaLauncher = javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(testJavaVersion)
            }
        }
        useJUnitPlatform()
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}
