package com.tb24.fn.model.command;

// com.epicgames.fortnite.core.game.commands.mtx.SetMtxPlatform
// profileId == common_core
public class SetMtxPlatform {
	public EMtxPlatform newPlatform;

	public enum EMtxPlatform {
		WeGame, EpicPCKorea, Epic, EpicPC, EpicAndroid, PSN, Live, IOSAppStore, Nintendo, Samsung, Shared;
	}
}
