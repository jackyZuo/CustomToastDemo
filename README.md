由于项目需要,需要再用户禁止通知权限之后仍然能弹出Toast.网上现成的尝试了三种方法测试之后发现都不太符合公司的需求而没有使用,其中机型适配(点名小米MIUI8)占了很大部分原因.而且要求是不能加任何权限!!!

效果图:


   ![使用系统Toast和禁止权限后的自定义Toast](http://img.blog.csdn.net/20170223171810795?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvcXFfMzQ2NzczMTc=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)
   
- **使用方法很简单,没有做二次封装.上代码**
```
public void showToast(View view){
        if(CustomToast.isNotificationEnabled(this)){
            //通知可用使用原生Toast
            Toast.makeText(getApplicationContext(), "系统Toast", Toast.LENGTH_SHORT).show();
        }else{
            CustomToast.makeText(getApplicationContext(),"自定义Toast", CustomToast.LENGTH_SHORT).show();
        }
    }
```
首先判断用户是否禁止了通知(有的时候其实用户只是想屏蔽推送通知),如果未禁止直接使用系统原生的Toast,否则用我们自定义的
判断是否有权限的代码就不贴了,跟本文关系不大.需要注意的是,API19以上才可以判断权限,所以可以加一个判断

```
public static boolean isNotificationEnabled(Context context) {
        //API 19以下的直接使用自定义的Toast
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            //这里19以下的我直接使用了CustomToast
            return false;
        }
        AppOpsManager mAppOps = null;
        mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        //此处省略N行代码
        ************;
}
```

```
 public static CustomToast makeText(Context context, CharSequence text, int duration) {
        Toast toast = Toast.makeText(context, text, duration);
        CustomToast exToast = new CustomToast(context);
        exToast.toast = toast;
        exToast.setDuration(duration);
        exToast.setText(text);
        //添加到集合中,自己维护队列
        equeue.add(exToast);
        return exToast;
    }
```
makeText就返回了一个实例对象,并把这个对象加到了equeue集合中.为了仿照系统弹Toast的时候一个显示完毕再弹出第二个.下面看show方法了.

```
 public void show() {
        //正在展示 或者 要展示的view为null
        if (isShow || mView == null) {
            return;
        }
        isShow = true;
        toast.setView(mView);
        //这里通过反射拿到toast的TN对象
        initTN();
        try {
            //展示toast
            show.invoke(mTN);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Toast到时间隐藏
        handler.postDelayed(hideRunnable, mDuration == LENGTH_SHORT ? LENGTH_SHORT : LENGTH_LONG);
    }
```
hideRunnable只是调了一个hide方法

```
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
```
该方法出自 [突破小米悬浮窗权限控制–不需要权限的悬浮窗](http://www.codexiu.cn/android/blog/25708/)
```
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
        //这里设置toast的位置
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 200);
    }
```

简单的介绍到这里了.欢迎交流,
参考文章:http://www.codexiu.cn/android/blog/25708/

博客地址:http://blog.csdn.net/qq_34677317/article/details/56676970
