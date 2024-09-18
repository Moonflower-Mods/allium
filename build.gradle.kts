import java.util.*

plugins {
    id("maven-publish")
    id("fabric-loom") version "1.7-SNAPSHOT"
}
// Common Mod Properties
val mavenGroup: String by project

// Fabric Properties
val minecraftVersion: String by project
val yarnMappings: String by project
val loaderVersion: String by project

// Common Dependencies
val cobalt: String by project
val tinyParser: String by project
val enhancedReflections: String by project

// Bouquet Dependencies
val nettyHttp: String by project
val placeholderApi: String by project

dependencies {
    minecraft("com.mojang", "minecraft", minecraftVersion)
    mappings("net.fabricmc", "yarn", yarnMappings, null, "v2")
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "fabric-loom")

    repositories {
        maven("https://maven.hugeblank.dev/releases") {
            content {
                includeGroup("dev.hugeblank")
            }
        }
        maven("https://squiddev.cc/maven") {
            content {
                includeGroup("cc.tweaked")
            }
        }
        maven("https://maven.nucleoid.xyz") {
            content {
                includeGroup("eu.pb4")
            }
        }
        maven("https://basique.top/maven/releases") {
            content {
                includeGroup("me.basiqueevangelist")
            }
        }
    }

    dependencies {
        minecraft("com.mojang", "minecraft", minecraftVersion)
        mappings("net.fabricmc", "yarn", yarnMappings, null, "v2")
        modImplementation("net.fabricmc", "fabric-loader", loaderVersion)
    }

    tasks {
        processResources {
            inputs.property("version", project.version)

            filesMatching("fabric.mod.json") {
                expand(mutableMapOf("version" to project.version))
            }
        }

        jar {
            from("LICENSE") {
                rename { "${it}_${project.base.archivesName.get()}" }
            }
        }

        loom {
            val moduleName = project.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            runs {
                named("client") {
                    configName = "$moduleName Client"
                    ideConfigGenerated(true)
                    runDir("../run")
                    programArgs("-username", "GradleDev")
                }
                named("server") {
                    configName = "$moduleName Server"
                    ideConfigGenerated(true)
                    runDir("../run")
                }
            }
        }
    }

    publishing {
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
}

tasks {
    register<GradleBuild>("buildAll") {
        group = "build"
        tasks = subprojects.map { ":${it.name}:build" }
    }

    loom {
        runs {
            named("client") {
                ideConfigGenerated(false)
            }
            named("server") {
                ideConfigGenerated(false)
            }
        }
    }
}