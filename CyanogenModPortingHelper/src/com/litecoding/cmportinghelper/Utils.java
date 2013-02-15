package com.litecoding.cmportinghelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.crittercism.app.Crittercism;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class Utils {
	private static final Executor EXECUTOR = Executors.newCachedThreadPool();
	private static final Handler HANDLER = new Handler(Looper.getMainLooper());
	
	public static void runInBackground(Runnable runnable) {
		EXECUTOR.execute(runnable);
	}
	
	public static void runOnUIThread(Runnable runnable) {
		HANDLER.post(runnable);
	}
	
	public static boolean copyFile(String fromPath, String toPath) {
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fromPath));
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(toPath));
			
			byte[] buff = new byte[1024];
			while(true) {
				int readBytes = bis.read(buff);
				if(readBytes == -1)
	        		break;
				bos.write(buff, 0, readBytes);
			}
			bos.flush();
			
			bis.close();
			bos.close();
		} catch(Exception e) {
			Crittercism.logHandledException(e);
			Log.e(MainActivity.TAG, "Exception while copying file", e);
			return false;
		}
		return true;
	}
	
	/**
	 * Analog of java.util.Arrays.copyOf() due to insufficient minimum API level (7 against 9) 
	 * @param source
	 * @param newLen
	 * @return
	 */
	public static <T> T[] arrayCopyOf(T[] source, int newLen) {
		@SuppressWarnings("unchecked")
		T[] newArr = (T[]) Array.newInstance(source.getClass(), newLen);
		
		for(int i = 0; i < Math.min(source.length, newLen); i++)
			newArr[i] = source[i];
		
		return newArr;
	}
}
