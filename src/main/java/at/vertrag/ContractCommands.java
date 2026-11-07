package at.vertrag;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /vertrag                       zeigt den Vertrag im Buch in der Hand (mit Klick-Buttons)
 * /vertrag unterschreiben <Name> Ersteller unterschreibt
 * /vertrag gegenzeichnen <Name>  Gegenpartei unterschreibt
 * /vertrag aufloesen             nur der Ersteller
 * /vertrag schulden              zeigt offene Schulden
 */
public final class ContractCommands {
	private ContractCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("vertrag")
						.executes(ctx -> {
							ContractServer.show(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer());
							return 1;
						})
						.then(Commands.literal("unterschreiben")
								.then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
									ContractServer.sign(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer(),
											StringArgumentType.getString(ctx, "name"));
									return 1;
								})))
						.then(Commands.literal("gegenzeichnen")
								.then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
									ContractServer.countersign(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer(),
											StringArgumentType.getString(ctx, "name"));
									return 1;
								})))
						.then(Commands.literal("auto").executes(ctx -> {
							ContractServer.auto(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer());
							return 1;
						}))
						.then(Commands.literal("aufloesen").executes(ctx -> {
							ContractServer.dissolve(ctx.getSource().getPlayerOrException(), ctx.getSource().getServer());
							return 1;
						}))
						.then(Commands.literal("schulden").executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							String id = player.getUUID().toString();
							ContractStore store = ContractStore.get(ctx.getSource().getServer());

							int count = 0;
							for (ContractStore.Debt debt : store.debts()) {
								ContractRules.ItemInfo info = ContractRules.info(debt.item());
								String label = info == null ? debt.item() : info.label();
								if (debt.debtorId().equals(id)) {
									player.sendSystemMessage(Component.literal(
											"Du schuldest " + debt.creditorName() + " noch " + debt.amount() + " " + label + "."));
									count++;
								} else if (debt.creditorId().equals(id)) {
									player.sendSystemMessage(Component.literal(
											debt.debtorName() + " schuldet dir noch " + debt.amount() + " " + label + "."));
									count++;
								}
							}
							if (count == 0) {
								player.sendSystemMessage(Component.literal("Du hast keine offenen Schulden."));
							}
							return count;
						}))));
	}
}
