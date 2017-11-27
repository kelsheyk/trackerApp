package com.example.android_tracker;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.io.Serializable;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

/**
 * Activity to demonstrate basic retrieval of the Google user's ID, email address, and basic
 * profile.
 */
public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener
{
    Context context = this;
    private static final String TAG = "==> LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_SIGN_OUT = 9002;

    private GoogleSignInAccount acct = null;
    private GoogleSignInClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Button listeners
        findViewById(R.id.button_sign_in).setOnClickListener(this);
        findViewById(R.id.button_sign_out).setOnClickListener(this);
        findViewById(R.id.my_list_button).setOnClickListener(this);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = GoogleSignIn.getClient(this, gso);

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        user = mAuth.getCurrentUser();
        acct = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(user);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN)
        {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try
            {
                acct = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(acct);
            }
            catch (ApiException e)
            {
                updateUI(null);
            }
        }
    }

    // [START auth_with_google]
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct)
    {
        Log.i("**-** ID is  ---->", acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (task.isSuccessful())
                        {
                            user = mAuth.getCurrentUser();
                            updateUI(user);
                        }
                        else
                        {
                            findViewById(R.id.sign_in_status).setVisibility(View.VISIBLE);
                            updateUI(null);
                        }
                    }
                });
    }
    // [END auth_with_google]

    // [START signIn]
    private void signIn()
    {
        Intent signInIntent = mGoogleApiClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // [START signOut]
    private void signOut()
    {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        mGoogleApiClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        acct = null;
                        user = null;
                        updateUI(null);
                    }
                });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        findViewById(R.id.sign_in_status).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }


    private void updateUI(final FirebaseUser user)
    {
        if(user != null)
        {
            findViewById(R.id.button_sign_in).setVisibility(View.GONE);
            findViewById(R.id.button_sign_out).setVisibility(View.VISIBLE);
            findViewById(R.id.my_list_button).setVisibility(View.VISIBLE);
        }
        else
        {
            findViewById(R.id.button_sign_in).setVisibility(View.VISIBLE);
            findViewById(R.id.button_sign_out).setVisibility(View.GONE);
            findViewById(R.id.my_list_button).setVisibility(View.GONE);
        }
    }

    private void setNewActivityIntent(Intent intent)
    {
        if(acct == null)
        {
            return;
        }

        intent.putExtra("userName", acct.getDisplayName());
        intent.putExtra("userEmail", acct.getEmail());
        intent.putExtra("userId", acct.getId());
//        Log.i("0000000000000 ", user.toString());
        intent.putExtra("userToken", acct.getIdToken());
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button_sign_in:
                signIn();
                break;
            case R.id.button_sign_out:
                Toast.makeText(context, "Signing out", Toast.LENGTH_SHORT).show();
                signOut();
                break;
            case R.id.my_list_button:
                Intent serviceIntent = new Intent(context, PostLocationService.class);
                context.startService(serviceIntent);

                Intent intent = new Intent(context, MyListActivity.class);
                setNewActivityIntent(intent);
                startActivity(intent);
                break;
        }
    }
}
