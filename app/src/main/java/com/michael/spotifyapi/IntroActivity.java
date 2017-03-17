package com.michael.spotifyapi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroBase;
import com.github.paolorotolo.appintro.AppIntroFragment;

import java.util.ArrayList;

import static android.R.drawable.stat_sys_data_bluetooth;

public class IntroActivity extends AppIntro {

    public static AppIntroBase itself;
    ArrayList<Integer> initialHeartRates;
    private BroadcastReceiver initialBroadcastReceiver = new BroadcastReceiver() {
        // Receives broadcasts sent from other points of the app, like the SensorsDataService
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(SensorService.ACTION_HR)) {
                // Prints out the heart rate
                final TextView HeartRate = (TextView) findViewById(R.id.initial_hr_display);
                int result = intent.getIntExtra(SensorService.EXTRA_HR, 0);
                HeartRate.setText(Integer.toString(result));
                initialHeartRates.add(result);
            }
        }
    };
    ArrayList<Integer> activeHeartRates;
    private BroadcastReceiver activeBroadcastReceiver = new BroadcastReceiver() {
        // Receives broadcasts sent from other points of the app, like the SensorsDataService
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(SensorService.ACTION_HR)) {
                // Prints out the heart rate
                final TextView HeartRate = (TextView) findViewById(R.id.active_hr_display);
                int result = intent.getIntExtra(SensorService.EXTRA_HR, 0);
                HeartRate.setText(Integer.toString(result));
                activeHeartRates.add(result);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        itself = this;
        this.setFinishOnTouchOutside(false);

        initialHeartRates = new ArrayList<>();
        activeHeartRates = new ArrayList<>();
        // Note here that we DO NOT use setContentView();

        // Add your slide fragments here.
        // AppIntro will automatically generate the dots indicator and buttons.
        addSlide(AppIntroFragment.newInstance("Please give me access to your bluetooth device", "Long description why", stat_sys_data_bluetooth, getColor(R.color.colorPrimary)));
        addSlide(ChooseBluetoothSlide.newInstance(R.layout.choose_bluetooth_slide));
        addSlide(RestingHrSlide.newInstance(R.layout.resting_hr_slide));
        addSlide(ActiveHrSlide.newInstance(R.layout.active_hr_slide));

        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(getColor(R.color.colorPrimaryDark));
        setSeparatorColor(getColor(R.color.colorAccent));

        // Hide Skip/Done button.
        showSkipButton(false);
        setProgressButtonEnabled(true);
        setBackButtonVisibilityWithDone(true);
        setFadeAnimation();
        //askForPermissions(new String[]{Manifest.permission.BLUETOOTH}, 1);
        askForPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.angry_btn:
                MainActivity.itself.isInitialising = false;
                if (SensorService.itself == null) {
                    Log.d("Starting", "SensorsDataService");
                    Intent intent = new Intent(this, SensorService.class);
                    startService(intent);
                }
                Handler mHandler = new Handler();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        InitialMeasurement();
                    }
                };
                mHandler.postDelayed(runnable, 500);
                break;
            case R.id.active_button:
                MainActivity.itself.isInitialising = false;
                if (SensorService.itself == null) {
                    Log.d("Starting", "SensorsDataService");
                    Intent intent = new Intent(this, SensorService.class);
                    startService(intent);
                }
                mHandler = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        ActiveMeasurement();
                    }
                };
                mHandler.postDelayed(runnable, 500);
                break;
        }
    }

    private void RegisterBroadcastReceiver(int i) {
        // Register all the different types of broadcast intents the receiver should listen to.
        if (i == 0) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(SensorService.ACTION_BATTERY_STATUS);
            filter.addAction(SensorService.ACTION_HR);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            registerReceiver(initialBroadcastReceiver, filter);
        } else if (i == 1){
            IntentFilter filter = new IntentFilter();
            filter.addAction(SensorService.ACTION_BATTERY_STATUS);
            filter.addAction(SensorService.ACTION_HR);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            registerReceiver(activeBroadcastReceiver, filter);
        }
    }

    private void InitialMeasurement() {
        RegisterBroadcastReceiver(0);
        SensorService.itself.StartMeasuring();
        final Handler mHandler = new Handler();
        final int[] i = {0};
        Button button = (Button) findViewById(R.id.angry_btn);
        button.setEnabled(false);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                TextView title = (TextView) findViewById(R.id.initial_title);
                i[0]++;
                if (i[0] >= 309){
                    title.setText("Done!");
                    TextView hrValue = (TextView) findViewById(R.id.initial_hr_display);
                    hrValue.setText("");
                    UnregisterBroadcastReceiver(0);
                    new calculateHrValuesTask().execute();
                } else if (i[0] >= 299 && i[0] < 309){
                    title.setText("0:30");
                    mHandler.postDelayed(this, 100);
                } else {
                    title.setText("0:" + String.valueOf(i[0]/10));
                    mHandler.postDelayed(this, 100);
                }
            }
        };
        mHandler.postDelayed(runnable, 100);
    }

    private void ActiveMeasurement() {
        RegisterBroadcastReceiver(1);
        SensorService.itself.StartMeasuring();
        final Handler mHandler = new Handler();
        final int[] i = {0};
        Button button = (Button) findViewById(R.id.active_button);
        button.setEnabled(false);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                TextView title = (TextView) findViewById(R.id.active_title);
                i[0]++;
                if (i[0] >= 309){
                    title.setText("Done!");
                    TextView hrValue = (TextView) findViewById(R.id.active_hr_display);
                    hrValue.setText("");
                    UnregisterBroadcastReceiver(1);
                    new calculateActiveHrValuesTask().execute();
                } else if (i[0] >= 299 && i[0] < 309){
                    title.setText("0:30");
                    mHandler.postDelayed(this, 100);
                } else {
                    title.setText("0:" + String.valueOf(i[0]/10));
                    mHandler.postDelayed(this, 100);
                }
            }
        };
        mHandler.postDelayed(runnable, 100);
    }

    private void UnregisterBroadcastReceiver(int i) {
        if (i == 0) {
            unregisterReceiver(initialBroadcastReceiver);
        } else if (i == 1) {
            unregisterReceiver(activeBroadcastReceiver);
        }
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean("Introduced", true);
        edit.apply();
        finish();
        // Do something when users tap on Done button.
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }

    private class calculateHrValuesTask extends AsyncTask<Void, Void, Integer[]>{

        @Override
        protected Integer[] doInBackground(Void... params) {
            Integer[] HrValues = new Integer[3];
            Integer sum = 0;
            Integer max = 0;
            Integer min = 200;
            for (Integer i: initialHeartRates){
                sum += i;
                if (i>max){
                    max = i;
                }
                if (i<min){
                    min = i;
                }
            }
            HrValues[0]=sum/initialHeartRates.size();
            HrValues[1]=max;
            HrValues[2]=min;
            return HrValues;
        }

        protected void onPostExecute(Integer[] result){
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(itself);
            SharedPreferences.Editor edit = pref.edit();
            edit.putInt("CalmHeartRate", result[0]);
            TextView hrLabel = (TextView) findViewById(R.id.initial_hr_display);
            hrLabel.setText("Average: " + result[0] + "\nMaximum: " + result[1] + "\nMinimum: " + result[2]);
            RestingHrSlide.setPolicyRespected(true);
        }
    }

    private class calculateActiveHrValuesTask extends AsyncTask<Void, Void, Integer[]>{

        @Override
        protected Integer[] doInBackground(Void... params) {
            Integer[] HrValues = new Integer[3];
            Integer sum = 0;
            Integer max = 0;
            Integer min = 200;
            for (Integer i: activeHeartRates){
                sum += i;
                if (i>max){
                    max = i;
                }
                if (i<min){
                    min = i;
                }
            }
            HrValues[0]=sum/activeHeartRates.size();
            HrValues[1]=max;
            HrValues[2]=min;
            return HrValues;
        }

        protected void onPostExecute(Integer[] result){
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(itself);
            SharedPreferences.Editor edit = pref.edit();
            edit.putInt("ActiveHeartRate", result[0]);
            TextView hrLabel = (TextView) findViewById(R.id.active_hr_display);
            hrLabel.setText("Average: " + result[0] + "\nMaximum: " + result[1] + "\nMinimum: " + result[2]);
            ActiveHrSlide.setPolicyRespected(true);
        }
    }
}
