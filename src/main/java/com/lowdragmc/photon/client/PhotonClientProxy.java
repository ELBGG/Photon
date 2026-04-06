package com.lowdragmc.photon.client;

import com.lowdragmc.photon.PhotonNetworking;
import dev.felnull.specialmodelloader.api.event.SpecialModelLoaderEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

@Environment(EnvType.CLIENT)
public class PhotonClientProxy implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PhotonShaders.init());
        PhotonShaders.registerShaders();
        PhotonNetworking.registerClientHandlers();
        PhotonClientListeners.init();
        registerObjModelScope();
    }

    /**
     * Tell SpecialModelLoader to process OBJ models from the photon and ldlib2 namespaces.
     * These use the "neoforge:obj" loader format, handled automatically by SpecialModelLoader's
     * NeoForgeCompat layer.
     */
    private static void registerObjModelScope() {
        SpecialModelLoaderEvents.LOAD_SCOPE.register(() -> (resourceManager, location) -> {
            String ns = location.getNamespace();
            return ns.equals("photon") || ns.equals("ldlib2");
        });
    }
}
