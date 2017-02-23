package cn.jacky.zuo.customtoastdemo;


import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by jackyZuo on 2017/2/8 0006.
 * 自定义Toast
 */
public class CustomToast {

    public static final int LENGTH_SHORT = 2000;
    public static final int LENGTH_LONG = 3500;
    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

    private Toast toast;
    private int mDuration;
    private static boolean isShow = false;
    private static final ArrayList<CustomToast> equeue = new ArrayList<>();

    private Object mTN;
    private Method show;
    private Method hide;
    private TextView mView;
    private Handler handler = new Handler();


    public CustomToast(Context context) {
        LayoutInflater inflate = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //UI
        mView = (TextView) inflate.inflate(R.layout.custom_toast, null);
    }

    private Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Show the view for the specified duration.
     */

    public void show() {
        if (isShow || mView == null) {
            return;
        }
        isShow = true;
        toast.setView(mView);
        initTN();
        try {
            show.invoke(mTN);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Toast到时间隐藏
        handler.postDelayed(hideRunnable, mDuration == LENGTH_SHORT ? LENGTH_SHORT : LENGTH_LONG);
    }

    /**
     * Close the view if it's showing, or don't show it if it isn't showing yet.
     * You do not normally have to call this.  Normally view will disappear on its own
     * after the appropriate duration.
     */
    private void hide() {
        if (!isShow) return;

        try {
            hide.invoke(mTN);
        } catch (Exception e) {
            e.printStackTrace();
        }
        isShow = false;
        equeue.remove(0);
        //队列中还有消息则继续show
        if (equeue.size() > 0) {
            equeue.get(0).show();
        }
    }

    public static CustomToast makeText(Context context, CharSequence text) {
        return makeText(context, text, LENGTH_SHORT);
    }

    public static CustomToast makeText(Context context, CharSequence text, int duration) {

        Toast toast = Toast.makeText(context, text, duration);
        CustomToast exToast = new CustomToast(context);
        exToast.toast = toast;
        exToast.setDuration(duration);
        exToast.setText(text);

        equeue.add(exToast);
        return exToast;
    }

    public static CustomToast makeText(Context context, int resId, int duration) {
        String text = "";
        try {
            text = context.getResources().getString(resId);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        return makeText(context, text, duration);
    }

    public void setText(CharSequence s) {
        mView.setText(s);
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    /**
     * 反射拿到Toast的TN对象
     */
    private void initTN() {
        try {
            Field tnField = toast.getClass().getDeclaredField("mTN");
            tnField.setAccessible(true);
            mTN = tnField.get(toast);
            show = mTN.getClass().getMethod("show");
            hide = mTN.getClass().getMethod("hide");

            Field tnParamsField = mTN.getClass().getDeclaredField("mParams");
            tnParamsField.setAccessible(true);

            WindowManager.LayoutParams params = (WindowManager.LayoutParams) tnParamsField.get(mTN);
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

            /**调用tn.show()之前一定要先设置mNextView*/
            Field tnNextViewField = mTN.getClass().getDeclaredField("mNextView");
            tnNextViewField.setAccessible(true);
            tnNextViewField.set(mTN, toast.getView());
        } catch (Exception e) {
            e.printStackTrace();
        }
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
    }

    /**
     * 通知是否可用
     *
     * @param context
     * @return
     */
    public static boolean isNotificationEnabled(Context context) {

        //API 19以下的直接使用自定义的Toast
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            return false;
        }
        AppOpsManager mAppOps = null;
        mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo appInfo = context.getApplicationInfo();

        String pkg = context.getApplicationContext().getPackageName();

        int uid = appInfo.uid;
        Class appOpsClass = null; /* Context.APP_OPS_MANAGER */
        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());
            Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE, String.class);
            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
            int value = (int) opPostNotificationValue.get(Integer.class);
            return ((int) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}