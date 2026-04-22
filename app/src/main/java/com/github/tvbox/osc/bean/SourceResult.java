package com.github.tvbox.osc.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;

public class SourceResult implements MultiItemEntity {
    public static final int TYPE_SOURCE = 1;

    public String sourceKey;
    public String sourceName;
    public long speedMs;
    public Movie.Video video;

    public SourceResult(String sourceKey, String sourceName, long speedMs, Movie.Video video) {
        this.sourceKey = sourceKey;
        this.sourceName = sourceName;
        this.speedMs = speedMs;
        this.video = video;
    }

    @Override
    public int getItemType() {
        return TYPE_SOURCE;
    }

    public String getSpeedText() {
        if (speedMs <= 0) return "";
        if (speedMs < 1000) return speedMs + "ms";
        return String.format("%.1fs", speedMs / 1000.0);
    }
}
