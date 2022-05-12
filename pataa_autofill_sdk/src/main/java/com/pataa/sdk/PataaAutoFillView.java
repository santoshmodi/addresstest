package com.pataa.sdk;

import static com.pataa.sdk.AppConstants.ON_ACT_RSLT_PATAA_DATA;
import static com.pataa.sdk.AppConstants.REFRESS_INTERVAL_FOR_VALIDATION_CHECK;
import static com.pataa.sdk.AppConstants.metaClientKey;
import static com.pataa.sdk.AppConstants.metaEnableDebugKey;
import static com.pataa.sdk.AppConstants.metaEnableDevelopmentKey;
import static com.pataa.sdk.Utill.getAppHash;
import static com.pataa.sdk.Utill.getMeta;
import static com.pataa.sdk.Validation.SEARCH_PATAA_CODE_MAXIMUM_THRESHOLD;
import static com.pataa.sdk.Validation.isPataaCodeValid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PataaAutoFillView extends FrameLayout {
//    private ActivityResultLauncher<Intent> launchSomeActivity;
    public static boolean enableLogger;
    public static String sha1;
    private OnAddress address;
    private Activity activity;
    private View vCreateNow;
    private View vContainer;
    private View vValidPataa;
    private View vClickHere;
    private View btnGreenTickPataaFound;
    private View btnCrossPataaNotFound;
    private View btnAddAddress;
    private TextView tvShipTo;
    private AlertDialog whatIsPataaDialog;
    private EditText editText;
    private View btnAutoFill;
private Context context;
private String apikey = "";
    public PataaAutoFillView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context =context;
        initView();
    }

    public PataaAutoFillView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context =context;
        initView();
    }

    public PataaAutoFillView(Context context, OnAddress onAddress) {
        super(context);
        this.address = onAddress;
        this.context =context;
        initView();
    }

    public PataaAutoFillView setAddressCallBack(OnAddress onAddress) {
        this.address = onAddress;
        return this;
    }
    public PataaAutoFillView setCurrentActivity(Activity activity, String apikey) {
        this.activity = activity;
        this.apikey = apikey;
        return this;
    }



    public PataaAutoFillView setCurrentActivity(Activity activity) {
        this.activity = activity;

//        launchSomeActivity = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
//                new ActivityResultCallback<ActivityResult>() {
//                    @Override
//                    public void onActivityResult(ActivityResult result) {
//                        if (result.getResultCode() == Activity.RESULT_OK) {
//                            Intent data = result.getData();
//                            String pc = data.getStringExtra(ON_ACT_RSLT_PATAA_DATA);
//                            if (pc != null) {
//                                getPataadetail(pc);
//                            }
//                        }
//                    }
//                });
        return this;
    }

    public Activity getCurrentActivity() {
        if (activity == null) {
            Logger.e("Current activity object is required please se the data on \nsetCurrentActivity(ACTIVITY);");
            return null;
        }
        return activity;
    }

    public void getPataadetail(String pataaCode) {
        if (Utill.isNotNullOrEmpty(pataaCode) && Validation.isPataaCodeValid(pataaCode)) {
            if (editText != null && editText != null) {
                editText.setText(pataaCode);
                handler.removeCallbacks(runnable);
                btnAutoFill.performClick();
                btnAddAddress.setVisibility(GONE);
            }
        }
    }

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                String s = editText.getText().toString().trim();

                vValidPataa.setVisibility(GONE);
                vCreateNow.setVisibility(GONE);

                if (isPataaCodeValid(s)) {
                    Logger.e("is valid pataa code : " + s);
                    setToInitialViewState(false);
                    btnAutoFill.setVisibility(VISIBLE);
                    btnAddAddress.setVisibility(GONE);
                } else {
                    Logger.e("is not a valid pataa : " + s);
                    setToInitialViewState(false);
                }
            } catch (Exception e) {
                Logger.e(e.getMessage());
                e.printStackTrace();
            }
        }
    };

    private void initView() {
        enableLogger = Utill.getMetaBoolean(getContext(), metaEnableDebugKey());
        View inflatedView = inflate(getContext(), R.layout.pataa_auto_fill_cell, this);
        editText = inflatedView.findViewById(R.id.edtPataaEntry);
        View edtCaret = inflatedView.findViewById(R.id.edtCaret);
        View tvClickHere = inflatedView.findViewById(R.id.tvClickHere);
        View edtHint = inflatedView.findViewById(R.id.edtHint);
        View tvCreateNow = inflatedView.findViewById(R.id.tvCreateNow);
        btnAddAddress = inflatedView.findViewById(R.id.btnAddAddress);
        btnCrossPataaNotFound = inflatedView.findViewById(R.id.btnCrossPataaNotFound);
        btnGreenTickPataaFound = inflatedView.findViewById(R.id.btnGreenTickPataaFound);
        vContainer = inflatedView.findViewById(R.id.vContainer);
        btnAutoFill = inflatedView.findViewById(R.id.btnAutoFill);
        vClickHere = inflatedView.findViewById(R.id.vClickHere);
        vCreateNow = inflatedView.findViewById(R.id.vCreateNow);
        vValidPataa = inflatedView.findViewById(R.id.vValidPataa);
        tvShipTo = inflatedView.findViewById(R.id.tvShipTo);

        tvClickHere.setOnClickListener(v -> {
            callCreatePataa();
        });
        btnAddAddress.setOnClickListener(v -> {
            callCreatePataa();
        });
        tvCreateNow.setOnClickListener(v -> {
            callCreatePataa();
        });

        btnAutoFill.setOnClickListener(v -> {
            if (Utill.isNetworkConnected(v.getContext())) getPataadetail(editText);
            else if (address != null) address.onNetworkIsNotAvailable();
        });

        btnCrossPataaNotFound.setOnClickListener(v -> {
            setToInitialViewState(true);
        });

        inflatedView.findViewById(R.id.vWhatIsPataa).setOnClickListener(v -> {
            openWhatIsPataa(this);
        });

        addFiltersToEditText(editText);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    if (edtHint != null) {
                        edtHint.setVisibility(charSequence.length() > 0 ? GONE : VISIBLE);
                    }

                    handler.removeCallbacks(runnable);
                    handler.postDelayed(runnable, REFRESS_INTERVAL_FOR_VALIDATION_CHECK);
                    edtCaret.setVisibility(charSequence.length() > 0 ? VISIBLE : GONE);
                } catch (Exception e) {
                    Logger.e(e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if (Validation.isPataaCodeValid(editText.getText().toString().trim())) {
                        btnAutoFill.performClick();
                        return false;
                    }
                    return true;
                }
                return false;
            }
        });

    }

    public String getSha1() {
        if (!Utill.isNotNullOrEmpty(sha1)) {
            sha1 = getAppHash(getCurrentActivity());
        }
        Log.e("SHA1", sha1);
        return sha1;
    }

    private void callCreatePataa() {
        if (getCurrentActivity() != null) {
            String url = vContainer.getContext().getString(Utill.getMetaBoolean(vContainer.getContext(), metaEnableDevelopmentKey()) ? R.string.create_pataa_web_url_development : R.string.create_pataa_web_url);
            Intent intent = CreatePataaActivity.createPataa(vContainer.getContext(), url, getMeta(getContext(), metaClientKey()), new DialogCallback() {
                @Override
                public void Response(String pataa) {
                    getPataadetail(pataa);
                }
            });
           activity.startActivityForResult( intent, 200);

//            CreatePataaActivityDialog dailog = new CreatePataaActivityDialog(vContainer.getContext(), ((Activity)context), url, new DialogCallback() {
//                @Override
//                public void Response(String pataa) {
//
//                }
//            });
//            dailog.show();
        }
    }

    private void openWhatIsPataa(View view) {
        if (whatIsPataaDialog != null) {
            whatIsPataaDialog.show();
        } else
            whatIsPataaDialog = Utill.showWhatIsPataaDialog(view.getContext(), new WhatIsPataaDialogCallBack() {
                @Override
                public void clickOnCreatePataa() {
                    callCreatePataa();
                }

                @Override
                public void onDismiss() {

                }

                @Override
                public void onPataaClick(String pataaCode) {

                }
            });
    }

    private void addFiltersToEditText(EditText editText) {
        try {
            ArrayList<InputFilter> curInputFilters = new ArrayList<InputFilter>(Arrays.asList(editText.getFilters()));
            curInputFilters.add(0, new AlphaNumericAndDashInputOnlyA2ZFilter());
            curInputFilters.add(1, new InputFilter.AllCaps());
            curInputFilters.add(2, new InputFilter.LengthFilter(SEARCH_PATAA_CODE_MAXIMUM_THRESHOLD));
            InputFilter[] newInputFilters = curInputFilters.toArray(new InputFilter[curInputFilters.size()]);
            editText.setFilters(newInputFilters);
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        } catch (Exception e) {
            Logger.e(e.getMessage());
            e.printStackTrace();
        }
    }

    public void getPataadetail(EditText editText) {
        if (getCurrentActivity() == null) return;

        Api.getApi(getContext()).getPataaDetail(
                apikey.length()==0?getMeta(getContext(), metaClientKey()):apikey,
                editText.getText().toString().trim().toUpperCase(),
                getSha1()
        ).enqueue(new Callback<GetPataaDetailResponse>() {

            @Override
            public void onResponse(Call<GetPataaDetailResponse> call, Response<GetPataaDetailResponse> response) {
                try {
                    Logger.e("request : " + new Gson().toJson(call.request().body()));
                    Logger.e("api data");
                    Logger.e(response.body().getMsg());
                    Logger.e(new Gson().toJson(response.body()));
                    Log.e("request",  new Gson().toJson(call.request().body()));
                    Log.e("response",  new Gson().toJson(response.body()));
                    if (response.body().getStatus() == 200) {
                        setPataaDetail(response.body());
                    } else if (response.body().getStatus() == 204) {
                        setPataaDetailNotFound(response.body());
                    } else {
                        Logger.e(response.body().getMsg());
                        Logger.e(response.body());
                    }
                } catch (Exception e) {
                    Logger.e(e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<GetPataaDetailResponse> call, Throwable t) {
                try {
                    Logger.e(call.request().body());
                    t.printStackTrace();
                } catch (Exception e) {
                    Logger.e(e.getMessage());
                    e.printStackTrace();
                }
            }

        });
    }

    private void setPataaDetail(GetPataaDetailResponse pataaDetail) {
        try {
            Drawable drawableValidPataa = vContainer.getContext().getDrawable(R.drawable.bg_green_border);

            if (tvShipTo != null) {
                tvShipTo.setText(tvShipTo.getContext().getString(R.string.ship_to, pataaDetail.getResult().getPataa().getFormattedAddressShort()));
                vContainer.setBackground(drawableValidPataa);
                vCreateNow.setVisibility(GONE);
                btnCrossPataaNotFound.setVisibility(GONE);
                btnAutoFill.setVisibility(GONE);
                vClickHere.setVisibility(GONE);

                btnGreenTickPataaFound.setVisibility(VISIBLE);
                vValidPataa.setVisibility(VISIBLE);
            }
            address.onPataaFound(pataaDetail.getResult().getUser(), pataaDetail.getResult().getPataa());
        } catch (Exception e) {
            Logger.e(e.getMessage());
            e.printStackTrace();
        }
    }

    private void setPataaDetailNotFound(GetPataaDetailResponse pataaDetail) {
        try {
            Drawable drawableNotFound = vContainer.getContext().getDrawable(R.drawable.bg_red_border);
            vContainer.setBackground(drawableNotFound);
            vCreateNow.setVisibility(VISIBLE);
            btnCrossPataaNotFound.setVisibility(VISIBLE);

            vClickHere.setVisibility(GONE);
            vValidPataa.setVisibility(GONE);
            btnAutoFill.setVisibility(GONE);
            btnGreenTickPataaFound.setVisibility(GONE);
            btnAddAddress.setVisibility(GONE);

            address.onPataaNotFound(pataaDetail.getMsg());
        } catch (Exception e) {
            Logger.e(e.getMessage());
            e.printStackTrace();
        }
    }

    private void setToInitialViewState(boolean clearTheField) {
        try {
            Drawable drawable = vContainer.getContext().getDrawable(R.drawable.bg_black_border);
            vContainer.setBackground(drawable);
            vClickHere.setVisibility(VISIBLE);

            vCreateNow.setVisibility(GONE);
            btnCrossPataaNotFound.setVisibility(GONE);
            vValidPataa.setVisibility(GONE);
            btnAutoFill.setVisibility(GONE);
            btnGreenTickPataaFound.setVisibility(GONE);
            btnAddAddress.setVisibility(VISIBLE);
            if (clearTheField) editText.setText("");
        } catch (Exception e) {
            Logger.e(e.getMessage());
            e.printStackTrace();
        }
    }
}