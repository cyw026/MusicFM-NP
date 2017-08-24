package com.hr.musicfm.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hr.musicfm.R;

public class BlankFragment extends BaseFragment implements PlayListExtractorWorker.OnPlayListInfoReceive {
    @Nullable
    private final String TAG = "BlankFragment@" + Integer.toHexString(hashCode());

    private static final String INFO_LIST_KEY = "info_list_key";
    private static final String CHANNEL_INFO_KEY = "channel_info_key";
    private static final String PAGE_NUMBER_KEY = "page_number_key";

    private InfoListAdapter infoListAdapter;

    private ChannelExtractorWorker currentChannelWorker;
    private PlaylistInfo currentPlaylistInfo;
    private int serviceId = -1;
    private String channelName = "";
    private String channelUrl = "";
    private String feedUrl = "";
    private int pageNumber = 0;
    private boolean hasNextPage = true;

    /*//////////////////////////////////////////////////////////////////////////
    // Views
    //////////////////////////////////////////////////////////////////////////*/

    private RecyclerView channelVideosList;

    private View headerRootLayout;
    private ImageView headerChannelBanner;
    private ImageView headerAvatarView;
    private TextView headerTitleView;
    private TextView headerSubscribersTextView;
    private Button headerSubscribeButton;

    /*////////////////////////////////////////////////////////////////////////*/

    public ChannelFragment() {
    }

    public static Fragment getInstance(int serviceId, String channelUrl, String name) {
        ChannelFragment instance = new ChannelFragment();
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
            channelUrl = savedInstanceState.getString(Constants.KEY_URL);
            channelName = savedInstanceState.getString(Constants.KEY_TITLE);
            serviceId = savedInstanceState.getInt(Constants.KEY_SERVICE_ID, -1);

            pageNumber = savedInstanceState.getInt(PAGE_NUMBER_KEY, 0);
            Serializable serializable = savedInstanceState.getSerializable(PLAYLIST_INFO_KEY);
            if (serializable instanceof PlaylistInfo) currentPlaylistInfo = (PlaylistInfo) serializable;
        }
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
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onResume() {
        if (DEBUG) Log.d(TAG, "onResume() called");
        super.onResume();
        if (wasLoading.getAndSet(false) && (currentChannelWorker == null || !currentChannelWorker.isRunning())) {
            loadPage(pageNumber);
        }
    }

    @Override
    public void onStop() {
        if (DEBUG) Log.d(TAG, "onStop() called");
        super.onStop();
        wasLoading.set(currentChannelWorker != null && currentChannelWorker.isRunning());
        if (currentChannelWorker != null && currentChannelWorker.isRunning()) currentChannelWorker.cancel();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG) Log.d(TAG, "onSaveInstanceState() called with: outState = [" + outState + "]");
        super.onSaveInstanceState(outState);
        outState.putString(Constants.KEY_URL, channelUrl);
        outState.putString(Constants.KEY_TITLE, channelName);
        outState.putInt(Constants.KEY_SERVICE_ID, serviceId);

        outState.putSerializable(INFO_LIST_KEY, infoListAdapter.getItemsList());
        outState.putSerializable(PLAYLIST_INFO_KEY, currentPlaylistInfo);
        outState.putInt(PAGE_NUMBER_KEY, pageNumber);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init's
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        
        resultRecyclerView = ((RecyclerView) rootView.findViewById(R.id.result_playlist_view));
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
                NavigationHelper.openVideoDetailFragment(getFragmentManager(), serviceId, url, title);
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
        if (currentChannelWorker != null && currentChannelWorker.isRunning()) currentChannelWorker.cancel();
        isLoading.set(true);
        pageNumber = page;
        infoListAdapter.showFooter(false);

        AnimationUtils.animateView(loadingProgressBar, true, 200);
        AnimationUtils.animateView(errorPanel, false, 200);

        imageLoader.cancelDisplayTask(headerChannelBanner);
        imageLoader.cancelDisplayTask(headerAvatarView);

        headerSubscribeButton.setVisibility(View.GONE);
        headerSubscribersTextView.setVisibility(View.GONE);

        headerTitleView.setText(channelName != null ? channelName : "");
        headerChannelBanner.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.channel_banner));
        headerAvatarView.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.buddy));
        if (activity.getSupportActionBar() != null) activity.getSupportActionBar().setTitle(channelName != null ? channelName : "");

        currentChannelWorker = new ChannelExtractorWorker(activity, serviceId, channelUrl, page, false, this);
        currentChannelWorker.start();
    }

    private void loadMoreVideos() {
        if (DEBUG) Log.d(TAG, "loadMoreVideos() called");
        if (currentChannelWorker != null && currentChannelWorker.isRunning()) currentChannelWorker.cancel();
        isLoading.set(true);
        currentChannelWorker = new ChannelExtractorWorker(activity, serviceId, channelUrl, pageNumber, true, this);
        currentChannelWorker.start();
    }

    private void setChannel(int serviceId, String channelUrl, String name) {
        this.serviceId = serviceId;
        this.channelUrl = channelUrl;
        this.channelName = name;
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

        currentPlaylistInfo = info;

        AnimationUtils.animateView(errorPanel, false, 300);
        AnimationUtils.animateView(channelVideosList, true, 200);
        AnimationUtils.animateView(loadingProgressBar, false, 200);

        hasNextPage = info.hasNextPage;
        if (!hasNextPage) infoListAdapter.showFooter(false);

        if (addVideos) infoListAdapter.addInfoItemList(info.related_streams);

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
