plugins {
  kotlin("jvm")
}

kotlin {
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

dependencies {
  implementation(libs.kotlinCoroutinesCore)
  compileOnly(libs.composeRuntimeJvm) // for @Stable annotations
  api(libs.unicorn)

  testImplementation(libs.koTestRunner)
  testImplementation(libs.koTestAssertions)
  testImplementation(libs.koTestProperty)
  testImplementation(libs.turbine)
}
