package org.example;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.joml.Vector2d;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.events.client.input.EventMouse;
import org.rusherhack.client.api.events.render.EventRender2D;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.setting.ColorSetting;
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.event.stage.Stage;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.NumberSetting;
import org.rusherhack.core.setting.Setting;
import org.rusherhack.core.utils.MathUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.awt.Color.RED;
import static java.awt.Color.WHITE;

public class ShulkerViewer extends ToggleableModule {
	public Setting<Boolean> dynamicColor = new BooleanSetting("DynamicColor", false);
	public Setting<Color> backgroundColor = new ColorSetting("Background", new Color(248, 248, 255, 150)).setVisibility(()-> !dynamicColor.getValue());
	public Setting<Boolean> compact = new BooleanSetting("Compact", false);
	public Setting<Float> scale = new NumberSetting<>("Scale", 1.0f, 0.5f, 1.5f);


	public ShulkerViewer() {
		super("ShulkerViewer", "Show shulkers on sides", ModuleCategory.MISC);

		//register settings
		this.registerSettings(
				dynamicColor,
				backgroundColor,
				compact,
				scale
		);
	}


	private final Vector2d mousePos = new Vector2d();
	private final Set<Shulker> shulkers = new CopyOnWriteArraySet<>();
	private int height, offset;

	@Subscribe
	public void onRender2D(EventRender2D event) {
		final GuiGraphics context = event.getRenderContext().graphics();

		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();

		int y = (int) ((3 + offset));
		int totalHeight = 0;

		context.pose().pushPose();
		context.pose().scale(scale.getValue(), scale.getValue(), scale.getValue());

		for (Shulker shulkerInfo : shulkers) {
			int count = 0, x = 2, startY = y, maxX = 22;
			List<ItemStack> stacks = shulkerInfo.getStacks();

			for (ItemStack stack : stacks) {
				if (shulkerInfo.isCompact() && stack.isEmpty()) {
					break;
				}

				if (count > 0 && count % 9 == 0) {
					x = 2;
					y += (int) (18);
				}

				if ((x + 22) > 0 && x < screenWidth && (y + 22) > 0 && y < screenHeight) {
					context.renderItem(stack, x + 2, y);

					if (stack.getCount() > 999) {
						String text = String.format("%.1fk", stack.getCount() / 1000f);
						context.renderItemDecorations(mc.font, stack, x + 2, y, text);
					} else {
						context.renderItemDecorations(mc.font, stack, x + 2, y);
					}
				}

				x += 20;
				count++;
				maxX = Math.max(maxX, x);
			}

			if (count == 0 && shulkerInfo.isCompact()) {
				if ((x + 22) > 0 && x < screenWidth && (y + 22) > 0 && y < screenHeight) {
					context.renderItem(shulkerInfo.getShulker(), x + 2, y + 1);
				}
			}

			y += (int) (18);

			if (isClickedWithinBounds(mousePos, maxX * scale.getValue(), startY * scale.getValue(), y * scale.getValue())) {
				InventoryUtils.clickSlot(shulkerInfo.getSlot(), false);
				mousePos.set(0);
			}

			if ((startY + 22)  > 0 && startY < screenHeight) {
				int color = dynamicColor.getValue() ? getShulkerColor(shulkerInfo.getShulker()).getRGB() : backgroundColor.getValue().getRGB();
				context.fill(2, startY, maxX, y, color);
			}

			y += 3;
		}

		totalHeight = y - (3 + offset);
		if (totalHeight <= screenHeight) {
			offset = 0;
		}

		mousePos.set(0);
		height = y - offset;
		context.pose().popPose();
	}

	@Subscribe
	public void mouseScroll(EventMouse.Scroll event) {
		if (!(mc.screen instanceof AbstractContainerScreen<?>)) return;

		if (event.getScrollDeltaY() != 0 && height > mc.getWindow().getGuiScaledHeight()) {
			this.offset = MathUtils.clamp((int) (offset + Math.ceil(event.getScrollDeltaY()) * 10),
					-height + mc.getWindow().getGuiScaledHeight(), 0);
		}
	}

	private boolean isClickedWithinBounds(Vector2d mousePos, float maxX, float minY, float maxY) {
		return mousePos.x >= 2 && mousePos.x <= maxX && mousePos.y >= minY && mousePos.y <= maxY;
	}



