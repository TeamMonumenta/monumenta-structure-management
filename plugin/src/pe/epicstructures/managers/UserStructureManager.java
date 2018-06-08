package pe.epicstructures.managers;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

import org.bukkit.scheduler.BukkitRunnable;

import pe.epicstructures.Plugin;

public class UserStructureManager {
	private Queue<UserStructure> mLoadedStructures;

	public UserStructureManager(Plugin plugin) {
		mLoadedStructures = new ConcurrentLinkedQueue<UserStructure>();

		new BukkitRunnable() {
			@Override
			public void run() {
				Iterator<UserStructure> iter = mLoadedStructures.iterator();
				while (iter.hasNext()) {
					UserStructure struct = iter.next();
					if (!struct.playerInStructure()) {
						struct.finalize();
						iter.remove();
					}
				}
			}
		}.runTaskTimer(plugin, 0, 15);
	}

	public void add(UserStructure structure) {
		mLoadedStructures.add(structure);
	}

	public void unloadAll() {
		for (UserStructure structure : mLoadedStructures) {
			structure.finalize();
		}
		mLoadedStructures.clear();
	}
}
