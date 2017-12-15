package com.example.xw.app;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import com.baidu.location.LocationClient;
import com.example.xw.bean.City;
import com.example.xw.db.CityDB;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xw on 2017/11/1.
 */

// 应用程序类，一个APP中只有一个，单例模式
public class MyApplication extends Application{
    private static final String TAG = "MyAPP";

    private static MyApplication myApplication;//单例入口
    private CityDB mCityDB;//全局数据库
    public List<City> mCityList;//所有城市列表

    public LocationClient mLocationClient;//定位功能

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d( TAG, "MyApplication->OnCreate");
        myApplication = this;
        mCityDB = openCityDB();// 初始化APP的数据库
        initCityList();// 初始化城市列表
        mLocationClient = new LocationClient(this.getApplicationContext());

        PushAgent mPushAgent = PushAgent.getInstance(this);
        //注册推送服务，每次调用register方法都会回调该接口
        mPushAgent.register(new IUmengRegisterCallback() {

            @Override
            public void onSuccess(String deviceToken) {
                //注册成功会返回device token
                Log.d("mytoken", deviceToken );
            }

            @Override
            public void onFailure(String s, String s1) {

            }
        });

    }

    // 中能通过此方法获取该单例类的对象
    public static MyApplication getInstance(){
        return myApplication;
    }

    // 在子线程中查询数据库，初始化城市列表信息
    private void initCityList(){
        mCityList = new ArrayList<City>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                prepareCityList();
            }
        }).start();
    }

    // 初始化城市列表信息
    private boolean prepareCityList() {
        // 获得数据库中的数据
        mCityList = mCityDB.getAllCity();
        int i=0;
        for (City city : mCityList) {
            i++;
            String cityName = city.getCity();
            String cityCode = city.getNumber();
            //Log.d(TAG,cityCode+":"+cityName);
        }
        //Log.d(TAG,"i="+i);
        return true;
    }

    // 打开数据库
    public CityDB openCityDB(){
        String path = "/data"
                + Environment.getDataDirectory().getAbsolutePath()
                + File.separator + getPackageName()
                + File.separator + "databases1"
                + File.separator
                + CityDB.CITY_DB_NAME;
        File db = new File(path);
        Log.d(TAG,path);
        // 如果数据库文件不存在，先将文件拷贝到指定路径中
        if(!db.exists()){
            // 数据文件路径
            String pathfolder = "/data" + Environment.getDataDirectory().getAbsolutePath()
                    + File.separator + getPackageName()
                    + File.separator + "databases1"
                    + File.separator;
            File dirFirstFolder = new File(pathfolder);
            if(!dirFirstFolder.exists()){
                dirFirstFolder.mkdirs();
                Log.i("MyApp","mkdirs");
            }
            Log.i("MyApp","db is not exists");
            try {
                InputStream is = getAssets().open("city.db");
                FileOutputStream fos = new FileOutputStream(db);
                int len = -1;
                byte[] buffer = new byte[1024];
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    fos.flush();
                }
                fos.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
        return new CityDB(this, path);
    }

    public List<City> getCityList() {
        return mCityList;
    }
}
