package com.michael.spotifyapi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback, OnChartValueSelectedListener {

    public static final String CLIENT_ID = "3a4fb9c0247847a2aafa0ee68fc3043e";
    public static final String REDIRECT_URI = "michael-spotifyapi://callback";
    public static final int REQUEST_CODE = 1337;
    public static final String TYPE_PLAYLIST = "Playlist";
    public static MainActivity itself;
    ArrayList<Integer> lastFourtyHeartRateValues;

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
                lastFourtyHeartRateValues.add(i);
                if (lastFourtyHeartRateValues.size() > 40){
                    Log.d("Array size", String.valueOf(lastFourtyHeartRateValues.size()));
                    lastFourtyHeartRateValues.remove(0);
                }
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
        set.setCircleRadius(1f);
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

    void playMusic(String s, String type) {
        if (mPlayer.getMetadata().contextUri == null || !mPlayer.getMetadata().contextUri.equals(s)) {
            switch (type) {
                case TYPE_PLAYLIST:
                    if (mPlayer != null) {
                        mPlayer.playUri(null, s, 0, 0);
                        ImageView playButton = (ImageView) findViewById(R.id.play_button);
                        playButton.setImageDrawable(getDrawable(R.drawable.ic_pause_button));
                        PlaylistFragment.itself.setPlaylistGreen(s);
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
        } else if (!mPlayer.getPlaybackState().isPlaying) {
            resumePlayMusic();
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
        lastFourtyHeartRateValues = new ArrayList<>();

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
            case R.id.auto_determine_music:
                autoDetermineMusic("");
                break;
        }
    }

    private void autoDetermineMusic(String s) {
        if (s.equals("")) {
            long begin = System.currentTimeMillis(); // starting time in milliseconds
            long end = System.currentTimeMillis() + 30 * 60 * 1000;// ending time in milliseconds
            String[] proj =
                    new String[]{
                            CalendarContract.Events._ID,
                            CalendarContract.Events.DTSTART,
                            CalendarContract.Events.DTEND,
                            CalendarContract.Events.TITLE};
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            String mSelectionClause = "("+ CalendarContract.Events.DTSTART + " BETWEEN " + begin + " AND " + end
                    + " OR " + CalendarContract.Events.DTSTART + " < " + begin + " AND " + CalendarContract.Events.DTEND + " > " + begin
                    + ") AND " + CalendarContract.Events.CALENDAR_ID + " = " + pref.getLong(getString(R.string.calendar_id), 0);
            String[] mSelectionArgs = {};
            Cursor mCursor = getContentResolver().query(
                    CalendarContract.Events.CONTENT_URI,   // The content URI of the words table
                    proj,                        // The columns to return for each row
                    mSelectionClause,                   // Selection criteria
                    null,                     // Selection criteria
                    null);
            if (mCursor.getCount() > 0) {
                while (mCursor.moveToNext()){
                    Log.d("Found event", mCursor.getString(3));
                    determineSimilaritiesToPlaylistTags(mCursor.getString(3));
                }
            } else {
                Log.d("Found no", "event");
                determineSimilaritiesToPlaylistTags("relax");
            }
            mCursor.close();
        }
    }

    private class SimilarityPackage{
        ArrayList<String> tags;
        String title;
        SimilarityPackage(String title, ArrayList<String> tags){
            this.tags = tags;
            this.title = title;
        }
    }

    private void determineSimilaritiesToPlaylistTags(String string) {
        DbHelper mDbHelper = new DbHelper(this);
        ArrayList<String> tags = mDbHelper.queryForTags();
        new CalculateSimilarityTask().execute(new SimilarityPackage(string, tags));
    }

    private class CalculateSimilarityTask extends AsyncTask<SimilarityPackage, Void, Map<String, Float>> {

        CalculateSimilarityTask() {
        }

        @Override
        protected Map<String, Float> doInBackground(SimilarityPackage... params) {
            ArrayList<String> tags = params[0].tags;
            String title = params[0].title;
            String hrtag = getTagFromNeuralNetwork();
            // Instantiate the RequestQueue.
            String url ="http://swoogle.umbc.edu/SimService/GetSimilarity?operation=api&";
            final HashMap<String, Float> tagFitMeasures = new HashMap<>();
            for (final String tag: tags) {
                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url + "phrase1=" + title + "&phrase2=" + tag);
                try {
                    HttpResponse execute = client.execute(httpGet);
                    InputStream content = execute.getEntity().getContent();

                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                    String s = "";
                    while ((s = buffer.readLine()) != null) {
                        tagFitMeasures.put(tag, Float.valueOf(s));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (!hrtag.equals("")) {
                for (final String tag : tags) {
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(url + "phrase1=" + title + "&phrase2=" + tag);
                    try {
                        HttpResponse execute = client.execute(httpGet);
                        InputStream content = execute.getEntity().getContent();

                        BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                        String s = "";
                        while ((s = buffer.readLine()) != null) {
                            Float currentvalue = tagFitMeasures.get(tag);
                            Float score = (currentvalue + Float.valueOf(s));
                            score *= score;
                            tagFitMeasures.put(tag, score);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return tagFitMeasures;
        }

        private String getTagFromNeuralNetwork() {
            String result = "";
            Log.d("RNN", String.valueOf(lastFourtyHeartRateValues.size()));
            if (lastFourtyHeartRateValues.size() >= 40) {
                Log.d("RNN", "starting request from RNN");
                String uri = "http://81.169.137.80:33333/predict";
                JSONObject jsonData = new JSONObject();
                JSONArray jsonArray = new JSONArray();
                ArrayList<Integer> hrValues = lastFourtyHeartRateValues;
                Collections.reverse(hrValues);
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(itself);
                Integer calmHeartRate = pref.getInt("CalmHeartRate", 70);
                Integer activeHeartRate = pref.getInt("ActiveHeartRate", 130);
                for (Integer i: hrValues){
                    Integer value = ((i-calmHeartRate)*(150-65))/(activeHeartRate-calmHeartRate)+65;
                    jsonArray.put(value);
                }
                try {
                    jsonData.put("data", jsonArray);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String data = jsonData.toString();
                try {
                    URL object = new URL(uri);

                    HttpURLConnection con = (HttpURLConnection) object.openConnection();
                    con.setDoOutput(true);
                    con.setDoInput(true);
                    con.setRequestProperty("Content-Type", "application/json");
                    con.setRequestProperty("Accept", "application/json");
                    con.setRequestMethod("POST");
                    Log.d("RNN", data);

                    OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
                    wr.write(data);
                    wr.flush();

                    StringBuilder sb = new StringBuilder();
                    int HttpResult = con.getResponseCode();
                    if (HttpResult == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(con.getInputStream(), "utf-8"));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        br.close();
                        System.out.println("" + sb.toString());
                        result = sb.toString();
                        switch (result){
                            case "0":
                                return "calm";
                            case "1":
                                return "activating";
                            case "2":
                                return "power";
                            case "3":
                                return "cooling";
                        }
                        Log.d("RNN", sb.toString());
                    } else {
                        Log.d("RNN", con.getResponseMessage());
                    }

                } catch (Exception e) {
                    Log.d("RNN", e.getMessage());
                }
                return result;
            }
            else return "";
        }

        protected void onPostExecute(Map<String, Float> result) {
            Log.d("Map Values:", result.toString());
            rankPlaylists(result);
        }
    }

    private void rankPlaylists(Map<String, Float> result) {
        HashMap<Long, Float> playlistScores = new DbHelper(this).calculatePlaylistScore(result);
        Long bestPlaylistId = getHighestPlaylistId(playlistScores);
        String SpotifyUri = new DbHelper(this).getPlaylistUri(bestPlaylistId);
        playMusic(SpotifyUri, TYPE_PLAYLIST);
    }

    private Long getHighestPlaylistId(HashMap<Long, Float> playlistScores) {
        HashMap.Entry<Long, Float> maxEntry = null;
        for (HashMap.Entry<Long, Float> entry: playlistScores.entrySet()){
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0){
                maxEntry = entry;
            }
        }
        Log.d("Found best playlist", String.valueOf(maxEntry.getKey()));
        return maxEntry.getKey();
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
                Log.d("MainActivity", "fetching playlists");
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