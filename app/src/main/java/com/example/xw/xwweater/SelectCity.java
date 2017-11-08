package com.example.xw.xwweater;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.xw.app.MyApplication;
import com.example.xw.bean.City;

import java.util.List;

public class SelectCity extends Activity implements View.OnClickListener{

    private ImageView backBtn;
    private ListView mListView;
    private List<City> cityList;
    private String citycode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_city);

        backBtn = (ImageView) findViewById(R.id.title_back);
        backBtn.setOnClickListener(this);

        mListView = (ListView)findViewById(R.id.list_city);

        initViews();
    }

    public void initViews(){
        // 获取全局数据库中的数据
        MyApplication myApplication = (MyApplication)getApplication();
        cityList = myApplication.getCityList();

        String[] data = new String[cityList.size()];
        int i = 0;
        for(City city : cityList){
            data[i] = city.getCity();
            data[i] += "  ";
            data[i] += city.getNumber();
            System.out.println(data[i]);
            i++;
        }
        // 设置适配器：关联数据与布局
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(SelectCity.this, android.R.layout.simple_list_item_1, data);
        mListView.setAdapter(adapter);

        // 设置ListView条目点击事件
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(SelectCity.this, "你单击了:"+i, Toast.LENGTH_SHORT).show();
                citycode = cityList.get(i).getNumber();
                Intent intent = new Intent();
                intent.putExtra("cityCode", citycode);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    public void onClick(View view){
        if(view.getId()==R.id.title_back){
            Intent i = new Intent();
            i.putExtra("cityCode", citycode);
            setResult(RESULT_OK, i);
            finish();
        }
    }
}
