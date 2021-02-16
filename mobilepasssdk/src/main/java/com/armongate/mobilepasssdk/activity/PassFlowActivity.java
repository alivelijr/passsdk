package com.armongate.mobilepasssdk.activity;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.delegate.PassFlowDelegate;
import com.armongate.mobilepasssdk.fragment.MapFragment;
import com.armongate.mobilepasssdk.fragment.QRCodeReaderFragment;
import com.armongate.mobilepasssdk.fragment.StatusFragment;
import com.armongate.mobilepasssdk.manager.DelegateManager;

public class PassFlowActivity extends AppCompatActivity implements PassFlowDelegate {

    public PassFlowActivity() {
        super(R.layout.activity_pass_flow);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set flow delegate listener for activities
        DelegateManager.getInstance().setCurrentPassFlowDelegate(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment, QRCodeReaderFragment.class, null)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (DelegateManager.getInstance().getCurrentMobilePassDelegate() != null) {
            DelegateManager.getInstance().getCurrentMobilePassDelegate().onPassCancelled();
        }
    }

    @Override
    public void onQRCodeFound(String code) {
        Log.i("MobilePass", "QR Code received > " + code);
        replaceFragment(MapFragment.class);
    }

    @Override
    public void onLocationValid() {
        Log.i("MobilePass", "User location is valid");
        replaceFragment(StatusFragment.class);
    }

    private void replaceFragment(Class newFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment, newFragment, null)
                .setReorderingAllowed(true)
                .commit();
    }
}
