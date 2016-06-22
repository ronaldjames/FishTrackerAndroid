package com.kodingwithkyle.kyle.fishtracker;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Iterator;


public class MainActivity extends AppCompatActivity implements LocationListener,
        OnMapReadyCallback,DialogFrag.DeleteFishListener,
        AddMarkerFragment.AddMarkerFragListener {

    private long rowId;
    private double myLatitude, myLongitude;
    private LocationManager locationManager;
    private Location myLocation;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    private Activity activity = this;
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set the layout
        setContentView(R.layout.activity_main);

        setUpMap();//setup the map
    }//end of onCreate()

    private void setUpMap() {
        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }//end setUpMap()

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }//end onResume()

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop method tracing that the activity started during onCreate()
        android.os.Debug.stopMethodTracing();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {

            case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setUpMap();
                } else {
                    Toast.makeText(this, "The app was not allowed to access your location. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                }
            }
            break;

            default: {
                break;
            }
        }
    }


    @Override
    public void onMapReady(GoogleMap map) {


        if (googleMap != null) {
            return;
        }
        //start the task to load the markers
        new LoadMarkersTask().execute((Object[]) null);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
            return;
        }

        googleMap = map;
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.setMyLocationEnabled(true);

        locationManager = (LocationManager) getSystemService(
                Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();

        String provider = locationManager.getBestProvider(criteria, true);

        myLocation = locationManager.getLastKnownLocation(provider);

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (myLocation != null) {
            Log.e("TAG", "GPS is on");
            myLatitude = myLocation.getLatitude();
            myLongitude = myLocation.getLongitude();

        } else {
            //This is what you need:
            locationManager.requestLocationUpdates(provider, 1000, 0, this);
        }
        LatLng latLng = new LatLng(myLatitude, myLongitude);

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        googleMap.animateCamera(CameraUpdateFactory.zoomTo(20));


        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {


                AddMarkerFragment addMarkerFragment = new AddMarkerFragment();

                Bundle arguments = new Bundle();
                arguments.putString("lat", latLng.latitude + "");
                arguments.putString("lng", latLng.longitude + "");
                addMarkerFragment.setArguments(arguments);
                getFragmentManager().beginTransaction().replace(R.id.map, addMarkerFragment).addToBackStack("a").commit();
            }
        });
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                DetailsFragment detailsFragment = new DetailsFragment();

                // get the rowId
                // specify rowID as an argument to the DetailsFragment
                Bundle arguments = new Bundle();
                arguments.putLong("rowId", Long.parseLong(marker.getTitle()));
                arguments.putDouble("lat", marker.getPosition().latitude);
                arguments.putDouble("lng", marker.getPosition().longitude);
                detailsFragment.setArguments(arguments);

                // use a FragmentTransaction to display the DetailsFragment
                FragmentTransaction transaction = getFragmentManager()
                        .beginTransaction();
                transaction.replace(R.id.map, detailsFragment);
                transaction.addToBackStack("m");
                transaction.commit(); // causes DetailsFragment to display

                return false;
            }
        });

    }//end of onMapReady()

    @Override
    public void onFishDeleted() {
        googleMap.clear();
        //start the task to load the markers
        new LoadMarkersTask().execute((Object[]) null);
        getFragmentManager().popBackStack();
    }


    @Override
    public void onAddMarkerCompleted() {

        new LoadMarkersTask().execute((Object[]) null);
        getFragmentManager().popBackStack();
    }//end onAddMarkerCompleted()

    @Override
    public void onLocationChanged(Location location) {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
            return;
        }

        //remove location callback:
        locationManager.removeUpdates(this);

        //open the map:
        myLatitude = location.getLatitude();
        myLongitude = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // performs database query outside GUI thread
    private class LoadMarkersTask extends AsyncTask<Object, Object, Cursor> {
        DatabaseConnector databaseConnector = new DatabaseConnector(activity);

        // open database & get Cursor representing specified contact's data
        @Override
        protected Cursor doInBackground(Object... params) {
            databaseConnector.open();
            return databaseConnector.getAllMarkers();
        }

        // use the Cursor returned from the doInBackground method
        @Override
        protected void onPostExecute(Cursor result) {
            super.onPostExecute(result);

            result.moveToFirst(); // move to the first item

            ArrayList<MarkerOptions> markerList = new ArrayList<MarkerOptions>();
            while (result.getPosition() != result.getCount()) {

                int latIndex = result.getColumnIndex("lat");
                String lat = result.getString(latIndex);
                int longitudeIndex = result.getColumnIndex("longitude");
                String longitude = result.getString(longitudeIndex);
                int idIndex = result.getColumnIndex("_id");
                rowId = result.getLong(idIndex);
                int speciesIndex = result.getColumnIndex("species");
                String species = result.getString(speciesIndex);
                LatLng latLng = new LatLng(Double.parseDouble(lat),
                        Double.parseDouble(longitude));

                MarkerOptions mo = new MarkerOptions().position(latLng)
                        .title(String.valueOf(rowId));
                markerList.add(mo);
                result.moveToNext();
            }

            Iterator<MarkerOptions> iter = markerList.iterator();

            //iterate through the array list of marker options and add the markers
            //to the map
            while (iter.hasNext()) {

                MarkerOptions mO = iter.next();
                googleMap.addMarker(mO);
            }

            result.close(); // close the result cursor
            databaseConnector.close(); // close database connection
        } // end method onPostExecute
    } // end class LoadMarkersTask


}//end of Main Activity


