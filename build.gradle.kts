import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("com.playmonumenta.gradle-config") version "2.+"
}

dependencies {
    compileOnly(libs.fawe.core)
    compileOnly(libs.fawe.bukkit) {
        isTransitive = false
    }
    compileOnly(libs.commandapi)
    compileOnly(libs.sq) {
        artifact {
            classifier = "all"
        }
    }
}

tasks {
    javadoc {
        (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
    }
}

monumenta {
    name("MonumentaStructureManagement")
    paper(
        "com.playmonumenta.structures.StructuresPlugin", BukkitPluginDescription.PluginLoadOrder.POSTWORLD, "1.20",
        depends = listOf("CommandAPI", "FastAsyncWorldEdit", "ScriptedQuests")
    )
}
