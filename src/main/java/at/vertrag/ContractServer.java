package at.vertrag;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/** Serverseitige Logik. Bedient wird alles über /vertrag, die Spieler brauchen keine Mod. */
public final class ContractServer {
	private ContractServer() {
	}

	private static void say(ServerPlayer player, String text) {
		player.sendSystemMessage(Component.literal(text));
	}

	private static Component button(String label, ChatFormatting color, ClickEvent click) {
		return Component.literal("[" + label + "]").withStyle(Style.EMPTY.withColor(color).withClickEvent(click));
	}

	private static String cleanName(String raw) {
		String name = raw == null ? "" : raw.strip();
		if (name.length() > 32) {
			name = name.substring(0, 32);
		}
		return name;
	}

	// ---------------------------------------------------------------- R-Taste

	/**
	 * Wird von der R-Taste aufgerufen. Ohne Vertrags-Buch passiert still nichts.
	 * Neuer Vertrag: unterschreibt mit dem Spielernamen. Offene Gegenpartei: unterschreibt als Gegenpartei.
	 * Sonst: zeigt den Vertrag mit den Buttons (z. B. zum Auflösen).
	 */
	public static void auto(ServerPlayer player, MinecraftServer server) {
		ItemStack stack = player.getMainHandItem();
		if (!ContractBooks.isBook(stack)) {
			return;
		}
		String id = ContractItems.idOf(stack);
		if (id.isEmpty() && !ContractBooks.isContract(ContractBooks.readText(stack))) {
			return;
		}

		ContractStore.Entry entry = id.isEmpty() ? null : ContractStore.get(server).find(id);
		String name = player.getName().getString();

		if (entry == null) {
			sign(player, server, name);
		} else if (entry.live() && entry.partnerId().isEmpty()
				&& !entry.creatorId().equals(player.getUUID().toString())) {
			countersign(player, server, name);
		} else {
			show(player, server);
		}
	}

	// ---------------------------------------------------------------- Anzeigen

