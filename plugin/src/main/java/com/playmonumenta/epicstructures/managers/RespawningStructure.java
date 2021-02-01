package com.playmonumenta.epicstructures.managers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.epicstructures.utils.StructureUtils;
import com.playmonumenta.scriptedquests.zones.Zone;
import com.playmonumenta.scriptedquests.zones.ZoneLayer;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;

public class RespawningStructure implements Comparable<RespawningStructure> {
	public class StructureBounds {
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

	private Plugin mPlugin;
	private World mWorld;
	private Random mRandom;

	protected String mConfigLabel;        // The label used to modify this structure via commands
	private String mName;                 // What the pretty name of the structure is
	private Vector mLoadPos;              // Where it will be loaded
	private StructureBounds mInnerBounds; // The bounding box for the structure itself
	private StructureBounds mOuterBounds; // The bounding box for the nearby area around the structure
	private int mExtraRadius;             // Radius around the structure that still gets messages
	private int mTicksLeft;               // How many ticks remaining until respawn
	private int mMinimumRespawnTime;      // Minimum ticks between respawns
	private int mRespawnTime;             // How many ticks between respawns
	private String mPostRespawnCommand;   // Command run via the console after respawning structure
	private boolean mForcedRespawn;       // Was this set to have a forced respawn via compass
	private boolean mConquered;           // Is the POI conquered
	private String mLastPlayerRespawn;    // Player who last forced a respawn

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

	public static RespawningStructure fromConfig(Plugin plugin, World world, String configLabel,
	        ConfigurationSection config) throws Exception {
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

		return new RespawningStructure(plugin, world, config.getInt("extra_detection_radius"), configLabel,
		                               config.getString("name"), config.getStringList("structure_paths"),
		                               new Vector(config.getInt("x"), config.getInt("y"), config.getInt("z")),
		                               config.getInt("respawn_period"), config.getInt("ticks_until_respawn"),
		                               postRespawnCommand, specialPaths, nextRespawnPath, spawnerBreakTrigger);
	}

