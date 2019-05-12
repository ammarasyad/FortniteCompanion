package com.tb24.fn.model;

import java.util.Date;

public class LoginResponse extends BaseOauthResponse {
	public String access_token;
	public String app;
	public String refresh_expires;
	public Date refresh_expires_at;
	public String refresh_token;
}
