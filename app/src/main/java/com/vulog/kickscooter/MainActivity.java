package com.vulog.kickscooter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vulog.kickscooter.model.IoT;
import com.vulog.kickscooter.model.Vehicle;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback {

    private static final String BASE_URL = "https://europe-west1-vulog-comodule-poc.cloudfunctions.net/api";
    private static String TAG = MainActivity.class.getName();

    private OkHttpClient httpClient;
    //private String mIotId = "bc125edbccc6508a";
    private String mIotId = "";


    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private FirebaseAuth.AuthStateListener mAuthListener;

    private static final int RC_SIGN_IN = 9001;

    private SignInButton mSignInButton;
    private Button mStartButton;
    private Button mStopButton;
    private ProgressBar mProgressBar;
    private View mCommandGroupView;
    private MenuItem mMenuItemLogout;
    private MenuItem mMenuItemRefresh;

    DatabaseReference mRefIoTs = FirebaseDatabase.getInstance().getReference("iots");
    DatabaseReference mRefVehicles = FirebaseDatabase.getInstance().getReference("vehicles");

    Map<String, IoT> mIoTs = new HashMap<>();
    Map<String, Vehicle> mVehicles = new HashMap<>();

    //ObjectMapper mapper = new ObjectMapper();


    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    GoogleSignInClient mGoogleSignInClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mymenu, menu);

        mMenuItemLogout = menu.getItem(1);
        mMenuItemRefresh = menu.getItem(0);
        mMenuItemLogout.setVisible(false);
        updateUI();
        //mMenuItemRefresh.setVisible(false);
        //mMenuItemLogout = (MenuItem) findViewById(R.id.logout);
        //mMenuItemLogout.setVisible(false);
        // Configure the search info and add any event listeners...
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.logout:
                onClickLogout(null);
                return true;
            case R.id.refresh:
                onClickRefresh(null);
                return true;
        }
        return (super.onOptionsItemSelected(menuItem));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        httpClient = new OkHttpClient();


        mSignInButton = findViewById(R.id.login_with_google);
        mSignInButton.setSize(SignInButton.SIZE_WIDE);
        mSignInButton.setOnClickListener(this);

        mCommandGroupView = (View) findViewById(R.id.command_group);
        mStartButton = (Button) findViewById(R.id.but_start);
        mStopButton = (Button) findViewById(R.id.but_stop);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        //mSignOutButton = findViewById(R.id.but_logout);

        configureSignIn();

        mAuth = FirebaseAuth.getInstance();

        //this is where we start the Auth state Listener to listen for whether the user is signed in or not
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                // Get signedIn user
                FirebaseUser user = firebaseAuth.getCurrentUser();

                //if user is signed in, we call a helper method to save the user details to Firebase
                if (user != null) {
                    // User is signed in
                    //createUserInFirebaseHelper();
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationProviderClient = LocationServices
                .getFusedLocationProviderClient(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        //FirebaseDatabase database = FirebaseDatabase.getInstance();
        //DatabaseReference myRef = database.getReference("message");
        //myRef.setValue("Hello, World!");

        mRefVehicles.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mVehicles.clear();

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Vehicle vehicle = child.getValue(Vehicle.class);
                    mVehicles.put(vehicle.getId(), vehicle);
                    Log.d(TAG, "Vehicle id: " + vehicle.getId());
                }
                updateMap();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        mRefIoTs.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mIoTs.clear();

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    // TODO: handle the post
                    IoT iot = child.getValue(IoT.class);
                    if (mIoTs.containsKey(iot.getId())) {
                    } else {
                        mIoTs.put(iot.getId(), iot);
                    }
                    Log.d(TAG, "IoT id: " + iot.getId());
                }

                updateMap();
                updateUI();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void updateMap() {
        mMap.clear();
        // Add a marker in Sydney, Australia, and move the camera.
        //LatLng nice = new LatLng(43.675819, 7.289429);
        //mMap.addMarker(new MarkerOptions().position(nice).title("Marker in Nice"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(nice));
        for (IoT iot : mIoTs.values()) {
            LatLng iotLatLng = new LatLng(iot.getGpsLatitude(), iot.getGpsLongitude());
            Vehicle vehicle = mVehicles.get(iot.getId());
            float markerColor = BitmapDescriptorFactory.HUE_BLUE;
            String markerName = vehicle==null?iot.getId():vehicle.getName();

            if (vehicle != null) {
                if (vehicle.getBooked()) {
                    markerColor = BitmapDescriptorFactory.HUE_RED;
                    markerName = "Booked By: " + vehicle.getBookedByName();
                }
                else if (vehicle.getEnabled() == false) {
                    markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                }
            }

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(iotLatLng)
                    .title(markerName)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            );
            marker.setTag(new String (iot.getId()));
            if (vehicle != null && vehicle.getEnabled()) {
                marker.showInfoWindow();
                mIotId = iot.getId();
                updateUI();
            }
        }

    }

    final Handler mHandler = new Handler();
    public void onClickStartStop(View v) {
        if (mIotId == "") {
            Toast.makeText(this, R.string.noVehicleSelected,Toast.LENGTH_LONG);
            return;
        }
        String action = "startIoT";
        switch (v.getId()) {
            case R.id.but_stop:
                action = "stopIoT";
                break;
            case R.id.but_start:
                action = "startIoT";
                break;
        }

        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.VISIBLE);
        mStartButton.setEnabled(false);
        mStopButton.setEnabled(false);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                mStartButton.setEnabled(true);
                mStopButton.setEnabled(true);
            }
        }, 15000);

        executeMbaasGet(mIotId, action);
        updateUI();
    }

    public void onClickRefresh(View v) {
        String action = "refresh";

        executeMbaasGet(mIotId, action);
    }


    private void executeMbaasGet(String iotId, String action) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addPathSegment(action)
                .addPathSegment(iotId)
                .build();

        //FirebaseAuth.getInstance().getAccessToken(true);
        String token = FirebaseAuth.getInstance().getCurrentUser().getIdToken(false).getResult().getToken();


        final Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        //sendCommand(request);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (Response response = httpClient.newCall(request).execute()) {
                    // Consume and discard the response body.
                    String resStr = response.body().string();
                    //Synthesis synthesis = mapper.readValue(resStr, Synthesis.class);
                    Log.d("Response", "" + response.code());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void onClickTest(View v) {

    }



    // ----------- SIGN IN GOOGLE -------------

    // This method configures Google SignIn
    public void configureSignIn() {
        // Configure sign-in to request the user's basic profile like name and email
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        updateUI();
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount googleAccount) {


        Log.d(TAG, "firebaseAuthWithGoogle:" + googleAccount.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(googleAccount.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            updateUI();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            //Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI();
                        }

                        // ...
                    }
                });
    }

    private void updateUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            mSignInButton.setVisibility(View.VISIBLE);
            mCommandGroupView.setVisibility(View.GONE);
            if (mMenuItemLogout != null) mMenuItemLogout.setVisible(false);
        } else {
            mSignInButton.setVisibility(View.GONE);
            mCommandGroupView.setVisibility(View.VISIBLE);
            if (mMenuItemLogout != null) mMenuItemLogout.setVisible(true);
        }

        IoT iot = mIoTs.get(mIotId);
        if (iot != null) {
            Boolean powerOn = iot.getVehiclePowerOn();

            int mStartButtonVisibilityPrev = mStartButton.getVisibility();
            mStartButton.setVisibility(powerOn?View.GONE:View.VISIBLE);
            mStopButton.setVisibility(powerOn?View.VISIBLE:View.GONE);
            // If Status of Start changed ==> reset the ProgressBar
            if (mStartButtonVisibilityPrev != mStartButton.getVisibility()) {
            //if (true) {
                mHandler.removeCallbacksAndMessages(null); // Cancel the runnable timer
                mProgressBar.setVisibility(View.GONE);
                mStartButton.setEnabled(true);
                mStopButton.setEnabled(true);
            }

            Vehicle vehicle = mVehicles.get(mIotId);
            boolean vehicleEnabled = vehicle != null && vehicle.getEnabled();

             if (mProgressBar.getVisibility() == View.GONE) {
                 //Do not check this when action is in progress
                 if (powerOn) {
                     // Check if user can end the trip of this vehicle
                     if (vehicleEnabled && vehicle.getBooked()) {
                         boolean userOwnsTrip = true;
                         if (mAuth.getCurrentUser() != null &&
                                 !vehicle.getBookedByEmail().equals(mAuth.getCurrentUser().getEmail())) {
                             userOwnsTrip = false;
                             // Vehicle booked by another user ==> Unable to command the device
                         }
                         mStartButton.setEnabled(userOwnsTrip);
                         mStopButton.setEnabled(userOwnsTrip);
                     }
                 } else {
                     mStartButton.setEnabled(vehicleEnabled);
                     mStopButton.setEnabled(vehicleEnabled);
                 }
             }
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount googleAccount = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(googleAccount);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }

    @Override
    public void onClick(View v) {
        signIn();
    }

    public void onClickLogout(View view) {
        FirebaseAuth.getInstance().signOut();
        mGoogleSignInClient.signOut();
        mAuth.signOut();
        updateUI();

    }

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(onMyLocationButtonClickListener);
        //mMap.setOnMyLocationClickListener(onMyLocationClickListener);
        enableMyLocationIfPermitted();

        mMap.getUiSettings().setZoomControlsEnabled(true);
        showMyLocation();

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mIotId = (String) marker.getTag();
                marker.showInfoWindow();
                updateUI();
                return true;
            }
        });

        //mMap.setMinZoomPreference(11);

    }

    private void enableMyLocationIfPermitted() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocationIfPermitted();
                }
                return;
            }

        }
    }

    private void showMyLocation() {
        try {
            Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        Location location = task.getResult();
                        if (location == null) return;
                        LatLng currentLatLng = new LatLng(location.getLatitude(),
                                location.getLongitude());
                        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(currentLatLng,
                                15);
                        mMap.moveCamera(update);
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }

        // Add a marker in Sydney, Australia, and move the camera.
        //LatLng nice = new LatLng(43.675819, 7.289429);
        //mMap.addMarker(new MarkerOptions().position(nice).title("Marker in Nice"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(nice));

    }

    private GoogleMap.OnMyLocationButtonClickListener onMyLocationButtonClickListener =
            new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    //mMap.setMinZoomPreference(15);
                    return false;
                }
            };

}
