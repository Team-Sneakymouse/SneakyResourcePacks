import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("jvm") version "2.3.20"
	`maven-publish`
}

group = "io.github.team-sneakymouse"

version =
	providers.exec {
		workingDir(rootDir)
		commandLine("git", "show", "-s", "--format=%ct:%h", "--abbrev=12", "HEAD")
	}.standardOutput.asText.map { commit ->
		val (timestamp, hash) = commit.trim().split(":", limit = 2)
		val date =
			DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC)
				.format(Instant.ofEpochSecond(timestamp.toLong()))
		"$date-$hash"
	}.get()

repositories {
	maven {
		url = uri("https://plugins.gradle.org/m2/")
	}
	maven {
		url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
	}
	maven {
		url = uri("https://repo.mikeprimm.com/")
	}
	mavenCentral()
	maven("https://repo.papermc.io/repository/maven-public/")
	maven("https://maven.sneakyrp.com/releases")
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.20")
	compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
	compileOnly("io.github.team-sneakymouse:sneakycharactermanager-paper:2026.10.09-84db8a554af1") {
		isTransitive = false
	}
	// compileOnly("me.clip:placeholderapi:2.11.5")
	// compileOnly("us.dynmap:dynmap-api:3.4-beta-3")
	// compileOnly("us.dynmap:DynmapCoreAPI:3.4")
	// implementation("com.squareup.okhttp3:okhttp:4.12.0")
	// implementation("org.json:json:20240303")
}

kotlin {
	jvmToolchain(25)
}

java {
	withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_25)
	}
}

tasks.processResources {
	inputs.property("version", project.version.toString())
	filesMatching("paper-plugin.yml") {
		expand("version" to project.version.toString())
	}
}

tasks.jar {
	archiveBaseName.set(rootProject.name)
	archiveFileName.set("${rootProject.name}-${project.version}.jar")
	manifest {
		attributes["Main-Class"] = "com.danidipp.sneakyresourcepacks.SneakyResourcePacks"
	}

	from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

sourceSets {
	main {
		java.srcDir("src/main/kotlin")
		resources.srcDir(file("src/resources"))
	}
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			artifactId = rootProject.name
			from(components["java"])
			pom {
				name.set("SneakyResourcePacks")
				description.set("Paper plugin for managing multiple server resourcepacks")
				url.set("https://github.com/Team-Sneakymouse/SneakyResourcePacks")
				scm {
					url.set("https://github.com/Team-Sneakymouse/SneakyResourcePacks")
					connection.set("scm:git:https://github.com/Team-Sneakymouse/SneakyResourcePacks.git")
				}
			}
		}
	}
	repositories {
		maven {
			name = "sneakyrp"
			url = uri("https://maven.sneakyrp.com/releases")
			credentials(PasswordCredentials::class)
			authentication {
				create<org.gradle.authentication.http.BasicAuthentication>("basic")
			}
		}
	}
}

tasks.withType<PublishToMavenRepository>().configureEach {
	dependsOn(tasks.check)
}
