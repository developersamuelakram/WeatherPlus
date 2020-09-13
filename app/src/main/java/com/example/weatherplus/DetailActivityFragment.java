package com.example.weatherplus;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.weatherplus.data.WeatherContract;
import com.example.weatherplus.fragment.ForecastFragment;
import com.example.weatherplus.util.Utility;

import java.util.Currency;


public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String DETAIL_URI = "DETAIL_URI";
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_WIND_SPEED = 6;
    static final int COL_WEATHER_DEGREES = 7;
    static final int COL_WEATHER_PRESSURE = 8;
    static final int COL_LOCATION_SETTING = 9;
    static final int COL_WEATHER_CONDITION_ID = 10;
    static final int COL_COORD_LAT = 11;
    static final int COL_COORD_LONG = 12;
     private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    private static final String SUNSHINE_HASH_TAG = "#SUNSHINEAPP:";
    private static final int DETAIL_FORECAST_LOADER_ID  = 0;

    private static final String[] FORECAST_COLUMN =

            {
                    WeatherContract.WeatherEntry.TABLE_NAME  + "."
                     + WeatherContract.WeatherEntry._ID,
                    WeatherContract.WeatherEntry.COLUMN_DATE,
                    WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                    WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                    WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                    WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
                    WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
                    WeatherContract.WeatherEntry.COLUMN_DEGRESS,
                    WeatherContract.WeatherEntry.COLUMN_PRESSURE,
                    WeatherContract.LocationEntry.COLUMN_LOCATON_SETTING,
                    WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                    WeatherContract.LocationEntry.COLUMN_COORD_LAT,
                    WeatherContract.LocationEntry.COLUMN_COORD_LONG,


            };


    ShareActionProvider mShareActionProvider; //forsharing shit
    String mForecastString;

    // the views in detail activity fragment
    TextView mDayTv, mDateTv, mHighTv, mLowTv, mHumidityTv, mWindTv, mPressureTv, mForecastTextView;
    ImageView mImage;
    Uri mUri;



    public DetailActivityFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true);
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        Bundle args = getArguments();
        if (args!=null) {

            mUri = args.getParcelable(DETAIL_URI);

        }


        mDayTv = view.findViewById(R.id.detail_day);
        mDateTv = view.findViewById(R.id.detail_date);
        mHighTv = view.findViewById(R.id.detail_hi);
        mLowTv = view.findViewById(R.id.detail_low);
        mHumidityTv = view.findViewById(R.id.detail_humidity);
        mWindTv  = view.findViewById(R.id.detail_wind);
        mPressureTv  = view.findViewById(R.id.detail_pressure);
        mForecastTextView = view.findViewById(R.id.detail_forecast);
        mImage =  view.findViewById(R.id.detail_icon);



        return view;
    }



    // work on menu here or below same thing

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.detail_fragment, menu);

        MenuItem itemshare = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(itemshare);

        if (mForecastString!=null) {
            // createing a new method

            mShareActionProvider.setShareIntent(createshareintent());
        } else {
            Log.d(LOG_TAG, "share provider is null");
        }
    }

    private Intent createshareintent() {

        Intent shareintent = new Intent(Intent.ACTION_SEND);
        shareintent.putExtra(Intent.EXTRA_TEXT, mForecastString + SUNSHINE_HASH_TAG);
        shareintent.setType("text/plain");
        shareintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);


        return shareintent;
    }


    @Override
    public void onActivityCreated(Bundle savedInstancestate) {

        getLoaderManager().initLoader(DETAIL_FORECAST_LOADER_ID, getArguments(), this);

        super.onActivityCreated(savedInstancestate);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {

        if (mUri !=null) {


            return new CursorLoader(getActivity(),
                    mUri, FORECAST_COLUMN,
                    null,
                    null,
                    null
            );
        }

        return null;



    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data.moveToFirst()) {

            Context context = getContext();

            long timemills = data.getLong(COL_WEATHER_DATE);
            String date = Utility.formatDate(timemills);

            String location = data.getString(COL_LOCATION_SETTING);
            String desc = data.getString(COL_WEATHER_DESC);

            mForecastString = date + "" + location + "" + desc;
            mForecastTextView.setText(desc);

            // this converts the day into the name of the day
            mDayTv.setText(Utility.getDayName(context, timemills));
            // this was converted above so no need to do it again
            mDateTv.setText(date);
            mHighTv.setText(Utility.formatTemperature(context, data.getDouble(COL_WEATHER_MAX_TEMP), Utility.isMetric(context)));
            mLowTv.setText(Utility.formatTemperature(context, data.getDouble(COL_WEATHER_MIN_TEMP), Utility.isMetric(context)));

            mHumidityTv.setText(String.format(context.getString(R.string.format_humidity),
                    data.getFloat(COL_WEATHER_HUMIDITY)));

            mWindTv.setText(Utility.getFormattedWind(context,
                    data.getFloat(COL_WEATHER_WIND_SPEED), data.getFloat(COL_WEATHER_DEGREES)));

            mPressureTv.setText(String.format(
                    context.getString(R.string.format_pressure), data.getFloat(COL_WEATHER_PRESSURE)));



            // WEATHERID gives the number
            mImage.setImageResource(Utility.getArtResourceForWeatherCondition(data.getInt(COL_WEATHER_CONDITION_ID)));

            if (mShareActionProvider !=null) {

                mShareActionProvider.setShareIntent(createshareintent());

            }






        }

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

        mForecastTextView.setText("");

    }


    void onLocationChanged(String newLocation) {
        Uri uri = mUri;

        if (uri!=null) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            mUri = WeatherContract.WeatherEntry.buildweatherlocationwithdate(newLocation, date);
            getLoaderManager().restartLoader(DETAIL_FORECAST_LOADER_ID, null, this);


        }

    }

}