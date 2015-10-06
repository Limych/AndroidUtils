/*
 * Copyright (c) 2015 Andrey “Limych” Khrolenok
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
 *
 */

package com.khrolenok.androidutils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Random;

/**
 * Created by Limych on 05.10.2015
 */
public class AppUpdate {

	private static final int UPDATE_CHECK_PROBABILITY = 5;

	private static final String PREF_VERSION_IN_STORE = "com.khrolenok.androidutils.appupdate$versionInStore";

	private Context context;
	private static AppUpdate appUpdate;

	public AppUpdate(Context context) {
		this.context = context;
		if( new Random().nextInt(100) < UPDATE_CHECK_PROBABILITY ) check4NewVersion();
	}

	public static synchronized AppUpdate getInstance(Context context) {
		if( appUpdate == null ) appUpdate = new AppUpdate(context);
		return appUpdate;
	}

	private class CheckVersionTask extends AsyncTask<Void, Void, Void> {

		private final String packageName;

		public CheckVersionTask() {
			packageName = context.getPackageName();
		}

		@Override
		protected Void doInBackground(Void... params) {
			String version = null;
			try{
				version = Jsoup.connect("https://play.google.com/store/apps/details?id=" + packageName + "&hl=en")
						.timeout(30000)
						.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
						.referrer("http://www.google.com")
						.get()
						.select("div[itemprop=softwareVersion]")
						.first()
						.ownText();
			} catch( IOException ignored ){
			}
			if( version != null ){
				SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(context);
				pm.edit().putString(PREF_VERSION_IN_STORE, version).apply();
			}
			return null;
		}
	}

	public void check4NewVersion() {
		final CheckVersionTask checkVersionTask = new CheckVersionTask();
		checkVersionTask.execute();
	}

	@Nullable
	public String getAppVersionInStore() {
		SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(context);
		return pm.getString(PREF_VERSION_IN_STORE, null);
	}

	public boolean hasNewVersion() {
		final String version = getAppVersionInStore();
		if( version == null ) return false;

		final ComparableVersion versionInStore = new ComparableVersion(version);
		final ComparableVersion appVersion = new ComparableVersion(BuildConfig.VERSION_NAME);
		return ( appVersion.compareTo(versionInStore) >= 0 );
	}

	public void addMenuItemIfHasNewVersion(Menu menu, int itemId) {
		addMenuItemIfHasNewVersion(menu, Menu.NONE, itemId, Menu.NONE, R.drawable.ic_action_google_play);
	}

	public void addMenuItemIfHasNewVersion(Menu menu, int groupId, int itemId, int order, int iconRes) {
		if( !hasNewVersion() ) return;

		menu.add(groupId, itemId, order, R.string.appupdate_action).setIcon(iconRes)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}

	/**
	 * Interface used to allow the creator of an app to run some code when
	 * in a RateMe dialog positive button is clicked.
	 */
	public interface OnAppUpdateProceedListener {
		/**
		 * This method will be invoked when a positive button in the RateMe dialog is clicked.
		 */
		void onAppUpdateProceed();
	}

	public void showUpdateDialog() {
		showUpdateDialog(null);
	}

	public void showUpdateDialog(final OnAppUpdateProceedListener listener) {
		final int market = AppStore.GOOGLE_PLAY;
		final AlertDialog.Builder builder = new AlertDialog.Builder(context)
				.setMessage(R.string.appupdate_message)
				.setCancelable(true)
				.setPositiveButton(R.string.appupdate_install, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						if( listener != null ) listener.onAppUpdateProceed();
						AppStore.openStore(context, market);
					}
				})
				.setNeutralButton(R.string.appupdate_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		builder.create().show();
	}
}
