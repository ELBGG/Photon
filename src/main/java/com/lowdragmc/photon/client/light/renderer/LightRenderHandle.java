package com.lowdragmc.photon.client.light.renderer;

import com.lowdragmc.photon.client.light.data.PointLightData;

/**
 * Handle to a registered point light.
 * Kept as a concrete class for API compatibility with LightFXObject.
 */
public final class LightRenderHandle extends AbstractLightRenderer.LightHandle<PointLightData> {

    LightRenderHandle(PointLightData data, PointLightRenderer owner) {
        super(data, owner);
    }

    @Override
    public PointLightData getData() { return super.getData(); }
}
