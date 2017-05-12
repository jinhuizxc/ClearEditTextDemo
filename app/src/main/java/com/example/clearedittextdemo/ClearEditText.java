package com.example.clearedittextdemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

/**
 * Created by song on 2017/5/9.
 */

public class ClearEditText extends EditText implements View.OnFocusChangeListener {
    /**
     * 普通类型
     */
    private static final int TYPE_NORMAL = -1;
    /**
     * 自带清除功能的类型
     */
    private static final int TYPE_CAN_CLEAR = 0;
    /**
     * 自带密码查看功能的类型
     */
    private static final int TYPE_CAN_WATCH_PWD = 1;

    /**
     * 是否开启查看密码，默认没有
     */
    private boolean openWatchPwd = false;

    /**
     * 控件是否有焦点
     */
    private boolean hasFoucs;

    private TypedArray ta;
    private int funType;
    private int closeId;
    private int openId;
    private Drawable mOpenDrawable;
    private Drawable rightDrawable;
    private OnRightClickListener onRightClickListener;

    public ClearEditText(Context context) {
        this(context, null);
    }

    public ClearEditText(Context context, AttributeSet attrs) {
        //声明此构造方法，才能在xml中声明更多的属性
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public ClearEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ta = context.obtainStyledAttributes(attrs, R.styleable.ClearEditText);

        funType = ta.getInt(R.styleable.ClearEditText_functionType, TYPE_NORMAL);

        //获取资源id
        closeId = ta.getResourceId(R.styleable.ClearEditText_closewatchwpd, R.mipmap.eye_close);
        openId = ta.getResourceId(R.styleable.ClearEditText_openwatchwpd, R.mipmap.eye_open);

        initView();//初始化
    }

