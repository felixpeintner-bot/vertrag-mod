package at.vertrag;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reine Server-Mod: Spieler brauchen nichts zu installieren. */
public class ContractMod implements ModInitializer {
	public static final String MOD_ID = "vertrag";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ContractEngine.register();
		ContractProtection.register();
		ContractCommands.register();

		LOGGER.info("Vertrag-Mod (Server) geladen");
	}
}
