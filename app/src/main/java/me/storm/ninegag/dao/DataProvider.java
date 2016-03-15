package me.storm.ninegag.dao;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import me.storm.ninegag.App;

/**
 * Created by storm on 14-4-8.
 */
//koala@20160315: 自定义的内容提供器
public class DataProvider extends ContentProvider {
    static final String TAG = DataProvider.class.getSimpleName();

    static final Object DBLock = new Object();

    public static final String AUTHORITY = "com.storm.9gag.provider";

    public static final String SCHEME = "content://";

    // messages
    public static final String PATH_FEEDS = "/feeds";

    public static final Uri FEEDS_CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_FEEDS);

    private static final int FEEDS = 0;

    /*
     * MIME type definitions
     */
    public static final String FEED_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.storm.9gag.feed";

    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "feeds", FEEDS);
        //koala@20160314:
        // Add a URI to match, and the code to return when this URI is matched.
        // URI nodes may be exact match string, the token "*" that matches any text,
        // or the token "#" that matches only* numbers.
    }

    private static DBHelper mDBHelper;

    public static DBHelper getDBHelper() {
        if (mDBHelper == null) {
            mDBHelper = new DBHelper(App.getContext());
        }
        return mDBHelper;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        synchronized (DBLock) {
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            String table = matchTable(uri);     //koala@20160314: matchTable()自建方法，获取表的名字
            queryBuilder.setTables(table);      //koala@20160315: 设置要查询的表

            SQLiteDatabase db = getDBHelper().getReadableDatabase();    //koala@20160314: 创建数据库
            Cursor cursor = queryBuilder.query(db, // The database to   //koala@20160315: 在数据库db中查询
                    // queryFromDB
                    projection, // The columns to return from the queryFromDB
                    selection, // The columns for the where clause
                    selectionArgs, // The values for the where clause
                    null, // don't group the rows
                    null, // don't filter by row groups
                    sortOrder // The sort order
            );

            //koala@20160315: Register to watch a content URI for changes.
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case FEEDS:
                return FEED_CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        synchronized (DBLock) {
            String table = matchTable(uri);
            SQLiteDatabase db = getDBHelper().getWritableDatabase();
            long rowId = 0;

            //koala@20160315: Android数据库操作（特别是写操作）是非常慢的，将所有操作打包成一个事务能大大提高处理速度
            db.beginTransaction();  //koala@20160315: 开启事务
            try {
                rowId = db.insert(table, null, values);     //koala@20160315: 开启事务后，结束事务前，可进行批量操作（此处只有插入操作）
                db.setTransactionSuccessful();  //koala@20160315: 事务处理成功
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            } finally {
                db.endTransaction();    //koala@20160315: 结束事务
            }
            if (rowId > 0) {    //koala@20160315: 插入操作返回新插入的行号，或-1
                Uri returnUri = ContentUris.withAppendedId(uri, rowId);     //koala@20160315: 返回新插入value的路径uri
                getContext().getContentResolver().notifyChange(uri, null);  //koala@20160315: Notify registered observers that a row was updated and attempt to sync changesto the network.
                return returnUri;
            }
            throw new SQLException("Failed to insert row into " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        synchronized (DBLock) {
            SQLiteDatabase db = getDBHelper().getWritableDatabase();

            int count = 0;
            String table = matchTable(uri);
            db.beginTransaction();
            try {
                count = db.delete(table, selection, selectionArgs);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            getContext().getContentResolver().notifyChange(uri, null);
            return count;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        synchronized (DBLock) {
            SQLiteDatabase db = getDBHelper().getWritableDatabase();
            int count;
            String table = matchTable(uri);
            db.beginTransaction();
            try {
                count = db.update(table, values, selection, selectionArgs);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            getContext().getContentResolver().notifyChange(uri, null);

            return count;
        }
    }

    private String matchTable(Uri uri) {
        String table = null;
        switch (sUriMatcher.match(uri)) {
            case FEEDS:
                table = FeedsDataHelper.FeedsDBInfo.TABLE_NAME;     //koala@20160314: 比对成功后返回表名
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return table;
    }
}
