package com.example.weatherplus.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

public class WeatherContract {

    public static final String CONTENT_AUTHORITY = "com.example.weatherplus"; // packagename
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY); // creating a base URI
    //creating two paths of two database
    public static final String PATH_WEATHER = "weather";
    public static final String PATH_LOCATION = "location";

    // method for date
    public static long normalizeDate(long startDate) {

        Time time = new Time();

        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }

    // creating two tables and columns

    public static final class LocationEntry implements BaseColumns {

        public static final String TABLE_NAME = "location";
        public static final String COLUMN_LOCATON_SETTING = "location_settings";
        public static final String COLUMN_CITY_NAME = "city_name";
        public static final String COLUMN_COORD_LAT = "lat";
        public static final String COLUMN_COORD_LONG = "long";

        // CREATING A NEW CONTENTURI
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();
        // type
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        public static final String CONTENT_ITEM_TYPE
                = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;


        // creating a new method
        static Uri buildLocationUri(long id) {

            return ContentUris.withAppendedId(CONTENT_URI, id);

        }

        static String getLocationSettingFromUri(Uri uri) {

            return uri.getPathSegments().get(0);
        }

    }


        // another table

        public static final class WeatherEntry implements BaseColumns{

            public static final String TABLE_NAME = "weather";
            public static final String COLUMN_LOC_KEY = "location_id";
            public static final String COLUMN_DATE = "date";
            public static final String COLUMN_WEATHER_ID = "weather_id";
            public static final String COLUMN_SHORT_DESC = "short_desc";
            public static final String COLUMN_MIN_TEMP = "min";
            public static final String COLUMN_MAX_TEMP = "max";
            public static final String COLUMN_HUMIDITY = "humidity";
            public static final String COLUMN_PRESSURE = "pressure";
            public static final String COLUMN_WIND_SPEED = "wind";
            public static final String COLUMN_DEGRESS = "degress";

            // each table must have its own contentURI
            public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build();

            static final String CONTENT_TYPE =
                    ContentResolver.CURSOR_DIR_BASE_TYPE + "/"  + CONTENT_AUTHORITY + "/" + PATH_WEATHER;

            static final String CONTENT_ITEM_TYPE =
                    ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"  + CONTENT_AUTHORITY + "/" + PATH_WEATHER;


            // creating a uri
            public static Uri buildweatherUri(long id) {
                return ContentUris.withAppendedId(CONTENT_URI, id);

            }

          public  static Uri buildweatherlocationwithstartdate(String locationSettings, long startDate) {

                long normalizeddate = normalizeDate(startDate);
                return CONTENT_URI.buildUpon().appendPath(locationSettings)
                        .appendQueryParameter(COLUMN_DATE, Long.toString(normalizeddate)).build();
            }

            public static Uri buildweatherlocationwithdate(String locationSettings, long date) {

                return CONTENT_URI.buildUpon().appendPath(locationSettings)
                        .appendPath(Long.toString(normalizeDate(date))).build();

            }

            public static String getLocationsettingsFromUri(Uri uri) {

                return uri.getPathSegments().get(1);
            }


            public static long getDateFromUri(Uri uri) {
                return Long.parseLong(uri.getPathSegments().get(2));
            }

            static long getStartDatefromUri(Uri uri) {

                String dateString = uri.getQueryParameter(COLUMN_DATE);

                if (dateString!=null && dateString.length() > 0) {

                    return Long.parseLong(dateString);
                } else {
                    return 0;
                }
            }


        }



    }
