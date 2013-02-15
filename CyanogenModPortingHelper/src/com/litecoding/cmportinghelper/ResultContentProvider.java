package com.litecoding.cmportinghelper;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

public class ResultContentProvider extends ContentProvider {
	public static final String AUTHORITY = "com.litecoding.cmportinghelper";
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	static {
		sUriMatcher.addURI(AUTHORITY, "tmp-#-#.zip", 0);
	}
	
	@Override
	public int delete(Uri uri, String arg1, String[] arg2) {
		// nothing to do here
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return "application/x-zip";
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// nothing to do here
		return null;
	}

	@Override
	public boolean onCreate() {
		//nothing to do here
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if(sUriMatcher.match(uri) == UriMatcher.NO_MATCH)
			return null;
		
		File f = new File(getContext().getFilesDir(), uri.getLastPathSegment());
		return new FileDataCursor(f, projection);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// nothing to do here
		return 0;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		if(!mode.contains("r"))
			throw new FileNotFoundException("Invalid open mode " + mode);
	
		File f = new File(getContext().getFilesDir(), uri.getLastPathSegment());
		return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
	}
	
	public class FileDataCursor extends AbstractCursor {
		private String mColumns[] = null;
		private File mFile = null;

		public FileDataCursor(File f, String[] columns) {
			mColumns = Utils.arrayCopyOf(columns, columns.length);
			mFile = f;
		}
		
		@Override
		public String[] getColumnNames() {
			return Utils.arrayCopyOf(mColumns, mColumns.length);
		}

		@Override
		public int getCount() {
			return 1;
		}

		@Override
		public double getDouble(int column) {
			return (Double) getAttributeValue(column);
		}

		@Override
		public float getFloat(int column) {
			return (Float) getAttributeValue(column);
		}

		@Override
		public int getInt(int column) {
			return (Integer) getAttributeValue(column);
		}

		@Override
		public long getLong(int column) {
			return (Long) getAttributeValue(column);
		}

		@Override
		public short getShort(int column) {
			return (Short) getAttributeValue(column);
		}

		@Override
		public String getString(int column) {
			return (String) getAttributeValue(column);
		}

		@Override
		public boolean isNull(int column) {
			return getAttributeValue(column) == null;
		}
		
		private Object getAttributeValue(int column) {
			String attrName = mColumns[column];
			if("_size".equals(attrName))
				return mFile.length();
			else if("_display_name".equals(attrName))
				return mFile.getName();
			return null;
		}
	}
}
