package com.lowdragmc.photon.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.List;

@Environment(EnvType.CLIENT)
public class PhotonClientListeners {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            List<LiteralArgumentBuilder<FabricClientCommandSource>> commands = ClientCommands.createClientCommands();
            for (var builder : commands) {
                dispatcher.register(builder);
            }
        });
    }
}
