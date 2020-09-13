package com.example.weatherplus.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.weatherplus.R;
import com.example.weatherplus.sync.SunshineSyncAdapter;
import com.example.weatherplus.util.Utility;

import java.util.prefs.Preferences;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {




    @Override
    protected void onPause() {

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }


    @Override
    protected void onResume() {

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notification_key)));






    }

    private void bindPreferenceSummaryToValue(Preference preference) {

        preference.setOnPreferenceChangeListener(this);

        if (preference instanceof CheckBoxPreference) {

            onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                    .getBoolean(preference.getKey(), true));
        } else {

            onPreferenceChange(preference, PreferenceManager.
                    getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
        }
    }



    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {

        String stringvalue = value.toString();

        if (preference instanceof ListPreference) {


            ListPreference listPreference = (ListPreference) preference;

            int prefindex = listPreference.findIndexOfValue(stringvalue);

            if (prefindex>=0) {
                preference.setSummary(listPreference.getEntries()[prefindex]);


            }
        } else {
            String prefkey = preference.getKey();

            if (prefkey.equals(getString(R.string.pref_location_key))) {

                @SunshineSyncAdapter.LocationStatus int locationstatus = Utility.getSyncStatus(this);

                switch (locationstatus) {
                    case SunshineSyncAdapter.LOCATION_STATUS_OK:
                        break;

                        case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                            stringvalue = getString(R.string.pref_location_error_description, stringvalue);
                            break;

                            case SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN:
                                stringvalue = getString(R.string.pref_location_unknown_description, stringvalue);
                                break;

                    default:
                        break;



                }


            } else if (prefkey.equals(getString(R.string.pref_notification_key))) {

                if (value.equals(Boolean.FALSE)) {
                    stringvalue = getString(R.string.pref_notification_disabled_description);
                } else {
                    stringvalue = getString(R.string.pref_notification_enabled_description);
                }
            }

            preference.setSummary(stringvalue);
        }

        SunshineSyncAdapter.syncImmediately(this);
        return true;

    }


    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {


        if (key.equals(getString(R.string.pref_location_key))) {

            Utility.setSynchStatus(this, SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN);
            SunshineSyncAdapter.syncImmediately(this);

        }

    }


}