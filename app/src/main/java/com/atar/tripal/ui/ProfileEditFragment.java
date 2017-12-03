package com.atar.tripal.ui;


import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;

import com.atar.tripal.R;
import com.atar.tripal.db.Details;
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

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileEditFragment extends Fragment {


    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 2;

    public ProfileEditFragment() {}

    /**
     * Data
     */
    private User mUser;

    /**
     * UI Widgets
     */
    private View mView;
    private MaterialEditText mDate, mAboutMe, mInterests, mMusic, mOrigin;
    private Spinner mGender;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_profile_edit, container, false);

        setRetainInstance(true);

        mUser = Details.getProfile(getContext());

        initUIWidgets();

        setData();

        return mView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE && getContext() != null) {
            if (resultCode == RESULT_OK) {
                String s = PlaceAutocomplete.getPlace(getContext(), data).getAddress().toString();
                mOrigin.setText(s);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Toast.makeText(getContext(), R.string.went_wrong, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initUIWidgets(){
        mDate = mView.findViewById(R.id.pe_date);
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
                            Format formatter = new SimpleDateFormat("MMM dd, yyyy",
                                    Locale.getDefault());
                            String s = formatter.format(calendarChanged.getTime());
                            mDate.setText(s);
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
        mGender = mView.findViewById(R.id.pe_gender);
        if(getContext() != null){
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource
                    (getContext(), R.array.genders, android.R.layout.simple_list_item_1);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mGender.setAdapter(adapter);
        }
        mAboutMe = mView.findViewById(R.id.pe_about_me);
        mInterests = mView.findViewById(R.id.pe_interests);
        mMusic = mView.findViewById(R.id.pe_movies);
        mOrigin = mView.findViewById(R.id.pe_origin);
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
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setData(){
        mDate.setText(mUser.getBirthDate());
        if(mUser.getIsMale()){
            mGender.setSelection(0, true);
        } else {
            mGender.setSelection(1, true);
        }
        mAboutMe.setText(mUser.getAboutMe());
        mInterests.setText(mUser.getInterests());
        mMusic.setText(mUser.getMusicBooks());
        mOrigin.setText(mUser.getOrigin());
    }

    public User generateUser(){
        mUser.setInterests(mInterests.getText().toString());
        mUser.setMusicBooks(mMusic.getText().toString());
        mUser.setAboutMe(mAboutMe.getText().toString());
        mUser.setBirthDate(mDate.getText().toString());
        mUser.setIsMale(mGender.getSelectedItemPosition() == 0);
        mUser.setOrigin(mOrigin.getText().toString());
        return mUser;
    }

}
