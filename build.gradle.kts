import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

plugins {
  kotlin("jvm") version "1.7.20" apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.composeDesktop) apply false
}

allprojects {
  buildscript {
    repositories {
      google()
      mavenCentral()
    }
  }
  repositories {
    google()
    mavenCentral()
    mavenLocal()
  }
}

subprojects {
  plugins.withType<KotlinPluginWrapper> {
    apply(plugin = libs.plugins.dokka.get().pluginId)
    apply(plugin = "maven-publish")

    val dokkaHtml by tasks.existing(DokkaTask::class)

    val dokkaJar by tasks.creating(org.gradle.jvm.tasks.Jar::class) {
      group = JavaBasePlugin.DOCUMENTATION_GROUP
      archiveClassifier.set("javadoc")
      from(dokkaHtml)
    }

    val sourcesJar by tasks.creating(org.gradle.jvm.tasks.Jar::class) {
      archiveClassifier.set("sources")
      from(project.the<SourceSetContainer>()["main"].allSource)
    }

    configure<PublishingExtension> {
      publications.withType<MavenPublication> {
        artifact(dokkaJar)
        artifact(sourcesJar)
      }
    }
  }

  plugins.withType<MavenPublishPlugin> {
    apply(plugin = "org.gradle.signing")

    configure<PublishingExtension> {
      repositories {
        mavenLocal()
        maven {
          name = "MavenCentral"
          val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
          val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
          val versionName: String by project
          url = if (versionName.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
          credentials {
            username = project.findProperty("NEXUS_USERNAME")?.toString()
            password = project.findProperty("NEXUS_PASSWORD")?.toString()
          }
        }
      }

      publications.create<MavenPublication>("maven") {
        val versionName: String by project
        val pomGroupId: String by project
        groupId = pomGroupId
        version = versionName

        from(components["java"])

        pom {
          val pomDescription: String by project
          val pomUrl: String by project
          val pomName: String by project
          description.set(pomDescription)
          url.set(pomUrl)
          name.set(pomName)
          scm {
            val pomScmUrl: String by project
            val pomScmConnection: String by project
            val pomScmDevConnection: String by project
            url.set(pomScmUrl)
            connection.set(pomScmConnection)
            developerConnection.set(pomScmDevConnection)
          }
          licenses {
            license {
              val pomLicenseName: String by project
              val pomLicenseUrl: String by project
              val pomLicenseDist: String by project
              name.set(pomLicenseName)
              url.set(pomLicenseUrl)
              distribution.set(pomLicenseDist)
            }
          }
          developers {
            developer {
              val pomDeveloperId: String by project
              val pomDeveloperName: String by project
              id.set(pomDeveloperId)
              name.set(pomDeveloperName)
            }
          }
        }
      }

      configure<SigningExtension> {
        sign(publications)
      }
    }
  }
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("!**/build/**/*.*")
    ktlint(libs.versions.ktlint.get()).userData(mapOf("indent_size" to "2", "max_line_length" to "120"))
    trimTrailingWhitespace()
    endWithNewline()
  }

  kotlinGradle {
    target("**/*.gradle.kts")
    ktlint(libs.versions.ktlint.get()).userData(mapOf("indent_size" to "2", "max_line_length" to "120"))
    trimTrailingWhitespace()
    endWithNewline()
  }
}
