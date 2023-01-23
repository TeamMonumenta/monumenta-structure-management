package com.playmonumenta.structures.managers;

import com.playmonumenta.scriptedquests.zones.Zone;
import com.playmonumenta.scriptedquests.zones.ZoneFragment;
import com.playmonumenta.scriptedquests.zones.ZoneLayer;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import com.playmonumenta.structures.StructuresPlugin;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RespawnManager {
	public static final String ZONE_LAYER_NAME_INSIDE = "Respawning Structures Inside";
	public static final String ZONE_LAYER_NAME_NEARBY = "Respawning Structures Nearby";
	private static @Nullable RespawnManager INSTANCE = null;
	public static ArgumentSuggestions SUGGESTIONS_STRUCTURES = ArgumentSuggestions.strings((info) -> {
		if (INSTANCE == null) {
			return new String[]{};
		}
		return INSTANCE.listStructures();
	});

	private final StructuresPlugin mPlugin;
	private final World mWorld;
	protected final ZoneManager mZoneManager;

	private final SortedMap<String, RespawningStructure> mRespawns = new ConcurrentSkipListMap<>();
	private final int mTickPeriod;
	private final BukkitRunnable mRunnable = new BukkitRunnable() {
		@Override
		public void run() {
			for (RespawningStructure struct : mRespawns.values()) {
				struct.tick(mTickPeriod);
			}
		}
	};
	private boolean mTaskScheduled = false;
	private boolean mStructuresLoaded = false;
	protected ZoneLayer mZoneLayerInside = new ZoneLayer(ZONE_LAYER_NAME_INSIDE);
	protected ZoneLayer mZoneLayerNearby = new ZoneLayer(ZONE_LAYER_NAME_NEARBY, true);
	private final Map<Zone, RespawningStructure> mStructuresByZone = new LinkedHashMap<>();

	public RespawnManager(StructuresPlugin plugin, World world, YamlConfiguration config) {
		INSTANCE = this;
		mPlugin = plugin;
		mWorld = world;

		com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin;
		scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin)Bukkit.getPluginManager().getPlugin("ScriptedQuests");
		if (scriptedQuestsPlugin == null) {
			throw new RuntimeException("Respawn Manager attempted to access ScriptedQuests before it loaded");
		}
		mZoneManager = scriptedQuestsPlugin.mZoneManager;
		// Register empty zone layers so replacing them is easier
		mZoneManager.registerPluginZoneLayer(mZoneLayerInside);
		mZoneManager.registerPluginZoneLayer(mZoneLayerNearby);

		// Load the frequency that the plugin should check for respawning structures
		if (!config.isInt("check_respawn_period")) {
			plugin.getLogger().warning("No check_respawn_period setting specified - using default 20");
			mTickPeriod = 20;
		} else {
			mTickPeriod = config.getInt("check_respawn_period");
		}

		// Load the respawning structures configuration section
		ConfigurationSection respawnSection = config.getConfigurationSection("respawning_structures");
		if (respawnSection == null) {
			plugin.getLogger().log(Level.INFO, "No respawning structures defined");
			mStructuresLoaded = true;
			return;
		}

		// Load the structures asynchronously so this doesn't hold up the start of the server
		Set<String> keys = respawnSection.getKeys(false);

		mZoneLayerInside.invalidate();
		mZoneLayerNearby.invalidate();
		mZoneLayerInside = new ZoneLayer(ZONE_LAYER_NAME_INSIDE);
		mZoneLayerNearby = new ZoneLayer(ZONE_LAYER_NAME_NEARBY, true);
		mStructuresByZone.clear();

		final AtomicInteger numRemaining = new AtomicInteger(keys.size());

		// Iterate over all the respawning entries (shallow list at this level)
		for (String key : keys) {
			if (!respawnSection.isConfigurationSection(key)) {
				mPlugin.getLogger().warning("respawning_structures entry '" + key + "' is not a configuration section!");
				numRemaining.decrementAndGet();
				continue;
			}


			RespawningStructure.fromConfig(mPlugin, mWorld, key, respawnSection.getConfigurationSection(key)).whenComplete((structure, ex) -> {
				numRemaining.decrementAndGet();
				if (ex != null) {
					mPlugin.getLogger().warning("Failed to load respawning structure entry '" + key + "': " + ex.getMessage());
					ex.printStackTrace();
				} else {
					mRespawns.put(key, structure);
					mPlugin.getLogger().info("Successfully loaded respawning structure '" + key + "': ");
				}
			});
		}

		// Poll until all structures have finished loading
		new BukkitRunnable() {
			@Override
			public void run() {
				if (numRemaining.get() == 0) {
					// Now that all structures have loaded, enable this respawn manager

					// Schedule a repeating task to trigger structure countdowns
					mRunnable.runTaskTimer(mPlugin, 0, mTickPeriod);

					// Enable the plugin zone layers that have been populated (but not registered) during the reload
					mZoneManager.replacePluginZoneLayer(mZoneLayerInside);
					mZoneManager.replacePluginZoneLayer(mZoneLayerNearby);

					mTaskScheduled = true;
					mStructuresLoaded = true;
					this.cancel();
				}
			}
		}.runTaskTimer(StructuresPlugin.getInstance(), 5, 5);
	}

	public static RespawnManager getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("Attempted to get RespawnManager before it loaded");
		}
		return INSTANCE;
	}

	public CompletableFuture<Void> addStructure(int extraRadius, String configLabel, String name, String path, Vector loadPos, int respawnTime) {

		return RespawningStructure.withParameters(mPlugin, mWorld, extraRadius, configLabel,
		                                          name, Collections.singletonList(path), loadPos,
												  respawnTime, respawnTime, null, null,
												  null, null).thenApply((structure) -> {
			mRespawns.put(configLabel, structure);
			mPlugin.saveConfig();
			mZoneManager.replacePluginZoneLayer(mZoneLayerInside);
			mZoneManager.replacePluginZoneLayer(mZoneLayerNearby);
			return null;
		});
	}

	public void removeStructure(String configLabel) throws Exception {
		if (!mRespawns.containsKey(configLabel)) {
			throw new Exception("Structure '" + configLabel + "' does not exist");
		}

		mRespawns.remove(configLabel);
		mPlugin.saveConfig();

		mZoneLayerInside.invalidate();
		mZoneLayerNearby.invalidate();
		mZoneLayerInside = new ZoneLayer(ZONE_LAYER_NAME_INSIDE);
		mZoneLayerNearby = new ZoneLayer(ZONE_LAYER_NAME_NEARBY, true);
		mStructuresByZone.clear();

		for (RespawningStructure struct : mRespawns.values()) {
			struct.registerZone();
		}

		mZoneManager.replacePluginZoneLayer(mZoneLayerInside);
		mZoneManager.replacePluginZoneLayer(mZoneLayerNearby);
	}

	public List<RespawningStructure> getStructures(Vector loc, boolean includeNearby) {
		List<RespawningStructure> structures = new ArrayList<>();
		ZoneFragment zoneFragment = mZoneManager.getZoneFragment(loc);
		if (zoneFragment == null) {
			return structures;
		}

		String layerName = includeNearby ? ZONE_LAYER_NAME_NEARBY : ZONE_LAYER_NAME_INSIDE;
		List<Zone> zones = zoneFragment.getParentAndEclipsed(layerName);
		for (Zone zone : zones) {
			RespawningStructure struct = mStructuresByZone.get(zone);
			if (struct == null) {
				continue;
			}
			structures.add(struct);
		}

		return structures;
	}

	/* Human readable */
	public void listStructures(CommandSender sender) {
		if (mRespawns.isEmpty()) {
			sender.sendMessage("No respawning structures registered");
			return;
		}

		sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Respawning Structure List");
		StringBuilder structuresString = new StringBuilder(ChatColor.GREEN + "");
		for (Map.Entry<String, RespawningStructure> entry : mRespawns.entrySet()) {
			structuresString.append(entry.getKey()).append("  ");
		}
		sender.sendMessage(structuresString.toString());
	}

	/* Machine-readable list */
	public String[] listStructures() {
		return mRespawns.keySet().toArray(new String[]{});
	}

	public void structureInfo(CommandSender sender, String label) throws Exception {
		sender.sendMessage(ChatColor.GREEN + label + " : " + ChatColor.RESET +
		                   getStructure(label).getInfoString());
	}

	public void setTimer(String label, int ticksUntilRespawn) throws Exception {
		getStructure(label).setRespawnTimer(ticksUntilRespawn);
	}

	public void setTimerPeriod(String label, int ticksUntilRespawn) throws Exception {
		getStructure(label).setRespawnTimerPeriod(ticksUntilRespawn);
	}

	public void setPostRespawnCommand(String label, @Nullable String command) throws Exception {
		getStructure(label).setPostRespawnCommand(command);
		mPlugin.saveConfig();
	}

	public void setSpawnerBreakTrigger(String label, @Nullable SpawnerBreakTrigger trigger) throws Exception {
		getStructure(label).setSpawnerBreakTrigger(trigger);
		mPlugin.saveConfig();
	}

	public void activateSpecialStructure(String label, @Nullable String nextRespawnPath) throws Exception {
		getStructure(label).activateSpecialStructure(nextRespawnPath);
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

	public void compassRespawn(Player player, String label) throws Exception {
		getStructure(label).forcedRespawn(player, false);
	}

	public void forceConquerRespawn(String label) throws Exception {
		getStructure(label).conquerStructure();
	}

	public void cleanup() {
		if (mTaskScheduled) {
			mRunnable.cancel();
			mTaskScheduled = false;
		}
		mRespawns.clear();
	}

	public YamlConfiguration getConfig() throws Exception {
		if (!mStructuresLoaded) {
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
		for (RespawningStructure struct : mRespawns.values()) {
			struct.spawnerBreakEvent(loc);
		}
	}

	protected void registerRespawningStructureZone(Zone zone, RespawningStructure structure) {
		mStructuresByZone.put(zone, structure);
	}

	private RespawningStructure getStructure(String label) throws Exception {
		RespawningStructure struct = mRespawns.get(label);
		if (struct == null) {
			throw new Exception("Structure '" + label + "' not found!");
		}
		return struct;
	}
}
