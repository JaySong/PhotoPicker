package com.jay.photopicker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jay.ui.PhotoPickerActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn1;
    private Button btn2;
    private TextView tvContent;
    private boolean isMultiSelect;
    private EditText etMax;
    private int defaultMaxCount = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btn1 = (Button) findViewById(R.id.btn1);
        btn2 = (Button) findViewById(R.id.btn2);
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);

        tvContent = (TextView) findViewById(R.id.tvContent);

        etMax = (EditText) findViewById(R.id.etMax);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(MainActivity.this, PhotoPickerActivity.class);

        switch (v.getId()) {
            case R.id.btn1://单选
                isMultiSelect = false;
                break;
            case R.id.btn2://多选
                isMultiSelect = true;
                Bundle bundle = new Bundle();
                bundle.putBoolean(PhotoPickerActivity.IS_MULTI_SELECT, true);

                String s = etMax.getText().toString();
                try {
                    defaultMaxCount = Integer.valueOf(s);
                } catch (NumberFormatException e) {
                    defaultMaxCount = 5;
                }
                bundle.putInt(PhotoPickerActivity.MAX_SELECT_SIZE, defaultMaxCount);
                intent.putExtras(bundle);
                break;
        }
        startActivityForResult(intent,1001);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            if (isMultiSelect) {
                //多选
                ArrayList<String> results = data.getStringArrayListExtra(PhotoPickerActivity.SELECT_RESULTS_ARRAY);
                tvContent.setText(null);
                if (results == null) {
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < results.size(); i++) {
                    sb.append(i+1).append('：').append(results.get(i)).append("\n");
                }
                tvContent.setText(sb.toString());

            }else{
                //单选
                String result = data.getStringExtra(PhotoPickerActivity.SELECT_RESULTS);
                tvContent.setText(result);
            }
        }
    }
}
