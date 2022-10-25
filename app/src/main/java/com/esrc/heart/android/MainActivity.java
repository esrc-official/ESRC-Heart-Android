package com.esrc.heart.android;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.esrc.heart.sdk.android.ESRC;
import com.esrc.heart.sdk.android.ESRCException;
import com.esrc.heart.sdk.android.ESRCFragment;
import com.esrc.heart.sdk.android.ESRCLicense;
import com.esrc.heart.sdk.android.ESRCType;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "MainActivity";
    private static final String APP_ID = "";  // Application ID.

    // Permission
    private static final int PERMISSIONS_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {INTERNET, CAMERA, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE};

    // Property
    private ESRCType.Property mProperty = new ESRCType.Property(
            false,  // Whether visualize result or not. It is only valid If you bind the ESRC Fragment (i.e., Step 2).
            true,  // Whether analyze measurement environment or not.
            true,  // Whether detect face or not.
            true,  // Whether estimate remote hr or not. If enableFace is false, it is also automatically set to false.
            true,  // Whether analyze HRV or not. If enableFace or enableRemoteHR is false, it is also automatically set to false.
            true,  // Whether recognize engagement or not. If enableRemoteHR and enableHRV are false, it is also automatically set to false.
            true);  // Whether print information about ESRC processing.

    // Layout variables for FaceBox
    private TextView mFaceBoxText;
    private ImageView mFaceBoxImage;
    private GradientDrawable mFaceBoxDrawable;

    // Layout variables for Heart Rate
    private View mHRValContainer;
    private TextView mHRValText;
    private ProgressBar mHRSpinnerDummy;
    private ProgressBar mHRSpinner;

    // Layout variables for HRV-SDNN
    private View mHRVSdnnValContainer;
    private TextView mHRVSdnnValText;
    private ProgressBar mHRVSdnnSpinnerDummy;
    private ProgressBar mHRVSdnnSpinner;

    // Layout variables for HRV-RMSSD
    private View mHRVRmssdValContainer;
    private TextView mHRVRmssdValText;
    private ProgressBar mHRVRmssdSpinnerDummy;
    private ProgressBar mHRVRmssdSpinner;

    // Layout variables for HRV-lnLF
    private View mHRVlnLFValContainer;
    private TextView mHRVlnLFValText;
    private ProgressBar mHRVlnLFSpinnerDummy;
    private ProgressBar mHRVlnLFSpinner;

    // Layout variables for HRV-lnHF
    private View mHRVlnHFValContainer;
    private TextView mHRVlnHFValText;
    private ProgressBar mHRVlnHFSpinnerDummy;
    private ProgressBar mHRVlnHFSpinner;

    // Layout variables for ANS Balance
    private Group mAnsBalanceBarGroup;
    private SeekBar mAnsBalanceSeekbar;
    private ProgressBar mANSBalanceSpinnerDummy;
    private ProgressBar mANSBalanceSpinner;

    // Dialog variables
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ESRCFragment.newInstance())
                    .commit();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Request Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        // Initialize layout
        initLayout();

        // Initialize ESRC
        ESRC.init(APP_ID, this, new ESRCLicense.ESRCLicenseHandler() {
            @Override
            public void onValidatedLicense() {
                // Start
                start();
            }

            @Override
            public void onInvalidatedLicense() {
                Toast.makeText(getApplicationContext(), "Invalid license", Toast.LENGTH_SHORT).show();
            }
        });

        // Timer
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "If you want to use the ESRC SDK, please visit the homepage: https://www.esrc.co.kr",
                        Toast.LENGTH_LONG).show();
                MainActivity.this.finish();
            }
        }, 300000);
    }

    @Override
    protected void onDestroy() {
        // Stop
        stop();

        // Remove timer
        mHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    /**
     * Initialize layout.
     */
    private void initLayout() {
        // Initialize layout for FaceBox
        mFaceBoxText = findViewById(R.id.facebox_text);
        mFaceBoxImage = findViewById(R.id.facebox_image);
        mFaceBoxDrawable = (GradientDrawable) mFaceBoxImage.getBackground();

        // Initialize layout for Heart Rate
        mHRValContainer = findViewById(R.id.hr_val_container);
        mHRValText = findViewById(R.id.hr_val_text);
        mHRSpinnerDummy = findViewById(R.id.hr_spinner_dummy);
        mHRSpinner = findViewById(R.id.hr_spinner);

        // Initialize layout for HRV-SDNN
        mHRVSdnnValContainer = findViewById(R.id.hrv_sdnn_val_container);
        mHRVSdnnValText = findViewById(R.id.hrv_sdnn_val_text);
        mHRVSdnnSpinnerDummy = findViewById(R.id.hrv_sdnn_spinner_dummy);
        mHRVSdnnSpinner = findViewById(R.id.hrv_sdnn_spinner);

        // Initialize layout for HRV-RMSSD
        mHRVRmssdValContainer = findViewById(R.id.hrv_rmssd_val_container);
        mHRVRmssdValText = findViewById(R.id.hrv_rmssd_val_text);
        mHRVRmssdSpinnerDummy = findViewById(R.id.hrv_rmssd_spinner_dummy);
        mHRVRmssdSpinner = findViewById(R.id.hrv_rmssd_spinner);

        // Initialize layout for HRV-lnLF
        mHRVlnLFValContainer = findViewById(R.id.hrv_lnlf_val_container);
        mHRVlnLFValText = findViewById(R.id.hrv_lnlf_val_text);
        mHRVlnLFSpinnerDummy = findViewById(R.id.hrv_lnlf_spinner_dummy);
        mHRVlnLFSpinner = findViewById(R.id.hrv_lnlf_spinner);

        // Initialize layout for HRV-lnHF
        mHRVlnHFValContainer = findViewById(R.id.hrv_lnhf_val_container);
        mHRVlnHFValText = findViewById(R.id.hrv_lnhf_val_text);
        mHRVlnHFSpinnerDummy = findViewById(R.id.hrv_lnhf_spinner_dummy);
        mHRVlnHFSpinner = findViewById(R.id.hrv_lnhf_spinner);

        // Initialize layout for ANS Balance
        mAnsBalanceBarGroup = findViewById(R.id.ans_balance_bar_group);
        mAnsBalanceSeekbar = findViewById(R.id.ans_balance_seekbar);
        mANSBalanceSpinnerDummy = findViewById(R.id.ans_balance_spinner_dummy);
        mANSBalanceSpinner = findViewById(R.id.ans_balance_spinner);
    }

    /**
     * Starts ESRC process.
     */
    private void start() {
        // Show dummy spinners
        mHRSpinnerDummy.setVisibility(View.VISIBLE);
        mHRVSdnnSpinnerDummy.setVisibility(View.VISIBLE);
        mHRVRmssdSpinnerDummy.setVisibility(View.VISIBLE);
        mHRVlnLFSpinnerDummy.setVisibility(View.VISIBLE);
        mHRVlnHFSpinnerDummy.setVisibility(View.VISIBLE);
        mANSBalanceSpinnerDummy.setVisibility(View.VISIBLE);

        // Hide spinners
        mHRSpinner.setVisibility(View.GONE);
        mHRVSdnnSpinner.setVisibility(View.GONE);
        mHRVRmssdSpinner.setVisibility(View.GONE);
        mHRVlnLFSpinner.setVisibility(View.GONE);
        mHRVlnHFSpinner.setVisibility(View.GONE);
        mANSBalanceSpinner.setVisibility(View.GONE);

        // Start ESRC
        ESRC.start(mProperty, new ESRC.ESRCHandler() {
            @Override
            public void onAnalyzedMeasureEnv(ESRCType.MeasureEnv measureEnv, ESRCException e) {
                if (e == null) {
                    Log.d(TAG, "onAnalyzedMeasureEnv: " + measureEnv.toString());
                } else {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDetectedFace(ESRCType.Face face, ESRCException e) {
                if (e == null) {
                    Log.d(TAG, "onDetectedFace: " + face.toString());

                    // If face is detected
                    if (face.getIsDetect()) {
                        // Show FaceBox
                        int color = getResources().getColor(R.color.primary_color);
                        mFaceBoxText.setTextColor(color);
                        mFaceBoxDrawable.setStroke(8, color);
                    } else {
                        // Hide FaceBox
                        int color = getResources().getColor(R.color.gray);
                        mFaceBoxText.setTextColor(color);
                        mFaceBoxDrawable.setStroke(4, color);

                        // Hide containers
                        mHRValContainer.setVisibility(View.GONE);
                        mHRVSdnnValContainer.setVisibility(View.GONE);
                        mHRVRmssdValContainer.setVisibility(View.GONE);
                        mHRVlnLFValContainer.setVisibility(View.GONE);
                        mHRVlnHFValContainer.setVisibility(View.GONE);
                        mAnsBalanceBarGroup.setVisibility(View.GONE);
                    }
                } else {
                    e.printStackTrace();
                }
            }

            @Override
            public void didChangedProgressRatioOnRemoteHR(ESRCType.ProgressRatio progressRatio, ESRCException e) {
                if (e == null) {
                    Log.d(TAG, "didChangedProgressRatioOnRemoteHR: " + progressRatio.toString());

                    // Set HR spinner
                    mHRSpinner.setProgress((int) progressRatio.getProgress());

                    if (progressRatio.getProgress() > 0) {
                        // Hide dummy spinner
                        if (mHRSpinnerDummy.getVisibility() == View.VISIBLE) {
                            mHRSpinnerDummy.setVisibility(View.GONE);
                            mHRSpinner.setVisibility(View.VISIBLE);
                        }
                    }
                    else {
                        // Show dummy spinner
                        if (mHRSpinnerDummy.getVisibility() == View.GONE) {
                            mHRSpinnerDummy.setVisibility(View.VISIBLE);
                            mHRSpinner.setVisibility(View.GONE);
                        }
                    }
                } else {
                    e.printStackTrace();
                }
            }

            @Override
            public void onEstimatedRemoteHR(ESRCType.RemoteHR remoteHR, ESRCException e) {
                if (e == null) {
                    Log.d(TAG, "onEstimatedRemoteHR: " + remoteHR.toString());

                    // Hide HR spinner
                    if(mHRSpinner.getVisibility() == View.VISIBLE) {
                        mHRSpinner.setVisibility(View.GONE);
                        mHRSpinnerDummy.setVisibility(View.GONE);
                    }

                    // Set HR values
                    mHRValText.setText(Long.toString(Math.round(remoteHR.getHR())));

                    // Show container for HR
                    mHRValContainer.setVisibility(View.VISIBLE);
                } else {
                    e.printStackTrace();
                }
            }

            @Override
            public void didChangedProgressRatioOnHRV(ESRCType.ProgressRatio progressRatio, ESRCException e) {
                if (e == null) {
                    Log.d(TAG, "didChangedProgressRatioOnHRV: " + progressRatio.toString());

                    // Set HRV spinner
                    mHRVSdnnSpinner.setProgress((int) progressRatio.getProgress());
                    mHRVRmssdSpinner.setProgress((int) progressRatio.getProgress());
                    mHRVlnLFSpinner.setProgress((int) progressRatio.getProgress());
                    mHRVlnHFSpinner.setProgress((int) progressRatio.getProgress());
                    mANSBalanceSpinner.setProgress((int) progressRatio.getProgress());

                    // Hide dummy spinner
                    if(progressRatio.getProgress() > 0) {
                        if (mHRVSdnnSpinnerDummy.getVisibility() == View.VISIBLE) {
                            mHRVSdnnSpinnerDummy.setVisibility(View.GONE);
                            mHRVRmssdSpinnerDummy.setVisibility(View.GONE);
                            mHRVlnLFSpinnerDummy.setVisibility(View.GONE);
                            mHRVlnHFSpinnerDummy.setVisibility(View.GONE);
                            mANSBalanceSpinnerDummy.setVisibility(View.GONE);
                            mHRVSdnnSpinner.setVisibility(View.VISIBLE);
                            mHRVRmssdSpinner.setVisibility(View.VISIBLE);
                            mHRVlnLFSpinner.setVisibility(View.VISIBLE);
                            mHRVlnHFSpinner.setVisibility(View.VISIBLE);
                            mANSBalanceSpinner.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAnalyzedHRV(ESRCType.HRV hrv, ESRCException e) {
                if (e == null) {
                    Log.d(TAG, "onAnalyzedHRV: " + hrv.toString());

                    // Hide HRV spinner
                    if(mHRVSdnnSpinner.getVisibility() == View.VISIBLE) {
                        mHRVSdnnSpinnerDummy.setVisibility(View.GONE);
                        mHRVRmssdSpinnerDummy.setVisibility(View.GONE);
                        mHRVlnLFSpinnerDummy.setVisibility(View.GONE);
                        mHRVlnHFSpinnerDummy.setVisibility(View.GONE);
                        mANSBalanceSpinnerDummy.setVisibility(View.GONE);
                        mHRVSdnnSpinner.setVisibility(View.GONE);
                        mHRVRmssdSpinner.setVisibility(View.GONE);
                        mHRVlnLFSpinner.setVisibility(View.GONE);
                        mHRVlnHFSpinner.setVisibility(View.GONE);
                        mANSBalanceSpinner.setVisibility(View.GONE);
                    }

                    // Set HRV values
                    mHRVSdnnValText.setText(Double.toString((double)Math.round(hrv.getSdnn()*100) / 100));
                    mHRVRmssdValText.setText(Double.toString((double)Math.round(hrv.getRmssd()*100) / 100));
                    mHRVlnLFValText.setText(Double.toString((double)Math.round(hrv.getLnLf()*100) / 100));
                    mHRVlnHFValText.setText(Double.toString((double)Math.round(hrv.getLnHf()*100) / 100));
                    mAnsBalanceSeekbar.setProgress((int) hrv.getLfHf() + 2);

                    // Show container for HRV
                    mHRVSdnnValContainer.setVisibility(View.VISIBLE);
                    mHRVRmssdValContainer.setVisibility(View.VISIBLE);
                    mHRVlnLFValContainer.setVisibility(View.VISIBLE);
                    mHRVlnHFValContainer.setVisibility(View.VISIBLE);
                    mAnsBalanceBarGroup.setVisibility(View.VISIBLE);
                } else {
                    e.printStackTrace();
                }
            }

            @Override
            public void onRecognizedEngagement(ESRCType.Engagement engagement, ESRCException e) {
                if (e == null) {
//                    Log.d(TAG, "onRecognizedEngagement: " + engagement.toString());
                } else {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Stop ESRC process.
     */
    private void stop() {
        // Stop ESRC
        ESRC.stop();
    }

    /**
     * Check granted permissions.
     */
    private boolean hasPermissions(String[] permissions) {
        int result;

        for (String perms : permissions) {
            result = ContextCompat.checkSelfPermission(this, perms);

            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Request permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;

                    if (!cameraPermissionAccepted)
                        showDialogForPermission("If you reject permission, you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]");
                }
                break;
        }
    }

    /**
     * Show dialog for permission.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Permission");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("ALLOW", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("DENY", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }
}