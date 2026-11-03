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

public class MorphScreen extends Screen {
    private static final int TILE_W = 64, TILE_H = 80, GAP = 4;

    private final List<EntityType<?>> types;
    private final Map<EntityType<?>, LivingEntity> previews = new HashMap<>();
    private final Set<EntityType<?>> failed = new HashSet<>();
    private int page, cols, perPage, pages, startX, startY;

    public MorphScreen() {
        super(Component.translatable("morphmod.screen.title"));
        this.types = MorphState.unlocked();
    }

    @Override
    protected void init() {
        cols = Math.max(1, Math.min(10, (width - 30) / (TILE_W + GAP)));
        int rows = Math.max(1, (height - 90) / (TILE_H + GAP));
        perPage = cols * rows;
        pages = Math.max(1, (types.size() + perPage - 1) / perPage);
        if (page >= pages) page = pages - 1;
        if (page < 0) page = 0;

        startX = (width - (cols * (TILE_W + GAP) - GAP)) / 2;
        startY = 28;
        int from = page * perPage;

        for (int i = 0; i < perPage && from + i < types.size(); i++) {
            EntityType<?> t = types.get(from + i);
            int x = startX + (i % cols) * (TILE_W + GAP);
            int y = startY + (i / cols) * (TILE_H + GAP);
            addRenderableWidget(Button.builder(Component.empty(), b -> {
                MorphModClient.morph(t);
                onClose();
            }).bounds(x, y, TILE_W, TILE_H).build());
        }

        int by = height - 28;
        Button prev = Button.builder(Component.literal("<"), b -> {
            page--;
            rebuildWidgets();
        }).bounds(width / 2 - 125, by, 30, 20).build();
        prev.active = page > 0;
        addRenderableWidget(prev);

        addRenderableWidget(Button.builder(Component.translatable("morphmod.demorph"), b -> {
            MorphModClient.morph(null);
            onClose();
        }).bounds(width / 2 - 90, by, 180, 20).build());

        Button next = Button.builder(Component.literal(">"), b -> {
            page++;
            rebuildWidgets();
        }).bounds(width / 2 + 95, by, 30, 20).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);
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
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        int from = page * perPage;
        for (int i = 0; i < perPage && from + i < types.size(); i++) {
            EntityType<?> t = types.get(from + i);
            int x = startX + (i % cols) * (TILE_W + GAP);
            int y = startY + (i / cols) * (TILE_H + GAP);

            LivingEntity e = preview(t);
            if (e != null) {
                try {
                    float size = Math.max(0.5F, Math.max(e.getBbHeight(), e.getBbWidth()));
                    int scale = Math.max(3, Math.min(60, Math.round(34.0F / size)));
                    InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 2, y + 4, x + TILE_W - 2,
                            y + TILE_H - 18, scale, 0.0625F, mouseX, mouseY, e);
                } catch (Throwable ex) {
                    failed.add(t);
                }
            }

            String name = t.getDescription().getString();
            if (this.font.width(name) > TILE_W - 4) {
                name = this.font.plainSubstrByWidth(name, TILE_W - 12) + "...";
            }
            g.drawCenteredString(this.font, Component.literal(name), x + TILE_W / 2, y + TILE_H - 14, 0xFFFFFFFF);
        }

        g.drawCenteredString(this.font, Component.literal((page + 1) + " / " + pages),
                this.width / 2, this.height - 42, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0 && page < pages - 1) {
            page++;
            rebuildWidgets();
            return true;
        }
        if (scrollY > 0 && page > 0) {
            page--;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
