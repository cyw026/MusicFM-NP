package com.hr.musicfm.extractor;

import com.hr.musicfm.extractor.services.youtube.YoutubeService;

/**
 * Created by the-scrabi on 18.02.17.
 */

class ServiceList {
    public static final StreamingService[] serviceList = {
            new YoutubeService(0)
    };
}