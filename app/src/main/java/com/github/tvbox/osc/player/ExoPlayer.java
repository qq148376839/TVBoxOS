package com.github.tvbox.osc.player;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import com.github.tvbox.osc.util.AudioTrackMemory;
import com.github.tvbox.osc.util.LOG;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.doikki.videoplayer.exo.ExoMediaPlayer;

@OptIn(markerClass = UnstableApi.class)
public class ExoPlayer extends ExoMediaPlayer {

    private static AudioTrackMemory memory;

    public ExoPlayer(Context context) {
        super(context);
        memory = AudioTrackMemory.getInstance(context);
    }

    public TrackInfo getTrackInfo() {
        TrackInfo data = new TrackInfo();
        if (mInternalPlayer == null) return data;

        Tracks tracks = mInternalPlayer.getCurrentTracks();
        List<Tracks.Group> groups = tracks.getGroups();

        boolean audioSelected = false;
        int audioGroupIndex = 0;
        int subtitleGroupIndex = 0;

        for (int gi = 0; gi < groups.size(); gi++) {
            Tracks.Group group = groups.get(gi);
            @C.TrackType int type = group.getType();
            if (type != C.TRACK_TYPE_AUDIO && type != C.TRACK_TYPE_TEXT) continue;

            TrackGroup mediaTrackGroup = group.getMediaTrackGroup();
            for (int ti = 0; ti < group.length; ti++) {
                Format fmt = group.getTrackFormat(ti);
                TrackInfoBean bean = new TrackInfoBean();
                bean.language = getLanguage(fmt);
                bean.name = getName(fmt);
                bean.groupIndex = gi;
                bean.index = ti;
                boolean selected = group.isTrackSelected(ti);
                bean.selected = selected;
                if (type == C.TRACK_TYPE_AUDIO) {
                    if (selected) audioSelected = true;
                    data.addAudio(bean);
                    audioGroupIndex++;
                } else {
                    data.addSubtitle(bean);
                    subtitleGroupIndex++;
                }
            }
        }

        // 如果没有任何音轨被选中，默认选第一个
        if (!audioSelected && data.getAudio() != null && !data.getAudio().isEmpty()) {
            data.getAudio().get(0).selected = true;
        }

        return data;
    }

    public void setTrack(int groupIndex, int trackIndex, String playKey) {
        try {
            if (mInternalPlayer == null) {
                LOG.i("echo-setTrack: player is null");
                return;
            }

            Tracks tracks = mInternalPlayer.getCurrentTracks();
            List<Tracks.Group> groups = tracks.getGroups();

            if (groupIndex < 0 || groupIndex >= groups.size()) {
                LOG.i("echo-setTrack: Invalid group index: " + groupIndex);
                return;
            }

            Tracks.Group group = groups.get(groupIndex);
            if (group.getType() != C.TRACK_TYPE_AUDIO) {
                LOG.i("echo-setTrack: Group " + groupIndex + " is not audio");
                return;
            }

            TrackGroup mediaTrackGroup = group.getMediaTrackGroup();
            if (trackIndex < 0 || trackIndex >= mediaTrackGroup.length) {
                LOG.i("echo-setTrack: Invalid track index: " + trackIndex);
                return;
            }

            TrackSelectionOverride override = new TrackSelectionOverride(
                    mediaTrackGroup, Collections.singletonList(trackIndex));
            DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters();
            builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO);
            builder.addOverride(override);
            trackSelector.setParameters(builder.build());

            if (!playKey.isEmpty()) {
                memory.save(playKey, groupIndex, trackIndex);
            }
        } catch (Exception e) {
            LOG.i("echo-setTrack error: " + e.getMessage());
        }
    }

    public void loadDefaultTrack(String playKey) {
        Pair<Integer, Integer> pair = memory.exoLoad(playKey);
        if (pair == null) return;
        if (mInternalPlayer == null) return;

        Tracks tracks = mInternalPlayer.getCurrentTracks();
        List<Tracks.Group> groups = tracks.getGroups();

        int groupIndex = pair.first;
        int trackIndex = pair.second;

        if (groupIndex < 0 || groupIndex >= groups.size()) return;

        Tracks.Group group = groups.get(groupIndex);
        if (group.getType() != C.TRACK_TYPE_AUDIO) return;

        TrackGroup mediaTrackGroup = group.getMediaTrackGroup();
        if (trackIndex < 0 || trackIndex >= mediaTrackGroup.length) return;

        TrackSelectionOverride override = new TrackSelectionOverride(
                mediaTrackGroup, Collections.singletonList(trackIndex));
        DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters();
        builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO);
        builder.addOverride(override);
        trackSelector.setParameters(builder.build());
    }

    private static final Map<String, String> LANG_MAP = new HashMap<>();
    static {
        LANG_MAP.put("zh", "中文");
        LANG_MAP.put("zh-cn", "中文");
        LANG_MAP.put("en", "英语");
        LANG_MAP.put("en-us", "英语");
    }

    private String getLanguage(Format fmt) {
        String lang = fmt.language;
        if (lang == null || lang.isEmpty() || "und".equalsIgnoreCase(lang)) {
            return "未知";
        }
        String name = LANG_MAP.get(lang.toLowerCase());
        return name != null ? name : lang;
    }

    private String getName(Format fmt) {
        String channelLabel;
        if (fmt.channelCount <= 0) {
            channelLabel = "";
        } else if (fmt.channelCount == 1) {
            channelLabel = "单声道";
        } else if (fmt.channelCount == 2) {
            channelLabel = "立体声";
        } else {
            channelLabel = fmt.channelCount + " 声道";
        }
        String codec = "";
        if (fmt.sampleMimeType != null) {
            String mime = fmt.sampleMimeType.substring(fmt.sampleMimeType.indexOf('/') + 1);
            codec = mime.toUpperCase();
        }
        return String.join(", ", channelLabel, codec);
    }
}
