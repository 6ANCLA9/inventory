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
import io.zak.inventory.data.entities.Employee;

public class EditEmployeeActivity extends AppCompatActivity {

    private static final String TAG = "AddEmployee";

    private EditText etName, etPosition, etContact, etAddress, etLicense;
    private Spinner statusSpinner;
    private ImageButton btnBack, btnDelete;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Employee mEmployee;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        // Widgets
        TextView tvTitle = findViewById(R.id.title);
        tvTitle.setText(R.string.edit_employee);

        etName = findViewById(R.id.et_name);
        etPosition = findViewById(R.id.et_position);
        etContact = findViewById(R.id.et_contact);
        etAddress = findViewById(R.id.et_address);
        etLicense = findViewById(R.id.et_license);
        statusSpinner = findViewById(R.id.status_spinner);
        btnBack = findViewById(R.id.btn_back);

        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setVisibility(View.VISIBLE); // make it visible

        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnDelete.setOnClickListener(v -> {
            if (mEmployee != null) {
                dialogBuilder.setTitle("Confirm Delete")
                        .setMessage("This will delete all data related to this Employee entry. " +
                                "Are you sure you want to delete this Employee entry?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            dialog.dismiss();
                            deleteEmployee();
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

        int id = getIntent().getIntExtra("employee_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Employee id.")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Employee entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).employees().getEmployee(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(employees -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + employees.size() + " " + Thread.currentThread());
            mEmployee = employees.get(0);
            displayInfo(mEmployee);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Employee entry: " +err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void displayInfo(Employee employee) {
        if (employee != null) {
            etName.setText(employee.employeeName);
            etAddress.setText(employee.employeeAddress);
            etContact.setText(employee.employeeContactNo);
            etPosition.setText(employee.position);
            etLicense.setText(employee.licenseNo);

            List<String> statusList = Arrays.asList(getResources().getStringArray(R.array.status_array));
            int pos = statusList.indexOf(employee.employeeStatus);
            statusSpinner.setSelection(pos);
        }
    }

    private boolean validated() {
        boolean isValid = true;

        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError("Required");
            isValid = false;
        }
        if (etPosition.getText().toString().trim().isEmpty()) {
            etPosition.setError("Required");
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
        mEmployee.employeeName = Utils.normalize(etName.getText().toString());
        mEmployee.position = Utils.normalize(etPosition.getText().toString());
        mEmployee.employeeContactNo = Utils.normalize(etContact.getText().toString());
        mEmployee.employeeAddress = Utils.normalize(etAddress.getText().toString());
        mEmployee.employeeStatus = statusSpinner.getSelectedItem().toString();
        mEmployee.licenseNo = Utils.normalize(etLicense.getText().toString());

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Updating Employee entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).employees().update(mEmployee);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            Log.d(TAG, "Row affected=" + rowCount + ": " + Thread.currentThread());
            if (rowCount > 0) {
                Toast.makeText(this, "Updated Employee entry.", Toast.LENGTH_SHORT).show();
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

    private void deleteEmployee() {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Deleting Employee entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).employees().delete(mEmployee);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with row count=" + rowCount + " " + Thread.currentThread());
            if (rowCount > 0) {
                Toast.makeText(this, "Deleted Employee entry.", Toast.LENGTH_SHORT).show();
            }
            goToEmployeeList();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while deleting Employee entry: " + err)
                    .setPositiveButton("OK", (d, w) -> {
                        d.dismiss();
                        goToEmployeeList();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    private void goToEmployeeList() {
        startActivity(new Intent(this, EmployeesActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
