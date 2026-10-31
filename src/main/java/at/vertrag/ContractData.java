package at.vertrag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Der Vertragsstatus, der als Data Component direkt im Buch-Item gespeichert wird.
 */
public record ContractData(Party creator, Party partner, String status, int textHash) {
	public static final String ACTIVE = "ACTIVE";
	public static final String CANCELLED = "CANCELLED";
	public static final String PUNISHED = "PUNISHED";

	public record Party(String id, String name, String signature) {
		public static final Party EMPTY = new Party("", "", "");

		public static final Codec<Party> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("id").forGetter(Party::id),
				Codec.STRING.fieldOf("name").forGetter(Party::name),
				Codec.STRING.fieldOf("signature").forGetter(Party::signature)
		).apply(instance, Party::new));

		public static final StreamCodec<ByteBuf, Party> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Party::id,
				ByteBufCodecs.STRING_UTF8, Party::name,
				ByteBufCodecs.STRING_UTF8, Party::signature,
				Party::new
		);

		public boolean isEmpty() {
			return this.id.isEmpty();
		}
	}

	public static final Codec<ContractData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Party.CODEC.fieldOf("creator").forGetter(ContractData::creator),
			Party.CODEC.fieldOf("partner").forGetter(ContractData::partner),
			Codec.STRING.fieldOf("status").forGetter(ContractData::status),
			Codec.INT.fieldOf("text_hash").forGetter(ContractData::textHash)
	).apply(instance, ContractData::new));

	public static final StreamCodec<ByteBuf, ContractData> STREAM_CODEC = StreamCodec.composite(
			Party.STREAM_CODEC, ContractData::creator,
			Party.STREAM_CODEC, ContractData::partner,
			ByteBufCodecs.STRING_UTF8, ContractData::status,
			ByteBufCodecs.INT, ContractData::textHash,
			ContractData::new
	);

	public boolean isActive() {
		return ACTIVE.equals(this.status);
	}

	public boolean isCreator(String playerId) {
		return this.isActive() && this.creator.id().equals(playerId);
	}

	public ContractData withPartner(Party newPartner) {
		return new ContractData(this.creator, newPartner, this.status, this.textHash);
	}

	public ContractData cancelled() {
		return new ContractData(this.creator, this.partner, CANCELLED, this.textHash);
	}

	public ContractData punished() {
		return new ContractData(this.creator, this.partner, PUNISHED, this.textHash);
	}

	public boolean isPunished() {
		return PUNISHED.equals(this.status);
	}
}
