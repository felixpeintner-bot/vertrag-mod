package at.vertrag;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModComponents {
	public static final DataComponentType<ContractData> CONTRACT = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			ContractMod.id("contract"),
			DataComponentType.<ContractData>builder()
					.persistent(ContractData.CODEC)
					.networkSynchronized(ContractData.STREAM_CODEC)
					.build()
	);

	private ModComponents() {
	}

	/** Lädt die Klasse, damit die Registrierung ausgeführt wird. */
	public static void init() {
	}
}
