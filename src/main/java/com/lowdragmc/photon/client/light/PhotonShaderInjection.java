package com.lowdragmc.photon.client.light;

import java.util.*;
import java.util.function.Function;

/**
 * Registry for per-shader GLSL injection callbacks.
 * Ported from Shimmer's ShaderInjection.
 */
public final class PhotonShaderInjection {
    private static final Map<String, List<Function<String, String>>> VSH = new HashMap<>();
    private static final Map<String, List<Function<String, String>>> FSH = new HashMap<>();

    private PhotonShaderInjection() {}

    public static void registerVSH(String shaderName, Function<String, String> transform) {
        VSH.computeIfAbsent(shaderName, s -> new ArrayList<>()).add(transform);
    }

    public static void registerFSH(String shaderName, Function<String, String> transform) {
        FSH.computeIfAbsent(shaderName, s -> new ArrayList<>()).add(transform);
    }

    public static boolean hasVSH(String name) { return VSH.containsKey(name); }
    public static boolean hasFSH(String name) { return FSH.containsKey(name); }

    public static String injectVSH(String name, String src) {
        for (var fn : VSH.getOrDefault(name, Collections.emptyList())) src = fn.apply(src);
        return src;
    }

    public static String injectFSH(String name, String src) {
        for (var fn : FSH.getOrDefault(name, Collections.emptyList())) src = fn.apply(src);
        return src;
    }
}
