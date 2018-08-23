package pe.epicstructures.managers;

import com.boydti.fawe.object.schematic.Schematic;

import java.util.TreeMap;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.Set;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.World;
import org.bukkit.util.Vector;

import pe.epicstructures.Plugin;

public class RespawnManager {
	private Plugin mPlugin;
	private World mWorld;

	private SortedMap<String, RespawningStructure> mRespawns = null;

	public RespawnManager(Plugin plugin, World world, YamlConfiguration config) {
		mPlugin = plugin;
		mWorld = world;

		// Load the respawning structures configuration section
		if (!config.isConfigurationSection("respawning_structures")) {
			plugin.getLogger().log(Level.INFO, "No respawning structures defined");
			return;
		}
		ConfigurationSection respawnSection = config.getConfigurationSection("respawning_structures");

		// Load the extra radius where players are determined to be near a structure
		int extraRadius;
		if (!config.isInt("extra_detection_radius")) {
			plugin.getLogger().log(Level.WARNING,
			                       "No extra_detection_radius setting specified - using default 32");
			extraRadius = 32;
		} else {
			extraRadius = config.getInt("extra_detection_radius");
		}

		// Load the frequency that the plugin should check for respawning structures
		int tickPeriod;
		if (!config.isInt("check_respawn_period")) {
			plugin.getLogger().log(Level.WARNING,
			                       "No check_respawn_period setting specified - using default 20");
			tickPeriod = 20;
		} else {
			tickPeriod = config.getInt("check_respawn_period");
		}

		Set<String> keys = respawnSection.getKeys(false);
		mRespawns = new TreeMap<String, RespawningStructure>();

		// Iterate over all the respawning entries (shallow list at this level)
		for (String key : keys) {
			if (!respawnSection.isConfigurationSection(key)) {
				plugin.getLogger().log(Level.WARNING,
				                       "respawning_structures entry '" + key + "' is not a configuration section!");
				continue;
			}

			try {
				mRespawns.put(key, RespawningStructure.fromConfig(plugin, world, extraRadius, key,
				              respawnSection.getConfigurationSection(key)));
			} catch (Exception e) {
				plugin.getLogger().log(Level.WARNING, "Failed to load respawning structure entry'" + key + "': ", e);
				continue;
			}
		}

		// create sync task that counts down and loads
	}

	public void addStructure(int extraRadius, String configLabel, String name, String path,
	                         Vector loadPos, int respawnTime) throws Exception {
		mRespawns.put(configLabel, new RespawningStructure(mPlugin, mWorld, extraRadius, configLabel,
		              name, path, loadPos, respawnTime, respawnTime));
		//TODO: Save config here
	}

	public void removeStructure(String configLabel) throws Exception {
		if (!mRespawns.containsKey(configLabel)) {
			throw new Exception("Structure '" + configLabel + "' does not exist");
		}

		mRespawns.remove(configLabel);
		//TODO: Save config here
	}

	public void dumpInfo(CommandSender sender) {
		for (Map.Entry<String, RespawningStructure> entry : mRespawns.entrySet()) {
			sender.sendMessage(String.format("%s : %s", entry.getKey(), entry.getValue().getInfoString()));
		}
	}

	public void setTimer(String label, int ticksUntilRespawn) throws Exception {
		RespawningStructure struct = mRespawns.get(label);
		if (struct == null) {
			throw new Exception("Structure '" + label + "' not found!");
		}
		struct.setRespawnTimer(ticksUntilRespawn);
	}

	// TODO save config someplace?
	public void onDisable() {

	}
}
