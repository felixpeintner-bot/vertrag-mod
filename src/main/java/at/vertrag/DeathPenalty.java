package at.vertrag;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Stirbt der Ersteller eines gültigen Vertrags, verliert er 20 Diamanten.
 * Der Vertrag (das Buch) muss dafür im Inventar des Erstellers liegen.
 */
public final class DeathPenalty {
	public static final int DIAMONDS = 20;

	private DeathPenalty() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player) {
				apply(player);
			}
			return true; // Tod nicht verhindern
		});
	}

	private static void apply(ServerPlayer player) {
		Inventory inv = player.getInventory();
		String playerId = player.getUUID().toString();

		boolean hasActiveContract = false;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ContractData data = inv.getItem(i).get(ModComponents.CONTRACT);
			if (data != null && data.isCreator(playerId)) {
				hasActiveContract = true;
				break;
			}
		}
		if (!hasActiveContract) {
			return;
		}

		int remaining = DIAMONDS;
		for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.is(Items.DIAMOND)) {
				int take = Math.min(remaining, stack.getCount());
				stack.shrink(take);
				remaining -= take;
			}
		}

		int lost = DIAMONDS - remaining;
		player.sendSystemMessage(Component.translatable("message.vertrag.death_penalty", lost));
	}
}
