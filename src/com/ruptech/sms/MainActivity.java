package com.ruptech.sms;

import android.app.Activity;
import android.os.Bundle;

import com.ruptech.sms.utils.Utils;
public class MainActivity extends Activity {

	public static final String TAG = Utils.CATEGORY + MainActivity.class.getSimpleName();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.finish();
	}
}
