package com.ruptech.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.util.Log;

import com.ruptech.sms.task.GenericTask;
import com.ruptech.sms.task.TaskAdapter;
import com.ruptech.sms.task.TaskParams;
import com.ruptech.sms.task.TaskResult;
import com.ruptech.sms.task.impl.SendSmsTask;
import com.ruptech.sms.utils.Utils;

/**
 * 
 * @author Administrator
 * 
 */
public class SmsMesssageReceiver extends BroadcastReceiver {

	static final String TAG = Utils.CATEGORY
			+ SmsMesssageReceiver.class.getSimpleName();

	public void doSendSmsMessage(Context context, Intent intent) {

		Object[] pdus = (Object[]) intent.getExtras().get("pdus");
		if (pdus != null && pdus.length > 0) {
			SmsMessage[] messages = new SmsMessage[pdus.length];

			for (int i = 0; i < messages.length; i++) {
				byte[] pdu = (byte[]) pdus[i];
				messages[i] = SmsMessage.createFromPdu(pdu);
			}

			for (SmsMessage msg : messages) {
				String address = msg.getOriginatingAddress();
				String smsContent = msg.getMessageBody().trim();

				if (address.indexOf("15778000") > 0 || address.indexOf("1577-8000") > 0) {
					if (!Utils.isEmpty(smsContent)) {
						doSendSms(smsContent);
					}
				}
			}
		}
	}

	public static void doSendSms(final String smsContent) {
		TaskAdapter smsRequestTranslateListener = new TaskAdapter() {
			@Override
			public String getName() {
				return "SendSmsTask";
			}

			@Override
			public void onPostExecute(GenericTask task, TaskResult result) {

			}

			@Override
			public void onPreExecute(GenericTask task) {
			}

			@Override
			public void onProgressUpdate(GenericTask task, Object param) {
			}
		};
		SendSmsTask mSendSmsTask = new SendSmsTask();

		mSendSmsTask.setListener(smsRequestTranslateListener);

		TaskParams params = new TaskParams();
		params.put("message", smsContent);
		mSendSmsTask.execute(params);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			doSendSmsMessage(context, intent);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}
}