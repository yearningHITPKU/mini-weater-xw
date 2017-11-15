package com.example.xw;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.example.xw.xwweater.R;

/**
 * Created by xw on 2017/11/15.
 */

public class ClearEditText extends EditText implements View.OnFocusChangeListener, TextWatcher{

    private Drawable mClearDrawable;

    public ClearEditText(Context context){this(context, null);}

    public ClearEditText(Context context, AttributeSet attrs){
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public ClearEditText(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        mClearDrawable = getCompoundDrawables()[2];
        if(mClearDrawable == null){
            //mClearDrawable = getResources().getDrawable(R.drawable.emotionstore_progresscancelbtn);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        return super.onTouchEvent(event);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus){

    }

    protected void setClearIconVisible(boolean visible){

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int count, int after){

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after){

    }

    @Override
    public void afterTextChanged(Editable s){

    }

    public void setShakeAnimation(){
        //this.setAnimation(shakeAnimation(5));
    }

}
