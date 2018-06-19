package pe.epicstructures.managers;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

import pe.epicstructures.Plugin;

public class UserStructureManager {
	private Queue<UserStructure> mLoadedStructures;
	private int mTaskId;

	public UserStructureManager(Plugin plugin) {
		mLoadedStructures = new ConcurrentLinkedQueue<UserStructure>();

		mTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			@Override
			public void run() {
				Iterator<UserStructure> iter = mLoadedStructures.iterator();
				while (iter.hasNext()) {
					UserStructure struct = iter.next();
					if (!struct.playerInStructure()) {
						struct.saveAndCleanup();
						iter.remove();
					}
				}
			}
		}, 0L, 10L);
	}

	public void add(UserStructure structure) {
		mLoadedStructures.add(structure);
	}

	public void unloadAll(Plugin plugin) {
		plugin.getServer().getScheduler().cancelTask(mTaskId);

		for (UserStructure structure : mLoadedStructures) {
			structure.saveAndCleanup();
		}
		mLoadedStructures.clear();
	}
}
