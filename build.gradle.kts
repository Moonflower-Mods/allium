plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
    id("maven-publish")
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

allprojects {
    apply(plugin = "fabric-loom")
    apply(plugin = "maven-publish")

    repositories {
        maven("https://maven.hugeblank.dev/releases") {
            content {
                includeGroup("dev.hugeblank")
            }
        }
        maven("https://squiddev.cc/maven") {
            content {
                includeGroup("org.squiddev")
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

        modImplementation(include("org.squiddev", "Cobalt", cobalt))
        modImplementation(include("me.basiqueevangelist","enhanced-reflection", enhancedReflections))
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
    }
}

dependencies {
    implementation("net.fabricmc", "tiny-mappings-parser", tinyParser)
    implementation("io.netty", "netty-codec-http", nettyHttp)
    implementation("eu.pb4", "placeholder-api", placeholderApi)
}

evaluationDependsOnChildren()

tasks {
    loom {
        val loomRootDir = project.projectDir.relativeTo(project.rootDir)

        runs {
            named("client") {
                client()
                configName = "Moonflower Dev Client"
                ideConfigGenerated(true)
                runDir(loomRootDir.resolve("run").toString())
                programArgs("-username", "GradleDev")
                dependencies {
                    implementation(project(path = ":allium", configuration = "namedElements"))
                    implementation(project(path = ":bouquet", configuration = "namedElements"))
                }
            }
            register("allium") {
                client()
                configName = "Allium Only"
                ideConfigGenerated(true)
                runDir(loomRootDir.resolve("run").toString())
                programArgs("-username", "GradleDev")
                dependencies {
                    implementation(project(path = ":allium", configuration = "namedElements"))
                }
            }
        }
    }
}