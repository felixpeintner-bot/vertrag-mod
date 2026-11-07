package at.vertrag.client;

import at.vertrag.ContractBooks;
import at.vertrag.ContractItems;
import at.vertrag.ContractMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * Optional beim Spieler: die Taste R schickt nur den Befehl "/vertrag auto" an den Server.
 * Ohne Vertrags-Buch in der Hand passiert absichtlich gar nichts (keine Meldung).
 */
public class ContractModClient implements ClientModInitializer {
	private static KeyMapping openKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(ContractMod.id("main"));

		openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vertrag.open",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_R,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.consumeClick()) {
				press(client);
			}
		});
	}

	private static void press(Minecraft client) {
		if (client.player == null || client.screen != null) {
			return;
		}
		ItemStack stack = client.player.getMainHandItem();
		if (!ContractBooks.isBook(stack)) {
			return;
		}
		boolean hasContract = !ContractItems.idOf(stack).isEmpty()
				|| ContractBooks.isContract(ContractBooks.readText(stack));
		if (!hasContract) {
			return;
		}
		client.player.connection.sendCommand("vertrag auto");
	}
}
