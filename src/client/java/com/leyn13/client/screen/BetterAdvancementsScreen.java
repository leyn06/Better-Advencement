package com.leyn13.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Écran des progrès amélioré :
 * - Pagination des progrès en cartes
 * - Recherche par nom et description
 * - Tri (vanilla, A→Z, progression, inachevés)
 * - Filtre par catégorie (onglets racine)
 * - Barre de progression globale et par progrès
 * - Tooltips au survol
 * - Navigation clavier (flèches) et molette
 */
public class BetterAdvancementsScreen extends Screen implements ClientAdvancements.Listener {

	// Codes clavier GLFW stables (flèches gauche/droite)
	private static final int KEYCODE_LEFT = 263;
	private static final int KEYCODE_RIGHT = 262;

	// ===== Palette =====
	private static final int COLOR_PANEL = 0xF0101826;
	private static final int COLOR_HEADER_TOP = 0xFF1B2A4A;
	private static final int COLOR_HEADER_BOTTOM = 0xFF131F38;
	private static final int COLOR_BORDER = 0xFF3D5A99;
	private static final int COLOR_TITLE = 0xFFFFD700;
	private static final int COLOR_TEXT = 0xFFE8EAF0;
	private static final int COLOR_TEXT_DIM = 0xFF9FB3C8;
	private static final int COLOR_DONE = 0xFF7CFC9A;
	private static final int COLOR_CHALLENGE = 0xFFE6B84C;
	private static final int COLOR_BAR_TRACK = 0xFF202940;
	private static final int COLOR_BAR_FILL_TOP = 0xFF43A047;
	private static final int COLOR_BAR_FILL_BOTTOM = 0xFF81C784;
	private static final int COLOR_CARD_BG = 0x601C2540;
	private static final int COLOR_CARD_BG_HOVER = 0x60273047;
	private static final int COLOR_CHIP_BG = 0xFF24334D;
	private static final int COLOR_CHIP_SELECTED = 0xFF3D5A99;
	private static final int COLOR_CHIP_HOVER = 0xFF31415F;

	// ===== Layout =====
	private static final int HEADER_H = 46;
	private static final int CONTROLS_H = 30;
	private static final int CATEGORY_H = 22;
	private static final int GRID_TOP = HEADER_H + CONTROLS_H + CATEGORY_H + 8;
	private static final int BOTTOM_H = 40;
	private static final int CARD_H = 50;
	private static final int GAP = 6;
	private static final int COLUMNS = 2;

	private final ClientAdvancements clientAdvancements;

	private final List<AdvancementEntry> allEntries = new ArrayList<>();
	private final List<AdvancementNode> categories = new ArrayList<>();
	private List<AdvancementEntry> visible = new ArrayList<>();

	private String searchText = "";
	private SortMode sortMode = SortMode.DEFAULT;
	private AdvancementNode selectedRoot = null; // null = toutes catégories
	private boolean hideCompleted = false;
	private int categoryScroll = 0;

	private int page = 0;
	private long pageChangeTime = 0;

	private EditBox searchBox;
	private CycleButton<SortMode> sortButton;
	private Button hideButton;
	private Button prevButton;
	private Button nextButton;

	// Chips de catégories recalculées à chaque frame pour la gestion du clic
	private record CategoryChip(int x, int y, int w, int h, AdvancementNode root, boolean isAll, int scrollDir) {
	}

	private final List<CategoryChip> chips = new ArrayList<>();

	public BetterAdvancementsScreen(ClientAdvancements clientAdvancements) {
		super(Component.translatable("advancements"));
		this.clientAdvancements = clientAdvancements;
	}

	// =====================================================================
	// Cycle de vie
	// =====================================================================

