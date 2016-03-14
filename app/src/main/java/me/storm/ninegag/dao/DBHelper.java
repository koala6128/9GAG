package me.storm.ninegag.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by storm on 14-4-8.
 */
//koala@20160314: DBHelper提供创建SQLiteDatabase数据库的功能，可自定义数据库的名字，具体创建的实现在FeedsDataHelper中配置

public class DBHelper extends SQLiteOpenHelper {
    // 数据库名
    private static final String DB_NAME = "9gag.db";

    // 数据库版本
    private static final int VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        FeedsDataHelper.FeedsDBInfo.TABLE.create(db);   //koala@20160314: 通过FeedsDataHelper在数据库中建表
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
