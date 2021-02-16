package com.armongate.mobilepassapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.armongate.mobilepasssdk.MobilePass;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;

public class MainActivity extends AppCompatActivity implements MobilePassDelegate {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onTest(View v) {
        MobilePass aa = new MobilePass(this, "", "");
        aa.setDelegate(this);
        aa.triggerQRCodeRead();
    }

    @Override
    public void onPassCancelled() {
        Log.i("MobilePass", "Flow CANCELLED!");
    }

    @Override
    public void onPassCompleted() {

    }

    @Override
    public void needCameraPermission() {

    }

    @Override
    public void needLocationPermission() {

    }
}
