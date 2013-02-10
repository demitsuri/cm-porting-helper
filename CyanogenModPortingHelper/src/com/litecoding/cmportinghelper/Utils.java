package com.litecoding.cmportinghelper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Looper;

public class Utils {
	private static final Executor EXECUTOR = Executors.newCachedThreadPool();
	private static final Handler HANDLER = new Handler(Looper.getMainLooper());
	
	public static void runInBackground(Runnable runnable) {
		EXECUTOR.execute(runnable);
	}
	
	public static void runOnUIThread(Runnable runnable) {
		HANDLER.post(runnable);
	}
	
	public static void copyFile(String fromPath, String toPath) {
		
	}
}
