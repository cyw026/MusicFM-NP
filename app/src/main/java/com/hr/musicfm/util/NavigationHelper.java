package com.hr.musicfm.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.hr.musicfm.MainActivity;
import com.hr.musicfm.about.AboutActivity;
import com.hr.musicfm.download.DownloadActivity;
import com.hr.musicfm.extractor.NewPipe;
import com.hr.musicfm.extractor.StreamingService;
import com.hr.musicfm.extractor.stream_info.AudioStream;
import com.hr.musicfm.extractor.stream_info.StreamInfo;
import com.hr.musicfm.fragments.FeedFragment;
import com.hr.musicfm.fragments.channel.ChannelFragment;
import com.hr.musicfm.fragments.detail.VideoDetailFragment;
import com.hr.musicfm.fragments.search.SearchFragment;
import com.hr.musicfm.player.BackgroundPlayer;
import com.hr.musicfm.player.BasePlayer;
import com.hr.musicfm.player.VideoPlayer;
import com.hr.musicfm.settings.SettingsActivity;

import com.hr.musicfm.R;
import com.hr.musicfm.fragments.MainFragment;

@SuppressWarnings({"unused", "WeakerAccess"})
public class NavigationHelper {
    public static final String MAIN_FRAGMENT_TAG = "main_fragment_tag";

    /*//////////////////////////////////////////////////////////////////////////
    // Players
    //////////////////////////////////////////////////////////////////////////*/

    public static Intent getOpenVideoPlayerIntent(Context context, Class targetClazz, StreamInfo info, int selectedStreamIndex) {
        Intent mIntent = new Intent(context, targetClazz)
                .putExtra(BasePlayer.VIDEO_TITLE, info.title)
                .putExtra(BasePlayer.VIDEO_URL, info.webpage_url)
                .putExtra(BasePlayer.VIDEO_THUMBNAIL_URL, info.thumbnail_url)
                .putExtra(BasePlayer.CHANNEL_NAME, info.uploader)
                .putExtra(VideoPlayer.INDEX_SEL_VIDEO_STREAM, selectedStreamIndex)
                .putExtra(VideoPlayer.VIDEO_STREAMS_LIST, Utils.getSortedStreamVideosList(context, info.video_streams, info.video_only_streams, false))
                .putExtra(VideoPlayer.VIDEO_ONLY_AUDIO_STREAM, Utils.getHighestQualityAudio(info.audio_streams));
        if (info.start_position > 0) mIntent.putExtra(BasePlayer.START_POSITION, info.start_position * 1000);
        return mIntent;
    }

    public static Intent getOpenVideoPlayerIntent(Context context, Class targetClazz, VideoPlayer instance) {
        return new Intent(context, targetClazz)
                .putExtra(BasePlayer.VIDEO_TITLE, instance.getVideoTitle())
                .putExtra(BasePlayer.VIDEO_URL, instance.getVideoUrl())
                .putExtra(BasePlayer.VIDEO_THUMBNAIL_URL, instance.getVideoThumbnailUrl())
                .putExtra(BasePlayer.CHANNEL_NAME, instance.getChannelName())
                .putExtra(VideoPlayer.INDEX_SEL_VIDEO_STREAM, instance.getSelectedStreamIndex())
                .putExtra(VideoPlayer.VIDEO_STREAMS_LIST, instance.getVideoStreamsList())
                .putExtra(VideoPlayer.VIDEO_ONLY_AUDIO_STREAM, instance.getAudioStream())
                .putExtra(BasePlayer.START_POSITION, ((int) instance.getPlayer().getCurrentPosition()))
                .putExtra(BasePlayer.PLAYBACK_SPEED, instance.getPlaybackSpeed());
    }

    public static Intent getOpenBackgroundPlayerIntent(Context context, StreamInfo info) {
        return getOpenBackgroundPlayerIntent(context, info, info.audio_streams.get(Utils.getPreferredAudioFormat(context, info.audio_streams)));
    }

    public static Intent getOpenBackgroundPlayerIntent(Context context, StreamInfo info, AudioStream audioStream) {
        Intent mIntent = new Intent(context, BackgroundPlayer.class)
                .putExtra(BasePlayer.VIDEO_TITLE, info.title)
                .putExtra(BasePlayer.VIDEO_URL, info.webpage_url)
                .putExtra(BasePlayer.VIDEO_THUMBNAIL_URL, info.thumbnail_url)
                .putExtra(BasePlayer.CHANNEL_NAME, info.uploader)
                .putExtra(BasePlayer.CHANNEL_NAME, info.uploader)
                .putExtra(BackgroundPlayer.AUDIO_STREAM, audioStream);
        if (info.start_position > 0) mIntent.putExtra(BasePlayer.START_POSITION, info.start_position * 1000);
        return mIntent;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Through FragmentManager
    //////////////////////////////////////////////////////////////////////////*/

    public static void gotoMainFragment(FragmentManager fragmentManager) {
        ImageLoader.getInstance().clearMemoryCache();

        boolean popped = fragmentManager.popBackStackImmediate(MAIN_FRAGMENT_TAG, 0);
        if (!popped) openMainFragment(fragmentManager);
    }

    private static void openMainFragment(FragmentManager fragmentManager) {
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.custom_fade_in, R.anim.custom_fade_out, R.anim.custom_fade_in, R.anim.custom_fade_out)
                .replace(R.id.fragment_holder, new MainFragment())
                .addToBackStack(MAIN_FRAGMENT_TAG)
                .commit();
    }

