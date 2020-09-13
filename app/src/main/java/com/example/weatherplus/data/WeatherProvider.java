package com.example.weatherplus.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.se.omapi.SEService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;

import java.net.URI;

public class WeatherProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private WeatherDbHelper mOpenHelper;

    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;

    private static final SQLiteQueryBuilder sWeatherbyLocationSettingQueryBuilder;
            static {
                sWeatherbyLocationSettingQueryBuilder = new SQLiteQueryBuilder();
                sWeatherbyLocationSettingQueryBuilder.setTables(
                        WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN "
                                + WeatherContract.LocationEntry.TABLE_NAME + " ON "
                                + WeatherContract.WeatherEntry.TABLE_NAME + "."
                                + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " = "
                                + WeatherContract.LocationEntry.TABLE_NAME + "."
                        + WeatherContract.LocationEntry._ID);

            }

            //location.locationsetting


    private static final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME + "." + WeatherContract.LocationEntry.COLUMN_LOCATON_SETTING + "=?";

            // location.locationsetting =? and date >=?

    private static final String sLocationSettingWithStartDateSelection
            = WeatherContract.LocationEntry.TABLE_NAME + "."  + WeatherContract.LocationEntry.COLUMN_LOCATON_SETTING + "=? AND " +
            WeatherContract.WeatherEntry.COLUMN_DATE + " >=?";


    // location.locationsetting =? and date = ?
    private static final String sLocationSettingAndDaySelection =
            WeatherContract.LocationEntry.TABLE_NAME + "." + WeatherContract.LocationEntry.COLUMN_LOCATON_SETTING + "=? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " = ?";



    private Cursor getWeatherLocationSetting (Uri uri, String[] projection, String sortOrder) {
        // using from our contract
        String locationSetting = WeatherContract.WeatherEntry.getLocationsettingsFromUri(uri);
        long startDate = WeatherContract.WeatherEntry.getStartDatefromUri(uri);
        String[] selectionArgs;
        String selection;

        if (startDate == 0) {

            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selectionArgs = new String[]{locationSetting, Long.toString(startDate)};
            selection = sLocationSettingWithStartDateSelection;

        }

        return sWeatherbyLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection, selection, selectionArgs, null, null, sortOrder);
    }

    private Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection, String sortOrder) {
        // because the method is form setting and date so we are using that from contract
        String locationSetting  = WeatherContract.WeatherEntry.getLocationsettingsFromUri(uri);
        long date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherbyLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(), projection, sLocationSettingAndDaySelection,
                new String[]{locationSetting, Long.toString(date)}, null, null, sortOrder);



    }

    // create the buildUriMatcher method

    static UriMatcher buildUriMatcher() {

        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //URI FOR EACH PATH
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.PATH_WEATHER, WEATHER);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.PATH_WEATHER +"/*", WEATHER_WITH_LOCATION);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.PATH_WEATHER +"/*/#", WEATHER_WITH_LOCATION_AND_DATE);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.PATH_LOCATION , LOCATION);

        return uriMatcher;


    }



    @Override
    public boolean onCreate() {

        mOpenHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        Cursor retCursor;
        int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER_WITH_LOCATION_AND_DATE: {
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;

            }

            case WEATHER_WITH_LOCATION: {
                retCursor = getWeatherLocationSetting(uri, projection, sortOrder);
                break;

            }

            case WEATHER: {
                retCursor = mOpenHelper.getReadableDatabase().query(WeatherContract.WeatherEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            }


            case LOCATION: {
                retCursor = mOpenHelper.getReadableDatabase().query
                        (WeatherContract.LocationEntry.TABLE_NAME,
                                projection, selection, selectionArgs,
                                null, null, sortOrder);
                break;
        }

        default:
        throw new UnsupportedOperationException("UNKNOWN URI" + uri);



        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;

    }





    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {

        int match  = sUriMatcher.match(uri);
        switch (match) {

            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;

            case WEATHER_WITH_LOCATION:
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;

            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;



            default:
                throw new UnsupportedOperationException("UNKNOWN URI" + uri);




        }

    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri,  ContentValues values) {

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {

            case WEATHER: {

                normalizeDate(values);
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);

                if (_id >0) {

                    returnUri = WeatherContract.WeatherEntry.buildweatherUri(_id);
                } else {

                    throw new android.database.SQLException("FAILED TO INSERT ROW"  + uri);
                }

                break;


            }

            case LOCATION: {

                long _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);

                if (_id>0) {

                    // taken from the contract
                    returnUri = WeatherContract.LocationEntry.buildLocationUri(_id);
                } else {
                    throw new android.database.SQLException("FAILED TO INSERT A ROW" + uri);
                }

                break;

            }

            default:
                throw new UnsupportedOperationException("UNKNOWN URI" + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;



    }

    private void normalizeDate(ContentValues values) {

        //working on data

        if (values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)) {

            long dateValue = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);

            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateValue));


        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);

        if (selection == null) selection = "1";

        int rowsdelete = 0;

        switch (match) {


            case WEATHER: {

                rowsdelete = db.delete(WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;


            }

            case LOCATION: {

                rowsdelete = db.delete(WeatherContract.LocationEntry.TABLE_NAME, selection,selectionArgs);
                break;

            }

            default:
               throw new UnsupportedOperationException("unknown uri" + uri);



        }

        if (rowsdelete!=0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsdelete;






    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {

            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            int match = sUriMatcher.match(uri);
            int rowsupdate = 0;

            switch (match) {

                case WEATHER: {
                    rowsupdate = db.update(WeatherContract.WeatherEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                    break;


                }

                case LOCATION: {
                    rowsupdate = db.update(WeatherContract.LocationEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                    break;

                }

                default:
                    throw new UnsupportedOperationException("unknown uri"  + uri);


            }


            if (rowsupdate!=0) {
                getContext().getContentResolver().notifyChange(uri, null);

            }

            return rowsupdate;

    }

    // study what is bulinsert

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);

        switch (match) {
            case WEATHER:

                db.beginTransaction();

                int returncount = 0;

                try {

                    for (ContentValues value : values) {
                        normalizeDate(value);

                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);


                        if (_id != -1) {

                            returncount++;
                        }
                    }

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                getContext().getContentResolver().notifyChange(uri, null);
                return returncount;


            default:
                return  super.bulkInsert(uri, values);
        }


    }


    @Override
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
