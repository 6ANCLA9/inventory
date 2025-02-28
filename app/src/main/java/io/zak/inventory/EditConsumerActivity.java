package io.zak.inventory;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Consumer;

public class EditConsumerActivity extends AppCompatActivity {

    private static final String TAG = "EditConsumer";

    // Widgets
    private EditText etName, etContact, etEmail, etAddress;
    private ImageButton btnBack, btnDelete;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;


    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Consumer mConsumer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_consumer);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {

        TextView tvTitle = findViewById(R.id.title);
        tvTitle.setText(R.string.edit_consumer);
        etName = findViewById(R.id.et_name);
        etContact = findViewById(R.id.et_contact);
        etEmail = findViewById(R.id.et_email);
        etAddress = findViewById(R.id.et_address);
        btnBack = findViewById(R.id.btn_back);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setVisibility(View.VISIBLE);
        progressGroup = findViewById(R.id.progress_group);


        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });
        btnDelete.setOnClickListener(v -> {
            if (mConsumer != null) {
                dialogBuilder.setTitle("Confirm Delete")
                        .setMessage("This will delete all data related to this Consumer entry. " +
                                "Are you sure you want to delete this entry?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            dialog.dismiss();
                            deleteConsumer();
                        });
                dialogBuilder.create().show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // check consumer id
        int id = getIntent().getIntExtra("consumer_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Consumer ID: " + id)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            return AppDatabaseImpl.getDatabase(getApplicationContext()).consumers().getConsumer(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(consumers -> {
            progressGroup.setVisibility(View.GONE);
            mConsumer = consumers.get(0);
            displayInfo(mConsumer);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Consumer entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void displayInfo(Consumer consumer) {
        if (consumer != null) {
            etName.setText(consumer.consumerName);
            etAddress.setText(consumer.consumerAddress);
            etContact.setText(consumer.consumerContactNo);
            etEmail.setText(consumer.consumerEmail);
        }
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
        mConsumer.consumerName = Utils.normalize(etName.getText().toString());
        mConsumer.consumerContactNo = Utils.normalize(etContact.getText().toString());
        mConsumer.consumerEmail = Utils.normalize(etEmail.getText().toString());
        mConsumer.consumerAddress = Utils.normalize(etAddress.getText().toString());

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            return AppDatabaseImpl.getDatabase(getApplicationContext()).consumers().update(mConsumer);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressGroup.setVisibility(View.GONE);
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while updating Consumer entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void deleteConsumer() {
        if (mConsumer != null) {
            progressGroup.setVisibility(View.VISIBLE);
            disposables.add(Single.fromCallable(() -> {
                return AppDatabaseImpl.getDatabase(getApplicationContext()).consumers().delete(mConsumer);
            }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
                progressGroup.setVisibility(View.GONE);
                if (rowCount > 0) {
                    Toast.makeText(this, "Deleted Consumer entry.", Toast.LENGTH_SHORT).show();
                }
                startActivity(new Intent(this, ConsumersActivity.class));
                finish();
            }, err -> {
                progressGroup.setVisibility(View.GONE);
                dialogBuilder.setTitle("Database Error")
                        .setMessage("Error while deleting Consumer entry: " + err)
                        .setPositiveButton("OK", (dialog, which) -> {
                            dialog.dismiss();
                            goBack();
                        });
                dialogBuilder.create().show();
            }));
        }
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}
