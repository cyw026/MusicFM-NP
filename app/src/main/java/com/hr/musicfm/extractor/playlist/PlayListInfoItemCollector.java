package com.hr.musicfm.extractor.playlist;

import com.hr.musicfm.extractor.InfoItemCollector;
import com.hr.musicfm.extractor.exceptions.ParsingException;

public class PlayListInfoItemCollector extends InfoItemCollector {
    public PlayListInfoItemCollector(int serviceId) {
        super(serviceId);
    }

    public PlayListInfoItem extract(PlayListInfoItemExtractor extractor) throws ParsingException {
        final PlayListInfoItem resultItem = new PlayListInfoItem();

        resultItem.name = extractor.getPlayListName();
        resultItem.serviceId = getServiceId();
        resultItem.webPageUrl = extractor.getWebPageUrl();
        try {
            resultItem.thumbnailUrl = extractor.getThumbnailUrl();
        } catch (Exception e) {
            addError(e);
        }
        return resultItem;
    }

    public void commit(PlayListInfoItemExtractor extractor) throws ParsingException {
        try {
            addItem(extract(extractor));
        } catch (Exception e) {
            addError(e);
        }
    }
}
