package com.hr.musicfm.info_list;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.hr.musicfm.extractor.InfoItem;
import com.hr.musicfm.extractor.InfoItem.InfoType;
import com.hr.musicfm.info_list.AdViewHolder;
import com.hr.musicfm.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdListener;
/**
 * Created by Christian Schabesberger on 01.08.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * InfoListAdapter.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class InfoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = InfoListAdapter.class.toString();

    private final InfoItemBuilder infoItemBuilder;
    private final ArrayList<InfoItem> infoItemList;
    private boolean showFooter = false;
    private View header = null;
    private View footer = null;
    private static final int AD_TYPE = 4;

    public class HFHolder extends RecyclerView.ViewHolder {
        public HFHolder(View v) {
            super(v);
            view = v;
        }
        public View view;
    }

    public void showFooter(boolean show) {
        showFooter = show;
        notifyDataSetChanged();
    }

    public class AdHolder extends RecyclerView.ViewHolder {
        public AdHolder(View v) {
            super(v);
            view = v;
        }
        public View view;
    }

    public InfoListAdapter(Activity a) {
        infoItemBuilder = new InfoItemBuilder(a);
        infoItemList = new ArrayList<>();
    }

    public void setOnStreamInfoItemSelectedListener
            (InfoItemBuilder.OnInfoItemSelectedListener listener) {
        infoItemBuilder.setOnStreamInfoItemSelectedListener(listener);
    }

    public void setOnChannelInfoItemSelectedListener
            (InfoItemBuilder.OnInfoItemSelectedListener listener) {
        infoItemBuilder.setOnChannelInfoItemSelectedListener(listener);
    }

    public void addInfoItemList(List<InfoItem> data) {
        if(data != null) {
            for(int i=0;i<data.size();i++){
                if(i > 0 && i%5==0)
                {
                    infoItemList.add(null);
                }
                infoItemList.add(data.get(i));
            }

//            infoItemList.addAll(data);
            notifyDataSetChanged();
        }
    }

    public void addInfoItem(InfoItem data) {
        if (data != null) {
            infoItemList.add( data );
            notifyDataSetChanged();
        }
    }

    public void clearStreamItemList() {
        if(infoItemList.isEmpty()) {
            return;
        }
        infoItemList.clear();
        notifyDataSetChanged();
    }

    public void setHeader(View header) {
        this.header = header;
        notifyDataSetChanged();
    }

    public void setFooter(View view) {
        this.footer = view;
        notifyDataSetChanged();
    }

    public ArrayList<InfoItem> getItemsList() {
        return infoItemList;
    }

    @Override
    public int getItemCount() {
        int count = infoItemList.size();
        if(header != null) count++;
        if(footer != null && showFooter) count++;
        return count;
    }

    // don't ask why we have to do that this way... it's android accept it -.-
    @Override
    public int getItemViewType(int position) {
        if(header != null && position == 0) {
            return 0;
        } else if(header != null) {
            position--;
        }
        if(footer != null && position == infoItemList.size() && showFooter) {
            return 1;
        }

        InfoItem item = infoItemList.get(position);

        if(item==null)
            return AD_TYPE;

        switch(item.infoType()) {
            case STREAM:
                return 2;
            case CHANNEL:
                return 3;
            case PLAYLIST:
                return 4;
            default:
                Log.e(TAG, "Trollolo");
                return -1;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        switch(type) {
            case 0:
                return new HFHolder(header);
            case 1:
                return new HFHolder(footer);
            case 2:
                return new StreamInfoItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.stream_item, parent, false));
            case 3:
                return new ChannelInfoItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.channel_item, parent, false));
            case 4:
                Log.e(TAG, "Playlist is not yet implemented");
                return createAdView(parent);
            default:
                Log.e(TAG, "Trollolo");
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int i) {
        //god damn f*** ANDROID SH**
        if(holder instanceof InfoItemHolder) {
            if(header != null) {
                i--;
            }
            infoItemBuilder.buildByHolder((InfoItemHolder) holder, infoItemList.get(i));
        } else if(holder instanceof HFHolder && i == 0 && header != null) {
            ((HFHolder) holder).view = header;
        } else if(holder instanceof HFHolder && i == infoItemList.size() && footer != null && showFooter) {
            ((HFHolder) holder).view = footer;
        }else if(holder instanceof AdHolder && infoItemList.get(i) == null) {
            String[] ids = {
                    "ca-app-pub-5814663467390565/6721235260",
                    "ca-app-pub-5814663467390565/1286566443",
                    "ca-app-pub-5814663467390565/6882809374"
            };

            AdHolder ad = (AdHolder) holder;
            AdView v = (AdView) ad.view;

//            Random rand = new Random();
//            int index = rand.nextInt(3);
//            v.setAdUnitId(ids[index]);
            AdRequest adRequest = new AdRequest.Builder().build();
            v.loadAd(adRequest);
        }
    }

    public RecyclerView.ViewHolder createAdView(ViewGroup parent) {

        String[] ids = {
                "ca-app-pub-5814663467390565/6721235260",
                "ca-app-pub-5814663467390565/1286566443",
                "ca-app-pub-5814663467390565/6882809374"
        };
        Context context = parent.getContext();
        AdView v = new AdView(context);
        v.setAdSize(AdSize.SMART_BANNER);
        Random rand = new Random();
        int index = rand.nextInt(3);
        v.setAdUnitId(ids[index]);

        v.setAdListener(new AdListener() {
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
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Log.i("Ads", "onAdOpened");
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
                Log.i("Ads", "onAdLeftApplication");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
                Log.i("Ads", "onAdClosed");
            }
        });

//        float density = parent.getContext().getResources().getDisplayMetrics().density;
//        int height = Math.round(AdSize.SMART_BANNER.getHeight() * density);
//        AbsListView.LayoutParams params = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,height);
//        v.setLayoutParams(params);

        AdHolder viewHolder = new AdHolder(v);
        return viewHolder;
    }

}
