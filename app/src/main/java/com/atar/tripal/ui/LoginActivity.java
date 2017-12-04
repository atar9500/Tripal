package com.atar.tripal.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.atar.tripal.R;
import com.atar.tripal.callbacks.LoginListener;
import com.atar.tripal.db.Details;
import com.atar.tripal.net.ApiClient;
import com.atar.tripal.net.ApiInterface;
import com.atar.tripal.net.NetConstants;
import com.atar.tripal.objects.User;
import com.atar.tripal.objects.Result;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;

public class LoginActivity extends AppCompatActivity implements LoginListener {

    private static final String LOG_IN_FRAGMENT = "LogInFragment";
    private static final String SIGN_UP_FRAGMENT = "SignUpFragment";
    private static final int GOOGLE_LOGIN = 11;

    /**
     * UI Widgets
     */
    private LinearLayout mLoadingScreen;
    private FrameLayout mContainer;

    /**
     * Fragments
     */
    private LogInFragment mLogInFragment;
    private SignUpFragment mSignUpFragment;

    /**
     * Data
     */
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ApiInterface mInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        mInterface = ApiClient.getAPIClient().create(ApiInterface.class);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initFragments();
        initUIWidgets();

        if(savedInstanceState != null){
            String state = savedInstanceState.getString("frag");
            if(state != null && state.equals(LOG_IN_FRAGMENT) && !mLogInFragment.isAdded()){
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.la_container, mLogInFragment, LOG_IN_FRAGMENT).commit();
                showStatusBar();
            } else if(state != null && state.equals(SIGN_UP_FRAGMENT) && !mSignUpFragment.isAdded()){
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.la_container, mSignUpFragment, SIGN_UP_FRAGMENT).commit();
                showStatusBar();
            } else {
                hideStatusBar();
            }
        } else {
            hideStatusBar();
        }

        String i = Details.getProfileId(this);

        if(i != null){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            mLoadingScreen.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            mLoadingScreen.setVisibility(View.GONE);
            startActivity(intent);
            if(!connectedToInternet()){
                Snackbar.make(findViewById(R.id.login_activity),
                        R.string.no_connection, Snackbar.LENGTH_LONG).show();
            }
            finish();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(Details.getProfileId(this) != null){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            mLoadingScreen.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            mLoadingScreen.setVisibility(View.GONE);
            startActivity(intent);
            if(!connectedToInternet()){
                Snackbar.make(findViewById(R.id.login_activity),
                        R.string.no_connection, Snackbar.LENGTH_LONG).show();
            }
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if(getCurrentFragment() instanceof LogInFragment){
            int stateSheet = mLogInFragment.getSheetState();
            if(stateSheet != BottomSheetBehavior.STATE_HIDDEN
                    && stateSheet != BottomSheetBehavior.STATE_COLLAPSED
                    && stateSheet != -33){
                mLogInFragment.hideSheet();
            } else {
                mLogInFragment.cleanFields();
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(0, android.R.anim.fade_out)
                        .remove(mLogInFragment).commit();
                hideStatusBar();
            }
        } else if(getCurrentFragment() instanceof  SignUpFragment){
            mSignUpFragment.cleanFields();
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(0, android.R.anim.fade_out)
                    .remove(mSignUpFragment).commit();
            hideStatusBar();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_LOGIN && resultCode == RESULT_OK) {
            onPreValidate();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                User user = new User();
                user.setId(account.getId());
                user.setUsername(account.getDisplayName());
                sendRequestToServerGoogle(user);
            } catch (ApiException e) {
                Toast.makeText(this, R.string.went_wrong,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(getCurrentFragment() instanceof LogInFragment){
            outState.putString("frag", LOG_IN_FRAGMENT);
        } else if(getCurrentFragment() instanceof SignUpFragment){
            outState.putString("frag", SIGN_UP_FRAGMENT);
        }
    }

    @Override
    public void onSignInClick(String email, String password) {
        onPreValidate();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if(firebaseUser != null && firebaseUser.isEmailVerified()){
                                User user = new User(firebaseUser.getUid());
                                signInUser(user);
                            } else if(firebaseUser != null && !firebaseUser.isEmailVerified()) {
                                onAfterValidate();
                                Toast.makeText(LoginActivity.this, R.string.please_confirm,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                onAfterValidate();
                                mLogInFragment.onFailedLogIn(false);
                            }
                        } else {
                            onAfterValidate();
                            mLogInFragment.onFailedLogIn(true);
                        }
                    }
                });
    }

    @Override
    public void onCreateAccount(final User user, String email, String password) {
        onPreValidate();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            FirebaseUser googleUser = mAuth.getCurrentUser();
                            if(googleUser != null){
                                verifyEmail(googleUser);
                                user.setId(googleUser.getUid());
                                registerUser(user);
                            }
                        } else {
                            mSignUpFragment.onFailedRegister(true);
                            onAfterValidate();
                        }
                    }
                });
    }

    @Override
    public void onResetClick(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            mLogInFragment.hideSheet();
                            new AlertDialog.Builder(LoginActivity.this)
                                    .setMessage(R.string.email_pass_reset)
                                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    });
                        } else {
                            mLogInFragment.setError();
                        }
                    }
                });
    }

    private void verifyEmail(@NonNull FirebaseUser user){
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(LoginActivity.this, R.string.email_verification,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    R.string.went_wrong,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void onPreValidate() {
        if(mContainer.isShown()){
            mContainer.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            mContainer.setVisibility(View.GONE);
        }
        if(!mLoadingScreen.isShown()){
            mLoadingScreen.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            mLoadingScreen.setVisibility(View.VISIBLE);
        }
    }

    private void onAfterValidate() {
        if(mLoadingScreen.isShown()){
            mLoadingScreen.setAnimation(AnimationUtils.loadAnimation(
                    LoginActivity.this, R.anim.fade_out));
            mLoadingScreen.setVisibility(View.GONE);
        }
        if(!mContainer.isShown()){
            mContainer.setAnimation(AnimationUtils.loadAnimation(
                    LoginActivity.this, R.anim.fade_in));
            mContainer.setVisibility(View.VISIBLE);
        }
    }

    private void initUIWidgets(){

        findViewById(R.id.la_background).setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mContainer = findViewById(R.id.la_container);
        mLoadingScreen = findViewById(R.id.la_loading);

        Button signIn = findViewById(R.id.la_sign_in);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showStatusBar();
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, 0)
                        .add(R.id.la_container, mLogInFragment, LOG_IN_FRAGMENT).commit();
            }
        });
        Button signUp = findViewById(R.id.la_sign_up);
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showStatusBar();
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, 0)
                        .add(R.id.la_container, mSignUpFragment, SIGN_UP_FRAGMENT).commit();
            }
        });
        Button googleSign = findViewById(R.id.la_google_sign);
        googleSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, GOOGLE_LOGIN);
            }
        });
    }

    private void initFragments(){
        FragmentManager fm = getSupportFragmentManager();
        mLogInFragment = (LogInFragment) fm.findFragmentByTag(LOG_IN_FRAGMENT);
        if(mLogInFragment == null){
            mLogInFragment = new LogInFragment();
        }
        mSignUpFragment = (SignUpFragment) fm.findFragmentByTag(SIGN_UP_FRAGMENT);
        if(mSignUpFragment == null){
            mSignUpFragment = new SignUpFragment();
        }
    }

    private void registerUser(final User user){
        Call<Result> call = mInterface.registerUser(user);
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(@NonNull Call<Result> call,
                                   @NonNull retrofit2.Response<Result> response) {
                Result result = response.body();
                if(response.isSuccessful() && result!= null){
                    switch(result.getResult()){
                        case NetConstants.RESULT_SUCCESS:
                            Details.saveProfile(user, LoginActivity.this);
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.la_container, mLogInFragment, LOG_IN_FRAGMENT).commit();
                            onAfterValidate();
                            break;
                        case NetConstants.RESULT_FAILED:
                            onAfterValidate();
                            Toast.makeText(LoginActivity.this, R.string.went_wrong,
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                } else {
                    onAfterValidate();
                    Toast.makeText(LoginActivity.this, R.string.went_wrong,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                if(getCurrentFragment() instanceof LogInFragment){
                    mLogInFragment.onFailedLogIn(false);
                } else if(getCurrentFragment() instanceof SignUpFragment){
                    mSignUpFragment.onFailedRegister(false);
                }
                onAfterValidate();
                Toast.makeText(LoginActivity.this, R.string.could_not_connect,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signInUser(User user){
        user.setToken(Details.getToken(this));
        Call<User> call = mInterface.signWithEmail(user);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call,
                                   @NonNull retrofit2.Response<User> response) {
                User p = response.body();
                if(response.isSuccessful() && p != null){
                    Details.saveProfileId(p.getId(),
                            LoginActivity.this);
                    Details.saveUsername(p.getUsername(),
                            LoginActivity.this);
                    Intent intent = new Intent(
                            LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    onAfterValidate();
                    Toast.makeText(LoginActivity.this, R.string.went_wrong,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if(getCurrentFragment() instanceof LogInFragment){
                    mLogInFragment.onFailedLogIn(false);
                } else if(getCurrentFragment() instanceof SignUpFragment){
                    mSignUpFragment.onFailedRegister(false);
                }
                onAfterValidate();
                Toast.makeText(LoginActivity.this, R.string.could_not_connect,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendRequestToServerGoogle(User user){
        user.setToken(Details.getToken(this));
        Call<User> call = mInterface.signWithGoogle(user);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull retrofit2.Response<User> response) {
                User user = response.body();
                if(response.isSuccessful() && user != null){
                    if(user.getAboutMe().equals("CREATED")){
                        Details.saveProfileId(user.getId(), LoginActivity.this);
                        Details.saveUsername(user.getUsername(), LoginActivity.this);
                        Intent intent2 = new Intent(LoginActivity.this, ProfileActivity.class);
                        intent2.putExtra(ProfileActivity.CODE, ProfileActivity.SIGN_UP_GOOGLE_CODE);
                        startActivity(intent2);
                    } else {
                        Details.saveProfile(user, LoginActivity.this);
                        Intent intent1 = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent1);
                    }
                } else {
                    onAfterValidate();
                    Toast.makeText(LoginActivity.this, R.string.went_wrong,
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                Snackbar.make(findViewById(R.id.login_activity),
                        R.string.no_connection, Snackbar.LENGTH_LONG).show();
                onAfterValidate();
            }
        });
    }

    private Fragment getCurrentFragment(){
        return getSupportFragmentManager().findFragmentById(R.id.la_container);
    }

    private boolean connectedToInternet(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm != null){
            Network[] activeNetworks = cm.getAllNetworks();
            for (Network n: activeNetworks) {
                NetworkInfo nInfo = cm.getNetworkInfo(n);
                if(nInfo.isConnected())
                    return true;
            }
        }
        return false;
    }

    private void hideStatusBar(){
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    private void showStatusBar(){
        getWindow().clearFlags(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
    }
}
