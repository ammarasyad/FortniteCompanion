package com.tb24.fn.model;

import java.util.Date;

public class ExternalAuth {
	public String accountId;
	public String type;
	public AuthId[] authIds;
	public String externalAuthId;
	public String externalAuthSecondaryId;
	public Date dateAdded;
	public String externalDisplayName;
	public String externalAuthIdType;
	public Date lastLogin;

	public static class AuthId {
		public String id;
		public String type;
	}
}
