package com.xinsane.nfc.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xinsane.nfc.CardActivity;
import com.xinsane.nfc.R;
import com.xinsane.nfc.data.PaintItem;
import com.xinsane.nfc.view.PaintView;

import java.util.List;

public class GridPaintListAdapterForCardActivity extends RecyclerView.Adapter {
    private List<PaintItem> list;
    private CardActivity activity;
    private boolean inCard;

    public GridPaintListAdapterForCardActivity(List<PaintItem> list, CardActivity activity, boolean inCard) {
        this.activity = activity;
        this.list = list;
        this.inCard = inCard;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view;
        if (inCard)
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_paint_horizontal, viewGroup, false);
        else
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_paint, viewGroup, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.paintView.setData(list.get(i).getData());
        if (!inCard)
            holder.title.setText(list.get(i).getTitle());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PaintView paintView;
        TextView title;
        ViewHolder(View view, final GridPaintListAdapterForCardActivity adapter) {
            super(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    PaintItem item = adapter.list.get(position);
                    if (adapter.inCard)
                        adapter.activity.onRemove(item);
                    else
                        adapter.activity.onAdd(item);
                }
            });
            paintView = view.findViewById(R.id.paint_view);
            paintView.setEnable(false);
            if (!adapter.inCard)
                title = view.findViewById(R.id.text_paint_title);
        }
    }
}
