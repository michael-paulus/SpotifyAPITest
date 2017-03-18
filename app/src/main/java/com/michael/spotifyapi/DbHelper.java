package com.michael.spotifyapi;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;

/**
 * Created by Michael on 18.03.2017.
 */

class DbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "Playlist.db";
    private static final String PLAYLIST_ADDED = "playlist added to db";

    DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PLAYLIST_TABLE);
        db.execSQL(SQL_CREATE_PLAYLIST_TAG_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_PLAYLIST_TABLE);
        db.execSQL(SQL_DELETE_PLAYLIST_TAG_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private static final String PLAYLIST_TABLE_NAME = "playlist_table";
    private static final String PLAYLIST__ID = "_id";
    private static final String PLAYLIST_NAME = "playlist_name";
    private static final String PLAYLIST_SPOTIFY_URI = "spotify_uri";
    private static final String SQL_CREATE_PLAYLIST_TABLE =
            "CREATE TABLE " + PLAYLIST_TABLE_NAME + " (" +
                    PLAYLIST__ID + " INTEGER PRIMARY KEY," +
                    PLAYLIST_NAME + " TEXT," +
                    PLAYLIST_SPOTIFY_URI + " TEXT)";
    private static final String PLAYLIST_TAG_TABLE_NAME = "playlist_tag_table";
    private static final String PLAYLIST_TAG_ID = "playlist_tag_id";
    private static final String TAG = "tag";
    private static final String SQL_CREATE_PLAYLIST_TAG_TABLE =
            "CREATE TABLE " + PLAYLIST_TAG_TABLE_NAME + " (" +
                    PLAYLIST_TAG_ID + " INTEGER PRIMARY KEY, " +
                    PLAYLIST__ID + " INTEGER , " +
                    TAG + " TEXT)";

    private static final String SQL_DELETE_PLAYLIST_TABLE =
            "DROP TABLE IF EXISTS " + PLAYLIST_TABLE_NAME;
    private static final String SQL_DELETE_PLAYLIST_TAG_TABLE =
            "DROP TABLE IF EXISTS " + PLAYLIST_TAG_TABLE_NAME;

    void storePlaylist(Playlist playlist) {
        if (playlist.name.contains("API")) {
            SQLiteDatabase db = getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(PLAYLIST_NAME, playlist.name);
            values.put(PLAYLIST_SPOTIFY_URI, playlist.uri);

            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insert(PLAYLIST_TABLE_NAME, null, values);

            ArrayList<String> playlistTags = new ArrayList<>(Arrays.asList(playlist.description.split(",")));
            Log.d("Tags", playlistTags.toString());
            for (String tag: playlistTags){
                ContentValues tagValues = new ContentValues();
                tagValues.put(PLAYLIST__ID, newRowId);
                tagValues.put(TAG, tag);

                long newTagRowId = db.insert(PLAYLIST_TAG_TABLE_NAME, null, tagValues);
            }
            Log.d("Songs", playlist.tracks.toString());
            PlaylistFragment.itself.notfiyDataSetChanged();
        }
    }

    void deletePlaylists() {
        SQLiteDatabase db = getWritableDatabase();
        String selection = "";
        String[] selectionArgs = {};
        int deletecount = db.delete(PLAYLIST_TABLE_NAME, selection, selectionArgs);
        Log.d("deleted", String.valueOf(deletecount) + " playlist(s)");
        deletecount = db.delete(PLAYLIST_TAG_TABLE_NAME, selection, selectionArgs);
        Log.d("deleted", String.valueOf(deletecount) + " tag(s)");
    }

    ArrayList<PlaylistFragment.SmallPlaylistRep> queryForSmallPlaylistReps() {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                PLAYLIST__ID,
                PLAYLIST_NAME,
                PLAYLIST_SPOTIFY_URI
        };

        String selection = "";
        String[] selectionArgs = { };

        String sortOrder =
                PLAYLIST__ID + " DESC";

        Cursor cursor = db.query(
                PLAYLIST_TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
        ArrayList<PlaylistFragment.SmallPlaylistRep> smallPlaylists = new ArrayList<>();

        while (cursor.moveToNext()){
            String[] subProjection = {
                    PLAYLIST__ID,
                    TAG
            };

            String subSelection = PLAYLIST__ID + " = ?";
            String[] subSelectionArgs = { cursor.getString(0) };


            String subSortOrder =
                    PLAYLIST__ID + " DESC";

            Cursor subCursor = db.query(
                    PLAYLIST_TAG_TABLE_NAME,                     // The table to query
                    subProjection,                               // The columns to return
                    subSelection,                                // The columns for the WHERE clause
                    subSelectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    subSortOrder                                 // The sort order
            );
            ArrayList<String> tags = new ArrayList<>();
            while (subCursor.moveToNext()){
                tags.add(subCursor.getString(1));
            }
            smallPlaylists.add(new PlaylistFragment.SmallPlaylistRep(cursor.getString(1), cursor.getString(2), tags));
        }

        return smallPlaylists;
    }
}
