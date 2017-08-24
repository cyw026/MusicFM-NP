package com.hr.musicfm.workers;

import android.content.Context;

import com.hr.musicfm.extractor.playlist.PlaylistExtractor;
import com.hr.musicfm.extractor.playlist.PlaylistInfo;
import com.hr.musicfm.extractor.exceptions.ExtractionException;
import com.hr.musicfm.extractor.exceptions.ParsingException;
import com.hr.musicfm.MainActivity;
import com.hr.musicfm.R;
import com.hr.musicfm.report.ErrorActivity;

import java.io.IOException;

import static com.hr.musicfm.report.UserAction.REQUESTED_PLAYLIST;

/**
 * Extract {@link ChannelInfo} with {@link ChannelExtractor} from the given url of the given service
 *
 * @author mauriciocolli
 */
@SuppressWarnings("WeakerAccess")
public class PlayListExtractorWorker extends ExtractorWorker {
    //private static final String TAG = "PlayListExtractorWorker";

    private int pageNumber;

    private PlayListInfo playListInfo = null;
    private OnPlayListInfoReceive callback;

    /**
     * Interface which will be called for result and errors
     */
    public interface OnPlayListInfoReceive {
        void onReceive(PlayListInfo info);
        void onError(int messageId);
        /**
         * Called when an unrecoverable error has occurred.
         * <p> This is a good place to finish the caller. </p>
         */
        void onUnrecoverableError(Exception exception);
    }

    /**
     * @param context           context for error reporting purposes
     * @param serviceId         id of the request service
     * @param playListUrl       playListUrl of the service (e.g. https://www.youtube.com/channel/UC_aEa8K-EOJ3D6gOs7HcyNg)
     * @param pageNumber        which page to extract
     * @param callback          listener that will be called-back when events occur (check {@link ChannelExtractorWorker.OnChannelInfoReceive})
     */
    public PlayListExtractorWorker(Context context, int serviceId, String playListUrl, int pageNumber, OnPlayListInfoReceive callback) {
        super(context, playListUrl, serviceId);
        this.pageNumber = pageNumber;
        this.callback = callback;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.callback = null;
        this.playListInfo = null;
    }

    @Override
    protected void doWork(int serviceId, String url) throws Exception {
        PlayListExtractor extractor = getService().getPlayListExtractorInstance(url, pageNumber);
        playListInfo = PlayListInfo.getInfo(extractor);

        if (!playListInfo.errors.isEmpty()) handleErrorsDuringExtraction(playListInfo.errors, REQUESTED_PLAYLIST);

        if (callback != null && playListInfo != null && !isInterrupted()) getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (isInterrupted() || callback == null) return;

                callback.onReceive(playListInfo);
                onDestroy();
            }
        });
    }


    @Override
    protected void handleException(final Exception exception, int serviceId, String url) {
        if (callback == null || getHandler() == null || isInterrupted()) return;

        if (exception instanceof IOException) {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(R.string.network_error);
                }
            });
        } else if (exception instanceof ParsingException || exception instanceof ExtractionException) {
            ErrorActivity.reportError(getHandler(), getContext(), exception, MainActivity.class, null, ErrorActivity.ErrorInfo.make(REQUESTED_PLAYLIST, getServiceName(), url, R.string.parsing_error));
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onUnrecoverableError(exception);
                }
            });
        } else {
            ErrorActivity.reportError(getHandler(), getContext(), exception, MainActivity.class, null, ErrorActivity.ErrorInfo.make(REQUESTED_PLAYLIST, getServiceName(), url, R.string.general_error));
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onUnrecoverableError(exception);
                }
            });
        }
    }
}

