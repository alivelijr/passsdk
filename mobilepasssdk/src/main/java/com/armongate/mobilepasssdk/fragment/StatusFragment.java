package com.armongate.mobilepasssdk.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.view.GIFView;

public class StatusFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        GIFView gifImageView = view.findViewById(R.id.gifViewer);
        gifImageView.setGifImageResource(R.drawable.failed);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i("MobilePass", "Attach fragment > Status");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i("MobilePass", "Detach fragment > Status");
    }
}
