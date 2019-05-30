package com.tb24.fn.model.command;

public class EquipBattleRoyaleCustomization {
	public ECustomizationSlot slotName;
	public String itemToSlot;
	public Integer indexWithinSlot;
	public VariantUpdate[] variantUpdates;

	public enum ECustomizationSlot {
		Backpack, VictoryPose, LoadingScreen, Character, Glider, Dance, CallingCard, ConsumableEmote, MapMarker, SkyDiveContrail, Hat, PetSkin, ItemWrap, MusicPack, BattleBus, Pickaxe, VehicleDecoration
	}

	public static class VariantUpdate {
		public String channel;
		public String active;
		public String[] owned;
	}
}
