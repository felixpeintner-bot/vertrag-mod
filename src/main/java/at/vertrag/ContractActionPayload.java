package at.vertrag;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Paket Client -> Server: eine Vertragsaktion mit optionaler Unterschrift. */
public record ContractActionPayload(int action, String signature) implements CustomPacketPayload {
	public static final int CREATE = 0;
	public static final int COUNTERSIGN = 1;
	public static final int CANCEL = 2;

	public static final Identifier ID = ContractMod.id("contract_action");

	public static final CustomPacketPayload.Type<ContractActionPayload> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, ContractActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ContractActionPayload::action,
			ByteBufCodecs.STRING_UTF8, ContractActionPayload::signature,
			ContractActionPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
