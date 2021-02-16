package com.armongate.mobilepasssdk.delegate;

public interface PassFlowDelegate {
    void onQRCodeFound(String code);
    void onLocationValid();
}
