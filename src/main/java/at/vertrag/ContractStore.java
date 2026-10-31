package at.vertrag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Serverseitiger Speicher (pro Welt): alle abgeschlossenen Verträge und offene Schulden.
 * So funktionieren Regeln auch dann, wenn das Buch gerade bei jemand anderem liegt.
 */
public class ContractStore extends SavedData {

	/** Ein registrierter Vertrag mit Textstand zum Zeitpunkt der Gegenunterschrift. */
	public record Entry(String id, String creatorId, String creatorName, String partnerId, String partnerName,
						String status, String text) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("id").forGetter(Entry::id),
				Codec.STRING.fieldOf("creator_id").forGetter(Entry::creatorId),
				Codec.STRING.fieldOf("creator_name").forGetter(Entry::creatorName),
				Codec.STRING.fieldOf("partner_id").forGetter(Entry::partnerId),
				Codec.STRING.fieldOf("partner_name").forGetter(Entry::partnerName),
				Codec.STRING.fieldOf("status").forGetter(Entry::status),
				Codec.STRING.fieldOf("text").forGetter(Entry::text)
		).apply(i, Entry::new));

		/** Gültig und von beiden unterschrieben. */
		public boolean active() {
			return ContractData.ACTIVE.equals(this.status) && !this.partnerId.isEmpty();
		}

		public ContractRules.Role roleOf(String playerId) {
			if (this.creatorId.equals(playerId)) {
				return ContractRules.Role.CREATOR;
			}
			if (this.partnerId.equals(playerId)) {
				return ContractRules.Role.PARTNER;
			}
			return null;
		}

		public String idOf(ContractRules.Role role) {
			return role == ContractRules.Role.CREATOR ? this.creatorId : this.partnerId;
		}

		public String nameOf(ContractRules.Role role) {
			return role == ContractRules.Role.CREATOR ? this.creatorName : this.partnerName;
		}

		public Entry withStatus(String newStatus) {
			return new Entry(this.id, this.creatorId, this.creatorName, this.partnerId, this.partnerName, newStatus, this.text);
		}

		public Entry withPartner(String pid, String pname, String newText) {
			return new Entry(this.id, this.creatorId, this.creatorName, pid, pname, this.status, newText);
		}
	}

	/** Eine offene Schuld: 'debtorId' schuldet 'creditorId' noch 'amount' Stück von 'item'. */
	public record Debt(String debtorId, String debtorName, String creditorId, String creditorName, String item, int amount) {
		public static final Codec<Debt> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("debtor_id").forGetter(Debt::debtorId),
				Codec.STRING.fieldOf("debtor_name").forGetter(Debt::debtorName),
				Codec.STRING.fieldOf("creditor_id").forGetter(Debt::creditorId),
				Codec.STRING.fieldOf("creditor_name").forGetter(Debt::creditorName),
				Codec.STRING.fieldOf("item").forGetter(Debt::item),
				Codec.INT.fieldOf("amount").forGetter(Debt::amount)
		).apply(i, Debt::new));

		public Debt withAmount(int newAmount) {
			return new Debt(this.debtorId, this.debtorName, this.creditorId, this.creditorName, this.item, newAmount);
		}
	}

	private static final Codec<ContractStore> CODEC = RecordCodecBuilder.create(i -> i.group(
			Entry.CODEC.listOf().fieldOf("contracts").forGetter(s -> s.entries),
			Debt.CODEC.listOf().fieldOf("debts").forGetter(s -> s.debts)
	).apply(i, ContractStore::new));

	private static final SavedDataType<ContractStore> TYPE = new SavedDataType<>(
			ContractMod.id("contract_store"),
			ContractStore::new,
			CODEC,
			null
	);

	private final List<Entry> entries;
	private final List<Debt> debts;

	public ContractStore() {
		this(List.of(), List.of());
	}

	public ContractStore(List<Entry> entries, List<Debt> debts) {
		this.entries = new ArrayList<>(entries);
		this.debts = new ArrayList<>(debts);
	}

	public static ContractStore get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Entry> entries() {
		return this.entries;
	}

	public Entry find(String id) {
		for (Entry e : this.entries) {
			if (e.id().equals(id)) {
				return e;
			}
		}
		return null;
	}

	public void put(Entry entry) {
		for (int i = 0; i < this.entries.size(); i++) {
			if (this.entries.get(i).id().equals(entry.id())) {
				this.entries.set(i, entry);
				this.setDirty();
				return;
			}
		}
		this.entries.add(entry);
		this.setDirty();
	}

	public List<Debt> debts() {
		return this.debts;
	}

	public void addDebt(Debt debt) {
		this.debts.add(debt);
		this.setDirty();
	}

	public void replaceDebt(int index, Debt debt) {
		this.debts.set(index, debt);
		this.setDirty();
	}

	public void removeDebt(int index) {
		this.debts.remove(index);
		this.setDirty();
	}
}
