import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.RunHandler
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.SessionHandler

plugins {
    java
    `maven-publish`
    id("com.palantir.git-version") version "0.12.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1" // Generates plugin.yml
    id("org.hidetake.ssh") version "2.10.1"
    id("net.ltgt.errorprone") version "2.0.2"
    id("net.ltgt.nullaway") version "1.3.0"
    checkstyle
    pmd
}

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        url = uri("mvn-repo")
    }

    maven {
        url = uri("https://maven.pkg.github.com/TeamMonumenta/scripted-quests/")
    }

    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    // Adventure API needed by FAWE
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/scripted-quests/master/mvn-repo/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-api:4.11.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.11.0")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.4.4")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.4.4") { isTransitive = false }
    compileOnly("com.bergerkiller.bukkit:LightCleaner:1.15.2-v1")
    compileOnly("com.bergerkiller.bukkit:BKCommonLib:1.15.2-v2")
    compileOnly("dev.jorel.CommandAPI:commandapi-core:6.0.0")
    compileOnly("com.google.code.gson:gson:2.8.5")
    compileOnly("com.playmonumenta:scripted-quests:5.0")

    errorprone("com.google.errorprone:error_prone_core:2.10.0")
    errorprone("com.uber.nullaway:nullaway:0.9.5")
}

group = "com.playmonumenta"
val gitVersion: groovy.lang.Closure<String> by extra
version = gitVersion()
description = "MonumentaStructureManagement"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

// Configure plugin.yml generation
bukkit {
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "com.playmonumenta.structures.StructuresPlugin"
    apiVersion = "1.16"
    name = "MonumentaStructureManagement"
    authors = listOf("The Monumenta Team")
    depend = listOf("CommandAPI", "ScriptedQuests", "MonumentaRedisSync")
    softDepend = listOf("LightCleaner")
}

publishing {
    publications.create<MavenPublication>("maven") {
        project.shadow.component(this)
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/TeamMonumenta/monumenta-structure-management")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xmaxwarns")
    options.compilerArgs.add("10000")

    // TODO: Also need to re-enable these deprecation warnings
    //options.compilerArgs.add("-Xlint:deprecation")

    options.errorprone {
        // TODO This must be turned back on as soon as some of the other warnings are under control
        option("NullAway:AnnotatedPackages", "com.playmonumenta")

        allErrorsAsWarnings.set(true)

        /*** Disabled checks ***/
        // These we almost certainly don't want
        check("CatchAndPrintStackTrace", CheckSeverity.OFF) // This is the primary way a lot of exceptions are handled
        check("FutureReturnValueIgnored", CheckSeverity.OFF) // This one is dumb and doesn't let you check return values with .whenComplete()
        check("ImmutableEnumChecker", CheckSeverity.OFF) // Would like to turn this on but we'd have to annotate a bunch of base classes
        check("LockNotBeforeTry", CheckSeverity.OFF) // Very few locks in our code, those that we have are simple and refactoring like this would be ugly
        check("StaticAssignmentInConstructor", CheckSeverity.OFF) // We have tons of these on purpose
        check("StringSplitter", CheckSeverity.OFF) // We have a lot of string splits too which are fine for this use
        check("MutablePublicArray", CheckSeverity.OFF) // These are bad practice but annoying to refactor and low risk of actual bugs
    }
}

val basicssh = remotes.create("basicssh") {
    host = "admin-eu.playmonumenta.com"
    port = 8822
    user = "epic"
    knownHosts = allowAnyHosts
    agent = System.getenv("IDENTITY_FILE") == null
    identity = if (System.getenv("IDENTITY_FILE") == null) null else file(System.getenv("IDENTITY_FILE"))
}

val adminssh = remotes.create("adminssh") {
    host = "admin-eu.playmonumenta.com"
    port = 9922
    user = "epic"
    knownHosts = allowAnyHosts
    agent = System.getenv("IDENTITY_FILE") == null
    identity = if (System.getenv("IDENTITY_FILE") == null) null else file(System.getenv("IDENTITY_FILE"))
}

tasks.create("stage-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(basicssh) {
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/stage/m12/server_config/plugins")
                execute("cd /home/epic/stage/m12/server_config/plugins && rm -f MonumentaStructureManagement.jar && ln -s " + shadowJar.archiveFileName.get() + " MonumentaStructureManagement.jar")
            }
        }
    }
}

tasks.create("build-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(adminssh) {
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/project_epic/server_config/plugins")
                execute("cd /home/epic/project_epic/server_config/plugins && rm -f MonumentaStructureManagement.jar && ln -s " + shadowJar.archiveFileName.get() + " MonumentaStructureManagement.jar")
                execute("cd /home/epic/project_epic/mobs/plugins && rm -f MonumentaStructureManagement.jar && ln -s ../../server_config/plugins/MonumentaStructureManagement.jar")
            }
        }
    }
}

tasks.create("play-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(adminssh) {
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/play/m8/server_config/plugins")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/play/m11/server_config/plugins")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/play/m13/server_config/plugins")
                execute("cd /home/epic/play/m8/server_config/plugins && rm -f MonumentaStructureManagement.jar && ln -s " + shadowJar.archiveFileName.get() + " MonumentaStructureManagement.jar")
                execute("cd /home/epic/play/m11/server_config/plugins && rm -f MonumentaStructureManagement.jar && ln -s " + shadowJar.archiveFileName.get() + " MonumentaStructureManagement.jar")
                execute("cd /home/epic/play/m13/server_config/plugins && rm -f MonumentaStructureManagement.jar && ln -s " + shadowJar.archiveFileName.get() + " MonumentaStructureManagement.jar")
            }
        }
    }
}

fun Service.runSessions(action: RunHandler.() -> Unit) =
    run(delegateClosureOf(action))

fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) =
    session(*remotes, delegateClosureOf(action))

fun SessionHandler.put(from: Any, into: Any) =
    put(hashMapOf("from" to from, "into" to into))
