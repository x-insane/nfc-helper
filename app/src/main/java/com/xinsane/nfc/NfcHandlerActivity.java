package com.xinsane.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.xinsane.nfc.data.PaintItem;
import com.xinsane.util.LogUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class NfcHandlerActivity extends AppCompatActivity {
    public static List<PaintItem> list = null;

    private TextView mTextMessage;
    private NfcAdapter nfcAdapter;
    private PendingIntent nfcIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_handler);
        mTextMessage = findViewById(R.id.message);
        Intent intent = getIntent();
        if (intent != null) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null)
                handleCard(tag);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), 0);
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null)
            handleCard(tag);
    }

    boolean auth(MifareClassic mfc, int j) throws IOException {
        boolean auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_NFC_FORUM);
        if (!auth)
            auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_DEFAULT);
        if (!auth)
            auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY);
        return auth;
        /*
         * MifareClassic.KEY_DEFAULT; // FFFFFFFFFFFF
         * MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY; // A0A1A2A3A4A5
         * MifareClassic.KEY_NFC_FORUM; // D3F7D3F7D3F7
         */
    }

    private void handleCard(Tag tag) {
        StringBuilder text = new StringBuilder();

        if (tag != null) {
            text.append("ID: ").append(bytesToHex(tag.getId())).append("\n\n");
            String[] techList = tag.getTechList();
            text.append(Arrays.toString(techList)).append("\n\n");
            MifareClassic mfc = MifareClassic.get(tag);
            if (mfc != null) {
                try {
                    StringBuilder metaInfo = new StringBuilder();
                    mfc.connect();
                    int type = mfc.getType(); // 获取TAG的类型
                    int sectorCount = mfc.getSectorCount(); // 获取TAG中包含的扇区数
                    String typeS;
                    switch (type) {
                        case MifareClassic.TYPE_CLASSIC:
                            typeS = "TYPE_CLASSIC";
                            break;
                        case MifareClassic.TYPE_PLUS:
                            typeS = "TYPE_PLUS";
                            break;
                        case MifareClassic.TYPE_PRO:
                            typeS = "TYPE_PRO";
                            break;
                        case MifareClassic.TYPE_UNKNOWN:
                        default:
                            typeS = "TYPE_UNKNOWN";
                            break;
                    }
                    metaInfo.append("卡片类型：").append(typeS).append("\n")
                            .append("共 ").append(sectorCount).append(" 个扇区\n")
                            .append("共 ").append(mfc.getBlockCount()).append(" 个块\n")
                            .append("存储空间: ").append(mfc.getSize()).append(" B\n");

                    if (type != MifareClassic.TYPE_CLASSIC)
                        text.append("不支持的卡片类型");
                    else {
                        if (list != null && !list.isEmpty()) {
                            // 写数据
                            text.append("writing data...\n\n");
                            byte[] buffer = new byte[16];
                            buffer[0] = (byte) list.size();
                            write(mfc, buffer, 4);
                            int index = 0;
                            for (int i = 5; i < 64; i++) {
                                if (i % 4 == 3)
                                    continue;
                                byte[] data = list.get(index).getData();
                                System.arraycopy(data, 0, buffer, 0, 8);
                                index ++;
                                data = list.get(index).getData();
                                if (index < list.size()) {
                                    System.arraycopy(data, 0, buffer, 8, 8);
                                    index ++;
                                }
                                write(mfc, buffer, i);
                                if (index >= list.size())
                                    break;
                            }
                        }
                        // 读取TAG
                        for (int j = 0; j < sectorCount; j++) {
                            boolean auth = auth(mfc, j);
                            if (auth) {
                                metaInfo.append("Sector ").append(j).append(": 验证成功\n");
                                // 读取扇区中的块
                                int bCount = mfc.getBlockCountInSector(j);
                                int bIndex = mfc.sectorToBlock(j);
                                for (int i = 0; i < bCount; i++) {
                                    byte[] data = mfc.readBlock(bIndex);
                                    metaInfo.append("Block ").append(bIndex).append(": ")
                                            .append(bytesToHex(data)).append("\n");
                                    bIndex ++;
                                }
                            } else {
                                metaInfo.append("Sector ").append(j).append(": 验证失败\n");
                            }
                        }
                        text.append(metaInfo.toString());
                    }
                } catch (Exception e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } finally {
                    try {
                        mfc.close();
                    } catch (IOException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            mTextMessage.setText(text);
        }
    }

    private boolean write(MifareClassic mfc, byte[] bytes, int index) {
        try {
            if (!auth(mfc, index / 4))
                return false;
            mfc.writeBlock(index, bytes);
        } catch (IOException e) {
            LogUtil.e("IOException while writeMifareClassicBarCode MifareClassic: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 将数据写入扇，扇中每块必须16位
     */
    private Boolean writeMifareClassicBarCode(MifareClassic mfc, byte[] barCodeByte, int sectorIndex) {
        boolean flag = false;
        int bCount = mfc.getBlockCountInSector(sectorIndex);
        int bIndex = mfc.sectorToBlock(sectorIndex); // 获取block起始编号
        int barIndexCount = barCodeByte.length / 16 + 1;
        for (int i = 0; i < bCount - 1 && i < barIndexCount; i++){
            byte[] bar = new byte[16];
            for (int j = 0; j < 16; j++) {
                if (j + i*16 < barCodeByte.length)
                    bar[j] = barCodeByte[j + i*16];
                else
                    bar[j] = 0;
            }
            try {
                mfc.writeBlock(bIndex, bar);
                bIndex ++;
                if (i == bIndex - 2)
                    flag = true;
            } catch (IOException e) {
                LogUtil.e("IOException while writeMifareClassicBarCode MifareClassic: " + e.getMessage());
            }
        }
        return flag;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
