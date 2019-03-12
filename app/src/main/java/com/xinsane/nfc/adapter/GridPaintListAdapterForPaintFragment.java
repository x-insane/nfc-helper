package com.xinsane.nfc.adapter;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xinsane.nfc.PaintActivity;
import com.xinsane.nfc.R;
import com.xinsane.nfc.data.PaintItem;
import com.xinsane.nfc.fragment.PaintListFragment;
import com.xinsane.nfc.view.PaintView;
import java.util.List;

public class GridPaintListAdapterForPaintFragment extends RecyclerView.Adapter {
    private List<PaintItem> list;
    private PaintListFragment fragment;

    public GridPaintListAdapterForPaintFragment(List<PaintItem> list, PaintListFragment fragment) {
        this.fragment = fragment;
        this.list = list;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_paint, viewGroup, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.paintView.setData(list.get(i).getData());
        holder.title.setText(list.get(i).getTitle());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PaintView paintView;
        TextView title;
        ViewHolder(View view, final GridPaintListAdapterForPaintFragment adapter) {
            super(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    PaintItem item = adapter.list.get(position);
                    Intent intent = new Intent(adapter.fragment.getContext(), PaintActivity.class);
                    intent.putExtra("title", item.getTitle());
                    intent.putExtra("data", PaintListFragment.byteArrayToHexString(item.getData()));
                    adapter.fragment.startActivityForResult(intent, 1);
                }
            });
            paintView = view.findViewById(R.id.paint_view);
            paintView.setEnable(false);
            title = view.findViewById(R.id.text_paint_title);
        }
    }
}
