package io.zak.inventory;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Vehicle;

public class EditVehicleActivity extends AppCompatActivity {

    private static final String TAG = "AddVehicle";

    private EditText etName, etPlateno;
    private Spinner typeSpinner, statusSpinner;
    private ImageButton btnBack, btnDelete;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private Drawable errorDrawable;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Vehicle mVehicle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vehicle);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        // Widgets
        TextView tvTitle = findViewById(R.id.title);
        tvTitle.setText(R.string.edit_vehicle);

        etName = findViewById(R.id.et_name);
        typeSpinner = findViewById(R.id.type_spinner);
        etPlateno = findViewById(R.id.et_plate_no);
        statusSpinner = findViewById(R.id.status_spinner);
        btnBack = findViewById(R.id.btn_back);

        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setVisibility(View.VISIBLE); // make it visible

        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);

        errorDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_x_circle);

        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this,
                R.array.vehicle_types, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter1);

        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter2);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnDelete.setOnClickListener(v -> {
            if (mVehicle != null) {
                dialogBuilder.setTitle("Confirm Delete")
                        .setMessage("This will delete all data related to this Vehicle entry. " +
                                "Are you sure you want to delete this Vehicle entry?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            dialog.dismiss();
                            deleteVehicle();
                        });
                dialogBuilder.create().show();
            }
        });
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) {
                saveAndClose();
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
                    .setMessage("Invalid Vehicle id.")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Vehicle entry: " + Thread.currentThread());
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
                    .setMessage("Error while fetching Vehicle entry: " +err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void displayInfo(Vehicle vehicle) {
        if (vehicle != null) {
            etName.setText(vehicle.vehicleName);
            etPlateno.setText(vehicle.plateNo);

            List<String> statusList = Arrays.asList(getResources().getStringArray(R.array.status_array));
            int pos = statusList.indexOf(vehicle.vehicleStatus);
            statusSpinner.setSelection(pos);

            List<String> typeList = Arrays.asList(getResources().getStringArray(R.array.status_array));
            int pos2 = statusList.indexOf(vehicle.vehicleType);
            typeSpinner.setSelection(pos);
        }
    }

    private boolean validated() {
        // remove all drawable in EditText
        etName.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        etPlateno.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        // check required fields
        if (etName.getText().toString().isBlank()) {
            etName.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
        }
        if (etPlateno.getText().toString().isBlank()) {
            etPlateno.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
        }

        return !etName.getText().toString().isBlank() && !etPlateno.getText().toString().isBlank();
    }

    private void saveAndClose() {
        mVehicle.vehicleName = Utils.normalize(etName.getText().toString());
        mVehicle.vehicleType = typeSpinner.getSelectedItem().toString();
        mVehicle.plateNo = Utils.normalize(etPlateno.getText().toString());
        mVehicle.vehicleStatus = statusSpinner.getSelectedItem().toString();

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Updating Vehicle entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).vehicles().update(mVehicle);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            Log.d(TAG, "Row affected=" + rowCount + ": " + Thread.currentThread());
            if (rowCount > 0) {
                Toast.makeText(this, "Updated Vehicle entry.", Toast.LENGTH_SHORT).show();
            }
            goBack();
        }, err -> {
            Log.e(TAG, "Database Error: " + err);

            // show dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while updating Employe entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void deleteVehicle() {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Deleting Vehicle entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).vehicles().delete(mVehicle);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with row count=" + rowCount + " " + Thread.currentThread());
            if (rowCount > 0) {
                Toast.makeText(this, "Deleted Vehicle entry.", Toast.LENGTH_SHORT).show();
            }
            goToVehicleList();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while deleting Vehicle entry: " + err)
                    .setPositiveButton("OK", (d, w) -> {
                        d.dismiss();
                        goToVehicleList();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    private void goToVehicleList() {
        startActivity(new Intent(this, VehiclesActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}