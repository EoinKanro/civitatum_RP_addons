package team.civitatum.couple.client;

import java.util.UUID;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import team.civitatum.couple.CarryHooks;
import team.civitatum.couple.core.Interaction;
import team.civitatum.couple.core.RadialLayout;
import team.civitatum.couple.network.CoupleNetwork;

public final class RadialScreen extends Screen {
    private final UUID target;
    private final Component targetName;
    private final boolean release;
    private int selected = -1;
    private boolean committed;
    private int frames;
    public RadialScreen(Player target) {
        this(target.getUUID(), target.getDisplayName(), CarryHooks.occupied(net.minecraft.client.Minecraft.getInstance().player));
    }
    public RadialScreen(UUID target, Component name, boolean release) {
        super(Component.translatable("ui.civitatum_couple.title"));
        this.target = target; this.targetName = name; this.release = release;
    }
    @Override protected void init() {
        if (frames == 0) GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), minecraft.getWindow().getScreenWidth() / 2.0, minecraft.getWindow().getScreenHeight() / 2.0);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() {
        if (minecraft.player == null || !minecraft.player.isAlive()) { onClose(); return; }
        // Screens reset KeyMapping state on opening. Poll the physical rebound key instead.
        if (frames > 1 && !ClientEvents.held(CoupleClient.MENU)) {
            selected = selectionAt(minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth(),
                    minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight());
            commit();
        }
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        frames++;
        int cx = width / 2, cy = height / 2;
        float outer = outerRadius();
        float inner = outer * 0.43F;
        selected = RadialLayout.select(mouseX - cx, mouseY - cy, inner, outer, 3);
        g.fill(0, 0, width, height, 0x65080A0E);
        g.drawCenteredString(font, title, cx, (int)(cy - outer - 23), 0xFFEAE3DA);
        g.flush();
        for (int i = 0; i < 3; i++) ring(g, cx, cy, inner, outer, i, 3, i == selected ? 0xF05B3945 : 0xED20232B);
        g.flush();
        for (int i = 0; i < 3; i++) {
            double angle = RadialLayout.centerAngle(i, 3);
            int x = (int)(cx + Math.cos(angle) * outer * 0.72), y = (int)(cy + Math.sin(angle) * outer * 0.72);
            int color = i == selected ? 0xFFFFD6C9 : 0xFFDAD5CC;
            Component text = Component.translatable(i == 0 ? (release ? "action.civitatum_couple.release" : "action.civitatum_couple.carry")
                    : i == 1 ? "action.civitatum_couple.kiss" : "ui.civitatum_couple.settings");
            var lines = font.split(text, Math.max(58, (int)(outer * 0.92)));
            int lineY = y - lines.size() * 5;
            for (var line : lines) { g.drawString(font, line, x - font.width(line) / 2, lineY, color, false); lineY += 10; }
        }
        String name = font.plainSubstrByWidth(targetName.getString(), (int)(inner * 1.8));
        g.drawCenteredString(font, name, cx, cy - 10, 0xFFF3EDE4);
        g.drawCenteredString(font, Component.translatable("ui.civitatum_couple.cancel"), cx, cy + 5, 0xFF9198A4);
        g.drawCenteredString(font, Component.translatable("ui.civitatum_couple.release_key", CoupleClient.MENU.getTranslatedKeyMessage()), cx, (int)(cy + outer + 12), 0xFFC9C2B8);
    }
    private void commit() {
        if (committed) return;
        committed = true;
        minecraft.setScreen(null);
        if (selected == 2) { minecraft.setScreen(new SettingsScreen(null)); return; }
        if (selected == 0 || selected == 1) {
            Interaction action = selected == 1 ? Interaction.KISS : release ? Interaction.RELEASE : Interaction.CARRY;
            PacketDistributor.sendToServer(new CoupleNetwork.Action(target, action.ordinal()));
        }
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            selected = selectionAt(x, y);
            commit(); return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { onClose(); return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public void onClose() { committed = true; minecraft.setScreen(null); }

    private float outerRadius() { return Math.max(56, Math.min(106, Math.min(width / 2F - 14, height / 2F - 34))); }
    private int selectionAt(double x, double y) {
        float outer = outerRadius();
        return RadialLayout.select(x - width / 2.0, y - height / 2.0, outer * 0.43, outer, 3);
    }

    private static void ring(GuiGraphics graphics, float cx, float cy, float inner, float outer, int segment, int count, int color) {
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer b = graphics.bufferSource().getBuffer(RenderType.gui());
        double center = RadialLayout.centerAngle(segment, count), span = Math.PI * 2 / count;
        double start = center - span / 2 + 0.035, end = center + span / 2 - 0.035;
        int steps = 32;
        for (int i = 0; i < steps; i++) {
            double a = start + (end - start) * i / steps, n = start + (end - start) * (i + 1) / steps;
            // GUI projection reverses winding: emit the same front-face order as GuiGraphics.fill.
            vertex(b,matrix,cx,cy,inner,a,color); vertex(b,matrix,cx,cy,inner,n,color);
            vertex(b,matrix,cx,cy,outer,n,color); vertex(b,matrix,cx,cy,outer,a,color);
        }
    }
    private static void vertex(VertexConsumer b, Matrix4f m, float cx, float cy, float radius, double angle, int color) {
        b.addVertex(m, cx + (float)Math.cos(angle) * radius, cy + (float)Math.sin(angle) * radius, 0).setColor(color);
    }
}
