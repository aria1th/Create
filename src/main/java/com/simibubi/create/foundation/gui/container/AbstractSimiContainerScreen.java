package com.simibubi.create.foundation.gui.container;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.TickableGuiEventListener;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;

import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.simibubi.create.lib.mixin.accessor.ScreenAccessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
@ParametersAreNonnullByDefault
public abstract class AbstractSimiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

	protected int windowXOffset, windowYOffset;

	public AbstractSimiContainerScreen(T container, Inventory inv, Component title) {
		super(container, inv, title);
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowSize(int width, int height) {
		imageWidth = width;
		imageHeight = height;
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowOffset(int xOffset, int yOffset) {
		windowXOffset = xOffset;
		windowYOffset = yOffset;
	}

	@Override
	protected void init() {
		super.init();
		leftPos += windowXOffset;
		topPos += windowYOffset;
	}

	@Override
	protected void containerTick() {
		for (GuiEventListener listener : children()) {
			if (listener instanceof TickableGuiEventListener tickable) {
				tickable.tick();
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected <W extends GuiEventListener & Widget & NarratableEntry> void addRenderableWidgets(W... widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected <W extends GuiEventListener & Widget & NarratableEntry> void addRenderableWidgets(Collection<W> widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected void removeWidgets(GuiEventListener... widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	protected void removeWidgets(Collection<? extends GuiEventListener> widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		partialTicks = minecraft.getFrameTime();

		renderBackground(matrixStack);

		super.render(matrixStack, mouseX, mouseY, partialTicks);

		renderForeground(matrixStack, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
		// no-op to prevent screen- and inventory-title from being rendered at incorrect location
		// could also set this.titleX/Y and this.playerInventoryTitleX/Y to the proper values instead
	}

	protected void renderForeground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		renderTooltip(matrixStack, mouseX, mouseY);
		for (Widget widget : ((ScreenAccessor) this).create$getRenderables()) {
			if (widget instanceof AbstractSimiWidget simiWidget && simiWidget.isHovered()) {
				List<Component> tooltip = simiWidget.getToolTip();
				if (!tooltip.isEmpty())
					renderComponentTooltip(matrixStack, tooltip, mouseX, mouseY);
			}
		}
	}

	public int getLeftOfCentered(int textureWidth) {
		return leftPos - windowXOffset + (imageWidth - textureWidth) / 2;
	}

	public void renderPlayerInventory(PoseStack ms, int x, int y) {
		AllGuiTextures.PLAYER_INVENTORY.render(ms, x, y, this);
		font.draw(ms, playerInventoryTitle, x + 8, y + 6, 0x404040);
	}

	/**
	 * Used for moving JEI out of the way of extra things like block renders.
	 *
	 * @return the space that the GUI takes up outside the normal rectangle defined by {@link ContainerScreen}.
	 */
	public List<Rect2i> getExtraAreas() {
		return Collections.emptyList();
	}

	@Deprecated
	protected void debugWindowArea(PoseStack matrixStack) {
		fill(matrixStack, leftPos + imageWidth, topPos + imageHeight, leftPos, topPos, 0xD3D3D3D3);
	}

	@Deprecated
	protected void debugExtraAreas(PoseStack matrixStack) {
		for (Rect2i area : getExtraAreas()) {
			fill(matrixStack, area.getX() + area.getWidth(), area.getY() + area.getHeight(), area.getX(), area.getY(), 0xD3D3D3D3);
		}
	}

}
