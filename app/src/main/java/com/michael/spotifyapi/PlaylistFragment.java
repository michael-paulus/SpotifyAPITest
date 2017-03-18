package com.michael.spotifyapi;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Playlist;

public class PlaylistFragment extends ListFragment {

    public static PlaylistFragment itself;
    private LayoutInflater mInflater;
    public PlaylistAdapter playlistAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PlaylistFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        mInflater = inflater;
        itself = this;

        playlistAdapter = new PlaylistAdapter();
        setListAdapter(playlistAdapter);
        return view;
    }

    public void notfiyDataSetChanged() {
        DbHelper mDbHelper = new DbHelper(getContext());
        ArrayList<SmallPlaylistRep> smallPlaylists = mDbHelper.queryForSmallPlaylistReps();
        for (SmallPlaylistRep smallPlaylist: smallPlaylists){
            playlistAdapter.addPlayList(smallPlaylist);
        }
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        SmallPlaylistRep smallPlaylist = playlistAdapter.getPlaylist(position);
        MainActivity.itself.playMusic(smallPlaylist.uri, MainActivity.TYPE_PLAYLIST);
    }

    static class SmallPlaylistRep {
        String name;
        String uri;
        ArrayList<String> tags;

        SmallPlaylistRep(String name, String uri, ArrayList<String> tags){
            this.name = name;
            this.uri = uri;
            this.tags = tags;
        }
    }

    private class PlaylistAdapter extends BaseAdapter {
        private ArrayList<SmallPlaylistRep> playlists;
        private LayoutInflater mInflator;

        public PlaylistAdapter() {
            super();
            playlists = new ArrayList<>();
            this.mInflator = mInflater;
        }

        public void addPlayList(SmallPlaylistRep smallPlaylist) {
            if (!playlists.contains(smallPlaylist)) {
                playlists.add(smallPlaylist);
                notifyDataSetChanged();
            }
        }

        public SmallPlaylistRep getPlaylist(int position) {
            return playlists.get(position);
        }

        public void clear() {
            playlists.clear();
        }

        @Override
        public int getCount() {
            return playlists.size();
        }

        @Override
        public Object getItem(int i) {
            return playlists.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            PlaylistFragment.ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.playlist_fragment_item, null);
                viewHolder = new PlaylistFragment.ViewHolder();
                viewHolder.playlistName = (TextView) view.findViewById(R.id.playlist_name);
                viewHolder.playlistTags = (TextView) view.findViewById(R.id.playlist_tags);
                view.setTag(viewHolder);
            } else {
                viewHolder = (PlaylistFragment.ViewHolder) view.getTag();
            }

            SmallPlaylistRep smallPlaylist = playlists.get(i);
            final String deviceName = smallPlaylist.name;
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.playlistName.setText(smallPlaylist.name);
            else
                viewHolder.playlistName.setText(R.string.unknown_playlist);
            viewHolder.playlistTags.setText(smallPlaylist.tags.toString());

            return view;
        }
    }

    static class ViewHolder {
        TextView playlistName;
        TextView playlistTags;
    }
}
