plugins {
  kotlin("jvm")
}

kotlin {
}

dependencies {
  implementation(libs.kotlinCoroutinesCore)
  compileOnly(libs.composeRuntimeJvm) // for @Stable annotations
  implementation(libs.unicorn)
}
