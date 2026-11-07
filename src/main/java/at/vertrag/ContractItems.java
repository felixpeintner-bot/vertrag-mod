package at.vertrag;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Das Buch trägt nur die Vertrags-ID (in der normalen CUSTOM_DATA-Komponente), damit auch
 * Spieler ohne Mod das Item problemlos sehen. Name und Beschreibung sind normaler Text.
 */
public final class ContractItems {
	public static final String KEY = "vertrag_id";

	private ContractItems() {
	}

	/** Vertrags-ID des Buches oder leerer String. */
	public static String idOf(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return "";
		}
		return data.copyTag().getStringOr(KEY, "");
	}

	public static void setId(ItemStack stack, String id) {
		CompoundTag tag = new CompoundTag();
		tag.putString(KEY, id);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	/** Setzt Name, Beschreibung und Glitzer passend zum Vertragsstatus. */
	public static void applyLabel(ItemStack stack, ContractStore.Entry entry) {
		List<Component> lore = new ArrayList<>();
		String nr = entry.id().length() >= 8 ? entry.id().substring(0, 8) : entry.id();
		lore.add(Component.literal("Vertrags-Nr. " + nr).withStyle(ChatFormatting.DARK_GRAY));

		if (entry.live()) {
			stack.set(DataComponents.CUSTOM_NAME,
					Component.literal("Vertrag - ORIGINAL").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

			lore.add(Component.literal("Ersteller: " + entry.creatorName() + " (gez. " + entry.creatorSig() + ")")
					.withStyle(ChatFormatting.YELLOW));
			if (entry.partnerId().isEmpty()) {
				lore.add(Component.literal("Gegenpartei fehlt noch").withStyle(ChatFormatting.GRAY));
			} else {
				lore.add(Component.literal("Gegenpartei: " + entry.partnerName() + " (gez. " + entry.partnerSig() + ")")
						.withStyle(ChatFormatting.YELLOW));
			}
			lore.add(Component.literal("Unzerstoerbar - nur der Ersteller kann ihn aufloesen").withStyle(ChatFormatting.AQUA));
		} else {
			stack.set(DataComponents.CUSTOM_NAME,
					Component.literal("Vertrag - AUFGELOEST").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
			stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);

			lore.add(Component.literal("Ersteller: " + entry.creatorName()).withStyle(ChatFormatting.GRAY));
			lore.add(Component.literal("Aufgeloest - keine Wirkung mehr").withStyle(ChatFormatting.RED));
		}

		stack.set(DataComponents.LORE, new ItemLore(lore));
	}

	/** Siegel-Effekt: Ton und Partikel um den Spieler. */
	public static void seal(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(),
				40, 0.6, 0.6, 0.6, 0.2);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(),
				12, 0.4, 0.4, 0.4, 0.0);
	}
}
