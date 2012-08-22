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

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class Song {
	
	public static final String TAG = "Song";
	
	private static final String JSON_DATA = "data";
	private static final String JSON_SONG = "song";
	private static final String JSON_ID = "id";
    private static final String JSON_URL = "url";
    private static final String JSON_TITLE = "title";

    
	private int resId = R.drawable.placeholder_song;
	private String id;
	private String url;
	private String title;
	private String imageUrl;
	private String description;
	private String siteName;
	private String musician;
	private String audioUrl;
	
	private Drawable drawable;
	
	public Song(int resId, String title) {
		this.resId = resId;
		this.title = title;
	}
	
	public Song(JSONObject json) throws JSONException {		
		JSONObject jsonSong = json.getJSONObject(JSON_DATA).getJSONObject(JSON_SONG);
		
		id = jsonSong.getString(JSON_ID);
		url = jsonSong.getString(JSON_URL);
		title = jsonSong.getString(JSON_TITLE);
	}
	
	public Drawable getDrawable(Context c) {
		// Return a cached value if it exists
        if (drawable != null)
            return drawable;

        File file = getLocalFile(c);

        // Cache the song image
        if (file != null && file.exists()) {
            drawable = new BitmapDrawable(c.getResources(), file.getPath());
            return drawable;
        } else {
            return c.getResources().getDrawable(resId);
        }
    }
	
	public File getLocalFile(Context c) {
        if (null == url) return null;

        String fileName = getId();
        return new File(c.getFilesDir() + "/" + fileName);
    }
	
	public String toString() {
		return getTitle();
	}

	public int getResId() {
		return resId;
	}

	public void setResId(int resId) {
		this.resId = resId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public String getMusician() {
		return musician;
	}

	public void setMusician(String musician) {
		this.musician = musician;
	}

	public String getAudioUrl() {
		return audioUrl;
	}

	public void setAudioUrl(String audioUrl) {
		this.audioUrl = audioUrl;
	}
	
}
