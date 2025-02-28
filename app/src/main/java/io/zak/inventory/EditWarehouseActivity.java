package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Warehouse;

public class EditWarehouseActivity extends AppCompatActivity {

    private static final String TAG = "EditWarehouse";

    private EditText etName, etContact, etAddress;
    private ImageButton btnBack, btnDelete;
    private Button btnCancel, btnSave;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Warehouse mWarehouse; // to edit

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_warehouse);
        getWidgets();
        setListeners();
        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void getWidgets() {
        // Widgets
        TextView tvTitle = findViewById(R.id.title);
        tvTitle.setText(R.string.edit_warehouse);

        etName = findViewById(R.id.et_name);
        etContact = findViewById(R.id.et_contact);
        etAddress = findViewById(R.id.et_address);
        btnBack = findViewById(R.id.btn_back);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setVisibility(View.VISIBLE);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });

        btnDelete.setOnClickListener(v -> {
            if (mWarehouse != null) {
                dialogBuilder.setTitle("Confirm Delete")
                        .setMessage("This will delete this Warehouse entry and all of its stocks. " +
                                "Are you sure you want to delete this Warehouse entry?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            dialog.dismiss();
                            deleteWarehouse();
                        });
                dialogBuilder.create().show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // check id from Intent
        int id = getIntent().getIntExtra("warehouse_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Warehouse ID. Try again.")
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
        }

        // fetch Warehouse entry
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Warehouse entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).warehouses().getWarehouse(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with list size=" + list.size() + " " + Thread.currentThread());
            mWarehouse = list.get(0);
            displayInfo(mWarehouse);
        }, err -> {
            Log.d(TAG, "Database Error: " + err);
        }));
    }

    private void displayInfo(Warehouse warehouse) {
        etName.setText(warehouse.warehouseName);
        etAddress.setText(warehouse.warehouseAddress);
        etContact.setText(warehouse.warehouseContactNo);
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    private boolean validated() {
        boolean isValid = true;

        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError("Required");
            isValid = false;
        }
        if (etContact.getText().toString().trim().isEmpty()) {
            etContact.setError("Required");
            isValid = false;
        }
        if (etAddress.getText().toString().trim().isEmpty()) {
            etAddress.setError("Required");
            isValid = false;
        }

        return isValid;
    }

    private void saveAndClose() {
        mWarehouse.warehouseName = Utils.normalize(etName.getText().toString());
        mWarehouse.warehouseAddress = Utils.normalize(etAddress.getText().toString());
        mWarehouse.warehouseContactNo = Utils.normalize(etContact.getText().toString());

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Updating Warehouse entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).warehouses().update(mWarehouse);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            Log.d(TAG, "Done. Returned with ID " + id + ": " + Thread.currentThread());
            goBack();
        }, err -> {
            Log.e(TAG, "Database Error: " + err);

            // show dialog
            dialogBuilder.setTitle("Database Error").setMessage(err.toString());
            AlertDialog dialog = dialogBuilder.create();
            dialog.show();
        }));
    }

    private void deleteWarehouse() {
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Deleting Warehouse entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).warehouses().delete(mWarehouse);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            Log.d(TAG, "Done. Row affected=" + rowCount + " " + Thread.currentThread());

            if (rowCount > 0) {
                Toast.makeText(this, "Warehouse Entry Deleted", Toast.LENGTH_SHORT).show();
            }

            // return to WarehousesActivity
            startActivity(new Intent(this, WarehousesActivity.class));
            finish();
        }, err -> {
            Log.e(TAG, "Database Error: " + err);

            // dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while deleting Warehouse entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}
