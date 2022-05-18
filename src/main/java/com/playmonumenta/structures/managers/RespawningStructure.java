package com.playmonumenta.structures.managers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.playmonumenta.scriptedquests.zones.Zone;
import com.playmonumenta.scriptedquests.zones.ZoneLayer;
import com.playmonumenta.structures.StructuresAPI;
import com.playmonumenta.structures.StructuresPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;

public class RespawningStructure implements Comparable<RespawningStructure> {
	public static class StructureBounds {
		public Vector mLowerCorner;
		public Vector mUpperCorner;

		public StructureBounds(Vector lowerCorner, Vector upperCorner) {
			mLowerCorner = Vector.getMinimum(lowerCorner, upperCorner);
			mUpperCorner = Vector.getMaximum(lowerCorner, upperCorner);
		}

		public boolean within(Vector vec) {
			return vec.getX() >= mLowerCorner.getX() && vec.getX() <= mUpperCorner.getX() &&
			       vec.getY() >= mLowerCorner.getY() && vec.getY() <= mUpperCorner.getY() &&
			       vec.getZ() >= mLowerCorner.getZ() && vec.getZ() <= mUpperCorner.getZ();
		}
	}

	private enum NearbyState {
		UNKNOWN,
		PLAYER_WITHIN,
		NO_PLAYER_WITHIN
	}

	private StructuresPlugin mPlugin;
	private World mWorld;
	private Random mRandom;

	protected String mConfigLabel;        // The label used to modify this structure via commands
	private String mName;                 // What the pretty name of the structure is
	private Vector mLoadPos;              // Where it will be loaded
	private StructureBounds mInnerBounds; // The bounding box for the structure itself
	private StructureBounds mOuterBounds; // The bounding box for the nearby area around the structure
	private int mExtraRadius;             // Radius around the structure that still gets messages
	private int mTicksLeft;               // How many ticks remaining until respawn
	private int mRespawnTime;             // How many ticks between respawns
	private String mPostRespawnCommand;   // Command run via the console after respawning structure
	private boolean mForcedRespawn;       // Was this set to have a forced respawn via compass
	private NearbyState mPlayerNearbyLastTick; // Was there a player nearby last tick while respawn time was < 0?
	private boolean mConquered;           // Is the POI conquered
	private UUID mLastPlayerRespawn;      // Player who last forced a respawn
	private int mTimesPlayerSpawned;	  // How many times in a row it's been reset through conquered or force respawn
	private boolean mInitialized = false; // This structure has finished loading the initial schematic/dimensions

	// Path String -> BlockArrayClipboard maps
	private final List<String> mGenericVariants = new ArrayList<String>();
	private final List<String> mSpecialVariants = new ArrayList<String>();

	// Which structure will be spawned next
	// If this is null, one of the genericVariants will be chosen randomly
	// If this is a path, it must be one of mSpecialVariants (not in generic variants!)
	private String mNextRespawnPath;

	// If this is non-null, then some action happens when players break some number of spawners
	private SpawnerBreakTrigger mSpawnerBreakTrigger;

	@Override
	public int compareTo(RespawningStructure other) {
		return mConfigLabel.compareTo(other.mConfigLabel);
	}

