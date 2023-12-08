package com.playmonumenta.structures.managers;

import com.fastasyncworldedit.core.util.collection.BlockSet;
import com.fastasyncworldedit.core.util.collection.MemBlockSet;
import com.playmonumenta.scriptedquests.zones.Zone;
import com.playmonumenta.scriptedquests.zones.ZoneLayer;
import com.playmonumenta.structures.StructuresAPI;
import com.playmonumenta.structures.StructuresPlugin;
import com.playmonumenta.structures.utils.MSLog;
import com.playmonumenta.structures.utils.MessagingUtils;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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
	private @Nullable String mPostRespawnCommand;   // Command run via the console after respawning structure
	private boolean mForcedRespawn;       // Was this set to have a forced respawn via compass
	private NearbyState mPlayerNearbyLastTick; // Was there a player nearby last tick while respawn time was < 0?
	private boolean mConquered;           // Is the POI conquered
	private @Nullable UUID mLastPlayerRespawn; // Player who last forced a respawn
	private int mTimesPlayerSpawned;      // How many times in a row it's been reset through conquered or force respawn

	/**
	 * A block set of structure void blocks in this structure. Coordinates are relative to the structure, e.g. (0,0,0) is the lowest (x,y,z) block in the structure.
	 */
	private @Nullable BlockSet mStructureVoidBlocks = null;

	// Path String -> BlockArrayClipboard maps
	private final List<String> mGenericVariants = new ArrayList<>();
	private final List<String> mSpecialVariants = new ArrayList<>();

	// Which structure will be spawned next
	// If this is null, one of the genericVariants will be chosen randomly
	// If this is a path, it must be one of mSpecialVariants (not in generic variants!)
	private @Nullable String mNextRespawnPath;

	// If this is non-null, then some action happens when players break some number of spawners
	private @Nullable SpawnerBreakTrigger mSpawnerBreakTrigger;

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
				spawnerBreakTrigger = SpawnerBreakTrigger.fromConfig(
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

	public static CompletableFuture<RespawningStructure> withParameters(StructuresPlugin plugin,
	                                                                    World world,
	                                                                    int extraRadius,
	                                                                    String configLabel,
	                                                                    String name,
	                                                                    List<String> genericPaths,
	                                                                    Vector loadPos,
	                                                                    int respawnTime,
	                                                                    int ticksLeft,
	                                                                    @Nullable String postRespawnCommand,
	                                                                    @Nullable List<String> specialPaths,
	                                                                    @Nullable String nextRespawnPath,
	                                                                    @Nullable SpawnerBreakTrigger spawnerBreakTrigger) {

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
			                                                        postRespawnCommand,
					nextRespawnPath, spawnerBreakTrigger);

			// Load the first schematic to get its size
			return StructuresAPI.loadStructure(structure.mGenericVariants.get(0)).thenApply((clipboard) -> {
				if (specialPaths != null) {
					structure.mSpecialVariants.addAll(specialPaths);
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

				// save locations of structure void blocks
				int size1 = 1 + (Math.max(clipboard.getDimensions().getBlockX(), Math.max(clipboard.getDimensions().getBlockY(), clipboard.getDimensions().getBlockZ())) >> 4);
				MemBlockSet structureVoidBlocks = new MemBlockSet(size1, 0, 0, 0, 32);
				BlockType structureVoid = BlockType.REGISTRY.get("minecraft:structure_void");
				if (structureVoid != null) {
					BlockVector3 minimumPoint = clipboard.getMinimumPoint();
					for (BlockVector3 pos : clipboard) {
						// cannot use pos directly, as its x/y/z values are not properly set, only the blockX/Y/Z values
						if (structureVoid.equals(clipboard.getBlock(pos.getBlockX() + minimumPoint.getBlockX(), pos.getBlockY() + minimumPoint.getBlockY(), pos.getBlockZ() + minimumPoint.getBlockZ()).getBlockType())) {
							structureVoidBlocks.add(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
						}
					}
				} else {
					MSLog.warning("Cannot find minecraft:structure_void in registry");
				}
				structure.mStructureVoidBlocks = structureVoidBlocks;

				structure.registerZone();

				return structure;
			});
		} catch (Exception ex) {
			future.completeExceptionally(ex);
			return future;
		}
	}

	private RespawningStructure(StructuresPlugin plugin,
	                            World world,
	                            int extraRadius,
	                            String configLabel,
	                            String name,
	                            List<String> genericPaths,
	                            Vector loadPos,
	                            int respawnTime,
	                            int ticksLeft,
	                            @Nullable String postRespawnCommand,
	                            @Nullable String nextRespawnPath,
	                            @Nullable SpawnerBreakTrigger spawnerBreakTrigger) {
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

		mGenericVariants.addAll(genericPaths);

		// Set the next respawn path (or not if null)
		activateSpecialStructure(nextRespawnPath);
	}

	public String getInfoString() {
		return "name='" + mName + "' pos=(" + mLoadPos.getBlockX() + " " +
		       mLoadPos.getBlockY() + " " + mLoadPos.getBlockZ() +
		       ") paths={" + String.join(" ", mGenericVariants) + "} period=" + mRespawnTime + " ticksLeft=" +
		       mTicksLeft +
		       (mPostRespawnCommand == null ? "" : " respawnCmd='" + mPostRespawnCommand + "'") +
		       (mSpecialVariants.isEmpty() ? "" : " specialPaths={" + String.join(" ", mSpecialVariants) + "}") +
		       (mSpawnerBreakTrigger == null ? "" : " spawnerTrigger={" + mSpawnerBreakTrigger.getInfoString() + "}");
	}

	public void activateSpecialStructure(@Nullable String nextRespawnPath) {
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

		StructuresAPI.loadAndPasteStructure(respawnPath, new Location(mWorld, mLoadPos.getBlockX(), mLoadPos.getBlockY(), mLoadPos.getBlockZ()), true).whenComplete((unused, exception) -> {
			if (exception != null) {
				mPlugin.getLogger().severe("Failed to respawn structure '" + mConfigLabel + "'");
				exception.printStackTrace();
			} else if (mPostRespawnCommand != null) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), mPostRespawnCommand);
			}
		});
	}

	public void forcedRespawn(Player player, boolean ignoreDefaultSpawnerCount) {
		if (!isNearby(player)) {
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must be nearby the POI to force its respawn.");
			return;
		}

		if (mForcedRespawn) {
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + mName + " is already forced to respawn!");
		} else {
			if (isConquered() || ignoreDefaultSpawnerCount) {
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
						for (Player p : mWorld.getPlayers()) {
							if (isNearby(p)) {
								p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + mName + " will forcibly respawn in "
										+ minutesLeft + " minutes!");
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
		Component message = Component.text(
			mTicksLeft <= 0 ? mName + " will respawn when empty." : mName + " is respawning in " + MessagingUtils.durationToString(mTicksLeft),
			mTicksLeft <= 600 ? NamedTextColor.RED : NamedTextColor.GREEN
		).decorate(TextDecoration.BOLD);

		boolean within = isWithin(player);
		if (within) {
			message = message.append(Component.text(" [Within]"));
		}

		if (mForcedRespawn || mNextRespawnPath != null) {
			message = message.append(Component.text(" [Forced Respawn]", NamedTextColor.RED).decorate(TextDecoration.BOLD));
		}

		player.sendMessage(message);
		if (within) {
			long otherPlayersWithin = player.getWorld().getPlayers().stream()
				                          .filter(p -> p != player && isWithin(p) && p.getGameMode() != GameMode.SPECTATOR && p.getGameMode() != GameMode.CREATIVE)
				                          .count();
			if (otherPlayersWithin > 0) {
				player.sendMessage(Component.text((otherPlayersWithin == 1 ? "There is one other player here." : "There are " + otherPlayersWithin + " other players here."), NamedTextColor.AQUA).decoration(TextDecoration.BOLD, false));
			}
		}
		if (mConquered && !mForcedRespawn) {
			player.sendMessage(Component.text("[Force " + mName + " to respawn]", NamedTextColor.LIGHT_PURPLE)
				                   .clickEvent(ClickEvent.runCommand("/compassrespawn " + mConfigLabel)));
		}
	}

	public int getTicksLeft() {
		return mTicksLeft;
	}

	public boolean isNearby(Player player) {
		return isNearby(player.getLocation());
	}

	public boolean isNearby(Location location) {
		return mWorld.equals(location.getWorld()) && mOuterBounds.within(location.toVector());
	}

	public boolean isWithin(Player player) {
		return isWithin(player.getLocation());
	}

	public boolean isWithin(Location location) {
		Vector playerLoc = location.toVector();
		return mWorld.equals(location.getWorld()) && mInnerBounds.within(playerLoc)
			       && (mStructureVoidBlocks == null || !mStructureVoidBlocks.contains(playerLoc.getBlockX() - mInnerBounds.mLowerCorner.getBlockX(),
			playerLoc.getBlockY() - mInnerBounds.mLowerCorner.getBlockY(), playerLoc.getBlockZ() - mInnerBounds.mLowerCorner.getBlockZ()));
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

	public void setPostRespawnCommand(@Nullable String postRespawnCommand) {
		mPostRespawnCommand = postRespawnCommand;
	}

	public Map<String, Object> getConfig() {
		Map<String, Object> configMap = new LinkedHashMap<>();

		configMap.put("name", mName);
		configMap.put("structure_paths", mGenericVariants);
		configMap.put("x", mLoadPos.getBlockX());
		configMap.put("y", mLoadPos.getBlockY());
		configMap.put("z", mLoadPos.getBlockZ());
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

	public void setSpawnerBreakTrigger(@Nullable SpawnerBreakTrigger trigger) {
		mSpawnerBreakTrigger = trigger;
	}

	public void conquerStructure() {
		if (mForcedRespawn) {
			// POI was already scheduled to respawn forcibly - conquering does nothing at this point, no reason to increase timer
			return;
		}

		// Count how long it took to conquer POI, only set to zero if greater than minimum respawn time
		int playerCausedDelay = mTimesPlayerSpawned * 5 * 60 * 20;
		mTicksLeft = Math.min(playerCausedDelay, (mRespawnTime / 2));

		int minutesLeft = mTicksLeft / (60 * 20);
		String minutesPlural = (minutesLeft > 1) ? "s" : "";

		mConquered = true;
		for (Player player : mWorld.getPlayers()) {
			if (player.getGameMode() != GameMode.SPECTATOR &&
			    isNearby(player)) {
				if (mTicksLeft <= 0) {
					player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + mName + " has been conquered! It will respawn once all players leave the area.");
				} else {
					player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + mName + " has been conquered! It will respawn in " +
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

	public void registerZone() {
		RespawnManager respawnManager = StructuresPlugin.getRespawnManager();
		ZoneLayer zoneLayerInside = respawnManager.mZoneLayerInside;
		ZoneLayer zoneLayerNearby = respawnManager.mZoneLayerNearby;

		Zone insideZone = new Zone(zoneLayerInside,
		                           mInnerBounds.mLowerCorner.clone(),
		                           mInnerBounds.mUpperCorner.clone(),
		                           mName,
		                           new LinkedHashSet<>());
		Zone nearbyZone = new Zone(zoneLayerNearby,
		                           mOuterBounds.mLowerCorner.clone(),
		                           mOuterBounds.mUpperCorner.clone(),
		                           mName,
		                           new LinkedHashSet<>());
		zoneLayerInside.addZone(insideZone);
		zoneLayerNearby.addZone(nearbyZone);

		respawnManager.registerRespawningStructureZone(insideZone, this);
		respawnManager.registerRespawningStructureZone(nearbyZone, this);
	}
}
