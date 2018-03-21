package pe.epicstructures.commands;

import java.io.File;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.EditSessionBuilder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.World;

import com.sk89q.worldedit.function.operation.Operations;
import com.boydti.fawe.object.HasFaweQueue;
import com.sk89q.worldedit.MutableBlockVector2D;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.function.RegionFunction;

import com.sk89q.worldedit.entity.metadata.EntityType;
import com.sk89q.worldedit.function.EntityFunction;
import com.sk89q.worldedit.command.util.EntityRemover;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.function.visitor.EntityVisitor;

import pe.epicstructures.Plugin;
import pe.epicstructures.utils.CommandUtils;
import pe.epicstructures.utils.MessagingUtils;

public class LoadStructure implements CommandExecutor {
	Plugin mPlugin;
	org.bukkit.World mWorld;

	public LoadStructure(Plugin plugin, org.bukkit.World world) {
		mPlugin = plugin;
		mWorld = world;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 4) {
			sender.sendMessage(ChatColor.RED + "This command requires exactly four arguments");
			return false;
		}

		// Parse the coordinates to load the structure
		Vector loadPos;
		try {
			Location loadLoc = CommandUtils.parseLocationFromString(sender, mWorld, arg3[1], arg3[2], arg3[3]);

			loadPos = new Vector(loadLoc.getBlockX(), loadLoc.getBlockY(), loadLoc.getBlockZ());
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse coordinates");
			MessagingUtils.sendStackTrace(sender, e);
			return false;
		}

		Schematic schem;
		try {
			schem = mPlugin.mStructureManager.loadSchematic(arg3[0]);
		} catch (Exception e) {
			mPlugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();

			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Failed to load structure");
				MessagingUtils.sendStackTrace(sender, e);
			}
			return false;
		}

		EditSession copyWorld = new EditSessionBuilder(mWorld.getName()).autoQueue(false).build();

		boolean pasteAir = true;
		_paste(schem.getClipboard(), copyWorld, loadPos, pasteAir);

		// TODO: Is this needed?
		copyWorld.flushQueue();

		sender.sendMessage("Loaded structure '" + arg3[0] + "' at " + loadPos);

		return true;
	}

	// Custom paste function copied and modified from
	// FastAsyncWorldedit/core/src/main/java/com/boydti/fawe/object/schematic/Schematic.java
	//
	// Ignores structure blocks, leaving the original block in place
	public void _paste(Clipboard clipboard, EditSession extent, Vector to, final boolean pasteAir) {
		Region sourceRegion = clipboard.getRegion().clone();
		Region destRegion = new CuboidRegion(to,
		                                     new Vector(to).add(sourceRegion.getMaximumPoint()).subtract(sourceRegion.getMinimumPoint()));
		final Vector destBot = destRegion.getMinimumPoint();
		final int maxY = extent.getMaximumPoint().getBlockY();
		final Vector bot = clipboard.getMinimumPoint();
		final Vector origin = clipboard.getOrigin();

		// ******************************** Blocks *************************************** //
		final int relx = to.getBlockX() - origin.getBlockX();
		final int rely = to.getBlockY() - origin.getBlockY();
		final int relz = to.getBlockZ() - origin.getBlockZ();
		RegionVisitor regionVisitor = new RegionVisitor(sourceRegion, new RegionFunction() {
			MutableBlockVector2D mpos2d_2 = new MutableBlockVector2D();
			MutableBlockVector2D mpos2d = new MutableBlockVector2D();
			{
				mpos2d.setComponents(Integer.MIN_VALUE, Integer.MIN_VALUE);
			}
			@Override
			public boolean apply(Vector mutable) throws WorldEditException {
				BaseBlock block = clipboard.getBlock(mutable);
				int xx = mutable.getBlockX() + relx;
				int yy = mutable.getBlockY() + rely;
				int zz = mutable.getBlockZ() + relz;
				if ((!pasteAir && block.getId() == 0) || block.getId() == 217) {
					return false;
				}
				//      if (extent.getBlock(xx, yy, zz).hasNbtData()) {
				//          extent.setBlock(xx, yy, zz, FaweCache.getBlock(0, 0));
				//      } else {
				extent.setBlock(xx, yy, zz, block);
				//      }
				return false;
			}
		}, (HasFaweQueue)(null));

		// TODO: Make this run async (completeSmart)
		Operations.completeBlindly(regionVisitor);


		// ******************************** Entities *************************************** //
		/*
		 * Make an iterable function that removes all the things that should
		 * be removed when a POI resets
		 */
		EntityFunction removeFunc = new EntityFunction() {
			@Override
			public boolean apply(Entity entity) throws WorldEditException {
				EntityType type = entity.getFacet(EntityType.class);
				Vector loc = entity.getLocation().toVector();

				// Don't remove entities in parts of the POI that don't reset
				if (clipboard.getBlock(loc.subtract(to).add(origin)).getId() == 217) {
					return false;
				}

				if (type == null ||
				type.isPlayerDerived() ||
				type.isAnimal() ||
				type.isTamed() ||
				type.isGolem() ||
				type.isNPC() ||
				type.isArmorStand()) {
					return false;
				}

				if (type.isLiving() ||
				//type.isItem() ||
				type.isProjectile() ||
				type.isFallingBlock() ||
				type.isBoat() ||
				type.isTNT() ||
				type.isExperienceOrb()) {
					entity.remove();
					return true;
				}

				return false;
			}
		};

		// Get the currently loaded entities in the destination region
		List <? extends Entity > entities = extent.getEntities(destRegion);
		// Visit those entities and when visited, remove them
		EntityVisitor EntityVisitor = new EntityVisitor(entities.iterator(), removeFunc);

		//mPlugin.getLogger().info("Got " + entities.size() + " entities in " + destRegion.toString());

		// Run the visit/remover
		try {
			// TODO: Make this run async (completeSmart)
			Operations.completeLegacy(EntityVisitor);
		} catch (WorldEditException e) {};

		// TODO: Is this needed?
		extent.flushQueue();
	}
}
