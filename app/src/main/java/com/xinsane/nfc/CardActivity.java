package com.xinsane.nfc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.xinsane.nfc.adapter.GridPaintListAdapterForCardActivity;
import com.xinsane.nfc.data.PaintItem;
import com.xinsane.nfc.fragment.PaintListFragment;

import java.util.ArrayList;
import java.util.List;

public class CardActivity extends AppCompatActivity {

    RecyclerView paintListView;
    RecyclerView cardContentListView;
    SharedPreferences data = App.getContext().getSharedPreferences("paint_list", Context.MODE_PRIVATE);
    private List<PaintItem> paintList = new ArrayList<>();
    private List<PaintItem> cardContentItemList = new ArrayList<>();
    GridPaintListAdapterForCardActivity paintListAdapter;
    GridPaintListAdapterForCardActivity cardContentAdapter;
    private NfcAdapter nfcAdapter;
    private PendingIntent nfcIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        for (String key : data.getAll().keySet())
            paintList.add(new PaintItem().setTitle(key).setData(PaintListFragment.hexStringToByteArray(data.getString(key, null))));
        paintListView = findViewById(R.id.paint_list);
        cardContentListView = findViewById(R.id.card_content);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {
                return 1;
            }
        });
        paintListView.setLayoutManager(gridLayoutManager);
        paintListAdapter = new GridPaintListAdapterForCardActivity(paintList, this, false);
        paintListView.setAdapter(paintListAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        cardContentListView.setLayoutManager(linearLayoutManager);
        cardContentAdapter = new GridPaintListAdapterForCardActivity(cardContentItemList, this, true);
        cardContentListView.setAdapter(cardContentAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, NfcHandlerActivity.class), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NfcHandlerActivity.list = cardContentItemList;
        if (nfcAdapter != null)
            nfcAdapter.enableForegroundDispatch(this, nfcIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null)
            nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NfcHandlerActivity.list = null;
    }

    public void onAdd(PaintItem item) {
        cardContentItemList.add(item);
        cardContentAdapter.notifyDataSetChanged();
    }

    public void onRemove(PaintItem item) {
        cardContentItemList.remove(item);
        cardContentAdapter.notifyDataSetChanged();
    }
}
