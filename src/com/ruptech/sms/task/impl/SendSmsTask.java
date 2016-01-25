package com.ruptech.sms.task.impl;

import android.util.Log;

import com.ruptech.sms.App;
import com.ruptech.sms.task.GenericTask;
import com.ruptech.sms.task.TaskParams;
import com.ruptech.sms.task.TaskResult;

public class SendSmsTask extends GenericTask {
	private String message;

	@Override
	protected TaskResult _doInBackground(TaskParams... params) {
		TaskParams param = params[0];

		message = param.getString("message");
		try {
			App.httpConnection.sendSmsMessage(message);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			return TaskResult.FAILED;
		}

		return TaskResult.OK;
	}
}