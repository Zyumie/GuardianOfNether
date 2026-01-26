package fr.zyumie.Listener;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.StackEntity;

public class AntiStackListener implements Listener {

	private final StackMob sm;

	// Configurer le type de mob à bloquer
	private final EntityType blockedEntityType = EntityType.WITHER_SKELETON;

	public AntiStackListener(JavaPlugin plugin) {
		// Récupérer l'instance de StackMob depuis le serveur
		this.sm = (StackMob) plugin.getServer().getPluginManager().getPlugin("StackMob");
	}

	/**
	 * Listener en priorité HIGHEST pour contrer le stack d'un certain type de mob
	 * S'exécute AVANT le SpawnListener normal (qui utilise la priorité par défaut
	 * NORMAL)
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		// Vérifier si c'est un Mob
		// Vérifier si c'est le type de mob à bloquer
		if (!(event.getEntity() instanceof Mob) || (event.getEntity().getType() != blockedEntityType)) {
			return;
		}

		LivingEntity entity = event.getEntity();

		// Planifier la tâche pour s'exécuter après le spawn
		sm.getScheduler().runTask(entity, () -> {
			// Vérifier si l'entité est déjà stackée
			if (!sm.getEntityManager().isStackedEntity(entity)) {
				return;
			}

			StackEntity stackEntity = sm.getEntityManager().getStackEntity(entity);
			if (stackEntity == null) {
				return;
			}

			// Empêcher le stack en retirant les données de stack
			stackEntity.removeStackData();

			// Optionnel: Vous pouvez aussi forcer la taille à 1
			// stackEntity.setSize(1);
		});
	}

	/**
	 * Alternative: Bloquer directement lors de tentatives de merge Utilise
	 * également HIGHEST pour s'exécuter en premier
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityMerge(CreatureSpawnEvent event) {
		if (!(event.getEntity() instanceof Mob) || (event.getEntity().getType() != blockedEntityType)) {
			return;
		}

		LivingEntity entity = event.getEntity();

		// Empêcher toute logique de stack pour ce type de mob
		sm.getScheduler().runTask(entity, () -> {
			StackEntity stackEntity = sm.getEntityManager().getStackEntity(entity);
			if (stackEntity != null) {
				// Désactiver temporairement le stack pour cette entité
				stackEntity.removeStackData();
			}
		});
	}
}