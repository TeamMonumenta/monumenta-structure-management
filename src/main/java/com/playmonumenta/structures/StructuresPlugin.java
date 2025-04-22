package com.playmonumenta.structures;

import com.playmonumenta.structures.commands.ActivateSpecialStructure;
import com.playmonumenta.structures.commands.AddRespawningStructure;
import com.playmonumenta.structures.commands.ChangeLogLevel;
import com.playmonumenta.structures.commands.CompassRespawn;
import com.playmonumenta.structures.commands.ForceConquerRespawn;
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
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitWorker;

public class StructuresPlugin extends JavaPlugin {
	public @Nullable RespawnManager mRespawnManager = null;

	private static @Nullable StructuresPlugin INSTANCE = null;

	private @Nullable YamlConfiguration mConfig = null;
	private @Nullable CustomLogger mLogger = null;

	@Override
	public void onLoad() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}

		ChangeLogLevel.register();
		ActivateSpecialStructure.register(this);
		AddRespawningStructure.register(this);
		CompassRespawn.register();
		ForceConquerRespawn.register();
		ForceloadLazy.register();
		ListRespawningStructures.register();
		LoadStructure.register();
		ReloadStructures.register(this);
		RemoveRespawningStructure.register();
		RespawnStructure.register();
		SaveStructure.register();
		SetPostRespawnCommand.register();
		SetRespawnTimer.register();
		SetSpawnerBreakTrigger.register();
	}

	@Override
	public void onEnable() {
		INSTANCE = this;

		//TODO: Command to add an alternate generic structure

		PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(new EventListener(), this);

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

		// Log any async threads that haven't finished yet
		for (BukkitWorker worker : Bukkit.getScheduler().getActiveWorkers()) {
			if (!worker.getOwner().equals(this)) {
				continue;
			}

			Thread thread = worker.getThread();
			StringBuilder builder = new StringBuilder("Unterminated thread stacktrace:");
			for (StackTraceElement traceElement : thread.getStackTrace()) {
				builder.append("\n\tat ").append(traceElement);
			}
			getLogger().severe(builder.toString());
		}
		// Cancel any remaining tasks
		getServer().getScheduler().cancelTasks(this);

		INSTANCE = null;
	}

	public File getConfigFile() {
		File configFile = new File(getDataFolder(), "config.yml");

		if (!configFile.exists()) {
			try {
				// Create parent directories if they do not exist
				if (!configFile.getParentFile().mkdirs()) {
					throw new IOException();
				}

				// Create the file if it does not exist
				if (!configFile.createNewFile()) {
					throw new IOException();
				}
			} catch (IOException ex) {
				getLogger().log(Level.SEVERE, "Failed to create non-existent configuration file");
			}

			// TODO: Put sample config file in here also
		}

		return configFile;
	}

	@Override
	public void reloadConfig() {
		// Do not save first
		if (mRespawnManager != null) {
			mRespawnManager.cleanup();
			mRespawnManager = null;
		}

		File configFile = getConfigFile();

		mConfig = YamlConfiguration.loadConfiguration(configFile);

		/* TODO: Non-hardcoded worlds! These should be saved into the respawning structure */
		mRespawnManager = new RespawnManager(this, Bukkit.getWorlds().get(0), mConfig);
	}

	@Override
	public void saveConfig() {
		if (mRespawnManager != null) {
			File configFile = getConfigFile();
			try {
				mConfig = mRespawnManager.getConfig();
				mConfig.save(configFile);
			} catch (Exception ex) {
				getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
			}
		}
	}

	@Override
	public Logger getLogger() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}
		return mLogger;
	}

	public static StructuresPlugin getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("Attempted to get StructurePlugin before it finished loading");
		}
		return INSTANCE;
	}

	public static RespawnManager getRespawnManager() {
		StructuresPlugin plugin = getInstance();
		if (plugin.mRespawnManager == null) {
			throw new RuntimeException("Attempted to get StructurePlugin before it finished loading");
		}
		return plugin.mRespawnManager;
	}
}
