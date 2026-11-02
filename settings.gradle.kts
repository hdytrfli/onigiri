enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "onigiri"

include(":app")
include(":core")
project(":core").projectDir = file("app/modules/core")
include(":data")
project(":data").projectDir = file("app/modules/data")
include(":player")
project(":player").projectDir = file("app/modules/player")
include(":player:core")
project(":player:core").projectDir = file("app/modules/player/core")
include(":player:local")
project(":player:local").projectDir = file("app/modules/player/local")
include(":setup")
project(":setup").projectDir = file("app/modules/setup")
include(":feature")
project(":feature").projectDir = file("app/modules/feature")
include(":feature:film")
project(":feature:film").projectDir = file("app/modules/feature/film")
include(":settings")
project(":settings").projectDir = file("app/modules/settings")
include(":ui")
project(":ui").projectDir = file("app/modules/ui")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}