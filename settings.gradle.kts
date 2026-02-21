pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "jayo-stages-root"

include(":jayo-stages")

project(":jayo-stages").projectDir = file("./core")