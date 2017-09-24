package com.hr.musicfm.fragments.detail;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.hr.musicfm.App;
import com.hr.musicfm.player.BasePlayer;
import com.hr.musicfm.player.VideoPlayer;
import com.nirhart.parallaxscroll.views.ParallaxScrollView;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.hr.musicfm.ImageErrorLoadingListener;
import com.hr.musicfm.ReCaptchaActivity;
import com.hr.musicfm.download.DownloadDialog;
import com.hr.musicfm.extractor.InfoItem;
import com.hr.musicfm.extractor.MediaFormat;
import com.hr.musicfm.extractor.NewPipe;
import com.hr.musicfm.extractor.stream_info.AudioStream;
import com.hr.musicfm.extractor.stream_info.StreamInfo;
import com.hr.musicfm.extractor.stream_info.VideoStream;
import com.hr.musicfm.fragments.BaseFragment;
import com.hr.musicfm.info_list.InfoItemBuilder;
import com.hr.musicfm.player.MainVideoPlayer;
import com.hr.musicfm.player.PlayVideoActivity;
import com.hr.musicfm.player.PopupVideoPlayer;
import com.hr.musicfm.report.ErrorActivity;
import com.hr.musicfm.report.UserAction;
import com.hr.musicfm.util.AnimationUtils;
import com.hr.musicfm.util.Constants;
import com.hr.musicfm.util.Localization;
import com.hr.musicfm.util.NavigationHelper;
import com.hr.musicfm.util.PermissionHelper;
import com.hr.musicfm.util.Utils;

import com.hr.musicfm.R;
import com.hr.musicfm.workers.StreamExtractorWorker;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.ListGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.GSYBaseActivityDetail;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.model.GSYVideoModel;
import com.shuyu.gsyvideoplayer.video.ListGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.listener.StandardVideoAllCallBack;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.model.GSYVideoModel;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.ListGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;


import butterknife.BindView;
import butterknife.ButterKnife;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Stack;
import java.util.List;

import static com.hr.musicfm.util.AnimationUtils.animateView;

public class VideoDetailFragment extends BaseFragment implements StreamExtractorWorker.OnStreamInfoReceivedListener, SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener {
    private static final String TAG = ".VideoDetailFragment";

    // Amount of videos to show on start
    private static final int INITIAL_RELATED_VIDEOS = 8;

    private static final String KORE_PACKET = "org.xbmc.kore";
    private static final String STACK_KEY = "stack_key";
    private static final String INFO_KEY = "info_key";
    private static final String WAS_RELATED_EXPANDED_KEY = "was_related_expanded_key";

    public static final String AUTO_PLAY = "auto_play";

    private String thousand;
    private String million;
    private String billion;

    private ArrayList<VideoStream> sortedStreamVideosList;
    private ActionBarHandler actionBarHandler;

    private InfoItemBuilder infoItemBuilder = null;
    private StreamInfo currentStreamInfo = null;
    private StreamExtractorWorker curExtractorWorker;

    private String videoTitle;
    private String videoUrl;
    private int serviceId = -1;

    private static final int RELATED_STREAMS_UPDATE_FLAG = 0x1;
    private static final int RESOLUTIONS_MENU_UPDATE_FLAG = 0x2;
    private static final int TOOLBAR_ITEMS_UPDATE_FLAG = 0x4;
    private int updateFlags = 0;

    private boolean autoPlayEnabled;
    private boolean showRelatedStreams;
    private boolean wasRelatedStreamsExpanded = false;

    private Handler uiHandler;
    private Handler backgroundHandler;
    private HandlerThread backgroundHandlerThread;

    /*//////////////////////////////////////////////////////////////////////////
    // Views
    //////////////////////////////////////////////////////////////////////////*/

    private Spinner spinnerToolbar;

    private ParallaxScrollView parallaxScrollRootView;
    private LinearLayout contentRootLayoutHiding;

    private View thumbnailBackgroundButton;
    private ImageView thumbnailImageView;
    private ImageView thumbnailPlayButton;

    private View videoTitleRoot;
    private TextView videoTitleTextView;
    private ImageView videoTitleToggleArrow;
    private TextView videoCountView;

    private TextView detailControlsBackground;
    private TextView detailControlsPopup;

    private LinearLayout videoDescriptionRootLayout;
    private TextView videoUploadDateView;
    private TextView videoDescriptionView;

    private View uploaderRootLayout;
    private TextView uploaderTextView;
    private ImageView uploaderThumb;

    private TextView thumbsUpTextView;
    private ImageView thumbsUpImageView;
    private TextView thumbsDownTextView;
    private ImageView thumbsDownImageView;
    private TextView thumbsDisabledTextView;

    private TextView nextStreamTitle;
    private LinearLayout relatedStreamRootLayout;
    private LinearLayout relatedStreamsView;
    private ImageButton relatedStreamExpandButton;
    private OnVideoPlayListener onVideoPlayedListener;

    private boolean activityPaused;

    @BindView(R.id.detail_player)
    ListGSYVideoPlayer detailPlayer;

    protected boolean isPlay;

    protected boolean isPause;

    protected OrientationUtils orientationUtils;

    //ad
    private InterstitialAd mInterstitialAd;


    /*////////////////////////////////////////////////////////////////////////*/

    public static VideoDetailFragment getInstance(int serviceId, String url) {
        return getInstance(serviceId, url, "");
    }

    public static VideoDetailFragment getInstance(int serviceId, String videoUrl, String videoTitle) {
        VideoDetailFragment instance = getInstance();
        instance.selectVideo(serviceId, videoUrl, videoTitle);
        return instance;
    }

    public static VideoDetailFragment getInstance() {
        return new VideoDetailFragment();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Fragment's Lifecycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
        if (DEBUG) Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");

        if (savedInstanceState != null) {
            videoTitle = savedInstanceState.getString(Constants.KEY_TITLE);
            videoUrl = savedInstanceState.getString(Constants.KEY_URL);
            serviceId = savedInstanceState.getInt(Constants.KEY_SERVICE_ID, 0);
            wasRelatedStreamsExpanded = savedInstanceState.getBoolean(WAS_RELATED_EXPANDED_KEY, false);
            Serializable serializable = savedInstanceState.getSerializable(STACK_KEY);
            if (serializable instanceof Stack) {
                //noinspection unchecked
                Stack<StackItem> list = (Stack<StackItem>) serializable;
                stack.clear();
                stack.addAll(list);
            }

            Serializable serial = savedInstanceState.getSerializable(INFO_KEY);
            if (serial instanceof StreamInfo) currentStreamInfo = (StreamInfo) serial;
        }

        showRelatedStreams = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(getString(R.string.show_next_video_key), true);
        PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(this);

        thousand = getString(com.hr.musicfm.R.string.short_thousand);
        million = getString(R.string.short_million);
        billion = getString(com.hr.musicfm.R.string.short_billion);

        if (uiHandler == null) {
            uiHandler = new Handler(Looper.getMainLooper(), new UICallback());
        }
        if (backgroundHandler == null) {
            HandlerThread handlerThread = new HandlerThread("VideoDetailFragment-BG");
            handlerThread.start();
            backgroundHandlerThread = handlerThread;
            backgroundHandler = new Handler(handlerThread.getLooper(), new BackgroundCallback(uiHandler, getContext()));
        }

        // adMob
        mInterstitialAd = new InterstitialAd(activity);
        mInterstitialAd.setAdUnitId("ca-app-pub-5814663467390565/4043146164");
        // MainVideoPlayer ad
//        mInterstitialAd.setAdUnitId("ca-app-pub-5814663467390565/9908605575");
        //test
//        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.i("Ads", "onAdLoaded");
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
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });

