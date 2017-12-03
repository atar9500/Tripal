package com.atar.tripal.ui;


import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;

import com.atar.tripal.R;
import com.atar.tripal.callbacks.LoginListener;
import com.atar.tripal.objects.User;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class SignUpFragment extends Fragment implements TextWatcher {

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 2;

    /**
     * Data
     */
    private User mUser;
    private LoginListener mCallback;

    /**
     * UI Widgets
     */
    private View mView;
    private MaterialEditText mUsername, mEmail, mPassword, mConfirmPassword,
            mDate, mAboutMe, mInterests, mMusic, mOrigin;
    private Button mContinue;
    private Spinner mGender;

    public SignUpFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_sign_up, container, false);

        setRetainInstance(true);

        mUser = new User();

        initFields();

        return mView;
    }

    @Override
    public void onAttach(Context context) {
        mCallback = (LoginListener)context;
        super.onAttach(context);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE && getContext() != null) {
            if (resultCode == RESULT_OK) {
                String s = PlaceAutocomplete.getPlace(getContext(), data).getAddress().toString();
                mOrigin.setText(s);
                mUser.setOrigin(s);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Toast.makeText(getContext(), R.string.went_wrong, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initFields(){
        mContinue = mView.findViewById(R.id.su_continue);
        mContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isOkToCreate()){
                    createProfile();
                    mCallback.onCreateAccount(mUser, mEmail.getText().toString(),
                            mPassword.getText().toString());
                } else {
                    Toast.makeText(getContext(), R.string.fields_missing, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mUsername = mView.findViewById(R.id.su_name);
        mUsername.addTextChangedListener(this);

        mPassword = mView.findViewById(R.id.su_password);
        mPassword.addTextChangedListener(this);

        mEmail = mView.findViewById(R.id.su_email);
        mEmail.addTextChangedListener(this);

        mConfirmPassword = mView.findViewById(R.id.su_confirm_password);
        mConfirmPassword.addTextChangedListener(this);

        mDate = mView.findViewById(R.id.su_date);
        mDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getContext() != null){
                    Calendar calendar = User.getCalender(mUser.getBirthDate());
                    DatePickerDialog dialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                            Calendar calendarChanged = Calendar.getInstance();
                            calendarChanged.set(i, i1, i2);
                            Format formatter1 = new SimpleDateFormat(User.FORMAT_DATE,
                                    Locale.getDefault());
                            String s1 = formatter1.format(calendarChanged.getTime());
                            mUser.setBirthDate(s1);
                            Format formatter2 = new SimpleDateFormat(User.FORMAT_DATE,
                                    Locale.getDefault());
                            String s2 = formatter2.format(calendarChanged.getTime());
                            mDate.setText(s2);
                        }
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH));
                    Calendar min = Calendar.getInstance();
                    min.set(min.get(Calendar.YEAR) - 100, min.get(Calendar.MONTH),
                            min.get(Calendar.DAY_OF_MONTH));
                    dialog.getDatePicker().setMinDate(min.getTimeInMillis());
                    Calendar max = Calendar.getInstance();
                    max.set(max.get(Calendar.YEAR) - 18, max.get(Calendar.MONTH),
                            max.get(Calendar.DAY_OF_MONTH));
                    dialog.getDatePicker().setMaxDate(max.getTimeInMillis());
                    dialog.show();
                }
            }
        });
        mDate.addTextChangedListener(this);

        mGender = mView.findViewById(R.id.su_gender);
        if(getContext() != null){
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource
                    (getContext(), R.array.genders, android.R.layout.simple_list_item_1);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mGender.setAdapter(adapter);
            mGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    mUser.setIsMale(i == 0);
                    updateButton();
                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });
        }

        mAboutMe = mView.findViewById(R.id.su_about_me);

        mInterests = mView.findViewById(R.id.su_interests);

        mMusic = mView.findViewById(R.id.su_movies);

        mOrigin = mView.findViewById(R.id.su_origin);
        mOrigin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(getActivity() != null){
                        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                                .build();
                        Intent intent = new PlaceAutocomplete.IntentBuilder
                                (PlaceAutocomplete.MODE_OVERLAY).setFilter(typeFilter)
                                .build(getActivity());
                        startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                    }
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        Toolbar toolbar = mView.findViewById(R.id.su_toolbar);
        toolbar.setTitle("Sign Up");
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            toolbar.setNavigationIcon(R.mipmap.arrow_rtl);
        } else {
            toolbar.setNavigationIcon(R.mipmap.arrow_ltr);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getActivity() != null){
                    getActivity().onBackPressed();
                }
            }
        });

    }

    public void onFailedRegister(boolean isExist){
        if(isExist){
            mEmail.setError(getString(R.string.user_exists));
        } else {
            Toast.makeText(getContext(), R.string.could_not_connect, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isOkToCreate(){
        if(mUsername.getText().toString().trim().equals("")){
            mUsername.setError(getString(R.string.required));
            return false;
        } else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(mEmail.getText().toString()).matches()){
            mEmail.setError(getString(R.string.invalid_email));
            return false;
        } else if(!mPassword.isCharactersCountValid()){
            mPassword.setError(getString(R.string.required));
            return false;
        } else if(!mConfirmPassword.isCharactersCountValid()){
            mConfirmPassword.setError(getString(R.string.required));
            return false;
        } else if(mOrigin.getText().toString().trim().equals("")){
            mOrigin.setError(getString(R.string.required));
            return false;
        } else if(!mPassword.getText().toString().equals(mConfirmPassword.getText().toString())){
            mPassword.setError(getString(R.string.pass_not_match));
            mConfirmPassword.setError(getString(R.string.pass_not_match));
            return false;
        } else {
            return true;
        }
    }

    private void createProfile(){
        mUser.setUsername(mUsername.getText().toString());
        mUser.setAboutMe(mAboutMe.getText().toString());
        mUser.setMusicBooks(mMusic.getText().toString());
        mUser.setInterests(mInterests.getText().toString());
    }

    private void updateButton(){
        if(getContext() != null){
            String s1 = mPassword.getText().toString();
            String s2 = mConfirmPassword.getText().toString();
            String s3 = mDate.getText().toString();
            String s4 = mUsername.getText().toString();

            mContinue.setEnabled(android.util.Patterns.EMAIL_ADDRESS.matcher(mEmail.getText().toString().trim()).matches()
                    && mPassword.isCharactersCountValid() && mUsername.isCharactersCountValid()
                    && mConfirmPassword.isCharactersCountValid() && !s3.equals("")
                    && !s4.equals("") && s1.equals(s2));
            if(mContinue.isEnabled()){
                mContinue.setBackgroundTintList(getResources().getColorStateList(R.color.message_outcome));
            } else {
                mContinue.setBackgroundTintList(getResources().getColorStateList(R.color.writing_color));
                if(!s1.equals(s2)){
                    mConfirmPassword.setError(getString(R.string.pass_not_match));
                }
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void afterTextChanged(Editable editable) {
        updateButton();
    }

    public void cleanFields(){
        mPassword.setText("");
        mUsername.setText("");
        mConfirmPassword.setText("");
        mDate.setText("");
        mGender.setSelection(0);
        mEmail.setText("");
        mAboutMe.setText("");
        mInterests.setText("");
        mOrigin.setText("");
        mMusic.setText("");
        mContinue.setEnabled(false);
        mContinue.setBackgroundTintList(getResources().getColorStateList(R.color.writing_color));
    }
}
