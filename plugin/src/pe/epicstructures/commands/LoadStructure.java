package pe.epicstructures.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.EditSessionBuilder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.World;

import com.sk89q.worldedit.util.Location;
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

public class LoadStructure implements CommandExecutor {
	Plugin mPlugin;
	org.bukkit.World mWorld;

	public LoadStructure(Plugin plugin, org.bukkit.World world) {
		mPlugin = plugin;
		mWorld = world;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		sender.sendMessage(ChatColor.RED + "I am a potato");

		if (!(sender instanceof Player)) {
			return false;
		}

		Player player = (Player)sender;
		if (!player.getName().equals("Combustible")) {
			sender.sendMessage(ChatColor.RED + "No potato for you");
			return false;
		}

		sender.sendMessage(ChatColor.RED + "Found Combustible");

		EditSession copyWorld = new EditSessionBuilder(mWorld.getName()).autoQueue(false).build();
		Vector pos1 = new Vector(-1073, 130, -1376);
		Vector pos2 = new Vector(-1168, 41, -1249);
		CuboidRegion copyRegion = new CuboidRegion(pos1, pos2);

		BlockArrayClipboard lazyCopy = copyWorld.lazyCopy(copyRegion);

		Schematic schem = new Schematic(lazyCopy);
		boolean pasteAir = true;
		Vector to = new Vector(-968, 41, -1249);
		_paste(schem.getClipboard(), copyWorld, to, pasteAir, player);
		copyWorld.flushQueue();

		return true;
	}

	// Custom paste function copied and modified from
	// FastAsyncWorldedit/core/src/main/java/com/boydti/fawe/object/schematic/Schematic.java
	//
	// Ignores structure blocks, leaving the original block in place
    public void _paste(Clipboard clipboard, EditSession extent, Vector to, final boolean pasteAir, Player player) {
        Region sourceRegion = clipboard.getRegion().clone();
        Region destRegion = new CuboidRegion(to, new Vector(to).add(sourceRegion.getMaximumPoint()).subtract(sourceRegion.getMinimumPoint()));
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
		//		if (extent.getBlock(xx, yy, zz).hasNbtData()) {
		//			extent.setBlock(xx, yy, zz, FaweCache.getBlock(0, 0));
		//		} else {
					extent.setBlock(xx, yy, zz, block);
		//		}
				return false;
			}
		}, (HasFaweQueue) (null));
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
						type.isItem() ||
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
		List<? extends Entity> entities = extent.getEntities(destRegion);
		// Visit those entities and when visited, remove them
		EntityVisitor EntityVisitor = new EntityVisitor(entities.iterator(), removeFunc);

		//mPlugin.getLogger().info("Got " + entities.size() + " entities in " + destRegion.toString());

		// Run the visit/remover
		try {
			Operations.completeLegacy(EntityVisitor);
		} catch (WorldEditException e) {};




		// Flush the queue - maybe not necessary?
		extent.flushQueue();
    }
}
