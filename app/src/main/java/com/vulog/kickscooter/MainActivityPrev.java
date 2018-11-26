package com.vulog.kickscooter;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
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
import com.vulog.kickscooter.model.BoxData;
import com.vulog.kickscooter.model.BoxMessage;
import com.vulog.kickscooter.model.IoT;
import com.vulog.kickscooter.model.Synthesis;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivityPrev extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = MainActivityPrev.class.getName();

    public static String API_KEY = "fbd3djb0v8sf9665h52gbu97ll";
    public static String BASE_URL = "https://api.comodule.com/externalsharingmoduleapi/v2/module/";

    private OkHttpClient client;
    //private OkHttpClient clientWs;
    private WebSocketConnector wsc;
    private String iotId= "bc125edbccc6508a";
    private String boxId = "emul_labbox_poc_01";



    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private FirebaseAuth.AuthStateListener mAuthListener;

    private static final int RC_SIGN_IN = 9001;

    private SignInButton mSignInButton;
    private View mCommandGroupView;

    DatabaseReference mRefIoTs = FirebaseDatabase.getInstance().getReference("iots");

    List<IoT> mIoTs = new ArrayList<>();

    ObjectMapper mapper = new ObjectMapper();


    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new OkHttpClient();
        //clientWs = new OkHttpClient();
        wsc = new WebSocketConnector("ws://emulator.vulog.center:8081/box");


        mSignInButton = (SignInButton) findViewById(R.id.login_with_google);
        mCommandGroupView = findViewById(R.id.command_group);
        mSignInButton.setSize(SignInButton.SIZE_WIDE);

        mSignInButton.setOnClickListener(this);

        configureSignIn();

        mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();

        //this is where we start the Auth state Listener to listen for whether the user is signed in or not
        mAuthListener = new FirebaseAuth.AuthStateListener(){
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                // Get signedIn user
                FirebaseUser user = firebaseAuth.getCurrentUser();

                //if user is signed in, we call a helper method to save the user details to Firebase
                if (user != null) {
                    // User is signed in
                    //createUserInFirebaseHelper();
                    updateUI(null);
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addPathSegment(iotId)
                .addQueryParameter("apiKey", API_KEY)
                .build();
        final Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        sendCommand(request);

        //FirebaseDatabase database = FirebaseDatabase.getInstance();
        //DatabaseReference myRef = database.getReference("message");
        //myRef.setValue("Hello, World!");

        mRefIoTs.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    // TODO: handle the post
                    IoT iot = child.getValue(IoT.class);
                    mIoTs.add(iot);
                    Log.d(TAG, "IoT id: " + iot.getId());
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void onClickStartStop(View v)
    {
        String content = "{value: false}";
        if (v.getId() == R.id.but_start) content = "{value: true}";

        RequestBody body = RequestBody.create(JSON, content);

        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addPathSegment(iotId)
                .addPathSegment("vehiclePowerOn")
                .addQueryParameter("apiKey", API_KEY)
                .build();
        final Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        sendCommand(request);
    }


    public void onClickTest(View v)
    {
        wsc.connect(new WebSocketConnector.ServerListener() {
            @Override
            public void onNewMessage(String message) {
                Log.d("Message", message);
            }

            @Override
            public void onStatusChange(WebSocketConnector.ConnectionStatus status) {
                if (status.equals(WebSocketConnector.ConnectionStatus.CONNECTED)) {
                    updateBoxMessage(new BoxData(boxId, true, null, null, null, null));
                }
                Log.d("Connection Status", status.name());

            }
        });

        //EchoWebSocketListener listener = new EchoWebSocketListener();
        //WebSocket ws = clientWs.newWebSocket(request, listener);
        //clientWs.dispatcher().executorService().shutdown();
        //wsc.sendMessage("{\"boxId\":\"emul_labbox_poc_01\",\"type\":\"live\",\"name\":\"update\",\"token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6ImxhYmJveF9wb2MiLCJ0Y3BIb3N0IjoiNTQuMzYuMTU1LjE5OCIsInRjcFBvcnQiOjgwMDgsImh0dHBIb3N0IjoiNTQuMzYuMTU1LjE5OCIsImh0dHBQb3J0Ijo4MDgyLCJleHBlcnQiOnRydWUsInJvbGUiOiJVU0VSIiwiaWF0IjoxNTM4MDc3NzgzfQ.6-qtibLfBDW7GjCYKMoYP7tskex2WW7wxitfegLjeDY\",\"data\":{\"inCharge\":true}}\n");

    }


    public void onClickRefresh(View v)
    {
        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addPathSegment(iotId)
                .addQueryParameter("apiKey", API_KEY)
                .build();
        final Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        sendCommand(request);

    }

    private void updateBoxMessage(final BoxData boxData) {
        BoxMessage boxMessage = new BoxMessage("emul_labbox_poc_01",
                "live",
                "update",
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6ImxhYmJveF9wb2MiLCJ0Y3BIb3N0IjoiNTQuMzYuMTU1LjE5OCIsInRjcFBvcnQiOjgwMDgsImh0dHBIb3N0IjoiNTQuMzYuMTU1LjE5OCIsImh0dHBQb3J0Ijo4MDgyLCJleHBlcnQiOnRydWUsInJvbGUiOiJVU0VSIiwiaWF0IjoxNTM4MDc3NzgzfQ.6-qtibLfBDW7GjCYKMoYP7tskex2WW7wxitfegLjeDY",
                boxData);
        //wsc.sendMessage("{\"boxId\":\"emul_labbox_poc_01\",\"type\":\"live\",\"name\":\"update\",\"token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6ImxhYmJveF9wb2MiLCJ0Y3BIb3N0IjoiNTQuMzYuMTU1LjE5OCIsInRjcFBvcnQiOjgwMDgsImh0dHBIb3N0IjoiNTQuMzYuMTU1LjE5OCIsImh0dHBQb3J0Ijo4MDgyLCJleHBlcnQiOnRydWUsInJvbGUiOiJVU0VSIiwiaWF0IjoxNTM4MDc3NzgzfQ.6-qtibLfBDW7GjCYKMoYP7tskex2WW7wxitfegLjeDY\",\"data\":{\"inCharge\":true}}\n");
        try {

            wsc.sendMessage(mapper.writeValueAsString(boxMessage)+"\n");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void sendCommand(final Request request) {
        new Thread(new Runnable(){
            @Override
            public void run() {
                try (Response response = client.newCall(request).execute()) {
                    // Consume and discard the response body.
                    String resStr = response.body().string();
                    Synthesis synthesis = mapper.readValue(resStr, Synthesis.class);

                    BoxData boxData = new BoxData(boxId,
                            synthesis.getVehicleCharging(),
                            !synthesis.getVehiclePowerOn(),
                            null,
                            synthesis.getGpsLongitude(),
                            synthesis.getGpsLatitude()
                    );

                    updateBoxMessage(boxData);

                    JSONObject json = new JSONObject(resStr);
                    Log.w("Response", resStr);
                    Log.w("Synthesis", synthesis.toString());
                    ;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // ----------- SIGN IN GOOGLE -------------

    // This method configures Google SignIn
    public void configureSignIn(){
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            //Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user == null) {
            mSignInButton.setVisibility(View.VISIBLE);
            mCommandGroupView.setVisibility(View.INVISIBLE);

        }
        else {
            mSignInButton.setVisibility(View.INVISIBLE);
            mCommandGroupView.setVisibility(View.VISIBLE);
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
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
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
}
