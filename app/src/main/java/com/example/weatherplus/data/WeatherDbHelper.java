package com.example.weatherplus.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WeatherDbHelper extends SQLiteOpenHelper {


    // database name and schmea version
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME ="weather.db";


    WeatherDbHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        String SQL_TABLE_WEATHER = "CREATE TABLE " + WeatherContract.WeatherEntry.TABLE_NAME + "( "
                + WeatherContract.WeatherEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " INTEGER NOT NULL, "
                + WeatherContract.WeatherEntry.COLUMN_DATE + " INTEGER NOT NULL,"
                + WeatherContract.WeatherEntry.COLUMN_SHORT_DESC + " TEXT NOT NULL, "
                + WeatherContract.WeatherEntry.COLUMN_WEATHER_ID + " INTEGER NOT NULL, "
                + WeatherContract.WeatherEntry.COLUMN_MIN_TEMP + " REAL NOT NULL, "
                + WeatherContract.WeatherEntry.COLUMN_MAX_TEMP + " REAL NOT NULL, "
                + WeatherContract.WeatherEntry.COLUMN_HUMIDITY + " REAL NOT NULL, "
                + WeatherContract.WeatherEntry.COLUMN_PRESSURE + " REAL NOT NULL, "
                + WeatherContract.WeatherEntry.COLUMN_WIND_SPEED + " REAL NOT NULL,"
                + WeatherContract.WeatherEntry.COLUMN_DEGRESS + " REAL NOT NULL, "

                + "FOREIGN KEY (" + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + ") REFERENCES "
                + WeatherContract.LocationEntry.TABLE_NAME + "(" + WeatherContract.LocationEntry._ID + "),"
                + "UNIQUE(" + WeatherContract.WeatherEntry.COLUMN_DATE + ","
                + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + ") ON CONFLICT REPLACE);";



        String SQL_TABLE_LOCATION = "CREATE TABLE " + WeatherContract.LocationEntry.TABLE_NAME + " (" +
                WeatherContract.LocationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + WeatherContract.LocationEntry.COLUMN_LOCATON_SETTING + " TEXT UNIQUE NOT NULL,"
                + WeatherContract.LocationEntry.COLUMN_CITY_NAME + " TEXT NOT NULL, "
                + WeatherContract.LocationEntry.COLUMN_COORD_LAT + " REAL NOT NULL, "
                + WeatherContract.LocationEntry.COLUMN_COORD_LONG + " REAL NOT NULL, "
                + "UNIQUE (" + WeatherContract.LocationEntry.COLUMN_COORD_LAT + ","
                + WeatherContract.LocationEntry.COLUMN_COORD_LONG + ") ON CONFLICT REPLACE);";




            sqLiteDatabase.execSQL(SQL_TABLE_WEATHER);
            sqLiteDatabase.execSQL(SQL_TABLE_LOCATION);



    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WeatherContract.LocationEntry.TABLE_NAME);

        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WeatherContract.WeatherEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);


    }
}
