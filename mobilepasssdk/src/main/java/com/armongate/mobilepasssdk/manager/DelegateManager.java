package com.armongate.mobilepasssdk.manager;

import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.delegate.PassFlowDelegate;

public class DelegateManager {

    private static DelegateManager      mInstance                   = null;
    private static PassFlowDelegate     mCurrentPassFlowDelegate    = null;
    private static MobilePassDelegate   mCurrentMobilePassDelegate  = null;

    private DelegateManager() {

    }

    public static DelegateManager getInstance() {
        if (mInstance == null) {
            mInstance  = new DelegateManager();
        }

        return mInstance;
    }

    public PassFlowDelegate getCurrentPassFlowDelegate() {
        return mCurrentPassFlowDelegate;
    }
    public MobilePassDelegate getCurrentMobilePassDelegate() { return mCurrentMobilePassDelegate; }

    public void setCurrentPassFlowDelegate(PassFlowDelegate listener) {
        mCurrentPassFlowDelegate = listener;
    }

    public void setCurrentMobilePassDelegate(MobilePassDelegate listener) {
        mCurrentMobilePassDelegate = listener;
    }

}
