package com.hr.musicfm.info_list;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hr.musicfm.R;
import com.hr.musicfm.extractor.InfoItem;

/**
 * Created by sinogz on 2017/8/25.
 */

public class AdViewHolder extends InfoItemHolder {

    public AdViewHolder(View v) {
        super(v);
    }

    @Override
    public InfoItem.InfoType infoType() {
        return InfoItem.InfoType.AD_VIEW;
    }
}