val mavenGroup: String by project

val alliumVersion: String by project
val alliumReleaseCandidate: String by project
val alliumBaseName: String by project

val tinyParser: String by project


// Following by example, using semantic versioning null
var v = alliumVersion
if ("0" != alliumReleaseCandidate) {
	v = "$v-rc$alliumReleaseCandidate"
}
version = v
group = mavenGroup

base {
	archivesName.set(alliumBaseName)
}

dependencies {
	modImplementation(include("net.fabricmc", "tiny-mappings-parser", tinyParser))
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

// configure the maven publication
publishing {
	publications {
		register("mavenJava", MavenPublication::class) {
			from(components["java"])
			groupId = mavenGroup
			artifactId = alliumBaseName
			version = version
		}
	}

	repositories {
		maven {
			name = "hugeblankRepo"
			url = uri("https://maven.hugeblank.dev/releases")
			credentials(PasswordCredentials::class)
			authentication {
				create<BasicAuthentication>("basic")
			}
		}
	}
}