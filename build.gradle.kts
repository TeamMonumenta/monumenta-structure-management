import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("com.playmonumenta.gradle-config") version "1.+"
}

dependencies {
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.fawe.core)
    compileOnly(libs.fawe.bukkit) {
        isTransitive = false
    }
    compileOnly(libs.light.cleaner)
    compileOnly(libs.bkcommonlib)
    compileOnly(libs.commandapi)
    compileOnly(libs.gson)
    compileOnly(libs.sq) {
        artifact {
            classifier = "all"
        }
    }
}

monumenta {
    name("MonumentaStructureManagement")
    paper(
        "com.playmonumenta.structures.StructuresPlugin", BukkitPluginDescription.PluginLoadOrder.POSTWORLD, "1.18",
        depends = listOf("CommandAPI", "ScriptedQuests", "MonumentaRedisSync"),
        softDepends = listOf("LightCleaner")
    )
}
