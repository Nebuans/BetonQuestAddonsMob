package id.passeo.betonquestaddons.BetonQuest;

import id.passeo.betonquestaddons.BetonQuestAddons;
import io.lumine.xikage.mythicmobs.api.bukkit.BukkitAPIHelper;
import lombok.SneakyThrows;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.Objective;
import org.betonquest.betonquest.compatibility.protocollib.hider.EntityHider;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.id.EventID;
import org.betonquest.betonquest.id.ObjectiveID;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.betonquest.betonquest.utils.location.CompoundLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnEntityObjective extends Objective implements Listener {

    private final String mobName;
    private final CompoundLocation loc;

    private final ConcurrentHashMap<UUID, Entity> entityMap = new ConcurrentHashMap<>();
    private final EntityHider entityHider = new EntityHider(BetonQuestAddons.getInstance(), EntityHider.Policy.BLACKLIST);

    private final EventID[] failEvents;

    public SpawnEntityObjective(Instruction instruction) throws InstructionParseException {
        super(instruction);
        mobName = instruction.next();
        loc = instruction.getLocation();

        failEvents = instruction.getList(instruction.getOptional("fail"), instruction::getEvent).toArray(new EventID[0]);
    }

    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, BetonQuestAddons.getInstance());

    }

    @SneakyThrows
    @Override
    public void start(String playerID) {
        final Player player = PlayerConverter.getPlayer(playerID);
        final Location location = loc.getLocation(playerID);
        new BukkitRunnable() {
            @SneakyThrows
            @Override
            public void run() {
                Entity entity = new BukkitAPIHelper().spawnMythicMob(mobName, location, 1);
                entityMap.put(player.getUniqueId(), entity);
                for(final Player onlinePlayer: Bukkit.getOnlinePlayers()){
                    if(onlinePlayer != player){
                        entityHider.hideEntity(onlinePlayer, entity);
                    }
                }
            }
        }.runTask(BetonQuestAddons.getInstance());
    }

    @Override
    public void stop() {
        final SpawnEntityObjective instance = this;
        new BukkitRunnable() {
            @Override
            public void run() {
                HandlerList.unregisterAll(instance);
            }
        }.runTask(BetonQuestAddons.getInstance());
    }

    @Override
    public void stop(String playerID) {
        final Player player = PlayerConverter.getPlayer(playerID);
        if (!entityMap.containsKey(player.getUniqueId())) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if(entityMap.get(player.getUniqueId()) == null){
                    return;
                }
                entityMap.remove(player.getUniqueId());
            }
        }.runTask(BetonQuest.getInstance());
    }

    @Override
    public String getDefaultDataInstruction() {
        return "";
    }

    @Override
    public String getProperty(String name, String playerID) {
        return "";
    }

    @EventHandler
    private void onPlayerLogin(final PlayerJoinEvent e) {
        for (final Map.Entry<UUID, Entity> entities : entityMap.entrySet()) {
            if (!e.getPlayer().getUniqueId().equals(entities.getKey())) {
                entityHider.hideEntity(e.getPlayer(), entities.getValue());
            }
        }
    }

    @EventHandler
    private void onPlayerLogout(final PlayerQuitEvent e){
        failObjective(PlayerConverter.getID(e.getPlayer()));
    }

    private void failObjective(final String playerID) {
        if (failEvents.length > 0) {
            for (final EventID event : failEvents) {
                BetonQuest.event(playerID, event);
            }
            BetonQuest.getInstance().getPlayerData(playerID).removeRawObjective((ObjectiveID) instruction.getID());
            cancelObjectiveForPlayer(playerID);
        }
    }
}
