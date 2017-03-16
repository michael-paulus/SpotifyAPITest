package com.michael.spotifyapi;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.os.BatteryManager.EXTRA_STATUS;

public class SensorService extends Service {
    public SensorService() {
    }

    public static final String
            ACTION_BATTERY_STATUS = SensorService.class.getName() + "BatteryStatus",
            ACTION_HR = SensorService.class.getName() + "HeartRate",
            EXTRA_STATUS = "extra_status",
            EXTRA_HR = "extra_hr",
            TAG = "MainActivity",
            CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private static final UUID UUID_HRS =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"),
            UUID_HRD =
                    UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"),
            Battery_Service_UUID =
                    UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb"),
            Battery_Level_UUID =
                    UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static SensorService itself;
    private final BluetoothGattCallback mGattCallback;
    String UserHRM = "";
    PowerManager.WakeLock wakeLock = null;
    BluetoothDevice hrmDevice = null;
    BluetoothGatt mBluetoothGatt = null;
    BluetoothGattService heartRateService = null;
    BluetoothGattCharacteristic heartRateCharacteristic = null;
    BluetoothGattService batteryLevelService = null;
    BluetoothGattCharacteristic batteryLevelCharacteristic = null;
    private SensorManager mSensorManager;
    private Sensor androidSensor;
    // map below allows to reduce amount of collected data
    private Map<Integer, Integer> recordedSensorTypes = new HashMap<Integer, Integer>();
    private BluetoothAdapter mBluetoothAdapter;
    private boolean isInitialising = true;
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            // already found the hrm
                            if (hrmDevice != null) {
                                return;
                            }

                            String name = device.getAddress();

                            if (name.equals(UserHRM)) {
                                hrmDevice = device;
                                connectDevice(device);
                            }

                        }
                    }).run();
                }
            };
    SensorEventListener sensorEventListener = new SensorEventListener() {

        /*
        Event listener that gets triggered by the android service, when a sensor value has changed
         */
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Don't unregister the Step Counter Sensor or it will lose all information.
            if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) {
                // I don't know what happens here. Have to ask Iaros.
                if (!recordedSensorTypes.containsKey(event.sensor.getType())) {
                    return;
                }
                recordedSensorTypes.remove(event.sensor.getType());
                mSensorManager.unregisterListener(sensorEventListener, event.sensor);
            }

            // If the sensor value is of dimension 1, it is a heart rate value, thus gets stored in that way.
            if (event.values.length == 1) {
                // Do something with the data
            }

            if (recordedSensorTypes.isEmpty()) {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private boolean hrmDisconnected;

    /*
        Out of the box stuff, that is needed to maintain or establish a bluetooth connection.
         */ {
        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                String intentAction;
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mBluetoothGatt.discoverServices();
                    showToast(getApplicationContext(), "HRM connected", Toast.LENGTH_SHORT);
                    hrmDisconnected = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    showToast(getApplicationContext(), "HRM disconnected", Toast.LENGTH_SHORT);
                    hrmDisconnected = true;
                    //mBluetoothAdapter.startLeScan(mLeScanCallback);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    heartRateService = gatt.getService(UUID_HRS);
                    batteryLevelService = gatt.getService(Battery_Service_UUID);

                    if (batteryLevelService != null) {
                        batteryLevelCharacteristic =
                                batteryLevelService.getCharacteristic(Battery_Level_UUID);
                    }


                    if (heartRateService != null) {

                        heartRateCharacteristic = heartRateService.getCharacteristic(UUID_HRD);
                        boolean res = gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
                        gatt.setCharacteristicNotification(heartRateCharacteristic, true);

                        try {
                            BluetoothGattDescriptor descriptor = heartRateCharacteristic.getDescriptor(
                                    UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));

                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                            mBluetoothGatt.writeDescriptor(descriptor);
                            showToast(getApplicationContext(), "Reading HRM", Toast.LENGTH_SHORT);
                        } catch (Exception ex) {
                            Log.e(TAG, "wuuuuut?");

                        }


                    }

                } else {
                    Log.w(TAG, "onServicesDiscovered received: " + status);
                }
            }


            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status) {

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    int BatteryStatus = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    sendBatteryStatus(BatteryStatus);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {

                if (recordedSensorTypes.containsKey(Sensor.TYPE_HEART_RATE)) {
                    recordedSensorTypes.remove(Sensor.TYPE_HEART_RATE);

                    int result = ReadHeartRateData(characteristic);

                    sendHR(result);

                    mBluetoothGatt.readCharacteristic(batteryLevelCharacteristic);
                } else return;

            }
        };
    }

    private void showToast(final Context context, final String message, final int length) {
        Activity mActivity = MainActivity.itself;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, length).show();
            }
        });
    }

    @Override
    public void onCreate() {

        Log.d("SensorsDataService", "Created");

        itself = this;

        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (!MainActivity.itself.isInitialising){
            StartMeasuring();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void connectDevice(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
    }

    // broadcasts the heart rate via intent
    private void sendHR(int result) {
        // Send a broadcast with the current HR
        Intent hrintent = new Intent(ACTION_HR);
        hrintent.putExtra(EXTRA_HR, result);
        sendBroadcast(hrintent);
    }

    // reads the heart rate from the bluetooth sensor's value
    public int ReadHeartRateData(BluetoothGattCharacteristic characteristic) {
        int flag = characteristic.getProperties();
        int format = -1;
        if ((flag & 0x01) != 0) {
            format = BluetoothGattCharacteristic.FORMAT_UINT16;
            Log.d(TAG, "Heart rate format UINT16.");
        } else {
            format = BluetoothGattCharacteristic.FORMAT_UINT8;
            Log.d(TAG, "Heart rate format UINT8.");
        }
        final int heartRate = characteristic.getIntValue(format, 1);
        return heartRate;

    }

    public void getHrmAddress() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        UserHRM = pref.getString(getString(R.string.device_address), "");
    }

    void StartMeasuring() {
        // Starts a new Measurement in the database.
        // as a secondary key for the rpe values and the record data

        getHrmAddress();

        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

    }

    // Stops measuring the heart rate
    void StopMeasuring() {

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        mBluetoothAdapter.stopLeScan(mLeScanCallback);

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            hrmDevice = null;
        }
    }

    // broadcasts the battery status via intent
    private void sendBatteryStatus(int Status) {
        // Send a broadcast with the battery status of the HRM
        Intent batteryintent = new Intent(ACTION_BATTERY_STATUS);
        batteryintent.putExtra(EXTRA_STATUS, Status);
        sendBroadcast(batteryintent);
    }
}
