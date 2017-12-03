package com.atar.tripal.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.atar.tripal.BuildConfig;
import com.atar.tripal.R;
import com.atar.tripal.adapters.NearbyPlacesAdapter;
import com.atar.tripal.adapters.PlacePhotoAdapter;
import com.atar.tripal.adapters.PlacesPredictionsAdapter;
import com.atar.tripal.callbacks.HangoutCallback;
import com.atar.tripal.callbacks.SearchResultsCallback;
import com.atar.tripal.callbacks.SuggestionsCallback;
import com.atar.tripal.db.Details;
import com.atar.tripal.net.NetConstants;
import com.atar.tripal.net.NetworkRequestQueue;
import com.atar.tripal.objects.Hangout;
import com.atar.tripal.objects.PlaceInfo;
import com.atar.tripal.objects.PlacePrediction;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBufferResponse;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResponse;
import com.google.android.gms.location.places.PlacePhotoResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import me.relex.circleindicator.CircleIndicator;

public class MapFragment extends Fragment implements OnMapReadyCallback,
        SuggestionsCallback, SearchResultsCallback {

    private static final String NEARBY_SEARCH = "nearbysearch/json?";
    private static final String TEXT_SEARCH = "textsearch/json?";

    private static final String TAG = "MapFragment";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 31;

    private GoogleMap mMap;
    private GeoDataClient mGeoDataClient;

    /**
     * Data
     */
    private LatLngBounds mArea;
    private HashMap<String, PlaceInfo> mPlaceResults;
    private List<Bitmap> mPlacePhotos;

    private HangoutCallback mCallback;

    /**
     * UI WIDGETS
     */
    private View mView;
    private ProgressBar mLoadingSearch;
    private BottomSheetBehavior mPlaceSheet, mNearbySheet;

    /**
     * UI Widgets for place's card
     */
    private TextView mError;
    private ProgressBar mLoadingPhotos;
    private PlacePhotoAdapter mPhotoAdapter;
    private TextView mPlaceType, mPlaceAddress, mPlaceTitle;
    private RatingBar mPlaceRating;
    private Button mSetPlace;
    private ViewPager mPlaceSlider;

    /**
     * UI Widgets for the Suggestions list
     */
    private PlacesPredictionsAdapter mPredictionsAdapter;
    private ProgressBar mLoadingSuggestions;

    public MapFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_map, container, false);

        mPlaceResults = new HashMap<>();
        mPlacePhotos = new ArrayList<>();

        initMap();
        initClients();
        initUIWidgets();

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(getActivity() != null){
            Hangout hangout = mCallback.getHangout();
            if(hangout != null && mPlaceResults.size() == 0) {
                mGeoDataClient.getPlaceById(hangout.getPlaceId())
                        .addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                        if (task.isSuccessful()) {
                            PlaceBufferResponse places = task.getResult();
                            if(places.getCount() > 0){
                                PlaceInfo info = new PlaceInfo(places.get(0));
                                mMap.clear();
                                mPlaceResults.clear();
                                mPlaceResults.put(info.getId(), info);
                                markPlaceOnMap(info);
                                CameraUpdate update = CameraUpdateFactory.newLatLngZoom
                                        (new LatLng(info.getLatitude(),
                                        info.getLongtitude()), 17);
                                mMap.animateCamera(update);
                            }
                            places.release();
                        } else {
                            Log.e(TAG, "Place not found.");
                            Toast.makeText(getContext(), R.string.no_results_found,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
        hideKeyboard();
    }

    @Override
    public void onAttach(Context context) {
        mCallback = (HangoutCallback)context;
        super.onAttach(context);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mPlaceSlider.setCurrentItem(0, true);
                mPlacePhotos.clear();
                mPhotoAdapter.notifyDataSetChanged();
                showPlace((String) marker.getTag());
                return false;
            }
        });
        LatLng currentLocation = Details.getLocation(getContext());
        if(currentLocation.latitude != 0 && currentLocation.longitude != 0){
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom
                    (currentLocation, 13);
            mMap.animateCamera(update);
        }
        mMap.setBuildingsEnabled(true);
        mMap.setMinZoomPreference(10);
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mArea = mMap.getProjection().getVisibleRegion().latLngBounds;
            }
        });

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mMap.setPadding(0,Math.round(60 * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)),
                0, Math.round(45 * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)));

        if(getContext() != null){
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_retro_style));
            if(checkPermissions()){
                try{
                    mMap.setMyLocationEnabled(true);
                } catch (SecurityException e){
                    e.printStackTrace();
                }
            } else {
                requestPermissions(PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                try{
                    mMap.setMyLocationEnabled(true);
                } catch (SecurityException e){
                    e.printStackTrace();
                }
            } else {
                showSnackbar(R.string.permission_location_rationale,
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
        }
    }

    @Override
    public void onSuggestionClick(String placeId) {
        mMap.clear();
        mPlacePhotos.clear();
        mPlaceResults.clear();
        mLoadingSearch.setVisibility(View.VISIBLE);
        mGeoDataClient.getPlaceById(placeId).addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                if (task.isSuccessful()) {
                    PlaceBufferResponse places = task.getResult();
                    PlaceInfo info = new PlaceInfo(places.get(0));
                    mMap.clear();
                    mPlaceResults.clear();
                    mPlaceResults.put(info.getId(), info);
                    mLoadingSearch.setVisibility(View.INVISIBLE);
                    markPlaceOnMap(info);
                    places.release();
                }
            }
        });
        hideKeyboard();
        mPredictionsAdapter.setPlacePredictions(null);
    }

    @Override
    public void findNearby(String s) {
        mMap.clear();
        mPlacePhotos.clear();
        mPlaceResults.clear();
        mLoadingSearch.setVisibility(View.VISIBLE);
        LatLng location = Details.getLocation(getContext());
        searchForPlaces("https://maps.googleapis.com/maps/api/place/" + NEARBY_SEARCH +
                "type=" + s + "&location=" + location.latitude + "," + location.longitude +
                "&radius=" + 10000 +  "&key=" + NetConstants.SECOND_API + "&language=" +
                Locale.getDefault().getDisplayLanguage().substring(0, 2), NEARBY_SEARCH);
        hideKeyboard();
    }

    private void initUIWidgets(){

        // Initialling the search suggestions
        mPredictionsAdapter = new PlacesPredictionsAdapter(this);
        RecyclerView listSuggestions = mView.findViewById(R.id.map_suggestions);
        listSuggestions.setAdapter(mPredictionsAdapter);
        listSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));

        mLoadingSuggestions = mView.findViewById(R.id.map_loading);

        // Initialling the search field
        mLoadingSearch = mView.findViewById(R.id.map_loading_search);

        final SearchView searchField = mView.findViewById(R.id.map_search);
        searchField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mPredictionsAdapter.setPlacePredictions(null);
                if(!query.trim().equals("") && getActivity() != null){
                    mLoadingSearch.setVisibility(View.VISIBLE);
                    mMap.clear();
                    mPlacePhotos.clear();
                    mPlaceResults.clear();
                    LatLng location = Details.getLocation(getContext());
                    searchForPlaces("https://maps.googleapis.com/maps/api/place/" + TEXT_SEARCH +
                            "query=" + query.trim() + "&location=" + location.latitude + "," + location.longitude +
                            "&radius=" + 10000 + "&key=" + NetConstants.SECOND_API + "&language=" +
                            Locale.getDefault().getDisplayLanguage().substring(0, 2), TEXT_SEARCH);
                }
                hideKeyboard();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mPredictionsAdapter.setPlacePredictions(null);
                if(newText != null && !newText.trim().equals("")){
                    findPredictions(newText);
                }
                return false;
            }
        });
        searchField.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mLoadingSearch.setVisibility(View.INVISIBLE);
                mPredictionsAdapter.setPlacePredictions(null);
                hideKeyboard();
                return false;
            }
        });

        // Initialling the Nearby Suggestions Sheet
        mNearbySheet = BottomSheetBehavior.from((LinearLayout)mView.findViewById(R.id.nearby_sheet));

        RecyclerView nearbyList = mView.findViewById(R.id.nearby_suggestions);
        nearbyList.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        nearbyList.setHasFixedSize(true);
        nearbyList.setAdapter(new NearbyPlacesAdapter(this));

        // Initialling the Bottom Sheet for places
        mPlaceSheet = BottomSheetBehavior.from((LinearLayout)mView.findViewById(R.id.hangout_sheet));
        mPlaceSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        mPlaceSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState != BottomSheetBehavior.STATE_HIDDEN) {
                    mView.findViewById(R.id.map_place_shadow).setVisibility(View.VISIBLE);
                } else {
                    mView.findViewById(R.id.map_place_shadow).setVisibility(View.INVISIBLE);
                    mError.setVisibility(View.INVISIBLE);
                    mLoadingPhotos.setVisibility(View.INVISIBLE);
                    mPlacePhotos.clear();
                    mPhotoAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
        mPlaceType = mView.findViewById(R.id.map_sheet_type);
        mPlaceAddress = mView.findViewById(R.id.map_sheet_address);
        mPlaceTitle = mView.findViewById(R.id.map_sheet_title);
        mSetPlace = mView.findViewById(R.id.map_sheet_set);
        mPlaceRating = mView.findViewById(R.id.map_sheet_rating);

        mPlaceSlider = mView.findViewById(R.id.map_sheet_slider);
        mPhotoAdapter = new PlacePhotoAdapter(mPlacePhotos, getContext());
        mPlaceSlider.setAdapter(mPhotoAdapter);
        mLoadingPhotos = mView.findViewById(R.id.map_photo_loading);
        mError = mView.findViewById(R.id.map_loading_error);
        CircleIndicator indicator = mView.findViewById(R.id.map_sheet_indicator);
        indicator.setViewPager(mPlaceSlider);
        mPhotoAdapter.registerDataSetObserver(indicator.getDataSetObserver());
    }

    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_map);
        mapFragment.getMapAsync(this);
    }

    private void initClients(){
        if(getContext() != null){
            mGeoDataClient = Places.getGeoDataClient
                    (getContext(), null);
        }
    }

    public void findPredictions(String query){
        mLoadingSuggestions.setVisibility(View.VISIBLE);
        mPredictionsAdapter.setPlacePredictions(null);
        AutocompleteFilter autocompleteFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                .build();
        Task<AutocompletePredictionBufferResponse> placeResult = mGeoDataClient
                .getAutocompletePredictions(query, mArea, autocompleteFilter);
        placeResult.addOnCompleteListener(new OnCompleteListener<AutocompletePredictionBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<AutocompletePredictionBufferResponse> task) {
                AutocompletePredictionBufferResponse likelyPlaces = task.getResult();
                List<PlacePrediction> placePredictions = new ArrayList<>();
                for (AutocompletePrediction autocompletePrediction : likelyPlaces) {
                    PlacePrediction placePrediction = new PlacePrediction();
                    placePrediction.setId(autocompletePrediction.getPlaceId());
                    placePrediction.setPrimaryText(autocompletePrediction
                            .getPrimaryText(null).toString());
                    placePrediction.setSecondaryText(autocompletePrediction
                            .getSecondaryText(null).toString());
                    placePrediction.setTypes(autocompletePrediction.getPlaceTypes());
                    placePredictions.add(placePrediction);
                }
                mLoadingSuggestions.setVisibility(View.GONE);
                mPredictionsAdapter.setPlacePredictions(placePredictions);
                likelyPlaces.release();
            }
        });
    }

    private void markPlaceOnMap(PlaceInfo info){
        LatLng where = new LatLng(info.getLatitude(), info.getLongtitude());
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(where)
                .draggable(false));
        marker.setTag(info.getId());
    }

    private void requestPermissions(final int requestCode) {
        if(getActivity() != null){
            boolean shouldProvideRationale =
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (shouldProvideRationale) {
                Log.i(TAG, "Displaying permission rationale to provide additional context.");
                showSnackbar(R.string.permission_location_rationale,
                        android.R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Request permission
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                        requestCode);
                            }
                        });
            } else {
                Log.i(TAG, "Requesting permission");
                if(getActivity() != null){
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            requestCode);
                }
            }
        }
    }

    private boolean checkPermissions() {
        if(getActivity() != null){
            int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION);
            return permissionState == PackageManager.PERMISSION_GRANTED;
        } else {
            return false;
        }
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        if(getActivity() != null){
            Snackbar.make(
                    getActivity().findViewById(R.id.hangout_content),
                    getString(mainTextStringId),
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(actionStringId), listener).show();
        }
    }

    private void showPlace(final String s){

        final PlaceInfo info = mPlaceResults.get(s);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(info.getLatitude(),
                info.getLongtitude()), 17));

        mPlaceSheet.setState(BottomSheetBehavior.STATE_HIDDEN);

        mError.setVisibility(View.INVISIBLE);
        mLoadingPhotos.setVisibility(View.INVISIBLE);

        mPlaceAddress.setText(info.getAddress());
        mPlaceTitle.setText(info.getName());
        if(info.getType() != 0){
            mPlaceType.setText(info.getType());
            mPlaceType.setVisibility(View.VISIBLE);
        } else {
            mPlaceType.setVisibility(View.GONE);
        }
        mPlaceRating.setRating(info.getStars());
        mSetPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onSetPlaceClick(info);
            }
        });

        Task<PlacePhotoMetadataResponse> photoMetadataResponse = mGeoDataClient
                .getPlacePhotos(info.getId());
        mLoadingPhotos.setVisibility(View.VISIBLE);
        mError.setVisibility(View.INVISIBLE);
        photoMetadataResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoMetadataResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlacePhotoMetadataResponse> task) {
                if(task.isSuccessful()){
                    PlacePhotoMetadataResponse photos = task.getResult();
                    PlacePhotoMetadataBuffer photoMetadataBuffer = photos.getPhotoMetadata();
                    for(int i = 0; i < photoMetadataBuffer.getCount(); i++){
                        PlacePhotoMetadata photoMetadata = photoMetadataBuffer.get(i);
                        Task<PlacePhotoResponse> photoResponse = mGeoDataClient
                                .getPhoto(photoMetadata);
                        photoResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoResponse>() {
                            @Override
                            public void onComplete(@NonNull Task<PlacePhotoResponse> task) {
                                if(task.isSuccessful() &&
                                        mPlaceSheet.getState() != BottomSheetBehavior.STATE_HIDDEN){
                                    PlacePhotoResponse photo = task.getResult();
                                    mLoadingPhotos.setVisibility(View.INVISIBLE);
                                    mError.setVisibility(View.INVISIBLE);
                                    mPlacePhotos.add(photo.getBitmap());
                                    mPhotoAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                        photoResponse.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                    if(photoMetadataBuffer.getCount() == 0){
                        mLoadingPhotos.setVisibility(View.INVISIBLE);
                        mError.setVisibility(View.VISIBLE);
                    }
                } else {
                    mLoadingPhotos.setVisibility(View.INVISIBLE);
                    mError.setVisibility(View.VISIBLE);
                }
            }
        });
        mPlaceSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mNearbySheet.setState(BottomSheetBehavior.STATE_COLLAPSED);

    }

    private void hideKeyboard(){
        if(getActivity() != null){
            View view = getActivity().getCurrentFocus();
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (view != null && imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public int getPlaceSheetState(){
        return mPlaceSheet.getState();
    }

    public void hidePlaceSheet(){
        mPlaceSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public int getNearbySheetState(){
        return mNearbySheet.getState();
    }

    public void collapseNearbySheet(){
        mNearbySheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void searchForPlaces(final String URL, final String type){
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
                URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{
                    JSONArray resultsArray = response.getJSONArray("results");
                    for(int i = 0; i < resultsArray.length(); i++){
                        PlaceInfo info = new PlaceInfo();
                        JSONObject result = resultsArray.getJSONObject(i);
                        if(type.equals("textsearch")){
                            info.setAddress(result.getString("formatted_address"));
                        } else {
                            info.setAddress(result.getString("vicinity"));
                        }
                        JSONObject geometry = result.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");
                        info.setLatitude(location.getDouble("lat"));
                        info.setLongtitude(location.getDouble("lng"));
                        info.setName(result.getString("name"));
                        info.setId(result.getString("place_id"));
                        try{
                            info.setStars((float)result.getDouble("rating"));
                        } catch (JSONException j){
                            j.printStackTrace();
                            info.setStars(0);
                        }
                        JSONArray types = result.getJSONArray("types");
                        for(int j = 0; j < types.length(); j++){
                            switch (types.getString(j)){
                                case "bar":
                                    info.setType(R.string.bar);
                                    break;
                                case "atm":
                                    info.setType(R.string.atm);
                                    break;
                                case "cafe":
                                    info.setType(R.string.cafe);
                                    break;
                                case "liquor_store":
                                    info.setType(R.string.liquor_store);
                                    break;
                                case "meal_takeaway":
                                    info.setType(R.string.restaurants);
                                    break;
                                case "city_hall":
                                    info.setType(R.string.city_hall);
                                    break;
                                case "library":
                                    info.setType(R.string.library);
                                    break;
                                case "night_club":
                                    info.setType(R.string.night_club);
                                    break;
                                case "shopping_mall":
                                    info.setType(R.string.shopping_mall);
                                    break;
                                case "store":
                                    info.setType(R.string.store);
                                    break;
                                case "park":
                                    info.setType(R.string.park);
                                    break;
                            }
                            if(info.getType() != 0){
                                break;
                            }
                        }
                        mPlaceResults.put(info.getId(), info);
                    }
                    String s = response.getString("next_page_token");
                    searchForPlaces(URL + "&pagetoken=" + s, type);
                } catch (JSONException e){
                    e.printStackTrace();
                    mLoadingSearch.setVisibility(View.INVISIBLE);
                    if(mPlaceResults.size() > 0){
                        CameraUpdate update = CameraUpdateFactory.newLatLngZoom
                                (Details.getLocation(getContext()), 13);
                        mMap.animateCamera(update);
                        for(PlaceInfo info: mPlaceResults.values()){
                            markPlaceOnMap(info);
                        }
                    } else {
                        Toast.makeText(getContext(), R.string.no_results_found,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mPlaceResults.clear();
                Toast.makeText(getContext(), R.string.no_connection, Toast.LENGTH_SHORT).show();
            }
        });
        NetworkRequestQueue.getInstance(getContext()).addToRequestQueue(request);
    }

}
