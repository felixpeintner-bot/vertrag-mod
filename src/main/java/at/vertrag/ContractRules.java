package at.vertrag;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liest feste Sätze aus dem Vertragstext:
 *
 * <pre>
 * WENN Ersteller stirbt
 * DANN gibt Gegenpartei dem Ersteller 2 Diamanten
 *
 * WENN Gegenpartei stirbt
 * DANN wird Ersteller getötet
 * </pre>
 *
 * Zeilenumbrüche, Satzzeichen und Groß-/Kleinschreibung sind egal.
 */
public final class ContractRules {
	public static final int MAX_AMOUNT = 2304;
	public static final int MAX_RULES = 10;

	public enum Role {
		CREATOR,
		PARTNER;

		static Role of(String word) {
			return word.equals("ersteller") ? CREATOR : PARTNER;
		}
	}

	/** Eine Regel: wenn 'trigger' stirbt, dann entweder 'target' töten oder 'from' schuldet 'to' die Menge. */
	public record Rule(Role trigger, boolean kill, Role from, Role to, int amount, String item, Role target) {
	}

	public record ItemInfo(Item item, String label) {
	}

	private static final Map<String, ItemInfo> ITEMS = new HashMap<>();
	private static final Map<String, String> SYNONYMS = new HashMap<>();

	private static void item(Item item, String label, String... words) {
		String key = label.toLowerCase(Locale.ROOT);
		ITEMS.put(key, new ItemInfo(item, label));
		SYNONYMS.put(key, key);
		for (String word : words) {
			SYNONYMS.put(word, key);
		}
	}

	static {
		item(Items.DIAMOND, "Diamanten", "diamant");
		item(Items.IRON_INGOT, "Eisenbarren", "eisenbarren", "eisen");
		item(Items.GOLD_INGOT, "Goldbarren", "goldbarren", "gold");
		item(Items.EMERALD, "Smaragde", "smaragd");
		item(Items.NETHERITE_INGOT, "Netheritbarren", "netheritebarren", "netheritbarren", "netherit");
		item(Items.COPPER_INGOT, "Kupferbarren", "kupferbarren", "kupfer");
		item(Items.COAL, "Kohle");
		item(Items.LAPIS_LAZULI, "Lapislazuli");
		item(Items.REDSTONE, "Redstone");
		item(Items.ENDER_PEARL, "Enderperlen", "enderperle");
		item(Items.GOLDEN_APPLE, "Goldaepfel", "goldapfel");
		item(Items.APPLE, "Aepfel", "apfel");
		item(Items.BREAD, "Brote", "brot");
		item(Items.ARROW, "Pfeile", "pfeil");
	}

	private static final Pattern RULE = Pattern.compile(
			"wenn (ersteller|gegenpartei) stirbt dann (?:gibt (ersteller|gegenpartei) (?:dem|der) (ersteller|gegenpartei) (\\d{1,4}) ([a-z]+)"
					+ "|wird (ersteller|gegenpartei) getoetet)");

	private ContractRules() {
	}

	public static ItemInfo info(String key) {
		return ITEMS.get(key);
	}

	private static String subject(Role role) {
		return role == Role.CREATOR ? "der Ersteller" : "die Gegenpartei";
	}

	private static String dative(Role role) {
		return role == Role.CREATOR ? "dem Ersteller" : "der Gegenpartei";
	}

	/** Beschreibt eine Regel in normalem Deutsch für die Anzeige. */
	public static String describe(Rule rule) {
		String start = "Wenn " + subject(rule.trigger()) + " stirbt: ";
		if (rule.kill()) {
			return start + subject(rule.target()) + " wird getoetet";
		}
		ItemInfo info = ITEMS.get(rule.item());
		String label = info == null ? rule.item() : info.label();
		return start + subject(rule.from()) + " gibt " + dative(rule.to()) + " " + rule.amount() + " " + label;
	}

	public static String normalize(String text) {
		String s = text.toLowerCase(Locale.ROOT)
				.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
		s = s.replaceAll("[^a-z0-9]+", " ").trim();
		return s;
	}

	public static List<Rule> parse(String text) {
		List<Rule> rules = new ArrayList<>();
		Matcher m = RULE.matcher(normalize(text));
		while (m.find() && rules.size() < MAX_RULES) {
			Role trigger = Role.of(m.group(1));
			if (m.group(6) != null) {
				Role target = Role.of(m.group(6));
				rules.add(new Rule(trigger, true, null, null, 0, null, target));
				continue;
			}
			Role from = Role.of(m.group(2));
			Role to = Role.of(m.group(3));
			int amount = Integer.parseInt(m.group(4));
			String itemKey = SYNONYMS.get(m.group(5));
			if (from == to || itemKey == null || amount < 1 || amount > MAX_AMOUNT) {
				continue; // ungültige Regel wird ignoriert
			}
			rules.add(new Rule(trigger, false, from, to, amount, itemKey, null));
		}
		return rules;
	}
}