	@Override
	protected void init() {
		// Enregistre notre écran comme listener : ClientAdvancements rejoue
		// alors l'état complet (arbre + progression) vers nos callbacks.
		this.clientAdvancements.setListener(this);
		rebuildEntries();

		int px = panelX();
		int pw = panelWidth();
		int py = panelY();

		searchBox = new EditBox(this.font, px + 8, py + HEADER_H + 6, 200, 16,
				Component.literal("Rechercher"));
		searchBox.setMaxLength(50);
		searchBox.setHint(Component.literal("Rechercher..."));
		searchBox.setValue(searchText);
		searchBox.setResponder(text -> {
			searchText = text.toLowerCase(Locale.ROOT);
			applyFilters();
		});
		addRenderableWidget(searchBox);

		hideButton = addRenderableWidget(Button.builder(
				Component.literal(hideCompleted ? "Masquer ✓ : ON" : "Masquer ✓ : OFF"),
				button -> {
					hideCompleted = !hideCompleted;
					button.setMessage(Component.literal(hideCompleted ? "Masquer ✓ : ON" : "Masquer ✓ : OFF"));
					applyFilters();
				}).bounds(px + 214, py + HEADER_H + 4, 110, 20).build());

		sortButton = CycleButton.<SortMode>builder(mode -> Component.literal(mode.label()), SortMode.DEFAULT)
				.displayOnlyValue()
				.withValues(SortMode.values())
				.create(px + pw - 148, py + HEADER_H + 4, 140, 20, Component.literal("Tri"),
						(button, mode) -> {
							sortMode = mode;
							applyFilters();
						});
		addRenderableWidget(sortButton);

		prevButton = addRenderableWidget(Button.builder(Component.literal("❮"), button -> goToPage(page - 1))
				.bounds(px + pw / 2 - 120, panelBottom() - 30, 40, 20).build());
		nextButton = addRenderableWidget(Button.builder(Component.literal("❯"), button -> goToPage(page + 1))
				.bounds(px + pw / 2 + 80, panelBottom() - 30, 40, 20).build());

		pageChangeTime = System.currentTimeMillis();
		applyFilters();
	}

	// =====================================================================
	// Données (listener de ClientAdvancements)
	// =====================================================================

	@Override
	public void onAdvancementsUpdated() {
		rebuildEntries();
		applyFilters();
	}

	@Override
	public void onAdvancementsCleared() {
		rebuildEntries();
		applyFilters();
	}

	@Override
	public void onSelectedTabChanged(AdvancementHolder holder) {
		// Géré par nos propres chips de catégories
	}

	private void rebuildEntries() {
		allEntries.clear();
		categories.clear();
		AdvancementTree tree = clientAdvancements.tree();
		if (tree == null) {
			return;
		}
		for (AdvancementNode root : tree.roots()) {
			DisplayInfo display = displayOf(root);
			if (display != null && !display.hidden()) {
				categories.add(root);
			}
		}
		for (AdvancementNode node : tree.nodes()) {
			DisplayInfo display = displayOf(node);
			if (display == null || display.hidden()) {
				continue;
			}
			ItemStack icon = display.icon().apply(display.icon().count(), display.icon().components());
			allEntries.add(new AdvancementEntry(node, display.title(), display.description(),
					display.type(), icon, node.root()));
		}
	}

	private static DisplayInfo displayOf(AdvancementNode node) {
		return node.holder().value().display().orElse(null);
	}

	// =====================================================================
	// Filtres / tri / pagination
	// =====================================================================

	private void applyFilters() {
		List<AdvancementEntry> list = new ArrayList<>(allEntries);
		if (selectedRoot != null) {
			list.removeIf(entry -> !entry.root().equals(selectedRoot));
		}
		if (hideCompleted) {
			list.removeIf(this::isDone);
		}
		final String search = searchText;
		if (!search.isEmpty()) {
			list.removeIf(entry -> !entry.title().getString().toLowerCase(Locale.ROOT).contains(search)
					&& !entry.description().getString().toLowerCase(Locale.ROOT).contains(search));
		}
		switch (sortMode) {
			case A_TO_Z -> list.sort(Comparator.comparing(entry -> entry.title().getString()));
			case PROGRESS -> list.sort(Comparator.comparingDouble((AdvancementEntry entry) -> percentOf(entry))
					.reversed()
					.thenComparing(entry -> entry.title().getString()));
			case INCOMPLETE -> list.sort(Comparator.comparingDouble((AdvancementEntry entry) -> percentOf(entry))
					.thenComparing(entry -> entry.title().getString()));
			case DEFAULT -> {
				// ordre vanilla conservé
			}
		}
		this.visible = list;
		goToPage(page); // re-clamp
		updateButtons();
	}

	private void goToPage(int target) {
		int max = Math.max(0, pageCount() - 1);
		int newPage = Math.max(0, Math.min(target, max));
		if (newPage != page) {
			page = newPage;
			pageChangeTime = System.currentTimeMillis();
			updateButtons();
		}
	}

	private void updateButtons() {
		if (prevButton != null) {
			prevButton.active = page > 0;
		}
		if (nextButton != null) {
			nextButton.active = page < pageCount() - 1;
		}
	}

