package com.tb24.fn.model;

import java.util.Date;

public class BaseOauthResponse {
	public String account_id;
	public String client_id;
	public String client_service;
	/**
	 * Nonexistent if X-Epic-Device-Id header isn't provided
	 */
	public String device_id;
	public Date expires_at;
	public Integer expires_in;
	public String in_app_id;
	public Boolean internal_client;
	public Date lastPasswordValidation;
	public Perm[] perms;
	public String token_type;
}
