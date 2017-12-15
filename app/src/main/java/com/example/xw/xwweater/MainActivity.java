package com.example.xw.xwweater;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.mtp.MtpObjectInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.example.xw.app.MyApplication;
import com.example.xw.bean.City;
import com.example.xw.bean.TodayWeather;
import com.example.xw.service.MyService;
import com.example.xw.util.NetUtil;
import com.umeng.analytics.MobclickAgent;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener,ViewPager.OnPageChangeListener{

    private static final int UPDATE_TODAY_WEATHER = 1;

    private ArrayList<TodayWeather> mWeathersList;

    private LocationClient mLocationClient;
    private BDLocationListener mBDLocationListener;

    private MyBroadcast myBroadcast;// 广播，用来接收service的消息
    private Intent serviceIntent;
    private MyService myService;
    private MyService.MyBinder myBinder;

    // 布局文件中的控件
    private ImageView mUpdateBtn;
    private ProgressBar mProgressAnim;
    private ImageView mCitySelect;
    private String lastCityCode;//最近一次显示的城市的代号，使得启动应用显示上次退出之前的画面
    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv,temperatureTv, climateTv, windTv, city_name_Tv, nowtemperature;
    private ImageView weatherImg, pmImg;
    private String lastPM25;

    // 未来6天天气显示所需变量
    private ViewPagerAdapter viewPagerAdapter;
    private ViewPager vp;
    private List<View> views;
    private ImageView[] dots;
    private int[] ids = {R.id.main_iv1, R.id.main_iv2};

    // 昨天
    private TextView w1_date1, w1_temperature1, w1_climate1, w1_wind1;
    private ImageView w1_img1;

    // 今天
    private TextView w1_date2, w1_temperature2, w1_climate2, w1_wind2;
    private ImageView w1_img2;

    // 未来第1天
    private TextView w1_date3, w1_temperature3, w1_climate3, w1_wind3;
    private ImageView w1_img3;

    // 未来第2天
    private TextView w1_date4, w1_temperature4, w1_climate4, w1_wind4;
    private ImageView w1_img4;

    // 未来第3天
    private TextView w1_date5, w1_temperature5, w1_climate5, w1_wind5;
    private ImageView w1_img5;

    // 未来第4天
    private TextView w1_date6, w1_temperature6, w1_climate6, w1_wind6;
    private ImageView w1_img6;

    // 该函数用于处理子线程发送的msg消息
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case UPDATE_TODAY_WEATHER:
                    //updateTodayWeather((TodayWeather) msg.obj);//根据子线程发回来的天气信息，更新当前界面的显示
                    updateTodayWeather((ArrayList<TodayWeather>) msg.obj);//根据子线程发回来的天气信息，更新当前界面的显示
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onStart() {

        mBDLocationListener = new MyLocationListener();
        mLocationClient = ((MyApplication)getApplication()).mLocationClient;
        mLocationClient.registerLocationListener(mBDLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");   // 设置坐标类型
        option.setIsNeedAddress(true);  //设置是否需要地址信息，默认不需要
        //option.setScanSpan(1000);     //多久定位一次
        mLocationClient.setLocOption(option);
        mLocationClient.start();

        myBroadcast = new MyBroadcast();

        IntentFilter filter = new IntentFilter();
        filter.addAction("SERVICE");

        registerReceiver(myBroadcast, filter);
        serviceIntent = new Intent(this, MyService.class);

        // 获取以往数据
        SharedPreferences sharedPreferences = getSharedPreferences("XW", MODE_PRIVATE);
        String cityCode = sharedPreferences.getString("main_city_code", "101010100");
        String address;
        if( lastCityCode != null ){
            address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + lastCityCode;
        }else{
            address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        }
        serviceIntent.putExtra("url", address);

        bindService(serviceIntent, conn, Context.BIND_AUTO_CREATE);

        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUpdateBtn = (ImageView)findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);
        mProgressAnim = (ProgressBar)findViewById(R.id.title_update_progress);

        // 测试网络是否联通
        if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
            Log.d("myWeather", "网络OK");
            Toast.makeText(MainActivity.this,"网络OK！", Toast.LENGTH_LONG).show();
        }else {
            Log.d("myWeather", "网络挂了");
            Toast.makeText(MainActivity.this,"网络挂了！", Toast.LENGTH_LONG).show();
        }

        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);//添加点击事件

        mWeathersList = new ArrayList<TodayWeather>();

        //getDeviceInfo(getApplicationContext());

        // 初始化今日天气界面
        initView();

        // 初始化未来6天天气界面
        initViews();

        // 初始化滑动标号
        initDots();
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //打印出当前的城市名
            String cityName = location.getCity().substring(0,location.getCity().length()-1 );
            Toast.makeText(MainActivity.this, cityName, Toast.LENGTH_SHORT).show();

            // 根据城市名称找到cityCode,更新天气信息
            List<City> cityList = ((MyApplication)getApplication()).getCityList();
            for(City tmp: cityList){
                if(tmp.getCity().equals(cityName)){
                    lastCityCode = tmp.getNumber();
                    queryWeatherCode(lastCityCode);
                }
            }

            //location.getLongitude();    获取当前位置经度
            //location.getLatitude();     获取当前位置纬度
        }
    }

    public class MyBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<TodayWeather> weathers = (ArrayList<TodayWeather>)intent.getSerializableExtra("wList");

            if(weathers != null){
                //System.out.println(weathers.get(0));
                updateTodayWeather(weathers);
            }
        }
    }

    void initViews(){
        LayoutInflater inflater = LayoutInflater.from(this);
        views = new ArrayList<View>();
        views.add(inflater.inflate(R.layout.weather_page1, null));
        views.add(inflater.inflate(R.layout.weather_page2, null));
        viewPagerAdapter = new ViewPagerAdapter(views, this);
        vp = (ViewPager)findViewById(R.id.nextWth_viewpager);
        vp.setAdapter(viewPagerAdapter);
        vp.setOnPageChangeListener(this);

        // 初始化所有控件
        w1_date1 = (TextView)views.get(0).findViewById(R.id.w1_date1);
        w1_temperature1 = (TextView)views.get(0).findViewById(R.id.w1_temperature1);
        w1_climate1 = (TextView)views.get(0).findViewById(R.id.w1_climate1);
        w1_wind1 = (TextView)views.get(0).findViewById(R.id.w1_wind1);
        w1_img1 = (ImageView)views.get(0).findViewById(R.id.w1_img1);

        w1_date2 = (TextView)views.get(0).findViewById(R.id.w1_date2);
        w1_temperature2 = (TextView)views.get(0).findViewById(R.id.w1_temperature2);
        w1_climate2 = (TextView)views.get(0).findViewById(R.id.w1_climate2);
        w1_wind2 = (TextView)views.get(0).findViewById(R.id.w1_wind2);
        w1_img2 = (ImageView)views.get(0).findViewById(R.id.w1_img2);

        w1_date3 = (TextView)views.get(0).findViewById(R.id.w1_date3);
        w1_temperature3 = (TextView)views.get(0).findViewById(R.id.w1_temperature3);
        w1_climate3 = (TextView)views.get(0).findViewById(R.id.w1_climate3);
        w1_wind3 = (TextView)views.get(0).findViewById(R.id.w1_wind3);
        w1_img3 = (ImageView)views.get(0).findViewById(R.id.w1_img3);

        w1_date4 = (TextView)views.get(1).findViewById(R.id.w2_date1);
        w1_temperature4 = (TextView)views.get(1).findViewById(R.id.w2_temperature1);
        w1_climate4 = (TextView)views.get(1).findViewById(R.id.w2_climate1);
        w1_wind4 = (TextView)views.get(1).findViewById(R.id.w2_wind1);
        w1_img4 = (ImageView)views.get(1).findViewById(R.id.w2_img1);

        w1_date5 = (TextView)views.get(1).findViewById(R.id.w2_date2);
        w1_temperature5 = (TextView)views.get(1).findViewById(R.id.w2_temperature2);
        w1_climate5 = (TextView)views.get(1).findViewById(R.id.w2_climate2);
        w1_wind5 = (TextView)views.get(1).findViewById(R.id.w2_wind2);
        w1_img5 = (ImageView)views.get(1).findViewById(R.id.w2_img2);

        w1_date6 = (TextView)views.get(1).findViewById(R.id.w2_date3);
        w1_temperature6 = (TextView)views.get(1).findViewById(R.id.w2_temperature3);
        w1_climate6 = (TextView)views.get(1).findViewById(R.id.w2_climate3);
        w1_wind6 = (TextView)views.get(1).findViewById(R.id.w2_wind3);
        w1_img6 = (ImageView)views.get(1).findViewById(R.id.w2_img3);
    }

    void initDots(){
        dots = new ImageView[views.size()];
        for(int i = 0; i < views.size(); i++){
            dots[i] = (ImageView)findViewById(ids[i]);
        }
    }

    // 初始化程序启动界面
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
        nowtemperature = (TextView)findViewById(R.id.nowtemperature) ;

        // 获取SharedPreferences中的内容
        SharedPreferences sharedPreferences = getSharedPreferences("XW",MODE_PRIVATE);
        // 存储最近搜索过的城市代号
        lastCityCode = sharedPreferences.getString("main_city_code","101010100");
        // 如果sharedPreferences中没有内容，则初始化为N/A
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
            nowtemperature.setText("N/A");
        }else{// sharedPreference中有内容，则按其中的内容对启动界面进行初始化
            city_name_Tv.setText(sharedPreferences.getString("city_name_Tv","N/A"));
            cityTv.setText(sharedPreferences.getString("cityTv","N/A"));
            timeTv.setText(sharedPreferences.getString("timeTv","N/A"));
            humidityTv.setText(sharedPreferences.getString("humidityTv","N/A"));
            lastPM25 = sharedPreferences.getString("pmDataTv","N/A");
            pmDataTv.setText(lastPM25);
            pmQualityTv.setText(sharedPreferences.getString("pmQualityTv","N/A"));
            weekTv.setText(sharedPreferences.getString("weekTv","N/A"));
            temperatureTv.setText(sharedPreferences.getString("temperatureTv","N/A"));
            climateTv.setText(sharedPreferences.getString("climateTv","N/A"));
            windTv.setText(sharedPreferences.getString("windTv","N/A"));
            nowtemperature.setText(sharedPreferences.getString("nowtemperature","N/A"));
            updatePM25Image(sharedPreferences.getInt("pm25",66),pmImg);
            updateWeatherImage(sharedPreferences.getString("type","晴"),weatherImg);
        }
    }

    // 根据城市代号，在子线程中获取天气信息
    private void queryWeatherCode(String cityCode){
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey="+cityCode;
        Log.d("myWeather",address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                TodayWeather todayWeather = null;
                //ArrayList<TodayWeather> weathersList = new ArrayList<TodayWeather>();
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
                    mWeathersList = parseXML(responseStr);// 解析获得的xml格式的网页数据
                    //if(todayWeather != null){
                    if(!mWeathersList.isEmpty()){
                        //Log.d("myWeather",todayWeather.toString());
                        Log.d("myWeather",mWeathersList.get(0).toString());

                        // 将解析后得到的天气信息对象传回UI线程处理
                        Message msg = new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        //msg.obj=todayWeather;
                        msg.obj = mWeathersList;
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

    // 根据查询到的天气信息，更新界面显示的数据
    void updateTodayWeather(ArrayList<TodayWeather> weathersList){
        city_name_Tv.setText(weathersList.get(1).getCity()+"天气");
        cityTv.setText(weathersList.get(1).getCity());
        nowtemperature.setText("温度："+ weathersList.get(1).getWendu() + "℃");
        timeTv.setText(weathersList.get(1).getUpdatetime()+ "发布");
        humidityTv.setText("湿度："+weathersList.get(1).getShidu());
        if(weathersList.get(1).getPm25() != null){
            pmDataTv.setText(weathersList.get(1).getPm25());
            pmQualityTv.setText(weathersList.get(1).getQuality());
        }
        weekTv.setText(weathersList.get(1).getDate());
        temperatureTv.setText(weathersList.get(1).getHigh()+"~"+weathersList.get(1).getLow());
        climateTv.setText(weathersList.get(1).getType());
        windTv.setText("风力:" + weathersList.get(1).getFengli());
        int pm25Value = 0;
        if(weathersList.get(1).getPm25()!=null){
            pm25Value = Integer.parseInt(weathersList.get(1).getPm25());
            updatePM25Image(pm25Value, pmImg);
            updateWeatherImage(weathersList.get(1).getType(), weatherImg);
        }
        Toast.makeText(MainActivity.this,"更新成功！",Toast.LENGTH_SHORT).show();
        mUpdateBtn.setVisibility(View.VISIBLE);
        mProgressAnim.setVisibility(View.INVISIBLE);

        // 将本次查询得到的数据存入SharedPreferences，方便下次初始化时使用
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
        editor.putString("nowtemperature",nowtemperature.getText().toString());
        editor.putInt("pm25",pm25Value);
        editor.putString("type",weathersList.get(0).getType());
        editor.commit();//提交操作，很关键

        updateWeathers(weathersList);
    }

    void updateWeathers(ArrayList<TodayWeather> weathersList){
        w1_climate1.setText(weathersList.get(0).getType());
        w1_date1.setText(weathersList.get(0).getDate());
        w1_temperature1.setText(weathersList.get(0).getHigh()+"~"+weathersList.get(0).getLow());
        w1_wind1.setText(weathersList.get(0).getFengli());
        updateWeatherImage(weathersList.get(0).getType(), w1_img1);

        w1_climate2.setText(weathersList.get(1).getType());
        w1_date2.setText(weathersList.get(1).getDate());
        w1_temperature2.setText(weathersList.get(1).getHigh()+"~"+weathersList.get(1).getLow());
        w1_wind2.setText(weathersList.get(1).getFengli());
        updateWeatherImage(weathersList.get(1).getType(), w1_img2);

        w1_climate3.setText(weathersList.get(2).getType());
        w1_date3.setText(weathersList.get(2).getDate());
        w1_temperature3.setText(weathersList.get(2).getHigh()+"~"+weathersList.get(2).getLow());
        w1_wind3.setText(weathersList.get(2).getFengli());
        updateWeatherImage(weathersList.get(2).getType(), w1_img3);

        w1_climate4.setText(weathersList.get(3).getType());
        w1_date4.setText(weathersList.get(3).getDate());
        w1_temperature4.setText(weathersList.get(3).getHigh()+"~"+weathersList.get(3).getLow());
        w1_wind4.setText(weathersList.get(3).getFengli());
        updateWeatherImage(weathersList.get(3).getType(), w1_img4);

        w1_climate5.setText(weathersList.get(4).getType());
        w1_date5.setText(weathersList.get(4).getDate());
        w1_temperature5.setText(weathersList.get(4).getHigh()+"~"+weathersList.get(4).getLow());
        w1_wind5.setText(weathersList.get(4).getFengli());
        updateWeatherImage(weathersList.get(4).getType(), w1_img5);

        w1_climate6.setText(weathersList.get(5).getType());
        w1_date6.setText(weathersList.get(5).getDate());
        w1_temperature6.setText(weathersList.get(5).getHigh()+"~"+weathersList.get(5).getLow());
        w1_wind6.setText(weathersList.get(5).getFengli());
        updateWeatherImage(weathersList.get(5).getType(), w1_img6);
    }

    // 解析从网站上获取到的xml页面信息
    private ArrayList<TodayWeather> parseXML(String xmldata){

        // 获取当前时间，判断是白天还是晚上
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        String strHour = sdf.format(new Date());
        int hour = Integer.parseInt(strHour);
        boolean isDay;
        if( hour >= 0 && hour < 18){
            isDay = true;
        }else{
            isDay = false;
        }

        TodayWeather todayWeather = null;
        ArrayList<TodayWeather> weathersList = new ArrayList<TodayWeather>();
        int fengxiangCount = 0;
        int fengliCount = 0;
        int dateCount = 0;
        int highCount = 0;
        int lowCount = 0;
        int typeCount = 0;
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
                        // 根元素，创建一个天气对象
                        if(xmlPullParser.getName().equals("resp")){
                            todayWeather = new TodayWeather();
                        }

                        // 未来天气元素，创建一个天气对象
                        if(xmlPullParser.getName().equals("weather") && !weathersList.isEmpty()){
                            todayWeather = (TodayWeather) todayWeather.clone();
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
                            } else if (xmlPullParser.getName().equals("fengxiang")) {
                                eventType = xmlPullParser.next();
                                if( isDay && fengxiangCount == 0){
                                    todayWeather.setFengxiang(xmlPullParser.getText());
                                }
                                if( !isDay && fengxiangCount == 1){
                                    todayWeather.setFengxiang(xmlPullParser.getText());
                                }
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fx_1")) {
                                eventType = xmlPullParser.next();
                                if( isDay && fengxiangCount == 0){
                                    todayWeather.setFengxiang(xmlPullParser.getText());
                                }
                                if( !isDay && fengxiangCount == 1){
                                    todayWeather.setFengxiang(xmlPullParser.getText());
                                }
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fengli")) {
                                eventType = xmlPullParser.next();
                                //todayWeather.setFengli(xmlPullParser.getText());
                                if( isDay && fengliCount == 0 ){
                                    todayWeather.setFengli(xmlPullParser.getText());
                                }else
                                if( !isDay && fengliCount == 1 ){
                                    todayWeather.setFengli(xmlPullParser.getText());
                                }
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("fl_1") ) {
                                eventType = xmlPullParser.next();
                                //todayWeather.setFengxiang(xmlPullParser.getText());
                                if( isDay && fengliCount == 0 ){
                                    todayWeather.setFengli(xmlPullParser.getText());
                                }
                                if( !isDay && fengliCount == 1 ){
                                    todayWeather.setFengli(xmlPullParser.getText());
                                }
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("date")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("date_1")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("high")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            } else if (xmlPullParser.getName().equals("high_1")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            } else if (xmlPullParser.getName().equals("low")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            } else if (xmlPullParser.getName().equals("low_1")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }else if (xmlPullParser.getName().equals("type")) {
                                eventType = xmlPullParser.next();
                                //todayWeather.setType(xmlPullParser.getText());
                                if( isDay && typeCount == 0){
                                    todayWeather.setType(xmlPullParser.getText());
                                }
                                if( !isDay && typeCount == 1){
                                    todayWeather.setType(xmlPullParser.getText());
                                }
                                typeCount++;
                            } else if (xmlPullParser.getName().equals("type_1") ) {
                                eventType = xmlPullParser.next();
                                //todayWeather.setType(xmlPullParser.getText());
                                if( isDay && typeCount == 0){
                                    todayWeather.setType(xmlPullParser.getText());
                                }
                                if( !isDay && typeCount == 1){
                                    todayWeather.setType(xmlPullParser.getText());
                                }
                                typeCount++;
                            }
                        }
                        break;

                    // 判断当前事件是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        if(xmlPullParser.getName().equals("weather") || xmlPullParser.getName().equals("yesterday")) {
                            Log.d("myWeather",todayWeather.toString());
                            fengliCount = 0;
                            fengxiangCount = 0;
                            typeCount = 0;
                            weathersList.add(todayWeather);
                        }
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
        return weathersList;
    }

    // 控件的点击事件处理函数
    public void onClick(View view){

        // 如果点击的是选择城市按钮，则跳转到城市选择界面
        if(view.getId()==R.id.title_city_manager){
            Intent i = new Intent(this, SelectCity.class);
            startActivityForResult(i,1);// 等待跳转到的界面返回结果
        }

        // 如果点击更新按钮，则根据SharedPreferences中存储的城市代号，查询天气信息并更新显示
        if(view.getId()==R.id.title_update_btn){
            SharedPreferences sharedPreferences = getSharedPreferences("XW", MODE_PRIVATE);
            String citymode = sharedPreferences.getString("main_city_code", "101010100");//101160101
            Log.d("myWeather",citymode);

            mUpdateBtn.setVisibility(View.INVISIBLE);
            mProgressAnim.setVisibility(View.VISIBLE);

            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather", "网络OK");
                queryWeatherCode(citymode);// 根据城市代号查询天气，并更新显示
            }else {
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this,"网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 处理SelectCity返回的结果（选中的城市代号）
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String newCityCode = data.getStringExtra("cityCode");
            if(newCityCode != null) {
                lastCityCode = newCityCode;// 记录最近查询的城市代号

                // 修改Service中的网址
                Parcel in = Parcel.obtain();
                in.writeString("http://wthrcdn.etouch.cn/WeatherApi?citykey=" + lastCityCode);
                try {
                    myBinder.transact(0, in, null, IBinder.FLAG_ONEWAY );
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            Log.d("myWeather", "选择的城市代码为"+newCityCode);

            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather", "网络OK");
                queryWeatherCode(lastCityCode);// 更新界面显示
            } else {
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 根据pm2.5的值和天气，显示对应的图片
    void updatePM25Image(int pm25Value, ImageView pmImg){
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
    }

    void updateWeatherImage(String weatherStr, ImageView weatherImg){
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

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        for(int i = 0; i < ids.length; i++){
            if( i == position ){
                dots[i].setImageResource(R.drawable.page_indicator_focused);
            }else{
                dots[i].setImageResource(R.drawable.page_indicator_unfocused);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    protected void onDestroy() {
        mLocationClient.unRegisterLocationListener(mBDLocationListener);//取消注册的位置监听，以免内存泄露
        mLocationClient.stop();// 退出时销毁定位
        stopService(serviceIntent);
        super.onDestroy();
    }

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (MyService.MyBinder) service;
            myService = ((MyService.MyBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myService.stopSelf();
            unregisterReceiver(myBroadcast);
        }
    };

    @Override
    protected void onPause() {
        myService.stopSelf();
        unregisterReceiver(myBroadcast);
        super.onPause();

        // 友盟统计Activity访问次数
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 友盟统计Activity访问次数
        MobclickAgent.onResume(this);
    }

/*
    public static boolean checkPermission(Context context, String permission) {
        boolean result = false;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                Class<?> clazz = Class.forName("android.content.Context");
                Method method = clazz.getMethod("checkSelfPermission", String.class);
                int rest = (Integer) method.invoke(context, permission);
                if (rest == PackageManager.PERMISSION_GRANTED) {
                    result = true;
                } else {
                    result = false;
                }
            } catch (Exception e) {
                result = false;
            }
        } else {
            PackageManager pm = context.getPackageManager();
            if (pm.checkPermission(permission, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
                result = true;
            }
        }
        return result;
    }
    public static String getDeviceInfo(Context context) {
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String device_id = null;
            if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                device_id = tm.getDeviceId();
            }
            String mac = null;
            FileReader fstream = null;
            try {
                fstream = new FileReader("/sys/class/net/wlan0/address");
            } catch (FileNotFoundException e) {
                fstream = new FileReader("/sys/class/net/eth0/address");
            }
            BufferedReader in = null;
            if (fstream != null) {
                try {
                    in = new BufferedReader(fstream, 1024);
                    mac = in.readLine();
                } catch (IOException e) {
                } finally {
                    if (fstream != null) {
                        try {
                            fstream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            json.put("mac", mac);
            if (TextUtils.isEmpty(device_id)) {
                device_id = mac;
            }
            if (TextUtils.isEmpty(device_id)) {
                device_id = android.provider.Settings.Secure.getString(context.getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID);
            }
            json.put("device_id", device_id);
            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }*/

}
