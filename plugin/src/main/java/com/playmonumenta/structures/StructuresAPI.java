package com.playmonumenta.structures;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.bergerkiller.bukkit.common.wrappers.LongHashSet;
import com.bergerkiller.bukkit.lightcleaner.lighting.LightingService;
import com.bergerkiller.bukkit.lightcleaner.lighting.LightingService.ScheduleArguments;
import com.fastasyncworldedit.core.object.clipboard.DiskOptimizedClipboard;
import com.fastasyncworldedit.core.util.EditSessionBuilder;
import com.playmonumenta.structures.utils.CommandUtils;
import com.sk89q.worldedit.EditSession;
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
	 *
	 * See the details of those functions for more information
	 */
	public static CompletableFuture<Void> loadAndPasteStructure(@Nonnull String path, @Nonnull Location loc, boolean includeEntities) {
		return loadStructure(path).thenAccept((clipboard) -> pasteStructure(clipboard, loc, includeEntities));
	}

	/**
	 * Loads a structure from the disk and returns it.
	 *
	 * Can be called from the main thread or an async thread, will return immediately and do its work on an async thread
	 *
	 * @param path Relative path under the structures/ folder of the structure to load, not including the extension
	 *
	 * @return Returns a future which will be completed on the main thread when loading is complete or an error occurs.
	 *         Suggest chaining on .whenComplete((clipboard, ex) -> your code) to consume the result on the main thread
	 *         clipboard will be non-null on success, otherwise ex will be a non-null exception if something went wrong
	 */
	public static CompletableFuture<BlockArrayClipboard> loadStructure(@Nonnull String path) {
		CompletableFuture<BlockArrayClipboard> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(StructuresPlugin.getInstance(), () -> {
			final BlockArrayClipboard clipboard;

			try {
				File file = CommandUtils.getAndValidateSchematicPath(StructuresPlugin.getInstance(), path, true);

				ClipboardFormat format = ClipboardFormats.findByAlias(FORMAT);
				Clipboard newClip = format.load(file);
				if (newClip instanceof BlockArrayClipboard) {
					clipboard = (BlockArrayClipboard)newClip;
				} else if (newClip instanceof DiskOptimizedClipboard) {
					clipboard = ((DiskOptimizedClipboard)newClip).toClipboard();
				} else {
					future.completeExceptionally(new Exception("Loaded unknown clipboard type: " + newClip.getClass().toString()));
					return;
				}

				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					future.complete(clipboard);
				});
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					future.completeExceptionally(ex);
				});
			}
		});

		return future;
	}

	/**
	 * Save a structure given a bounding box at the specified path.
	 *
	 * Can be called from the main thread or an async thread, will return immediately and do its work on an async thread
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
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (!loc1.getWorld().equals(loc2.getWorld())) {
			future.completeExceptionally(new Exception("Locations must have the same world"));
			return future;
		}

		Bukkit.getScheduler().runTaskAsynchronously(StructuresPlugin.getInstance(), () -> {
			// Create file to save under
			try (Closer closer = Closer.create()) {
				File file = CommandUtils.getAndValidateSchematicPath(StructuresPlugin.getInstance(), path, false);
				if (!file.exists()) {
					file.getParentFile().mkdirs();
					file.createNewFile();
				}

				Clipboard clipboard = copyArea(loc1, loc2).get();

				ClipboardFormat format = ClipboardFormats.findByAlias(FORMAT);
				FileOutputStream fos = closer.register(new FileOutputStream(file));
				BufferedOutputStream bos = closer.register(new BufferedOutputStream(fos));
				ClipboardWriter writer = closer.register(format.getWriter(bos));
				writer.write(clipboard);

				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					future.complete(null);
				});
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					future.completeExceptionally(ex);
				});
			}
		});

		return future;
	}

	/**
	 * Copies a bounding box to a clipboard that can be used with pasteStructure().
	 *
	 * Can be called from the main thread or an async thread, will return immediately and do its work on an async thread
	 *
	 * @param loc1 One corner of the bounding box to save
	 * @param loc2 The opposite corner
	 */
	public static CompletableFuture<BlockArrayClipboard> copyArea(@Nonnull Location loc1, @Nonnull Location loc2) {
		CompletableFuture<BlockArrayClipboard> future = new CompletableFuture<>();

		if (!loc1.getWorld().equals(loc2.getWorld())) {
			future.completeExceptionally(new Exception("Locations must have the same world"));
			return future;
		}

		Bukkit.getScheduler().runTaskAsynchronously(StructuresPlugin.getInstance(), () -> {
			// Parse the coordinates of the structure to save
			BlockVector3 pos1 = BlockVector3.at(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ());
			BlockVector3 pos2 = BlockVector3.at(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ());

			BlockVector3 minpos = pos1.getMinimum(pos2);
			BlockVector3 maxpos = pos1.getMaximum(pos2);

			World world = new BukkitWorld(loc1.getWorld());
			CuboidRegion cReg = new CuboidRegion(world, minpos, maxpos);
			BlockArrayClipboard clipboard = new BlockArrayClipboard(cReg);
			clipboard.setOrigin(cReg.getMinimumPoint());

			/* Copy the region (including entities and biomes) into the clipboard object */
			EditSession extent = new EditSessionBuilder(world)
				.autoQueue(true)
				.fastmode(true)
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

			Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
				future.complete(clipboard);
			});
		});

		return future;
	}

	/**
	 * Pastes a structure at the given location, ignoring structure void similarly to vanilla structure blocks.
	 *
	 * Can be called from the main thread or an async thread, will return immediately and do its work on an async thread
	 */
	public static CompletableFuture<Void> pasteStructure(@Nonnull BlockArrayClipboard clipboard, @Nonnull Location loc, boolean includeEntities) {
		Plugin plugin = StructuresPlugin.getInstance();

		/*
		 * This is the future given to the caller - when it's completed it should be safe to interact with the pasted area
		 * In particular there is a 6s delay after pasting before this is marked as completed
		 */
		CompletableFuture<Void> future = new CompletableFuture<>();
		if (plugin == null) {
			future.completeExceptionally(new Exception("MonumentaStructureManagement plugin isn't loaded"));
			return future;
		}

		/*
		 * This future is used to signal the pasting system that this schematic has completed and the next one can start pasting
		 * This does not have the 6s delay returned to the user
		 */
		CompletableFuture<Void> signal = new CompletableFuture<>();

		PENDING_TASKS.add(new PendingTask(signal, () -> {
			final long initialTime = System.currentTimeMillis(); // <-- START

			final Region sourceRegion = clipboard.getRegion();
			final BlockVector3 size = sourceRegion.getMaximumPoint().subtract(sourceRegion.getMinimumPoint());
			final org.bukkit.World world = loc.getWorld();
			final BlockVector3 to = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
			final Vector pos1 = new Vector((double)to.getX(), (double)to.getY(), (double)to.getZ());
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

			final Set<BlockVector2> chunks = shiftedRegion.getChunks();
			final AtomicInteger numRemaining = new AtomicInteger(chunks.size());

			/* Set of positions (relative to the clipboard / origin) that should not be overwritten when pasting */
			final Set<Long> noLoadPositions = new HashSet<>();

			/* This chunk consumer removes entities and sets spawners/brewstands/furnaces to air */
			final Consumer<Chunk> chunkConsumer = (final Chunk chunk) -> {
				numRemaining.decrementAndGet();

				/*
				 * Mark this chunk so it will stay loaded. Keep a reference count so chunks definitely stay loaded, even when
				 * multiple overlapping structures are pasted simultaneously
				 */
				Long key = chunk.getChunkKey();
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
								final int relx = state.getX() - to.getX();
								final int rely = state.getY() - to.getY();
								final int relz = state.getZ() - to.getZ();
								noLoadPositions.add(compressToLong(relx, rely, relz));
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

			/* Load all the chunks in the region and run the chunk consumer */
			for (final BlockVector2 chunkCoords : chunks) {
				world.getChunkAtAsync(chunkCoords.getX(), chunkCoords.getZ(), chunkConsumer);
			}

			new BukkitRunnable() {
				int numTicksWaited = 0;
				@Override
				public void run() {
					numTicksWaited++;
					if (numTicksWaited >= 30 * 20) {
						String msg = "Timed out waiting for chunks to load to paste structure!";
						plugin.getLogger().severe(msg);
						this.cancel();
						signal.completeExceptionally(new Exception(msg));
						future.completeExceptionally(new Exception(msg));
						return;
					}
					if (numRemaining.get() == 0) {
						this.cancel();

						/* Actually load the structure asynchronously now that all the chunks have been processed for entities / blocks that shouldn't be replaced */
						Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
							plugin.getLogger().info("Initial processing took " + Long.toString(System.currentTimeMillis() - initialTime) + " milliseconds (mostly async)"); // STOP -->

							final long pasteTime = System.currentTimeMillis(); // <-- START
							try (EditSession extent = new EditSessionBuilder(new BukkitWorld(world))
								.autoQueue(true)
								.fastmode(true)
								.combineStages(true)
								.changeSetNull()
								.checkMemory(false)
								.allowedRegionsEverywhere()
								.limitUnlimited()
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
								copy.setCopyingBiomes(false);
								copy.setFilterFunction(filterFunction);
								copy.setCopyingEntities(includeEntities);
								Operations.completeBlindly(copy);
							}
							plugin.getLogger().info("Loading structure took " + Long.toString(System.currentTimeMillis() - pasteTime) + " milliseconds (async)"); // STOP -->

							/* Allow the next structure load task to start at this point */
							signal.complete(null);

							/* 6s later, signal caller that loading is complete */
							Bukkit.getScheduler().runTaskLater(plugin, () -> {
								future.complete(null);
							}, 120);

							/* Schedule light cleaning on the main thread so it can safely check plugin enabled status */
							Bukkit.getScheduler().runTask(plugin, () -> {
								if (!Bukkit.getPluginManager().isPluginEnabled("LightCleaner")) {
									return;
								}

								final long lightTime = System.currentTimeMillis(); // <-- START

								/* Relight an area 16 blocks bigger than the respawned area */
								final Set<BlockVector2> lightingChunks = new CuboidRegion(to.subtract(16, 16, 16), to.add(size).add(16, 16, 16)).getChunks();
								final LongHashSet lightCleanerChunks = new LongHashSet(lightingChunks.size());
								for (final BlockVector2 chunk : lightingChunks) {
									lightCleanerChunks.add(chunk.getX(), chunk.getZ());
								}
								ScheduleArguments args = new ScheduleArguments();
								args.setWorld(world);
								args.setChunks(lightCleanerChunks);
								args.setLoadedChunksOnly(true);
								LightingService.schedule(args);

								plugin.getLogger().info("scheduleLighting took " + Long.toString(System.currentTimeMillis() - lightTime) + " milliseconds (main thread)"); // STOP -->

								/* 10s later, unmark all chunks as force loaded */
								Bukkit.getScheduler().runTaskLater(plugin, () -> {
									for (final BlockVector2 chunkCoords : shiftedRegion.getChunks()) {
										final Consumer<Chunk> consumer = (final Chunk chunk) -> {
											Long key = chunk.getChunkKey();
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
								}, 200);
							});
						});
					}
				}
			}.runTaskTimer(plugin, 0, 1);
		}));

		ensureTask(plugin);

		return future;
	}

	private static class PendingTask {
		private final CompletableFuture<Void> mSignal;
		private final Runnable mStartTask;

		protected PendingTask(CompletableFuture<Void> signal, Runnable startTask) {
			mSignal = signal;
			mStartTask = startTask;
		}
	}

	private static final HashMap<Long, Integer> CHUNK_TICKET_REFERENCE_COUNT = new HashMap<>();
	private static Deque<PendingTask> PENDING_TASKS = new ConcurrentLinkedDeque<>();
	private static BukkitRunnable RUNNING_TASK = null;

	private static final EnumSet<EntityType> keptEntities = EnumSet.of(
		EntityType.PLAYER,
		EntityType.DROPPED_ITEM,
		EntityType.EXPERIENCE_ORB,
		EntityType.IRON_GOLEM,
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
		if (entity instanceof ArmorStand) {
			final ArmorStand stand = (ArmorStand)entity;
			if ((stand.getCustomName() != null && !stand.getCustomName().isEmpty())
			    || stand.isMarker()
			    || (!stand.getScoreboardTags().isEmpty())) {
				return false;
			}
		}

		/* Keep tameable critters that have an owner */
		if (entity instanceof Tameable) {
			final Tameable critter = (Tameable)entity;
			if (critter.getOwner() != null) {
				return false;
			}
		}

		/* Keep entities with the appropriate tag */
		if (entity.getScoreboardTags().contains("RespawnPersistent")) {
			return false;
		}

		/* Remove otherwise */
		return true;
	}

	private static Long compressToLong(final int x, final int y, final int z) {
		return Long.valueOf(
		           (((long)(x & ((1 << 24) - 1))) << 40) |
		           (((long)(y & ((1 << 16) - 1))) << 24) |
		           ((long)(z & ((1 << 24) - 1)))
		       );
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
