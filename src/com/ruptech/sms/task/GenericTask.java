package com.ruptech.sms.task;

import java.util.Observable;
import java.util.Observer;

import android.os.AsyncTask;

public abstract class GenericTask extends AsyncTask<TaskParams, Object, TaskResult> implements Observer {
	public static final String TAG = "TaskManager";

	private boolean isCancelable = true;
	private TaskListener mListener = null;

	abstract protected TaskResult _doInBackground(TaskParams... params);

	@Override
	protected TaskResult doInBackground(TaskParams... params) {
		TaskResult result = _doInBackground(params);
		return result;
	}

	public void doPublishProgress(Object... values) {
		super.publishProgress(values);
	}

	public TaskListener getListener() {
		return mListener;
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();

		if (mListener != null) {
			mListener.onCancelled(this);
		}
	}

	@Override
	protected void onPostExecute(TaskResult result) {
		super.onPostExecute(result);

		if (mListener != null) {
			mListener.onPostExecute(this, result);
		}

	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		if (mListener != null) {
			mListener.onPreExecute(this);
		}

	}

	@Override
	protected void onProgressUpdate(Object... values) {
		super.onProgressUpdate(values);

		if (mListener != null) {
			if (values != null && values.length > 0) {
				mListener.onProgressUpdate(this, values[0]);
			}
		}

	}

	public void setCancelable(boolean flag) {
		isCancelable = flag;
	}

	public void setListener(TaskListener taskListener) {
		mListener = taskListener;
	}

	@Override
	public void update(Observable o, Object arg) {
		if (TaskManager.CANCEL_ALL == (Integer) arg && isCancelable) {
			if (getStatus() == GenericTask.Status.RUNNING) {
				cancel(true);
			}
		}
	}
}
