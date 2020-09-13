package com.example.weatherplus.activities;

import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.example.weatherplus.DetailActivityFragment;
import com.example.weatherplus.R;

public class DetailActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (savedInstanceState == null) {

            DetailActivityFragment df = new DetailActivityFragment();

            Bundle args = new Bundle();
            args.putParcelable(DetailActivityFragment.DETAIL_URI, getIntent().getData());

            df.setArguments(args);


            getSupportFragmentManager().beginTransaction().add(R.id.weather_detail_container, df).commit();

        }







    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;

    }

    @Override
    public  boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_settings:
                startActivity(new Intent(DetailActivity.this, SettingsActivity.class));
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

}