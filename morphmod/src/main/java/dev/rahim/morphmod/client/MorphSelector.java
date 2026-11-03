package dev.rahim.morphmod.client;

import dev.rahim.morphmod.MorphState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HUD morph selector in the style of the classic Morph mod: a column of tiles sliding in from
 * the left edge. The selected tile stays at a fixed spot, the list scrolls underneath it.
 * Keys: [ and ] move, Enter confirms, Backspace (or any screen) cancels. Entry 0 = human.
 */
public final class MorphSelector {
    private static final int SIZE = 50;
    private static final int TOP = 60;
    private static final int SHOW_TIME = 8;

    private static boolean show;
    private static int showTime;
    private static List<EntityType<?>> entries = new ArrayList<>();
    private static int index;
    private static float shown, shownPrev;

    private static final Map<EntityType<?>, LivingEntity> PREVIEWS = new HashMap<>();
    private static final Set<EntityType<?>> FAILED = new HashSet<>();
    private static Object previewLevel;

    private MorphSelector() {}

    public static void tick(Minecraft mc, KeyMapping up, KeyMapping down, KeyMapping confirm, KeyMapping cancel) {
        int ups = 0, downs = 0, confirms = 0, cancels = 0;
        while (up.consumeClick()) ups++;
        while (down.consumeClick()) downs++;
        while (confirm.consumeClick()) confirms++;
        while (cancel.consumeClick()) cancels++;

        if (mc.player == null || mc.screen != null) {
            show = false;
        } else if (!show) {
            if (ups > 0 || downs > 0) open(mc);
        } else {
            int n = entries.size();
            if (n > 0) {
                index = Math.floorMod(index - ups + downs, n);
            }
            if (confirms > 0 && n > 0) {
                EntityType<?> t = entries.get(index);
                show = false;
                MorphModClient.morph(t == EntityType.PLAYER ? null : t);
            } else if (cancels > 0) {
                show = false;
            }
        }

        shownPrev = shown;
        shown += (index - shown) * 0.5F;
        if (Math.abs(index - shown) < 0.01F) shown = index;

        showTime = show ? Math.min(SHOW_TIME, showTime + 1) : Math.max(0, showTime - 1);
    }

    private static void open(Minecraft mc) {
        entries = new ArrayList<>();
        entries.add(EntityType.PLAYER); // human
        entries.addAll(MorphState.unlocked());

        EntityType<?> current = MorphState.getActive(mc.player.getUUID());
        int i = current == null ? 0 : entries.indexOf(current);
        index = Math.max(0, i);
        shown = shownPrev = index;
        show = true;
    }

    private static LivingEntity preview(Minecraft mc, EntityType<?> t) {
        if (t == EntityType.PLAYER) return mc.player;
        if (previewLevel != mc.level) {
            PREVIEWS.clear();
            FAILED.clear();
            previewLevel = mc.level;
        }
        if (FAILED.contains(t)) return null;
        LivingEntity e = PREVIEWS.get(t);
        if (e != null) return e;
        try {
            Entity en = t.create(mc.level, EntitySpawnReason.COMMAND);
            if (en instanceof LivingEntity le) {
                PREVIEWS.put(t, le);
                return le;
            }
        } catch (Throwable ignored) {
        }
        FAILED.add(t);
        return null;
    }

    private static void drawFrame(GuiGraphics g, int x, int y, boolean selected) {
        if (selected) {
            g.fill(x, y, x + SIZE, y + SIZE, 0xFFD8D8D8);
            g.fill(x + 2, y + 2, x + SIZE - 2, y + SIZE - 2, 0xB0606060);
        } else {
            g.fill(x, y, x + SIZE, y + SIZE, 0xFF1A1A1A);
            g.fill(x + 2, y + 2, x + SIZE - 2, y + SIZE - 2, 0x90101010);
        }
    }

    public static void render(GuiGraphics g, float partial) {
        if (showTime <= 0 || entries.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float p = show ? (showTime + partial) / SHOW_TIME : (showTime - partial) / SHOW_TIME;
        p = Math.max(0.0F, Math.min(1.0F, p));
        p = p * p * (3.0F - 2.0F * p);
        int posX = Math.round(-SIZE * (1.0F - p));

        float sh = shownPrev + (shown - shownPrev) * partial;
        int guiH = mc.getWindow().getGuiScaledHeight();
        int n = entries.size();
        int first = Math.max(0, (int) Math.floor(sh - (float) TOP / SIZE) - 1);
        int last = Math.min(n - 1, (int) Math.ceil(sh + (float) (guiH - TOP) / SIZE) + 1);

        EntityType<?> current = MorphState.getActive(mc.player.getUUID());
        if (current == null) current = EntityType.PLAYER;

        // pass 1: frames
        for (int i = first; i <= last; i++) {
            int y = TOP + Math.round((i - sh) * SIZE);
            drawFrame(g, posX, y, false);
        }
        drawFrame(g, posX, TOP, true);

        // pass 2: mobs and names
        for (int i = first; i <= last; i++) {
            EntityType<?> t = entries.get(i);
            int y = TOP + Math.round((i - sh) * SIZE);

            LivingEntity e = preview(mc, t);
            if (e != null) {
                MorphModClient.bypassSwap = (t == EntityType.PLAYER);
                try {
                    float w = Math.max(e.getBbWidth(), e.getBbHeight());
                    float scale = 0.4F * SIZE / Math.max(1.0F, w / 1.95F);
                    InventoryScreen.renderEntityInInventoryFollowsMouse(g, posX + 3, y + 3, posX + SIZE - 3,
                            y + SIZE - 3, Math.max(3, Math.round(scale)), 0.0625F,
                            posX + SIZE / 2.0F, y + SIZE / 2.0F, e);
                } catch (Throwable ex) {
                    FAILED.add(t);
                } finally {
                    MorphModClient.bypassSwap = false;
                }
            }

            Component name = t == EntityType.PLAYER
                    ? Component.translatable("morphmod.human")
                    : t.getDescription();
            int color = (i == index) ? 0xFFFFFF55 : (t == current ? 0xFFFFAA00 : 0xFFFFFFFF);
            g.drawString(mc.font, name, posX + SIZE + 5, y + (SIZE - 9) / 2, color);
        }
    }
}
