package at.vertrag;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Führt die Vertragsregeln aus:
 * - Beim Tod eines Vertragspartners werden Schulden angelegt oder Spieler getötet.
 * - Jede Sekunde werden Schulden beglichen, sobald der Schuldner genug im Inventar hat.
 */
public final class ContractEngine {
	/** Spieler, die gerade durch eine Vertragsregel getötet werden (verhindert Endlosschleifen). */
	private static final Set<String> KILLED_BY_CONTRACT = new HashSet<>();

	private ContractEngine() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player) {
				onDeath(player);
			}
			return true; // Tod nicht verhindern
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				settleDebts(server);
			}
		});
	}

	// ---------------------------------------------------------------- Tod

	private static void onDeath(ServerPlayer dead) {
		String deadId = dead.getUUID().toString();
		if (KILLED_BY_CONTRACT.contains(deadId)) {
			return;
		}
		MinecraftServer server = dead.level().getServer();
		if (server == null) {
			return;
		}

		ContractStore store = ContractStore.get(server);
		for (ContractStore.Entry entry : List.copyOf(store.entries())) {
			if (!entry.active()) {
				continue;
			}
			ContractRules.Role role = entry.roleOf(deadId);
			if (role == null) {
				continue;
			}
			for (ContractRules.Rule rule : ContractRules.parse(entry.text())) {
				if (rule.trigger() == role) {
					execute(server, store, entry, rule, deadId);
				}
			}
		}
	}

	private static void execute(MinecraftServer server, ContractStore store, ContractStore.Entry entry,
								ContractRules.Rule rule, String deadId) {
		if (rule.kill()) {
			String targetId = entry.idOf(rule.target());
			if (targetId.equals(deadId)) {
				return; // der ist ja schon tot
			}
			ServerPlayer target = player(server, targetId);
			if (target == null) {
				return;
			}
			target.sendSystemMessage(Component.literal("Vertragsregel: Du wirst getoetet."));
			server.execute(() -> {
				KILLED_BY_CONTRACT.add(targetId);
				try {
					target.kill((ServerLevel) target.level());
				} finally {
					KILLED_BY_CONTRACT.remove(targetId);
				}
			});
			return;
		}

		ContractRules.ItemInfo info = ContractRules.info(rule.item());
		if (info == null) {
			return;
		}
		ContractStore.Debt debt = new ContractStore.Debt(
				entry.idOf(rule.from()), entry.nameOf(rule.from()),
				entry.idOf(rule.to()), entry.nameOf(rule.to()),
				rule.item(), rule.amount());
		store.addDebt(debt);

		Component msg = Component.literal("Vertragsschuld: " + debt.debtorName() + " schuldet "
				+ debt.creditorName() + " " + debt.amount() + " " + info.label() + ".");
		tell(server, debt.debtorId(), msg);
		tell(server, debt.creditorId(), msg);
	}

	// ---------------------------------------------------------------- Schulden

	private static void settleDebts(MinecraftServer server) {
		ContractStore store = ContractStore.get(server);
		if (store.debts().isEmpty()) {
			return;
		}

		for (int i = store.debts().size() - 1; i >= 0; i--) {
			ContractStore.Debt debt = store.debts().get(i);
			ServerPlayer debtor = player(server, debt.debtorId());
			ServerPlayer creditor = player(server, debt.creditorId());
			ContractRules.ItemInfo info = ContractRules.info(debt.item());
			if (info == null) {
				store.removeDebt(i);
				continue;
			}
			if (debtor == null || creditor == null) {
				continue; // beide müssen online sein
			}

			int taken = take(debtor, info, debt.amount());
			if (taken <= 0) {
				continue;
			}

			ItemStack reward = new ItemStack(info.item(), taken);
			if (!creditor.getInventory().add(reward)) {
				creditor.drop(reward, false);
			}

			int left = debt.amount() - taken;
			if (left <= 0) {
				store.removeDebt(i);
			} else {
				store.replaceDebt(i, debt.withAmount(left));
			}

			Component msg = Component.literal("Schuld beglichen: " + debt.debtorName() + " hat " + taken + " "
					+ info.label() + " an " + debt.creditorName() + " uebergeben (noch offen: " + left + ").");
			debtor.sendSystemMessage(msg);
			creditor.sendSystemMessage(msg);
		}
	}

	/** Entfernt bis zu 'amount' passende Items aus dem Inventar und gibt die entfernte Menge zurück. */
	private static int take(ServerPlayer player, ContractRules.ItemInfo info, int amount) {
		Inventory inv = player.getInventory();
		int remaining = amount;
		for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.is(info.item())) {
				int n = Math.min(remaining, stack.getCount());
				stack.shrink(n);
				remaining -= n;
			}
		}
		return amount - remaining;
	}

	// ---------------------------------------------------------------- Hilfen

	private static ServerPlayer player(MinecraftServer server, String id) {
		try {
			return server.getPlayerList().getPlayer(UUID.fromString(id));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static void tell(MinecraftServer server, String id, Component msg) {
		ServerPlayer p = player(server, id);
		if (p != null) {
			p.sendSystemMessage(msg);
		}
	}
}
