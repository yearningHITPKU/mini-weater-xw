package com.example.xw.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.xw.bean.City;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xw on 2017/11/1.
 */

// 数据库类，用于对数据库进行管理
public class CityDB {
    public static final String CITY_DB_NAME = "city.db";//数据库名称
    private static final String CITY_TABLE_NAME = "city";//表名称
    private SQLiteDatabase db;// 数据库

    // 构造函数，从手机path路径下，读取数据库文件，保存在db变量中
    public CityDB(Context context, String path) {
        db = context.openOrCreateDatabase(path, Context.MODE_PRIVATE, null);
    }

    // 查询数据库，并将数据以List<City>的方式返回
    public List<City> getAllCity() {
        List<City> list = new ArrayList<City>();
        Cursor c = db.rawQuery("SELECT * from " + CITY_TABLE_NAME, null);
        while (c.moveToNext()) {
            String province = c.getString(c.getColumnIndex("province"));
            String city = c.getString(c.getColumnIndex("city"));
            String number = c.getString(c.getColumnIndex("number"));
            String allPY = c.getString(c.getColumnIndex("allpy"));
            String allFirstPY = c.getString(c.getColumnIndex("allfirstpy"));
            String firstPY = c.getString(c.getColumnIndex("firstpy"));
            City item = new City(province, city, number, firstPY, allPY, allFirstPY);
            list.add(item);
        }
        return list;
    }
}
