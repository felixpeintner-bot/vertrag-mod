package at.vertrag;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Serverseitige Logik. Alle Regeln werden hier geprüft, der Client wird nicht vertraut. */
public final class ContractServer {
	private ContractServer() {
	}

	public static void handle(ContractActionPayload payload, ServerPlayer player) {
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
			default -> {
			}
		}
	}

	private static void msg(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable(key));
	}
}
