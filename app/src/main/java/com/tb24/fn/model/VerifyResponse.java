package com.tb24.fn.model;

import java.util.Date;

public class VerifyResponse {
	public String token;
	public String session_id;
	public String token_type;
	public String client_id;
	public boolean internal_client;
	public String client_service;
	public String account_id;
	public int expires_in;
	public Date expires_at;
	public String auth_method;
	public Date lastPasswordValidation;
	public String app;
	public String in_app_id;
	// might be nonexistent
	public String device_id;
	public Perm[] perms;
}
