package com.hr.musicfm.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hr.musicfm.extractor.playlist.PlayListInfo;
import com.jakewharton.rxbinding2.view.RxView;
import com.hr.musicfm.ImageErrorLoadingListener;
import com.hr.musicfm.database.subscription.SubscriptionEntity;
import com.hr.musicfm.extractor.InfoItem;
import com.hr.musicfm.extractor.channel.ChannelInfo;
import com.hr.musicfm.fragments.BaseFragment;
import com.hr.musicfm.fragments.SubscriptionService;
import com.hr.musicfm.fragments.search.OnScrollBelowItemsListener;
import com.hr.musicfm.info_list.InfoItemBuilder;
import com.hr.musicfm.info_list.InfoListAdapter;
import com.hr.musicfm.util.AnimationUtils;
import com.hr.musicfm.util.Constants;
import com.hr.musicfm.util.NavigationHelper;
import com.hr.musicfm.workers.PlayListExtractorWorker;

import com.hr.musicfm.R;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.hr.musicfm.util.AnimationUtils.animateView;


public class BlankFragment extends BaseFragment implements PlayListExtractorWorker.OnPlayListInfoReceive {
    @Nullable
    private final String TAG = "BlankFragment@" + Integer.toHexString(hashCode());

    private static final String INFO_LIST_KEY = "info_list_key";
    private static final String PLAYLIST_INFO_KEY = "playlist_info_key";
    private static final String PAGE_NUMBER_KEY = "page_number_key";

    private InfoListAdapter infoListAdapter;

    private PlayListExtractorWorker currentPlaylistWorker;
    private PlayListInfo currentPlaylistInfo;
    private int serviceId = -1;
    private String playlistName = "";
    private String playlistUrl = "";
    private String feedUrl = "";
    private int pageNumber = 0;
    private boolean hasNextPage = true;

    /*//////////////////////////////////////////////////////////////////////////
    // Views
    //////////////////////////////////////////////////////////////////////////*/

    private RecyclerView resultRecyclerView;

    /*////////////////////////////////////////////////////////////////////////*/

    public BlankFragment() {
    }

