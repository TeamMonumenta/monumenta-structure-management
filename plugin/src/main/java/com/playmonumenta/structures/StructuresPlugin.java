package com.playmonumenta.structures;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import com.playmonumenta.structures.commands.ActivateSpecialStructure;
import com.playmonumenta.structures.commands.AddRespawningStructure;
import com.playmonumenta.structures.commands.CompassRespawn;
import com.playmonumenta.structures.commands.ForceloadLazy;
import com.playmonumenta.structures.commands.ListRespawningStructures;
import com.playmonumenta.structures.commands.LoadStructure;
import com.playmonumenta.structures.commands.ReloadStructures;
import com.playmonumenta.structures.commands.RemoveRespawningStructure;
import com.playmonumenta.structures.commands.RespawnStructure;
import com.playmonumenta.structures.commands.SaveStructure;
import com.playmonumenta.structures.commands.SetPostRespawnCommand;
import com.playmonumenta.structures.commands.SetRespawnTimer;
import com.playmonumenta.structures.commands.SetSpawnerBreakTrigger;
import com.playmonumenta.structures.managers.EventListener;
import com.playmonumenta.structures.managers.RespawnManager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class StructuresPlugin extends JavaPlugin {
	public RespawnManager mRespawnManager = null;

	private static StructuresPlugin INSTANCE = null;

	private File mConfigFile;
	private YamlConfiguration mConfig;

	@Override
	public void onLoad() {
		ActivateSpecialStructure.register(this);
		AddRespawningStructure.register(this);
		CompassRespawn.register();
		ForceloadLazy.register();
		ListRespawningStructures.register();
		LoadStructure.register();
		ReloadStructures.register(this);
		RemoveRespawningStructure.register();
		RespawnStructure.register();
		SaveStructure.register();
		SetPostRespawnCommand.register();
		SetRespawnTimer.register();
		SetSpawnerBreakTrigger.register(this);
	}

	@Override
	public void onEnable() {
		INSTANCE = this;

		//TODO: Command to add an alternate generic structure

		PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(new EventListener(this), this);

		reloadConfig();
	}

	@Override
	public void onDisable() {
		// Save current structure respawn times
		saveConfig();

		// Cancel structure respawning and clear list
		if (mRespawnManager != null) {
			mRespawnManager.cleanup();
			mRespawnManager = null;
		}

		// Cancel any remaining tasks
		getServer().getScheduler().cancelTasks(this);

		INSTANCE = null;
	}

	@Override
	public void reloadConfig() {
		// Do not save first
		if (mRespawnManager != null) {
			mRespawnManager.cleanup();
			mRespawnManager = null;
		}

		if (mConfigFile == null) {
			mConfigFile = new File(getDataFolder(), "config.yml");
		}

		if (!mConfigFile.exists()) {
			try {
				// Create parent directories if they do not exist
				mConfigFile.getParentFile().mkdirs();

				// Create the file if it does not exist
				mConfigFile.createNewFile();
			} catch (IOException ex) {
				getLogger().log(Level.SEVERE, "Failed to create non-existent configuration file");
			}

			// TODO: Put sample config file in here also
		}

		mConfig = YamlConfiguration.loadConfiguration(mConfigFile);

		/* TODO: Non-hardcoded worlds! These should be saved into the respawning structure */
		mRespawnManager = new RespawnManager(this, Bukkit.getWorlds().get(0), mConfig);
	}

	@Override
	public void saveConfig() {
		if (mRespawnManager != null) {
			try {
				mConfig = mRespawnManager.getConfig();
				mConfig.save(mConfigFile);
			} catch (Exception ex) {
				getLogger().log(Level.SEVERE, "Could not save config to " + mConfigFile, ex);
			}
		}
	}

	public void asyncLog(Level level, String message) {
		new BukkitRunnable() {
			@Override
			public void run()
			{
				getLogger().log(level, message);
			}
		}.runTask(this);
	}

	public void asyncLog(Level level, String message, Exception ex) {
		new BukkitRunnable() {
			@Override
			public void run()
			{
				getLogger().log(level, message, ex);
			}
		}.runTask(this);
	}

	public static StructuresPlugin getInstance() {
		return INSTANCE;
	}
}
