// settings.gradle.kts  (project root)

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Required for MPAndroidChart
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "YourGymApp"
include(":app")
