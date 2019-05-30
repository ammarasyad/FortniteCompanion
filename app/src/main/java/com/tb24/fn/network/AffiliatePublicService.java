package com.tb24.fn.network;

import com.tb24.fn.model.Affiliate;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface AffiliatePublicService {
	@GET("/affiliate/api/public/affiliates/slug/{slug}")
	Call<Affiliate> affiliate(@Path("slug") String slug);
}
