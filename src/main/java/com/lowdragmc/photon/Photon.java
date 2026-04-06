package com.lowdragmc.photon;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import net.fabricmc.api.ModInitializer;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Photon implements ModInitializer {
    public static final String MOD_ID = "photon";
    public static final String NAME = "Photon";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    @Override
    public void onInitialize() {
        Photon.init();
        PhotonCommonProxy.init();
    }

    public static void init() {
        LOGGER.info("{} is initializing on platform: {}", NAME, Platform.platformName());
        if (new File(LDLib2.getAssetsDir(), "photon").mkdirs()) {
            LOGGER.info("Created photon assets folder");
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isShaderModInstalled() {
        return LDLib2.isModLoaded("iris") || LDLib2.isModLoaded("oculus");
    }

    public static boolean isUsingShaderPack() {
        if (isShaderModInstalled()) {
            return IrisApi.getInstance().isShaderPackInUse();
        }
        return false;
    }
}