	@Subscribe(stage = Stage.PRE)
	public void mouseClick(EventMouse.Key event){
		if(event.getButton() == 0 && event.getAction() == 1 && event.getMouseX() < 200) {
			mousePos.set(event.getMouseX(), event.getMouseY());
			System.out.println("CLICKED AT X:" + mousePos.x + ", Y:" + mousePos.y);
		}
	}



	@Subscribe
	public void onTick(EventUpdate event) {
		shulkers.clear();
		if(!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;

		for(Slot slot : screen.getMenu().slots){
			Shulker shulker = Shulker.create(slot.getItem(), slot.index);
			if(shulker != null) {
				shulkers.add(shulker);
			}
		}

	}

	public static class Shulker {
		private final ItemStack shulker;
		private final boolean compact;
		private final int slot;
		private final NonNullList<ItemStack> stacks;

		public Shulker(ItemStack shulker, boolean compact, int slot, NonNullList<ItemStack> stacks) {
			this.shulker = shulker;
			this.compact = compact;
			this.slot = slot;
			this.stacks = stacks;
		}

		public ItemStack getShulker() {
			return shulker;
		}

		public boolean isCompact() {
			return compact;
		}

		public int getSlot() {
			return slot;
		}

		public NonNullList<ItemStack> getStacks() {
			return stacks;
		}

		public static Shulker create(ItemStack stack, int slot) {
			if (!isValidShulkerBox(stack)) {
				return null;
			}

			NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
			CompoundTag nbt = getCompoundTag(stack);
			boolean compact = ShulkerViewerPlugin.shulkerViewer.compact.getValue();


			if (nbt.contains("Items", 9)) {
				ContainerHelper.loadAllItems(nbt, items);
				compact = processItems(items, compact);
			}

			if (compact) {
				items = compactItems(items);
			}

			return new Shulker(stack, compact, slot, items);
		}

		private static boolean isValidShulkerBox(ItemStack stack) {
			return stack.getItem() instanceof BlockItem &&
					((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock;
		}

		private static CompoundTag getCompoundTag(ItemStack stack) {
			return stack.getOrCreateTag().getCompound("BlockEntityTag");
		}

		private static boolean processItems(NonNullList<ItemStack> items, boolean compact) {
			Item unstackable = null;

			for (ItemStack item : items) {
				if (!item.isEmpty() && item.getMaxStackSize() == 1) {
					if (unstackable != null && !item.getItem().equals(unstackable)) {
						compact = false;
					}
					unstackable = item.getItem();
				}
			}

			return compact;
		}

		private static NonNullList<ItemStack> compactItems(NonNullList<ItemStack> items) {
			Map<Item, Integer> itemCountMap = new HashMap<>();

			for (ItemStack item : items) {
				if (!item.isEmpty()) {
					itemCountMap.put(item.getItem(), item.getCount() + itemCountMap.getOrDefault(item.getItem(), 0));
				}
			}

			NonNullList<ItemStack> compactedItems = NonNullList.withSize(27, ItemStack.EMPTY);
			int index = 0;

			for (Map.Entry<Item, Integer> entry : itemCountMap.entrySet()) {
				compactedItems.set(index++, new ItemStack(entry.getKey(), entry.getValue()));
			}

			return compactedItems;
		}
	}

	public static Color getShulkerColor(ItemStack shulkerItem) {
		if (shulkerItem.getItem() instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			if (block == Blocks.ENDER_CHEST) return new Color(0, 50, 50, 50);
			if (block instanceof ShulkerBoxBlock shulkerBlock) {
				DyeColor dye = shulkerBlock.getColor();
				if (dye == null) return new Color(WHITE.getRed(), WHITE.getGreen(), WHITE.getBlue(), 50);
				float[] colorComponents = dye.getTextureDiffuseColors();
				int r = (int) (colorComponents[0] * 255);
				int g = (int) (colorComponents[1] * 255);
				int b = (int) (colorComponents[2] * 255);

				return new Color(r, g, b, 50	);
			}
		}
		return new Color(WHITE.getRed(), WHITE.getGreen(), WHITE.getBlue(), 50);
	}


}