	public static void show(ServerPlayer player, MinecraftServer server) {
		ItemStack stack = player.getMainHandItem();
		if (!ContractBooks.isBook(stack)) {
			say(player, "Halte ein Buch (Buch und Feder) in der Hand und tippe /vertrag.");
			return;
		}

		String id = ContractItems.idOf(stack);
		String text = ContractBooks.readText(stack);
		if (id.isEmpty() && !ContractBooks.isContract(text)) {
			say(player, "Das Buch ist kein Vertrag. Die erste Zeile muss VERTRAG lauten.");
			return;
		}

		ContractStore store = ContractStore.get(server);
		ContractStore.Entry entry = id.isEmpty() ? null : store.find(id);
		String playerId = player.getUUID().toString();

		player.sendSystemMessage(Component.literal("===== VERTRAG =====").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

		if (entry == null) {
			say(player, "Status: noch nicht unterschrieben");
		} else if (!entry.live()) {
			player.sendSystemMessage(Component.literal("Status: AUFGELOEST").withStyle(ChatFormatting.RED));
		} else {
			player.sendSystemMessage(Component.literal("Status: GUELTIG (ORIGINAL)").withStyle(ChatFormatting.GREEN));
			say(player, "Ersteller: " + entry.creatorName() + " (gez. " + entry.creatorSig() + ")");
			if (entry.partnerId().isEmpty()) {
				say(player, "Gegenpartei: fehlt noch");
			} else {
				say(player, "Gegenpartei: " + entry.partnerName() + " (gez. " + entry.partnerSig() + ")");
			}
			if (entry.textHash() != ContractBooks.hash(text)) {
				player.sendSystemMessage(Component.literal("Achtung: Der Buchtext wurde nachtraeglich geaendert. "
						+ "Es gilt der Text bei der Unterschrift.").withStyle(ChatFormatting.GOLD));
			}
		}

		String effectiveText = entry != null ? entry.text() : text;
		List<ContractRules.Rule> rules = ContractRules.parse(effectiveText);
		if (rules.isEmpty()) {
			player.sendSystemMessage(Component.literal("Keine Regeln erkannt: der Vertrag hat keine automatische Wirkung.")
					.withStyle(ChatFormatting.GOLD));
		} else {
			say(player, "Erkannte Regeln:");
			for (ContractRules.Rule rule : rules) {
				say(player, " - " + ContractRules.describe(rule));
			}
		}

		if (entry == null || !entry.live()) {
			player.sendSystemMessage(button("Unterschreiben & abschliessen", ChatFormatting.GREEN,
					new ClickEvent.SuggestCommand("/vertrag unterschreiben ")));
		} else if (entry.creatorId().equals(playerId)) {
			player.sendSystemMessage(button("Vertrag aufloesen", ChatFormatting.RED,
					new ClickEvent.RunCommand("/vertrag aufloesen")));
		} else if (entry.partnerId().isEmpty()) {
			player.sendSystemMessage(button("Als Gegenpartei unterschreiben", ChatFormatting.GREEN,
					new ClickEvent.SuggestCommand("/vertrag gegenzeichnen ")));
		}
	}

	// ---------------------------------------------------------------- Unterschreiben

	/** Der Ersteller unterschreibt: das Buch wird zum Original. */
	public static void sign(ServerPlayer player, MinecraftServer server, String rawName) {
		ItemStack stack = player.getMainHandItem();
		if (!ContractBooks.isBook(stack)) {
			say(player, "Halte das Vertrags-Buch in der Hand.");
			return;
		}
		String text = ContractBooks.readText(stack);
		if (!ContractBooks.isContract(text)) {
			say(player, "Das Buch ist kein Vertrag. Die erste Zeile muss VERTRAG lauten.");
			return;
		}

		ContractStore store = ContractStore.get(server);
		String oldId = ContractItems.idOf(stack);
		if (!oldId.isEmpty()) {
			ContractStore.Entry old = store.find(oldId);
			if (old != null && old.live()) {
				say(player, "In diesem Buch gibt es schon einen gueltigen Vertrag.");
				return;
			}
		}

		String name = cleanName(rawName);
		if (name.isEmpty()) {
			say(player, "Schreibe deine Unterschrift hinter den Befehl, zum Beispiel: /vertrag unterschreiben Max Muster");
			return;
		}

		String id = UUID.randomUUID().toString();
		ContractStore.Entry entry = new ContractStore.Entry(id,
				player.getUUID().toString(), player.getName().getString(), name,
				"", "", "",
				ContractStore.ACTIVE, ContractBooks.hash(text), text);
		store.put(entry);
		ContractItems.setId(stack, id);
		ContractItems.applyLabel(stack, entry);
		ContractItems.seal(player);

		say(player, "Vertrag unterschrieben. Das Buch ist jetzt das ORIGINAL.");
		say(player, "Die andere Person haelt das Buch in der Hand und tippt /vertrag.");
		int rules = ContractRules.parse(text).size();
		if (rules == 0) {
			say(player, "Hinweis: Es wurden keine Regeln erkannt, der Vertrag hat keine automatische Wirkung.");
		}
	}

	/** Die Gegenpartei unterschreibt. */
	public static void countersign(ServerPlayer player, MinecraftServer server, String rawName) {
		ItemStack stack = player.getMainHandItem();
		String id = ContractBooks.isBook(stack) ? ContractItems.idOf(stack) : "";
		ContractStore store = ContractStore.get(server);
		ContractStore.Entry entry = id.isEmpty() ? null : store.find(id);
		String playerId = player.getUUID().toString();

		if (entry == null || !entry.live() || !entry.partnerId().isEmpty() || entry.creatorId().equals(playerId)) {
			say(player, "Du kannst diesen Vertrag nicht als Gegenpartei unterschreiben.");
			return;
		}
		if (entry.textHash() != ContractBooks.hash(ContractBooks.readText(stack))) {
			say(player, "Der Buchtext wurde nach der Unterschrift des Erstellers geaendert. Unterschreiben nicht moeglich.");
			return;
		}

		String name = cleanName(rawName);
		if (name.isEmpty()) {
			say(player, "Schreibe deine Unterschrift hinter den Befehl, zum Beispiel: /vertrag gegenzeichnen Erika Muster");
			return;
		}

		ContractStore.Entry updated = entry.withPartner(playerId, player.getName().getString(), name);
		store.put(updated);
		ContractItems.applyLabel(stack, updated);
		ContractItems.seal(player);

		server.getPlayerList().broadcastSystemMessage(
				Component.literal(entry.creatorName() + " und " + player.getName().getString()
						+ " haben einen Vertrag geschlossen.").withStyle(ChatFormatting.GOLD), false);

		int rules = ContractRules.parse(updated.text()).size();
		say(player, "Du hast den Vertrag unterschrieben. Er gilt jetzt mit " + rules + " Regel(n).");
		ServerPlayer creator = server.getPlayerList().getPlayer(UUID.fromString(entry.creatorId()));
		if (creator != null) {
			say(creator, player.getName().getString() + " hat deinen Vertrag unterschrieben. Er gilt jetzt.");
		}
	}

	// ---------------------------------------------------------------- Auflösen

	/** Nur der Ersteller darf den Vertrag auflösen. */
	public static void dissolve(ServerPlayer player, MinecraftServer server) {
		ItemStack stack = player.getMainHandItem();
		String id = ContractBooks.isBook(stack) ? ContractItems.idOf(stack) : "";
		ContractStore store = ContractStore.get(server);
		ContractStore.Entry entry = id.isEmpty() ? null : store.find(id);

		if (entry == null || !entry.live()) {
			say(player, "Halte ein gueltiges Vertrags-Buch in der Hand.");
			return;
		}
		if (!entry.creatorId().equals(player.getUUID().toString())) {
			say(player, "Nur der Ersteller kann den Vertrag aufloesen.");
			return;
		}

		ContractStore.Entry updated = entry.withStatus(ContractStore.CANCELLED);
		store.put(updated);
		ContractItems.applyLabel(stack, updated);
		say(player, "Der Vertrag wurde aufgeloest. Offene Schulden bleiben bestehen (/vertrag schulden).");

		if (!entry.partnerId().isEmpty()) {
			ServerPlayer partner = server.getPlayerList().getPlayer(UUID.fromString(entry.partnerId()));
			if (partner != null) {
				say(partner, entry.creatorName() + " hat den Vertrag aufgeloest.");
			}
		}
	}
}
