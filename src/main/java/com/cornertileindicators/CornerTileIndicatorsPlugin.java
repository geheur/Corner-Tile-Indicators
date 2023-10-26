/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.cornertileindicators;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Corner Tile Indicators",
	description = "Tile Indicators Plugin, but only rendering the tile's corners.",
	conflicts = {"Tile Indicators"},
	tags = {"highlight", "overlay"}
)
public class CornerTileIndicatorsPlugin extends Plugin
{
	@Inject private CornerTileIndicatorsConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private CornerTileIndicatorsOverlay overlay;
	@Inject private ConfigManager configManager;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);

		copyBasePluginConfig();
	}

	private void copyBasePluginConfig()
	{
		if (config.mirrorSettings()) {
			for (String key : configManager.getConfigurationKeys("tileindicators."))
			{
				key = key.substring("tileindicators.".length());
				String value = configManager.getConfiguration("tileindicators", key);
//				System.out.println("copying " + key + " " + value);
				if (value != null) configManager.setConfiguration("cornertileindicators", key, value);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e) {
		// I don't want this to apply to profile change config changes because the config is in an invalid state
		// during a profile change, so I check the thread since the EDT is where normal config changes happen.
		if (!e.getGroup().equals("cornertileindicators") || !SwingUtilities.isEventDispatchThread()) return;
		if (e.getKey().equals("mirrorSettings") || e.getKey().contains("TileCornersOnly")) return;

//		System.out.println("changing " + e.getKey() + " to " + e.getNewValue());
		configManager.setConfiguration("tileindicators", e.getKey(), e.getNewValue());
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged e) {
		copyBasePluginConfig();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}

	@Provides
	CornerTileIndicatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CornerTileIndicatorsConfig.class);
	}
}
