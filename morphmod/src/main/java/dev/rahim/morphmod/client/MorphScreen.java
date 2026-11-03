package dev.rahim.morphmod.client;

import dev.rahim.morphmod.MorphState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Morph list in the style of the classic Morph mod: a column of icon tiles glued to the
 * left edge of the screen, with the mob name to the right of each tile.
 */
public class MorphScreen extends Screen {
    private static final int TILE = 40;      // tile size (square)
    private static final int STEP = TILE + 1; // tile + 1px gap
    private static final int CLICK_W = 150;   // clickable width (tile + name)

    private final List<EntityType<?>> types;
    private final Map<EntityType<?>, LivingEntity> previews = new HashMap<>();
    private final Set<EntityType<?>> failed = new HashSet<>();
    private int scroll, visible;

    public MorphScreen() {
        super(Component.translatable("morphmod.screen.title"));
        this.types = MorphState.unlocked();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** No dark/blurred background: the world stays visible behind the list. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void init() {
        visible = Math.max(1, (height - 26) / STEP);
        int maxScroll = Math.max(0, types.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        for (int i = 0; i < visible && scroll + i < types.size(); i++) {
            EntityType<?> t = types.get(scroll + i);
            Button b = Button.builder(Component.empty(), btn -> {
                MorphModClient.morph(t);
                onClose();
            }).bounds(0, i * STEP, CLICK_W, TILE).build();
            b.setAlpha(0.0F); // invisible hit-box, the tile is drawn in render()
            addRenderableWidget(b);
        }

        addRenderableWidget(Button.builder(Component.translatable("morphmod.demorph"), btn -> {
            MorphModClient.morph(null);
            onClose();
        }).bounds(0, height - 22, CLICK_W, 20).build());
    }

    private LivingEntity preview(EntityType<?> t) {
        if (failed.contains(t)) return null;
        LivingEntity e = previews.get(t);
        if (e != null) return e;
        try {
            Entity en = t.create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND);
            if (en instanceof LivingEntity le) {
                previews.put(t, le);
                return le;
            }
        } catch (Throwable ignored) {
        }
        failed.add(t);
        return null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        if (types.isEmpty()) {
            g.drawString(this.font, Component.translatable("morphmod.empty"), 8, 8, 0xFFFFFFFF);
        }

        Minecraft mc = Minecraft.getInstance();
        EntityType<?> current = mc.player == null ? null : MorphState.getActive(mc.player.getUUID());

        for (int i = 0; i < visible && scroll + i < types.size(); i++) {
            EntityType<?> t = types.get(scroll + i);
            int y = i * STEP;
            boolean hovered = mouseX >= 0 && mouseX < CLICK_W && mouseY >= y && mouseY < y + TILE;
            boolean selected = t == current;
            boolean lit = hovered || selected;

            // tile frame + background
            g.fill(0, y, TILE, y + TILE, lit ? 0xFFE6E6F5 : 0xFF2C2C3C);
            g.fill(1, y + 1, TILE - 1, y + TILE - 1, lit ? 0xE0505068 : 0xE0181824);

            LivingEntity e = preview(t);
            if (e != null) {
                try {
                    float size = Math.max(0.5F, Math.max(e.getBbHeight(), e.getBbWidth()));
                    int scale = Math.max(3, Math.min(40, Math.round(24.0F / size)));
                    InventoryScreen.renderEntityInInventoryFollowsMouse(g, 2, y + 2, TILE - 2, y + TILE - 2,
                            scale, 0.0625F, mouseX, mouseY, e);
                } catch (Throwable ex) {
                    failed.add(t);
                }
            }

            String name = t.getDescription().getString();
            int maxW = CLICK_W - TILE - 8;
            if (this.font.width(name) > maxW) {
                name = this.font.plainSubstrByWidth(name, maxW - 8) + "...";
            }
            g.drawString(this.font, Component.literal(name), TILE + 5, y + (TILE - 8) / 2,
                    lit ? 0xFFFFFF55 : 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, types.size() - visible);
        if (scrollY < 0 && scroll < maxScroll) {
            scroll++;
            rebuildWidgets();
            return true;
        }
        if (scrollY > 0 && scroll > 0) {
            scroll--;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
