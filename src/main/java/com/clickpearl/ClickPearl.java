package com.clickpearl;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClickPearl implements ModInitializer {
	public static final String MOD_ID = "clickpearl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Click Pearl mod initialized!");
	}
}
