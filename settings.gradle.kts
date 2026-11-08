pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // TODO remove before release: ru.kode:unicorn2:2.3.0 is not on Maven Central yet
    mavenLocal()
  }
}

rootProject.name = "amvi"

include(":amvi-viewmodel")
include(":amvi-component-compose")