	public static CompletableFuture<RespawningStructure> fromConfig(StructuresPlugin plugin, World world, String configLabel, ConfigurationSection config) {
		CompletableFuture<RespawningStructure> future = new CompletableFuture<>();

		try {
			if (!config.isString("name")) {
				throw new Exception("Invalid name");
			} else if (!config.isList("structure_paths")) {
				throw new Exception("Invalid structure_paths");
			} else if (!config.isInt("x")) {
				throw new Exception("Invalid x value");
			} else if (!config.isInt("y")) {
				throw new Exception("Invalid y value");
			} else if (!config.isInt("z")) {
				throw new Exception("Invalid z value");
			} else if (!config.isInt("respawn_period")) {
				throw new Exception("Invalid respawn_period value");
			} else if (!config.isInt("ticks_until_respawn")) {
				throw new Exception("Invalid ticks_until_respawn value");
			} else if (!config.isInt("extra_detection_radius")) {
				throw new Exception("Invalid extra_detection_radius value");
			}

			String postRespawnCommand = null;
			if (config.isString("post_respawn_command")) {
				postRespawnCommand = config.getString("post_respawn_command");
			}

			List<String> specialPaths = null;
			if (config.isList("structure_special_paths")) {
				specialPaths = config.getStringList("structure_special_paths");
			}

			String nextRespawnPath = null;
			if (config.isString("next_respawn_path")) {
				nextRespawnPath = config.getString("next_respawn_path");
			}

			SpawnerBreakTrigger spawnerBreakTrigger = null;
			if (config.isConfigurationSection("spawner_break_trigger")) {
				spawnerBreakTrigger = SpawnerBreakTrigger.fromConfig(plugin,
						config.getConfigurationSection("spawner_break_trigger"));
			}

			return withParameters(plugin, world, config.getInt("extra_detection_radius"), configLabel,
								  config.getString("name"), config.getStringList("structure_paths"),
								  new Vector(config.getInt("x"), config.getInt("y"), config.getInt("z")),
								  config.getInt("respawn_period"), config.getInt("ticks_until_respawn"),
								  postRespawnCommand, specialPaths, nextRespawnPath, spawnerBreakTrigger);
		} catch (Exception ex) {
			future.completeExceptionally(ex);
			return future;
		}
	}

	public static CompletableFuture<RespawningStructure> withParameters(StructuresPlugin plugin, World world, int extraRadius,
		                       String configLabel, String name, List<String> genericPaths,
		                       Vector loadPos, int respawnTime, int ticksLeft,
		                       String postRespawnCommand, List<String> specialPaths,
		                       String nextRespawnPath, SpawnerBreakTrigger spawnerBreakTrigger) {

		CompletableFuture<RespawningStructure> future = new CompletableFuture<>();

		try {
			if (respawnTime < 200) {
				throw new Exception("Minimum respawn_period value is 200 ticks");
			}

			if (genericPaths.size() < 1) {
				throw new Exception("No structures specified for '" + configLabel + "'");
			}

			RespawningStructure structure = new RespawningStructure(plugin, world, extraRadius,
			                                                        configLabel, name, genericPaths,
			                                                        loadPos, respawnTime, ticksLeft,
			                                                        postRespawnCommand, specialPaths,
			                                                        nextRespawnPath, spawnerBreakTrigger);

			// Load the first schematic to get its size
			return StructuresAPI.loadStructure(structure.mGenericVariants.get(0)).thenApply((clipboard) -> {
				if (specialPaths != null) {
					for (String path : specialPaths) {
						structure.mSpecialVariants.add(path);
					}
				}

				// TODO: Add a check that these are all the same size

				// Determine structure size
				Region clipboardRegion = clipboard.getRegion().clone();
				BlockVector3 structureSize = clipboardRegion.getMaximumPoint().subtract(clipboardRegion.getMinimumPoint());

				// Create a bounding box for the structure itself, plus a slightly larger box to notify nearby players
				structure.mInnerBounds = new StructureBounds(structure.mLoadPos, structure.mLoadPos.clone().add(new Vector(structureSize.getX(), structureSize.getY(), structureSize.getZ())));
				Vector extraRadiusVec = new Vector(structure.mExtraRadius, structure.mExtraRadius, structure.mExtraRadius);
				structure.mOuterBounds = new StructureBounds(structure.mInnerBounds.mLowerCorner.clone().subtract(extraRadiusVec),
															 structure.mInnerBounds.mUpperCorner.clone().add(extraRadiusVec));

				structure.registerZone();
				structure.mInitialized = true;

				return structure;
			});
		} catch (Exception ex) {
			future.completeExceptionally(ex);
			return future;
		}
	}

	private RespawningStructure(StructuresPlugin plugin, World world, int extraRadius,
		                        String configLabel, String name, List<String> genericPaths,
		                        Vector loadPos, int respawnTime, int ticksLeft,
		                        String postRespawnCommand, List<String> specialPaths,
		                        String nextRespawnPath, SpawnerBreakTrigger spawnerBreakTrigger) {
		mPlugin = plugin;
		mWorld = world;
		mRandom = new Random();
		mConfigLabel = configLabel;
		mName = name;
		mLoadPos = loadPos;
		mExtraRadius = extraRadius;
		mRespawnTime = respawnTime;
		mTicksLeft = ticksLeft;
		mPostRespawnCommand = postRespawnCommand;
		mSpawnerBreakTrigger = spawnerBreakTrigger;
		mForcedRespawn = false;
		mPlayerNearbyLastTick = NearbyState.UNKNOWN;
		mConquered = false;
		mLastPlayerRespawn = null;
		mTimesPlayerSpawned = 0;

		for (String path : genericPaths) {
			mGenericVariants.add(path);
		}

		// Set the next respawn path (or not if null)
		activateSpecialStructure(nextRespawnPath);
	}

