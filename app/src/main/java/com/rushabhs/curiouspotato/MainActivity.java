package com.rushabhs.curiouspotato;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import java.util.ArrayList;

public class MainActivity extends FragmentActivity  implements GoogleApiClient.OnConnectionFailedListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments representing
     * each object in a collection. We use a {@link android.support.v4.app.FragmentStatePagerAdapter}
     * derivative, which will destroy and re-create fragments as needed, saving and restoring their
     * state in the process. This is important to conserve memory and is a best practice when
     * allowing navigation between objects in a potentially large collection.
     */
    private static final int REQUEST_LOCATION = 2;
    CollectionPagerAdapter mCollectionPagerAdapter;
    public GoogleApiClient mGoogleApiClient;
    public static ArrayList<Place> places = new ArrayList<>();
//    private static HashMap<Place, List<Integer>> places = new HashMap<>();
    /**
     * The {@link android.support.v4.view.ViewPager} that will display the object collection.
     */
    ViewPager mViewPager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an adapter that when requested, will return a fragment representing an object in
        // the collection.
        //
        // ViewPager and its adapters use support library fragments, so we must use
        // getSupportFragmentManager.
        mCollectionPagerAdapter = new CollectionPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager, attaching the adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mCollectionPagerAdapter);

        // Connect to Google api client
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .build();
//        .enableAutoManage(this, this)

        getNearByPlaces();

        for(int i=0; i<places.size(); i++){
            places.get(i).freeze();
        }
        Log.d("Places value is ", Integer.toString(places.size()));
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

        // ...
    }

    private void getNearByPlaces(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Display UI and wait for user interaction
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            }
        } else {
            // permission has been granted, continue as usual
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, null);
            Log.d("Services is Connected ", Boolean.toString(mGoogleApiClient.isConnected()));
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        Place p = placeLikelihood.getPlace();
                        Log.d("", String.format("Place '%s' with id '%s' has likelihood: %g",
                                p.getName(),
                                p.getId(),
                                placeLikelihood.getLikelihood()));
                        places.add(p);
                        PlaceListAdapter adapter = new PlaceListAdapter(MainActivity.this, R.layout.card_object_view, places);
                        ((ListView) findViewById(R.id.list)).setAdapter(adapter);
                        Log.d("Gets here ", "Test");
                        adapter.notifyDataSetChanged();
                    }
//                    likelyPlaces.release();
                }
            });
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getNearByPlaces();
            }
        }
    }

    /**
     * A {@link android.support.v4.app.FragmentStatePagerAdapter} that returns a fragment
     * representing an object in the collection.
     */
    public static class CollectionPagerAdapter extends FragmentStatePagerAdapter {

        private String[] types = {"All", "Cafe", "Airport", "Bank", "School", "Bar", "Lodging", "Pharmacy"};

        public CollectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new CategoryFragment();
            Bundle args = new Bundle();
            args.putString(CategoryFragment.OBJECT_TYPE,types[i]);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return types.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return types[position];
        }
    }

    /**
     * A dummy fragment representing a section of the app, but that simply displays types of places.
     */
    public static class CategoryFragment extends Fragment {

        public static final String OBJECT_TYPE = "type";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.card_list_view, container, false);
//            View rootView = inflater.inflate(R.layout.card_object_view, container, false);
            Bundle args = getArguments();
            Log.d("TextView value: ", args.getString(OBJECT_TYPE));
           return rootView;
        }
    }
}
