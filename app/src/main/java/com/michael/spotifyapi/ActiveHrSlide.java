package com.michael.spotifyapi;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.paolorotolo.appintro.ISlidePolicy;

/**
 * Created by Michael on 16.03.2017.
 */

public class ActiveHrSlide extends Fragment implements ISlidePolicy{

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    public static Fragment itself;
    private int layoutResId;
    private static boolean policyRespected = false;

    public static ActiveHrSlide newInstance(int layoutResId) {
        ActiveHrSlide restingHrSlide = new ActiveHrSlide();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        restingHrSlide.setArguments(args);

        return restingHrSlide;
    }

    public static void setPolicyRespected(boolean policyRespected) {
        ActiveHrSlide.policyRespected = policyRespected;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        itself = this;

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(layoutResId, container, false);
    }

    @Override
    public boolean isPolicyRespected() {
        return policyRespected;
    }

    @Override
    public void onUserIllegallyRequestedNextPage(){
            Toast.makeText(getContext(), "Please take the active measurement",
    Toast.LENGTH_SHORT).show();
    }
}
