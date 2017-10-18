package com.example.xw.xwweater;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class SelectCity extends Activity implements View.OnClickListener{

    private ImageView backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_city);

        backBtn = (ImageView) findViewById(R.id.title_back);
        backBtn.setOnClickListener(this);
    }

    public void onClick(View view){
        if(view.getId()==R.id.title_back){
            Intent i = new Intent();
            i.putExtra("cityCode", "101160101");
            setResult(RESULT_OK, i);
            finish();
        }
    }
}
