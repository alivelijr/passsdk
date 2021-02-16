package com.armongate.mobilepasssdk.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        SupportMapFragment supportMapFragment = (SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.google_maps);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        supportMapFragment.getMapAsync(this);

        view.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNext();
            }
        });

        return view;
    }


    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 42) {
            if (permissions.length == 1 &&
                    permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                initMap();
            }
        }
    }
     */

    @Override
    public boolean onMyLocationButtonClick() {
        Log.i("MobilePass", "My location button clicked!");

        /*
        if (DelegateManager.getInstance().getCurrentFlowDelegate() != null) {
            DelegateManager.getInstance().getCurrentFlowDelegate().onLocation();
        }
         */

        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Log.i("MobilePass", "My location clicked!");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            initMap();
        } else {
            // TODO End flow!
            // ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 42);
        }
    }

    private void initMap() {
        Log.i("MobilePass", "Init map now!");

        mMap.setMyLocationEnabled(true);

        LatLng sydney = new LatLng(39.88503538493434, 32.82159563359129);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Somewhere"));
        mMap.addCircle(new CircleOptions().center(sydney).radius(200).fillColor(0x20FF0000).strokeWidth(0));

        LatLng sydney2 = new LatLng(39.88063504980492, 32.82698887733094);
        mMap.addMarker(new MarkerOptions().position(sydney2).title("Marker in Somewhere"));
        mMap.addCircle(new CircleOptions().center(sydney2).radius(100).fillColor(0x20FF0000).strokeWidth(0));

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        initLocationTracking();
    }

    private void moveCamera(Location location) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f));
    }

    private void initLocationTracking() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    Log.i("MobilePass", "Location changed; Latitude: " + lastLocation.getLatitude() + ", Longitude: " + lastLocation.getLongitude());

                    Location dest = new Location(LocationManager.GPS_PROVIDER);
                    dest.setLatitude(39.88503538493434);
                    dest.setLongitude(32.82159563359129);
                    Log.i("MobilePass", "Distance: " + lastLocation.distanceTo(dest));

                    moveCamera(lastLocation);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(new LocationRequest(), locationCallback, null);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMap != null) {
            initLocationTracking();
        }
    }

    public void onNext() {
        DelegateManager.getInstance().getCurrentPassFlowDelegate().onLocationValid();
    }

}
