package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Vehicle;

public class ViewVehicleActivity extends AppCompatActivity {

    private static final String TAG = "ViewVehicle";

    // Widgets
    private ImageButton btnClose, btnEdit, btnSelectProfile;
    private TextView tvName, tvType, tvPlateno, tvStatus;
    private ImageView profile;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Vehicle mVehicle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_vehicle);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        btnClose = findViewById(R.id.btn_close);
        btnEdit = findViewById(R.id.btn_edit);
        btnSelectProfile = findViewById(R.id.btn_select_profile);
        tvName = findViewById(R.id.tv_name);
        tvType = findViewById(R.id.tv_type);
        tvPlateno = findViewById(R.id.tv_plate_no);
        tvStatus = findViewById(R.id.tv_status);
        profile = findViewById(R.id.profile);
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnClose.setOnClickListener(v -> goBack());
        btnEdit.setOnClickListener(v -> {
            if (mVehicle != null) {
                Intent intent = new Intent(this, EditVehicleActivity.class);
                intent.putExtra("vehicle_id", mVehicle.vehicleId);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        int id = getIntent().getIntExtra("vehicle_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Vehicle ID")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching vehicle with id=" + id + " " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).vehicles().getVehicle(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(vehicles -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + vehicles.size() + " " + Thread.currentThread());
            mVehicle = vehicles.get(0);
            displayInfo(mVehicle);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Vehicle entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void displayInfo(Vehicle vehicle) {
        if (vehicle != null) {
            tvName.setText(vehicle.vehicleName);
            tvType.setText(vehicle.vehicleType);
            tvPlateno.setText(vehicle.plateNo);
            tvStatus.setText(vehicle.vehicleStatus);
        }
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
