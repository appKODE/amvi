import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlinMultiplatform.get().pluginId)
  id(libs.plugins.androidKmpLibrary.get().pluginId)
  alias(libs.plugins.kotlinCompose)
  alias(libs.plugins.vanniktech.maven.publish)
}

group = providers.gradleProperty("pomGroupId").get()
version = providers.gradleProperty("versionName").get()

kotlin {
  jvmToolchain(11)

  jvm()

  androidLibrary {
    namespace = "ru.kode.amvi.component.compose"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = 21
  }

  iosArm64()
  iosSimulatorArm64()
  iosX64()

  sourceSets {
    commonMain.dependencies {
      // Compose Multiplatform; on Android it resolves to the androidx artifact below
      api(libs.cmpRuntime)
      api(project(":amvi-viewmodel"))
    }
    androidMain.dependencies {
      api(libs.composeRuntime)
    }
    jvmTest.dependencies {
      implementation(libs.bundles.koTestCommon)
      implementation(libs.bundles.koTestJvm)
    }
  }
}

mavenPublishing {
  coordinates(artifactId = "amvi-component-compose")
  configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty()))

  publishToMavenCentral()
  signAllPublications()

  pom {
    val pomName: String by project
    val pomDescription: String by project
    val pomUrl: String by project
    val pomScmUrl: String by project
    val pomScmConnection: String by project
    val pomScmDevConnection: String by project
    val pomLicenseName: String by project
    val pomLicenseUrl: String by project
    val pomLicenseDist: String by project
    val pomDeveloperId: String by project
    val pomDeveloperName: String by project

    name.set(pomName)
    description.set(pomDescription)
    url.set(pomUrl)

    scm {
      url.set(pomScmUrl)
      connection.set(pomScmConnection)
      developerConnection.set(pomScmDevConnection)
    }

    licenses {
      license {
        name.set(pomLicenseName)
        url.set(pomLicenseUrl)
        distribution.set(pomLicenseDist)
      }
    }

    developers {
      developer {
        id.set(pomDeveloperId)
        name.set(pomDeveloperName)
      }
    }
  }
}

tasks.named<Test>("jvmTest") {
  useJUnitPlatform()
}
