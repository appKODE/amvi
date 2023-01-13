plugins {
  kotlin("jvm")
  id(libs.plugins.composeDesktop.get().pluginId)
}

kotlin {
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

dependencies {
  implementation(compose.desktop.currentOs)
}
