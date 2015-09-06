package com.litecoding.cmportinghelper;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	public static final String TAG = "cm-porting-helper";
	
	private static final String KEY_ROOT = "com.litecoding.cmporthelper.MainActivity.";
	private static final String BTN_ACTION_TEXT_ID = KEY_ROOT + "btnAction.textId";
	private static final String BTN_ACTION_SCAN_MODE = KEY_ROOT + "btnAction.scanMode";
	private static final String BTN_ACTION_SHARE_MODE = KEY_ROOT + "btnAction.shareMode";
	private static final String TXT_COLLECTED_DATA = KEY_ROOT + "txtCollectedData.text";
	
	private String mFilesDir = null;
	
	protected InfoCollectionTask mCollectionTask = new InfoCollectionTask();
	protected boolean mIsActionScanMode = true;
	protected boolean mIsActionShareMode = false;
	
	protected String mCollectedData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Fabric.Builder builder = new Fabric.Builder(this);
		builder.kits(new Crashlytics()).debuggable(!BuildConfig.DEBUG);
        Fabric.with(builder.build());

        setContentView(R.layout.activity_main);
        
        try {
        	mFilesDir = getApplicationContext().getFilesDir().getAbsolutePath();
        } catch(Exception e) {
        	Crashlytics.logException(e);
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
		return false;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
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
			@SuppressWarnings("unchecked")
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
				case 0: {
					new ZipTask(MainActivity.this.getApplicationContext(), ZipTask.MODE_SD, mFilesDir).
						execute(mCollectionTask.getInfoEntries());
					break;
				}
				case 1: {
					new ZipTask(MainActivity.this.getApplicationContext(), ZipTask.MODE_EMAIL, mFilesDir).
						execute(mCollectionTask.getInfoEntries());
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
		
		String state = Environment.getExternalStorageState();
		if(!Environment.MEDIA_MOUNTED.equals(state)) {
			Toast.makeText(this, R.string.msg_sd_is_unavailable, Toast.LENGTH_SHORT).show();
			return;
		}
		
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
    		Crashlytics.logException(e);
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
	    
	    File srcFile = new File(srcPath);
        Uri uri = new Uri.Builder().scheme("content").authority(ResultContentProvider.AUTHORITY).path(srcFile.getName()).build();
        
	    emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
	    
	    startActivity(Intent.createChooser(emailIntent, 
	    getResources().getString(R.string.chooser_share_caption)));
		
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
    			
    			try {
    				file = new File(currPath);
    				BufferedReader br = new BufferedReader(new FileReader(file));
    				currInfo.mInfo = "";
        			String line;
        			while ((line = br.readLine()) != null) {
        				currInfo.mInfo = currInfo.mInfo.concat(line).concat("\n");
        			}
        			br.close();
    			} catch(Exception e) {
    				currInfo.mException = e;
    				Log.e(MainActivity.TAG, "Oops", e);
    			}

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
    	
    	private final FileFilter FILE_FILTER = new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				//accept files only
				if(!pathname.isFile())
					return false;
				
				//accept mask is tmp-*.zip
				if(!pathname.getName().startsWith("tmp-") ||
					!pathname.getName().endsWith(".zip"))
					return false;
				
				//accept files older than 10min
				if(pathname.lastModified() > System.currentTimeMillis() - 10* 60 * 1000)
					return false;
				
				return true;
			}
		};
    	
		private Context mContext = null;
    	private int mMode = MODE_SD;
    	private String mFilesDir = null;
    	
    	public ZipTask(Context ctx, int mode, String filesDir) {
    		mContext = ctx;
    		mMode = mode;
    		mFilesDir = filesDir;
    	}
    	
		@Override
		protected String doInBackground(List<InfoEntry>... params) {
			File file = null;
			try {
				//begin clear old files
				File filesDir = new File(mFilesDir);
				File[] oldFiles = filesDir.listFiles(FILE_FILTER);
				
				for(File oldFile : oldFiles)
					oldFile.delete();
				//end clear old files
				
				file = new File(filesDir, "tmp-" + 
						System.currentTimeMillis() + 
						"-" + System.nanoTime() + ".zip");
				
				FileOutputStream fos = mContext.openFileOutput(file.getName(), 
						Context.MODE_PRIVATE);
				ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
				byte[] buff = new byte[1024];
				try {
					for(InfoEntry entry : params[0]) {
						if(entry.mException != null)
							continue;
						String filename = entry.mPath.startsWith(File.separator) ? 
								entry.mPath.substring(1) : entry.mPath;
				        
				        ZipEntry zentry = new ZipEntry(filename);
				        zos.putNextEntry(zentry);
				         
				        BufferedInputStream bis = 
				        		new BufferedInputStream(new FileInputStream(entry.mPath));
				        int readBytes = 0;
				        while(true) {
				        	readBytes = bis.read(buff);
				        	if(readBytes == -1)
				        		break;
				        	zos.write(buff, 0, readBytes);
				        }
				        bis.close();
				        zos.closeEntry();
				     }
				 } catch(Exception e) {
					 Crashlytics.logException(e);
				 } finally {
				     zos.close();
				 }
			} catch(Exception e) {
				Crashlytics.logException(e);
				Log.e(MainActivity.TAG, "Oops on zip", e);
				file = null;
			}

			return file == null ? null : file.getAbsolutePath();
		}
    	
		@Override
    	protected void onPostExecute(String result) {
			switch (mMode) {
			case MODE_SD:
			default: {
				MainActivity.this.
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
