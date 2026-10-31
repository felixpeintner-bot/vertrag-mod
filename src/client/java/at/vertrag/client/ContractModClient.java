package at.vertrag.client;

import at.vertrag.ContractBooks;
import at.vertrag.ContractMod;
import at.vertrag.ModComponents;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

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
				openContract(client);
			}
		});
	}

	private static void openContract(Minecraft client) {
		if (client.player == null) {
			return;
		}

		ItemStack stack = client.player.getMainHandItem();
		if (!ContractBooks.isBook(stack)) {
			client.player.sendSystemMessage(Component.translatable("message.vertrag.no_book"));
			return;
		}

		String text = ContractBooks.readText(stack);
		if (!ContractBooks.isContract(text)) {
			client.player.sendSystemMessage(Component.translatable("message.vertrag.no_contract"));
			return;
		}

		client.gui.setScreen(new ContractScreen(
				client.player.getUUID().toString(),
				text,
				stack.get(ModComponents.CONTRACT)
		));
	}
}
