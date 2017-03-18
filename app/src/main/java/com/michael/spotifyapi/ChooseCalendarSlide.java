package com.michael.spotifyapi;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.paolorotolo.appintro.ISlidePolicy;

import java.util.ArrayList;

import static android.R.drawable.radiobutton_off_background;
import static android.R.drawable.radiobutton_on_background;

/**
 * Created by Michael on 16.03.2017.
 */

public class ChooseCalendarSlide extends ListFragment implements ISlidePolicy {

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    public static ChooseCalendarSlide itself;
    private LayoutInflater inflator;
    private int layoutResId;
    private static boolean policyRespected = false;
    private CalendarAdapter adapter;

    public static ChooseCalendarSlide newInstance(int layoutResId) {
        ChooseCalendarSlide chooseCalendarSlide = new ChooseCalendarSlide();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        chooseCalendarSlide.setArguments(args);

        return chooseCalendarSlide;
    }

    public static void setPolicyRespected(boolean policyRespected) {
        ChooseCalendarSlide.policyRespected = policyRespected;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        itself = this;

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final IntroActivity.SmallCalendar calendar = adapter.getItem(position);
        if (calendar == null) return;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(getString(R.string.calendar_account), calendar.account);
        editor.putString(getString(R.string.calendar_name), calendar.name);
        editor.putLong(getString(R.string.calendar_id), calendar.id);
        editor.apply();
        for (int i = 0; i < l.getChildCount(); i++) {
            if (i == position) {
                l.getChildAt(i).findViewById(R.id.calendar_checkbox).setBackgroundResource(radiobutton_on_background);
                TextView name = (TextView) l.getChildAt(i).findViewById(R.id.calendar_name);
                name.setTextColor(getContext().getColor(R.color.colorAccent));
                TextView address = (TextView) l.getChildAt(i).findViewById(R.id.account_name);
                address.setTextColor(getContext().getColor(R.color.colorAccent));
            } else {
                l.getChildAt(i).findViewById(R.id.calendar_checkbox).setBackgroundResource(radiobutton_off_background);
                TextView name = (TextView) l.getChildAt(i).findViewById(R.id.calendar_name);
                name.setTextColor(Color.WHITE);
                TextView address = (TextView) l.getChildAt(i).findViewById(R.id.account_name);
                address.setTextColor(Color.WHITE);
            }
        }
        setPolicyRespected(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.inflator = inflater;
        View rootView = inflater.inflate(layoutResId, container, false);
        adapter = new CalendarAdapter(this.getContext(), R.layout.listitem_calendar, new ArrayList<IntroActivity.SmallCalendar>());
        setListAdapter(adapter);
        return rootView;
    }

    @Override
    public boolean isPolicyRespected() {
        return policyRespected;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        Toast.makeText(getContext(), "Please choose a calendar",
                Toast.LENGTH_SHORT).show();
    }

    public void showCalendars(ArrayList<IntroActivity.SmallCalendar> calendars) {
        adapter.setCalendars(calendars);
    }

    static class ViewHolder {
        TextView calendarName;
        TextView accountName;
    }

    private class CalendarAdapter extends ArrayAdapter<IntroActivity.SmallCalendar>{

        private final LayoutInflater mInflator;
        private ArrayList<IntroActivity.SmallCalendar> calendars;

        public CalendarAdapter(@NonNull Context context, @LayoutRes int resource, ArrayList<IntroActivity.SmallCalendar> calendars) {
            super(context, resource, calendars);
            mInflator = ChooseCalendarSlide.this.inflator;
            this.calendars = calendars;
        }

        public void clear() {
            calendars.clear();
        }

        @Override
        public int getCount() {
            return calendars.size();
        }

        @Override
        public IntroActivity.SmallCalendar getItem(int i) {
            return calendars.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        public void setCalendars(ArrayList<IntroActivity.SmallCalendar> calendars){
            this.calendars = calendars;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_calendar, null);
                viewHolder = new ViewHolder();
                viewHolder.accountName = (TextView) view.findViewById(R.id.account_name);
                viewHolder.calendarName = (TextView) view.findViewById(R.id.calendar_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            IntroActivity.SmallCalendar calendar = calendars.get(i);
            final String calendarName = calendar.name;
            if (calendarName != null && calendarName.length() > 0)
                viewHolder.calendarName.setText(calendarName);
            else
                viewHolder.calendarName.setText(R.string.unknown_device);
            viewHolder.accountName.setText(calendar.account);

            return view;
        }
    }
}
