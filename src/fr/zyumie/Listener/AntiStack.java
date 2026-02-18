package fr.zyumie.Listener;

import fr.zyumie.SoftDepend.StackMobHook;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class AntiStack implements Listener {

    private final StackMobHook stackMob;
    private final EntityType blockedEntityType = EntityType.WITHER_SKELETON;

    public AntiStack(StackMobHook stackMob) {
        this.stackMob = stackMob;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!stackMob.isEnabled()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.getType() != blockedEntityType) return;

        stackMob.unstackEntity(mob);
    }
}
