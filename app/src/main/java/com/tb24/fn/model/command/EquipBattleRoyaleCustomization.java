package com.tb24.fn.model.command;

import com.tb24.fn.model.ECustomizationSlot;

/**
 * com.epicgames.fortnite.core.game.commands.athena.EquipBattleRoyaleCustomization
 * <p>
 * Profile ID: athena
 */
public class EquipBattleRoyaleCustomization {
	public ECustomizationSlot slotName;
	public String itemToSlot;
	public Integer indexWithinSlot;
	public VariantUpdate[] variantUpdates;

	public static class VariantUpdate {
		public String channel;
		public String active;
		public String[] owned;
	}
}
