package com.armongate.mobilepasssdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.armongate.mobilepasssdk.activity.PassFlowActivity;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.service.BaseService;

public class MobilePass {

    private Context mActiveContext;

    public MobilePass(Context context, String token, String serverUrl) {
        mActiveContext = context;
        BaseService.getInstance().setContext(context);
    }

    public void setDelegate(@Nullable MobilePassDelegate listener) {
        DelegateManager.getInstance().setCurrentMobilePassDelegate(listener);
    }

    public void setToken(String token, String language) {

    }

    public void triggerQRCodeRead() {
        // Open QR Code
        showActivity(PassFlowActivity.class);
    }

    private void showActivity(Class cls) {
        Intent intent = new Intent(mActiveContext, cls);
        final ComponentName component = new ComponentName(mActiveContext, cls);
        intent.setComponent(component);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActiveContext.startActivity(intent);
    }

    /*
    public void apiTest(AccessPointService.ServiceResultListener listener) {
        new AccessPointService().remoteOpen(listener);
    }

    public String test() {
        return CryptoManager.getInstance().test();
    }

    public String getPublicKey() {
        CryptoKeyPair kp = CryptoManager.getInstance().generateKeyPair();
        return kp.publicKey;
    }

    public void qr(Context sender) {
        mActiveContext = sender;

        // Set flow delegate listener for activities
        DelegateManager.getInstance().setCurrentFlowDelegate(this);

        // Open QR Code
        Intent intent = new Intent(sender, QRCodeReaderActivity.class);
        final ComponentName component = new ComponentName(sender, QRCodeReaderActivity.class);
        intent.setComponent(component);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sender.startActivity(intent);
    }
     */

}
