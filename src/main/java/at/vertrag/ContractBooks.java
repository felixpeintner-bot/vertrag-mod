package at.vertrag;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;

/** Hilfsfunktionen, um den Text aus einem Buch zu lesen und als Vertrag zu erkennen. */
public final class ContractBooks {
	public static final String HEADER = "VERTRAG";

	private ContractBooks() {
	}

	public static boolean isBook(ItemStack stack) {
		return stack.is(Items.WRITABLE_BOOK) || stack.is(Items.WRITTEN_BOOK);
	}

	/** Liest alle Seiten des Buches und hängt sie mit Zeilenumbruch aneinander. */
	public static String readText(ItemStack stack) {
		StringBuilder sb = new StringBuilder();

		WritableBookContent writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
		if (writable != null) {
			for (Filterable<String> page : writable.pages()) {
				sb.append(page.raw()).append('\n');
			}
		}

		WrittenBookContent written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		if (written != null) {
			for (Filterable<net.minecraft.network.chat.Component> page : written.pages()) {
				sb.append(page.raw().getString()).append('\n');
			}
		}

		return sb.toString();
	}

	/** Ein Buch ist ein Vertrag, wenn die erste nicht leere Zeile "VERTRAG" lautet. */
	public static boolean isContract(String text) {
		for (String line : text.split("\n")) {
			String trimmed = line.strip();
			if (trimmed.isEmpty()) {
				continue;
			}
			return trimmed.equalsIgnoreCase(HEADER);
		}
		return false;
	}

	/** Einfacher Hash, um spätere Textänderungen zu erkennen (kein kryptografischer Schutz). */
	public static int hash(String text) {
		return text.strip().hashCode();
	}
}
