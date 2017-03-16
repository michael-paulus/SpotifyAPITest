package com.michael.spotifyapi;

import android.Manifest;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroBase;
import com.github.paolorotolo.appintro.AppIntroFragment;

import static android.R.drawable.stat_sys_data_bluetooth;

public class IntroActivity extends AppIntro {

    public static AppIntroBase itself;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        itself = this;

        // Note here that we DO NOT use setContentView();

        // Add your slide fragments here.
        // AppIntro will automatically generate the dots indicator and buttons.
        addSlide(AppIntroFragment.newInstance("Please give me access to your bluetooth device", "Long description why", stat_sys_data_bluetooth, getColor(R.color.colorPrimary)));
        addSlide(ChooseBluetoothSlide.newInstance(R.layout.choose_bluetooth_slide));
        addSlide(RestingHrSlide.newInstance(R.layout.resting_hr_slide));
        addSlide(RestingHrSlide.newInstance(R.layout.resting_hr_slide));

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

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finish();
        // Do something when users tap on Done button.
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }
}
