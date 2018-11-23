package com.playmonumenta.epicstructures.managers;

import com.playmonumenta.epicstructures.Plugin;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class RespawnManager {
	private final Plugin mPlugin;
	private final World mWorld;

	private final SortedMap<String, RespawningStructure> mRespawns = new ConcurrentSkipListMap<String, RespawningStructure>();
	private final int mTickPeriod;
	private final BukkitRunnable mRunnable = new BukkitRunnable() {
		@Override
		public void run()
		{
			for (RespawningStructure struct : mRespawns.values()) {
				struct.tick(mTickPeriod);
			}
		}
	};
	private boolean taskScheduled = false;
	private boolean structuresLoaded = false;

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

		// Load the structures asynchronously so this doesn't hold up the start of the server
		ConfigurationSection respawnSection = config.getConfigurationSection("respawning_structures");
		new BukkitRunnable() {
			@Override
			public void run()
			{
				loadStructuresAsync(respawnSection);

				// Schedule a repeating task to trigger structure countdowns
				mRunnable.runTaskTimer(mPlugin, 0, mTickPeriod);
				taskScheduled = true;
				structuresLoaded = true;
			}
		}.runTaskAsynchronously(plugin);
	}

	/* It *should* be safe to call this async */
	private void loadStructuresAsync(ConfigurationSection respawnSection) {
		Set<String> keys = respawnSection.getKeys(false);

		// Iterate over all the respawning entries (shallow list at this level)
		for (String key : keys) {
			if (!respawnSection.isConfigurationSection(key)) {
				mPlugin.asyncLog(Level.WARNING, "respawning_structures entry '" + key + "' is not a configuration section!");
				continue;
			}

			try {
				mRespawns.put(key, RespawningStructure.fromConfig(mPlugin, mWorld, key,
				              respawnSection.getConfigurationSection(key)));
				mPlugin.asyncLog(Level.INFO, "Successfully loaded respawning structure '" + key + "': ");
			} catch (Exception e) {
				mPlugin.asyncLog(Level.WARNING, "Failed to load respawning structure entry '" + key + "': ", e);
				continue;
			}
		}
	}

	public void addStructure(int extraRadius, String configLabel, String name, String path,
	                         Vector loadPos, int respawnTime) throws Exception {
		mRespawns.put(configLabel, new RespawningStructure(mPlugin, mWorld, extraRadius, configLabel,
		              name, Arrays.asList(path), loadPos, respawnTime, respawnTime, null, null, null, null));
		mPlugin.saveConfig();
	}

	public void removeStructure(String configLabel) throws Exception {
		if (!mRespawns.containsKey(configLabel)) {
			throw new Exception("Structure '" + configLabel + "' does not exist");
		}

		mRespawns.remove(configLabel);
		mPlugin.saveConfig();
	}

	/* Human readable */
	public void listStructures(CommandSender sender) {
		if (mRespawns.isEmpty()) {
			sender.sendMessage("No respawning structures registered");
			return;
		}

		sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Respawning Structure List");
		String structuresString = ChatColor.GREEN + "";
		for (Map.Entry<String, RespawningStructure> entry : mRespawns.entrySet()) {
			structuresString += entry.getKey() + "  ";
		}
		sender.sendMessage(structuresString);
	}

	/* Machine readable list */
	public String[] listStructures() {
		return mRespawns.keySet().toArray(new String[mRespawns.size()]);
	}

	public void structureInfo(CommandSender sender, String label) {
		RespawningStructure struct = mRespawns.get(label);

		if (struct == null) {
			sender.sendMessage(ChatColor.RED + "Structure '" + label + "' not found!");
			return;
		}

		sender.sendMessage(ChatColor.GREEN + label + " : " + ChatColor.RESET +
						   struct.getInfoString());
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

	public void setSpawnerBreakTrigger(String label, SpawnerBreakTrigger trigger) throws Exception {
		RespawningStructure struct = mRespawns.get(label);
		if (struct == null) {
			throw new Exception("Structure '" + label + "' not found!");
		}
		struct.setSpawnerBreakTrigger(trigger);
		mPlugin.saveConfig();
	}

	public void activateSpecialStructure(String label, String nextRespawnPath) throws Exception {
		RespawningStructure struct = mRespawns.get(label);
		if (struct == null) {
			throw new Exception("Structure '" + label + "' not found!");
		}
		struct.activateSpecialStructure(nextRespawnPath);
	}

	public void tellNearbyRespawnTimes(Player player) {
		boolean nearbyStruct = false;
		for (RespawningStructure struct : mRespawns.values()) {
			if (struct.isNearby(player)) {
				struct.tellRespawnTime(player);
				nearbyStruct = true;
			}
		}

		if (!nearbyStruct) {
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You are not within range of a respawning area");
		}
	}

	public void cleanup() {
		if (taskScheduled = true) {
			mRunnable.cancel();
			taskScheduled = false;
		}
		mRespawns.clear();
	}

	public YamlConfiguration getConfig() throws Exception {
		if (!structuresLoaded) {
			throw new Exception("Structures haven't finished loading yet!");
		}

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

	public void spawnerBreakEvent(Location loc) {
		Vector vec = loc.toVector();
		for (RespawningStructure struct : mRespawns.values()) {
			struct.spawnerBreakEvent(vec);
		}
	}
}
