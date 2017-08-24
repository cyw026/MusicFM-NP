package com.hr.musicfm.database.history.dao;

import com.hr.musicfm.database.BasicDAO;

public interface HistoryDAO<T> extends BasicDAO<T> {
    T getLatestEntry();
}