    public static Fragment getInstance(int serviceId, String channelUrl, String name) {
        BlankFragment instance = new BlankFragment();
        instance.setChannel(serviceId, channelUrl, name);
        return instance;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Fragment's LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            playlistUrl = savedInstanceState.getString(Constants.KEY_URL);
            playlistName = savedInstanceState.getString(Constants.KEY_TITLE);
            serviceId = savedInstanceState.getInt(Constants.KEY_SERVICE_ID, -1);

            pageNumber = savedInstanceState.getInt(PAGE_NUMBER_KEY, 0);
            Serializable serializable = savedInstanceState.getSerializable(PLAYLIST_INFO_KEY);
            if (serializable instanceof PlayListInfo) currentPlaylistInfo = (PlayListInfo) serializable;
        }
        setChannel(0, "https://www.youtube.com/playlist?list=PLFgquLnL59alxIWnf4ivu5bjPeHSlsUe9", "jp");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "onCreateView() called with: inflater = [" + inflater + "], container = [" + container + "], savedInstanceState = [" + savedInstanceState + "]");
        return inflater.inflate(R.layout.fragment_blank, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (currentPlaylistInfo == null) loadPage(0);
        else handlePlaylistInfo(currentPlaylistInfo);
    }

    @Override
    public void onDestroyView() {
        if (DEBUG) Log.d(TAG, "onDestroyView() called");
        
        resultRecyclerView.removeAllViews();
        resultRecyclerView = null;

        super.onDestroyView();
    }

    @Override
    public void onResume() {
        if (DEBUG) Log.d(TAG, "onResume() called");
        super.onResume();
//        if (wasLoading.getAndSet(false) && (currentPlaylistWorker == null || !currentPlaylistWorker.isRunning())) {
//            loadPage(pageNumber);
//        }
    }

    @Override
    public void onStop() {
        if (DEBUG) Log.d(TAG, "onStop() called");
        super.onStop();
        wasLoading.set(currentPlaylistWorker != null && currentPlaylistWorker.isRunning());
        if (currentPlaylistWorker != null && currentPlaylistWorker.isRunning()) currentPlaylistWorker.cancel();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG) Log.d(TAG, "onSaveInstanceState() called with: outState = [" + outState + "]");
        super.onSaveInstanceState(outState);
        outState.putString(Constants.KEY_URL, playlistUrl);
        outState.putString(Constants.KEY_TITLE, playlistName);
        outState.putInt(Constants.KEY_SERVICE_ID, serviceId);

        outState.putSerializable(INFO_LIST_KEY, infoListAdapter.getItemsList());
        outState.putInt(PAGE_NUMBER_KEY, pageNumber);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init's
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        
        resultRecyclerView = ((RecyclerView) rootView.findViewById(R.id.playlist_view));
        resultRecyclerView.setLayoutManager(new LinearLayoutManager(activity));

        if (infoListAdapter == null) {
            infoListAdapter = new InfoListAdapter(getActivity());
            if (savedInstanceState != null) {
                //noinspection unchecked
                ArrayList<InfoItem> serializable = (ArrayList<InfoItem>) savedInstanceState.getSerializable(INFO_LIST_KEY);
                infoListAdapter.addInfoItemList(serializable);
            }

            infoListAdapter.setFooter(activity.getLayoutInflater().inflate(R.layout.pignate_footer, resultRecyclerView, false));
            infoListAdapter.showFooter(false);
            
        }

        resultRecyclerView.setAdapter(infoListAdapter);
    }

    @Override
    protected void initListeners() {
        super.initListeners();

        infoListAdapter.setOnStreamInfoItemSelectedListener(new InfoItemBuilder.OnInfoItemSelectedListener() {
            @Override
            public void selected(int serviceId, String url, String title) {
                if (DEBUG) Log.d(TAG, "selected() called with: serviceId = [" + serviceId + "], url = [" + url + "], title = [" + title + "]");
                NavigationHelper.openVideoDetailFragment(getParentFragment().getFragmentManager(), serviceId, url, title, false);
            }
        });

        resultRecyclerView.clearOnScrollListeners();
        resultRecyclerView.addOnScrollListener(new OnScrollBelowItemsListener() {
            @Override
            public void onScrolledDown(RecyclerView recyclerView) {
                if(hasNextPage && !isLoading.get()) {
                    pageNumber++;
                    recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            infoListAdapter.showFooter(true);
                        }
                    });
                    loadMoreVideos();
                }
            }
        });
    }

    @Override
    protected void reloadContent() {
        if (DEBUG) Log.d(TAG, "reloadContent() called");
        currentPlaylistInfo = null;
        infoListAdapter.clearStreamItemList();
        loadPage(0);
    }

    private void loadPage(int page) {
        if (DEBUG) Log.d(TAG, "loadPage() called with: page = [" + page + "]");
        if (currentPlaylistWorker != null && currentPlaylistWorker.isRunning()) currentPlaylistWorker.cancel();
        isLoading.set(true);
        pageNumber = page;
        infoListAdapter.showFooter(false);

        AnimationUtils.animateView(loadingProgressBar, true, 200);
        AnimationUtils.animateView(errorPanel, false, 200);

        currentPlaylistWorker = new PlayListExtractorWorker(activity, serviceId, playlistUrl, pageNumber, this);

        currentPlaylistWorker.start();
    }

    private void loadMoreVideos() {
        if (DEBUG) Log.d(TAG, "loadMoreVideos() called");
        if (currentPlaylistWorker != null && currentPlaylistWorker.isRunning()) currentPlaylistWorker.cancel();
        isLoading.set(true);
        currentPlaylistWorker = new PlayListExtractorWorker(activity, serviceId, playlistUrl, pageNumber, this);
        currentPlaylistWorker.start();
    }

    private void setChannel(int serviceId, String playlistUrl, String name) {
        this.serviceId = serviceId;
        this.playlistUrl = playlistUrl;
        this.playlistName = name;
    }

    private void handlePlaylistInfo(PlayListInfo info) {
        currentPlaylistInfo = info;

        AnimationUtils.animateView(errorPanel, false, 300);
        AnimationUtils.animateView(resultRecyclerView, true, 200);
        AnimationUtils.animateView(loadingProgressBar, false, 200);

        hasNextPage = info.hasNextPage;
        if (!hasNextPage) infoListAdapter.showFooter(false);

        infoListAdapter.addInfoItemList(info.related_streams);
    }


    @Override
    protected void setErrorMessage(String message, boolean showRetryButton) {
        super.setErrorMessage(message, showRetryButton);
        resetFragment();
    }

    private void resetFragment() {
    	currentPlaylistInfo = null;

        if (infoListAdapter != null) 
        	infoListAdapter.clearStreamItemList();
    }


    /*//////////////////////////////////////////////////////////////////////////
    // OnPlaylistInfoReceiveListener
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onReceive(PlayListInfo info) {
        if (DEBUG) Log.d(TAG, "onReceive() called with: info = [" + info + "]");
        if (info == null || isRemoving() || !isVisible()) return;

        handlePlaylistInfo(info);

        isLoading.set(false);
    }

    @Override
    public void onError(int messageId) {
        if (DEBUG) Log.d(TAG, "onError() called with: messageId = [" + messageId + "]");
        setErrorMessage(getString(messageId), true);
    }

    @Override
    public void onUnrecoverableError(Exception exception) {
        if (DEBUG) Log.d(TAG, "onUnrecoverableError() called with: exception = [" + exception + "]");
        activity.finish();
    }
}
