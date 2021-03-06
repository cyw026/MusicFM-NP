/*
 * Created by Christian Schabesberger on 02.08.16.
 * <p>
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * DownloadActivity.java is part of NewPipe.
 * <p>
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.hr.musicfm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.hr.musicfm.database.history.dao.HistoryDAO;
import com.hr.musicfm.database.history.dao.WatchHistoryDAO;
import com.hr.musicfm.database.history.model.HistoryEntry;
import com.hr.musicfm.extractor.StreamingService;
import com.hr.musicfm.extractor.stream_info.AudioStream;
import com.hr.musicfm.extractor.stream_info.VideoStream;
import com.hr.musicfm.history.HistoryActivity;
import com.hr.musicfm.util.Constants;
import com.hr.musicfm.database.AppDatabase;
import com.hr.musicfm.database.history.dao.SearchHistoryDAO;
import com.hr.musicfm.util.NavigationHelper;
import com.hr.musicfm.database.history.model.SearchHistoryEntry;
import com.hr.musicfm.database.history.model.WatchHistoryEntry;
import com.hr.musicfm.extractor.stream_info.StreamInfo;
import com.hr.musicfm.fragments.detail.VideoDetailFragment;
import com.hr.musicfm.fragments.search.SearchFragment;
import com.hr.musicfm.util.ThemeHelper;
import com.google.android.gms.ads.MobileAds;

import java.util.Date;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity implements
        VideoDetailFragment.OnVideoPlayListener,
        SearchFragment.OnSearchListener {
    public static final boolean DEBUG = false;
    private static final String TAG = "MainActivity";
    private WatchHistoryDAO watchHistoryDAO;
    private SearchHistoryDAO searchHistoryDAO;
    private SharedPreferences sharedPreferences;
    private PublishSubject<HistoryEntry> historyEntrySubject;

    //ad
    private InterstitialAd mInterstitialAd;

    /*//////////////////////////////////////////////////////////////////////////
    // Activity's LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG)
            Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        ThemeHelper.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportFragmentManager() != null && getSupportFragmentManager().getBackStackEntryCount() == 0) {
            initFragments();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle(R.string.app_name);

        AppDatabase database = NewPipeDatabase.getInstance(this);
        watchHistoryDAO = database.watchHistoryDAO();
        searchHistoryDAO = database.searchHistoryDAO();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        historyEntrySubject = PublishSubject.create();
        historyEntrySubject
                .observeOn(Schedulers.io())
                .subscribe(createHistoryEntryConsumer());

//        MobileAds.initialize(this, "ca-app-pub-5814663467390565~6175163138");
        // adMob
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-5814663467390565/8358937334");
//        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");

        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.i("Ads", "onAdLoaded");
                mInterstitialAd.show();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.i("Ads", "onAdFailedToLoad");
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when the ad is displayed.
                Log.i("Ads", "onAdOpened");
//                enterMainPage();
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
                Log.i("Ads", "onAdLeftApplication");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the interstitial ad is closed.
                Log.i("Ads", "onAdClosed");
            }
        });
    }

    @NonNull
    private Consumer<HistoryEntry> createHistoryEntryConsumer() {
        return new Consumer<HistoryEntry>() {
            @Override
            public void accept(HistoryEntry historyEntry) throws Exception {
                //noinspection unchecked
                HistoryDAO<HistoryEntry> historyDAO = (HistoryDAO<HistoryEntry>)
                        (historyEntry instanceof SearchHistoryEntry ? searchHistoryDAO : watchHistoryDAO);

                HistoryEntry latestEntry = historyDAO.getLatestEntry();
                if (historyEntry.hasEqualValues(latestEntry)) {
                    latestEntry.setCreationDate(historyEntry.getCreationDate());
                    historyDAO.update(latestEntry);
                } else {
                    historyDAO.insert(historyEntry);
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        watchHistoryDAO = null;
        searchHistoryDAO = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sharedPreferences.getBoolean(Constants.KEY_THEME_CHANGE, false)) {
            if (DEBUG) Log.d(TAG, "Theme has changed, recreating activity...");
            sharedPreferences.edit().putBoolean(Constants.KEY_THEME_CHANGE, false).apply();
            this.recreate();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "onNewIntent() called with: intent = [" + intent + "]");
        if (intent != null) {
            // Return if launched from a launcher (e.g. Nova Launcher, Pixel Launcher ...)
            // to not destroy the already created backstack
            String action = intent.getAction();
            if ((action != null && action.equals(Intent.ACTION_MAIN)) && intent.hasCategory(Intent.CATEGORY_LAUNCHER))
                return;
        }

        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (DEBUG) Log.d(TAG, "onBackPressed() called");

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        if (fragment instanceof VideoDetailFragment)
            if (((VideoDetailFragment) fragment).onActivityBackPressed()) return;


        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
//            finish();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        } else super.onBackPressed();


    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (DEBUG) Log.d(TAG, "onCreateOptionsMenu() called with: menu = [" + menu + "]");
        super.onCreateOptionsMenu(menu);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        if (!(fragment instanceof VideoDetailFragment)) {
            findViewById(R.id.toolbar).findViewById(R.id.toolbar_spinner).setVisibility(View.GONE);
        }

        if (!(fragment instanceof SearchFragment)) {
            findViewById(R.id.toolbar).findViewById(R.id.toolbar_search_container).setVisibility(View.GONE);

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_menu, menu);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
//            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.d(TAG, "onOptionsItemSelected() called with: item = [" + item + "]");
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home: {
                NavigationHelper.gotoMainFragment(getSupportFragmentManager());
                return true;
            }
            case R.id.action_settings: {
                NavigationHelper.openSettings(this);
                return true;
            }
            case R.id.action_about:
                NavigationHelper.openAbout(this);
                return true;
            case R.id.action_history:
                Intent intent = new Intent(this, HistoryActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init
    //////////////////////////////////////////////////////////////////////////*/

    private void initFragments() {
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_LINK_TYPE)) {
            handleIntent(getIntent());
        } else NavigationHelper.gotoMainFragment(getSupportFragmentManager());
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    private void handleIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "handleIntent() called with: intent = [" + intent + "]");

        if (intent.hasExtra(Constants.KEY_LINK_TYPE)) {
            String url = intent.getStringExtra(Constants.KEY_URL);
            int serviceId = intent.getIntExtra(Constants.KEY_SERVICE_ID, 0);
            String title = intent.getStringExtra(Constants.KEY_TITLE);
            switch (((StreamingService.LinkType) intent.getSerializableExtra(Constants.KEY_LINK_TYPE))) {
                case STREAM:
                    boolean autoPlay = intent.getBooleanExtra(VideoDetailFragment.AUTO_PLAY, false);
                    NavigationHelper.openVideoDetailFragment(getSupportFragmentManager(), serviceId, url, title, autoPlay);
                    break;
                case CHANNEL:
                    NavigationHelper.openChannelFragment(getSupportFragmentManager(), serviceId, url, title);
                    break;
            }
        } else if (intent.hasExtra(Constants.KEY_OPEN_SEARCH)) {
            String searchQuery = intent.getStringExtra(Constants.KEY_QUERY);
            if (searchQuery == null) searchQuery = "";
            int serviceId = intent.getIntExtra(Constants.KEY_SERVICE_ID, 0);
            NavigationHelper.openSearchFragment(getSupportFragmentManager(), serviceId, searchQuery);
        } else {
            NavigationHelper.gotoMainFragment(getSupportFragmentManager());
        }
    }


    private void addWatchHistoryEntry(StreamInfo streamInfo) {
        if (sharedPreferences.getBoolean(getString(R.string.enable_watch_history_key), true)) {
            WatchHistoryEntry entry = new WatchHistoryEntry(streamInfo);
            historyEntrySubject.onNext(entry);
        }
    }

    @Override
    public void onVideoPlayed(VideoStream videoStream, StreamInfo streamInfo) {
        addWatchHistoryEntry(streamInfo);
    }

    @Override
    public void onBackgroundPlayed(StreamInfo streamInfo, AudioStream audioStream) {
        addWatchHistoryEntry(streamInfo);
    }

    @Override
    public void onSearch(int serviceId, String query) {
        // Add search history entry
        if (sharedPreferences.getBoolean(getString(R.string.enable_search_history_key), true)) {
            SearchHistoryEntry searchHistoryEntry = new SearchHistoryEntry(new Date(), serviceId, query);
            historyEntrySubject.onNext(searchHistoryEntry);
        }
    }
}
