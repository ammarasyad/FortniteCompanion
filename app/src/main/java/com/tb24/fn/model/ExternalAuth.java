package com.tb24.fn.model;

public class ExternalAuth {
	public String accountId;
	public String type;
	public AuthId[] authIds;
	public String externalAuthId;
	public String externalAuthSecondaryId;
	public String dateAdded;
	public String externalDisplayName;
	public String externalAuthIdType;
	public String lastLogin;

	public static class AuthId {
		public String id;
		public String type;
	}
}
