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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class LoginFragment extends Fragment {

	public static final String TAG = "LoginFragment";
	public static final String APP_ID = "219647298162247";
	
	
	private SharedPreferences mPrefs;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		// Instantiate the Facebook instance and store it in a class that extends Application
		MusicDashboardApplication.mFacebook = new Facebook(APP_ID);
		MusicDashboardApplication.mAsyncRunner = new AsyncFacebookRunner(MusicDashboardApplication.mFacebook);
		
		/*
         * Get existing access_token if any
         */
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String access_token = mPrefs.getString("access_token", null);
        long expires = mPrefs.getLong("access_expires", -1);
        if(access_token != null) {
        	MusicDashboardApplication.mFacebook.setAccessToken(access_token);
        }
        if(expires != -1) {
        	MusicDashboardApplication.mFacebook.setAccessExpires(expires);
        }
        
        // If the session is valid redirect MusicGalleryActivity, the logged in activity
        if(MusicDashboardApplication.mFacebook.isSessionValid()) {
        	Intent i = new Intent(getActivity(), MusicGalleryActivity.class);
			startActivity(i);
        }
        
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstance) {
		View v = inflater.inflate(R.layout.fragment_login, parent, false);
		
		// Create the login button and click handler
		ImageButton loginButton = (ImageButton)v.findViewById(R.id.button_login);
		
		// Facebook - Login call
		loginButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Login to Facebook and ask for the required permissions
				MusicDashboardApplication.mFacebook.authorize(getActivity(), 
						new String[] { "publish_actions", "user_actions.music", "friends_actions.music" }, 
						new DialogListener () {

					@Override
					public void onComplete(Bundle values) {
						//Log.i(TAG, "Logged in successfully");
						// Save session information in Shared Preferences
						mPrefs.edit().putString("access_token", MusicDashboardApplication.mFacebook.getAccessToken()).commit();
						mPrefs.edit().putLong("access_expires", MusicDashboardApplication.mFacebook.getAccessExpires()).commit();
						// Go to logged in activity
						Intent i = new Intent(getActivity(), MusicGalleryActivity.class);
						startActivity(i);
					}

					@Override
					public void onFacebookError(FacebookError e) {
						Log.i(TAG, "Log in Facebook error: " + e.getMessage());
						
					}

					@Override
					public void onError(DialogError e) {
						Log.i(TAG, "Log in dialog error: " + e.getMessage());
						
					}

					@Override
					public void onCancel() {
						Log.i(TAG, "Logged in canceled");
						
					}
					
				});
				
			}
		});
		
		return v;
	}
	
    public void onLoginResult(int requestCode, int resultCode, Intent data) {
    	// For Facebook Login to work, this callback should be implemented
    	MusicDashboardApplication.mFacebook.authorizeCallback(requestCode, resultCode, data);
    }
	
	@Override
	public void onResume() {    
        super.onResume();
        
        if(MusicDashboardApplication.mFacebook.isSessionValid()) {
        	// Extend the access token when the app starts
        	MusicDashboardApplication.mFacebook.extendAccessTokenIfNeeded(getActivity(), null);
        }
    }
}
