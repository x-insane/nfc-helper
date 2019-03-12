package com.xinsane.nfc.data;

public class PaintItem {
    private String title;
    private byte[] data;

    public String getTitle() {
        return title;
    }

    public PaintItem setTitle(String title) {
        this.title = title;
        return this;
    }

    public byte[] getData() {
        return data;
    }

    public PaintItem setData(byte[] data) {
        this.data = data;
        return this;
    }
}
