package com.example.xw.xwweater;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
    private EditText searchEdit;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_city);

        backBtn = (ImageView) findViewById(R.id.title_back);
        backBtn.setOnClickListener(this);

        mListView = (ListView)findViewById(R.id.list_city);
        searchEdit = (EditText) findViewById(R.id.search_edit);

        initViews();
    }

    public void initViews(){
        // 获取全局数据库中的数据
        MyApplication myApplication = (MyApplication)getApplication();
        cityList = myApplication.getCityList();

        // 构造ListView中需要显示的信息的数组
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
        adapter = new ArrayAdapter<String>(SelectCity.this, android.R.layout.simple_list_item_1, data);
        mListView.setAdapter(adapter);
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // 设置ListView条目点击事件
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(SelectCity.this, "你单击了:"+i, Toast.LENGTH_SHORT).show();
                //citycode = cityList.get(i).getNumber();
                String cityAndcode = adapter.getItem(i);
                citycode = cityAndcode.substring(cityAndcode.length()-9,cityAndcode.length());
                Intent intent = new Intent();
                intent.putExtra("cityCode", citycode);
                setResult(RESULT_OK, intent);
                finish();// 跳转到主界面，并返回选中的条目对应的城市代号
            }
        });
    }

    // 处理各个控件的点击事件
    public void onClick(View view){
        if(view.getId()==R.id.title_back){
            Intent i = new Intent();
            i.putExtra("cityCode", citycode);
            setResult(RESULT_OK, i);
            finish();// 返回上一个界面，并将citycode返回
        }
    }
}