    public static void openSearchFragment(FragmentManager fragmentManager, int serviceId, String query) {
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.custom_fade_in, R.anim.custom_fade_out, R.anim.custom_fade_in, R.anim.custom_fade_out)
                .replace(R.id.fragment_holder, SearchFragment.getInstance(serviceId, query))
                .addToBackStack(null)
                .commit();
    }

    public static void openVideoDetailFragment(FragmentManager fragmentManager, int serviceId, String url, String title) {
        openVideoDetailFragment(fragmentManager, serviceId, url, title, false);
    }

    public static void openVideoDetailFragment(FragmentManager fragmentManager, int serviceId, String url, String title, boolean autoPlay) {
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_holder);
        if (title == null) title = "";

        if (fragment instanceof VideoDetailFragment && fragment.isVisible()) {
            VideoDetailFragment detailFragment = (VideoDetailFragment) fragment;
            detailFragment.setAutoplay(autoPlay);
            detailFragment.selectAndLoadVideo(serviceId, url, title);
            return;
        }

        VideoDetailFragment instance = VideoDetailFragment.getInstance(serviceId, url, title);
        instance.setAutoplay(autoPlay);

        fragmentManager.beginTransaction()
                .setCustomAnimations(com.hr.musicfm.R.anim.custom_fade_in, R.anim.custom_fade_out, R.anim.custom_fade_in, R.anim.custom_fade_out)
                .replace(R.id.fragment_holder, instance)
                .addToBackStack(null)
                .commit();
    }

    public static void openChannelFragment(FragmentManager fragmentManager, int serviceId, String url, String name) {
        if (name == null) name = "";
        fragmentManager.beginTransaction()
                .setCustomAnimations(com.hr.musicfm.R.anim.custom_fade_in, com.hr.musicfm.R.anim.custom_fade_out, R.anim.custom_fade_in, R.anim.custom_fade_out)
                .replace(R.id.fragment_holder, ChannelFragment.getInstance(serviceId, url, name))
                .addToBackStack(null)
                .commit();
    }

    public static void openWhatsNewFragment(FragmentManager fragmentManager) {
        fragmentManager.beginTransaction()
                .setCustomAnimations(com.hr.musicfm.R.anim.custom_fade_in, R.anim.custom_fade_out, R.anim.custom_fade_in, R.anim.custom_fade_out)
                .replace(R.id.fragment_holder, new FeedFragment())
                .addToBackStack(null)
                .commit();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Through Intents
    //////////////////////////////////////////////////////////////////////////*/

    public static void openSearch(Context context, int serviceId, String query) {
        Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.putExtra(Constants.KEY_SERVICE_ID, serviceId);
        mIntent.putExtra(Constants.KEY_QUERY, query);
        mIntent.putExtra(Constants.KEY_OPEN_SEARCH, true);
        context.startActivity(mIntent);
    }

    public static void openChannel(Context context, int serviceId, String url) {
        openChannel(context, serviceId, url, null);
    }

    public static void openChannel(Context context, int serviceId, String url, String name) {
        Intent openIntent = getOpenIntent(context, url, serviceId, StreamingService.LinkType.CHANNEL);
        if (name != null && !name.isEmpty()) openIntent.putExtra(Constants.KEY_TITLE, name);
        context.startActivity(openIntent);
    }

    public static void openVideoDetail(Context context, int serviceId, String url) {
        openVideoDetail(context, serviceId, url, null);
    }

    public static void openVideoDetail(Context context, int serviceId, String url, String title) {
        Intent openIntent = getOpenIntent(context, url, serviceId, StreamingService.LinkType.STREAM);
        if (title != null && !title.isEmpty()) openIntent.putExtra(Constants.KEY_TITLE, title);
        context.startActivity(openIntent);
    }

    public static void openMainActivity(Context context) {
        Intent mIntent = new Intent(context, MainActivity.class);
        context.startActivity(mIntent);
    }

    public static void openByLink(Context context, String url) throws Exception {
        Intent intentByLink = getIntentByLink(context, url);
        if (intentByLink == null) throw new NullPointerException("getIntentByLink(context = [" + context + "], url = [" + url + "]) returned null");
        intentByLink.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentByLink.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intentByLink);
    }

    private static Intent getOpenIntent(Context context, String url, int serviceId, StreamingService.LinkType type) {
        Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.putExtra(Constants.KEY_SERVICE_ID, serviceId);
        mIntent.putExtra(Constants.KEY_URL, url);
        mIntent.putExtra(Constants.KEY_LINK_TYPE, type);
        return mIntent;
    }

    private static Intent getIntentByLink(Context context, String url) throws Exception {
        StreamingService service = NewPipe.getServiceByUrl(url);
        if (service == null) throw new Exception("NewPipe.getServiceByUrl returned null for url > \"" + url + "\"");
        int serviceId = service.getServiceId();
        switch (service.getLinkTypeByUrl(url)) {
            case STREAM:
                Intent sIntent = getOpenIntent(context, url, serviceId, StreamingService.LinkType.STREAM);
                sIntent.putExtra(VideoDetailFragment.AUTO_PLAY, PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(context.getString(R.string.autoplay_through_intent_key), false));
                return sIntent;
            case CHANNEL:
                return getOpenIntent(context, url, serviceId, StreamingService.LinkType.CHANNEL);
            case NONE:
                throw new Exception("Url not known to service. service=" + serviceId + " url=" + url);
        }
        return null;
    }

    public static void openAbout(Context context) {
        Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    public static void openSettings(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    public static boolean openDownloads(Activity activity) {
        if (!PermissionHelper.checkStoragePermissions(activity)) {
            return false;
        }
        Intent intent = new Intent(activity, DownloadActivity.class);
        activity.startActivity(intent);
        return true;
    }
}
