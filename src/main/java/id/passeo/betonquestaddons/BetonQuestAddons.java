package id.passeo.betonquestaddons;

import id.passeo.betonquestaddons.BetonQuest.SpawnEntityObjective;
import org.betonquest.betonquest.BetonQuest;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetonQuestAddons extends JavaPlugin {

    private static BetonQuestAddons instance;

    public static BetonQuestAddons getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        BetonQuest.getInstance().registerObjectives("spawnmob", SpawnEntityObjective.class);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
