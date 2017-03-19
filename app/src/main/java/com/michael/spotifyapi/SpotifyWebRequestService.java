package com.michael.spotifyapi;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.client.Response;

public class SpotifyWebRequestService extends Service {
    public static SpotifyWebRequestService itself;
    private SpotifyService spotify;
    private DbHelper mDbHelper;

    public SpotifyWebRequestService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.d("SpotifyWebService", "Created");

        itself = this;
            SpotifyApi api = new SpotifyApi();

            // Most (but not all) of the Spotify Web API endpoints require authorisation.
            // If you know you'll only use the ones that don't require authorisation you can skip this step
            api.setAccessToken(PreferenceManager.getDefaultSharedPreferences(this).getString("SpotifyAccessToken", ""));

            spotify = api.getService();

        if (MainActivity.itself != null) {
            if (!MainActivity.itself.isInitialising) {
                mDbHelper = new DbHelper(this);
                getMyPlaylists();
            }
        }
    }

    public void getMyPlaylists() {
        try {
            spotify.getMyPlaylists(new SpotifyCallback<Pager<PlaylistSimple>>() {
                @Override
                public void failure(SpotifyError spotifyError) {
                    Log.d("getMyPlaylist", "failed");
                }

                @Override
                public void success(final Pager<PlaylistSimple> playlistSimplePager, Response response) {
                    mDbHelper = new DbHelper(SpotifyWebRequestService.itself);
                    mDbHelper.deletePlaylists();
                    spotify.getMe(new SpotifyCallback<UserPrivate>() {
                        @Override
                        public void failure(SpotifyError spotifyError) {
                            Log.d("getMe", "failed");
                        }

                        @Override
                        public void success(UserPrivate userPrivate, Response response) {
                            for (PlaylistSimple playlist : playlistSimplePager.items) {
                                spotify.getPlaylist(userPrivate.id, playlist.id, new SpotifyCallback<Playlist>() {
                                    @Override
                                    public void failure(SpotifyError spotifyError) {
                                        Log.d("getPlaylist", "failed");
                                    }

                                    @Override
                                    public void success(Playlist playlist, Response response) {
                                        mDbHelper.storePlaylist(playlist);
                                    }
                                });
                            }
                        }
                    });
                }
            });
        } catch (Exception e){
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    getMyPlaylists();
                }
            };
            Log.e("Error was", e.getMessage());
            Log.d("try", "failed");
            Handler handler = new Handler();
            handler.postDelayed(runnable, 5000);
        }
    }
}
