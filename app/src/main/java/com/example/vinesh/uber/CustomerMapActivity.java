package com.example.vinesh.uber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private FusedLocationProviderApi fusedLocationProvider = LocationServices.FusedLocationApi;
    private double mLatitude, mLongitude;
    private Button mLogout, mRequest;
    private LatLng pickLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);
        mLogout.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
        );

        mRequest.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View v){
                        mLatitude = mLastLocation.getLatitude();
                        mLongitude = mLastLocation.getLongitude();
                        pickLocation = new LatLng(mLatitude, mLongitude);
                        String userId = FirebaseAuth.getInstance().getUid();
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                        GeoFire geoFire = new GeoFire(ref);
                        geoFire.setLocation(userId, new GeoLocation(mLatitude, mLongitude));

                        mMap.addMarker(
                                new MarkerOptions().position(pickLocation).title("Pickup location")
                        );

                        mRequest.setText("Finding your driver");
                    }
                }
        );
    }
    private int radius= 1;
    private void getCloestDriver(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(pickLocation.latitude, pickLocation.longitude), radius);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPrmissionsLocation();
            return;
        }
        //current location icon
        mMap.setMyLocationEnabled(true);
        buildGoogleClientApi();
    }

    private synchronized void buildGoogleClientApi() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        mLatitude = mLastLocation.getLatitude();
        mLongitude = mLastLocation.getLongitude();
        LatLng latLng = new LatLng(mLatitude, mLongitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(11));

//        String userId = FirebaseAuth.getInstance().getUid();
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");
//        GeoFire geoFire = new GeoFire(ref);
//        geoFire.setLocation(userId, new GeoLocation(mLatitude, mLongitude));

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10*1000);
        mLocationRequest.setFastestInterval(5*1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        requestLocationUpdate();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestLocationUpdate(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            requestPrmissionsLocation();
            return ;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPrmissionsLocation(){
        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET
                }, 10
        );
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();
        String userId = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }

}
