package com.hr.musicfm.info_list;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.CardView;
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
import com.google.android.gms.ads.NativeExpressAdView;
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
    private  Activity activity;

    // The Native Express ad view type.
    private static final int NATIVE_EXPRESS_AD_VIEW_TYPE = 5;
    // A Native Express ad is placed in every nth position in the RecyclerView.
    public static final int ITEMS_PER_AD = 7;
    // The Native Express ad height.
    private static final int NATIVE_EXPRESS_AD_HEIGHT = 80;
    // The Native Express ad unit ID.
    private static final String AD_UNIT_ID = "ca-app-pub-5814663467390565/6721235260";

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

    /**
     * The {@link NativeExpressAdViewHolder} class.
     */
    public class NativeExpressAdViewHolder extends RecyclerView.ViewHolder {

        NativeExpressAdViewHolder(View v, NativeExpressAdView ad) {
            super(v);
            view = v;
            adView = ad;
            ad_card_view = (CardView)itemView.findViewById(R.id.ad_card_view);
            if (adView.getParent() != null) {
                ((ViewGroup) adView.getParent()).removeView(adView);
            }
            final ViewGroup adCardView = (ViewGroup) this.itemView;
            adCardView.addView(adView);
        }
        public View view;
        public CardView ad_card_view;
        public NativeExpressAdView adView;
    }

    public class AdViewInfoItem implements InfoItem {

        public String name = "";
        public String webPageUrl = "";

        public InfoType infoType() {
            return InfoType.AD_VIEW;
        }
        public String getTitle() {
            return name;
        }
        public String getLink() {
            return webPageUrl;
        }
    }

    public class AdHolder extends RecyclerView.ViewHolder {
        public AdHolder(AdView v) {
            super(v);
            adView = v;
        }
        public AdView adView;
    }

    public InfoListAdapter(Activity a) {
        infoItemBuilder = new InfoItemBuilder(a);
        infoItemList = new ArrayList<>();
        activity = a;
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

            List<InfoItem> temp = new ArrayList<>();
            for (int i = 0; i < data.size(); i ++) {
                if ((i + 1) % ITEMS_PER_AD == 0) {
                    final AdViewInfoItem item = new AdViewInfoItem();
                    temp.add(item);
                } else {
                    temp.add(data.get(i));
                }
            }
            infoItemList.addAll(temp);
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
        switch(item.infoType()) {
            case STREAM:
                return 2;
            case CHANNEL:
                return 3;
            case PLAYLIST:
                return 4;
            case AD_VIEW:
                return NATIVE_EXPRESS_AD_VIEW_TYPE;
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
                return createAdViewHolder(parent);
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                AdHolder holder = (AdHolder)createAdViewHolder(parent);
                holder.adView.loadAd(new AdRequest.Builder()
                        .addTestDevice("40378D17294F10E61A5AAE79217F1BFB")
                        .build());
                return holder;
            default:
                Log.e(TAG, "Trollolo");
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case 0:
                ((HFHolder) holder).view = header;
                break;
            case 1:
                ((HFHolder) holder).view = footer;
                break;
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                AdHolder h = (AdHolder) holder;
                final AdView adView = h.adView;
                adView.loadAd(new AdRequest.Builder()
                        .addTestDevice("40378D17294F10E61A5AAE79217F1BFB")
                        .build());
                break;
            case 2:
            case 3:
                if(header != null) {
                    position--;
                }
                infoItemBuilder.buildByHolder((InfoItemHolder) holder, infoItemList.get(position));
            default:
        }
    }

    public RecyclerView.ViewHolder createAdViewHolder(ViewGroup parent) {

        Context context = parent.getContext();
        AdView v = new AdView(context);
        v.setAdSize(AdSize.SMART_BANNER);
        v.setAdUnitId(AD_UNIT_ID);

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

        AdHolder viewHolder = new AdHolder(v);
        return viewHolder;
    }

}
