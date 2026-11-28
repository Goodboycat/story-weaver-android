pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // Native Jitsi Meet SDK (voice room with phone screen share), see docs/VOICE_ROOM.md
    maven { url = uri("https://github.com/jitsi/jitsi-maven-repository/raw/master/releases") }
  }
}

rootProject.name = "Story Weaver"

include(":app")
