package cn.jacky.zuo.customtoastdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void showToast(View view){
        if(CustomToast.isNotificationEnabled(this)){
            Toast.makeText(getApplicationContext(), "系统Toast", Toast.LENGTH_SHORT).show();
        }else{
            CustomToast.makeText(getApplicationContext(),"自定义Toast", CustomToast.LENGTH_SHORT).show();
        }
    }
}
