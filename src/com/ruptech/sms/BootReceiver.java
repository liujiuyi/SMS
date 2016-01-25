package com.ruptech.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		Intent mMainActivity=new Intent(ctx, MainActivity.class);
		ctx.startActivity(mMainActivity);  
	}
}