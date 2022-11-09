package com.playmonumenta.structures.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SpawnerBreakTrigger {
	// Number of spawners remaining when the POI resets / is fresh
	int mSpawnerCount;
	// Number currently remaining
	int mSpawnerCountRemaining;

	QuestComponent mQuestComponent;
	String mQuestComponentStr;
	com.playmonumenta.scriptedquests.Plugin mScriptedQuestsPlugin;

	// TODO:
	// Want to be able to specify different triggers for special structures or alternate variants.
	// This probably means having a "default" spawner break object for each structure, but letting
	// sub-structures override that default. This would require a bit of restructuring...
	//
	// For now, all structures of the same label share this information
	public static SpawnerBreakTrigger fromConfig(@Nullable ConfigurationSection config) throws Exception {
		if (config == null) {
			throw new NullPointerException("Got null config");
		}
		if (!config.isInt("spawner_count")) {
			throw new Exception("Invalid spawner_count");
		} else if (!config.isString("scripted_quests_component")) {
			throw new Exception("Invalid scripted_quests_component");
		}

		int countRemaining = config.isInt("spawner_count_remaining") ?
			config.getInt("spawner_count_remaining") : config.getInt("spawner_count");
		return new SpawnerBreakTrigger(config.getInt("spawner_count"), countRemaining,
		                               config.getString("scripted_quests_component"));

	}

	public SpawnerBreakTrigger(int spawnerCount, int spawnerCountRemaining,
	                           String questComponentStr) throws Exception {
		mScriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin)Bukkit.getPluginManager().getPlugin("ScriptedQuests");
		if (mScriptedQuestsPlugin == null) {
			throw new Exception("ScriptedQuests is not present!");
		}

		mSpawnerCount = spawnerCount;
		mSpawnerCountRemaining = spawnerCountRemaining;

		Gson gson = new Gson();
		JsonObject object = gson.fromJson(questComponentStr, JsonObject.class);
		if (object == null) {
			throw new Exception("Failed to parse scripted_quests_component as JSON object");
		}

		mQuestComponentStr = questComponentStr;
		mQuestComponent = new QuestComponent("REPORT THIS BUG", "REPORT THIS BUG", EntityType.UNKNOWN, object);
	}

	// Called only when a spawner is broken in this structure
	public void spawnerBreakEvent(RespawningStructure structure) {
		mSpawnerCountRemaining--;
		if (mSpawnerCountRemaining <= 0) {
			for (Player player : structure.getWorld().getPlayers()) {
				if (structure.isWithin(player)) {
					mQuestComponent.doActionsIfPrereqsMet(new QuestContext(mScriptedQuestsPlugin, player, null));
				}
			}
		}
		if (getSpawnerRatio() <= 0 && !structure.isConquered()) {
			structure.conquerStructure();
		}
	}

	public double getSpawnerRatio() {
		return (double) mSpawnerCountRemaining / mSpawnerCount;
	}

	public void resetCount() {
		mSpawnerCountRemaining = mSpawnerCount;
	}

	public String getInfoString() {
		return "count=" + mSpawnerCount + " remaining=" + mSpawnerCountRemaining +
		       " component='" + mQuestComponentStr + "'";
	}

	public Map<String, Object> getConfig() {
		Map<String, Object> configMap = new LinkedHashMap<>();

		configMap.put("spawner_count", mSpawnerCount);
		configMap.put("spawner_count_remaining", mSpawnerCountRemaining);
		configMap.put("scripted_quests_component", mQuestComponentStr);

		return configMap;
	}
}
