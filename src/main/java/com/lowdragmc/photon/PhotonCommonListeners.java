package com.lowdragmc.photon;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.util.List;

public class PhotonCommonListeners {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            List<LiteralArgumentBuilder<CommandSourceStack>> commands = ServerCommands.createServerCommands();
            commands.forEach(dispatcher::register);
        });
    }
}
