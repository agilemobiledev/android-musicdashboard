/*
 * Copyright 2004 - Present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.samples.musicdashboard;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.samples.musicdashboard.SongFetcherThread.SongImageDownloadListener;

public class MusicGalleryFragment extends Fragment {

	public static final String TAG = "MusicGalleryFragment";
	private static final String JSON_NAME = "name";
	private static final String JSON_PICTURE = "picture";
	private static final String JSON_DATA = "data";
	private static final String JSON_URL = "url";
	
	private ArrayList<Song> songs;
	private GridView gridView;
	private ArrayAdapter<Song> adapter;
	private ImageView profileImageView;
	private TextView userNameTextView;
	
	private SongFetcherThread downloadThread;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		super.setRetainInstance(true);
		
		// We have a menu
		setHasOptionsMenu(true);
		
		// Get the music info
		fetchMusic();
	}
	
	void fetchMusic() {
		// Kill any current downloads
		if (downloadThread != null) {
			downloadThread.quit();
			downloadThread = null;
		}
		
		songs = new ArrayList<Song>();
		
		Context c = getActivity().getApplicationContext();
		final MusicFetcher fetcher = new MusicFetcher(c);
		
		// The handler for song image/info download
		Handler handler = new Handler();
		// The listener for song image/info download
		SongImageDownloadListener listener = new SongImageDownloadListener () {
			public void onSongImageUpdated() {
				if (null == adapter) return;
				// Notify the GridView that data has changed
				adapter.notifyDataSetChanged();
			}
		};
		
		// The thread to download song image info
		downloadThread = new SongFetcherThread("SongImage", c, handler, listener);
		downloadThread.start();
		
		final Handler mHandler = new Handler();
		// Facebook - Graph API request for the music.listens info
		MusicDashboardApplication.mAsyncRunner.request("me/music.listens", new BaseRequestListener() {

			@Override
			public void onComplete(final String response, Object state) {
				// Pass the JSON response and get the music info
				songs = fetcher.fetchSongs(response);
				mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                    	// Update the GridView with initial info that does
                    	// not include song image and other details that
                    	// are about to be downloaded
        				setAdapter(new SongAdapter(songs));
        				
        				// Clear the cache in preparation for image downloads
        				downloadThread.clearSongImages();
        				
        				// For each song that was parsed out in the "fetchSongs"
        				// call, get the song info and image
        				for (Song song : songs) {
        					downloadThread.downloadSongInfo(song);
        					downloadThread.downloadSongImage(song);
        				}
                    }
                });
				
			}
			
		});
	}
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup parent, Bundle savedInstance) {
		View v = inflater.inflate(R.layout.fragment_music_gallery, parent, false);
		
		// Setup logout button
		ImageButton logoutButton = (ImageButton)v.findViewById(R.id.button_logout);
		
		// Facebook - Logout call
		logoutButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				MusicDashboardApplication.mAsyncRunner.logout(getActivity(), new BaseRequestListener() {

					@Override
					public void onComplete(String response, Object state) {
						Log.i(TAG, "Logged out successfully");
						SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
						mPrefs.edit().putString("access_token", null).commit();
						mPrefs.edit().putLong("access_expires", -1).commit();;
						// Go to logged in activity
						Intent i = new Intent(getActivity(), LoginActivity.class);
						startActivity(i);						
					}
					
				});
				
			}
		});
		
		// Get personalization info - profile picture and name
		profileImageView = (ImageView)v.findViewById(R.id.profile_picture);
		userNameTextView = (TextView)v.findViewById(R.id.user_name);
		
		// Then make the request to get the actual personalization info
		requestUserData();
		
		// Get the music info
		gridView = (GridView)v.findViewById(R.id.music_gallery_gridView);
		setAdapter(new SongAdapter(songs));
		
		// For now, clicking on a song in the GridView displays the song's title.
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				SongAdapter adapter = (SongAdapter)parent.getAdapter();
				Song song = adapter.getItem(position);
				Toast.makeText(getActivity(), song.toString(), Toast.LENGTH_SHORT).show();
				
				// Play the song
				playSong(song.getAudioUrl());
				
			}
		});
		return v;
	}
	
	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.activity_music_gallery, menu);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				// User selects refresh, update song content
				fetchMusic();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	void setAdapter(ArrayAdapter<Song> adapter) {
		this.adapter = adapter;
		if (gridView != null) {
			gridView.setAdapter(adapter);
		}
	}
	
	/*
	 * Used to play a song
	 */
	private void playSong(String songUrl) {
		Uri uri = Uri.parse(songUrl);
		Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(launchBrowser);
	}
	
	/*
	 * Used by the GridView adapter to set the column width
	 */
	int getColumnWidth() {
        if (null == gridView) return 0;

        Resources r = getActivity().getResources();
        int horizontalSpacing = r.getDimensionPixelSize(R.dimen.gridview_horizontal_spacing);
        int numColumns = r.getInteger(R.integer.gridview_num_columns);

        int spacing = horizontalSpacing * (numColumns - 1);
        int padding = gridView.getListPaddingLeft() + gridView.getListPaddingRight();

        return (gridView.getWidth() - padding - spacing) / numColumns;
    }

	/*
	 * Used by the GridView adapter to set the column height for a row
	 */
	int getColumnHeight(Drawable drawable) {
        float aspectRatio = (float)drawable.getIntrinsicHeight() / drawable.getIntrinsicWidth();
        return (int)(getColumnWidth() * aspectRatio);
    }
	
	/*
	 * The GridView adapter
	 */
	private class SongAdapter extends ArrayAdapter<Song> {
		public SongAdapter(ArrayList<Song> songs) {
			super(getActivity(), 0, songs);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (null == view) {
				view = getActivity().getLayoutInflater().inflate(R.layout.gallery_song, parent, false);
			}
			
			ImageView songImageView = (ImageView)view.findViewById(R.id.gallery_song_photoImageView);
			Song song = getItem(position);
			Drawable drawable = song.getDrawable(getActivity());
			
			songImageView.setImageDrawable(drawable);
			
			int height = 0;
			int numColumns = getActivity().getResources().getInteger(R.integer.gridview_num_columns);
			int firstCell = position = position % numColumns;
			int nextRow = firstCell + numColumns;
			
			// find the highest height on our row
			for (int cell = firstCell; cell < nextRow && cell < getCount(); cell++) {
				Song cellSong = getItem(cell);
				Drawable cellDrawable = cellSong.getDrawable(getActivity());
				int cellHeight = getColumnHeight(cellDrawable);
				if (cellHeight > height) {
					height = cellHeight;
				}
			}
			
			songImageView.getLayoutParams().height = height;
			// notify songImageView that its layout params have changed
			songImageView.requestLayout();
			
			return view;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop any current downloads
		downloadThread.quit();
	}
	
	@Override
	public void onResume() {    
        super.onResume();
        if(MusicDashboardApplication.mFacebook.isSessionValid()) {
        	// Extend the access token if needed
        	MusicDashboardApplication.mFacebook.extendAccessTokenIfNeeded(getActivity(), null);
        } else {
        	// Go to logged in activity
			Intent i = new Intent(getActivity(), LoginActivity.class);
			startActivity(i);	
        }
    }
	
	/*
	 * Make a Graph API call to get personalization info
	 */
	void requestUserData() {
		Bundle params = new Bundle();
		params.putString("fields", "name,picture");

		final Handler handler = new Handler();
		// Facebook - Graph API call to get personalizatoin info
		MusicDashboardApplication.mAsyncRunner.request("me", params, new BaseRequestListener () {

			@Override
			public void onComplete(String response, Object state) {
				JSONObject jsonObject;
				try {
					Log.i(TAG, "User info response: "+response);
					jsonObject = new JSONObject(response);
					
					final String userName = jsonObject.getString(JSON_NAME);
					
					String pictureUrl = null;
					// An upcoming change in how picture data is returned means
					// we have to check two ways for picture URL info.
					try {
						pictureUrl = jsonObject.getJSONObject(JSON_PICTURE)
						.getJSONObject(JSON_DATA)
						.getString(JSON_URL);
					} catch (JSONException e) {
						// ignored
					}
					
					if (pictureUrl == null) {
						pictureUrl = jsonObject.getString(JSON_PICTURE);
					}
					
					final Drawable drawable = fetchImageFromUrl(getActivity(), pictureUrl, "profilepic.jpg");
			    	
					handler.post(new Runnable() {

						@Override
						public void run() {
							// Set the personalization UI info on the main thread
							userNameTextView.setText(userName);
							profileImageView.setImageDrawable(drawable);
						}
						
					});
				} catch (JSONException e) {
					Log.i(TAG, "Could not get user info");
				}
				
			}
			
		});
		
	}
	
	/*
	 * Method that fetches an URL and returns a Drawable
	 */
	private Drawable fetchImageFromUrl(Context ctx, String url, String saveFilename) {
		try {
			InputStream is = (InputStream) this.fetch(url);
			Drawable d = Drawable.createFromStream(is, "src");
			return d;
		} catch (MalformedURLException e) {
			Log.i(TAG, "Could not get fetch image.");
			return null;
		} catch (IOException e) {
			Log.i(TAG, "Could not get fetch image.");
			return null;
		}
	}

	private Object fetch(String address) throws MalformedURLException,IOException {
		URL url = new URL(address);
		Object content = url.getContent();
		return content;
	}
}
