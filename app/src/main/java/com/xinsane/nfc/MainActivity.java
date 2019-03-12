package com.xinsane.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.xinsane.nfc.fragment.AboutFragment;
import com.xinsane.nfc.fragment.CardListFragment;
import com.xinsane.nfc.fragment.PaintListFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private BottomNavigationView mBottomNavigationView;
    private NfcAdapter nfcAdapter;
    private PendingIntent nfcIntent;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_card_list:
                    mViewPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_paint_list:
                    mViewPager.setCurrentItem(1);
                    return true;
                case R.id.navigation_about:
                    mViewPager.setCurrentItem(2);
                    return true;
            }
            return false;
        }
    };

    private ViewPager.OnPageChangeListener mOnpageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) { }
        @Override
        public void onPageSelected(int i) {
            mBottomNavigationView.getMenu().getItem(i).setChecked(true);
        }
        @Override
        public void onPageScrollStateChanged(int i) { }
    };

    class NfcFragmentPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragments = new ArrayList<>();
        NfcFragmentPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            fragments.add(new CardListFragment());
            fragments.add(new PaintListFragment());
            fragments.add(new AboutFragment());
        }
        @Override
        public Fragment getItem(int i) {
            return fragments.get(i);
        }
        @Override
        public int getCount() {
            return fragments.size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewPager = findViewById(R.id.viewpager);
        mBottomNavigationView = findViewById(R.id.navigation);
        mBottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mViewPager.addOnPageChangeListener(mOnpageChangeListener);
        mViewPager.setAdapter(new NfcFragmentPagerAdapter(getSupportFragmentManager()));
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
        if (nfcAdapter != null)
            nfcAdapter.enableForegroundDispatch(this, nfcIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null)
            nfcAdapter.disableForegroundDispatch(this);
    }

}
