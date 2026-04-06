package com.lowdragmc.photon.core.mixins.accessor;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.brigadier.arguments.ArgumentType;

@Mixin(ArgumentTypeInfos.class)
public interface ArgumentTypeInfosAccessor {
    @Invoker("register")
    static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> ArgumentTypeInfo<A, T> invokeRegister(Registry<ArgumentTypeInfo<?, ?>> registry, String id, Class<? extends A> argumentClass, ArgumentTypeInfo<A, T> info) {
        throw new UnsupportedOperationException();
    }
}
