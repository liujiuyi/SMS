package com.ruptech.sms.task;

public interface TaskListener {
	String getName();

	void onCancelled(GenericTask task);

	void onPostExecute(GenericTask task, TaskResult result);

	void onPreExecute(GenericTask task);

	void onProgressUpdate(GenericTask task, Object param);
}