    private void initView() {
        //设置焦点改变监听
        setOnFocusChangeListener(this);

        //获取EditText的DrawableRight,假如没有设置我们就使用默认的图片,左上右下
        Drawable leftDrawable = getCompoundDrawables()[0];
        rightDrawable = getCompoundDrawables()[2];

        if (rightDrawable == null) {//如果右侧没有图标
            if (funType == TYPE_CAN_CLEAR) {
                //清除功能，默认设置为叉号图片
                rightDrawable = getResources().getDrawable(R.drawable.delete_select);
            } else if (funType == TYPE_CAN_WATCH_PWD) {
                //查看密码
                rightDrawable = getResources().getDrawable(closeId);
                mOpenDrawable = getResources().getDrawable(openId);
            }
        }

        if (leftDrawable != null) {
            int leftWidth = ta.getDimensionPixelOffset(R.styleable.ClearEditText_leftDrawableWidth, leftDrawable.getIntrinsicWidth());
            int leftHeight = ta.getDimensionPixelOffset(R.styleable.ClearEditText_leftDrawableHeight, leftDrawable.getIntrinsicHeight());
            leftDrawable.setBounds(0, 0, leftWidth, leftHeight);
        }

        if (rightDrawable != null) {
            int rightWidth = ta.getDimensionPixelOffset(R.styleable.ClearEditText_rightDrawableWidth, rightDrawable.getIntrinsicWidth());
            int rightHeight = ta.getDimensionPixelOffset(R.styleable.ClearEditText_rightDrawableHeight, rightDrawable.getIntrinsicHeight());
            rightDrawable.setBounds(0, 0, rightWidth, rightHeight);

            if (mOpenDrawable != null) {
                mOpenDrawable.setBounds(0, 0, rightWidth, rightHeight);
            }
            if (funType == TYPE_CAN_CLEAR) {//清除功能
                String content = getText().toString().trim();//获取输入的文本内容
                if (!TextUtils.isEmpty(content)) {//如果不为空则显示右侧图标
                    setRightImageVisible(true);
                    setSelection(content.length());//设置光标在文本内容的最后
                } else {
                    setRightImageVisible(false);
                }
            } else {
                setRightImageVisible(true);
            }

            //监听输入框内容变化
            addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    if (textListener != null) {
                        textListener.beforeTextChanged(s, start, count, after);
                    }
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int count, int after) {
                    //如果是带有清除功能的类型，当文本内容发生变化的时候，根据内容的长度是否为0进行隐藏或显示
                    if (funType == TYPE_CAN_CLEAR) {
                        setRightImageVisible(s.length() > 0);
                    }
                    if (textListener != null) {
                        textListener.onTextChanged(s, start, count, after);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (textListener != null) {
                        textListener.afterTextChanged(s);
                    }
                }
            });
        }
        ta.recycle();
    }

    /**
     * 因为我们不能直接给EditText设置点击事件，所以我们用记住我们按下的位置来模拟点击事件
     * 当我们按下的位置 在  EditText的宽度 - 图标到控件右边的间距 - 图标的宽度  和
     * EditText的宽度 - 图标到控件右边的间距之间我们就算点击了图标，竖直方向就没有考虑
     */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (getCompoundDrawables()[2] != null) {
                boolean isTouch = event.getX() > (getWidth() - getTotalPaddingRight()) && (event.getX() < (getWidth() - getPaddingRight()));
                if (isTouch){
                    if (onRightClickListener == null){
                        if (funType == TYPE_CAN_CLEAR){
                            //如果没有设置右边图标的点击事件，并且带有清除功能，默认清除文本
                            this.setText("");
                        }else if (funType == TYPE_CAN_WATCH_PWD) {
                            //如果没有设置右边图标的点击事件，并且带有查看密码功能，点击切换密码查看方式
                            if (openWatchPwd) {
                                //变为密文 TYPE_CLASS_TEXT 和 TYPE_TEXT_VARIATION_PASSWORD 必须一起使用
                                this.setTransformationMethod(PasswordTransformationMethod.getInstance());
                                openWatchPwd = false;
                            } else {
                                //变为明文
                                this.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                                openWatchPwd = true;
                            }
                            switchWatchPwdImage();//切换图标
                        }
                    }else{
                        //如果有则回调
                        onRightClickListener.onClick(this);
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * 切换查看密码的图标
     */
    private void switchWatchPwdImage() {
        if (openWatchPwd) {
            //开启查看
            setCompoundDrawables(getCompoundDrawables()[0],
                    getCompoundDrawables()[1], mOpenDrawable, getCompoundDrawables()[3]);
        } else {
            //关闭查看
            setCompoundDrawables(getCompoundDrawables()[0],
                    getCompoundDrawables()[1], rightDrawable, getCompoundDrawables()[3]);
        }
    }

    /**
     * 设置右侧图标显示与隐藏
     *
     * @param b
     */
    private void setRightImageVisible(boolean b) {
        Drawable right = b ? rightDrawable : null;
        setCompoundDrawables(getCompoundDrawables()[0],
                getCompoundDrawables()[1], right, getCompoundDrawables()[3]);
    }

    private TextListener textListener;

    public void addTextListener(TextListener textListener) {
        this.textListener = textListener;
    }

    /**
     * 右边图标点击的回调
     */
    public interface OnRightClickListener {
        void onClick(EditText editText);
    }

    public void setOnRightClickListener(OnRightClickListener onRightClickListener) {
        this.onRightClickListener = onRightClickListener;
    }

    /**
     * 输入框文本变化的回调，如果需要进行多一些操作判断，则设置此listen替代TextWatcher
     */
    public interface TextListener {

        void onTextChanged(CharSequence s, int start, int count, int after);

        void beforeTextChanged(CharSequence s, int start, int count, int after);

        void afterTextChanged(Editable s);
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        this.hasFoucs = b;
        if (funType == TYPE_CAN_CLEAR){
            if (hasFoucs) {
                setRightImageVisible(getText().toString().length() > 0);
            } else {
                setRightImageVisible(false);
            }
        }
    }
}
