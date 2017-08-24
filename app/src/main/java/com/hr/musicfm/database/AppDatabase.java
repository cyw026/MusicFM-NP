package com.hr.musicfm.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import com.hr.musicfm.database.history.dao.WatchHistoryDAO;
import com.hr.musicfm.database.subscription.SubscriptionDAO;
import com.hr.musicfm.database.history.dao.SearchHistoryDAO;
import com.hr.musicfm.database.history.model.SearchHistoryEntry;
import com.hr.musicfm.database.history.model.WatchHistoryEntry;
import com.hr.musicfm.database.subscription.SubscriptionEntity;
import com.hr.musicfm.database.history.Converters;

@TypeConverters({Converters.class})
@Database(entities = {SubscriptionEntity.class, WatchHistoryEntry.class, SearchHistoryEntry.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase{

    public static final String DATABASE_NAME = "newpipe.db";

    public abstract SubscriptionDAO subscriptionDAO();

    public abstract WatchHistoryDAO watchHistoryDAO();

    public abstract SearchHistoryDAO searchHistoryDAO();
}
