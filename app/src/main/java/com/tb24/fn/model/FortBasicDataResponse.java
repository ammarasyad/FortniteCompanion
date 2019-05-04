package com.tb24.fn.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

public class FortBasicDataResponse extends FortBasicDataResponseBase {
	public JsonObject survivalmessage;
	public JsonObject athenamessage;
	public JsonObject subgameselectdata;
	public NewsRoot savetheworldnews;
	public NewsRoot battleroyalenews;
	public JsonObject loginmessage;
	public PlaylistImages playlistimages;
	public NewsRoot battlepassaboutmessages;
	public PlaylistInformation playlistinformation;
	public TournamentInformation tournamentinformation;
	public NewsRoot emergencynotice;
	public JsonObject koreancafe;
	public CreativeAds creativeAds;
	public JsonObject playerSurvey;
	public CreativeAds creativeFeatures;
	public List<JsonElement> _suggestedPrefetch;

	public static class CommonUISimpleMessageBase {
		public String layout;
		public String image;
		public boolean hidden;
		public String messagetype;
		public String _type;
		public String adspace;
		public String title;
		public String body;
		public boolean spotlight;
	}

	public static class CommonUISimpleMessagePlatform {
		public boolean hidden;
		public String _type;
		public CommonUISimpleMessageBase message;
		public String platform;
	}

	public static class NewsRoot extends FortBasicDataResponseBase {
		public News news;
		public String header;
		public String style;
	}

	public static class News {
		public List<CommonUISimpleMessagePlatform> platform_messages;
		public String _type;
		public List<CommonUISimpleMessageBase> messages;
	}

	public static class PlaylistInformation extends FortBasicDataResponseBase {
		public String frontend_matchmaking_header_style;
		public String frontend_matchmaking_header_text;
		public PlaylistInformationList playlist_info;
	}

	public static class PlaylistInformationList {
		public List<FortPlaylistInfo> playlists;
		public String _type;
	}

	public static class FortPlaylistInfo {
		public String image;
		public String playlist_name;
		public String _type;
	}

	public static class TournamentInformation extends FortBasicDataResponseBase {
		public TournamentsInfo tournament_info;
	}

	public static class TournamentsInfo {
		public List<TournamentDisplayInfo> tournaments;
		public String _type;
	}

	public static class TournamentDisplayInfo {
		public String title_color;
		public String loading_screen_image;
		public String background_text_color;
		public String background_right_color;
		public String poster_back_image;
		public String _type;
		public String pin_earned_text;
		public String tournament_display_id;
		public String highlight_color;
		public String schedule_info;
		public String primary_color;
		public String flavor_description;
		public String poster_front_image;
		public String short_format_title;
		public String title_line_2;
		public String title_line_1;
		public String shadow_color;
		public String details_description;
		public String background_left_color;
		public String long_format_title;
		public String poster_fade_color;
		public String secondary_color;
		public String playlist_tile_image;
		public String base_color;
	}

	public static class PlaylistImages extends FortBasicDataResponseBase {
		public PlaylistImageList playlistimages;
	}

	public static class PlaylistImageList {
		public List<PlaylistImageEntry> images;
		public String _type;
	}

	public static class PlaylistImageEntry {
		public String image;
		public String _type;
		public String playlistname;
	}

	public static class CreativeAds extends FortBasicDataResponseBase {
		public CreativeAdInfo ad_info;
	}

	public static class CreativeAdInfo {
		public List<CreativeAdDisplayInfo> ads;
		public String _type;
	}

	public static class CreativeAdDisplayInfo {
		public String sub_header;
		public String image;
		public boolean hidden;
		public String _type;
		public String description;
		public String header;
		public String creator_name;
	}
}
