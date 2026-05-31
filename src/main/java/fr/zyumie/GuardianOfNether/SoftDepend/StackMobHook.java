package fr.zyumie.GuardianOfNether.SoftDepend;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;

/**
 * Hook optionnel vers StackMob.
 * Si StackMob n'est pas installé, isEnabled() retourne false
 * et toutes les méthodes sont des no-ops silencieux.
 */
public class StackMobHook {

    private final StackMob stackMob;

    public StackMobHook() {
        if (Bukkit.getPluginManager().getPlugin("StackMob") instanceof StackMob sm) {
            this.stackMob = sm;
        } else {
            this.stackMob = null;
        }
    }

    public boolean isEnabled() {
        return stackMob != null;
    }

    /**
     * Retire les données de stack d'une entité pour qu'elle ne soit pas stackée.
     */
    public void unstackEntity(LivingEntity entity) {
        if (!isEnabled()) return;
        stackMob.getScheduler().runTask(entity, () -> {
            if (!stackMob.getEntityManager().isStackedEntity(entity)) return;
            StackEntity stackEntity = stackMob.getEntityManager().getStackEntity(entity);
            if (stackEntity != null) stackEntity.removeStackData();
        });
    }
}