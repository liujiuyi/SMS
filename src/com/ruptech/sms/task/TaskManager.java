package com.ruptech.sms.task;

import java.util.Observable;
import java.util.Observer;

import android.util.Log;

public class TaskManager extends Observable {
	public static final Integer CANCEL_ALL = 1;

	public static final String TAG = "TaskManager";

	public void addTask(Observer task) {
		super.addObserver(task);
	}

	public void cancelAll() {
		Log.d(TAG, "All task Cancelled.");
		setChanged();
		notifyObservers(CANCEL_ALL);
	}
}