//        playerImpl = new VideoDetailFragment.VideoPlayerImpl();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "onCreateView() called with: inflater = [" + inflater + "], container = [" + container + "], savedInstanceState = [" + savedInstanceState + "]");
        View view = inflater.inflate(R.layout.fragment_video_detail, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        if (currentStreamInfo == null) selectAndLoadVideo(serviceId, videoUrl, videoTitle);
        else prepareAndLoad(currentStreamInfo, false);

        //String url = "http://baobab.wd jcdn.com/14564977406580.mp4";
//        List<GSYVideoModel> urls = new ArrayList<>();
//        urls.add(new GSYVideoModel("http://baobab.wdjcdn.com/14564977406580.mp4", "标题1"));
//        urls.add(new GSYVideoModel("http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f20.mp4", "标题2"));
//        urls.add(new GSYVideoModel("http://baobab.wdjcdn.com/14564977406580.mp4", "标题3"));
//        urls.add(new GSYVideoModel("http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f20.mp4", "标题4"));
        GSYVideoManager.instance().setVideoType(activity, 1);
//        detailPlayer.setUp(urls, true, 0);

        //增加封面
        ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(com.hr.musicfm.R.drawable.dummy_thumbnail_dark);
        detailPlayer.setThumbImageView(imageView);

        resolveNormalVideoUI();

        //外部辅助的旋转，帮助全屏
        orientationUtils = new OrientationUtils(activity, detailPlayer);
        //初始化不打开外部的旋转
        orientationUtils.setEnable(false);

        detailPlayer.setIsTouchWiget(true);
        //关闭自动旋转
        detailPlayer.setRotateViewAuto(false);
        detailPlayer.setLockLand(false);
        detailPlayer.setShowFullAnimation(false);
        detailPlayer.setNeedLockFull(true);

        detailPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //直接横屏
                orientationUtils.resolveByClick();

                //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                detailPlayer.startWindowFullscreen(activity, true, true);
            }
        });

        detailPlayer.setStandardVideoAllCallBack(new SampleListener() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                //开始播放了才能旋转和全屏
                orientationUtils.setEnable(true);
                isPlay = true;
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
            }

            @Override
            public void onClickStartError(String url, Object... objects) {
                super.onClickStartError(url, objects);
            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                super.onQuitFullscreen(url, objects);
                if (orientationUtils != null) {
                    orientationUtils.backToProtVideo();
                }
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                }
            }

            @Override
            public void onEnterFullscreen(String url, Object... objects) {
                super.onEnterFullscreen(url, objects);
                //隐藏调全屏对象的返回按键
                GSYVideoPlayer gsyVideoPlayer = (GSYVideoPlayer)objects[1];
                gsyVideoPlayer.getBackButton().setVisibility(View.GONE);
                gsyVideoPlayer.getTitleTextView().setVisibility(View.GONE);
            }
        });

        detailPlayer.setLockClickListener(new LockClickListener() {
            @Override
            public void onClick(View view, boolean lock) {
                if (orientationUtils != null) {
                    //配合下方的onConfigurationChanged
                    orientationUtils.setEnable(!lock);
                }
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onVideoPlayedListener = (OnVideoPlayListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onVideoPlayedListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Currently only used for enable/disable related videos
        // but can be extended for other live settings changes
        if (updateFlags != 0) {
            if (!isLoading.get() && currentStreamInfo != null) {
                if ((updateFlags & RELATED_STREAMS_UPDATE_FLAG) != 0) initRelatedVideos(currentStreamInfo);
                if ((updateFlags & RESOLUTIONS_MENU_UPDATE_FLAG) != 0) setupActionBarHandler(currentStreamInfo);
            }

            if ((updateFlags & TOOLBAR_ITEMS_UPDATE_FLAG) != 0 && actionBarHandler != null) actionBarHandler.updateItemsVisibility();
            updateFlags = 0;
        }

        // Check if it was loading when the activity was stopped/paused,
        // because when this happen, the curExtractorWorker is cancelled
        if (wasLoading.getAndSet(false)) selectAndLoadVideo(serviceId, videoUrl, videoTitle);

//        if (activityPaused) {
//            if (playerImpl.getPlayer() != null) {
//                playerImpl.play(true);
//            } else {
//                playerImpl.initPlayer();
//                playerImpl.getPlayPauseButton().setImageResource(com.hr.musicfm.R.drawable.ic_play_arrow_white);
//                playerImpl.play(false);
//                activityPaused = false;
//            }
//        }
        detailPlayer.onVideoResume();
        isPause = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        wasLoading.set(curExtractorWorker != null && curExtractorWorker.isRunning());
        if (curExtractorWorker != null && curExtractorWorker.isRunning()) curExtractorWorker.cancel();
        StreamInfoCache.getInstance().removeOldEntries();

        activityPaused = true;
//        detailPlayer.onVideoPause();
//        isPause = true;
//        if (playerImpl.getPlayer() != null) {
//            playerImpl.setVideoStartPos((int) playerImpl.getPlayer().getCurrentPosition());
//            playerImpl.destroyPlayer();
//        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (backgroundHandlerThread != null) {
            backgroundHandlerThread.quit();
        }
        backgroundHandlerThread = null;
        backgroundHandler = null;
        PreferenceManager.getDefaultSharedPreferences(activity).unregisterOnSharedPreferenceChangeListener(this);
//        if (playerImpl != null) playerImpl.destroy();
        GSYVideoPlayer.releaseAllVideos();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
    }

    @Override
    public void onDestroyView() {
        if (DEBUG) Log.d(TAG, "onDestroyView() called");
//        thumbnailImageView.setImageBitmap(null);
        relatedStreamsView.removeAllViews();
        spinnerToolbar.setOnItemSelectedListener(null);

        spinnerToolbar = null;

        parallaxScrollRootView = null;
        contentRootLayoutHiding = null;

        thumbnailBackgroundButton = null;
        thumbnailImageView = null;
        thumbnailPlayButton = null;

        videoTitleRoot = null;
        videoTitleTextView = null;
        videoTitleToggleArrow = null;
        videoCountView = null;

        detailControlsBackground = null;
        detailControlsPopup = null;

        videoDescriptionRootLayout = null;
        videoUploadDateView = null;
        videoDescriptionView = null;

        uploaderRootLayout = null;
        uploaderTextView = null;
        uploaderThumb = null;

        thumbsUpTextView = null;
        thumbsUpImageView = null;
        thumbsDownTextView = null;
        thumbsDownImageView = null;
        thumbsDisabledTextView = null;

        nextStreamTitle = null;
        relatedStreamRootLayout = null;
        relatedStreamsView = null;
        relatedStreamExpandButton = null;

        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG) Log.d(TAG, "onSaveInstanceState() called with: outState = [" + outState + "]");
        outState.putString(Constants.KEY_URL, videoUrl);
        outState.putString(Constants.KEY_TITLE, videoTitle);
        outState.putInt(Constants.KEY_SERVICE_ID, serviceId);
        outState.putSerializable(STACK_KEY, stack);

        int nextCount = currentStreamInfo != null && currentStreamInfo.next_video != null ? 2 : 0;
        if (relatedStreamsView != null && relatedStreamsView.getChildCount() > INITIAL_RELATED_VIDEOS + nextCount) {
            outState.putSerializable(WAS_RELATED_EXPANDED_KEY, true);
        }

        if (!isLoading.get() && (curExtractorWorker == null || !curExtractorWorker.isRunning())) {
            outState.putSerializable(INFO_KEY, currentStreamInfo);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ReCaptchaActivity.RECAPTCHA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    NavigationHelper.openVideoDetailFragment(getFragmentManager(), serviceId, videoUrl, videoTitle);
                } else Log.e(TAG, "ReCaptcha failed");
                break;
            default:
                Log.e(TAG, "Request code from activity not supported [" + requestCode + "]");
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(com.hr.musicfm.R.string.show_next_video_key))) {
            showRelatedStreams = sharedPreferences.getBoolean(key, true);
            updateFlags |= RELATED_STREAMS_UPDATE_FLAG;
        } else if (key.equals(getString(com.hr.musicfm.R.string.preferred_video_format_key))
                || key.equals(getString(com.hr.musicfm.R.string.default_resolution_key))
                || key.equals(getString(R.string.show_higher_resolutions_key))
                || key.equals(getString(R.string.use_external_video_player_key))) {
            updateFlags |= RESOLUTIONS_MENU_UPDATE_FLAG;
        } else if (key.equals(getString(R.string.show_play_with_kodi_key))) {
            updateFlags |= TOOLBAR_ITEMS_UPDATE_FLAG;
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // OnClick
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onClick(View v) {
        if (isLoading.get() || currentStreamInfo == null) return;

        switch (v.getId()) {
            case R.id.detail_controls_background:
                openInBackground();
                break;
            case R.id.detail_controls_popup:
                openInPopup();
                break;
            case R.id.detail_uploader_root_layout:
                if (currentStreamInfo.channel_url == null || currentStreamInfo.channel_url.isEmpty()) {
                    Log.w(TAG, "Can't open channel because we got no channel URL");
                } else {
                    NavigationHelper.openChannelFragment(getFragmentManager(), currentStreamInfo.service_id, currentStreamInfo.channel_url, currentStreamInfo.uploader);
                }
                break;
//            case com.hr.musicfm.R.id.detail_thumbnail_root_layout:
//            case R.id.detail_thumbnail_play_button:
//                playVideo(currentStreamInfo);
//                break;
            case R.id.detail_title_root_layout:
                toggleTitleAndDescription();
                break;
            case R.id.detail_related_streams_expand:
                toggleExpandRelatedVideos(currentStreamInfo);
                break;
        }
    }

    @Override
    protected void reloadContent() {
        if (DEBUG) Log.d(TAG, "reloadContent() called");
        if (currentStreamInfo != null) StreamInfoCache.getInstance().removeInfo(currentStreamInfo);
        currentStreamInfo = null;
        for (StackItem stackItem : stack) if (stackItem.getUrl().equals(videoUrl)) stackItem.setInfo(null);
        prepareAndLoad(null, true);
    }

    private void openInBackground() {
        if (isLoading.get()) return;

        boolean useExternalAudioPlayer = PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(activity.getString(R.string.use_external_audio_player_key), false);
        Intent intent;
        AudioStream audioStream = currentStreamInfo.audio_streams.get(Utils.getPreferredAudioFormat(activity, currentStreamInfo.audio_streams));
        onVideoPlayedListener.onBackgroundPlayed(currentStreamInfo, audioStream);

        if (!useExternalAudioPlayer && android.os.Build.VERSION.SDK_INT >= 16) {
            activity.startService(NavigationHelper.getOpenBackgroundPlayerIntent(activity, currentStreamInfo, audioStream));
            Toast.makeText(activity, R.string.background_player_playing_toast, Toast.LENGTH_SHORT).show();
        } else {
            intent = new Intent();
            try {
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(audioStream.url),
                        MediaFormat.getMimeById(audioStream.format));
                intent.putExtra(Intent.EXTRA_TITLE, currentStreamInfo.title);
                intent.putExtra("title", currentStreamInfo.title);
                // HERE !!!
                activity.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.no_player_found)
                        .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(activity.getString(R.string.fdroid_vlc_url)));
                                activity.startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i(TAG, "You unlocked a secret unicorn.");
                            }
                        });
                builder.create().show();
                Log.e(TAG, "Either no Streaming player for audio was installed, or something important crashed:");
                e.printStackTrace();
            }
        }
    }

    private void openInPopup() {
        if (isLoading.get()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !PermissionHelper.checkSystemAlertWindowPermission(activity)) {
            Toast toast = Toast.makeText(activity, R.string.msg_popup_permission, Toast.LENGTH_LONG);
            TextView messageView = (TextView) toast.getView().findViewById(android.R.id.message);
            if (messageView != null) messageView.setGravity(Gravity.CENTER);
            toast.show();
            return;
        }

        onVideoPlayedListener.onVideoPlayed(getSelectedVideoStream(), currentStreamInfo);
        Toast.makeText(activity, R.string.popup_playing_toast, Toast.LENGTH_SHORT).show();
        Intent mIntent = NavigationHelper.getOpenVideoPlayerIntent(activity, PopupVideoPlayer.class, currentStreamInfo, actionBarHandler.getSelectedVideoStream());
        activity.startService(mIntent);
    }

    private void toggleTitleAndDescription() {
        if (videoDescriptionRootLayout.getVisibility() == View.VISIBLE) {
            videoTitleTextView.setMaxLines(1);
            videoDescriptionRootLayout.setVisibility(View.GONE);
            videoTitleToggleArrow.setImageResource(R.drawable.arrow_down);
        } else {
            videoTitleTextView.setMaxLines(10);
            videoDescriptionRootLayout.setVisibility(View.VISIBLE);
            videoTitleToggleArrow.setImageResource(R.drawable.arrow_up);
        }
    }

    private void toggleExpandRelatedVideos(StreamInfo info) {
        if (DEBUG) Log.d(TAG, "toggleExpandRelatedVideos() called with: info = [" + info + "]");
        if (!showRelatedStreams) return;

        int nextCount = info.next_video != null ? 2 : 0;
        int initialCount = INITIAL_RELATED_VIDEOS + nextCount;

        if (relatedStreamsView.getChildCount() > initialCount) {
            relatedStreamsView.removeViews(initialCount, relatedStreamsView.getChildCount() - (initialCount));
            relatedStreamExpandButton.setImageDrawable(ContextCompat.getDrawable(activity, getResourceIdFromAttr(com.hr.musicfm.R.attr.expand)));
            return;
        }

        //Log.d(TAG, "toggleExpandRelatedVideos() called with: info = [" + info + "], from = [" + INITIAL_RELATED_VIDEOS + "]");
        for (int i = INITIAL_RELATED_VIDEOS; i < info.related_streams.size(); i++) {
            InfoItem item = info.related_streams.get(i);
            //Log.d(TAG, "i = " + i);
            relatedStreamsView.addView(infoItemBuilder.buildView(relatedStreamsView, item));
        }
        relatedStreamExpandButton.setImageDrawable(ContextCompat.getDrawable(activity, getResourceIdFromAttr(R.attr.collapse)));
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init
    //////////////////////////////////////////////////////////////////////////*/

    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        spinnerToolbar = (Spinner) toolbar.findViewById(R.id.toolbar_spinner);

        parallaxScrollRootView = (ParallaxScrollView) rootView.findViewById(com.hr.musicfm.R.id.detail_main_content);

        thumbnailBackgroundButton = rootView.findViewById(R.id.detail_thumbnail_root_layout);
//        thumbnailImageView = (ImageView) rootView.findViewById(R.id.endScreen);
//        thumbnailPlayButton = (ImageView) rootView.findViewById(R.id.detail_thumbnail_play_button);
//        playerImpl.setup(rootView.findViewById(R.id.detail_thumbnail_root_layout));


        contentRootLayoutHiding = (LinearLayout) rootView.findViewById(com.hr.musicfm.R.id.detail_content_root_hiding);

        videoTitleRoot = rootView.findViewById(R.id.detail_title_root_layout);
        videoTitleTextView = (TextView) rootView.findViewById(R.id.detail_video_title_view);
        videoTitleToggleArrow = (ImageView) rootView.findViewById(R.id.detail_toggle_description_view);
        videoCountView = (TextView) rootView.findViewById(R.id.detail_view_count_view);

        detailControlsBackground = (TextView) rootView.findViewById(R.id.detail_controls_background);
        detailControlsPopup = (TextView) rootView.findViewById(R.id.detail_controls_popup);

        videoDescriptionRootLayout = (LinearLayout) rootView.findViewById(R.id.detail_description_root_layout);
        videoUploadDateView = (TextView) rootView.findViewById(com.hr.musicfm.R.id.detail_upload_date_view);
        videoDescriptionView = (TextView) rootView.findViewById(com.hr.musicfm.R.id.detail_description_view);

        //thumbsRootLayout = (LinearLayout) rootView.findViewById(R.id.detail_thumbs_root_layout);
        thumbsUpTextView = (TextView) rootView.findViewById(com.hr.musicfm.R.id.detail_thumbs_up_count_view);
        thumbsUpImageView = (ImageView) rootView.findViewById(R.id.detail_thumbs_up_img_view);
        thumbsDownTextView = (TextView) rootView.findViewById(com.hr.musicfm.R.id.detail_thumbs_down_count_view);
        thumbsDownImageView = (ImageView) rootView.findViewById(com.hr.musicfm.R.id.detail_thumbs_down_img_view);
        thumbsDisabledTextView = (TextView) rootView.findViewById(com.hr.musicfm.R.id.detail_thumbs_disabled_view);

        uploaderRootLayout = rootView.findViewById(com.hr.musicfm.R.id.detail_uploader_root_layout);
        uploaderTextView = (TextView) rootView.findViewById(R.id.detail_uploader_text_view);
        uploaderThumb = (ImageView) rootView.findViewById(com.hr.musicfm.R.id.detail_uploader_thumbnail_view);

        relatedStreamRootLayout = (LinearLayout) rootView.findViewById(R.id.detail_related_streams_root_layout);
        nextStreamTitle = (TextView) rootView.findViewById(R.id.detail_next_stream_title);
        relatedStreamsView = (LinearLayout) rootView.findViewById(R.id.detail_related_streams_view);

        relatedStreamExpandButton = ((ImageButton) rootView.findViewById(R.id.detail_related_streams_expand));

        actionBarHandler = new ActionBarHandler(activity);
        videoDescriptionView.setAutoLinkMask(Linkify.WEB_URLS);
        videoDescriptionView.setMovementMethod(LinkMovementMethod.getInstance());

        infoItemBuilder = new InfoItemBuilder(activity);

        setHeightThumbnail();
    }

    protected void initListeners() {
        super.initListeners();
        infoItemBuilder.setOnStreamInfoItemSelectedListener(new InfoItemBuilder.OnInfoItemSelectedListener() {
            @Override
            public void selected(int serviceId, String url, String title) {
                //NavigationHelper.openVideoDetail(activity, url, serviceId);
                selectAndLoadVideo(serviceId, url, title);
            }
        });

        videoTitleRoot.setOnClickListener(this);
        uploaderRootLayout.setOnClickListener(this);
        thumbnailBackgroundButton.setOnClickListener(this);
        detailControlsBackground.setOnClickListener(this);
        detailControlsPopup.setOnClickListener(this);
        relatedStreamExpandButton.setOnClickListener(this);
//        thumbnailPlayButton.setOnClickListener(this);
    }

    private void initThumbnailViews(StreamInfo info) {
        ImageView thumbImageView = (ImageView) detailPlayer.getThumbImageView();
        if (thumbImageView == null) {
            thumbImageView = new ImageView(activity);
            thumbImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbImageView.setImageResource(com.hr.musicfm.R.drawable.dummy_thumbnail_dark);
            detailPlayer.setThumbImageView(thumbImageView);
        }
        if (info.thumbnail_url != null && !info.thumbnail_url.isEmpty()) {
            imageLoader.displayImage(info.thumbnail_url, thumbImageView, displayImageOptions, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    ErrorActivity.reportError(activity, failReason.getCause(), null, activity.findViewById(android.R.id.content), ErrorActivity.ErrorInfo.make(UserAction.LOAD_IMAGE, NewPipe.getNameOfService(currentStreamInfo.service_id), imageUri, com.hr.musicfm.R.string.could_not_load_thumbnails));
                }
            });
        }
