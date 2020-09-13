package com.example.weatherplus.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.IntDef;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;


import com.example.weatherplus.MainActivity;
import com.example.weatherplus.R;
import com.example.weatherplus.data.WeatherContract;
import com.example.weatherplus.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Vector;


public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();

    /**
     * query result status
     */
    public static final int LOCATION_STATUS_OK = 0;
    public static final int LOCATION_STATUS_SERVER_DOWN = 1;
    public static final int LOCATION_STATUS_SERVER_INVALID = 2;
    public static final int LOCATION_STATUS_UNKNOWN = 3;
    public static final int LOCATION_STATUS_INVALID = 4;
    private static final int HOUR_PER_SYNC = 6;
    private static final int SYNC_INTERVAL = 3600 * HOUR_PER_SYNC;
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;
    private ContentResolver mContentResolver;
    SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = getContext().getContentResolver();
    }

    private static void setSyncResult(Context context, @LocationStatus int syncResult) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(context.getString(R.string.pref_sync_result), syncResult).apply();
    }

    private static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Log.d(LOG_TAG, "setting up periodic sync every " + SYNC_INTERVAL + " seconds");
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    private static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {

        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Without calling setSyncAutomatically, our periodic sync will not be enabled.
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync Called.");

        Context context = getContext();
        String locationQuery = Utility.getPreferredLocation(context);
        String units = Utility.getPreferredUnit(context);
        mContentResolver = context.getContentResolver();

        // If there's no zip code, there's nothing to look up.  Verify size of params.
        if (locationQuery == null) {
            return;
        }

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;
        String format = "json";
        int numDays = 14;

        Log.d(LOG_TAG, "Starting Service with param: unit = " + units + "  location = " + locationQuery);

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "appid";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationQuery)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, Utility.MY_APP_ID)
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            if (inputStream == null) {
                setSyncResult(context, LOCATION_STATUS_SERVER_INVALID);
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                setSyncResult(context, LOCATION_STATUS_SERVER_DOWN);
                // Stream was empty.  No point in parsing.
                return;
            }
            forecastJsonStr = buffer.toString();
            getWeatherDataFromJson(context, forecastJsonStr, locationQuery);

        } catch (IOException e) {
            setSyncResult(context, LOCATION_STATUS_SERVER_DOWN);
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
        } catch (JSONException e) {
            setSyncResult(context, LOCATION_STATUS_SERVER_INVALID);
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName        A human-readable city name, e.g "Mountain View"
     * @param lat             the latitude of the city
     * @param lon             the longitude of the city
     * @return the row ID of the added location.
     */
    private long addLocation(String locationSetting, String cityName, double lat, double lon) {

        Cursor c = mContentResolver.query(WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATON_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        assert c != null;
        if (c.moveToFirst()) {
            int columnIndex = c.getColumnIndex(WeatherContract.LocationEntry._ID);
            return c.getLong(columnIndex);
        } else {
            ContentValues values = new ContentValues();
            values.put(WeatherContract.LocationEntry.COLUMN_LOCATON_SETTING, locationSetting);
            values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            Uri uri = mContentResolver.insert(WeatherContract.LocationEntry.CONTENT_URI, values);
            return ContentUris.parseId(uri);
        }
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getWeatherDataFromJson(Context context, String forecastJsonStr,
                                        String locationSetting)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        final String OWM_MESSAGE_CODE = "cod";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);

            if (forecastJson.has(OWM_MESSAGE_CODE)) {
                int errorCode = forecastJson.getInt(OWM_MESSAGE_CODE);
                switch (errorCode) {
                    case HttpURLConnection.HTTP_OK:
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        Utility.setSynchStatus(context, LOCATION_STATUS_INVALID);
                        return;
                    default:
                        Utility.setSynchStatus(context, LOCATION_STATUS_SERVER_DOWN);
                        return;
                }
            }
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            for (int i = 0; i < weatherArray.length(); i++) {
                // These are the values that will be collected.
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                        dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGRESS, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            }

            // add to database
            if (cVVector.size() > 0) {
                ContentValues[] cvArr = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArr);
                mContentResolver.bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArr);

                // delete entry older than 1 day
                Calendar cal = Calendar.getInstance(); //Get's a calendar object with the current time.

                Log.d(LOG_TAG, "Current Time: " + String.valueOf(cal.getTimeInMillis()));
                cal.add(Calendar.DATE, -1); //Signifies yesterday's date
                String yesterdayDate = String.valueOf(cal.getTimeInMillis());

                Log.d(LOG_TAG, "Deleting forecast before : " + yesterdayDate);
                int deletedEntry = mContentResolver.delete(WeatherContract.WeatherEntry.CONTENT_URI,
                        WeatherContract.WeatherEntry.COLUMN_DATE + " <= ?", new String[]{yesterdayDate});

                Log.d(LOG_TAG, "Deleted " + deletedEntry + " entries");

                // notify database has changed
                notifyWeather();
            }

            Log.d(LOG_TAG, "Fetching Weather Service Complete. " + cVVector.size() + " Inserted");

            setSyncResult(context, LOCATION_STATUS_OK);

        } catch (JSONException e) {
            setSyncResult(context, LOCATION_STATUS_SERVER_INVALID);
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }





    private void notifyWeather() {

        Context context = getContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationkey = context.getString(R.string.pref_enablenotification_key);
        boolean displayNotification = prefs.getBoolean(displayNotificationkey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));


        if (displayNotification) {

            String lastnotificationkey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastnotificationkey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {

                String locationQuery = Utility.getPreferredLocation(context);
                Uri weatherUri = WeatherContract.WeatherEntry.buildweatherlocationwithdate(locationQuery,
                        System.currentTimeMillis());


                Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null,
                        null, null);


                if (cursor.moveToFirst()) {
                    int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                    double high = cursor.getDouble(INDEX_MAX_TEMP);
                    double low = cursor.getDouble(INDEX_MIN_TEMP);
                    String desc = cursor.getString(INDEX_SHORT_DESC);


                    int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                    Resources resources = context.getResources();
                    Bitmap largeIcon = BitmapFactory.decodeResource(resources,
                            Utility.getArtResourceForWeatherCondition(weatherId));
                    String title = context.getString(R.string.app_name);

                    String contentText = String.format(context.getString(R.string.format_notification),
                            desc,
                            Utility.formatTemperature(context, high, Utility.isMetric(context)),
                            Utility.formatTemperature(context, low, Utility.isMetric(context)));




                    NotificationManager notifManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


                    String channnelId = "channel-01";
                    String channelname ="CHANNEL NAME";
                    int importance = NotificationManager.IMPORTANCE_HIGH;



                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

                        NotificationChannel mChannel = new NotificationChannel(channnelId, channelname, importance);
                        notifManager.createNotificationChannel(mChannel);
                    }



                    NotificationCompat.Builder notifBuilder =
                            new NotificationCompat.Builder(context, channnelId)
                                    .setColor(resources.getColor(R.color.sunshine_light_blue))
                                    .setSmallIcon(iconId)
                                    .setVibrate(new long[]{1000, 1000})
                                    .setLargeIcon(largeIcon)
                                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                                    .setContentTitle(title)
                                    .setPriority(Notification.PRIORITY_HIGH)
                                    .setChannelId(channnelId)
                                    .setContentText(contentText);

                    Intent resultIntent = new Intent(context, MainActivity.class);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0
                            , PendingIntent.FLAG_UPDATE_CURRENT);

                    notifBuilder.setContentIntent(resultPendingIntent);

                    notifManager.notify(WEATHER_NOTIFICATION_ID, notifBuilder.build());



                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastnotificationkey, System.currentTimeMillis());
                    editor.apply();
                    editor.commit();

                }

                cursor.close();


            }

        }


    }

    @IntDef({LOCATION_STATUS_OK, LOCATION_STATUS_SERVER_DOWN, LOCATION_STATUS_SERVER_INVALID, LOCATION_STATUS_UNKNOWN, LOCATION_STATUS_INVALID})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LocationStatus {
    }

}
