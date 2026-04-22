package com.github.tvbox.osc.bean;

import com.chad.library.adapter.base.entity.AbstractExpandableItem;
import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class AggregatedResult extends AbstractExpandableItem<SourceResult> implements MultiItemEntity {
    public static final int TYPE_HEADER = 0;

    private static final Pattern BRACKET_PATTERN = Pattern.compile("[（(][^)）]*[)）]");

    public String normalizedName;
    public String displayName;
    public String type;
    public String pic;
    public int year;
    public String note;
    public float relevanceScore;

    public AggregatedResult(String normalizedName, String displayName) {
        this.normalizedName = normalizedName;
        this.displayName = displayName;
    }

    @Override
    public int getItemType() {
        return TYPE_HEADER;
    }

    @Override
    public int getLevel() {
        return 0;
    }

    public void addSource(SourceResult source) {
        getSubItems().add(source);
    }

    @Override
    public List<SourceResult> getSubItems() {
        List<SourceResult> items = super.getSubItems();
        if (items == null) {
            items = new ArrayList<>();
            setSubItems(items);
        }
        return items;
    }

    public int getSourceCount() {
        return getSubItems().size();
    }

    public long getFastestSpeed() {
        long fastest = Long.MAX_VALUE;
        for (SourceResult s : getSubItems()) {
            if (s.speedMs > 0 && s.speedMs < fastest) fastest = s.speedMs;
        }
        return fastest == Long.MAX_VALUE ? 0 : fastest;
    }

    public void sortSourcesBySpeed() {
        List<SourceResult> items = getSubItems();
        Collections.sort(items, new Comparator<SourceResult>() {
            @Override
            public int compare(SourceResult a, SourceResult b) {
                long sa = a.speedMs <= 0 ? Long.MAX_VALUE : a.speedMs;
                long sb = b.speedMs <= 0 ? Long.MAX_VALUE : b.speedMs;
                return Long.compare(sa, sb);
            }
        });
    }

    public void fillFromFirstSource() {
        for (SourceResult s : getSubItems()) {
            Movie.Video v = s.video;
            if (pic == null && v.pic != null && !v.pic.isEmpty()) pic = v.pic;
            if (type == null && v.type != null && !v.type.isEmpty()) type = v.type;
            if (year == 0 && v.year > 0) year = v.year;
            if (note == null && v.note != null && !v.note.isEmpty()) note = v.note;
            if (pic != null) break;
        }
    }

    public static String normalize(String name) {
        if (name == null) return "";
        name = name.trim();
        name = BRACKET_PATTERN.matcher(name).replaceAll("");
        return name.trim();
    }

    public static float scoreRelevance(String videoName, String searchTitle) {
        String normalizedName = normalize(videoName);
        String normalizedSearch = normalize(searchTitle);

        if (normalizedName.equals(normalizedSearch)) return 1.0f;
        if (normalizedName.startsWith(normalizedSearch)) return 0.8f;
        if (normalizedName.contains(normalizedSearch)) return 0.6f;
        if (normalizedSearch.contains(normalizedName)) return 0.4f;
        return 0f;
    }
}
