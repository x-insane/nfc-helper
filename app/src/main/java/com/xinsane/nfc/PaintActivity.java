package com.xinsane.nfc;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.xinsane.nfc.fragment.PaintListFragment;
import com.xinsane.nfc.view.PaintView;

public class PaintActivity extends AppCompatActivity {

    private PaintView paintView;
    private String oldTitle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint);
        Button saveButton = findViewById(R.id.btn_save);
        Button deleteButton = findViewById(R.id.btn_delete);
        paintView = findViewById(R.id.paint_view);
        final EditText editText = findViewById(R.id.text_title);
        Intent intent = getIntent();
        if (intent != null) {
            oldTitle = intent.getStringExtra("title");
            editText.setText(oldTitle);
            paintView.setData(PaintListFragment.hexStringToByteArray(intent.getStringExtra("data")));
        }
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                if (oldTitle != null)
                    intent.putExtra("old_title", oldTitle);
                String title = editText.getText().toString();
                if (title.isEmpty())
                    title = PaintListFragment.byteArrayToHexString(paintView.getData());
                intent.putExtra("title", title);
                intent.putExtra("data", PaintListFragment.byteArrayToHexString(paintView.getData()));
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (oldTitle != null) {
                    Intent intent = new Intent();
                    intent.putExtra("title", oldTitle);
                    intent.putExtra("delete", true);
                    setResult(Activity.RESULT_OK, intent);
                } else
                    setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
    }
}