	private boolean isDone(AdvancementEntry entry) {
		AdvancementProgress prog = clientAdvancements.progress().get(entry.node().holder());
		return prog != null && prog.isDone();
	}

	private float percentOf(AdvancementEntry entry) {
		AdvancementProgress prog = clientAdvancements.progress().get(entry.node().holder());
		return prog == null ? 0.0f : prog.getPercent();
	}

	private int pageSize() {
		int gridH = panelHeight() - GRID_TOP - BOTTOM_H;
		int rows = Math.max(1, (gridH + GAP) / (CARD_H + GAP));
		return Math.max(1, rows * COLUMNS);
	}

	private int pageCount() {
		return (visible.size() + pageSize() - 1) / pageSize();
	}

	// =====================================================================
	// Géométrie
	// =====================================================================

	private int panelWidth() {
		return Math.min(this.width - 40, 600);
	}

	private int panelHeight() {
		return this.height - 16;
	}

	private int panelX() {
		return (this.width - panelWidth()) / 2;
	}

	private int panelY() {
		return 8;
	}

	private int panelBottom() {
		return panelY() + panelHeight();
	}

	// =====================================================================
	// Rendu
	// =====================================================================

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		extractTransparentBackground(g);

		int x = panelX();
		int y = panelY();
		int w = panelWidth();
		int h = panelHeight();

		// Panneau
		g.fill(x, y, x + w, y + h, COLOR_PANEL);
		g.fillGradient(x, y, x + w, y + HEADER_H, COLOR_HEADER_TOP, COLOR_HEADER_BOTTOM);
		g.fill(x, y + HEADER_H, x + w, y + HEADER_H + 1, COLOR_BORDER);
		g.fill(x, y, x + w, y + 1, COLOR_BORDER);
		g.fill(x, y + h - 1, x + w, y + h, COLOR_BORDER);
		g.fill(x, y, x + 1, y + h, COLOR_BORDER);
		g.fill(x + w - 1, y, x + w, y + h, COLOR_BORDER);

		// Widgets (recherche, tri, boutons) au-dessus du panneau
		super.extractRenderState(g, mouseX, mouseY, partialTick);

