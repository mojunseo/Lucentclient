package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.ChoiceSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Worn armor and held items with their durability. */
public class ArmorModule extends HudModule {
	private static final int ITEM = 16;
	private static final int ROW = 18;
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private final ChoiceSetting layout = setting(new ChoiceSetting("layout", "vertical", List.of("vertical", "horizontal")));
	private final ChoiceSetting durability = setting(new ChoiceSetting("durability", "percent", List.of("percent", "remaining", "none")));
	private final BooleanSetting showHeld = setting(new BooleanSetting("show_held", true));

	public ArmorModule() {
		super("armor", true, 1.0F, 0.6F);
	}

	private List<ItemStack> items(Minecraft minecraft) {
		List<ItemStack> items = new ArrayList<>();
		LocalPlayer player = minecraft.player;
		if (player != null) {
			for (EquipmentSlot slot : ARMOR) {
				ItemStack stack = player.getItemBySlot(slot);
				if (!stack.isEmpty()) items.add(stack);
			}
			if (showHeld.enabled() && !player.getMainHandItem().isEmpty()) items.add(player.getMainHandItem());
		}
		if (items.isEmpty() && previewing(minecraft)) {
			items.add(new ItemStack(Items.DIAMOND_HELMET));
			items.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
			items.add(new ItemStack(Items.DIAMOND_LEGGINGS));
			items.add(new ItemStack(Items.DIAMOND_BOOTS));
		}
		return items;
	}

	private String label(ItemStack stack) {
		if (!stack.isDamageableItem()) return stack.getCount() > 1 ? String.valueOf(stack.getCount()) : "";
		int max = stack.getMaxDamage();
		int left = max - stack.getDamageValue();
		return switch (durability.get()) {
			case "remaining" -> String.valueOf(left);
			case "none" -> "";
			default -> Math.round(left * 100.0F / max) + "%";
		};
	}

	private int labelWidth(Minecraft minecraft, List<ItemStack> items) {
		int width = 0;
		for (ItemStack stack : items) width = Math.max(width, minecraft.font.width(label(stack)));
		return width;
	}

	private boolean vertical() {
		return layout.get().equals("vertical");
	}

	@Override
	public int width(Minecraft minecraft) {
		List<ItemStack> items = items(minecraft);
		int labels = labelWidth(minecraft, items);
		if (vertical()) return ITEM + 4 + (labels > 0 ? labels + 4 : 0);
		return Math.max(1, items.size()) * (Math.max(ITEM, labels) + 4);
	}

	@Override
	public int height(Minecraft minecraft) {
		if (vertical()) return Math.max(1, items(minecraft).size()) * ROW + 2;
		return ITEM + (durability.get().equals("none") ? 4 : 14);
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		List<ItemStack> items = items(minecraft);
		fillBackground(graphics, 0, 0, width(minecraft), height(minecraft));
		int cell = Math.max(ITEM, labelWidth(minecraft, items)) + 4;
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			String label = label(stack);
			int color = stack.isDamageableItem() ? durabilityColor(stack) : textColor.argb();
			if (vertical()) {
				int y = 2 + i * ROW;
				graphics.item(stack, 2, y);
				graphics.text(minecraft.font, label, ITEM + 6, y + 4, color, shadow.enabled());
			} else {
				int x = i * cell + (cell - ITEM) / 2;
				graphics.item(stack, x, 2);
				graphics.text(minecraft.font, label, i * cell + (cell - minecraft.font.width(label)) / 2, ITEM + 4, color, shadow.enabled());
			}
		}
	}

	/** Green when new, through yellow, to red when about to break. */
	private int durabilityColor(ItemStack stack) {
		float left = 1.0F - stack.getDamageValue() / (float) stack.getMaxDamage();
		return 0xFF000000 | Mth.hsvToRgb(left / 3.0F, 0.8F, 1.0F);
	}
}
