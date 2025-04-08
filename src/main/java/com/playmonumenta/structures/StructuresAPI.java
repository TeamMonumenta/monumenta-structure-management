package com.playmonumenta.structures;

import com.fastasyncworldedit.core.extent.clipboard.DiskOptimizedClipboard;
import com.fastasyncworldedit.core.extent.processor.lighting.RelightMode;
import com.playmonumenta.structures.utils.CommandUtils;
import com.playmonumenta.structures.utils.MSLog;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Furnace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class StructuresAPI {
	public static final String FORMAT = "sponge";

	/**
	 * Convenience function to combine both loadStructure() and pasteStructure() into one operation.
	 * <p>
	 * Must be called from main thread, will return immediately and do its work on an async thread
	 * <p>
	 * See the details of those functions for more information
	 *
	 * @deprecated
	 * This old version defaults to not loading biomes, caller should specify what they prefer using alternate API
	 */
	@Deprecated
	public static CompletableFuture<Void> loadAndPasteStructure(@Nonnull String path, @Nonnull Location loc, boolean includeEntities) {
		return loadAndPasteStructure(path, loc, includeEntities, false);
	}

	/**
	 * Convenience function to combine both loadStructure() and pasteStructure() into one operation.
	 * <p>
	 * Must be called from main thread, will return immediately and do its work on an async thread
	 * <p>
	 * See the details of those functions for more information
	 */
	public static CompletableFuture<Void> loadAndPasteStructure(@Nonnull String path, @Nonnull Location loc, boolean includeEntities, boolean includeBiomes) {
		/* Clone the input variable to make sure the caller doesn't change it while we're still loading */
		Location pasteLoc = loc.clone();

		return loadStructure(path).thenCompose((clipboard) -> pasteStructure(clipboard, pasteLoc, includeEntities, includeBiomes));
	}

	/**
	 * Loads a structure from the disk and returns it.
	 * <p>
	 * Must be called from main thread, will return immediately and do its work on an async thread
	 *
	 * @param path Relative path under the structures/ folder of the structure to load, not including the extension
	 *
	 * @return Returns a future which will be completed on the main thread when loading is complete or an error occurs.
	 *         Suggest chaining on .whenComplete((clipboard, ex) -> your code) to consume the result on the main thread
	 *         clipboard will be non-null on success, otherwise ex will be a non-null exception if something went wrong
	 */
	public static CompletableFuture<BlockArrayClipboard> loadStructure(@Nonnull String path) {
		CompletableFuture<BlockArrayClipboard> future = new CompletableFuture<>();

		MSLog.fine("loadStructure: Started loading structure '" + path + "'");
		Bukkit.getScheduler().runTaskAsynchronously(StructuresPlugin.getInstance(), () -> {
			MSLog.fine("loadStructure: In async loading structure '" + path + "'");
			final BlockArrayClipboard clipboard;

			try {
				File file = CommandUtils.getAndValidateSchematicPath(StructuresPlugin.getInstance(), path, true);

				ClipboardFormat format = ClipboardFormats.findByAlias(FORMAT);
				if (format == null) {
					future.completeExceptionally(new Exception("Could not find structure format " + FORMAT));
					return;
				}
				Clipboard newClip = format.load(file);
				if (newClip instanceof BlockArrayClipboard) {
					clipboard = (BlockArrayClipboard) newClip;
				} else if (newClip instanceof DiskOptimizedClipboard) {
					clipboard = ((DiskOptimizedClipboard) newClip).toClipboard();
				} else {
					future.completeExceptionally(new Exception("Loaded unknown clipboard type: " + newClip.getClass()));
					return;
				}
				MSLog.fine("loadStructure: Async loaded structure '" + path + "'");

				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					MSLog.fine("loadStructure: Completing future on main thread for '" + path + "'");
					future.complete(clipboard);
					MSLog.fine("loadStructure: Loading complete for '" + path + "'");
				});
			} catch (Exception ex) {
				MSLog.fine("loadStructure: Async caught exception loading '" + path + "': " + ex.getMessage());
				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					MSLog.fine("loadStructure: Completing future with exception for '" + path + "': " + ex.getMessage());
					future.completeExceptionally(ex);
					MSLog.fine("loadStructure: Loading complete/failed for '" + path + "'");
				});
			}
		});

		return future;
	}

	/**
	 * Save a structure given a bounding box at the specified path.
	 * <p>
	 * Must be called from main thread, will return immediately and do its work on an async thread
	 * <p>
	 * XXX NOTE - even though most of the work is done async, if you .get() on this future on the main thread, the server will deadlock and crash
	 *
	 * @param path Relative path under the structures/ folder of the structure to load, not including the extension
	 * @param loc1 One corner of the bounding box to save
	 * @param loc2 The opposite corner
	 *
	 * @return Returns a future which will be completed on the main thread when the operation is complete or an error occurs.
	 *         Suggest chaining on .whenComplete((unused, ex) -> your code) to continue after the operation is complete
	 *         unused will always be null, ex will be a non-null exception if something went wrong
	 */
	public static CompletableFuture<Void> copyAreaAndSaveStructure(@Nonnull String path, @Nonnull Location loc1, @Nonnull Location loc2) {
		MSLog.fine("copyAreaAndSaveStructure: Started copying '" + path + "' at " +
				   loc1.getWorld().getName() + "(" + loc1.getBlockX() + " " + loc1.getBlockY() + " " + loc1.getBlockZ() + ")  " +
				   loc2.getWorld().getName() + "(" + loc2.getBlockX() + " " + loc2.getBlockY() + " " + loc2.getBlockZ() + ")");

		CompletableFuture<Void> future = new CompletableFuture<>();

		/* Copy locations so caller can't change them after calling API */
		Location copyLoc1 = loc1.clone();
		Location copyLoc2 = loc2.clone();

		if (!copyLoc1.getWorld().equals(copyLoc2.getWorld())) {
			MSLog.fine("copyAreaAndSaveStructure: Completing with exception for '" + path + "' due to world mismatch");
			future.completeExceptionally(new Exception("Locations must have the same world"));
			return future;
		}

		Bukkit.getScheduler().runTaskAsynchronously(StructuresPlugin.getInstance(), () -> {
			MSLog.fine("copyAreaAndSaveStructure: In async copy for '" + path + "'");

			// Create file to save under
			try (Closer closer = Closer.create()) {
				File file = CommandUtils.getAndValidateSchematicPath(StructuresPlugin.getInstance(), path, false);
				if (!file.exists()) {
					//noinspection ResultOfMethodCallIgnored
					file.getParentFile().mkdirs();
					if (!file.createNewFile()) {
						MSLog.warning("Failed to create " + path);
					}
				}
				MSLog.fine("copyAreaAndSaveStructure: Created file for '" + path + "', starting copyArea");

				Clipboard clipboard = copyArea(copyLoc1, copyLoc2).get();

				MSLog.fine("copyAreaAndSaveStructure: Area copied for '" + path + "'");

				ClipboardFormat format = ClipboardFormats.findByAlias(FORMAT);
				if (format == null) {
					throw new Exception("copyAreaAndSaveStructure: Could not find format " + FORMAT);
				}
				FileOutputStream fos = closer.register(new FileOutputStream(file));
				BufferedOutputStream bos = closer.register(new BufferedOutputStream(fos));
				ClipboardWriter writer = closer.register(format.getWriter(bos));
				writer.write(clipboard);

				MSLog.fine("copyAreaAndSaveStructure: Wrote output to file '" + path + "'");

				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					MSLog.fine("copyAreaAndSaveStructure: Completing future on main thread for '" + path + "'");
					future.complete(null);
					MSLog.fine("copyAreaAndSaveStructure: Successfully completed '" + path + "'");
				});
			} catch (Exception ex) {
				MSLog.fine("copyAreaAndSaveStructure: Caught async exception for '" + path + "': " + ex.getMessage());
				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					MSLog.fine("copyAreaAndSaveStructure: Completing future with exception for '" + path + "': " + ex.getMessage());
					future.completeExceptionally(ex);
					MSLog.fine("copyAreaAndSaveStructure: Failed to complete '" + path + "': " + ex.getMessage());
				});
			}
		});

		return future;
	}

	/**
	 * Copies a bounding box to a clipboard that can be used with pasteStructure().
	 * <p>
	 * Must be called from main thread, will return immediately and do its work on an async thread
	 *
	 * @param loc1 One corner of the bounding box to save
	 * @param loc2 The opposite corner
	 */
	public static CompletableFuture<BlockArrayClipboard> copyArea(@Nonnull Location loc1, @Nonnull Location loc2) {
		CompletableFuture<BlockArrayClipboard> future = new CompletableFuture<>();

		/* Copy locations so caller can't change them after calling API */
		Location copyLoc1 = loc1.clone();
		Location copyLoc2 = loc2.clone();

		Plugin plugin = StructuresPlugin.getInstance();
		if (plugin == null) {
			future.completeExceptionally(new Exception("MonumentaStructureManagement plugin isn't loaded"));
			return future;
		}

		if (!copyLoc1.getWorld().equals(copyLoc2.getWorld())) {
			future.completeExceptionally(new Exception("Locations must have the same world"));
			return future;
		}

		// Parse the coordinates of the structure to save
		BlockVector3 pos1 = BlockVector3.at(copyLoc1.getBlockX(), copyLoc1.getBlockY(), copyLoc1.getBlockZ());
		BlockVector3 pos2 = BlockVector3.at(copyLoc2.getBlockX(), copyLoc2.getBlockY(), copyLoc2.getBlockZ());

		BlockVector3 minPos = pos1.getMinimum(pos2);
		BlockVector3 maxPos = pos1.getMaximum(pos2);

		org.bukkit.World world = copyLoc1.getWorld();
		World faweWorld = new BukkitWorld(world);
		CuboidRegion cReg = new CuboidRegion(faweWorld, minPos, maxPos);

		markAndLoadChunks(world, cReg, null).whenComplete((unused, ex) -> {
			if (ex != null) {
				future.completeExceptionally(ex);
			} else {
				/* Copy on an async thread now that all chunks are loaded */
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					try {
						BlockArrayClipboard clipboard = new BlockArrayClipboard(cReg);
						clipboard.setOrigin(cReg.getMinimumPoint());

						/* Copy the region (including entities and biomes) into the clipboard object */
						EditSession extent = WorldEdit.getInstance().newEditSessionBuilder()
							.world(faweWorld)
							.fastMode(true)
							.combineStages(true)
							.changeSetNull()
							.checkMemory(false)
							.allowedRegionsEverywhere()
							.limitUnlimited()
							.build();
						ForwardExtentCopy copy = new ForwardExtentCopy(extent, cReg, clipboard, cReg.getMinimumPoint());
						copy.setCopyingEntities(true);
						copy.setCopyingBiomes(true);
						Operations.completeLegacy(copy);

						Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> future.complete(clipboard));

						/* 10s later, unmark all chunks as force loaded */
						Bukkit.getScheduler().runTaskLater(plugin, () -> unmarkChunksAsync(world, cReg), 200);
					} catch (Exception e) {
						future.completeExceptionally(e);
					}
				});
			}
		});

		return future;
	}

	/**
	 * Pastes a structure at the given location, ignoring structure void similarly to vanilla structure blocks.
	 * <p>
	 * Must be called from main thread, will return immediately and do its work on an async thread
	 *
	 * @deprecated
	 * This old version defaults to not loading biomes, caller should specify what they prefer using alternate API
	 */
	@Deprecated
	public static CompletableFuture<Void> pasteStructure(@Nonnull BlockArrayClipboard clipboard, @Nonnull Location loc, boolean includeEntities) {
		return pasteStructure(clipboard, loc, includeEntities, false);
	}

	/**
	 * Pastes a structure at the given location, ignoring structure void similarly to vanilla structure blocks.
	 * <p>
	 * Must be called from main thread, will return immediately and do its work on an async thread
	 */
	public static CompletableFuture<Void> pasteStructure(@Nonnull BlockArrayClipboard clipboard, @Nonnull Location loc, boolean includeEntities, boolean includeBiomes) {
		/*
		 * This is the future given to the caller - when it's completed it should be safe to interact with the pasted area
		 * In particular there is a 6s delay after pasting before this is marked as completed
		 */
		CompletableFuture<Void> future = new CompletableFuture<>();

		Plugin plugin = StructuresPlugin.getInstance();
		if (plugin == null) {
			future.completeExceptionally(new Exception("MonumentaStructureManagement plugin isn't loaded"));
			return future;
		}

		/* Clone the input variable to make sure the caller doesn't change it while we're still loading */
		Location pasteLoc = loc.clone();

		/*
		 * This future is used to signal the pasting system that this schematic has completed and the next one can start pasting
		 * This does not have the 6s delay returned to the user
		 */
		CompletableFuture<Void> signal = new CompletableFuture<>();

		PENDING_TASKS.add(new PendingTask(signal, () -> {
			final long initialTime = System.currentTimeMillis(); // <-- START

			final Region sourceRegion = clipboard.getRegion();
			final BlockVector3 size = sourceRegion.getMaximumPoint().subtract(sourceRegion.getMinimumPoint());
			final org.bukkit.World world = pasteLoc.getWorld();
			final BlockVector3 to = BlockVector3.at(pasteLoc.getBlockX(), pasteLoc.getBlockY(), pasteLoc.getBlockZ());
			final Vector pos1 = new Vector(to.getX(), to.getY(), (double)to.getZ());
			final Vector pos2 = pos1.clone().add(new Vector(size.getX() + 1, size.getY() + 1, size.getZ() + 1));
			final BoundingBox box = new BoundingBox(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());

			/*
			 * Clipboards seem to be offset at their original save location now, rather than at 0 0 0
			 * This offset can be added to a relative position to get the correct location within the clipboard
			 */
			final Region shiftedRegion = clipboard.getRegion().clone();
			final BlockVector3 clipboardAddOffset = shiftedRegion.getMinimumPoint();
			final Vector clipboardAddOffsetVec = new Vector(clipboardAddOffset.getX(), clipboardAddOffset.getY(), clipboardAddOffset.getZ());
			shiftedRegion.shift(clipboardAddOffset.multiply(-1, -1, -1));
			shiftedRegion.shift(to);

			/* Set of positions (relative to the clipboard / origin) that should not be overwritten when pasting */
			final Set<Long> noLoadPositions = new HashSet<>();

			/* This chunk consumer removes entities and sets spawners/brewing stands/furnaces to air */
			final Consumer<Chunk> chunkConsumer = (final Chunk chunk) -> {
				for (final BlockState state : chunk.getTileEntities(true)) {
					if (state instanceof CreatureSpawner || state instanceof BrewingStand || state instanceof Furnace || state instanceof Chest || state instanceof ShulkerBox) {
						final Location sLoc = state.getLocation();
						final BlockVector3 relPos = BlockVector3.at(sLoc.getBlockX(), sLoc.getBlockY(), sLoc.getBlockZ()).subtract(to).add(clipboardAddOffset);
						if (box.contains(sLoc.toVector()) && !clipboard.getBlock(relPos).getBlockType().equals(BlockTypes.STRUCTURE_VOID)) {
							if (state instanceof CreatureSpawner || state instanceof BrewingStand || state instanceof Furnace) {
								// TODO: Work around a bug in FAWE that corrupts these blocks if they're not removed first
								final Block block = state.getBlock();

								/* Set block to air and then dirt... which works around somehow the tile entity data being left behind */
								if (state instanceof BrewingStand || state instanceof Furnace) {
									Inventory inv;
									if (state instanceof BrewingStand) {
										inv = ((BrewingStand)state).getInventory();
									} else {
										inv = ((Furnace)state).getInventory();
									}
									for (int i = 0; i < inv.getSize(); i++) {
										inv.setItem(i, new ItemStack(Material.AIR));
									}
								}
								block.setType(Material.AIR);
								block.setType(Material.DIRT);
							} else if (state instanceof ShulkerBox) {
								/* Never overwrite shulker boxes */
								final int relX = state.getX() - to.getX();
								final int relY = state.getY() - to.getY();
								final int relZ = state.getZ() - to.getZ();
								noLoadPositions.add(compressToLong(relX, relY, relZ));
							}
						}
					}
				}

				if (includeEntities) {
					for (final Entity entity : chunk.getEntities()) {
						if (box.contains(entity.getLocation().toVector()) && entityShouldBeRemoved(entity)) {
							final Vector relPos = entity.getLocation().toVector().subtract(pos1).add(clipboardAddOffsetVec);
							if (!clipboard.getBlock(BlockVector3.at(relPos.getBlockX(), relPos.getBlockY(), relPos.getBlockZ())).getBlockType().equals(BlockTypes.STRUCTURE_VOID)) {
								entity.remove();
							}
						}
					}
				}
			};

			markAndLoadChunks(world, shiftedRegion, chunkConsumer).whenComplete((unused, ex) -> {
				if (ex != null) {
					signal.completeExceptionally(ex);
					future.completeExceptionally(ex);
				} else {
					/* Actually load the structure asynchronously now that all the chunks have been processed for entities / blocks that shouldn't be replaced */
					Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
						MSLog.finer(() -> "Initial processing took " + (System.currentTimeMillis() - initialTime) + " milliseconds (mostly async)"); // STOP -->

						final long pasteTime = System.currentTimeMillis(); // <-- START

						try (EditSession extent = WorldEdit.getInstance().newEditSessionBuilder()
							.world(new BukkitWorld(world))
							.fastMode(true)
							.combineStages(true)
							.changeSetNull()
							.checkMemory(false)
							.allowedRegionsEverywhere()
							.limitUnlimited()
							.relightMode(RelightMode.ALL)
							.build()) {

							/*
							 * Filter function to skip some blocks from overwriting what exists in the world
							 * If this function returns true, this location will be overwritten
							 */
							final RegionFunction filterFunction = position -> {
								final BlockType newBlockType = clipboard.getBlock(position).getBlockType();

								if (newBlockType == null || !newBlockType.equals(BlockTypes.STRUCTURE_VOID)) {
									// This position is not in structure void in the clipboard
									if (!noLoadPositions.contains(compressToLong(position.getBlockX(), position.getBlockY(), position.getBlockZ()))) {
										// This position is not in the list of blocks that should not be overwritten
										return true;
									}
								}

								// Don't overwrite by default
								return false;
							};


							final ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, clipboard.getRegion(), clipboard.getOrigin(), extent, to);
							copy.setCopyingBiomes(includeBiomes);
							copy.setFilterFunction(filterFunction);
							copy.setCopyingEntities(includeEntities);
							Operations.completeBlindly(copy);
						}
						MSLog.finer(() -> "Loading structure took " + (System.currentTimeMillis() - pasteTime) + " milliseconds (async)"); // STOP -->

						/* Add a 5 tick delay until the next task for shifting */
						Bukkit.getScheduler().runTaskLater(plugin, () -> signal.complete(null), 5);

						/* 6s later, signal caller that loading is complete */
						Bukkit.getScheduler().runTaskLater(plugin, () -> future.complete(null), 120);

						/* 10s later, unmark all chunks as force loaded */
						Bukkit.getScheduler().runTaskLater(plugin, () -> unmarkChunksAsync(world, shiftedRegion), 200);

					});
				}
			});
		}));

		ensureTask(plugin);

		return future;
	}

	/*
	 * Causes an area of chunks to be loaded and then kept loaded by adding plugin tickets to them.
	 *
	 * Will run the provided chunk consumer on each chunk as they load. Will wait for all chunks to finish loading, then
	 * complete the future on the main thread. This lets the caller chain on .whenComplete((unused, ex) -> code) to continue
	 * processing when all chunks are loaded.
	 *
	 * Must be called from main thread, will return immediately and do its work on an async thread
	 *
	 * Has a 600 tick timeout - if chunks fail to load in that time the future will complete with an exception and no attempt
	 * to repair this will be made. Those chunks will likely continue to load afterward, and will stay loaded...
	 */
	public static CompletableFuture<Void> markAndLoadChunks(org.bukkit.World world, Region region, @Nullable Consumer<Chunk> consumer) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		Plugin plugin = StructuresPlugin.getInstance();
		if (plugin == null) {
			future.completeExceptionally(new Exception("MonumentaStructureManagement plugin isn't loaded"));
			return future;
		}

		final Set<BlockVector2> chunks = region.getChunks();
		final AtomicInteger numRemaining = new AtomicInteger(chunks.size());

		/* This chunk consumer adds plugin chunk tickets to all the appropriate chunks to keep them loaded */
		final Consumer<Chunk> chunkConsumer = (final Chunk chunk) -> {
			numRemaining.decrementAndGet();

			/*
			 * Mark this chunk so it will stay loaded. Keep a reference count so chunks definitely stay loaded, even when
			 * multiple overlapping structures use it simultaneously
			 */
			WorldChunkKey key = new WorldChunkKey(world.getUID(), chunk.getChunkKey());
			Integer references = CHUNK_TICKET_REFERENCE_COUNT.get(key);
			if (references == null || references == 0) {
				references = 1;
				if (!chunk.addPluginChunkTicket(plugin)) {
					plugin.getLogger().warning("BUG: Plugin already has chunk ticket for " + chunk.getX() + "," + chunk.getZ());
				}
			} else {
				references += 1;
			}
			CHUNK_TICKET_REFERENCE_COUNT.put(key, references);

			/* Chain on the caller provided consumer */
			if (consumer != null) {
				consumer.accept(chunk);
			}
		};

		/* Load all the chunks in the region and run the chunk consumer */
		for (final BlockVector2 chunkCoords : chunks) {
			world.getChunkAtAsync(chunkCoords.getX(), chunkCoords.getZ(), chunkConsumer);
		}

		new BukkitRunnable() {
			int mNumTicksWaited = 0;
			@Override
			public void run() {
				mNumTicksWaited++;
				if (mNumTicksWaited >= 30 * 20) {
					String msg = "Timed out waiting for chunks to load";
					plugin.getLogger().severe(msg);
					this.cancel();
					future.completeExceptionally(new Exception(msg));
					return;
				}
				if (numRemaining.get() == 0) {
					this.cancel();

					future.complete(null);
				}
			}
		}.runTaskTimer(plugin, 0, 1);

		return future;
	}

	/*
	 * Unmarks chunks so that they can be unloaded. (the opposite of markAndLoadChunks)
	 *
	 * Must be called from main thread
	 *
	 * Is intelligent - if multiple marks are placed on the same chunk it won't be unloaded until
	 * the last user unmarks them.
	 */
	public static void unmarkChunksAsync(org.bukkit.World world, Region region) {
		Plugin plugin = StructuresPlugin.getInstance();
		if (plugin == null) {
			/* Couldn't have marked things with this plugin anyway since it's unloaded */
			return;
		}

		for (final BlockVector2 chunkCoords : region.getChunks()) {
			final Consumer<Chunk> consumer = (final Chunk chunk) -> {
				WorldChunkKey key = new WorldChunkKey(world.getUID(), chunk.getChunkKey());
				Integer references = CHUNK_TICKET_REFERENCE_COUNT.remove(key);
				if (references == null || references <= 0) {
					plugin.getLogger().warning("BUG: Chunk reference was cleared before it should have been: " + chunk.getX() + "," + chunk.getZ());
				} else if (references == 1) {
					if (!chunk.removePluginChunkTicket(plugin)) {
						plugin.getLogger().warning("BUG: Chunk ticket was already removed: " + chunk.getX() + "," + chunk.getZ());
					}
				} else {
					CHUNK_TICKET_REFERENCE_COUNT.put(key, references - 1);
				}
			};
			world.getChunkAtAsync(chunkCoords.getX(), chunkCoords.getZ(), consumer);
		}
	}

	private static class PendingTask {
		private final CompletableFuture<Void> mSignal;
		private final Runnable mStartTask;

		protected PendingTask(CompletableFuture<Void> signal, Runnable startTask) {
			mSignal = signal;
			mStartTask = startTask;
		}
	}

	public static class WorldChunkKey {
		private final UUID mUUID;
		private final long mChunkKey;

		public WorldChunkKey(UUID uuid, long chunkKey) {
			mUUID = uuid;
			mChunkKey = chunkKey;
		}

		@Override
		public int hashCode() {
			return (int)(mUUID.getMostSignificantBits() * mChunkKey);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof WorldChunkKey other)) {
				return false;
			} else {
				return mUUID.equals(other.mUUID) && mChunkKey == other.mChunkKey;
			}
		}
	}

	private static final HashMap<WorldChunkKey, Integer> CHUNK_TICKET_REFERENCE_COUNT = new HashMap<>();
	private static final Deque<PendingTask> PENDING_TASKS = new ConcurrentLinkedDeque<>();
	private static @Nullable BukkitRunnable RUNNING_TASK = null;

	private static final EnumSet<EntityType> keptEntities = EnumSet.of(
		EntityType.PLAYER,
		EntityType.DROPPED_ITEM,
		EntityType.EXPERIENCE_ORB,
		EntityType.VILLAGER,
		EntityType.TRIDENT,
		EntityType.HORSE,
		EntityType.COW,
		EntityType.PIG,
		EntityType.SHEEP,
		EntityType.CHICKEN
	);

	private static boolean entityShouldBeRemoved(final Entity entity) {
		/* Keep some entity types always */
		if (keptEntities.contains(entity.getType())) {
			return false;
		}

		/* Keep armor stands that have a name, are markers, or have tags */
		if (entity instanceof ArmorStand stand) {
			if ((stand.getCustomName() != null && !stand.getCustomName().isEmpty())
				|| stand.isMarker()
				|| !stand.getScoreboardTags().isEmpty()) {
				return false;
			}
		}

		/* Keep tamable critters that have an owner */
		if (entity instanceof Tameable critter) {
			if (critter.getOwner() != null) {
				return false;
			}
		}

		/* Keep entities with the appropriate tag */
		if (entity.getScoreboardTags().contains("RespawnPersistent")) {
			return false;
		}

		/* Keep entities that a player is riding (mostly boats - horses are already not removed in any case) */
		if (entity.getPassengers().stream().anyMatch(passenger -> passenger instanceof Player)) {
			return false;
		}

		/* Remove otherwise */
		return true;
	}

	private static Long compressToLong(final int x, final int y, final int z) {
		return (((long) (x & ((1 << 24) - 1))) << 40) |
				(((long) (y & ((1 << 16) - 1))) << 24) |
				((long) (z & ((1 << 24) - 1)));
	}

	private static void ensureTask(Plugin plugin) {
		if (RUNNING_TASK != null) {
			return;
		}

		RUNNING_TASK = new BukkitRunnable() {
			@Override
			public void run() {
				while (true) {
					PendingTask task = PENDING_TASKS.pollFirst();

					if (task != null) {
						try {
							Bukkit.getScheduler().runTask(plugin, task.mStartTask);
							task.mSignal.get(45, TimeUnit.SECONDS);
						} catch (Exception ex) {
							plugin.getLogger().severe("Structure task took longer than 45s to complete! Continuing to the next task. Exception: " + ex.getMessage());
						}
					}

					try {
						Thread.sleep(50);
					} catch (Exception ex) {
						plugin.getLogger().info("Structure loading task sleep was interrupted");
						RUNNING_TASK = null;
						break;
					}
				}
			}
		};
		RUNNING_TASK.runTaskAsynchronously(plugin);
	}
}
