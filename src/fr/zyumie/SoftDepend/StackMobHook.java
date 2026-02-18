package fr.zyumie.SoftDepend;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;

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

    public void unstackEntity(LivingEntity entity) {
        if (!isEnabled()) return;

        stackMob.getScheduler().runTask(entity, () -> {
            if (!stackMob.getEntityManager().isStackedEntity(entity)) return;

            StackEntity stackEntity = stackMob.getEntityManager().getStackEntity(entity);
            if (stackEntity != null) {
                stackEntity.removeStackData();
            }
        });
    }
}
