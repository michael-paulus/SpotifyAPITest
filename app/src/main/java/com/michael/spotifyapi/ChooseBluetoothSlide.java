package com.michael.spotifyapi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

public class ChooseBluetoothSlide extends ListFragment implements ISlidePolicy{

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private int layoutResId;
    private LayoutInflater inflater;
    private static final long SCAN_PERIOD = 10000;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };
    private boolean policyRespected;

    public static ChooseBluetoothSlide newInstance(int layoutResId) {
        ChooseBluetoothSlide restingHrSlide = new ChooseBluetoothSlide();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        restingHrSlide.setArguments(args);

        return restingHrSlide;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.inflater = inflater;

        try {
            mHandler = new Handler();

            // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
            // BluetoothAdapter through BluetoothManager.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();

        } catch (Exception ex) {
            ex.toString();
        }
        return inflater.inflate(layoutResId, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(getString(R.string.device_address), device.getAddress());
        Log.d("Device Address", device.getAddress());
        editor.putString(getString(R.string.device_name), device.getName());
        editor.apply();
        Log.d("Device Address", "has been stored");
        for (int i = 0; i < l.getChildCount(); i++){
            if (i == position){
                l.getChildAt(i).findViewById(R.id.checkbox).setBackgroundResource(radiobutton_on_background);
                TextView name = (TextView) l.getChildAt(i).findViewById(R.id.device_name);
                name.setTextColor(getContext().getColor(R.color.colorAccent));
                TextView address = (TextView) l.getChildAt(i).findViewById(R.id.device_address);
                address.setTextColor(getContext().getColor(R.color.colorAccent));
            } else {
                l.getChildAt(i).findViewById(R.id.checkbox).setBackgroundResource(radiobutton_off_background);
                TextView name = (TextView) l.getChildAt(i).findViewById(R.id.device_name);
                name.setTextColor(Color.WHITE);
                TextView address = (TextView) l.getChildAt(i).findViewById(R.id.device_address);
                address.setTextColor(Color.WHITE);
            }
        }

        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        setPolicyRespected(true);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = ChooseBluetoothSlide.this.inflater;
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    @Override
    public boolean isPolicyRespected() {
        return getPolicyRespected();
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        Toast.makeText(getContext(), "Please select a bluetooth hr monitor!",
                Toast.LENGTH_SHORT).show();

    }

    public void setPolicyRespected(boolean policyRespected) {
        this.policyRespected = policyRespected;
    }

    public boolean getPolicyRespected() {
        return policyRespected;
    }
}
