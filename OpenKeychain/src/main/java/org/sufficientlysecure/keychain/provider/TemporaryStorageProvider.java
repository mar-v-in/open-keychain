/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import android.text.TextUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.DatabaseUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TemporaryStorageProvider extends ContentProvider {

    public interface Columns {
        // These columns are also used by document and media providers
        public static final String DATA = "_data";
        public static final String MIME_TYPE = "mime_type";
    }

    private static final String DB_NAME = "tempstorage.db";
    private static final String TABLE_FILES = "files";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_MIME = "mime";
    private static final Uri BASE_URI = Uri.parse("content://org.sufficientlysecure.keychain.tempstorage/");
    private static final int DB_VERSION = 2;

    public static Uri createFile(Context context, String targetName, String targetMime) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, targetName);
        contentValues.put(COLUMN_MIME, targetMime);
        return context.getContentResolver().insert(BASE_URI, contentValues);
    }

    public static int cleanUp(Context context) {
        return context.getContentResolver().delete(BASE_URI, COLUMN_TIME + "< ?",
                new String[]{Long.toString(System.currentTimeMillis() - Constants.TEMPFILE_TTL)});
    }

    public static int remove(Context context, Uri uri) {
        return context.getContentResolver().delete(uri, null, null);
    }

    private class TemporaryStorageDatabase extends SQLiteOpenHelper {

        public TemporaryStorageDatabase(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FILES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_TIME + " INTEGER" +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 1) {
                db.execSQL("ALTER TABLE " + TABLE_FILES + " ADD COLUMN " + COLUMN_MIME + " TEXT DEFAULT '*/*';");
            }
        }
    }

    private TemporaryStorageDatabase db;

    private File getFile(Uri uri) throws FileNotFoundException {
        try {
            return getFile(Integer.parseInt(uri.getLastPathSegment()));
        } catch (NumberFormatException e) {
            throw new FileNotFoundException();
        }
    }

    private File getFile(int id) {
        return new File(getContext().getCacheDir(), "temp/" + id);
    }

    @Override
    public boolean onCreate() {
        db = new TemporaryStorageDatabase(getContext());
        return new File(getContext().getCacheDir(), "temp").mkdirs();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File file;
        try {
            file = getFile(uri);
        } catch (FileNotFoundException e) {
            return null;
        }
        Cursor fileInfo = db.getReadableDatabase().query(TABLE_FILES, new String[]{COLUMN_NAME, COLUMN_MIME}, COLUMN_ID + "=?",
                new String[]{uri.getLastPathSegment()}, null, null, null);
        if (fileInfo != null) {
            if (fileInfo.moveToNext()) {
                MatrixCursor cursor = new MatrixCursor(new String[]{
                        OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, Columns.DATA, Columns.MIME_TYPE});
                cursor.newRow().add(fileInfo.getString(0)).add(file.length()).add(file.getAbsolutePath()).add(fileInfo.getString(1));
                fileInfo.close();
                return cursor;
            }
            fileInfo.close();
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        String mime = "*/*";
        if (uri.getLastPathSegment() != null) {
            Cursor query = query(uri, null, null, null, null);
            try {
                String s = query.getString(query.getColumnIndexOrThrow(Columns.MIME_TYPE));
                if (!TextUtils.isEmpty(s)) mime = s;
            } catch (Exception ignored) {
            }
        }
        return mime;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (!values.containsKey(COLUMN_TIME)) {
            values.put(COLUMN_TIME, System.currentTimeMillis());
        }
        int insert = (int) db.getWritableDatabase().insert(TABLE_FILES, null, values);
        try {
            getFile(insert).createNewFile();
        } catch (IOException e) {
            return null;
        }
        return Uri.withAppendedPath(BASE_URI, Long.toString(insert));
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (uri.getLastPathSegment() != null) {
            selection = DatabaseUtil.concatenateWhere(selection, COLUMN_ID + "=?");
            selectionArgs = DatabaseUtil.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
        }
        Cursor files = db.getReadableDatabase().query(TABLE_FILES, new String[]{COLUMN_ID}, selection,
                selectionArgs, null, null, null);
        if (files != null) {
            while (files.moveToNext()) {
                getFile(files.getInt(0)).delete();
            }
            files.close();
            return db.getWritableDatabase().delete(TABLE_FILES, selection, selectionArgs);
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update not supported");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return openFileHelper(uri, mode);
    }
}
