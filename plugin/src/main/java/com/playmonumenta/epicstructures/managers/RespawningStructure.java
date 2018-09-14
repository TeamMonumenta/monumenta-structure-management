package com.playmonumenta.epicstructures.managers;

import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.epicstructures.utils.StructureUtils;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.regions.Region;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.World;

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
	private int mRespawnTime;             // How many ticks between respawns
	private String mPostRespawnCommand;   // Command run via the console after respawning structure

	// Path String -> BlockArrayClipboard maps
	private Map<String, BlockArrayClipboard> mGenericVariants = new HashMap<String, BlockArrayClipboard>();
	private Map<String, BlockArrayClipboard> mSpecialVariants = new HashMap<String, BlockArrayClipboard>();

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
		mTicksLeft = ticksLeft;
		mPostRespawnCommand = postRespawnCommand;
		mSpawnerBreakTrigger = spawnerBreakTrigger;

		if (mRespawnTime < 200) {
			throw new Exception("Minimum respawn_period value is 200 ticks");
		}

		// Load all of the supplied structures
		BlockArrayClipboard clipboard = null;
		for (String path : genericPaths) {
			// TODO: This is a sloppy way to get the dimensions... falling through to the last one
			clipboard = mPlugin.mStructureManager.loadSchematicClipboard("structures", path);
			mGenericVariants.put(path, clipboard);
		}
		if (clipboard == null) {
			throw new Exception("No structures specified for '" + mConfigLabel + "'");
		}

		if (specialPaths != null) {
			for (String path : specialPaths) {
				mSpecialVariants.put(path, mPlugin.mStructureManager.loadSchematicClipboard("structures", path));
			}
		}

		// TODO: Add a check that these are all the same size

		// Set the next respawn path (or not if null)
		activateSpecialStructure(nextRespawnPath);

		// Determine structure size
		Region clipboardRegion = clipboard.getRegion().clone();
		com.sk89q.worldedit.Vector structureSize =
		    clipboardRegion.getMaximumPoint().subtract(clipboardRegion.getMinimumPoint());

		// Create a bounding box for the structure itself, plus a slightly larger box to notify nearby players
		mInnerBounds = new StructureBounds(mLoadPos, mLoadPos.clone().add(new Vector(structureSize.getX(),
		                                                                             structureSize.getY(),
		                                                                             structureSize.getZ())));
		Vector extraRadiusVec = new Vector(extraRadius, extraRadius, extraRadius);
		mOuterBounds = new StructureBounds(mInnerBounds.mLowerCorner.clone().subtract(extraRadiusVec),
		                                   mInnerBounds.mUpperCorner.clone().add(extraRadiusVec));
	}

	public String getInfoString() {
		return "name='" + mName + "' pos=(" + Integer.toString((int)mLoadPos.getX()) + " " +
		       Integer.toString((int)mLoadPos.getY()) + " " + Integer.toString((int)mLoadPos.getZ()) +
			   ") paths={" + String.join(" ", mGenericVariants.keySet()) + "} period=" + Integer.toString(mRespawnTime) + " ticksleft=" +
			   Integer.toString(mTicksLeft) +
			   (mPostRespawnCommand == null ? "" : " respawnCmd='" + mPostRespawnCommand + "'") +
			   (mSpecialVariants.isEmpty() ? "" : " specialPaths={" + String.join(" ", mSpecialVariants.keySet()) + "}") +
			   (mSpawnerBreakTrigger == null ? "" : " spawnerTrigger={" + mSpawnerBreakTrigger.getInfoString() + "}");
	}

	public void activateSpecialStructure(String nextRespawnPath) throws Exception {
		if (nextRespawnPath != null && !mSpecialVariants.containsKey(nextRespawnPath)) {
			mSpecialVariants.put(nextRespawnPath, mPlugin.mStructureManager.loadSchematicClipboard("structures", nextRespawnPath));
		}
		//TODO: Check that this structure is the same size!

		mNextRespawnPath = nextRespawnPath;
	}

	public void respawn() {
		BlockArrayClipboard clipboard;
		if (mNextRespawnPath == null) {
			// No specified next path - pick a generic one at random
			List<BlockArrayClipboard> valueList = new ArrayList<BlockArrayClipboard>(mGenericVariants.values());
			clipboard = valueList.get(mRandom.nextInt(valueList.size()));
		} else {
			// Next path was specified - use it
			clipboard = mSpecialVariants.get(mNextRespawnPath);
			if (clipboard == null) {
				// This should not be possible because we check when setting mNextRespawnPath
				mPlugin.getLogger().log(Level.SEVERE, "Tried to spawn nonexistent nextRespawnPath='" + mNextRespawnPath + "'");
				return;
			}
			// Go back to generic versions after spawning this once
			mNextRespawnPath = null;
		}

		// Load the structure
		StructureUtils.paste(mPlugin, clipboard, mWorld,
		                     new com.sk89q.worldedit.Vector(mLoadPos.getX(), mLoadPos.getY(), mLoadPos.getZ()));

		// If a command was specified to run after, run it
		if (mPostRespawnCommand != null) {
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), mPostRespawnCommand);
		}

		// If we are tracking spawners for this structure, reset the count
		if (mSpawnerBreakTrigger != null) {
			mSpawnerBreakTrigger.resetCount();
		}

		mTicksLeft = mRespawnTime;
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

		if (mInnerBounds.within(player.getLocation().toVector())) {
			message += " [within]";
		}

		player.sendMessage(color + message);
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
		if (ticksUntilRespawn < 0) {
			respawn();
		} else {
			mTicksLeft = ticksUntilRespawn;
		}
	}

	public void setPostRespawnCommand(String postRespawnCommand) {
		mPostRespawnCommand = postRespawnCommand;
	}

	public Map<String, Object> getConfig() {
		Map<String, Object> configMap = new LinkedHashMap<String, Object>();

		configMap.put("name", mName);
		configMap.put("structure_paths", new ArrayList<>(mGenericVariants.keySet()));
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
			configMap.put("structure_special_paths", new ArrayList<>(mSpecialVariants.keySet()));
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
		if ((mTicksLeft >= 2400 && (mTicksLeft - ticks) < 2400) ||
		    (mTicksLeft >= 600 && (mTicksLeft - ticks) < 600)) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (mOuterBounds.within(player.getLocation().toVector())) {
					tellRespawnTime(player);
				}
			}
		}

		mTicksLeft -= ticks;

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
}
