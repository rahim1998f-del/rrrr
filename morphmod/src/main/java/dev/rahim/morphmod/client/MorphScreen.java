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

/** Vertical morph list on the left side of the screen (icon + name), like the original mod. */
public class MorphScreen extends Screen {
    private static final int LIST_X = 4, ROW_W = 150, ROW_H = 30, GAP = 2, BOX = 26;

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
        visible = Math.max(1, (height - 44) / (ROW_H + GAP));
        int maxScroll = Math.max(0, types.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        for (int i = 0; i < visible && scroll + i < types.size(); i++) {
            EntityType<?> t = types.get(scroll + i);
            int y = 6 + i * (ROW_H + GAP);
            addRenderableWidget(Button.builder(Component.empty(), b -> {
                MorphModClient.morph(t);
                onClose();
            }).bounds(LIST_X, y, ROW_W, ROW_H).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("morphmod.demorph"), b -> {
            MorphModClient.morph(null);
            onClose();
        }).bounds(LIST_X, height - 30, ROW_W, 20).build());
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
            g.drawString(this.font, Component.translatable("morphmod.empty"), LIST_X + ROW_W + 10, 12, 0xFFFFFFFF);
        }

        Minecraft mc = Minecraft.getInstance();
        EntityType<?> current = mc.player == null ? null : MorphState.getActive(mc.player.getUUID());

        for (int i = 0; i < visible && scroll + i < types.size(); i++) {
            EntityType<?> t = types.get(scroll + i);
            int y = 6 + i * (ROW_H + GAP);
            boolean hovered = mouseX >= LIST_X && mouseX < LIST_X + ROW_W && mouseY >= y && mouseY < y + ROW_H;
            boolean selected = t == current;

            int bx = LIST_X + 2, by = y + 2;
            g.fill(bx, by, bx + BOX, by + BOX, 0xFF101010);

            LivingEntity e = preview(t);
            if (e != null) {
                try {
                    float size = Math.max(0.5F, Math.max(e.getBbHeight(), e.getBbWidth()));
                    int scale = Math.max(3, Math.min(40, Math.round(20.0F / size)));
                    InventoryScreen.renderEntityInInventoryFollowsMouse(g, bx, by, bx + BOX, by + BOX,
                            scale, 0.0625F, mouseX, mouseY, e);
                } catch (Throwable ex) {
                    failed.add(t);
                }
            }

            String name = t.getDescription().getString();
            int maxW = ROW_W - BOX - 12;
            if (this.font.width(name) > maxW) {
                name = this.font.plainSubstrByWidth(name, maxW - 8) + "...";
            }
            int color = (hovered || selected) ? 0xFFFFFF55 : 0xFFFFFFFF;
            g.drawString(this.font, Component.literal(name), bx + BOX + 6, y + (ROW_H - 8) / 2, color);
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
