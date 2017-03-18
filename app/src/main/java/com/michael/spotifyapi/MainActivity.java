package com.michael.spotifyapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback, OnChartValueSelectedListener {

    public static final String CLIENT_ID = "3a4fb9c0247847a2aafa0ee68fc3043e";
    public static final String REDIRECT_URI = "michael-spotifyapi://callback";
    public static final int REQUEST_CODE = 1337;
    public static final String TYPE_PLAYLIST = "Playlist";
    public static MainActivity itself;

    private Player mPlayer;
    private String AccessToken;
    public boolean isInitialising = true;
    private LineChart mChart;
    private BroadcastReceiver activeBroadcastReceiver = new BroadcastReceiver() {
        // Receives broadcasts sent from other points of the app, like the SensorsDataService
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(SensorService.WAKE_UP)) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        playWakeUpMusic();
                    }
                };
                runOnUiThread(runnable);
            } else if (action.equals(SensorService.ACTION_HR)) {
                int i = intent.getIntExtra(SensorService.EXTRA_HR, 0);
                Log.d("Received heart rate", String.valueOf(i));
                addEntry(i);
            } else if (action.equals(SensorService.ACTION_HR_CONNECTED)) {
                mChart = (LineChart) findViewById(R.id.hr_chart);
                mChart.setOnChartValueSelectedListener(itself);

                // enable description text
                mChart.getDescription().setEnabled(false);

                // enable touch gestures
                //mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(false);
                mChart.setScaleEnabled(false);
                //mChart.setDrawGridBackground(false);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(false);

                // set an alternative background color
                //mChart.setBackgroundColor(Color.LTGRAY);

                LineData data = new LineData();
                data.setValueTextColor(Color.WHITE);

                // add empty data
                mChart.setData(data);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setEnabled(false);
                //l.setForm(Legend.LegendForm.LINE);
                //l.setTextColor(Color.WHITE);

                XAxis xl = mChart.getXAxis();
                xl.setTextColor(Color.WHITE);
                xl.setGranularity(1f);
                xl.setDrawGridLines(false);
                xl.setPosition(XAxis.XAxisPosition.BOTTOM);
                xl.setAvoidFirstLastClipping(true);
                xl.setEnabled(true);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.setTextColor(Color.WHITE);
                leftAxis.setAxisMinimum(40f);
                leftAxis.setDrawGridLines(true);

                YAxis rightAxis = mChart.getAxisRight();
                rightAxis.setEnabled(false);

            }
        }
    };

    public String getAccessToken() {
        return AccessToken;
    }

    private void addEntry(int i) {

        LineData data = mChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), (float) i), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(120);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(getColor(R.color.colorAccent));
        set.setLineWidth(2f);
        set.setCircleRadius(0f);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private void playWakeUpMusic() {
        playMusic("spotify:user:1154572061:playlist:6MPgJeqV7uSo8oIZCdRnGp", TYPE_PLAYLIST);
    }

    void playMusic(String s, String typePlaylist) {
        switch (typePlaylist) {
            case TYPE_PLAYLIST:
                if (mPlayer != null) {
                    mPlayer.playUri(null, s, 0, 0);
                    ImageView playButton = (ImageView) findViewById(R.id.play_button);
                    playButton.setImageDrawable(getDrawable(R.drawable.ic_pause_button));
                }
                break;
            default:
                if (mPlayer != null) {
                    mPlayer.playUri(null, s, 0, 0);
                    ImageView playButton = (ImageView) findViewById(R.id.play_button);
                    playButton.setImageDrawable(getDrawable(R.drawable.ic_pause_button));
                }
                break;
        }
    }

    private void resumePlayMusic() {
        if (mPlayer != null) {
            if (mPlayer.getMetadata().currentTrack != null) {
                mPlayer.resume(new Player.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("MainActivity", "Resumes playing");
                        ImageView playButton = (ImageView) findViewById(R.id.play_button);
                        playButton.setImageDrawable(getDrawable(R.drawable.ic_pause_button));
                    }

                    @Override
                    public void onError(Error error) {
                        playMusic("spotify:user:1154572061:playlist:6MPgJeqV7uSo8oIZCdRnGp", TYPE_PLAYLIST);
                    }
                });
            } else {
                playMusic("spotify:user:1154572061:playlist:6MPgJeqV7uSo8oIZCdRnGp", TYPE_PLAYLIST);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        itself = this;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        isInitialising = !pref.getBoolean("Introduced", false);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();
        registerMessageReceiver();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    private void registerMessageReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SensorService.WAKE_UP);
        filter.addAction(SensorService.ACTION_HR);
        filter.addAction(SensorService.ACTION_HR_CONNECTED);
        registerReceiver(activeBroadcastReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                this.AccessToken = response.getAccessToken();
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor edit = pref.edit();
                edit.putString("SpotifyAccessToken", AccessToken);
                edit.apply();
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            case kSpPlaybackNotifyTrackChanged:
                Metadata metadata = mPlayer.getMetadata();
                TextView songName = (TextView) findViewById(R.id.song_name);
                songName.setText(metadata.currentTrack.name);
                TextView artistName = (TextView) findViewById(R.id.artist_name);
                artistName.setText(metadata.currentTrack.artistName);
                try {
                    new DownloadImageTask().execute(new URL(metadata.currentTrack.albumCoverWebUrl));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                break;
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    private class DownloadImageTask extends AsyncTask<URL, Void, Bitmap> {
        ImageView bmImage;

        DownloadImageTask() {
            this.bmImage = (ImageView) findViewById(R.id.album_cover);
        }

        @Override
        protected Bitmap doInBackground(URL... params) {
            Bitmap mIcon11 = null;
            try {
                InputStream in = params[0].openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_button:
                if (mPlayer.getPlaybackState().isPlaying) {
                    mPlayer.pause(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            ImageView playButton = (ImageView) findViewById(R.id.play_button);
                            playButton.setImageDrawable(getDrawable(R.drawable.ic_action_name));
                        }

                        @Override
                        public void onError(Error error) {

                        }
                    });
                } else {
                    resumePlayMusic();
                }
                break;
            case R.id.skip_button:
                if (mPlayer.getPlaybackState().isPlaying) {
                    mPlayer.skipToNext(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Error error) {

                        }
                    });
                } else {
                    mPlayer.skipToNext(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            mPlayer.pause(new Player.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    ImageView playButton = (ImageView) findViewById(R.id.play_button);
                                    playButton.setImageDrawable(getDrawable(R.drawable.ic_action_name));
                                }

                                @Override
                                public void onError(Error error) {

                                }
                            });
                        }

                        @Override
                        public void onError(Error error) {

                        }
                    });
                }
                break;
            case R.id.go_back_button:
                if (mPlayer.getPlaybackState().isPlaying) {
                    mPlayer.skipToPrevious(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Error error) {
                            ImageView playButton = (ImageView) findViewById(R.id.play_button);
                            playButton.setImageDrawable(getDrawable(R.drawable.ic_action_name));
                        }
                    });
                } else {
                    mPlayer.skipToPrevious(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            mPlayer.pause(new Player.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    ImageView playButton = (ImageView) findViewById(R.id.play_button);
                                    playButton.setImageDrawable(getDrawable(R.drawable.ic_action_name));
                                }

                                @Override
                                public void onError(Error error) {

                                }
                            });
                        }

                        @Override
                        public void onError(Error error) {
                            ImageView playButton = (ImageView) findViewById(R.id.play_button);
                            playButton.setImageDrawable(getDrawable(R.drawable.ic_action_name));
                        }
                    });
                }
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");

        //Successfully set up a connection to Spotify, now able to play back tracks.
        //TODO: magic.
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!pref.getBoolean("Introduced", false)) {
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
        } else {
            if (SensorService.itself == null) {
                Log.d("Starting", "SensorsDataService");
                Intent intent = new Intent(this, SensorService.class);
                startService(intent);
            }
            if (SpotifyWebRequestService.itself == null) {
                Log.d("Starting", "SpotifyWebRequestService");
                Intent webIntent = new Intent(this, SpotifyWebRequestService.class);
                startService(webIntent);
            } else {
                SpotifyWebRequestService.itself.getMyPlaylists();
            }
        }

        Log.d("MainActivity", "AccessToken: " + AccessToken);
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.d("MainActivity", "Login failed");

    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.change_bluetooth:
                changeBluetooth();
                return true;
            case R.id.redo_intro:
                redoIntro();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void redoIntro() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = pref.edit();
        SensorService.itself.StopMeasuring();
        edit.putBoolean("Introduced", false);
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
    }

    private void changeBluetooth() {
        SensorService.itself.StopMeasuring();
        Intent intent = new Intent(this, ChangeBluetoothActivity.class);
        startActivity(intent);
    }
}