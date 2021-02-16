package com.armongate.mobilepasssdk.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRCodeReaderFragment extends Fragment implements SurfaceHolder.Callback, Detector.Processor<Barcode> {

    private CameraSource cameraSource;
    private BarcodeDetector detector;
    private boolean isQRFound = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_qrcode_reader, container, false);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // askForCameraPermission();
        } else {
            setupControls((SurfaceView)view.findViewById(R.id.qrSurfaceView));
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i("MobilePass", "Attach fragment > QR Code Reader");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i("MobilePass", "Detach fragment > QR Code Reader");
    }


    private void setupControls(SurfaceView cameraSV) {
        detector = new BarcodeDetector.Builder(getContext()).setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(getContext(), detector).setAutoFocusEnabled(true).build();

        cameraSV.getHolder().addCallback(this);

        detector.setProcessor(this);
    }

    /*
    private void askForCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 1001);
    }
     */

    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupControls();
            } else {
                Toast.makeText(getApplicationContext(), "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
     */

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            cameraSource.start(surfaceHolder);
        } catch (Exception exception) {
            Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        cameraSource.stop();
    }

    @Override
    public void release() {

    }

    @Override
    public void receiveDetections(Detector.Detections<Barcode> detections) {
        if (detections != null && detections.getDetectedItems() != null && detections.getDetectedItems().size() > 0) {
            SparseArray<Barcode> qrCodes = detections.getDetectedItems();
            Barcode code = qrCodes.valueAt(0);

            Pattern sPattern = Pattern.compile("https://app.armongate.com/(rq|bd|o|s)/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(/[0-2]{1})?()?$");

            if (!isQRFound) {
                Matcher matcher = sPattern.matcher(code.displayValue);
                if (matcher.matches()) {
                    isQRFound = true;

                    String parsedContent = matcher.group(1) + "/" + matcher.group(2);

                    if (matcher.group(1).equals("rq")) {
                        parsedContent += matcher.group(3);
                    }

                    if (DelegateManager.getInstance().getCurrentPassFlowDelegate() != null) {
                        DelegateManager.getInstance().getCurrentPassFlowDelegate().onQRCodeFound(parsedContent);
                    }
                } else {
                    Log.i("MobilePass", "Unknown QR Code!");
                }
            }
        }
    }

}
