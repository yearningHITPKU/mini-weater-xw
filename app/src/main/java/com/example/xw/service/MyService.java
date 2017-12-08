package com.example.xw.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.xw.bean.TodayWeather;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by xw on 2017/12/8.
 * 1、启动应用更新天气信息
 * 2、每个一定时间更新一次显示
 * 在后台运行
 */

public class MyService extends Service{

    private static final String TAG = "MyService";

    private Timer mTimer = new Timer();

    private ArrayList<TodayWeather> mWeatherList;

    private String mURL;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // 获取url
        mURL = intent.getStringExtra("url");
        return new MyBinder();
    }

    public class MyBinder extends Binder{
        public MyService getService(){
            return MyService.this;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"Service onCreate");

        // 开启时钟
        mTimer.schedule(task, 1, 5000);
    }

    // 每隔一定时间，从网上获取天气数据，并返回给MainActivity
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            HttpURLConnection con = null;
            try{
                URL url = new URL(mURL);
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
                    Log.d(TAG,str);
                }
                String responseStr = response.toString();
                Log.d(TAG, responseStr);
                mWeatherList = parseXML(responseStr);// 解析获得的xml格式的网页数据
                if(!mWeatherList.isEmpty()){
                    Log.d(TAG,mWeatherList.get(0).toString());
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(con != null)
                    con.disconnect();
            }

            Intent intent = new Intent();
            intent.setAction("SERVICE");
            intent.putExtra("wList",mWeatherList);
            sendBroadcast(intent);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"Service onStartCommand");

        // 获取url
        mURL = intent.getStringExtra("url");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
        super.onDestroy();
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
}
