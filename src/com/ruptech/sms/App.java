package com.ruptech.sms;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.ruptech.sms.http.HttpConnection;
import com.ruptech.sms.utils.Utils;

public class App extends Application implements Thread.UncaughtExceptionHandler {
	public static HttpConnection httpConnection;
	public static Context mContext;
	public static final String TAG = Utils.CATEGORY + App.class.getSimpleName();

	@Override
	public void onCreate() {
		super.onCreate();
		Thread.setDefaultUncaughtExceptionHandler(this);

		mContext = this.getApplicationContext();
		httpConnection = new HttpConnection(mContext);
	}

	@Override
	public void uncaughtException(Thread arg0, Throwable arg1) {
		// 出现异常重启
		Intent i = getBaseContext().getPackageManager()
				.getLaunchIntentForPackage(getBaseContext().getPackageName());
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		System.exit(0);
	}
}