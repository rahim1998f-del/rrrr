package dev.rahim.morphmod.client;

import dev.rahim.morphmod.MorphState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public class MorphScreen extends Screen {
    private final List<EntityType<?>> types;
    private int page;

    public MorphScreen() {
        super(Component.translatable("morphmod.screen.title"));
        this.types = MorphState.unlocked();
    }

    @Override
    protected void init() {
        int bw = 110, bh = 20, gap = 4;
        int cols = Math.max(1, Math.min(5, (width - 20) / (bw + gap)));
        int rows = Math.max(1, (height - 80) / (bh + gap));
        int perPage = cols * rows;
        int pages = Math.max(1, (types.size() + perPage - 1) / perPage);
        if (page >= pages) page = pages - 1;

        int startX = (width - (cols * (bw + gap) - gap)) / 2;
        int startY = 30;
        int from = page * perPage;

        for (int i = 0; i < perPage && from + i < types.size(); i++) {
            EntityType<?> t = types.get(from + i);
            int x = startX + (i % cols) * (bw + gap);
            int y = startY + (i / cols) * (bh + gap);
            addRenderableWidget(Button.builder(t.getDescription(), b -> {
                MorphModClient.morph(t);
                onClose();
            }).bounds(x, y, bw, bh).build());
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }
}