	public RespawningStructure(Plugin plugin, World world, int extraRadius,
		                       String configLabel, String name, List<String> genericPaths,
		                       Vector loadPos, int respawnTime, int ticksLeft,
		                       String postRespawnCommand, List<String> specialPaths,
		                       String nextRespawnPath, SpawnerBreakTrigger spawnerBreakTrigger) throws Exception {
		mPlugin = plugin;
		mWorld = world;
		mRandom = new Random();
		mConfigLabel = configLabel;
		mName = name;
		mLoadPos = loadPos;
		mExtraRadius = extraRadius;
		mRespawnTime = respawnTime;
		mMinimumRespawnTime = 0;
		mTicksLeft = ticksLeft;
		mPostRespawnCommand = postRespawnCommand;
		mSpawnerBreakTrigger = spawnerBreakTrigger;
		mForcedRespawn = false;
		mConquered = false;
		mLastPlayerRespawn = null;

		if (mRespawnTime < 200) {
			throw new Exception("Minimum respawn_period value is 200 ticks");
		}

		// Load all of the supplied structures
		BlockArrayClipboard clipboard = null;
		for (String path : genericPaths) {
			mGenericVariants.add(path);
		}

		if (mGenericVariants.size() < 1) {
			throw new Exception("No structures specified for '" + mConfigLabel + "'");
		}
		// Load the first schematic to get its size
		clipboard = mPlugin.mStructureManager.loadSchematic(mGenericVariants.get(0));

		if (specialPaths != null) {
			for (String path : specialPaths) {
				mSpecialVariants.add(path);
			}
		}

		// TODO: Add a check that these are all the same size

		// Set the next respawn path (or not if null)
		activateSpecialStructure(nextRespawnPath);

		// Determine structure size
		Region clipboardRegion = clipboard.getRegion().clone();
		BlockVector3 structureSize = clipboardRegion.getMaximumPoint().subtract(clipboardRegion.getMinimumPoint());

		// Create a bounding box for the structure itself, plus a slightly larger box to notify nearby players
		mInnerBounds = new StructureBounds(mLoadPos, mLoadPos.clone().add(new Vector(structureSize.getX(),
		                                                                             structureSize.getY(),
		                                                                             structureSize.getZ())));
		Vector extraRadiusVec = new Vector(extraRadius, extraRadius, extraRadius);
		mOuterBounds = new StructureBounds(mInnerBounds.mLowerCorner.clone().subtract(extraRadiusVec),
		                                   mInnerBounds.mUpperCorner.clone().add(extraRadiusVec));

		registerZone();
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

	public void activateSpecialStructure(String nextRespawnPath) throws Exception {
		//TODO: There needs to be a better way to register special versions!
		if (nextRespawnPath != null && !mSpecialVariants.contains(nextRespawnPath)) {
			mSpecialVariants.add(nextRespawnPath);
			mPlugin.mStructureManager.loadSchematic(nextRespawnPath);
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

		// Ensure player is not inside unless amped
		if (mNextRespawnPath == null) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (mInnerBounds.within(player.getLocation().toVector())) {
					if (!mForcedRespawn) {
						return;
					}
				}
			}
		}

		mTicksLeft = mRespawnTime;

		// Load the schematic asynchronously (this might access the disk!)
		// Then switch back to the main thread to initiate pasting
		new BukkitRunnable() {
			@Override
			public void run() {
				final BlockArrayClipboard clipboard;

				try {
					clipboard = mPlugin.mStructureManager.loadSchematic(respawnPath);
				} catch (Exception e) {
					mPlugin.asyncLog(Level.SEVERE, "Failed to load schematic '" + respawnPath +
					                 "' for respawning structure '" + mConfigLabel + "'", e);
					return;
				}

				/* Once the schematic is loaded, this task is used to paste it */
				new BukkitRunnable() {
					@Override
					public void run() {
						// Load the structure
						StructureUtils.paste(mPlugin, clipboard, mWorld,
											 BlockVector3.at(mLoadPos.getX(), mLoadPos.getY(), mLoadPos.getZ()), true);

						// If a command was specified to run after, run it
						if (mPostRespawnCommand != null) {
							Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), mPostRespawnCommand);
						}

						// If we are tracking spawners for this structure, reset the count
						if (mSpawnerBreakTrigger != null) {
							mSpawnerBreakTrigger.resetCount();
						}

						if (!mForcedRespawn) {
							mLastPlayerRespawn = null;
						}

						if (mConquered && !mForcedRespawn) {
							mMinimumRespawnTime = Math.min(mMinimumRespawnTime + 5 * 60 * 20, mRespawnTime / 2);
						} else if (!mForcedRespawn) {
							mMinimumRespawnTime = Math.max(mMinimumRespawnTime - 5 * 60 * 20, 0);
						}

						mForcedRespawn = false;
						mConquered = false;
					}
				}.runTask(mPlugin);
			}
		}.runTaskAsynchronously(mPlugin);
	}

	public void forcedRespawn(Player player) {
		if (player.hasMetadata("ForceResetPOI") && mOuterBounds.within(player.getLocation().toVector())) {
			player.removeMetadata("ForceResetPOI", mPlugin);
		} else {
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must be nearby the POI to force its respawn.");
			return;
		}

		if (mForcedRespawn) {
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + mName + " is already forced to respawn!");
		} else {
			if (mSpawnerBreakTrigger.getSpawnerRatio() <= 0) {
				if (player.getDisplayName() == mLastPlayerRespawn) {
					player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You cannot force a POI to respawn twice in a row.");
				} else {
					mLastPlayerRespawn = player.getDisplayName();
					mForcedRespawn = true;
					mTicksLeft = 5 * 20 * 60;
					for (Player p : Bukkit.getOnlinePlayers()) {
						if (mOuterBounds.within(p.getLocation().toVector())) {
							player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + mName + " has been forced to respawn in 5 minutes!");
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

		if (mInnerBounds.within(player.getLocation().toVector())) {
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
		if (mConquered) {
			player.setMetadata("ForceResetPOI", new FixedMetadataValue(mPlugin, mConfigLabel));
			player.spigot().sendMessage(clickable);
		}

		//Remove tag that allows command to be run 10s later
		new BukkitRunnable() {
			@Override
			public void run() {
				if (player.hasMetadata("ForceResetPOI")) {
					player.removeMetadata("ForceResetPOI", mPlugin);
				}
			}
		}.runTaskLater(mPlugin, 10 * 20);
	}

	public int getTicksLeft() {
		return mTicksLeft;
	}

	public boolean isNearby(Player player) {
		if (mOuterBounds.within(player.getLocation().toVector())) {
			return true;
		}
		return false;
	}

	public boolean isNearby(Location loc) {
		return isNearby(loc);
	}

	public boolean isWithin(Player player) {
		if (mInnerBounds.within(player.getLocation().toVector())) {
			return true;
		}
		return false;
	}

	public boolean isWithin(Location loc) {
		return isNearby(loc);
	}

	public void tellRespawnTimeIfNearby(Player player) {
		if (isNearby(player)) {
			tellRespawnTime(player);
		}
	}

	public void setRespawnTimer(int ticksUntilRespawn) {
		if (ticksUntilRespawn <= 0) {
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
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (mOuterBounds.within(player.getLocation().toVector())) {
					tellRespawnTime(player);
				}
			}
		}

		mTicksLeft -= ticks;

		if (mForcedRespawn) {
			boolean empty = true;
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (mInnerBounds.within(player.getLocation().toVector())) {
					empty = false;
				}
			}
			if (empty) {
				mTicksLeft = 0;
			}
		}

		if (mTicksLeft < 0) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.getGameMode() != GameMode.SPECTATOR &&
				    mOuterBounds.within(player.getLocation().toVector())) {
					respawn();
					break;
				}
			}
		}
	}

	// This event is called every time a spawner is broken anywhere
	// Have to test that it was within this structure
	public void spawnerBreakEvent(Vector vec) {
		// Only care about tracking spawners if there is a trigger
		if (mSpawnerBreakTrigger != null && mInnerBounds.within(vec)) {
			mSpawnerBreakTrigger.spawnerBreakEvent(this);
		}
	}

	public void setSpawnerBreakTrigger(SpawnerBreakTrigger trigger) {
		mSpawnerBreakTrigger = trigger;
	}

	public void conquerStructure() {
		// Count how long it took to conquer POI, only set to zero if greater than minimum respawn time
		int ticksToConquer = mRespawnTime - mTicksLeft;
		mTicksLeft = ticksToConquer < mMinimumRespawnTime ? mMinimumRespawnTime - ticksToConquer : 0;

		mConquered = true;
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getGameMode() != GameMode.SPECTATOR &&
			    mOuterBounds.within(player.getLocation().toVector())) {
				player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + mName +" has been conquered! It will respawn once all players leave the area.");
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
