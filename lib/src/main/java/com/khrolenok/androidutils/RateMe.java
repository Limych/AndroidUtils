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
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Date;

/**
 * Created by Limych on 05.10.2015
 */
public class RateMe {

	private Context context;
	private SharedPreferences pm;

	private static RateMe rateMe;

	private long firstRunDate;
	private final int launchesNumber;
	private final int launchesDuration;
	private int minLaunchesNumberToRate = 20;
	private int minLaunchesDurationToRate = 14; // days

	private static final String PREF_FIRST_RUN_DATE = "com.khrolenok.androidutils.rateme$firstRun";
	private static final String PREF_LAUNCHES_NUMBER = "com.khrolenok.androidutils.rateme$launches";
	private static final String PREF_RATED = "com.khrolenok.androidutils.rateme$rated";

	private static final int MILLIS_IN_DAY = 86400000;

	public RateMe(Context context) {
		this.context = context;
		pm = PreferenceManager.getDefaultSharedPreferences(context);

		// Calculate number of launches
		launchesNumber = pm.getInt(PREF_LAUNCHES_NUMBER, 0) + 1;
		pm.edit().putInt(PREF_LAUNCHES_NUMBER, launchesNumber).apply();

		// Calculate duration of launches
		final long currentDate = System.currentTimeMillis() / MILLIS_IN_DAY;
		firstRunDate = pm.getLong(PREF_FIRST_RUN_DATE, 0);
		if( firstRunDate == 0 ){
			firstRunDate = currentDate;
			pm.edit().putLong(PREF_FIRST_RUN_DATE, firstRunDate).apply();
		}
		launchesDuration = (int) ( currentDate - firstRunDate );
	}

	public static synchronized RateMe getInstance(Context context) {
		if( rateMe == null ) rateMe = new RateMe(context);
		return rateMe;
	}

	public Date getFirstRunDate() {
		return new Date(firstRunDate * MILLIS_IN_DAY);
	}

	/**
	 * Get number of application launches.
	 *
	 * @return Number of launches
	 */
	public int getLaunchesNumber() {
		return launchesNumber;
	}

	/**
	 * Get duration in days from first application run.
	 *
	 * @return Duration in days from first run
	 */
	public int getLaunchesDuration() {
		return launchesDuration;
	}

	public boolean isRated() {
		return pm.getBoolean(PREF_RATED, false);
	}

	public void setRated(boolean rated) {
		pm.edit().putBoolean(PREF_RATED, rated).apply();
	}

	public void setMinLaunchesNumberToRate(int minLaunchesNumberToRate) {
		this.minLaunchesNumberToRate = minLaunchesNumberToRate;
	}

	public int getMinLaunchesNumberToRate() {
		return minLaunchesNumberToRate;
	}

	public void setMinLaunchesDurationToRate(int minLaunchesDurationToRate) {
		this.minLaunchesDurationToRate = minLaunchesDurationToRate;
	}

	public int getMinLaunchesDurationToRate() {
		return minLaunchesDurationToRate;
	}

	public void addMenuItemIfNeeded(Menu menu, int itemId) {
		addMenuItemIfNeeded(menu, Menu.NONE, itemId, Menu.NONE, R.drawable.ic_action_rate);
	}

	public void addMenuItemIfNeeded(Menu menu, int groupId, int itemId, int order, int iconRes) {
		if( launchesDuration < minLaunchesDurationToRate
				|| launchesNumber < minLaunchesNumberToRate ) return;

		menu.add(groupId, itemId, order, R.string.rateme_action).setIcon(iconRes)
				.setShowAsAction(( isRated()
						? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_IF_ROOM ));
	}

	/**
	 * Interface used to allow the creator of an app to run some code when
	 * in a RateMe dialog positive button is clicked.
	 */
	public interface OnRateMeProceedListener {
		/**
		 * This method will be invoked when a positive button in the RateMe dialog is clicked.
		 */
		void onRateMeProceed();
	}

	public void showRateMeDialog() {
		showRateMeDialog(AppStore.GOOGLE_PLAY);
	}

	public void showRateMeDialog(int market) {
		showRateMeDialog(market, null);
	}

	public void showRateMeDialog(final OnRateMeProceedListener listener) {
		showRateMeDialog(AppStore.GOOGLE_PLAY, listener);
	}

	public void showRateMeDialog(final int market, final OnRateMeProceedListener listener) {
		final String msg = context.getResources().getQuantityString(R.plurals.rateme_message, launchesNumber, launchesNumber);
		final AlertDialog.Builder builder = new AlertDialog.Builder(context)
				.setMessage(msg)
				.setCancelable(true)
				.setPositiveButton(R.string.rateme_proceed, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						setRated(true);
						if( listener != null ) listener.onRateMeProceed();
						AppStore.openStore(context, market);
					}
				})
				.setNeutralButton(R.string.rateme_later, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		builder.create().show();
	}
}
