package com.atar.tripal.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.atar.tripal.BuildConfig;
import com.atar.tripal.R;
import com.atar.tripal.callbacks.ProfileCallback;
import com.atar.tripal.db.Details;
import com.atar.tripal.net.ApiClient;
import com.atar.tripal.net.ApiInterface;
import com.atar.tripal.net.NetConstants;
import com.atar.tripal.objects.Result;
import com.atar.tripal.objects.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity implements ProfileCallback {

    /**
     * Constants
     */
    public static final String VIEW_FRAGMENT = "ProfileViewFragment";
    public static final String EDIT_FRAGMENT = "ProfileEditFragment";

    public static final String CODE = "code";
    // When user clicks on friend's profile
    public static final int FRIEND_CODE = 45;
    // When user views his own profile
    public static final int PROFILE_CODE = 46;
    // When user need to insert data to his profile
    public static final int SIGN_UP_GOOGLE_CODE = 47;
    // When user change photo by taking photo from camera
    private static final int CHANGE_PHOTO_FROM_CAMERA = 21;
    // When user change photo by choosing photo from gallery
    private static final int CHANGE_PHOTO_FROM_GALLERY = 22;

    /**
     * Data
     */
    private User mUser;
    private int mCode;
    private ApiInterface mInterface;
    private boolean mDialogShown = false;
    private StorageReference mProfilePhotosRef;
    private String mPhotoPath;

    /**
     * UI Widgets
     */
    private FloatingActionButton mEditOrSave;
    private ImageView mProfilePic;
    private LinearLayout mLoading;
    private ProgressBar mLoadingPic;

    /**
     * Fragments
     */
    private ProfileEditFragment mEditFragment;
    private ProfileViewFragment mViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initFragments();

        mInterface = ApiClient.getAPIClient().create(ApiInterface.class);

        mEditOrSave = findViewById(R.id.pf_edit);


        if(savedInstanceState != null){
            mCode = savedInstanceState.getInt(CODE);
            mUser = (User)savedInstanceState.getSerializable("user");
            mDialogShown = savedInstanceState.getBoolean("isDialogShown");
            String whatFrag = savedInstanceState.getString("frag");
            if(whatFrag != null){
                switch(whatFrag){
                    case VIEW_FRAGMENT:
                        if(mCode == FRIEND_CODE) {
                            mEditOrSave.hide();
                            mEditOrSave.setClickable(false);
                        }
                        break;
                    case EDIT_FRAGMENT:
                        mEditOrSave.setImageResource(R.mipmap.save);
                        if(mCode == SIGN_UP_GOOGLE_CODE && mDialogShown){
                            new AlertDialog.Builder(this)
                                    .setTitle(getString(R.string.welcome_to_tripal) + " " + mUser.getUsername())
                                    .setMessage(R.string.before_you_continue)
                                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            mDialogShown = false;
                                            dialogInterface.dismiss();
                                        }
                                    }).show();
                        }
                        break;
                }
            }
        } else {
            mCode = getIntent().getIntExtra(CODE, FRIEND_CODE);
            if(mCode == SIGN_UP_GOOGLE_CODE){
                mUser = Details.getProfile(this);
                mDialogShown = true;
                mEditOrSave.setImageResource(R.mipmap.save);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.pf_container, mEditFragment, EDIT_FRAGMENT).commit();
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.welcome_to_tripal) + " " + mUser.getUsername())
                        .setMessage(R.string.before_you_continue)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mDialogShown = false;
                                dialogInterface.dismiss();
                            }
                        }).show();
            } else {
                if(mCode == FRIEND_CODE){
                    mEditOrSave.hide();
                    mEditOrSave.setClickable(false);
                } else {
                    mUser = Details.getProfile(this);
                }
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.pf_container, mViewFragment, VIEW_FRAGMENT).commit();
            }
        }

        Toolbar toolbar = findViewById(R.id.pf_toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            toolbar.setNavigationIcon(R.mipmap.arrow_rtl);
        } else {
            toolbar.setNavigationIcon(R.mipmap.arrow_ltr);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        initUIWidgets();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == CHANGE_PHOTO_FROM_GALLERY || requestCode == CHANGE_PHOTO_FROM_CAMERA){
                if(data != null && data.getData() != null){
                    UCrop.Options optionsCamera = new UCrop.Options();
                    optionsCamera.setCompressionFormat(Bitmap.CompressFormat.JPEG);
                    optionsCamera.setCompressionQuality(100);
                    optionsCamera.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary));
                    optionsCamera.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
                    optionsCamera.setActiveWidgetColor(ContextCompat.getColor(this, R.color.colorAccent));
                    UCrop.of(data.getData(), Uri.fromFile(new File(getCacheDir(),
                            System.currentTimeMillis() + ".jpg")))
                            .withAspectRatio(1, 1)
                            .withMaxResultSize(500, 500)
                            .withOptions(optionsCamera)
                            .start(this);
                } else {
                    Toast.makeText(this, R.string.went_wrong, Toast.LENGTH_SHORT).show();
                }
            } else if(requestCode == UCrop.REQUEST_CROP) {
                Uri uri = UCrop.getOutput(data);
                if(uri != null){
                    mPhotoPath = saveFile(uri);
                    if(mPhotoPath != null){
                        try {
                            File file = new File(mPhotoPath);
                            InputStream stream = new FileInputStream(file);
                            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                            mProfilePhotosRef = storageRef.child("users/" + Details
                                    .getProfileId(this) + "/profilePicture.jpg");
                            showLoadingPhoto();
                            UploadTask task = mProfilePhotosRef.putStream(stream);
                            addListenersToUploadTask(task);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else if(data == null){
            Toast.makeText(this, R.string.went_wrong, Toast.LENGTH_SHORT).show();
        } else if(resultCode == UCrop.RESULT_ERROR) {
            Throwable t = UCrop.getError(data);
            if(t != null){
                t.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case CHANGE_PHOTO_FROM_GALLERY:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, CHANGE_PHOTO_FROM_GALLERY);
                } else if(grantResults.length > 0){
                    showSnackbar(R.string.permission_external_rationale,
                            R.string.action_settings, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent();
                                    intent.setAction(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package",
                                            BuildConfig.APPLICATION_ID, null);
                                    intent.setData(uri);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });
                }
                break;
            case CHANGE_PHOTO_FROM_CAMERA:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(!checkPermissionExternal()){
                        requestPermissionExternal();
                    } else {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, CHANGE_PHOTO_FROM_CAMERA);
                    }
                } else if(grantResults.length > 0) {
                    showSnackbar(R.string.permission_camera_rationale,
                            R.string.action_settings, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent();
                                    intent.setAction(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package",
                                            BuildConfig.APPLICATION_ID, null);
                                    intent.setData(uri);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        menu.findItem(R.id.pm_change_pic).setVisible(mCode != FRIEND_CODE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.pm_change_pic:
                showChangePhotoDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CODE, mCode);
        outState.putSerializable("user", mUser);
        if(getCurrentFragment() instanceof ProfileViewFragment){
            outState.putString("frag", VIEW_FRAGMENT);
        } else {
            outState.putString("frag", EDIT_FRAGMENT);
        }
        outState.putBoolean("isDialogShown", mDialogShown);
        if(mProfilePhotosRef != null){
            outState.putString("reference", mProfilePhotosRef.toString());
        }
        outState.putString("photoPath", mPhotoPath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        final String stringRef = savedInstanceState.getString("reference");
        if (stringRef == null) {
            return;
        }
        mProfilePhotosRef = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);
        List<UploadTask> tasks = mProfilePhotosRef.getActiveUploadTasks();
        if (tasks.size() > 0) {
            mPhotoPath = savedInstanceState.getString("photoPath");
            showLoadingPhoto();
            UploadTask task = tasks.get(0);
            addListenersToUploadTask(task);
        }
    }

    @Override
    public void onBackPressed() {
        switch(mCode){
            case FRIEND_CODE:
                finish();
                break;
            case SIGN_UP_GOOGLE_CODE:
                new AlertDialog.Builder(this)
                        .setMessage("You can still edit your profile later.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(
                                        ProfileActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton("Keep editing", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {}
                        }).show();
                break;
            case PROFILE_CODE:
                if(getCurrentFragment() instanceof ProfileEditFragment){
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                            .replace(R.id.pf_container, mViewFragment, VIEW_FRAGMENT)
                            .commit();
                    mEditOrSave.setImageResource(R.mipmap.profile_edit);
                } else {
                    finish();
                }
                break;
            default:
                finish();
                break;
        }
    }

    @Override
    public User getCurrentUser() {
        return mUser;
    }

    private void initFragments(){
        FragmentManager fm = getSupportFragmentManager();
        mEditFragment = (ProfileEditFragment)fm.findFragmentByTag(EDIT_FRAGMENT) ;
        if(mEditFragment == null){
            mEditFragment = new ProfileEditFragment();
        }
        mViewFragment = (ProfileViewFragment)fm.findFragmentByTag(VIEW_FRAGMENT) ;
        if(mViewFragment == null){
            mViewFragment = new ProfileViewFragment();
        }
    }

    private void initUIWidgets(){
        mLoading = findViewById(R.id.pf_loading);
        if(mCode != FRIEND_CODE){
            mEditOrSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(getCurrentFragment() instanceof ProfileEditFragment){
                        showProgress();
                        final User user = mEditFragment.generateUser();
                        Call<Result> call = mInterface.updateUserInfo(user);
                        call.enqueue(new Callback<Result>() {
                            @Override
                            public void onResponse(@NonNull Call<Result> call,
                                    @NonNull Response<Result> response) {
                                Result result = response.body();
                                if(response.isSuccessful() && result != null &&
                                        result.getResult().equals(NetConstants.RESULT_SUCCESS)){
                                    Details.saveProfile(user, ProfileActivity.this);
                                    if(mCode == SIGN_UP_GOOGLE_CODE){
                                        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else if(mCode == PROFILE_CODE) {
                                        mUser = user;
                                        mViewFragment.showData();
                                        mEditOrSave.setImageResource(R.mipmap.profile_edit);
                                        getSupportFragmentManager().beginTransaction()
                                                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                                                .replace(R.id.pf_container, mViewFragment, VIEW_FRAGMENT)
                                                .commit();
                                        stopProgress();
                                    }
                                } else {
                                    stopProgress();
                                    Toast.makeText(ProfileActivity.this,
                                            R.string.went_wrong, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                                stopProgress();
                                t.printStackTrace();
                                Snackbar.make(findViewById(R.id.profile_activity),
                                        R.string.no_connection, Snackbar.LENGTH_LONG).show();
                            }
                        });
                    } else if(getCurrentFragment() instanceof ProfileViewFragment){
                        mEditOrSave.setImageResource(R.mipmap.save);
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                .replace(R.id.pf_container, mEditFragment).commit();
                    }
                }
            });
        } else {
            mEditOrSave.setVisibility(View.GONE);
        }

        mLoadingPic = findViewById(R.id.pf_load_pic);
        mProfilePic = findViewById(R.id.pf_pic);
        if(mCode != FRIEND_CODE){
            String path = Details.getPhotoPath(this);
            showLoadingPhoto();
            if(path != null){
                Picasso.with(this).load(new File(path))
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .into(mProfilePic, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                hideLoadingPhoto();
                            }

                            @Override
                            public void onError() {
                                Details.savePhotoPath(null, ProfileActivity.this);
                                getProfilePhoto();
                            }
                        });
            } else {
                getProfilePhoto();
            }
        } else {
            getProfilePhoto();
        }
    }

    private Fragment getCurrentFragment(){
        return getSupportFragmentManager().findFragmentById(R.id.pf_container);
    }

    private void showProgress(){
        mLoading.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        mLoading.setVisibility(View.VISIBLE);
    }

    private void stopProgress(){
        mLoading.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
        mLoading.setVisibility(View.INVISIBLE);
    }

    private void showChangePhotoDialog(){
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.image_dialog, null);
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
        LinearLayout gallery = sheetView.findViewById(R.id.mgd_gallery);
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!checkPermissionExternal()){
                    requestPermissionExternal();
                } else {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, CHANGE_PHOTO_FROM_GALLERY);
                }
                bottomSheetDialog.dismiss();
            }
        });
        LinearLayout camera = sheetView.findViewById(R.id.mgd_camera);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!checkPermissionCamera()){
                    requestPermissionCamera();
                } else if(!checkPermissionExternal()){
                    requestPermissionExternal();
                } else {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, CHANGE_PHOTO_FROM_CAMERA);
                }
                bottomSheetDialog.dismiss();
            }
        });
        LinearLayout delete = sheetView.findViewById(R.id.mgd_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();
            }
        });
    }

    private void showLoadingPhoto(){
        if(mProfilePic.isShown()){
            mProfilePic.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            mProfilePic.setVisibility(View.GONE);
        }
        if(!mLoadingPic.isShown()){
            mLoadingPic.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            mLoadingPic.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadingPhoto(){
        if(mLoadingPic.isShown()){
            mLoadingPic.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            mLoadingPic.setVisibility(View.INVISIBLE);
        }
        if(!mProfilePic.isShown()){
            mProfilePic.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            mProfilePic.setVisibility(View.VISIBLE);
        }
    }

    private boolean checkPermissionExternal() {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission
                .READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionExternal(){
        ActivityCompat.requestPermissions(ProfileActivity.this,
                new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE},
                CHANGE_PHOTO_FROM_GALLERY);
    }

    private boolean checkPermissionCamera(){
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionCamera(){
        ActivityCompat.requestPermissions(ProfileActivity.this,
                new String[] {Manifest.permission.CAMERA},
                CHANGE_PHOTO_FROM_CAMERA);
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(R.id.profile_activity),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private void addListenersToUploadTask(UploadTask task){
        task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot state) {
                hideLoadingPhoto();
                Details.savePhotoPath(mPhotoPath, ProfileActivity.this);
                Picasso.with(ProfileActivity.this).load(new File(mPhotoPath))
                        .memoryPolicy(MemoryPolicy.NO_STORE)
                        .into(mProfilePic);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
                hideLoadingPhoto();
                Toast.makeText(ProfileActivity.this,
                        R.string.went_wrong, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String saveFile(Uri sourceUri) {
        String sourceFilename= sourceUri.getPath();
        String destinationFilename = android.os.Environment.getExternalStorageDirectory()
                .getPath() + File.separatorChar + getString(R.string.app_name);
        File dir = new File(destinationFilename);
        if(!dir.exists()){
            if(!dir.mkdirs()){
                return null;
            }
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        destinationFilename += File.separatorChar + timestamp + ".jpg";

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(sourceFilename));
            bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
            byte[] buf = new byte[1024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
            bos.flush();
            return destinationFilename;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.went_wrong, Toast.LENGTH_SHORT).show();
            return null;
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getProfilePhoto(){
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference profilePhotosRef = storageRef.child("users/" + mUser.getId() + "/profilePicture.jpg");
        profilePhotosRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.with(ProfileActivity.this).load(uri).memoryPolicy(MemoryPolicy.NO_CACHE)
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .into(mProfilePic, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        hideLoadingPhoto();
                    }

                    @Override
                    public void onError() {
                        hideLoadingPhoto();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }

}
