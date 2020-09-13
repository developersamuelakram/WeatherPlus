package com.example.weatherplus.fragment;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.weatherplus.R;
import com.example.weatherplus.util.Utility;

public class ForecastAdapter extends CursorAdapter {



    static final int VIEW_TYPE_TODAY = 0;
    static final int VIEW_TYPE_FUTURE = 1;
    boolean usedTodayLayout = true;




    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }


    @Override
    public int getItemViewType(int position) {

        return (position == 0 && usedTodayLayout) ? VIEW_TYPE_TODAY: VIEW_TYPE_FUTURE;

    }

    @Override
    public int getViewTypeCount() {

        // because we have two layouts

        return 2;

    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {


        int viewType = getItemViewType(cursor.getPosition());

        int layoutid= -1;

        if (viewType == VIEW_TYPE_TODAY) {

            layoutid = R.layout.list_item_forecast_today;
        } else if (viewType== VIEW_TYPE_FUTURE) {

            layoutid = R.layout.list_item_forecast;
        }


        View view = LayoutInflater.from(context).inflate(layoutid, parent, false);
        ViewHolder vh = new ViewHolder(view);
        view.setTag(vh);
        return view;


    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder vh = (ViewHolder) view.getTag();

        vh.dateView.setText(getDay(context, cursor));
        vh.descriptionView.setText(getWeather(context, cursor));
        vh.hightempview.setText(getTempHigh(context, cursor));
        vh.lowtempview.setText(getTemplow(context, cursor));


        int viewType = getItemViewType(cursor.getPosition());

        if (viewType == VIEW_TYPE_TODAY) {

            vh.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID)));
        } else if ( viewType == VIEW_TYPE_FUTURE) {
            vh.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID)));


        }



    }

    private String getTemplow(Context context, Cursor c) {
        return Utility.formatTemperature(context, c.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP), Utility.isMetric(context));
    }

    private String getTempHigh(Context context, Cursor c) {
        return Utility.formatTemperature(context, c.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP), Utility.isMetric(context));

    }

    private String getWeather(Context context, Cursor c) {

        return c.getString(ForecastFragment.COL_WEATHER_DESC);
    }

    private String getDay(Context context, Cursor c) {

        return Utility.getFriendlyDayString(context, c.getLong(ForecastFragment.COL_WEATHER_DATE));

    }

    void setUsedTodayLayout(Boolean isUsed) {

        usedTodayLayout = isUsed;
    }

    public static class ViewHolder{

        ImageView iconView;
        TextView dateView, descriptionView, hightempview, lowtempview;


        public ViewHolder(View view) {


            iconView = view.findViewById(R.id.list_item_icon);
            dateView = view.findViewById(R.id.list_item_date_textview);
            descriptionView = view.findViewById(R.id.list_item_forecast_textview);
            hightempview = view.findViewById(R.id.list_item_high_textview);
            lowtempview = view.findViewById(R.id.list_item_low_textview);
        }
    }
}
