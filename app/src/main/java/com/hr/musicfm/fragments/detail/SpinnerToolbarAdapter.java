package com.hr.musicfm.fragments.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.hr.musicfm.extractor.MediaFormat;
import com.hr.musicfm.extractor.stream_info.VideoStream;
import com.hr.musicfm.R;

import java.util.List;

public class SpinnerToolbarAdapter extends BaseAdapter {
    private final List<VideoStream> videoStreams;
    private final boolean showIconNoAudio;

    private final Context context;

    public SpinnerToolbarAdapter(Context context, List<VideoStream> videoStreams, boolean showIconNoAudio) {
        this.context = context;
        this.videoStreams = videoStreams;
        this.showIconNoAudio = showIconNoAudio;
    }

    @Override
    public int getCount() {
        return videoStreams.size();
    }

    @Override
    public Object getItem(int position) {
        return videoStreams.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent, true);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(((Spinner) parent).getSelectedItemPosition(), convertView, parent, false);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent, boolean isDropdownItem) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.resolutions_spinner_item, parent, false);
        }

        ImageView woSoundIcon = (ImageView) convertView.findViewById(R.id.wo_sound_icon);
        TextView text = (TextView) convertView.findViewById(android.R.id.text1);
        VideoStream item = (VideoStream) getItem(position);
        text.setText(MediaFormat.getNameById(item.format) + " " + item.resolution);

        int visibility = !showIconNoAudio ? View.GONE
                : item.isVideoOnly ? View.VISIBLE
                : isDropdownItem ? View.INVISIBLE
                : View.GONE;
        woSoundIcon.setVisibility(visibility);

        return convertView;
    }

}