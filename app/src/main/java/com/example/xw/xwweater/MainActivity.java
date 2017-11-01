package com.example.xw.xwweater;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xw.bean.TodayWeather;
import com.example.xw.util.NetUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity implements View.OnClickListener{

    private static final int UPDATE_TODAY_WEATHER = 1;

    private ImageView mUpdateBtn;
    private ImageView mCitySelect;
    private String lastCityCode;//最近一次显示的城市的代号，使得启动应用显示上次退出之前的画面

    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv,temperatureTv, climateTv, windTv, city_name_Tv;
    private ImageView weatherImg, pmImg;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case UPDATE_TODAY_WEATHER:
                    updateTodayWeather((TodayWeather) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUpdateBtn = (ImageView)findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);

        if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
            Log.d("myWeather", "网络OK");
            Toast.makeText(MainActivity.this,"网络OK！", Toast.LENGTH_LONG).show();
        }else {
            Log.d("myWeather", "网络挂了");
            Toast.makeText(MainActivity.this,"网络挂了！", Toast.LENGTH_LONG).show();
        }

        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);

        initView();
    }

    void initView(){
        city_name_Tv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);

        SharedPreferences sharedPreferences = getSharedPreferences("XW",MODE_PRIVATE);
        lastCityCode = sharedPreferences.getString("main_city_code","101010100");
        if(sharedPreferences == null){
            city_name_Tv.setText("N/A");
            cityTv.setText("N/A");
            timeTv.setText("N/A");
            humidityTv.setText("N/A");
            pmDataTv.setText("N/A");
            pmQualityTv.setText("N/A");
            weekTv.setText("N/A");
            temperatureTv.setText("N/A");
            climateTv.setText("N/A");
            windTv.setText("N/A");
        }else{
            city_name_Tv.setText(sharedPreferences.getString("city_name_Tv","N/A"));
            cityTv.setText(sharedPreferences.getString("cityTv","N/A"));
            timeTv.setText(sharedPreferences.getString("timeTv","N/A"));
            humidityTv.setText(sharedPreferences.getString("humidityTv","N/A"));
            pmDataTv.setText(sharedPreferences.getString("pmDataTv","N/A"));
            pmQualityTv.setText(sharedPreferences.getString("pmQualityTv","N/A"));
            weekTv.setText(sharedPreferences.getString("weekTv","N/A"));
            temperatureTv.setText(sharedPreferences.getString("temperatureTv","N/A"));
            climateTv.setText(sharedPreferences.getString("climateTv","N/A"));
            windTv.setText(sharedPreferences.getString("windTv","N/A"));
            updateImage(sharedPreferences.getInt("pm25",66),sharedPreferences.getString("type","晴"));
        }

    }

    private void queryWeatherCode(String cityCode){
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey="+cityCode;
        Log.d("myWeather",address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                TodayWeather todayWeather = null;
                try{
                    URL url = new URL(address);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(8000);
                    con.setReadTimeout(8000);
                    InputStream in = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String str;
                    while((str=reader.readLine()) != null){
                        response.append(str);
                        Log.d("myWeather",str);
                    }
                    String responseStr = response.toString();
                    Log.d("myWeather", responseStr);
                    todayWeather = parseXML(responseStr);
                    if(todayWeather != null){
                        Log.d("myWeather",todayWeather.toString());

                        Message msg = new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj=todayWeather;
                        mHandler.sendMessage(msg);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(con != null)
                        con.disconnect();
                }
            }
        }).start();
    }

    void updateTodayWeather(TodayWeather todayWeather){
        city_name_Tv.setText(todayWeather.getCity()+"天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime()+ "发布");
        humidityTv.setText("湿度："+todayWeather.getShidu());
        pmDataTv.setText(todayWeather.getPm25());
        pmQualityTv.setText(todayWeather.getQuality());
        weekTv.setText(todayWeather.getDate());
        temperatureTv.setText(todayWeather.getHigh()+"~"+todayWeather.getLow());
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力:"+todayWeather.getFengli());
        int pm25Value = Integer.parseInt(todayWeather.getPm25());
        updateImage(pm25Value,todayWeather.getType());

        Toast.makeText(MainActivity.this,"更新成功！",Toast.LENGTH_SHORT).show();

        SharedPreferences settings  = (SharedPreferences)getSharedPreferences("XW", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("city_name_Tv",city_name_Tv.getText().toString());
        editor.putString("cityTv",cityTv.getText().toString());
        editor.putString("timeTv",timeTv.getText().toString());
        editor.putString("humidityTv",humidityTv.getText().toString());
        editor.putString("pmDataTv",pmDataTv.getText().toString());
        editor.putString("pmQualityTv",pmQualityTv.getText().toString());
        editor.putString("weekTv",weekTv.getText().toString());
        editor.putString("temperatureTv",temperatureTv.getText().toString());
        editor.putString("climateTv",climateTv.getText().toString());
        editor.putString("windTv",windTv.getText().toString());
        editor.putString("main_city_code",lastCityCode);
        editor.putInt("pm25",pm25Value);
        editor.putString("type",todayWeather.getType());
        editor.commit();
    }

    private TodayWeather parseXML(String xmldata){
        TodayWeather todayWeather = null;
        int fengxiangCount=0;
        int fengliCount =0;
        int dateCount=0;
        int highCount =0;
        int lowCount=0;
        int typeCount =0;
        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            Log.d("myWeather", "parseXML");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    // 判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    // 判断当前事件是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if(xmlPullParser.getName().equals("resp")){
                            todayWeather= new TodayWeather();
                        }
                        if (todayWeather != null) {
                            if (xmlPullParser.getName().equals("city")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setCity(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("updatetime")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("shidu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setShidu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("wendu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setWendu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("pm25")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setPm25(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("quality")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setQuality(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli(xmlPullParser.getText());
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType(xmlPullParser.getText());
                                typeCount++;
                            }
                        }

                        break;


                    // 判断当前事件是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        break;
                }
                // 进入下一个元素并触发相应事件
                eventType = xmlPullParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return todayWeather;
    }

    public void onClick(View view){

        if(view.getId()==R.id.title_city_manager){
            Intent i = new Intent(this, SelectCity.class);
            startActivityForResult(i,1);
        }

        if(view.getId()==R.id.title_update_btn){
            SharedPreferences sharedPreferences = getSharedPreferences("XW", MODE_PRIVATE);
            String citymode = sharedPreferences.getString("main_city_code", "101010100");//101160101
            Log.d("myWeather",citymode);

            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather", "网络OK");
                queryWeatherCode(citymode);
            }else {
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this,"网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String newCityCode = data.getStringExtra("cityCode");
            lastCityCode = newCityCode;
            Log.d("myWeather", "选择的城市代码为"+newCityCode);

            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather", "网络OK");
                queryWeatherCode(newCityCode);
            } else {
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    void updateImage(int pm25Value, String weatherStr){
        if(pm25Value >= 0 && pm25Value <= 50){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
        }else if(pm25Value >= 51 && pm25Value <= 100){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_51_100);
        }else if(pm25Value >= 101 && pm25Value <= 150){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_101_150);
        }else if(pm25Value >= 151 && pm25Value <= 200){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_151_200);
        }else if(pm25Value >= 201 && pm25Value <= 300){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_201_300);
        }else if(pm25Value > 300){
            pmImg.setImageResource(R.drawable.biz_plugin_weather_greater_300);
        }

        if(weatherStr.equals("暴雪")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoxue);
        }else if(weatherStr.equals("暴雨")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoyu);
        }else if(weatherStr.equals("大暴雨")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_dabaoyu);
        }else if(weatherStr.equals("大雪")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_daxue);
        }else if(weatherStr.equals("大雨")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_dayu);
        }else if(weatherStr.equals("多云")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_duoyun);
        }else if(weatherStr.equals("雷阵雨")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_leizhenyu);
        }else if(weatherStr.equals("雷阵雨冰雹")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_leizhenyubingbao);
        }else if(weatherStr.equals("晴")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_qing);
        }else if(weatherStr.equals("沙尘暴")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_shachenbao);
        }else if(weatherStr.equals("特大暴雨")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_tedabaoyu);
        }else if(weatherStr.equals("雾")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_wu);
        }else if(weatherStr.equals("小雪")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_xiaoxue);
        }else if(weatherStr.equals("小雨")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_xiaoyu);
        }else if(weatherStr.equals("阴")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_yin);
        }else if(weatherStr.equals("雨夹雪")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_yujiaxue);
        }else if(weatherStr.equals("阵雪")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhenxue);
        }else if(weatherStr.equals("阵雨")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhenyu);
        }else if(weatherStr.equals("中雪")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhongxue);
        }else if(weatherStr.equals("中雨")){
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhongyu);
        }
    }
}
