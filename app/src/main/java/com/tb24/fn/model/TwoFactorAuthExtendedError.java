package com.tb24.fn.model;

public class TwoFactorAuthExtendedError extends EpicError {
	public String challenge;
	public TwoFactorAuthMeta metadata;

	public static class TwoFactorAuthMeta {
		public String twoFactorMethod;
		public String[] alternateMethods;
	}
}
