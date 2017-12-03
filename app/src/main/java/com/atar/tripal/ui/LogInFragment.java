package com.atar.tripal.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atar.tripal.R;
import com.atar.tripal.callbacks.LoginListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class LogInFragment extends Fragment implements TextWatcher {

    private View mView;
    private LoginListener mCallback;

    /**
     * UI Widgets
     */
    private EditText mEmail, mPassword;
    private Button mLogIn;
    private BottomSheetBehavior mResetPassSheet;
    private EditText mEmailReset;
    private Toolbar mToolbar;

    public LogInFragment() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_log_in, container, false);

        setRetainInstance(true);

        initUIWidgets();

        return mView;
    }

    @Override
    public void onAttach(Context context) {
        mCallback = (LoginListener)context;
        super.onAttach(context);
    }

    private void initUIWidgets(){
        mLogIn = mView.findViewById(R.id.ln_enter);
        mLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onSignInClick(mEmail.getText().toString(), mPassword.getText().toString());
            }
        });

        mEmail = mView.findViewById(R.id.ln_email);
        mEmail.addTextChangedListener(this);

        mPassword = mView.findViewById(R.id.ln_password);
        mPassword.addTextChangedListener(this);

        TextView reset = mView.findViewById(R.id.ln_forgot);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mResetPassSheet != null){
                    mResetPassSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    View dialogView = getLayoutInflater().inflate(R.layout.reset_pass_sheet, null);
                    final AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(dialogView).show();
                    final EditText emailField = dialogView.findViewById(R.id.reset_email);
                    Button resetBtn = dialogView.findViewById(R.id.reset_ok);
                    resetBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String email = emailField.getText().toString();
                            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                                emailField.setError(getString(R.string.invalid_email));
                            } else {
                                mCallback.onResetClick(email);
                            }
                        }
                    });
                    Button dismiss = dialogView.findViewById(R.id.reset_cancel);
                    dismiss.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });
                }
            }
        });

        // Initialling Reset Password Sheet
        LinearLayout layout = mView.findViewById(R.id.reset_pass_sheet);

        // Because Tablets have AlertDialog instead of BottomSheet, we need to know if its a tablet or not.
        if(layout != null){
            mResetPassSheet = BottomSheetBehavior.from(layout);
            mResetPassSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
            mResetPassSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if(newState == BottomSheetBehavior.STATE_DRAGGING ||
                            newState == BottomSheetBehavior.STATE_SETTLING){
                        mView.findViewById(R.id.reset_pass_shadow).setVisibility(View.VISIBLE);
                    } else {
                        mView.findViewById(R.id.reset_pass_shadow).setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    if(slideOffset <= 0){
                        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                            mToolbar.setNavigationIcon(R.mipmap.arrow_rtl);
                        } else {
                            mToolbar.setNavigationIcon(R.mipmap.arrow_ltr);
                        }
                    } else {
                        mToolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
                    }
                }
            });
            mEmailReset = mView.findViewById(R.id.reset_email);
            Button resetBtn = mView.findViewById(R.id.reset_ok);
            resetBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String email = mEmailReset.getText().toString();
                    if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                        mEmailReset.setError(getString(R.string.invalid_email));
                    } else {
                        mCallback.onResetClick(email);
                    }
                }
            });
        }

        mToolbar = mView.findViewById(R.id.ln_toolbar);
        mToolbar.setTitle(R.string.log_in);
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            mToolbar.setNavigationIcon(R.mipmap.arrow_rtl);
        } else {
            mToolbar.setNavigationIcon(R.mipmap.arrow_ltr);
        }
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getActivity() != null){
                    getActivity().onBackPressed();
                }
            }
        });
    }

    public void onFailedLogIn(boolean incorrect){
        if(incorrect){
            mPassword.setError(getString(R.string.incorrect_sign));
        } else {
            mPassword.setError(getString(R.string.could_not_connect));
        }
    }

    public int getSheetState(){
        if(mResetPassSheet != null){
            return mResetPassSheet.getState();
        } else {
            return -33;
        }
    }

    public void hideSheet(){
        mResetPassSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public void setError(){
        mEmailReset.setError(getString(R.string.user_not_exist));
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void afterTextChanged(Editable editable) {
        if(getContext() != null){
            mLogIn.setEnabled(android.util.Patterns.EMAIL_ADDRESS.matcher
                    (mEmail.getText().toString().trim()).matches() &&
                    !mPassword.getText().toString().equals(""));
            if(mLogIn.isEnabled()){
                mLogIn.setBackgroundTintList(getResources().getColorStateList(R.color.message_outcome));
            } else {
                mLogIn.setBackgroundTintList(getResources().getColorStateList(R.color.writing_color));
            }
        }
    }

    public void cleanFields(){
        mPassword.setText("");
        if(mEmailReset != null){
            mEmailReset.setText("");
        }
        mEmail.setText("");
        mLogIn.setEnabled(false);
        mLogIn.setBackgroundTintList(getResources().getColorStateList(R.color.writing_color));
    }
}
