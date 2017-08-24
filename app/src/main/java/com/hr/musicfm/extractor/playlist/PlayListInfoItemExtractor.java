package com.hr.musicfm.extractor.playlist;

import com.hr.musicfm.extractor.exceptions.ParsingException;

public interface PlayListInfoItemExtractor {
    String getThumbnailUrl() throws ParsingException;
    String getPlayListName() throws ParsingException;
    String getWebPageUrl() throws ParsingException;
}
