package com.lowdragmc.photon;


public class PhotonConfig {
    public static final PhotonConfig INSTANCE = new PhotonConfig();

    public enum BloomMode {
        ADD,
        SCATTER
    }

    public boolean enableBloom = true;
    public int bloomMipLevel = 5;
    public double bloomThreshold = 1.0;
    public double bloomIntensity = 0.7;
    public boolean enableBloomWithIrisShader = true;
    public boolean irisShaderCompatibleMode = true;

    private PhotonConfig() {
    }
}
