package com.litecoding.cmportinghelper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.crittercism.app.Crittercism;
import com.flurry.android.FlurryAgent;
import com.litecoding.cmportinghelper.R;

import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String KEY_ROOT = "com.litecoding.cmporthelper.MainActivity.";
	private static final String BTN_ACTION_TEXT_ID = KEY_ROOT + "btnAction.textId";
	private static final String BTN_ACTION_SCAN_MODE = KEY_ROOT + "btnAction.scanMode";
	private static final String BTN_ACTION_SHARE_MODE = KEY_ROOT + "btnAction.shareMode";
	private static final String TXT_COLLECTED_DATA = KEY_ROOT + "txtCollectedData.text";
	
	private String mAppDir = null;
	
	protected InfoCollectionTask mCollectionTask = new InfoCollectionTask();
	protected boolean mIsActionScanMode = true;
	protected boolean mIsActionShareMode = false;
	
	protected String mCollectedData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Crittercism.init(getApplicationContext(), Consts.CRITTERCISM_ID);
        FlurryAgent.onStartSession(this, Consts.FLURRY_ID);
        
        try {
        	mAppDir = getPackageManager().
        			getPackageInfo(getPackageName(), 0).applicationInfo.dataDir;
        } catch(Exception e) {
        	Crittercism.logHandledException(e);
        }
        
        Bundle testBundle = savedInstanceState;
        if(testBundle == null)
        	testBundle = new Bundle();
        
        mIsActionScanMode = testBundle.getBoolean(BTN_ACTION_SCAN_MODE, true);	
        mIsActionShareMode = testBundle.getBoolean(BTN_ACTION_SHARE_MODE, false);
        
        final Button btnAction = (Button) findViewById(R.id.btnAction);
        btnAction.setText(testBundle.getInt(BTN_ACTION_TEXT_ID, 
        		mIsActionScanMode ? R.string.btn_caption_collect : R.string.btn_caption_share));
        btnAction.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(mIsActionScanMode) {
					mIsActionScanMode = false;
					mCollectionTask.execute(getResources().
							getStringArray(R.array.arr_examine_filepaths));
				}
				
				//disable button
				if(!mIsActionScanMode && !mIsActionShareMode) {
					btnAction.setEnabled(false);
					btnAction.setText(R.string.btn_caption_share);
				}
				
				if(mIsActionShareMode) {
					showShareDialog();
				}
			}
		});
        
        TextView txtCollectedData = (TextView) findViewById(R.id.txtCollectedData);
        txtCollectedData.setMovementMethod(new ScrollingMovementMethod());
        String tmpVal = testBundle.getString(TXT_COLLECTED_DATA);
        if(tmpVal == null)
        	tmpVal = getResources().getString(R.string.txt_empty_collected_data);
        txtCollectedData.setText(tmpVal);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	FlurryAgent.onEndSession(this);
    	
    	if(mCollectionTask != null) {
    		if(mCollectionTask.getStatus() == Status.RUNNING) {
    			mCollectionTask.cancel(false);
    		}
    	}
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outData) {
    	outData.putBoolean(BTN_ACTION_SCAN_MODE, mIsActionScanMode);
    	outData.putBoolean(BTN_ACTION_SHARE_MODE, mIsActionScanMode);
    	outData.putInt(BTN_ACTION_TEXT_ID, 
    			mIsActionScanMode ? R.string.btn_caption_collect : R.string.btn_caption_share);
    	outData.putString(TXT_COLLECTED_DATA, mCollectedData);
    }
    
    private void showCollectedData(String data) {
    	mIsActionScanMode = false;
    	mIsActionShareMode = true;
    	
    	Button btnAction = (Button) findViewById(R.id.btnAction);
        btnAction.setEnabled(true);
    	
    	mCollectedData = data;
    	
    	TextView txtCollectedData = (TextView) findViewById(R.id.txtCollectedData);
    	txtCollectedData.setText(data);
    }
    
    private void showShareDialog() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle(R.string.dialog_share_caption);
		builder.setItems(R.array.dialog_share_items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
				case 0: {
					new ZipTask(ZipTask.MODE_SD).execute(mCollectionTask.getInfoEntries());
					break;
				}
				case 1: {
					new ZipTask(ZipTask.MODE_EMAIL).execute(mCollectionTask.getInfoEntries());
					break;
				}
				default: {
					break;
				}
				}
			}
		});
		
		builder.create().show();
    }
    
    private void saveToSD(String srcPath) {
    	//save on SD
		File dir = null;
		
		dir = Environment.getExternalStorageDirectory();
		if(dir != null && dir.exists() && dir.isDirectory() && dir.canWrite()) {
			dir = new File(dir, "CMPortingHelper");
			if(dir.exists() || dir.mkdir()) {
				//copy file
				Utils.copyFile(srcPath, 
					new File(dir, "files_" + System.currentTimeMillis() + ".zip").getAbsolutePath());
			}
		}
    }
    
    private void sendByEmail(String srcPath) {
    	//send by email
    	PackageInfo pInfo = null;
    	try {
    		pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
    	} catch(Exception e) {
    		pInfo = new PackageInfo();
    		pInfo.versionCode = 0;
    		pInfo.versionName = "(unknown)";
    		Crittercism.logHandledException(e);
    	}
    	
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
	    emailIntent.setType("text/plain");
	    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.email_subject));
	    
	    StringBuilder builder = new StringBuilder();
	    for(InfoEntry entry : mCollectionTask.getInfoEntries()) {
	    	if(entry.mException == null)
	    		continue;
	    	builder.append("File: ");
	    	builder.append(entry.mPath);
	    	builder.append("\n");
	    	builder.append("Exception: ");
	    	builder.append(entry.mException);
	    	builder.append("\n");
	    	builder.append("\n");
	    }
	    
	    String emailBodyFmt = getResources().getString(R.string.email_body_fmt);
	    String emailBody = String.format(emailBodyFmt, 
	    		getPackageName(), 
	    		pInfo.versionName, 
	    		pInfo.versionCode,
	    		builder.toString());
	    emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
	    
        Uri u = Uri.fromFile(new File(srcPath));
        
	    emailIntent.putExtra(Intent.EXTRA_STREAM, u);
	    
	    startActivity(Intent.createChooser(emailIntent, 
	    getResources().getString(R.string.chooser_share_email)));
		
    }
    
    public class InfoCollectionTask extends AsyncTask<String, Integer, List<InfoEntry>> {
    	private List<String> mPendingPaths = null;
    	private List<String> mProcessedPaths = null;
    	private List<InfoEntry> mInfoEntries = null;

    	@Override
    	protected List<InfoEntry> doInBackground(String... filepaths) {
    		mPendingPaths = new ArrayList<String>(filepaths.length);
    		mProcessedPaths = new ArrayList<String>(filepaths.length);
    		mInfoEntries = new ArrayList<InfoEntry>(filepaths.length);
    		
    		mPendingPaths.addAll(Arrays.asList(filepaths));
    		
    		int progress = 0;
    		int total = filepaths.length;
    		
    		File file = null;
    		
    		while(mPendingPaths.size() > 0 && !isCancelled()) {
    			String currPath = mPendingPaths.remove(0);
    			InfoEntry currInfo = new InfoEntry();
    			currInfo.mPath = currPath;
    			
    			StringBuilder builder = new StringBuilder(65536);
    			try {
    				file = new File(currPath);
    				BufferedReader br = new BufferedReader(new FileReader(file));
    				currInfo.mInfo = "";
        			String line;
        			while ((line = br.readLine()) != null) {
        			   // process the line.
        				builder.append(line);
        				builder.append("\n");
        				
        				currInfo.mInfo = currInfo.mInfo.concat(line).concat("\n");
        			}
        			br.close();
    			} catch(Exception e) {
    				currInfo.mException = e;
    				Log.e("InfoCollectionTask", "Oops", e);
    			}
    			Log.d("InfoCollectionTask", builder.toString());
    			mProcessedPaths.add(currPath);
    			mInfoEntries.add(currInfo);
    			publishProgress((int)(((float)progress / (float)total) * 100));
    		}
    		
    		
    		return mInfoEntries;
    	}
    	
    	@Override
    	protected void onPostExecute(List<InfoEntry> result) {
    		StringBuilder builder = new StringBuilder(65536);
    		for(InfoEntry entry : result) {
    			builder.append("File: ");
    			builder.append(entry.mPath);
    			builder.append("\n");
    			
    			if(entry.mException != null) {
    				builder.append("Exception: ");
        			builder.append(entry.mException.toString());
        			builder.append("\n");
    			}
    			
    			if(entry.mInfo != null) {
        			builder.append(entry.mInfo);
        			builder.append("\n");
    			}
    			
    			builder.append("\n");
    		}
    		
    		MainActivity.this.showCollectedData(builder.toString());
    	}
    	
    	public List<String> getPendingPaths() {
    		return Collections.unmodifiableList(mPendingPaths);
    	}
    	
    	public List<String> getProcessedPaths() {
    		return Collections.unmodifiableList(mProcessedPaths);
    	}
    	
    	public List<InfoEntry> getInfoEntries() {
    		return Collections.unmodifiableList(mInfoEntries);
    	}

    }

    public static class InfoEntry {
    	public String mPath;
    	public Exception mException;
    	public String mInfo;
    }
    
    public class ZipTask extends AsyncTask<List<InfoEntry>, Integer, String> {
    	public static final int MODE_SD = 0;
    	public static final int MODE_EMAIL = 1;
    	
    	private int mMode = MODE_SD;
    	
    	public ZipTask(int mode) {
    		mMode = mode;
    	}
    	
		@Override
		protected String doInBackground(List<InfoEntry>... params) {
			File file = null;
			try {
				file = File.createTempFile("cmph-", ".zip");
				FileOutputStream fos = new FileOutputStream(file);
				ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
				byte[] buff = new byte[1024];
				try {
					for(InfoEntry entry : params[0]) {
						if(entry.mException != null)
							continue;
						String filename = entry.mPath;
				        
				        ZipEntry zentry = new ZipEntry(filename);
				        zos.putNextEntry(zentry);
				         
				        FileInputStream fis = new FileInputStream(filename);
				        int readBytes = 0;
				        while(fis.available() > 0) {
				        	readBytes = fis.read(buff);
				        	zos.write(buff, 0, readBytes);
				        }
				        zos.closeEntry();
				     }
				 } catch(Exception e) {
					 Crittercism.logHandledException(e);
				 } finally {
				     zos.close();
				 }
			} catch(Exception e) {
				Crittercism.logHandledException(e);
				file = null;
			}

			return file == null ? null : file.getAbsolutePath();
		}
    	
		@Override
    	protected void onPostExecute(String result) {
			switch (mMode) {
			case MODE_SD:
			default: {
				saveToSD(result);
				break;
			}

			case MODE_EMAIL:
				sendByEmail(result);
				break;
			}
		}
    }
}
