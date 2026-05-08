package fr.zyumie.GuardianOfNether.SoftDepend;

import fr.zyumie.GuardianOfNether.SoftDepend.StackMobHook;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Empêche StackMob de stacker le boss et ses sbires.
 * Enregistré uniquement si StackMob est détecté au démarrage.
 */
public class AntiStack implements Listener {

    private final StackMobHook stackMob;

    // On cible uniquement les Wither Skeletons — boss + sbires
    private static final EntityType TARGET_TYPE = EntityType.WITHER_SKELETON;

    // Clés PDC qui identifient nos entités
    private static final String BOSS_KEY   = "guardian_boss";
    private static final String MINION_KEY = "guardian_minion";

    public AntiStack(StackMobHook stackMob) {
        this.stackMob = stackMob;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!stackMob.isEnabled()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.getType() != TARGET_TYPE) return;

        // On ne déstack que nos propres entités — pas tous les wither skeletons du monde
        if (!isGuardianEntity(mob)) return;

        stackMob.unstackEntity(mob);
    }

    /**
     * Vérifie si l'entité appartient au plugin via ses tags PDC.
     */
    private boolean isGuardianEntity(Mob mob) {
        var pdc = mob.getPersistentDataContainer();
        // On vérifie boss ET minion
        return pdc.has(new NamespacedKey("guardianofnether", BOSS_KEY),   PersistentDataType.BYTE)
                || pdc.has(new NamespacedKey("guardianofnether", MINION_KEY), PersistentDataType.BYTE)
                || mob.getScoreboardTags().contains("guardian_boss")
                || mob.getScoreboardTags().contains("guardian_minion");
    }
}