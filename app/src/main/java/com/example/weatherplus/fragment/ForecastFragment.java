package com.example.weatherplus.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.weatherplus.R;
import com.example.weatherplus.data.WeatherContract;
import com.example.weatherplus.sync.SunshineSyncAdapter;
import com.example.weatherplus.util.Utility;

import javax.security.auth.callback.Callback;


public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;
    private static final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private static final int FORECAST_LOADER_ID = 1;
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATON_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };
    private static final String SELECTED_ITEM_POSITION = "sel";
    ListView mForecastListView;
    ForecastAdapter mForecastAdapter;
    SharedPreferences mSharedPreference;
    Callback mCallback = null;
    private int mPosition;

    public ForecastFragment() {
    }

    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onAttach(Context context) {
        try {
            mCallback = (Callback) context;
        } catch (ClassCastException c) {
            throw new ClassCastException(context.toString() + " must implement onItemSelected");
        }
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //report to activity that this fragment has menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context context = getContext();
        //inflate rootView
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        if (savedInstanceState != null) {
            mPosition = savedInstanceState.getInt(SELECTED_ITEM_POSITION);
        } else {
            mPosition = 0;
        }


        //create adapter for forecast list
        mForecastAdapter = new ForecastAdapter(context, null, 0);

        //find listView and attach the adapter
        mForecastListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        View emptyView = rootView.findViewById(R.id.listview_forecast_empty);
        mForecastListView.setEmptyView(emptyView);
        mForecastListView.setAdapter(mForecastAdapter);

        mForecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);

                mPosition = position;
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    Uri dateUri = WeatherContract.WeatherEntry.buildweatherlocationwithdate(locationSetting
                            , cursor.getLong(COL_WEATHER_DATE));
                    mCallback.onItemSelected(dateUri);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int idSelected = item.getItemId();
        if (idSelected == R.id.action_refresh) {
            //access weather API to get weather forecast (do in Background Thread)
            updateWeather();
            return true;
        }

        if (idSelected == R.id.action_location) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateWeather() {
        SunshineSyncAdapter.syncImmediately(getContext());
    }

    public void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_ITEM_POSITION, mPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri weatherWithDateUri = WeatherContract.WeatherEntry
                .buildweatherlocationwithstartdate(Utility.getPreferredLocation(getContext()),
                        System.currentTimeMillis());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        return new CursorLoader(getContext(), weatherWithDateUri, FORECAST_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()){
            mForecastAdapter.swapCursor(data);
            mForecastListView.smoothScrollToPositionFromTop(mPosition, 20, 1000);
            return;
        }

        //check network
        Activity act = getActivity();
        if (!Utility.isNetworkAvailable(act)){
            TextView emptyView = (TextView) act.findViewById(R.id.listview_forecast_empty);
            emptyView.setText(R.string.empty_forecast_nocon_text);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean newValue) {
        boolean mUseTodayLayout = newValue;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUsedTodayLayout(newValue);
        }
    }

    private void openPreferredLocationInMap() {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        if (null != mForecastAdapter) {
            Cursor c = mForecastAdapter.getCursor();
            if (null != c) {
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
                }
            }

        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getContext().getString(R.string.pref_sync_result))) {
            updateEmptyView();
        }
    }

    public void updateEmptyView() {
        if (mForecastAdapter.getCount() == 0) {
            Activity act = getActivity();
            int syncResult = Utility.getSyncStatus(act);
            int stringId;
            switch (syncResult) {
                case SunshineSyncAdapter.LOCATION_STATUS_OK:
                    return;
                case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                    stringId = R.string.empty_forecast_list_server_down;
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                    stringId = R.string.empty_forecast_list_server_error;
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                    stringId = R.string.empty_forecast_invalid;
                    break;
                default:
                    stringId = R.string.empty_forecast_empty_text;
            }

            TextView emptyView = (TextView) act.findViewById(R.id.listview_forecast_empty);
            if (emptyView != null) {
                emptyView.setText(stringId);
            }
        }
    }

    public interface Callback {
        void onItemSelected(Uri dateUri);
    }

}
