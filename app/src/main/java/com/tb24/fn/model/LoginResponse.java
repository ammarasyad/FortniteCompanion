package com.tb24.fn.model;

import java.util.Date;

public class LoginResponse {
	public int expires_in;
	public String token_type;
	public String in_app_id;
	public Date lastPasswordValidation;
	public Date expires_at;
	public String refresh_token;
	public String client_service;
	public String refresh_expires;
	public String app;
	public String internal_client;
	public String client_id;
	public Date refresh_expires_at;
	public String access_token;
	public String account_id;
	public Perm[] perms;
}
