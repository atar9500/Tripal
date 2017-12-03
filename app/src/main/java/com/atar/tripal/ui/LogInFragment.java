package com.atar.tripal.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
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

    public LogInFragment() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_sign_in, container, false);

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
        mResetPassSheet = BottomSheetBehavior.from((LinearLayout)mView.findViewById(R.id.reset_pass_sheet));
        if(mResetPassSheet != null){
            mResetPassSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
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
//            Button dismiss = mView.findViewById(R.id.reset_cancel);
//            dismiss.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    mResetPassSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
//                }
//            });
        }
    }

    public void onFailedLogIn(boolean incorrect){
        if(incorrect){
            mPassword.setError(getString(R.string.incorrect_sign));
        } else {
            mPassword.setError(getString(R.string.could_not_connect));
        }
    }

    public int getSheetState(){
        return mResetPassSheet.getState();
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
            CharSequence s1 = mPassword.getText();
            CharSequence s2 = mEmail.getText();
            mLogIn.setEnabled(s1 != null && !s1.equals("") && s2 != null && !s2.equals(""));
            if(mLogIn.isEnabled()){
                mLogIn.setBackgroundTintList(getResources().getColorStateList(R.color.message_outcome));
            } else {
                mLogIn.setBackgroundTintList(getResources().getColorStateList(R.color.writing_color));
            }
        }
    }
}
