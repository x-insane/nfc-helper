package com.xinsane.nfc.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xinsane.nfc.App;
import com.xinsane.nfc.PaintActivity;
import com.xinsane.nfc.R;
import com.xinsane.nfc.data.PaintItem;
import com.xinsane.nfc.adapter.GridPaintListAdapterForPaintFragment;

import java.util.ArrayList;
import java.util.List;

public class PaintListFragment extends Fragment {

    RecyclerView recyclerView;
    FloatingActionButton addButton;
    GridPaintListAdapterForPaintFragment adapter;
    SharedPreferences data = App.getContext().getSharedPreferences("paint_list", Context.MODE_PRIVATE);
    private List<PaintItem> list = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paint_list, container, false);
        for (String key : data.getAll().keySet())
            list.add(new PaintItem().setTitle(key).setData(hexStringToByteArray(data.getString(key, null))));
        recyclerView = view.findViewById(R.id.paint_list);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this.getContext(), 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {
                return 1;
            }
        });
        recyclerView.setLayoutManager(gridLayoutManager);
        adapter = new GridPaintListAdapterForPaintFragment(list, this);
        recyclerView.setAdapter(adapter);
        addButton = view.findViewById(R.id.btn_add);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), PaintActivity.class);
                PaintListFragment.this.startActivityForResult(intent, 1);
            }
        });
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                boolean delete = intent.getBooleanExtra("delete", false);
                String title = intent.getStringExtra("title");
                if (delete) {
                    for (PaintItem item : list) {
                        if (item.getTitle().equals(title)) {
                            list.remove(item);
                            break;
                        }
                    }
                    this.data.edit().remove(title).apply();
                }
                else {
                    String oldTitle = intent.getStringExtra("old_title");
                    SharedPreferences.Editor editor = this.data.edit();
                    String d = intent.getStringExtra("data");
                    if (oldTitle != null) {
                        for (PaintItem item : list) {
                            if (item.getTitle().equals(oldTitle)) {
                                item.setTitle(title);
                                item.setData(hexStringToByteArray(d));
                                break;
                            }
                        }
                        editor.remove(oldTitle);
                        editor.putString(title, d);
                    } else {
                        list.add(new PaintItem().setTitle(title).setData(hexStringToByteArray(d)));
                        editor.putString(title, d);
                    }
                    editor.apply();
                }
                adapter.notifyDataSetChanged();
            }
        }
    }

    public static String byteArrayToHexString(byte[] byteArray) {
        if (byteArray == null)
            return null;
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    @NonNull
    public static byte[] hexStringToByteArray(String str) {
        if (str == null)
            return new byte[0];
        if (str.length() == 0)
            return new byte[0];
        byte[] byteArray = new byte[str.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            String subStr = str.substring(2 * i, 2 * i + 2);
            byteArray[i] = ((byte)Integer.parseInt(subStr, 16));
        }
        return byteArray;
    }
}