//        } else thumbnailImageView.setImageResource(com.hr.musicfm.R.drawable.dummy_thumbnail_dark);

        if (info.uploader_thumbnail_url != null && !info.uploader_thumbnail_url.isEmpty()) {
            imageLoader.displayImage(info.uploader_thumbnail_url, uploaderThumb, displayImageOptions,
                    new ImageErrorLoadingListener(activity, activity.findViewById(android.R.id.content), info.service_id));
        }
    }

    private void initRelatedVideos(StreamInfo info) {
        if (relatedStreamsView.getChildCount() > 0) relatedStreamsView.removeAllViews();

        if (info.next_video != null && showRelatedStreams) {
            nextStreamTitle.setVisibility(View.VISIBLE);
            relatedStreamsView.addView(infoItemBuilder.buildView(relatedStreamsView, info.next_video));
            relatedStreamsView.addView(getSeparatorView());
            relatedStreamRootLayout.setVisibility(View.VISIBLE);
        } else nextStreamTitle.setVisibility(View.GONE);

        if (info.related_streams != null && !info.related_streams.isEmpty() && showRelatedStreams) {
            //long first = System.nanoTime(), each;
            int to = info.related_streams.size() >= INITIAL_RELATED_VIDEOS ? INITIAL_RELATED_VIDEOS : info.related_streams.size();
            for (int i = 0; i < to; i++) {
                InfoItem item = info.related_streams.get(i);
                //each = System.nanoTime();
                relatedStreamsView.addView(infoItemBuilder.buildView(relatedStreamsView, item));
                //if (DEBUG) Log.d(TAG, "each took " + ((System.nanoTime() - each) / 1000000L) + "ms");
            }
            //if (DEBUG) Log.d(TAG, "Total time " + ((System.nanoTime() - first) / 1000000L) + "ms");

            relatedStreamRootLayout.setVisibility(View.VISIBLE);
            relatedStreamExpandButton.setVisibility(View.VISIBLE);

            relatedStreamExpandButton.setImageDrawable(ContextCompat.getDrawable(activity, getResourceIdFromAttr(com.hr.musicfm.R.attr.expand)));
        } else {
            if (info.next_video == null) relatedStreamRootLayout.setVisibility(View.GONE);
            relatedStreamExpandButton.setVisibility(View.GONE);
        }
    }

    private void initPlayList(StreamInfo info) {



        List<GSYVideoModel> urls = new ArrayList<>();
        urls.add(new GSYVideoModel(getSelectedVideoStream().url, info.title));


//        if (info.related_streams != null && !info.related_streams.isEmpty() && showRelatedStreams) {
//            //long first = System.nanoTime(), each;
//            int to = info.related_streams.size() >= INITIAL_RELATED_VIDEOS ? INITIAL_RELATED_VIDEOS : info.related_streams.size();
//            for (int i = 0; i < to; i++) {
//                InfoItem item = info.related_streams.get(i);
//
//                urls.add(new GSYVideoModel(getSelectedVideoStream().url, info.title));
//            }
//        }

        if (detailPlayer != null) {
            detailPlayer.setUp(urls, true, 0);
            if (autoPlayEnabled) {
                detailPlayer.startPlayLogic();
            }
        }
    }
    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        actionBarHandler.setupMenu(menu, inflater);
        ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return actionBarHandler.onItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void setupActionBarHandler(final StreamInfo info) {
        sortedStreamVideosList = Utils.getSortedStreamVideosList(activity, info.video_streams, info.video_only_streams, false);
        actionBarHandler.setupStreamList(sortedStreamVideosList, spinnerToolbar);
        actionBarHandler.setOnShareListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {
                if (isLoading.get()) return;

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, info.webpage_url);
                intent.setType("text/plain");
                startActivity(Intent.createChooser(intent, activity.getString(com.hr.musicfm.R.string.share_dialog_title)));
            }
        });

        actionBarHandler.setOnOpenInBrowserListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {
                if (isLoading.get()) return;

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(info.webpage_url));
                startActivity(Intent.createChooser(intent, activity.getString(R.string.choose_browser)));
            }
        });

        actionBarHandler.setOnPlayWithKodiListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {
                if (isLoading.get()) return;

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setPackage(KORE_PACKET);
                    intent.setData(Uri.parse(info.webpage_url.replace("https", "http")));
                    activity.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage(R.string.kore_not_found)
                            .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(activity.getString(R.string.fdroid_kore_url)));
                                    activity.startActivity(intent);
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    builder.create().show();
                }
            }
        });

        actionBarHandler.setOnDownloadListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {

                if (isLoading.get() || !PermissionHelper.checkStoragePermissions(activity)) {
                    return;
                }

                try {
                    DownloadDialog downloadDialog = DownloadDialog.newInstance(info, sortedStreamVideosList, selectedStreamId);
                    downloadDialog.show(activity.getSupportFragmentManager(), "downloadDialog");
                } catch (Exception e) {
                    Toast.makeText(activity, R.string.could_not_setup_download_menu, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
    }

    /*//////////////////////////////////////////////////////////////////////////
    // OwnStack
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Stack that contains the "navigation history".<br>
     * The peek is the current video.
     */
    private final Stack<StackItem> stack = new Stack<>();

    public void clearHistory() {
        stack.clear();
    }

    public void pushToStack(String videoUrl, String videoTitle) {
        if (DEBUG) Log.d(TAG, "pushToStack() called with: videoUrl = [" + videoUrl + "], videoTitle = [" + videoTitle + "]");
        if (stack.size() > 0 && stack.peek().getUrl().equals(videoUrl)) return;
        stack.push(new StackItem(videoUrl, videoTitle));
    }

    public void setTitleToUrl(String videoUrl, String videoTitle) {
        if (videoTitle != null && !videoTitle.isEmpty()) {
            for (StackItem stackItem : stack) {
                if (stackItem.getUrl().equals(videoUrl)) stackItem.setTitle(videoTitle);
            }
        }
    }

    public void setStreamInfoToUrl(String videoUrl, StreamInfo info) {
        if (info != null) {
            for (StackItem stackItem : stack) {
                if (stackItem.getUrl().equals(videoUrl)) stackItem.setInfo(info);
            }
        }
    }

    public boolean onActivityBackPressed() {
        if (DEBUG) Log.d(TAG, "onActivityBackPressed() called");

        if (orientationUtils != null) {
            orientationUtils.backToProtVideo();
        }
        if (StandardGSYVideoPlayer.backFromWindowFull(activity)) {
            return true;
        }

        // That means that we are on the start of the stack,
        // return false to let the MainActivity handle the onBack
        if (stack.size() == 1) return false;
        // Remove top
        stack.pop();
        // Get url from the new top
        StackItem peek = stack.peek();

        if (peek.getInfo() != null) {
            final StreamInfo streamInfo = peek.getInfo();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    selectAndHandleInfo(streamInfo);
                }
            });
        } else {
            selectAndLoadVideo(0, peek.getUrl(), !TextUtils.isEmpty(peek.getTitle()) ? peek.getTitle() : "");
        }

        return true;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    public void setAutoplay(boolean autoplay) {
        this.autoPlayEnabled = autoplay;
    }

    public void selectVideo(int serviceId, String videoUrl, String videoTitle) {
        this.videoUrl = videoUrl;
        this.videoTitle = videoTitle;
        this.serviceId = serviceId;
    }

    public void selectAndHandleInfo(StreamInfo info) {
        selectAndHandleInfo(info, true);
    }

    public void selectAndHandleInfo(StreamInfo info, boolean scrollToTop) {
        if (DEBUG) Log.d(TAG, "selectAndHandleInfo() called with: info = [" + info + "], scrollToTop = [" + scrollToTop + "]");
        selectVideo(info.service_id, info.webpage_url, info.title);
        prepareAndLoad(info, scrollToTop);
    }

    public void selectAndLoadVideo(int serviceId, String videoUrl, String videoTitle) {
        selectAndLoadVideo(serviceId, videoUrl, videoTitle, true);
    }

    public void selectAndLoadVideo(int serviceId, String videoUrl, String videoTitle, boolean scrollToTop) {
        if (DEBUG) {
            Log.d(TAG, "selectAndLoadVideo() called with: serviceId = [" + serviceId + "], videoUrl = [" + videoUrl + "], videoTitle = [" + videoTitle + "], scrollToTop = [" + scrollToTop + "]");
        }

        selectVideo(serviceId, videoUrl, videoTitle);
        prepareAndLoad(null, scrollToTop);
    }

    /**
     * Prepare the UI for loading the info.</br>
     * If the argument info is not null, it'll be passed in {@link #handleStreamInfo(StreamInfo, boolean)}.</br>
     * If it is, check if the cache contains the info already.</br>
     * If the cache doesn't have the info, load from the network.
     *
     * @param info        info to prepare and load, can be null
     * @param scrollToTop whether or not scroll the scrollView to y = 0
     */
    public void prepareAndLoad(StreamInfo info, boolean scrollToTop) {
        if (DEBUG) Log.d(TAG, "prepareAndLoad() called with: info = [" + info + "]");
        isLoading.set(true);

        // Only try to get from the cache if the passed info IS null
        if (info == null && StreamInfoCache.getInstance().hasKey(videoUrl)) {
            info = StreamInfoCache.getInstance().getFromKey(videoUrl);
        }

        if (info != null) selectVideo(info.service_id, info.webpage_url, info.title);
        pushToStack(videoUrl, videoTitle);

        if (curExtractorWorker != null && curExtractorWorker.isRunning()) curExtractorWorker.cancel();
        AnimationUtils.animateView(spinnerToolbar, false, 200);
        AnimationUtils.animateView(errorPanel, false, 200);

        videoTitleTextView.setText(videoTitle != null ? videoTitle : "");
        videoTitleTextView.setMaxLines(1);
        AnimationUtils.animateView(videoTitleTextView, true, 0);

        videoDescriptionRootLayout.setVisibility(View.GONE);
        videoTitleToggleArrow.setImageResource(R.drawable.arrow_down);
        videoTitleToggleArrow.setVisibility(View.GONE);
        videoTitleRoot.setClickable(false);

//        AnimationUtils.animateView(thumbnailPlayButton, false, 50);
//        imageLoader.cancelDisplayTask(thumbnailImageView);
        imageLoader.cancelDisplayTask(uploaderThumb);
//        thumbnailImageView.setImageBitmap(null);
        uploaderThumb.setImageBitmap(null);

        if (info != null) {
            final StreamInfo infoFinal = info;
            final boolean greaterThanThreshold = parallaxScrollRootView.getScrollY() >
                    (int) (getResources().getDisplayMetrics().heightPixels * .1f);

            if (scrollToTop) {
                if (greaterThanThreshold) parallaxScrollRootView.smoothScrollTo(0, 0);
                else parallaxScrollRootView.scrollTo(0, 0);
            }

            AnimationUtils.animateView(contentRootLayoutHiding, false, greaterThanThreshold ? 250 : 0, 0, new Runnable() {
                @Override
                public void run() {
                    handleStreamInfo(infoFinal, false);
                    isLoading.set(false);
                    showContentWithAnimation(greaterThanThreshold ? 120 : 200, 0, .02f);
                }
            });
        } else {
            if (scrollToTop) parallaxScrollRootView.smoothScrollTo(0, 0);
            curExtractorWorker = new StreamExtractorWorker(activity, serviceId, videoUrl, this);
            curExtractorWorker.start();
            AnimationUtils.animateView(loadingProgressBar, true, 200);
            AnimationUtils.animateView(contentRootLayoutHiding, false, 200);
        }

        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.");
        }
    }

    private void handleStreamInfo(@NonNull final StreamInfo info, boolean fromNetwork) {
        if (DEBUG) Log.d(TAG, "handleStreamInfo() called with: info = [" + info + "]");
        currentStreamInfo = info;
        selectVideo(info.service_id, info.webpage_url, info.title);

        loadingProgressBar.setVisibility(View.GONE);
//        AnimationUtils.animateView(thumbnailPlayButton, true, 200);

        // Since newpipe is designed to work even if certain information is not available,
        // the UI has to react on missing information.
        if (fromNetwork) videoTitleTextView.setText(info.title);

        if (!TextUtils.isEmpty(info.uploader)) uploaderTextView.setText(info.uploader);
        uploaderTextView.setVisibility(!TextUtils.isEmpty(info.uploader) ? View.VISIBLE : View.GONE);
        uploaderThumb.setImageDrawable(ContextCompat.getDrawable(activity, com.hr.musicfm.R.drawable.buddy));

        if (info.view_count >= 0) videoCountView.setText(Localization.localizeViewCount(info.view_count, activity));
        videoCountView.setVisibility(info.view_count >= 0 ? View.VISIBLE : View.GONE);

        if (info.dislike_count == -1 && info.like_count == -1) {
            thumbsDownImageView.setVisibility(View.VISIBLE);
            thumbsUpImageView.setVisibility(View.VISIBLE);
            thumbsUpTextView.setVisibility(View.GONE);
            thumbsDownTextView.setVisibility(View.GONE);

            thumbsDisabledTextView.setVisibility(View.VISIBLE);
        } else {
            thumbsDisabledTextView.setVisibility(View.GONE);

            if (info.dislike_count >= 0) thumbsDownTextView.setText(getShortCount((long) info.dislike_count));
            thumbsDownTextView.setVisibility(info.dislike_count >= 0 ? View.VISIBLE : View.GONE);
            thumbsDownImageView.setVisibility(info.dislike_count >= 0 ? View.VISIBLE : View.GONE);

            if (info.like_count >= 0) thumbsUpTextView.setText(getShortCount((long) info.like_count));
            thumbsUpTextView.setVisibility(info.like_count >= 0 ? View.VISIBLE : View.GONE);
            thumbsUpImageView.setVisibility(info.like_count >= 0 ? View.VISIBLE : View.GONE);
        }


        videoDescriptionView.setVisibility(View.GONE);
        videoDescriptionRootLayout.setVisibility(View.GONE);
        videoTitleToggleArrow.setImageResource(R.drawable.arrow_down);
        videoTitleToggleArrow.setVisibility(View.VISIBLE);
        videoTitleRoot.setClickable(true);

//        AnimationUtils.animateView(spinnerToolbar, true, 500);
        setupActionBarHandler(info);
        initThumbnailViews(info);
        initRelatedVideos(info);
        initPlayList(info);
        if (wasRelatedStreamsExpanded) {
            toggleExpandRelatedVideos(currentStreamInfo);
            wasRelatedStreamsExpanded = false;
        }
        setTitleToUrl(info.webpage_url, info.title);
        setStreamInfoToUrl(info.webpage_url, info);

        prepareDescription(info.description);
        prepareUploadDate(info.upload_date);
    }

    private void prepareUploadDate(final String uploadDate) {
        // Hide until date is prepared or forever if no date is supplied
        videoUploadDateView.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(uploadDate)) {
            backgroundHandler.sendMessage(Message.obtain(backgroundHandler, BackgroundCallback.MESSAGE_UPLOADER_DATE, uploadDate));
        }
    }

    private void prepareDescription(final String descriptionHtml) {
        // Send the unparsed description to the handler as a message
        if (!TextUtils.isEmpty(descriptionHtml)) {
            backgroundHandler.sendMessage(Message.obtain(backgroundHandler, BackgroundCallback.MESSAGE_DESCRIPTION, descriptionHtml));
        }
    }

    /**
     * Get the currently selected video stream
     * @return the selected video stream
     */
    private VideoStream getSelectedVideoStream() {
        return sortedStreamVideosList.get(actionBarHandler.getSelectedVideoStream());
    }

    public void playVideo(StreamInfo info) {
        // ----------- THE MAGIC MOMENT ---------------
        VideoStream selectedVideoStream = getSelectedVideoStream();

        if(onVideoPlayedListener != null) {
            onVideoPlayedListener.onVideoPlayed(selectedVideoStream, info);
        }

        if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(this.getString(com.hr.musicfm.R.string.use_external_video_player_key), false)) {

            // External Player
            Intent intent = new Intent();
            try {
                intent.setAction(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.parse(selectedVideoStream.url), MediaFormat.getMimeById(selectedVideoStream.format))
                        .putExtra(Intent.EXTRA_TITLE, info.title)
                        .putExtra("title", info.title);
                this.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.no_player_found)
                        .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent()
                                        .setAction(Intent.ACTION_VIEW)
                                        .setData(Uri.parse(getString(R.string.fdroid_vlc_url)));
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(com.hr.musicfm.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                builder.create().show();
            }
        } else {
            Intent mIntent;
            boolean useOldPlayer = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getBoolean(getString(R.string.use_old_player_key), false)
                    || (Build.VERSION.SDK_INT < 16);
            if (!useOldPlayer) {
                // ExoPlayer
                mIntent = activity.getIntent();
                mIntent.putExtra(BasePlayer.VIDEO_TITLE, info.title)
                        .putExtra(BasePlayer.VIDEO_URL, info.webpage_url)
                        .putExtra(BasePlayer.VIDEO_THUMBNAIL_URL, info.thumbnail_url)
                        .putExtra(BasePlayer.CHANNEL_NAME, info.uploader)
                        .putExtra(VideoPlayer.INDEX_SEL_VIDEO_STREAM, actionBarHandler.getSelectedVideoStream())
                        .putExtra(VideoPlayer.VIDEO_STREAMS_LIST, Utils.getSortedStreamVideosList(activity, info.video_streams, info.video_only_streams, false))
                        .putExtra(VideoPlayer.VIDEO_ONLY_AUDIO_STREAM, Utils.getHighestQualityAudio(info.audio_streams));
                if (info.start_position > 0) mIntent.putExtra(BasePlayer.START_POSITION, info.start_position * 1000);

//                playerImpl.handleIntent(mIntent);
//                mIntent = NavigationHelper.getOpenVideoPlayerIntent(activity, MainVideoPlayer.class, info, actionBarHandler.getSelectedVideoStream());
            } else {
                // Internal Player
                mIntent = new Intent(activity, PlayVideoActivity.class)
                        .putExtra(PlayVideoActivity.VIDEO_TITLE, info.title)
                        .putExtra(PlayVideoActivity.STREAM_URL, selectedVideoStream.url)
                        .putExtra(PlayVideoActivity.VIDEO_URL, info.webpage_url)
                        .putExtra(PlayVideoActivity.START_POSITION, info.start_position);
                startActivity(mIntent);
            }

        }
    }

    private View getSeparatorView() {
        View separator = new View(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        int m8 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        int m5 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        params.setMargins(m8, m5, m8, m5);
        separator.setLayoutParams(params);

        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(R.attr.separatorColor, typedValue, true);
        separator.setBackgroundColor(typedValue.data);

        return separator;
    }

    private void setHeightThumbnail() {
        boolean isPortrait = getResources().getDisplayMetrics().heightPixels > getResources().getDisplayMetrics().widthPixels;
        int height = isPortrait ? (int) (getResources().getDisplayMetrics().widthPixels / (16.0f / 9.0f))
                : (int) (getResources().getDisplayMetrics().heightPixels / 2f);

        thumbnailBackgroundButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
//        playerImpl.setMinimumHeight(height);

//        thumbnailImageView.setScaleType(isPortrait ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
//        thumbnailImageView.setLayoutParams(new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, height));
//        thumbnailImageView.setMinimumHeight(height);
    }

    public String getShortCount(Long viewCount) {
        if (viewCount >= 1000000000) {
            return Long.toString(viewCount / 1000000000) + billion;
        } else if (viewCount >= 1000000) {
            return Long.toString(viewCount / 1000000) + million;
        } else if (viewCount >= 1000) {
            return Long.toString(viewCount / 1000) + thousand;
        } else {
            return Long.toString(viewCount);
        }
    }

    private void showContentWithAnimation(long duration, long delay, @FloatRange(from = 0.0f, to = 1.0f) float translationPercent) {
        int translationY = (int) (getResources().getDisplayMetrics().heightPixels *
                (translationPercent > 0.0f ? translationPercent : .12f));

        contentRootLayoutHiding.animate().setListener(null).cancel();
        contentRootLayoutHiding.setAlpha(0f);
        contentRootLayoutHiding.setTranslationY(translationY);
        contentRootLayoutHiding.setVisibility(View.VISIBLE);
        contentRootLayoutHiding.animate().alpha(1f).translationY(0)
                .setStartDelay(delay).setDuration(duration).setInterpolator(new FastOutSlowInInterpolator()).start();

        uploaderRootLayout.animate().setListener(null).cancel();
        uploaderRootLayout.setAlpha(0f);
        uploaderRootLayout.setTranslationY(translationY);
        uploaderRootLayout.setVisibility(View.VISIBLE);
        uploaderRootLayout.animate().alpha(1f).translationY(0)
                .setStartDelay((long) (duration * .5f) + delay).setDuration(duration).setInterpolator(new FastOutSlowInInterpolator()).start();

        if (showRelatedStreams) {
            relatedStreamRootLayout.animate().setListener(null).cancel();
            relatedStreamRootLayout.setAlpha(0f);
            relatedStreamRootLayout.setTranslationY(translationY);
            relatedStreamRootLayout.setVisibility(View.VISIBLE);
            relatedStreamRootLayout.animate().alpha(1f).translationY(0)
                    .setStartDelay((long) (duration * .8f) + delay).setDuration(duration).setInterpolator(new FastOutSlowInInterpolator()).start();
        }
    }

    private void setErrorImage(final int imageResource) {
//        if (thumbnailImageView == null || activity == null) return;
//        thumbnailImageView.setImageDrawable(ContextCompat.getDrawable(activity, imageResource));
//        AnimationUtils.animateView(thumbnailImageView, false, 0, 0, new Runnable() {
//            @Override
//            public void run() {
//                AnimationUtils.animateView(thumbnailImageView, true, 500);
//            }
//        });
    }

    @Override
    protected void setErrorMessage(String message, boolean showRetryButton) {
        super.setErrorMessage(message, showRetryButton);

        if (!TextUtils.isEmpty(videoUrl)) StreamInfoCache.getInstance().removeInfo(videoUrl);
        currentStreamInfo = null;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // OnStreamInfoReceivedListener callbacks
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onReceive(StreamInfo info) {
        if (DEBUG) Log.d(TAG, "onReceive() called with: info = [" + info + "]");
        if (info == null || isRemoving() || !isVisible()) return;

        handleStreamInfo(info, true);
        showContentWithAnimation(300, 0, 0);
        AnimationUtils.animateView(loadingProgressBar, false, 200);

        StreamInfoCache.getInstance().putInfo(info);
        isLoading.set(false);

    }

    @Override
    public void onError(int messageId) {
        if (DEBUG) Log.d(TAG, "onError() called with: messageId = [" + messageId + "]");
        //Toast.makeText(activity, messageId, Toast.LENGTH_LONG).show();
        setErrorImage(R.drawable.not_available_monkey);
        setErrorMessage(getString(messageId), true);
    }

    @Override
    public void onReCaptchaException() {
        Toast.makeText(activity, com.hr.musicfm.R.string.recaptcha_request_toast, Toast.LENGTH_LONG).show();
        // Starting ReCaptcha Challenge Activity
        startActivityForResult(new Intent(activity, ReCaptchaActivity.class), ReCaptchaActivity.RECAPTCHA_REQUEST);

        setErrorMessage(getString(R.string.recaptcha_request_toast), false);
    }

    @Override
    public void onBlockedByGemaError() {

        thumbnailBackgroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(com.hr.musicfm.R.string.c3s_url)));
                startActivity(intent);
            }
        });

        setErrorImage(R.drawable.gruese_die_gema);
        setErrorMessage(getString(R.string.blocked_by_gema), false);
    }

    @Override
    public void onContentErrorWithMessage(int messageId) {
        setErrorImage(com.hr.musicfm.R.drawable.not_available_monkey);
        setErrorMessage(getString(messageId), false);
    }

    @Override
    public void onContentError() {
        setErrorImage(com.hr.musicfm.R.drawable.not_available_monkey);
        setErrorMessage(getString(R.string.content_not_available), false);
    }

    @Override
    public void onUnrecoverableError(Exception exception) {
        activity.finish();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Background handling
    //////////////////////////////////////////////////////////////////////////*/

    private static class BackgroundCallback implements Handler.Callback {
        private static final int MESSAGE_DESCRIPTION = 1;
        public static final int MESSAGE_UPLOADER_DATE = 2;
        private final Handler uiHandler;
        private final Context context;

        BackgroundCallback(Handler uiHandler, Context context) {
            this.uiHandler = uiHandler;
            this.context = context;
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_DESCRIPTION:
                    handleDescription((String) msg.obj);
                    return true;
                case MESSAGE_UPLOADER_DATE:
                    handleUploadDate((String) msg.obj);
                    return true;
            }
            return false;
        }

        private void handleUploadDate(String uploadDate) {
            String localizedDate = Localization.localizeDate(uploadDate, context);
            uiHandler.sendMessage(Message.obtain(uiHandler, MESSAGE_UPLOADER_DATE, localizedDate));
        }

        private void handleDescription(String description) {
            Spanned parsedDescription;
            if (TextUtils.isEmpty(description)) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 24) {
                parsedDescription = Html.fromHtml(description, 0);
            } else {
                //noinspection deprecation
                parsedDescription = Html.fromHtml(description);
            }
            uiHandler.sendMessage(Message.obtain(uiHandler, MESSAGE_DESCRIPTION, parsedDescription));
        }
    }

    private class UICallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case BackgroundCallback.MESSAGE_DESCRIPTION:
                    if (videoDescriptionView != null) {
                        videoDescriptionView.setText((Spanned) msg.obj);
                        videoDescriptionView.setVisibility(View.VISIBLE);
                    }
                    return true;
                case BackgroundCallback.MESSAGE_UPLOADER_DATE:
                    if (videoUploadDateView != null) {
                        videoUploadDateView.setText((String) msg.obj);
                        videoUploadDateView.setVisibility(View.VISIBLE);
                    }
                    return true;
            }
            return false;
        }
    }

    public interface OnVideoPlayListener {
        /**
         * Called when a video is played
         * @param videoStream the video stream that is played
         * @param streamInfo the stream info
         */
        void onVideoPlayed(VideoStream videoStream, StreamInfo streamInfo);

        /**
         * Called when the audio is played in the background
         * @param streamInfo the stream info
         * @param audioStream the audio stream that is played
         */
        void onBackgroundPlayed(StreamInfo streamInfo, AudioStream audioStream);
    }

    public class SampleListener implements StandardVideoAllCallBack {

        //加载成功，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onPrepared(String url, Object... objects) {

        }

        //点击了开始按键播放，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickStartIcon(String url, Object... objects) {

        }

        //点击了错误状态下的开始按键，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickStartError(String url, Object... objects) {

        }

        //点击了播放状态下的开始按键--->停止，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickStop(String url, Object... objects) {

        }

        //点击了全屏播放状态下的开始按键--->停止，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickStopFullscreen(String url, Object... objects) {

        }

        //点击了暂停状态下的开始按键--->播放，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickResume(String url, Object... objects) {

        }

        //点击了全屏暂停状态下的开始按键--->播放，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickResumeFullscreen(String url, Object... objects) {

        }

        //点击了空白弹出seekbar，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickSeekbar(String url, Object... objects) {

        }

        //点击了全屏的seekbar，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickSeekbarFullscreen(String url, Object... objects) {

        }

        //播放完了，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onAutoComplete(String url, Object... objects) {

        }

        //进去全屏，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onEnterFullscreen(String url, Object... objects) {

        }

        //退出全屏，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onQuitFullscreen(String url, Object... objects) {

        }

        //进入小窗口，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onQuitSmallWidget(String url, Object... objects) {

        }

        //退出小窗口，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onEnterSmallWidget(String url, Object... objects) {

        }

        //触摸调整声音，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onTouchScreenSeekVolume(String url, Object... objects) {

        }

        //触摸调整进度，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onTouchScreenSeekPosition(String url, Object... objects) {

        }

        //触摸调整亮度，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onTouchScreenSeekLight(String url, Object... objects) {

        }

        //播放错误，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onPlayError(String url, Object... objects) {

        }

        //点击了空白区域开始播放，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickStartThumb(String url, Object... objects) {

        }

        //点击了播放中的空白区域，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickBlank(String url, Object... objects) {

        }

        //点击了全屏播放中的空白区域，objects[0]是title，object[1]是当前所处播放器（全屏或非全屏）
        @Override
        public void onClickBlankFullscreen(String url, Object... objects) {

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        detailPlayer.onVideoPause();
        isPause = true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //如果旋转了就全屏
        if (isPlay && !isPause) {
            detailPlayer.onConfigurationChanged(activity, newConfig, orientationUtils);
        }
    }

    private void resolveNormalVideoUI() {
        //增加title
        detailPlayer.getTitleTextView().setVisibility(View.GONE);
        detailPlayer.getBackButton().setVisibility(View.GONE);
    }
}
