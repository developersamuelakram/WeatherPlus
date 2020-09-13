package com.example.weatherplus;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.example.weatherplus.activities.DetailActivity;
import com.example.weatherplus.activities.SettingsActivity;
import com.example.weatherplus.fragment.ForecastFragment;
import com.example.weatherplus.sync.SunshineSyncAdapter;
import com.example.weatherplus.util.Utility;

public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback {

    public static final String  DETAIL_FRAGMENT_TAG = "DFTAG";
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    String mlocation;

    boolean mTwopane = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        mlocation = Utility.getPreferredLocation(this);
        if (findViewById(R.id.weather_detail_container)!=null) {

            mTwopane = true;

            if (savedInstanceState == null) {

                getSupportFragmentManager().beginTransaction().replace(R.id.weather_detail_container,
                        new DetailActivityFragment(), DETAIL_FRAGMENT_TAG).commit();


            }
        } else {
            mTwopane = false;
            getSupportActionBar().setElevation(0f);

        }

        ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);

        // this method was created in ff-frag;
        ff.setUseTodayLayout(!mTwopane);

        SunshineSyncAdapter.initializeSyncAdapter(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);
        return true;

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;


        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onResume() {

        super.onResume();

        String location = Utility.getPreferredLocation(this);
        if (location!=null && !location.equals(mlocation)) {

            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);

            if (ff!=null) {

                ff.onLocationChanged();

            }


            DetailActivityFragment df =
                    (DetailActivityFragment) getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT_TAG);


            if (df!=null) {

                df.onLocationChanged(location);
            }

            mlocation = location;
        }
    }

    @Override
    public void onItemSelected(Uri locationURI) {


        if (mTwopane) {
            Bundle args = new Bundle();
            args.putParcelable(DetailActivityFragment.DETAIL_URI, locationURI);

            DetailActivityFragment df = new DetailActivityFragment();
            df.setArguments(args);

            getSupportFragmentManager()
                    .beginTransaction().
                    replace(R.id.weather_detail_container, df)
                    .commit();

        } else {
            Intent i = new Intent(this, DetailActivity.class)
                    .setData(locationURI);
            startActivity(i);
        }

    }
}