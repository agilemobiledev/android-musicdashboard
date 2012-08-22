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

import android.app.Application;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

public class MusicDashboardApplication extends Application {
	//private static final String TAG = "MusicDashboardApplication";
	
	// Holds variables that can be used throughout the app
	public static Facebook mFacebook;
    public static AsyncFacebookRunner mAsyncRunner;

}
