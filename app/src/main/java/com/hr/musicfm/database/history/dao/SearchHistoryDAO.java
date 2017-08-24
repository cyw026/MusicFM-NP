package com.hr.musicfm.database.history.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import com.hr.musicfm.database.history.model.HistoryEntry;
import com.hr.musicfm.database.history.model.SearchHistoryEntry;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface SearchHistoryDAO extends HistoryDAO<SearchHistoryEntry> {

    String ORDER_BY_CREATION_DATE = " ORDER BY " + HistoryEntry.CREATION_DATE + " DESC";

    @Query("SELECT * FROM " + SearchHistoryEntry.TABLE_NAME + " WHERE " + HistoryEntry.ID + " = (SELECT MAX(" + HistoryEntry.ID + ") FROM " + SearchHistoryEntry.TABLE_NAME + ")")
    @Override
    SearchHistoryEntry getLatestEntry();

    @Query("DELETE FROM " + SearchHistoryEntry.TABLE_NAME)
    @Override
    int deleteAll();

    @Query("SELECT * FROM " + SearchHistoryEntry.TABLE_NAME + ORDER_BY_CREATION_DATE)
    @Override
    Flowable<List<SearchHistoryEntry>> findAll();

    @Query("SELECT * FROM " + SearchHistoryEntry.TABLE_NAME + " WHERE " + HistoryEntry.SERVICE_ID + " = :serviceId" + ORDER_BY_CREATION_DATE)
    @Override
    Flowable<List<SearchHistoryEntry>> listByService(int serviceId);
}
