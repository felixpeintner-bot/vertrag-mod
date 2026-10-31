package at.vertrag.client;

import at.vertrag.ContractActionPayload;
import at.vertrag.ContractBooks;
import at.vertrag.ContractData;
import at.vertrag.Signature;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertragsbildschirm: zeigt den Buchtext, die vorhandenen Unterschriften und ein Feld,
 * in dem man mit der Maus unterschreibt.
 */
public class ContractScreen extends Screen {
	private static final int PANEL_WIDTH = 280;
	private static final int TEXT_LINES = 11;
	private static final int LINE_HEIGHT = 10;
	private static final int PAPER = 0xFFF2EBD3;
	private static final int INK = 0xFF1A1A66;

	private final String playerId;
	private final String contractText;
	private final ContractData data;

	private final List<List<int[]>> strokes = new ArrayList<>();
	private List<int[]> currentStroke = null;

	private List<String> wrappedLines = new ArrayList<>();

	private boolean canCreate;
	private boolean canCountersign;
	private boolean canCancel;
	private boolean textChanged;
	private boolean needsCanvas;

	private int left;
	private int top;
	private int canvasX;
	private int canvasY;

	public ContractScreen(String playerId, String contractText, ContractData data) {
		super(Component.translatable("gui.vertrag.title"));
		this.playerId = playerId;
		this.contractText = contractText;
		this.data = data;
	}

	@Override
	protected void init() {
		this.canCreate = this.data == null || !this.data.isActive();
		this.canCountersign = this.data != null && this.data.isActive()
				&& !this.data.isCreator(this.playerId) && this.data.partner().isEmpty();
		this.canCancel = this.data != null && this.data.isCreator(this.playerId);
		this.needsCanvas = this.canCreate || this.canCountersign;
		this.textChanged = this.data != null && this.data.textHash() != ContractBooks.hash(this.contractText);

		this.wrappedLines = wrap(this.contractText, PANEL_WIDTH - 12);

		this.left = (this.width - PANEL_WIDTH) / 2;
		this.top = Math.max(4, (this.height - 320) / 2);

		int textBottom = this.top + 16 + TEXT_LINES * LINE_HEIGHT + 6;
		int sigRowY = textBottom + 6;
		this.canvasX = this.left + (PANEL_WIDTH - Signature.WIDTH) / 2;
		this.canvasY = sigRowY + 62 + 12;

		int buttonY = (this.needsCanvas ? this.canvasY + Signature.HEIGHT + 6 : sigRowY + 62 + 6);

		int x = this.left;
		if (this.needsCanvas) {
			Component label = Component.translatable(this.canCreate
					? "gui.vertrag.button.create" : "gui.vertrag.button.countersign");
			this.addRenderableWidget(Button.builder(label, btn -> sign())
					.bounds(x, buttonY, 150, 20).build());
			this.addRenderableWidget(Button.builder(Component.translatable("gui.vertrag.button.clear"), btn -> clear())
					.bounds(x + 154, buttonY, 60, 20).build());
			buttonY += 24;
		}
		if (this.canCancel && !this.data.partner().isEmpty()) {
			this.addRenderableWidget(Button.builder(Component.translatable("gui.vertrag.button.penalty_diamonds"),
							btn -> penalty(ContractActionPayload.PENALTY_DIAMONDS))
					.bounds(x, buttonY, 138, 20).build());
			this.addRenderableWidget(Button.builder(Component.translatable("gui.vertrag.button.penalty_kill"),
							btn -> penalty(ContractActionPayload.PENALTY_KILL))
					.bounds(x + 142, buttonY, 138, 20).build());
			buttonY += 24;
		}
		if (this.canCancel) {
			this.addRenderableWidget(Button.builder(Component.translatable("gui.vertrag.button.cancel"), btn -> cancel())
					.bounds(x, buttonY, 138, 20).build());
			this.addRenderableWidget(Button.builder(Component.translatable("gui.vertrag.button.close"), btn -> this.onClose())
					.bounds(x + 142, buttonY, 138, 20).build());
		} else {
			this.addRenderableWidget(Button.builder(Component.translatable("gui.vertrag.button.close"), btn -> this.onClose())
					.bounds(x, buttonY, PANEL_WIDTH, 20).build());
		}
	}

