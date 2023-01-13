
rootProject.name = "amvi"

include("amvi-viewmodel")
include("amvi-component-compose")

pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}
