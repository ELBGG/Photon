package com.lowdragmc.photon.client;

import com.lowdragmc.photon.client.postprocessing.PostProcessing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;

import java.util.List;

@Environment(EnvType.CLIENT)
public class PhotonClientListeners {

    static boolean postTestActive = false;
    static PostProcessing postTestEffect = PostProcessing.BLOOM_UNREAL;

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            List<LiteralArgumentBuilder<FabricClientCommandSource>> commands = ClientCommands.createClientCommands();
            for (var builder : commands) {
                dispatcher.register(builder);
            }
        });

        // Queue a draw every render frame (not every tick) so the effect has geometry
        // on each frame without flickering. WorldRenderEvents.START fires before
        // renderClouds(), i.e. before our LevelRendererMixin calls renderAll().
        WorldRenderEvents.START.register(context -> {
            if (!postTestActive) return;
            var mc = Minecraft.getInstance();
            if (mc.player == null) return;
            var player = mc.player;
            var box = new AABB(
                    player.getX() - 0.5, player.getY(),       player.getZ() - 0.5,
                    player.getX() + 0.5, player.getY() + 2.0, player.getZ() + 0.5);
            var cam = mc.gameRenderer.getMainCamera().getPosition();
            postTestEffect.postEntity(bufferSource -> {
                var buf = bufferSource.getBuffer(RenderType.lines());
                PoseStack stack = new PoseStack();
                stack.translate(-cam.x, -cam.y, -cam.z);
                LevelRenderer.renderLineBox(stack, buf, box, 1f, 1f, 1f, 1f);
            });
        });
    }
}
