package com.example.weatherplus.activities.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.example.weatherplus.R;

public class LocationEditTextPreference extends EditTextPreference {


    private static final String LOG_TAG = LocationEditTextPreference.class.getSimpleName();

    private static final int DEFAULT_MIN_TEXT_LENGTH = 2;
    private int minlength = DEFAULT_MIN_TEXT_LENGTH;


    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray styleAttr = context.obtainStyledAttributes(attrs, R.styleable.LocationEditTextPreference, 0, 0);


        try {

            minlength = styleAttr.getInt(R.styleable.LocationEditTextPreference_minLength, 0);
        } finally {
            styleAttr.recycle();
        }

        Log.d(LOG_TAG, "CREATED A LOCATION EDITTEXTPREFERNCE WITH MINIMUM LENGTH = " + minlength);
    }



    @Override
    protected void showDialog(Bundle state) {

        super.showDialog(state);
        EditText editText = getEditText();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                //do nothing

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                //do nothing

            }

            @Override
            public void afterTextChanged(Editable s) {

                Dialog d = getDialog();


                if (d instanceof AlertDialog) {
                    Button positivebutton = ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE);

                    if (s.length()>=minlength) {

                        positivebutton.setEnabled(true);
                    } else {
                        positivebutton.setEnabled(true);
                    }



                }
            }
        });




    }
}
