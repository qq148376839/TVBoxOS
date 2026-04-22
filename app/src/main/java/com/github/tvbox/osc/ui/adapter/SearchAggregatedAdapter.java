package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.AggregatedResult;
import com.github.tvbox.osc.bean.SourceResult;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.MD5;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class SearchAggregatedAdapter extends BaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder> {

    public SearchAggregatedAdapter() {
        super(new ArrayList<>());
        addItemType(AggregatedResult.TYPE_HEADER, R.layout.item_search_aggregated);
        addItemType(SourceResult.TYPE_SOURCE, R.layout.item_search_source);
    }

    @Override
    protected void convert(BaseViewHolder helper, MultiItemEntity item) {
        switch (item.getItemType()) {
            case AggregatedResult.TYPE_HEADER:
                bindHeader(helper, (AggregatedResult) item);
                break;
            case SourceResult.TYPE_SOURCE:
                bindSource(helper, (SourceResult) item);
                break;
        }
    }

    private void bindHeader(BaseViewHolder helper, AggregatedResult item) {
        helper.setText(R.id.tvName, item.displayName);

        StringBuilder meta = new StringBuilder();
        if (item.type != null && !item.type.isEmpty()) meta.append(item.type);
        if (item.year > 0) {
            if (meta.length() > 0) meta.append(" | ");
            meta.append(item.year);
        }
        if (item.note != null && !item.note.isEmpty()) {
            if (meta.length() > 0) meta.append(" | ");
            meta.append(item.note);
        }
        helper.setText(R.id.tvMeta, meta.toString());
        helper.setVisible(R.id.tvMeta, meta.length() > 0);

        int count = item.getSourceCount();
        long fastest = item.getFastestSpeed();
        String expandIcon = item.isExpanded() ? "\u25BE" : "\u25B8";
        String summary;
        if (fastest > 0) {
            summary = expandIcon + " " + count + " \u4E2A\u6E90\u53EF\u7528  \u6700\u5FEB " + formatSpeed(fastest);
        } else {
            summary = expandIcon + " " + count + " \u4E2A\u6E90\u53EF\u7528";
        }
        helper.setText(R.id.tvSourceSummary, summary);

        ImageView ivThumb = helper.getView(R.id.ivThumb);
        if (!TextUtils.isEmpty(item.pic)) {
            Picasso.get()
                    .load(item.pic)
                    .transform(new RoundTransformation(MD5.string2MD5(item.pic))
                            .centerCorp(true)
                            .override(AutoSizeUtils.mm2px(mContext, 120), AutoSizeUtils.mm2px(mContext, 160))
                            .roundRadius(AutoSizeUtils.mm2px(mContext, 10), RoundTransformation.RoundType.ALL))
                    .placeholder(R.drawable.img_loading_placeholder)
                    .noFade()
                    .error(ImgUtil.createTextDrawable(item.displayName))
                    .into(ivThumb);
        } else {
            ivThumb.setImageDrawable(ImgUtil.createTextDrawable(item.displayName));
        }
    }

    private void bindSource(BaseViewHolder helper, SourceResult item) {
        helper.setText(R.id.tvSourceName, item.sourceName);

        String speedText = item.getSpeedText();
        helper.setText(R.id.tvSpeed, speedText);
        helper.setVisible(R.id.tvSpeed, !speedText.isEmpty());

        boolean isFastest = false;
        int pos = helper.getAdapterPosition();
        if (pos > 0) {
            MultiItemEntity prev = getData().get(pos - 1);
            if (prev instanceof AggregatedResult) {
                isFastest = true;
            } else if (prev instanceof SourceResult) {
                for (int i = pos - 1; i >= 0; i--) {
                    MultiItemEntity check = getData().get(i);
                    if (check instanceof AggregatedResult) {
                        AggregatedResult parent = (AggregatedResult) check;
                        List<SourceResult> sources = parent.getSubItems();
                        if (sources != null && !sources.isEmpty() && sources.get(0) == item) {
                            isFastest = true;
                        }
                        break;
                    }
                }
            }
        }

        if (isFastest && item.speedMs > 0) {
            helper.setVisible(R.id.tvFastest, true);
            helper.setText(R.id.tvFastest, "\u26A1 ");
        } else {
            helper.setVisible(R.id.tvFastest, false);
        }
    }

    private String formatSpeed(long ms) {
        if (ms < 1000) return ms + "ms";
        return String.format("%.1fs", ms / 1000.0);
    }
}
