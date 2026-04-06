package com.lowdragmc.photon.client;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.*;
import com.lowdragmc.photon.client.fx.compat.FXCompat;
import com.lowdragmc.photon.client.gameobject.FXObject;
import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.ParticleQueueRenderType;
import com.lowdragmc.photon.client.fx.BlockEffectExecutor;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.core.mixins.accessor.ParticleEngineAccessor;
import com.lowdragmc.photon.gui.editor.FXEditor;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.network.chat.ClickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.lowdragmc.lowdraglib2.client.ClientCommands.createLiteral;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote ClientCommands
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
public class ClientCommands {

    public static List<LiteralArgumentBuilder<FabricClientCommandSource>> createClientCommands() {
        return List.of(
                createLiteral("photon_editor").executes(context -> {
                    if (Platform.getMinecraftServer() != null && !Platform.getMinecraftServer().isSingleplayer()) {
                        context.getSource().sendError(Component.literal("This command can only be used in singleplayer"));
                        return 0;
                    }
                    var minecraft = Minecraft.getInstance();
                    var entityPlayer = minecraft.player;
                    if (entityPlayer == null) return 0;
                    var ui = new ModularUI(UI.of(EditorWindow.open(FXEditor.WINDOW_ID, FXEditor::new).setId("fx_editor")))
                            .shouldCloseOnEsc(false)
                            .shouldCloseOnKeyInventory(false);
                    var screen = new ModularUIScreen(ui, Component.empty());
                    minecraft.tell(() -> minecraft.setScreen(screen));
                    return 1;
                }),
                createLiteral("photon_client")
                        .then(createLiteral("clear_particles")
                                .executes(context -> {
                                    if (Minecraft.getInstance().particleEngine instanceof ParticleEngineAccessor accessor) {
                                        accessor.getParticles().entrySet().removeIf(entry ->
                                                entry.getKey() instanceof ParticleQueueRenderType ||
                                                entry.getKey() == FXObject.NO_RENDER_RENDER_TYPE);
                                    }
                                    EntityEffectExecutor.CACHE.clear();
                                    BlockEffectExecutor.CACHE.clear();
                                    return 1;
                                }))
                        .then(createLiteral("clear_client_fx_cache")
                                .executes(context -> {
                                    if (Minecraft.getInstance().player != null) {
                                        Minecraft.getInstance().player.sendSystemMessage(Component.literal("clear client cache fx: " + FXHelper.clearCache()));
                                    } else {
                                        FXHelper.clearCache();
                                    }
                                    return 1;
                                }))
                        .then(createLiteral("convert")
                                .executes(context -> {
                                    if (Minecraft.getInstance().player != null) {
                                        Minecraft.getInstance().player.sendSystemMessage(
                                                Component.literal("trying to convert photon 1 fx under the ")
                                                        .append(Component.literal("[ldlib2/assets/photon/fx_old]")
                                                                .withStyle(style -> style.withColor(0xff008000)
                                                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                                                                                LDLib2.getAssetsDir() + "/photon/fx_old")))
                                                        )
                                                        .append(Component.literal(" folder"))
                                        );
                                    }
                                    var converted = FXCompat.convertFX();
                                    if (Minecraft.getInstance().player != null) {
                                        Minecraft.getInstance().player.sendSystemMessage(
                                                Component.literal("convert result: " + converted)
                                        );
                                    }
                                    return 1;
                                }))
        );
    }
}
