package pe.epicstructures.managers;

import com.boydti.fawe.object.schematic.Schematic;

import java.util.logging.Level;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.World;

import pe.epicstructures.Plugin;

public class RespawnManager {
	private Plugin mPlugin;
	private World mWorld;

	private SortedMap<String, RespawningStructure> mRespawns = new TreeMap<String, RespawningStructure>();
	private int mTickPeriod;
	private BukkitRunnable mRunnable = new BukkitRunnable() {
		@Override
		public void run()
		{
			for (RespawningStructure struct : mRespawns.values()) {
				struct.tick(mTickPeriod);
			}
		}
	};

	public RespawnManager(Plugin plugin, World world, YamlConfiguration config) {
		mPlugin = plugin;
		mWorld = world;

		// Load the frequency that the plugin should check for respawning structures
		if (!config.isInt("check_respawn_period")) {
			plugin.getLogger().log(Level.INFO,
			                       "No check_respawn_period setting specified - using default 20");
			mTickPeriod = 20;
		} else {
			mTickPeriod = config.getInt("check_respawn_period");
		}

		// Load the respawning structures configuration section
		if (!config.isConfigurationSection("respawning_structures")) {
			plugin.getLogger().log(Level.INFO, "No respawning structures defined");
			return;
		}
		ConfigurationSection respawnSection = config.getConfigurationSection("respawning_structures");

		Set<String> keys = respawnSection.getKeys(false);

		// Iterate over all the respawning entries (shallow list at this level)
		for (String key : keys) {
			if (!respawnSection.isConfigurationSection(key)) {
				plugin.getLogger().log(Level.WARNING,
				                       "respawning_structures entry '" + key + "' is not a configuration section!");
				continue;
			}

			try {
				mRespawns.put(key, RespawningStructure.fromConfig(plugin, world, key,
				              respawnSection.getConfigurationSection(key)));
			} catch (Exception e) {
				plugin.getLogger().log(Level.WARNING, "Failed to load respawning structure entry '" + key + "': ", e);
				continue;
			}
		}

		// Schedule a repeating task to trigger structure countdowns
		mRunnable.runTaskTimer(mPlugin, 0, mTickPeriod);
	}

	public void addStructure(int extraRadius, String configLabel, String name, String path,
	                         Vector loadPos, int respawnTime) throws Exception {
		mRespawns.put(configLabel, new RespawningStructure(mPlugin, mWorld, extraRadius, configLabel,
		              name, path, loadPos, respawnTime, respawnTime, null));
		mPlugin.saveConfig();
	}

	public void removeStructure(String configLabel) throws Exception {
		if (!mRespawns.containsKey(configLabel)) {
			throw new Exception("Structure '" + configLabel + "' does not exist");
		}

		mRespawns.remove(configLabel);
		mPlugin.saveConfig();
	}

	public void listStructures(CommandSender sender) {
		boolean empty = true;
		for (Map.Entry<String, RespawningStructure> entry : mRespawns.entrySet()) {
			sender.sendMessage(entry.getKey() + " : " + entry.getValue().getInfoString());
			empty = false;
		}
		if (empty) {
			sender.sendMessage("No respawning structures registered");
		}
	}

	public void setTimer(String label, int ticksUntilRespawn) throws Exception {
		RespawningStructure struct = mRespawns.get(label);
		if (struct == null) {
			throw new Exception("Structure '" + label + "' not found!");
		}
		struct.setRespawnTimer(ticksUntilRespawn);
	}

	public void setPostRespawnCommand(String label, String command) throws Exception {
		RespawningStructure struct = mRespawns.get(label);
		if (struct == null) {
			throw new Exception("Structure '" + label + "' not found!");
		}
		struct.setPostRespawnCommand(command);
		mPlugin.saveConfig();
	}

	public void cleanup() {
		mRunnable.cancel();
		mRespawns.clear();
	}

	public YamlConfiguration getConfig() {
		// Create the top-level config to return
		YamlConfiguration config = new YamlConfiguration();

		// Add global config
		config.set("check_respawn_period", mTickPeriod);

		// Create the container for respawning structures and iterate over them
		ConfigurationSection respawnConfig = config.createSection("respawning_structures");
		for (Map.Entry<String, RespawningStructure> entry : mRespawns.entrySet()) {
			// Create the container for this structure's data and load it with values
			respawnConfig.createSection(entry.getKey(), entry.getValue().getConfig());
		}

		return config;
	}
}
