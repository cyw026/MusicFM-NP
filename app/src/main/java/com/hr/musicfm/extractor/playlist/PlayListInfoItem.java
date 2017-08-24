package com.hr.musicfm.extractor.playlist;

import com.hr.musicfm.extractor.InfoItem;

public class PlayListInfoItem implements InfoItem {

    public int serviceId = -1;
    public String name = "";
    public String thumbnailUrl = "";
    public String webPageUrl = "";

    public InfoType infoType() {
        return InfoType.PLAYLIST;
    }
    public String getTitle() {
        return name;
    }
    public String getLink() {
        return webPageUrl;
    }
}
