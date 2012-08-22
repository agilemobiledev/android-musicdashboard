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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.util.Log;

public class MusicFetcher {
	public static final String TAG = "MusicFetcher";
	// Graph API endpoint
	public static final String GRAPH_ENDPOINT = "https://graph.facebook.com";

	private static final String JSON_DATA = "data";
	private static final String JSON_URL = "url";
    private static final String JSON_IMAGE = "image";
    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_SITENAME = "site_name";
    private static final String JSON_MUSICIAN = "musician";
    private static final String JSON_NAME = "name";
    private static final String JSON_AUDIO = "audio";
    
	private Context context;
	
	public MusicFetcher(Context context) {
		this.context = context;
	}
	
	/*
	 * Method to get the contents of a URL.
	 */
	String getUrl(String urlSpec) throws IOException {
		URL url = new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		//Log.i(TAG, "getting URL: " + urlSpec);
		
		try {
			InputStream in = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			StringBuilder result = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				result.append(line + "\n");
			}
			return result.toString();
		} finally {
			connection.disconnect();
		}
	}
	
	/*
	 * Method to download the contents of a URL into a file. This is used
	 * to fetch a song's image.
	 */
	void downloadUrlToFilePath(String urlSpec, File file) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        FileOutputStream out = null;

        try {
            File tempFile = File.createTempFile("download", ".jpg", file.getParentFile());
            out = new FileOutputStream(tempFile);
            InputStream in = connection.getInputStream();

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            out = null;
            tempFile.renameTo(file);
        } finally {
            connection.disconnect();
            if (out != null) 
                out.close();
        }
    }

	/*
	 * Method to help clear all the files in the cache.
	 */
    public void clearCache() {
        for (String fileName : context.fileList()) {
            fileName = fileName.toLowerCase();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
                context.getFileStreamPath(fileName).delete();
            }
        }
    }
    
    /*
     * Method that initiates the song image download.
     */
    public void downloadSongImage(Song song) {
    	if (song.getImageUrl() == null) return;
    	try {
    		downloadUrlToFilePath(song.getImageUrl(), song.getLocalFile(context));
    	} catch (IOException e) {
    		Log.i(TAG, "Failed to download song image: " + song.getImageUrl(), e);
    	}
    }
    
    /*
     * Method to download the detailed information for
     * a song.
     */
    public void downloadSongInfo(Song song) {

    	try {
    		
    		// No need to download song info if it exists
    		if (song.getImageUrl() != null) return;
    		
    		// Make a Graph API call to the endpoint
    		// represented by the song ID.
    		String jsonString = 
        		getUrl(GRAPH_ENDPOINT + "/" + song.getId());
    		JSONObject json = new JSONObject(jsonString);
    		if (json != null) {
    			// Set the song object's instance variables
    			
    			// Song image URL
    			song.setImageUrl(json.getJSONArray(JSON_IMAGE)
    					.getJSONObject(0)
    					.getString(JSON_URL));
    			
    			// Song description
    			song.setDescription(json.getString(JSON_DESCRIPTION));
    			
    			// Song attribution, e.g. Spotify
    			song.setSiteName(json.getString(JSON_SITENAME));
    			
    			// Song's musician
    			song.setMusician(json.getJSONObject(JSON_DATA)
    					.getJSONArray(JSON_MUSICIAN)
    					.getJSONObject(0)
    					.getString(JSON_NAME));
    			
    			// Song audio link
    			song.setAudioUrl(json.getJSONArray(JSON_AUDIO)
    					.getJSONObject(0)
    					.getString(JSON_URL));
    			
    		}
    	} catch (IOException e) {
            Log.i(TAG, "Exception downloading song info JSON", e);
        } catch (JSONException e) {
            Log.i(TAG, "Exception parsing JSON", e);
        }
        
    }
    
    /*
     * Extract music listens info from a JSON string. The JSON string
     * is a result of a call to the me/music.listens Graph API GET call.  
     */
    public ArrayList<Song> fetchSongs(String jsonString) {
    	ArrayList<Song> songs = new ArrayList<Song>();	
    	try {
    		//Log.i(TAG, "received jsonString: " + jsonString);
    		
    		String jsonData = new JSONObject(jsonString).getString(JSON_DATA);
    		JSONTokener tokener = new JSONTokener(jsonData.toString());
    		JSONArray array = (JSONArray)tokener.nextValue();
    		for (int i = 0; i < array.length(); i++) {
                JSONObject musicListensJson = array.optJSONObject(i);
                if (musicListensJson != null) {
                	// Initialize a song object. The details can be
                	// populated in a subsequent fetch.
                	songs.add(new Song(musicListensJson));
                }
            }
    	} catch (JSONException e) {
            Log.i(TAG, "Exception parsing JSON", e);
        }
        
        return songs;
    }
      
}
