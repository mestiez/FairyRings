package com.zooi.fairy;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public class FairyRingsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		//BlockRenderLayerMap.INSTANCE.putBlock(FairyRings.BlocksItems.Blocks.WHITE_MUSHROOM, RenderLayer.getCutout());
	}
}