		drawHeader(g, x, y, w);
		drawCategories(g, x, y, w, mouseX, mouseY);
		drawCards(g, x, y, w, mouseX, mouseY);
		drawBottomBar(g, x, w);
	}

	private void drawHeader(GuiGraphicsExtractor g, int x, int y, int w) {
		g.centeredText(this.font, Component.literal("✦ Progrès"), x + w / 2, y + 6, COLOR_TITLE);

		int total = allEntries.size();
		int done = 0;
		for (AdvancementEntry entry : allEntries) {
			if (isDone(entry)) {
				done++;
			}
		}
		float ratio = total == 0 ? 0f : (float) done / total;

		int bx = x + w - 260;
		int by = y + 22;
		int bw = 200;
		int bh = 10;
		g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF000000);
		g.fill(bx, by, bx + bw, by + bh, COLOR_BAR_TRACK);
		int fillW = (int) (bw * ratio);
		if (fillW > 0) {
			g.fillGradient(bx, by, bx + fillW, by + bh, COLOR_BAR_FILL_TOP, COLOR_BAR_FILL_BOTTOM);
		}
		g.text(this.font, done + " / " + total + " (" + (int) (ratio * 100) + "%)", bx + bw + 6, by + 1, COLOR_TEXT);

		String subtitle = visible.size() == allEntries.size()
				? allEntries.size() + " progrès au total"
				: visible.size() + " / " + allEntries.size() + " progrès affichés";
		g.centeredText(this.font, Component.literal(subtitle), x + w / 2, y + 20, COLOR_TEXT_DIM);
	}

	private void drawCategories(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
		chips.clear();
		int cy = y + HEADER_H + CONTROLS_H;
		int ch = 16;
		int cx = x + 8;

		// Flèche gauche si on peut défiler
		if (categoryScroll > 0) {
			int arrowW = 16;
			boolean hovered = inRect(mouseX, mouseY, cx, cy, arrowW, ch);
			g.fill(cx, cy, cx + arrowW, cy + ch, hovered ? COLOR_CHIP_HOVER : COLOR_CHIP_BG);
			g.centeredText(this.font, Component.literal("◀"), cx + arrowW / 2, cy + 4, COLOR_TEXT_DIM);
			chips.add(new CategoryChip(cx, cy, arrowW, ch, null, false, -1));
			cx += arrowW + 3;
		}

		// Chip "Tous"
		int allW = this.font.width("Tous") + 12;
		boolean allHovered = inRect(mouseX, mouseY, cx, cy, allW, ch);
		g.fill(cx, cy, cx + allW, cy + ch,
				selectedRoot == null ? COLOR_CHIP_SELECTED : allHovered ? COLOR_CHIP_HOVER : COLOR_CHIP_BG);
		g.centeredText(this.font, Component.literal("Tous"), cx + allW / 2, cy + 4, COLOR_TEXT);
		chips.add(new CategoryChip(cx, cy, allW, ch, null, true, 0));
		cx += allW + 3;

		// Chips des catégories
		for (int i = categoryScroll; i < categories.size(); i++) {
			AdvancementNode root = categories.get(i);
			DisplayInfo display = displayOf(root);
			String label = display == null ? "?" : display.title().getString();
			int cw = Math.min(this.font.width(label) + 12, 120);
			if (cx + cw > x + w - 26) {
				break;
			}
			boolean hovered = inRect(mouseX, mouseY, cx, cy, cw, ch);
			g.fill(cx, cy, cx + cw, cy + ch,
					selectedRoot == root ? COLOR_CHIP_SELECTED : hovered ? COLOR_CHIP_HOVER : COLOR_CHIP_BG);
			g.centeredText(this.font, Component.literal(truncate(label, cw - 8)), cx + cw / 2, cy + 4, COLOR_TEXT);
			chips.add(new CategoryChip(cx, cy, cw, ch, root, false, 0));
			cx += cw + 3;
		}

		// Flèche droite s'il reste des catégories masquées
		int lastEnd = x + w - 22;
		if (cx < lastEnd && categoryScroll < Math.max(0, categories.size() - 1)) {
			boolean canScroll = cx < lastEnd && categoryScroll < categories.size() - 1;
			if (canScroll) {
				int arrowW = 16;
				boolean hovered = inRect(mouseX, mouseY, lastEnd - arrowW, cy, arrowW, ch);
				g.fill(lastEnd - arrowW, cy, lastEnd, cy + ch, hovered ? COLOR_CHIP_HOVER : COLOR_CHIP_BG);
				g.centeredText(this.font, Component.literal("▶"), lastEnd - arrowW / 2, cy + 4, COLOR_TEXT_DIM);
				chips.add(new CategoryChip(lastEnd - arrowW, cy, arrowW, ch, null, false, 1));
			}
		}
	}

	private void drawCards(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
		int gx = x + 8;
		int gy = y + GRID_TOP;
		int gw = w - 16;
		int gridH = panelHeight() - GRID_TOP - BOTTOM_H;
		int cardW = (gw - GAP * (COLUMNS - 1)) / COLUMNS;

		if (visible.isEmpty()) {
			String message = allEntries.isEmpty() ? "Aucun progrès dans ce monde" : "Aucun progrès trouvé";
			g.centeredText(this.font, Component.literal(message), x + w / 2, gy + gridH / 2 - 10, COLOR_TEXT_DIM);
			if (!allEntries.isEmpty()) {
				g.centeredText(this.font, Component.literal("Essaie une autre recherche ou un autre tri"),
						x + w / 2, gy + gridH / 2 + 4, 0xFF6B7A93);
			}
			return;
		}

		int pageStart = page * pageSize();
		long elapsed = System.currentTimeMillis() - pageChangeTime;

		for (int i = 0; i < pageSize(); i++) {
			int index = pageStart + i;
			if (index >= visible.size()) {
				break;
			}
			AdvancementEntry entry = visible.get(index);
			int col = i % COLUMNS;
			int row = i / COLUMNS;
			int cx = gx + col * (cardW + GAP);
			int cy = gy + row * (CARD_H + GAP);

			// Animation d'apparition en cascade
			int alpha = (int) Math.min(255, Math.max(0, (elapsed - (long) i * 40) * 4));

			boolean hovered = inRect(mouseX, mouseY, cx, cy, cardW, CARD_H);
			g.fill(cx, cy, cx + cardW, cy + CARD_H, withAlpha(hovered ? COLOR_CARD_BG_HOVER : COLOR_CARD_BG, alpha));
			if (hovered) {
				g.fill(cx, cy, cx + cardW, cy + 1, withAlpha(0xFF88A9E8, alpha));
				g.fill(cx, cy + CARD_H - 1, cx + cardW, cy + CARD_H, withAlpha(0xFF88A9E8, alpha));
				g.fill(cx, cy, cx + 1, cy + CARD_H, withAlpha(0xFF88A9E8, alpha));
				g.fill(cx + cardW - 1, cy, cx + cardW, cy + CARD_H, withAlpha(0xFF88A9E8, alpha));
			}

			boolean done = isDone(entry);
			float percent = percentOf(entry);

			// Icône
			g.item(entry.icon(), cx + 6, cy + 8);

			// Titre (couleur selon état : vert = obtenu, or = défi, blanc = normal)
			int titleColor = done ? COLOR_DONE : entry.type() == AdvancementType.CHALLENGE ? COLOR_CHALLENGE : COLOR_TEXT;
			g.text(this.font, truncate(entry.title().getString(), cardW - 100), cx + 30, cy + 6,
					withAlpha(titleColor, alpha));

			// Badge d'état à droite
			if (done) {
				g.text(this.font, "✓", cx + cardW - 12, cy + 6, withAlpha(COLOR_DONE, alpha));
			} else if (percent > 0f) {
				g.text(this.font, (int) (percent * 100) + "%", cx + cardW - 32, cy + 6, withAlpha(COLOR_TEXT_DIM, alpha));
			}

			// Description tronquée
			g.text(this.font, truncate(entry.description().getString(), cardW - 40), cx + 30, cy + 19,
					withAlpha(COLOR_TEXT_DIM, alpha));

			// Barre de progression du progrès
			int pby = cy + CARD_H - 9;
			g.fill(cx + 6, pby, cx + cardW - 6, pby + 4, withAlpha(COLOR_BAR_TRACK, alpha));
			int pw = (int) ((cardW - 12) * percent);
			if (pw > 0) {
				g.fillGradient(cx + 6, pby, cx + 6 + pw, pby + 4,
						withAlpha(done ? COLOR_BAR_FILL_TOP : 0xFF7986CB, alpha),
						withAlpha(done ? COLOR_BAR_FILL_BOTTOM : 0xFF9FA8DA, alpha));
			}

			// Tooltip au survol : titre + description + progression des critères
			if (hovered) {
				List<Component> lines = new ArrayList<>();
				lines.add(entry.title());
				lines.add(entry.description());
				AdvancementProgress prog = clientAdvancements.progress().get(entry.node().holder());
				if (prog != null && !prog.isDone() && prog.hasProgress()) {
					lines.add(prog.getProgressText());
				}
				g.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
			}
		}
	}

	private void drawBottomBar(GuiGraphicsExtractor g, int x, int w) {
		int midX = x + w / 2;
		int by = panelBottom() - 24;
		g.centeredText(this.font, Component.literal("Page " + (page + 1) + " / " + Math.max(1, pageCount())),
				midX, by, COLOR_TEXT_DIM);
		g.centeredText(this.font, Component.literal("← → pour tourner les pages"),
				midX, panelBottom() - 10, 0xFF6B7A93);
	}

	// =====================================================================
	// Entrées (souris, clavier, molette)
	// =====================================================================

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (CategoryChip chip : chips) {
				if (inRect((int) event.x(), (int) event.y(), chip.x(), chip.y(), chip.w(), chip.h())) {
					if (chip.scrollDir() < 0) {
						categoryScroll = Math.max(0, categoryScroll - 1);
					} else if (chip.scrollDir() > 0) {
						categoryScroll = Math.min(Math.max(0, categories.size() - 1), categoryScroll + 1);
					} else if (chip.isAll()) {
						selectedRoot = null;
						applyFilters();
					} else if (chip.root() != null) {
						selectedRoot = chip.root();
						applyFilters();
					}
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY > 0) {
			goToPage(page + 1);
		} else if (scrollY < 0) {
			goToPage(page - 1);
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (searchBox != null && searchBox.isFocused()) {
			return super.keyPressed(event);
		}
		if (event.key() == KEYCODE_LEFT) {
			goToPage(page - 1);
			return true;
		}
		if (event.key() == KEYCODE_RIGHT) {
			goToPage(page + 1);
			return true;
		}
		return super.keyPressed(event);
	}

	// =====================================================================
	// Utilitaires
	// =====================================================================

	private static boolean inRect(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private static int withAlpha(int argb, int alpha) {
		return (argb & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
	}

	private String truncate(String text, int maxWidth) {
		if (this.font.width(text) <= maxWidth) {
			return text;
		}
		while (text.length() > 1 && this.font.width(text + "...") > maxWidth) {
			text = text.substring(0, text.length() - 1);
		}
		return text + "...";
	}
}