	public String getInfoString() {
		return "name='" + mName + "' pos=(" + Integer.toString((int)mLoadPos.getX()) + " " +
		       Integer.toString((int)mLoadPos.getY()) + " " + Integer.toString((int)mLoadPos.getZ()) +
		       ") paths={" + String.join(" ", mGenericVariants) + "} period=" + Integer.toString(mRespawnTime) + " ticksleft=" +
		       Integer.toString(mTicksLeft) +
		       (mPostRespawnCommand == null ? "" : " respawnCmd='" + mPostRespawnCommand + "'") +
		       (mSpecialVariants.isEmpty() ? "" : " specialPaths={" + String.join(" ", mSpecialVariants) + "}") +
		       (mSpawnerBreakTrigger == null ? "" : " spawnerTrigger={" + mSpawnerBreakTrigger.getInfoString() + "}");
	}

	public void activateSpecialStructure(String nextRespawnPath) {
		//TODO: There needs to be a better way to register special versions!
		if (nextRespawnPath != null && !mSpecialVariants.contains(nextRespawnPath)) {
			mSpecialVariants.add(nextRespawnPath);
		}
		//TODO: Check that this structure is the same size!

		mNextRespawnPath = nextRespawnPath;
	}

	public void respawn() {
		final String respawnPath;

		if (mNextRespawnPath == null) {
			// No specified next path - pick a generic one at random
			respawnPath = mGenericVariants.get(mRandom.nextInt(mGenericVariants.size()));
		} else {
			// Next path was specified - use it
			respawnPath = mNextRespawnPath;

			// Go back to generic versions after spawning this once
			mNextRespawnPath = null;
		}

		mPlayerNearbyLastTick = NearbyState.UNKNOWN;

		mTicksLeft = mRespawnTime;

		// If we are tracking spawners for this structure, reset the count
		if (mSpawnerBreakTrigger != null) {
			mSpawnerBreakTrigger.resetCount();
		}

		if (!mForcedRespawn) {
			mLastPlayerRespawn = null;
		}

		if (mConquered) {
			mTimesPlayerSpawned++;
		} else {
			mTimesPlayerSpawned = 0;
		}

		mForcedRespawn = false;
		mConquered = false;

		StructuresAPI.loadAndPasteStructure(respawnPath, new Location(mWorld, mLoadPos.getX(), mLoadPos.getY(), mLoadPos.getZ()), true).whenComplete((unused, exception) -> {
			if (exception != null) {
				mPlugin.getLogger().severe("Failed to respawn structure '" + mConfigLabel + "'");
				exception.printStackTrace();
			} else {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), mPostRespawnCommand);
			}
		});
	}

	public void forcedRespawn(Player player) {
		if (!isNearby(player)) {
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must be nearby the POI to force its respawn.");
			return;
		}

		if (mForcedRespawn) {
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + mName + " is already forced to respawn!");
		} else {
			if (isConquered()) {
				if (player.getUniqueId().equals(mLastPlayerRespawn)) {
					player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You cannot force a POI to respawn twice in a row.");
				} else {
					mLastPlayerRespawn = player.getUniqueId();
					mForcedRespawn = true;
					if (mTicksLeft < 2 * 60 * 20) {
						mTicksLeft = 2 * 60 * 20;
						for (Player p : mWorld.getPlayers()) {
							if (isNearby(p)) {
								p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + mName + " will forcibly respawn in 2 minutes (timer set)");
							}
						}
					} else {
						int minutesLeft = mTicksLeft / (60 * 20);
						String minutesPlural = (minutesLeft > 1) ? "s" : "";
						for (Player p : mWorld.getPlayers()) {
							if (isNearby(p)) {
								p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + mName + " will forcibly respawn in "
										+ minutesLeft + " minute" + minutesPlural + "!");
							}
						}
					}
				}
			} else {
				player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You cannot force a respawn on a Point of Interest that has not been conquered!");
			}
		}
	}

	public void tellRespawnTime(Player player) {
		int minutes = mTicksLeft / (60 * 20);
		int seconds = (mTicksLeft / 20) % 60;
		String message = mName + " is respawning in ";
		String color = ChatColor.GREEN + "" + ChatColor.BOLD;

		if (mTicksLeft <= 600) {
			color = ChatColor.RED + "" + ChatColor.BOLD;
		}

		if (minutes > 1) {
			message += Integer.toString(minutes) + " minutes";
		} else if (minutes == 1) {
			message += "1 minute";
		}

		if (minutes > 0 && seconds > 0) {
			message += " and ";
		}

		if (seconds > 1) {
			message += Integer.toString(seconds) + " seconds";
		} else if (seconds == 1) {
			message += "1 second";
		}

		if (mTicksLeft <= 0) {
			message = mName + " will respawn when empty.";
		}

		if (isWithin(player)) {
			message += " [Within]";
		}

		message = color + message;

		if (mForcedRespawn || mNextRespawnPath != null) {
			message += " " + ChatColor.RED + "" + ChatColor.BOLD + "[Forced Respawn]";
		}

		TextComponent clickable = new TextComponent("[Force " + mName + " to respawn]");
		clickable.setColor(ChatColor.LIGHT_PURPLE);
		clickable.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/compassrespawn " + mConfigLabel));

		player.spigot().sendMessage(new TextComponent(message));
		if (mConquered && !mForcedRespawn) {
			player.spigot().sendMessage(clickable);
		}
	}

	public int getTicksLeft() {
		return mTicksLeft;
	}

	public boolean isNearby(Player player) {
		return mWorld.equals(player.getWorld()) && mOuterBounds.within(player.getLocation().toVector());
	}

	public boolean isWithin(Player player) {
		return mWorld.equals(player.getWorld()) && mInnerBounds.within(player.getLocation().toVector());
	}

	public void tellRespawnTimeIfNearby(Player player) {
		if (isNearby(player)) {
			tellRespawnTime(player);
		}
	}

	public void setRespawnTimer(int ticksUntilRespawn) {
		if (ticksUntilRespawn <= 0) {
			mForcedRespawn = true;
			respawn();
		} else {
			mTicksLeft = ticksUntilRespawn;
		}
	}

	public void setRespawnTimerPeriod(int ticksUntilRespawn) {
		mRespawnTime = ticksUntilRespawn;
	}

	public void setPostRespawnCommand(String postRespawnCommand) {
		mPostRespawnCommand = postRespawnCommand;
	}

	public Map<String, Object> getConfig() {
		Map<String, Object> configMap = new LinkedHashMap<String, Object>();

		configMap.put("name", mName);
		configMap.put("structure_paths", mGenericVariants);
		configMap.put("x", (int)mLoadPos.getX());
		configMap.put("y", (int)mLoadPos.getY());
		configMap.put("z", (int)mLoadPos.getZ());
		configMap.put("extra_detection_radius", mExtraRadius);
		configMap.put("respawn_period", mRespawnTime);
		configMap.put("ticks_until_respawn", mTicksLeft);
		if (mPostRespawnCommand != null) {
			configMap.put("post_respawn_command", mPostRespawnCommand);
		}
		if (!mSpecialVariants.isEmpty()) {
			configMap.put("structure_special_paths", mSpecialVariants);
		}
		if (mNextRespawnPath != null) {
			configMap.put("next_respawn_path", mNextRespawnPath);
		}
		if (mSpawnerBreakTrigger != null) {
			configMap.put("spawner_break_trigger", mSpawnerBreakTrigger.getConfig());
		}

		return configMap;
	}

	public void tick(int ticks) {
		if (!mName.isEmpty() &&
		    ((mTicksLeft >= 2400 && (mTicksLeft - ticks) < 2400) ||
		     (mTicksLeft >= 600 && (mTicksLeft - ticks) < 600))) {
			for (Player player : mWorld.getPlayers()) {
				if (isNearby(player)) {
					tellRespawnTime(player);
				}
			}
		}

		mTicksLeft -= ticks;

		if (mTicksLeft < 0) {
			boolean isPlayerNearby = false;
			boolean isPlayerWithin = false;
			boolean isAmped = mNextRespawnPath != null;

			for (Player player : mWorld.getPlayers()) {
				if (player.getGameMode() != GameMode.SPECTATOR) {
					if (isWithin(player)) {
						isPlayerNearby = true;
						isPlayerWithin = true;
						break;
					} else if (isNearby(player)) {
						isPlayerNearby = true;
						/* Don't break - another player might still be within */
					}
				}
			}


			/*
			 * To respawn, a player must be nearby (within the outer detection zone)
			 * AND one of the following conditions
			 */
			boolean shouldRespawn = isPlayerNearby &&
					(mForcedRespawn || // The POI was force to respawn by a player
					 isAmped || // The POI is amplified for the next spawn
					 mPlayerNearbyLastTick == NearbyState.NO_PLAYER_WITHIN || // There was no player nearby last check (they teleported in)
					 !isPlayerWithin); // There is no player within the POI itself, just nearby OR respawn is forced

			mPlayerNearbyLastTick = isPlayerNearby ? NearbyState.PLAYER_WITHIN : NearbyState.NO_PLAYER_WITHIN;

			if (shouldRespawn) {
				respawn();
			}
		}
	}

	// This event is called every time a spawner is broken anywhere
	// Have to test that it was within this structure
	public void spawnerBreakEvent(Location loc) {
		// Only care about tracking spawners if there is a trigger
		if (mSpawnerBreakTrigger != null && mWorld.equals(loc.getWorld()) && mInnerBounds.within(loc.toVector())) {
			mSpawnerBreakTrigger.spawnerBreakEvent(this);
		}
	}

	public void setSpawnerBreakTrigger(SpawnerBreakTrigger trigger) {
		mSpawnerBreakTrigger = trigger;
	}

	public void conquerStructure() {
		if (mForcedRespawn) {
			// POI was already scheduled to respawn forcibly - conquering does nothing at this point, no reason to increase timer
			return;
		}

		// Count how long it took to conquer POI, only set to zero if greater than minimum respawn time
		int playerCausedDelay = mTimesPlayerSpawned * 5 * 60 * 20;
		mTicksLeft = playerCausedDelay <= (mRespawnTime / 2) ? playerCausedDelay : (mRespawnTime / 2);

		int minutesLeft = mTicksLeft / (60 * 20);
		String minutesPlural = (minutesLeft > 1) ? "s" : "";

		mConquered = true;
		for (Player player : mWorld.getPlayers()) {
			if (player.getGameMode() != GameMode.SPECTATOR &&
			    isNearby(player)) {
				if (mTicksLeft <= 0) {
					player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + mName +" has been conquered! It will respawn once all players leave the area.");
				} else {
					player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + mName +" has been conquered! It will respawn in " +
							minutesLeft + " minute" + minutesPlural + "!");
				}
			}
		}
	}

	public boolean isConquered() {
		return mConquered;
	}

	public boolean isForced() {
		return mForcedRespawn;
	}

	public void undoForce() {
		mForcedRespawn = false;
	}

	public int getRespawnTime() {
		return mRespawnTime;
	}

	public World getWorld() {
		return mWorld;
	}

	public boolean registerZone() {
		ZoneLayer zoneLayerInside = mPlugin.mRespawnManager.mZoneLayerInside;
		ZoneLayer zoneLayerNearby = mPlugin.mRespawnManager.mZoneLayerNearby;

		Zone insideZone = new Zone(zoneLayerInside,
		                           mInnerBounds.mLowerCorner.clone(),
		                           mInnerBounds.mUpperCorner.clone(),
		                           mName,
		                           new LinkedHashSet<String>());
		Zone nearbyZone = new Zone(zoneLayerNearby,
		                           mOuterBounds.mLowerCorner.clone(),
		                           mOuterBounds.mUpperCorner.clone(),
		                           mName,
		                           new LinkedHashSet<String>());
		zoneLayerInside.addZone(insideZone);
		zoneLayerNearby.addZone(nearbyZone);

		mPlugin.mRespawnManager.registerRespawningStructureZone(insideZone, this);
		mPlugin.mRespawnManager.registerRespawningStructureZone(nearbyZone, this);

		return true;
	}
}