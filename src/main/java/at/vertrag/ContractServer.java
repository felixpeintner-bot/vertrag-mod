package at.vertrag;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/** Serverseitige Logik. Alle Regeln werden hier geprüft, der Client wird nicht vertraut. */
public final class ContractServer {
	public static final int PENALTY_DIAMONDS = 20;

	private ContractServer() {
	}

	public static void handle(ContractActionPayload payload, ServerPlayer player, MinecraftServer server) {
		ItemStack stack = player.getMainHandItem();

		if (!ContractBooks.isBook(stack)) {
			msg(player, "message.vertrag.no_book");
			return;
		}

		String text = ContractBooks.readText(stack);
		if (!ContractBooks.isContract(text)) {
			msg(player, "message.vertrag.no_contract");
			return;
		}

		int hash = ContractBooks.hash(text);
		ContractData existing = stack.get(ModComponents.CONTRACT);
		String playerId = player.getUUID().toString();
		String playerName = player.getName().getString();

		switch (payload.action()) {
			case ContractActionPayload.CREATE -> {
				if (existing != null && existing.isActive()) {
					msg(player, "message.vertrag.cannot_create");
					return;
				}
				if (!Signature.isValid(payload.signature())) {
					msg(player, "message.vertrag.bad_signature");
					return;
				}
				ContractData.Party creator = new ContractData.Party(playerId, playerName, payload.signature());
				stack.set(ModComponents.CONTRACT,
						new ContractData(creator, ContractData.Party.EMPTY, ContractData.ACTIVE, hash));
				msg(player, "message.vertrag.created");
			}
			case ContractActionPayload.COUNTERSIGN -> {
				if (existing == null || !existing.isActive() || existing.isCreator(playerId) || !existing.partner().isEmpty()) {
					msg(player, "message.vertrag.cannot_countersign");
					return;
				}
				if (existing.textHash() != hash) {
					msg(player, "message.vertrag.text_changed");
					return;
				}
				if (!Signature.isValid(payload.signature())) {
					msg(player, "message.vertrag.bad_signature");
					return;
				}
				ContractData.Party partner = new ContractData.Party(playerId, playerName, payload.signature());
				stack.set(ModComponents.CONTRACT, existing.withPartner(partner));
				msg(player, "message.vertrag.countersigned");
			}
			case ContractActionPayload.CANCEL -> {
				if (existing == null || !existing.isActive()) {
					msg(player, "message.vertrag.cannot_countersign");
					return;
				}
				// Nur der Ersteller darf kündigen.
				if (!existing.isCreator(playerId)) {
					msg(player, "message.vertrag.only_creator");
					return;
				}
				stack.set(ModComponents.CONTRACT, existing.cancelled());
				msg(player, "message.vertrag.cancelled");
			}
			case ContractActionPayload.PENALTY_DIAMONDS, ContractActionPayload.PENALTY_KILL ->
					penalty(payload.action(), player, existing, stack, server);
			default -> {
			}
		}
	}

	/** Der Ersteller löst die Vertragsstrafe gegen die Gegenpartei aus. Nur einmal möglich. */
	private static void penalty(int action, ServerPlayer creator, ContractData existing, ItemStack stack, MinecraftServer server) {
		String creatorId = creator.getUUID().toString();

		if (existing == null || !existing.isActive() || !existing.isCreator(creatorId)) {
			msg(creator, "message.vertrag.only_creator");
			return;
		}
		if (existing.partner().isEmpty()) {
			msg(creator, "message.vertrag.no_partner");
			return;
		}

		ServerPlayer target;
		try {
			target = server.getPlayerList().getPlayer(UUID.fromString(existing.partner().id()));
		} catch (IllegalArgumentException e) {
			target = null;
		}
		if (target == null) {
			msg(creator, "message.vertrag.target_offline");
			return;
		}

		if (action == ContractActionPayload.PENALTY_DIAMONDS) {
			int taken = takeDiamonds(target, PENALTY_DIAMONDS);
			if (taken > 0) {
				ItemStack reward = new ItemStack(Items.DIAMOND, taken);
				if (!creator.getInventory().add(reward)) {
					creator.drop(reward, false);
				}
			}
			creator.sendSystemMessage(Component.translatable("message.vertrag.penalty_done_creator", taken, target.getName()));
			target.sendSystemMessage(Component.translatable("message.vertrag.penalty_done_target", taken, creator.getName()));
		} else {
			target.sendSystemMessage(Component.translatable("message.vertrag.kill_target", creator.getName()));
			creator.sendSystemMessage(Component.translatable("message.vertrag.kill_creator", target.getName()));
			target.kill((ServerLevel) target.level());
		}

		stack.set(ModComponents.CONTRACT, existing.punished());
	}

	/** Entfernt bis zu 'amount' Diamanten aus dem Inventar und gibt zurück, wie viele es waren. */
	private static int takeDiamonds(ServerPlayer player, int amount) {
		Inventory inv = player.getInventory();
		int remaining = amount;
		for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(Items.DIAMOND)) {
				int take = Math.min(remaining, s.getCount());
				s.shrink(take);
				remaining -= take;
			}
		}
		return amount - remaining;
	}

	private static void msg(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable(key));
	}
}
