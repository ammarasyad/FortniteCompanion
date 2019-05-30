package com.tb24.fn.model;

import com.tb24.fn.model.command.SetMtxPlatform;

import java.util.Date;

public class CommonCoreProfileAttributes extends ProfileAttributes {
	public Date mtx_affiliate_set_time;
	public Integer inventory_limit_bonus;
	public SetMtxPlatform.EMtxPlatform current_mtx_platform;
	public String mtx_affiliate;
	public Boolean allowed_to_send_gifts;
	public Boolean mfa_enabled;
	public Boolean allowed_to_receive_gifts;
	public String quickreturn_cooldown;
}
