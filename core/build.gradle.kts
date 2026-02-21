println("Using Gradle version: ${gradle.gradleVersion}")
println("Using Java compiler version: ${JavaVersion.current()}")

plugins {
    id("jayo-stages-commons")
}