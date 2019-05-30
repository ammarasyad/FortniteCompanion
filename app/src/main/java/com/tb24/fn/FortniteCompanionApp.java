package com.tb24.fn;

import android.app.Application;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.LruCache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.tb24.fn.event.CalendarDataLoadedEvent;
import com.tb24.fn.model.CalendarTimelineResponse;
import com.tb24.fn.model.EventDownloadResponse;
import com.tb24.fn.model.FortBasicDataResponse;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.RarityData;
import com.tb24.fn.model.XGameProfile;
import com.tb24.fn.model.assetdata.BannerColor;
import com.tb24.fn.model.assetdata.BannerIcon;
import com.tb24.fn.model.assetdata.FortHomebaseBannerColorMap;
import com.tb24.fn.network.AccountPublicService;
import com.tb24.fn.network.AffiliatePublicService;
import com.tb24.fn.network.CatalogPublicService;
import com.tb24.fn.network.DefaultInterceptor;
import com.tb24.fn.network.EventsPublicServiceLive;
import com.tb24.fn.network.FortniteContentWebsiteService;
import com.tb24.fn.network.FortnitePublicService;
import com.tb24.fn.network.PersonaPublicService;
import com.tb24.fn.util.ERegion;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.Utils;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FortniteCompanionApp extends Application {
	public static final String CLIENT_TOKEN_FORTNITE = "basic MzQ0NmNkNzI2OTRjNGE0NDg1ZDgxYjc3YWRiYjIxNDE6OTIwOWQ0YTVlMjVhNDU3ZmI5YjA3NDg5ZDMxM2I0MWE=";
	public static final String ACCOUNT_PUBLIC_SERVICE = "https://account-public-service-prod03.ol.epicgames.com";
	public static final String AFFILIATE_PUBLIC_SERVICE = "https://affiliate-public-service-prod.ol.epicgames.com";
	public static final String CATALOG_PUBLIC_SERVICE = "https://catalog-public-service-prod06.ol.epicgames.com";
	public static final String EVENTS_PUBLIC_SERVICE = "https://events-public-service-live.ol.epicgames.com";
	public static final String FORTNITECONTENT_WEBSITE = "https://fortnitecontent-website-prod07.ol.epicgames.com";
	public static final String FORTNITE_PUBLIC_SERVICE = "https://fortnite-public-service-prod11.ol.epicgames.com";
	public static final String LIGHTSWITCH_PUBLIC_SERVICE = "https://lightswitch-public-service-prod.ol.epicgames.com";
	public static final String PERSONA_PUBLIC_SERVICE = "https://persona-public-service-prod06.ol.epicgames.com";
	public static final String PRICEENGINE_PUBLIC_SERVICE_ECOMPROD = "https://priceengine-public-service-ecomprod01.ol.epicgames.com";
	public static final int[] MAX_XPS_S8 = new int[]{100, 200, 300, 400, 500, 650, 800, 950, 1100, 1250, 1400, 1550, 1700, 1850, 2000, 2150, 2300, 2450, 2600, 2750, 2900, 3050, 3200, 3350, 3500, 3650, 3800, 3950, 4100, 4250, 4400, 4550, 4700, 4850, 5000, 5150, 5300, 5450, 5600, 5800, 6000, 6200, 6400, 6600, 6800, 7000, 7200, 7400, 7600, 7800, 8100, 8400, 8700, 9000, 9300, 9600, 9900, 10200, 10500, 10800, 11200, 11600, 12000, 12400, 12800, 13200, 13600, 14000, 14400, 14800, 15300, 15800, 16300, 16800, 17300, 17800, 18300, 18800, 19300, 19800, 20800, 21800, 22800, 23800, 24800, 25800, 26800, 27800, 28800, 30800, 32800, 34800, 36800, 38800, 40800, 42800, 45800, 49800, 54800};
	public static final int MIN_DP_FOR_TWOCOLUMN = 600;
	public final Gson gson = new GsonBuilder().registerTypeAdapter(FortMcpProfile.class, new FortMcpProfile.Serializer()).create();
	public final EventBus eventBus = new EventBus();
	public final LruCache<String, Bitmap> bitmapCache = new LruCache<String, Bitmap>(16 * 1024 * 1024) {
		@Override
		protected int sizeOf(String key, Bitmap value) {
			return value.getByteCount();
		}
	};
	public static RarityData[] sRarityData;
	public OkHttpClient okHttpClient;
	public AccountPublicService accountPublicService;
	public AffiliatePublicService affiliatePublicService;
	public CatalogPublicService catalogPublicService;
	public EventsPublicServiceLive eventsPublicServiceLive;
	public FortniteContentWebsiteService fortniteContentWebsiteService;
	public FortnitePublicService fortnitePublicService;
	public PersonaPublicService personaPublicService;
	public FortBasicDataResponse basicData;
	public EventDownloadResponse eventData;
	public XGameProfile currentLoggedIn;
	public ERegion eventDataRegion;
	public Registry itemRegistry;
	public ProfileManager profileManager;
	public CalendarTimelineResponse calendarDataBase;
	public CalendarTimelineResponse.ClientEventState calendarData;
	public FortHomebaseBannerColorMap bannerColorMap;
	public Map<String, BannerColor> bannerColors;
	public Map<String, BannerIcon> bannerIcons;
	private Call calendarCall;

	@Override
	public void onCreate() {
		super.onCreate();
		OkHttpClient.Builder builder = new OkHttpClient.Builder()
				.cache(new Cache(getCacheDir(), 4 * 1024 * 1024))
				.addInterceptor(new DefaultInterceptor(this));
		Retrofit.Builder retrofitBuilder = new Retrofit.Builder().client(okHttpClient = builder.build()).addConverterFactory(GsonConverterFactory.create(gson));
		accountPublicService = retrofitBuilder.baseUrl(ACCOUNT_PUBLIC_SERVICE).build().create(AccountPublicService.class);
		affiliatePublicService = retrofitBuilder.baseUrl(AFFILIATE_PUBLIC_SERVICE).build().create(AffiliatePublicService.class);
		catalogPublicService = retrofitBuilder.baseUrl(CATALOG_PUBLIC_SERVICE).build().create(CatalogPublicService.class);
		eventsPublicServiceLive = retrofitBuilder.baseUrl(EVENTS_PUBLIC_SERVICE).build().create(EventsPublicServiceLive.class);
		fortniteContentWebsiteService = retrofitBuilder.baseUrl(FORTNITECONTENT_WEBSITE).build().create(FortniteContentWebsiteService.class);
		fortnitePublicService = retrofitBuilder.baseUrl(FORTNITE_PUBLIC_SERVICE).build().create(FortnitePublicService.class);
		personaPublicService = retrofitBuilder.baseUrl(PERSONA_PUBLIC_SERVICE).build().create(PersonaPublicService.class);
		itemRegistry = new Registry(this);
		FortItemStack.sRegistry = itemRegistry;
		ItemUtils.sSetData = gson.fromJson(Utils.getStringFromAssets(getAssets(), "Game/Athena/Items/Cosmetics/Metadata/CosmeticSets.json"), JsonArray.class).get(0).getAsJsonObject();
		ItemUtils.sUserFacingTagsData = gson.fromJson(Utils.getStringFromAssets(getAssets(), "Game/Athena/Items/Cosmetics/Metadata/CosmeticUserFacingTags.json"), JsonArray.class).get(0).getAsJsonObject();
		sRarityData = gson.fromJson(Utils.getStringFromAssets(getAssets(), "RarityData.json"), RarityData[].class);
//		dbgRarityData();
		loadBannerData();
		profileManager = new ProfileManager(this);
	}

	private void loadBannerData() {
		bannerColorMap = FortHomebaseBannerColorMap.parse(gson.fromJson(Utils.getStringFromAssets(getAssets(), "Game/Banners/BannerColorMap.json"), JsonElement.class), gson);
		bannerColors = new HashMap<>();

		for (Map.Entry<String, JsonElement> entry : gson.fromJson(Utils.getStringFromAssets(getAssets(), "Game/Banners/BannerColors.json"), JsonArray.class).get(0).getAsJsonObject().entrySet()) {
			if (entry.getValue().isJsonObject()) {
				bannerColors.put(entry.getKey().toLowerCase(Locale.US), gson.fromJson(entry.getValue(), BannerColor.class));
			}
		}

		bannerIcons = new HashMap<>();

		for (Map.Entry<String, JsonElement> entry : gson.fromJson(Utils.getStringFromAssets(getAssets(), "Game/Banners/BannerIcons.json"), JsonArray.class).get(0).getAsJsonObject().entrySet()) {
			if (entry.getValue().isJsonObject()) {
				bannerIcons.put(entry.getKey().toLowerCase(Locale.US), gson.fromJson(entry.getValue(), BannerIcon.class));
			}
		}
	}

//	private void dbgRarityData() {
//		for (int i = 0; i < sRarityData.length; i++) {
//			RarityData rarity = sRarityData[i];
//			System.out.printf("Rarity index %d: #%08X #%08X #%08X #%08X #%08X\n", i, rarity.Color1.toPackedARGB(), rarity.Color2.toPackedARGB(), rarity.Color3.toPackedARGB(), rarity.Color4.toPackedARGB(), rarity.Color5.toPackedARGB());
//		}
//	}

	public void clearLoginData() {
		PreferenceManager.getDefaultSharedPreferences(this)
				.edit()
				.putBoolean("is_logged_in", false)
				.remove("epic_account_token_type")
				.remove("epic_account_expires_at")
				.remove("epic_account_refresh_token")
				.remove("epic_account_access_token")
				.remove("epic_account_id")
				.apply();
		profileManager.clearProfileData();
		eventData = null;
	}

	public void loadCalendarData() {
		if (calendarData == null) {
//			calendarCall = fortnitePublicService.calendarTimeline();
			// Escape retrofit because it glitched the cache resulting item shop timer not updating right after UTC midnight
			calendarCall = okHttpClient.newCall(new Request.Builder().url(FortniteCompanionApp.FORTNITE_PUBLIC_SERVICE + "/fortnite/api/calendar/v1/timeline").build());
			new Thread() {
				@Override
				public void run() {
					try {
						final okhttp3.Response response = calendarCall.execute();

						if (response.isSuccessful()) {
							calendarDataBase = gson.fromJson(new JsonReader(response.body().charStream()), CalendarTimelineResponse.class);
							calendarData = gson.fromJson(calendarDataBase.channels.get("client-events").states[0].state, CalendarTimelineResponse.ClientEventState.class);
							response.body().close();
							eventBus.post(new CalendarDataLoadedEvent(calendarData));
						}
					} catch (IOException ignored) {
					}
				}
			}.start();
		}
	}
}
