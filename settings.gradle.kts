enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "findroid-plus"

include(":app")
include(":core")
include(":data")
include(":player:core")
include(":player:local")
include(":setup")
include(":feature:film")
include(":settings")
include(":ui")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}