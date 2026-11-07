package at.vertrag;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Macht ein gültiges Vertrags-Buch unzerstörbar, solange es als Gegenstand in der Welt liegt:
 * kein Despawn, kein Feuer, keine Lava, keine Explosion, kein Kaktus.
 * Fällt es ins Void, wird es zurück an die Oberfläche gesetzt.
 */
public final class ContractProtection {
	private static final List<ItemEntity> TRACKED = new ArrayList<>();

	private ContractProtection() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof ItemEntity item) {
				String id = ContractItems.idOf(item.getItem());
				if (id.isEmpty()) {
					return;
				}
				ContractStore.Entry entry = ContractStore.get(level.getServer()).find(id);
				if (entry != null && entry.live()) {
					item.setUnlimitedLifetime();
					item.setInvulnerable(true);
					TRACKED.add(item);
				}
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			Iterator<ItemEntity> it = TRACKED.iterator();
			while (it.hasNext()) {
				ItemEntity item = it.next();
				if (item.isRemoved()) {
					it.remove();
					continue;
				}
				if (item.level() instanceof ServerLevel level && item.getY() < level.getMinY() - 16) {
					item.setPos(item.getX(), level.getSeaLevel() + 1, item.getZ());
					item.setDeltaMovement(Vec3.ZERO);
				}
			}
		});
	}
}
