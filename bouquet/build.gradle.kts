val mavenGroup: String by project

val bouquetVersion: String by project
val bouquetReleaseCandidate: String by project
val bouquetBaseName: String by project

// Bouquet Dependencies
val nettyHttp: String by project
val placeholderApi: String by project

version = bouquetVersion
group = mavenGroup

base {
	archivesName.set(bouquetBaseName)
}

loom {
	splitEnvironmentSourceSets()
	mods {
		register("bouquet") {
			sourceSet(sourceSets["main"])
			sourceSet(sourceSets["client"])
		}
	}

}

dependencies {
	modImplementation(include("io.netty", "netty-codec-http", nettyHttp))
	modImplementation("eu.pb4", "placeholder-api", placeholderApi)
	implementation(project(path = ":allium", configuration = "namedElements"))
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

publishing {
	publications {
		register("mavenJava", MavenPublication::class) {
			from(components["java"])
			groupId = mavenGroup
			artifactId = bouquetBaseName
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