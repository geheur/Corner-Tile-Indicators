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

import static com.cornertileindicators.CornerTileIndicatorsConfig.HoveredTileSailingMode.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ColorUtil;

public class CornerTileIndicatorsOverlay extends Overlay
{
	private final Client client;
	private final CornerTileIndicatorsConfig config;

	private WorldPoint lastPlayerPosition = new WorldPoint(0, 0, 0);
	private int lastTickPlayerMoved = 0;
	private long lastTimePlayerStoppedMoving = 0;

	@Inject
	private CornerTileIndicatorsOverlay(Client client, CornerTileIndicatorsConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.highlightHoveredTile())
		{
			WorldView wv = client.getLocalPlayer().getWorldView();

			WorldView tlwv = client.getTopLevelWorldView();
			if (wv != tlwv) {
				var sailingMode = config.hoveredTileSailingMode();
				if (
					sailingMode == WORLD_GRID ||
					sailingMode == BOTH && wv.getSelectedSceneTile() == null
				) {
					wv = tlwv;
				}
			}

			Tile tile = wv.getSelectedSceneTile();
			if (tile != null)
			{
				renderTile(graphics, tile.getLocalLocation(), config.highlightHoveredColor(), config.hoveredTileBorderWidth(), config.hoveredTileFillColor(), config.hoveredTileCornersOnly(), config.hoveredTileCornerSize());
			}
		}

		if (config.highlightDestinationTile())
		{
			renderTile(graphics, client.getLocalDestinationLocation(), config.highlightDestinationColor(), config.destinationTileBorderWidth(), config.destinationTileFillColor(), config.destinationTileCornersOnly(), config.destinationTileCornerSize());
		}

		if (config.highlightCurrentTile())
		{
			WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();

			if (!playerPos.equals(lastPlayerPosition))
			{
				lastTickPlayerMoved = client.getTickCount();
			}
			else if (lastTickPlayerMoved + 1 == client.getTickCount())
			{
				lastTimePlayerStoppedMoving = System.currentTimeMillis();
			}

			lastPlayerPosition = playerPos;

			final LocalPoint playerPosLocal = LocalPoint.fromWorld(client, lastPlayerPosition);
			if (playerPosLocal != null)
			{
				Color color = config.highlightCurrentColor();
				Color fillColor = config.currentTileFillColor();
				// When not fading out the current tile, or when it has been 1 game tick or less since the player last
				// moved, draw the tile at full opacity. When using fadeout, drawing the indicator at full opacity for 1
				// game tick prevents it from fading out when moving on consecutive ticks.
				if (!config.trueTileFadeout() || client.getTickCount() - lastTickPlayerMoved <= 1)
				{
					renderTile(graphics, playerPosLocal, color, config.currentTileBorderWidth(), fillColor, config.currentTileCornersOnly(), config.currentTileCornerSize());
				}
				else
				{
					// It is 1 game tick after the player has stopped moving, so fade out the tile.
					long timeSinceLastMove = System.currentTimeMillis() - lastTimePlayerStoppedMoving;
					// The fadeout does not begin for 1 game tick, so subtract that.
					int fadeoutTime = config.trueTileFadeoutTime() - Constants.GAME_TICK_LENGTH;
					if (fadeoutTime != 0 && timeSinceLastMove < fadeoutTime)
					{
						double opacity = 1.0d - Math.pow(timeSinceLastMove / (double) fadeoutTime, 2);
						renderTile(graphics, playerPosLocal, ColorUtil.colorWithAlpha(color, (int) (opacity * color.getAlpha())), config.currentTileBorderWidth(), ColorUtil.colorWithAlpha(fillColor, (int) (opacity * fillColor.getAlpha())), config.currentTileCornersOnly(), config.currentTileCornerSize());
					}
				}
			}
		}

		return null;
	}

	private void renderTile(final Graphics2D graphics, final LocalPoint dest, final Color color, final double borderWidth, final Color fillColor, boolean cornersOnly, int divisor)
	{
		if (dest == null)
		{
			return;
		}

		final Polygon poly = Perspective.getCanvasTilePoly(client, dest);

		if (poly == null)
		{
			return;
		}

		if (cornersOnly)
		{
			renderPolygonCorners(graphics, poly, color, fillColor, new BasicStroke((float) borderWidth), divisor);
		}
		else
		{
			OverlayUtil.renderPolygon(graphics, poly, color, fillColor, new BasicStroke((float) borderWidth));
		}
	}

	public static void renderPolygonCorners(Graphics2D graphics, Polygon poly, Color color, Color fillColor, Stroke borderStroke, int divisor)
	{
		graphics.setColor(color);
		final Stroke originalStroke = graphics.getStroke();
		graphics.setStroke(borderStroke);

		for (int i = 0; i < poly.npoints; i++)
		{
			int ptx = poly.xpoints[i];
			int pty = poly.ypoints[i];
			int prev = (i - 1) < 0 ? (poly.npoints - 1) : (i - 1);
			int next = (i + 1) > (poly.npoints - 1) ? 0 : (i + 1);
			int ptxN = ((poly.xpoints[next]) - ptx) / divisor + ptx;
			int ptyN = ((poly.ypoints[next]) - pty) / divisor + pty;
			int ptxP = ((poly.xpoints[prev]) - ptx) / divisor + ptx;
			int ptyP = ((poly.ypoints[prev]) - pty) / divisor + pty;
			graphics.drawLine(ptx, pty, ptxN, ptyN);
			graphics.drawLine(ptx, pty, ptxP, ptyP);
		}

		graphics.setColor(fillColor);
		graphics.fill(poly);

		graphics.setStroke(originalStroke);
	}
}
