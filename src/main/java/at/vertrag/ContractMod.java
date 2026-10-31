package at.vertrag;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractMod implements ModInitializer {
	public static final String MOD_ID = "vertrag";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModComponents.init();

		PayloadTypeRegistry.serverboundPlay().register(ContractActionPayload.TYPE, ContractActionPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ContractActionPayload.TYPE,
				(payload, context) -> ContractServer.handle(payload, context.player()));

		LOGGER.info("Vertrag-Mod geladen");
	}
}