	private void sign() {
		String sig = Signature.encode(this.strokes);
		if (!Signature.isValid(sig)) {
			return;
		}
		int action = this.canCreate ? ContractActionPayload.CREATE : ContractActionPayload.COUNTERSIGN;
		ClientPlayNetworking.send(new ContractActionPayload(action, sig));
		this.onClose();
	}

	private void penalty(int action) {
		ClientPlayNetworking.send(new ContractActionPayload(action, ""));
		this.onClose();
	}

	private void cancel() {
		ClientPlayNetworking.send(new ContractActionPayload(ContractActionPayload.CANCEL, ""));
		this.onClose();
	}

	private void clear() {
		this.strokes.clear();
		this.currentStroke = null;
	}

	// ---------------------------------------------------------------- Maus

	private boolean inCanvas(double mx, double my) {
		return mx >= this.canvasX && mx < this.canvasX + Signature.WIDTH
				&& my >= this.canvasY && my < this.canvasY + Signature.HEIGHT;
	}

	private void addPoint(double mx, double my) {
		if (Signature.countPoints(this.strokes) >= Signature.MAX_POINTS) {
			return;
		}
		int x = (int) Math.max(0, Math.min(Signature.WIDTH - 1, mx - this.canvasX));
		int y = (int) Math.max(0, Math.min(Signature.HEIGHT - 1, my - this.canvasY));
		if (!this.currentStroke.isEmpty()) {
			int[] last = this.currentStroke.get(this.currentStroke.size() - 1);
			if (last[0] == x && last[1] == y) {
				return;
			}
		}
		this.currentStroke.add(new int[] {x, y});
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.needsCanvas && event.button() == 0 && inCanvas(event.x(), event.y())) {
			this.currentStroke = new ArrayList<>();
			this.strokes.add(this.currentStroke);
			addPoint(event.x(), event.y());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.currentStroke != null) {
			addPoint(event.x(), event.y());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.currentStroke != null) {
			this.currentStroke = null;
			return true;
		}
		return super.mouseReleased(event);
	}

	// ---------------------------------------------------------------- Rendern

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int y = this.top;
		graphics.text(this.font, this.title.getString(), this.left, y, 0xFFFFFFFF, true);

		// Status rechts oben
		Component status;
		int statusColor;
		if (this.data == null) {
			status = Component.translatable("gui.vertrag.status_new");
			statusColor = 0xFFCCCCCC;
		} else if (this.data.isActive()) {
			status = Component.translatable("gui.vertrag.status_active");
			statusColor = 0xFF55FF55;
		} else if (this.data.isPunished()) {
			status = Component.translatable("gui.vertrag.status_punished");
			statusColor = 0xFFFFAA00;
		} else {
			status = Component.translatable("gui.vertrag.status_cancelled");
			statusColor = 0xFFFF5555;
		}
		String statusText = status.getString();
		graphics.text(this.font, statusText, this.left + PANEL_WIDTH - this.font.width(statusText), y, statusColor, true);

		// Vertragstext auf "Papier"
		int textTop = y + 14;
		int textHeight = TEXT_LINES * LINE_HEIGHT + 8;
		graphics.fill(this.left, textTop, this.left + PANEL_WIDTH, textTop + textHeight, PAPER);
		graphics.outline(this.left, textTop, PANEL_WIDTH, textHeight, 0xFF000000);

		int shown = Math.min(TEXT_LINES, this.wrappedLines.size());
		for (int i = 0; i < shown; i++) {
			String line = this.wrappedLines.get(i);
			if (i == TEXT_LINES - 1 && this.wrappedLines.size() > TEXT_LINES) {
				line = Component.translatable("gui.vertrag.truncated").getString();
			}
			graphics.text(this.font, line, this.left + 6, textTop + 4 + i * LINE_HEIGHT, 0xFF202020, false);
		}

		// Vorhandene Unterschriften (halbe Größe)
		int sigRowY = textTop + textHeight + 6;
		if (this.data != null) {
			drawParty(graphics, this.data.creator(), "gui.vertrag.creator", this.left, sigRowY);
			if (!this.data.partner().isEmpty()) {
				drawParty(graphics, this.data.partner(), "gui.vertrag.partner", this.left + PANEL_WIDTH / 2 + 4, sigRowY);
			}
		}

		if (this.textChanged) {
			graphics.text(this.font, Component.translatable("gui.vertrag.text_changed").getString(),
					this.left, sigRowY + 52, 0xFFFFAA00, true);
		}

		// Unterschriftsfeld
		if (this.needsCanvas) {
			graphics.text(this.font, Component.translatable("gui.vertrag.sign_here").getString(),
					this.canvasX, this.canvasY - 11, 0xFFFFFFFF, true);
			graphics.fill(this.canvasX, this.canvasY, this.canvasX + Signature.WIDTH, this.canvasY + Signature.HEIGHT, PAPER);
			graphics.outline(this.canvasX, this.canvasY, Signature.WIDTH, Signature.HEIGHT, 0xFF000000);
			// Unterschriftslinie
			graphics.fill(this.canvasX + 6, this.canvasY + Signature.HEIGHT - 10,
					this.canvasX + Signature.WIDTH - 6, this.canvasY + Signature.HEIGHT - 9, 0xFF888888);
			drawStrokes(graphics, this.strokes, this.canvasX, this.canvasY, false);
		}
	}

	private void drawParty(GuiGraphicsExtractor graphics, ContractData.Party party, String labelKey, int x, int y) {
		String label = Component.translatable(labelKey, party.name()).getString();
		graphics.text(this.font, label, x, y, 0xFFFFFFFF, true);
		int boxW = Signature.WIDTH / 2;
		int boxH = Signature.HEIGHT / 2;
		graphics.fill(x, y + 11, x + boxW, y + 11 + boxH, PAPER);
		graphics.outline(x, y + 11, boxW, boxH, 0xFF000000);
		List<List<int[]>> parsed = Signature.parse(party.signature());
		if (parsed != null) {
			drawStrokes(graphics, parsed, x, y + 11, true);
		}
	}

	/** Zeichnet Striche als kleine Quadrate; zwischen Punkten wird linear aufgefüllt. */
	private void drawStrokes(GuiGraphicsExtractor graphics, List<List<int[]>> list, int ox, int oy, boolean half) {
		int size = half ? 1 : 2;
		for (List<int[]> stroke : list) {
			int[] prev = null;
			for (int[] p : stroke) {
				if (prev != null) {
					int dx = p[0] - prev[0];
					int dy = p[1] - prev[1];
					int steps = Math.max(Math.abs(dx), Math.abs(dy));
					for (int s = 1; s < steps; s++) {
						plot(graphics, ox, oy, prev[0] + dx * s / steps, prev[1] + dy * s / steps, size, half);
					}
				}
				plot(graphics, ox, oy, p[0], p[1], size, half);
				prev = p;
			}
		}
	}

	private void plot(GuiGraphicsExtractor graphics, int ox, int oy, int px, int py, int size, boolean half) {
		int x = ox + (half ? px / 2 : px);
		int y = oy + (half ? py / 2 : py);
		graphics.fill(x, y, x + size, y + size, INK);
	}

	// ---------------------------------------------------------------- Textumbruch

	private List<String> wrap(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		for (String paragraph : text.split("\n", -1)) {
			if (paragraph.isBlank()) {
				lines.add("");
				continue;
			}
			StringBuilder line = new StringBuilder();
			for (String word : paragraph.split(" ")) {
				String candidate = line.isEmpty() ? word : line + " " + word;
				if (this.font.width(candidate) > maxWidth && !line.isEmpty()) {
					lines.add(line.toString());
					line = new StringBuilder(word);
				} else {
					line = new StringBuilder(candidate);
				}
			}
			lines.add(line.toString());
		}
		return lines;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
