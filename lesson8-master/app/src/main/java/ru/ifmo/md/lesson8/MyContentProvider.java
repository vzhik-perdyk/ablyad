package ru.ifmo.md.lesson8;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class MyContentProvider extends ContentProvider {
    public static final String DATABASE_NAME =  "weather.db";

    public static final String TABLE_CITIES = "cities";
    public static final String TABLE_FORECAST = "forecast";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_CITY_NAME = "name";
    public static final String COLUMN_WEATHER_ICON = "weather_icon";
    public static final String COLUMN_TEMP = "temp";
    public static final String COLUMN_WIND = "wind";
    public static final String COLUMN_PRESSURE = "pressure";
    public static final String COLUMN_CITY_ID = "city_id";
    public static final String COLUMN_DATE = "date";

    public static final String AUTHORITY = "ru.ifmo.md.lesson8";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_CITIES_CREATE = "create table "
            + TABLE_CITIES + " ("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_CITY_NAME + " text not null, "
            + COLUMN_WEATHER_ICON + " text, "
            + COLUMN_TEMP + " text, "
            + COLUMN_WIND + " text, "
            + COLUMN_PRESSURE + " text);";

    public static final String TABLE_FORECAST_CREATE = "create table "
            + TABLE_FORECAST + " ("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_CITY_ID + " integer, "
            + COLUMN_DATE + " text, "
            + COLUMN_WEATHER_ICON + " text, "
            + COLUMN_TEMP + " text, "
            + COLUMN_WIND + " text, "
            + COLUMN_PRESSURE + " text);";

    public static final Uri TABLE_FORECAST_URI = Uri.parse("content://"
            + AUTHORITY + "/" + TABLE_FORECAST);

    public static final Uri TABLE_CITIES_URI = Uri.parse("content://"
            + AUTHORITY + "/" + TABLE_CITIES);

    static final int CITIES = 1;
    static final int CITIES_ID = 2;
    static final int FORECAST = 3;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, TABLE_FORECAST + "/#", FORECAST);
        uriMatcher.addURI(AUTHORITY, TABLE_CITIES, CITIES);
        uriMatcher.addURI(AUTHORITY, TABLE_CITIES + "/#", CITIES_ID);
    }

    MySQLiteHelper dbHelper;


    @Override
    public boolean onCreate() {
        dbHelper = new MySQLiteHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings2, String s2) {
        SQLiteQueryBuilder sqB = new SQLiteQueryBuilder();
        switch (uriMatcher.match(uri)) {
            case CITIES:
                sqB.setTables(TABLE_CITIES);
                break;
            case CITIES_ID:
                sqB.setTables(TABLE_CITIES);
                sqB.appendWhere(COLUMN_ID+"="+uri.getLastPathSegment());
                break;
            case FORECAST:
                sqB.setTables(TABLE_FORECAST);
                sqB.appendWhere(COLUMN_CITY_ID+"="+uri.getLastPathSegment());
                break;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = sqB.query(db, strings, s, strings2, null, null, s2);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;

    }

    @Override
    public String getType(Uri uri) {
        return "";
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase sqlQB = dbHelper.getWritableDatabase();
        long insertedID=1;
        switch (uriMatcher.match(uri)) {
            case CITIES:
                insertedID = sqlQB.insert(TABLE_CITIES, null, contentValues);
                break;
            case FORECAST:
                contentValues.put(COLUMN_CITY_ID, uri.getLastPathSegment());
                insertedID = sqlQB.insert(TABLE_FORECAST, null, contentValues);
                break;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.withAppendedPath(uri, Long.toString(insertedID));
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        SQLiteDatabase sqlQB = dbHelper.getWritableDatabase();
        int result = 0;
        String ending;
        switch (uriMatcher.match(uri)) {
            case CITIES_ID:
                ending = uri.getLastPathSegment();
                if (TextUtils.isEmpty(s)) {
                    result = sqlQB.delete(TABLE_CITIES, COLUMN_ID + " = "+ ending, null);
                }
                else {
                    result = sqlQB.delete(TABLE_CITIES, COLUMN_ID + " = "+ ending + " and " + s, strings);
                }
                break;

            case FORECAST:
                ending = uri.getLastPathSegment();
                if (TextUtils.isEmpty(s)) {
                    result = sqlQB.delete(TABLE_FORECAST, COLUMN_CITY_ID + " = "+ ending, null);
                }
                else {
                    result = sqlQB.delete(TABLE_FORECAST, COLUMN_CITY_ID + " = "+ ending + " and " + s, strings);
                }
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        SQLiteDatabase sqlQB = dbHelper.getWritableDatabase();
        int result = 0;
        String ending;
        switch (uriMatcher.match(uri)) {
            case CITIES_ID:
                ending = uri.getLastPathSegment();
                if (TextUtils.isEmpty(s)) {
                    result = sqlQB.update(TABLE_CITIES, contentValues, COLUMN_ID + " = "+ ending, null);
                }
                else {
                    result = sqlQB.update(TABLE_CITIES, contentValues, COLUMN_ID + " = "+ ending + " and " + s, strings);
                }
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    public class MySQLiteHelper extends SQLiteOpenHelper {


        public MySQLiteHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(TABLE_CITIES_CREATE);
            database.execSQL(TABLE_FORECAST_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(MySQLiteHelper.class.getName(),
                    "Upgrading database from version " + oldVersion + " to "
                            + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CITIES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FORECAST);
            onCreate(db);
        }
    }

}
