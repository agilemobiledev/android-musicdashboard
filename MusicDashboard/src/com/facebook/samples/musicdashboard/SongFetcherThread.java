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

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public class SongFetcherThread extends HandlerThread {

	private static final int INFO_LOAD = 0;
	private static final int IMAGE_LOAD = 1;
	private static final int IMAGE_CLEAR = 2;
	
	Context context;
	MusicFetcher fetcher;
	Handler listenerHandler;
	SongImageDownloadListener listener;
	Handler handler;
	
	/*
	 * Callback listener for song changes such as an
	 * image update.
	 */
	public interface SongImageDownloadListener {
		public void onSongImageUpdated();
	}
	
	public SongFetcherThread(String name, Context context, Handler listenerHandler, SongImageDownloadListener listener) {
		super(name);
		
		this.context = context;
        this.listenerHandler = listenerHandler;
        this.listener = listener;
        fetcher = new MusicFetcher(context);
	}
	
	@Override
	protected void onLooperPrepared() {
		handler = new Handler(getLooper()) {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == INFO_LOAD) {
					// Fetch detailed song info
					Song song = (Song)msg.obj;
					fetcher.downloadSongInfo(song);
				} else if (msg.what == IMAGE_LOAD) {
					// Fetch the image for a song
					Song song = (Song)msg.obj;
					fetcher.downloadSongImage(song);
					song.getDrawable(context);
				} else if (msg.what == IMAGE_CLEAR) {
					// Clear the cache
					fetcher.clearCache();
				}
				
				if (listener != null && listenerHandler != null) {
					// Call the listener on the main thread.
                    listenerHandler.post(new Runnable() {
                        public void run() {
                            listener.onSongImageUpdated();
                        }
                    });
                }
			}
		};
	}
	
	/*
	 * Method to initiate the song details download.
	 */
	public void downloadSongInfo(Song song) {
        handler
            .obtainMessage(INFO_LOAD, song)
            .sendToTarget();
    }
	
	/*
	 * Method to initiate the song image download.
	 */
	public void downloadSongImage(Song song) {
        handler
            .obtainMessage(IMAGE_LOAD, song)
            .sendToTarget();
    }

	/*
	 * Method to initiate the clearing of the cache.
	 */
    public void clearSongImages() {
        handler.removeMessages(IMAGE_LOAD);
        handler
            .obtainMessage(IMAGE_CLEAR)
            .sendToTarget();
    }

}
