package com.lowdragmc.photon;

import com.lowdragmc.photon.command.BlockEffectCommand;
import com.lowdragmc.photon.command.EntityEffectCommand;
import com.lowdragmc.photon.command.RemoveBlockEffectCommand;
import com.lowdragmc.photon.command.RemoveEntityEffectCommand;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PhotonNetworking {

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(BlockEffectCommand.TYPE, BlockEffectCommand.CODEC);
        PayloadTypeRegistry.playS2C().register(EntityEffectCommand.TYPE, EntityEffectCommand.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoveBlockEffectCommand.TYPE, RemoveBlockEffectCommand.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoveEntityEffectCommand.TYPE, RemoveEntityEffectCommand.CODEC);
    }

    public static void registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(BlockEffectCommand.TYPE, (payload, context) -> context.client().execute(() -> BlockEffectCommand.execute(payload)));
        ClientPlayNetworking.registerGlobalReceiver(EntityEffectCommand.TYPE, (payload, context) -> context.client().execute(() -> EntityEffectCommand.execute(payload)));
        ClientPlayNetworking.registerGlobalReceiver(RemoveBlockEffectCommand.TYPE, (payload, context) -> context.client().execute(() -> RemoveBlockEffectCommand.execute(payload)));
        ClientPlayNetworking.registerGlobalReceiver(RemoveEntityEffectCommand.TYPE, (payload, context) -> context.client().execute(() -> RemoveEntityEffectCommand.execute(payload)));
    }
}
