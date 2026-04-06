package com.lowdragmc.photon;

import com.lowdragmc.photon.command.EntityEffectCommand;
import com.lowdragmc.photon.command.FxLocationArgument;
import com.lowdragmc.photon.core.mixins.accessor.ArgumentTypeInfosAccessor;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class PhotonCommonProxy {

    public static void init() {
        PhotonNetworking.registerPayloads();
        
        ArgumentTypeInfosAccessor.invokeRegister(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, Photon.id("fx_location").toString(),
                FxLocationArgument.class, SingletonArgumentInfo.contextFree(FxLocationArgument::new));
                
        ArgumentTypeInfosAccessor.invokeRegister(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, Photon.id("fx_auto_rotate").toString(),
                EntityEffectCommand.AutoRotateType.class, SingletonArgumentInfo.contextFree(EntityEffectCommand.AutoRotateType::new));

        PhotonRegistries.init();
        PhotonCommonListeners.init();
    }
}
