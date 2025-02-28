package io.zak.inventory;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.BrandSpinnerAdapter;
import io.zak.inventory.adapters.CategorySpinnerAdapter;
import io.zak.inventory.adapters.SupplierSpinnerAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Brand;
import io.zak.inventory.data.entities.Category;
import io.zak.inventory.data.entities.Product;
import io.zak.inventory.data.entities.Supplier;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProduct";

    // Widgets
    private EditText etName, etPrice, etDescription, etCritLevel;
    private Spinner supplierSpinner, brandSpinner, categorySpinner;
    private TextView emptySupplierSpinner, emptyBrandSpinner, emptyCategorySpinner;
    private ImageButton btnBack, btnMinus, btnPlus;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;


    // list references to Brands and Categories
    private List<Supplier> supplierList;
    private List<Brand> brandList;
    private List<Category> categoryList;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Supplier mSelectedSupplier;
    private Brand mSelectedBrand;
    private Category mSelectedCategory;

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        etName = findViewById(R.id.et_name);
        etPrice = findViewById(R.id.et_price);
        etDescription = findViewById(R.id.et_description);
        etCritLevel = findViewById(R.id.et_crit_level);
        supplierSpinner = findViewById(R.id.supplier_spinner);
        emptySupplierSpinner = findViewById(R.id.empty_supplier_spinner);
        brandSpinner = findViewById(R.id.brand_spinner);
        emptyBrandSpinner = findViewById(R.id.empty_brand_spinner);
        categorySpinner = findViewById(R.id.category_spinner);
        emptyCategorySpinner = findViewById(R.id.empty_category_spinner);
        btnBack = findViewById(R.id.btn_back);
        btnMinus = findViewById(R.id.btn_minus);
        btnPlus = findViewById(R.id.btn_plus);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);
        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        supplierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (supplierList != null && !supplierList.isEmpty()) {
                    mSelectedSupplier = supplierList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });

        brandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (brandList != null && !brandList.isEmpty()) {
                    mSelectedBrand = brandList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (categoryList != null && !categoryList.isEmpty()) {
                    mSelectedCategory = categoryList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });

        btnBack.setOnClickListener(v -> goBack());
        btnMinus.setOnClickListener(v -> decrementQuantity());
        btnPlus.setOnClickListener(v -> incrementQuantity());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Supplier entries: " + Thread.currentThread());
            return database.suppliers().getAll();
        }).flatMap(suppliers -> {
            supplierList = suppliers;
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching Brand entries: " + Thread.currentThread());
                return  database.brands().getAll();
            });
        }).flatMap(brands -> {
            brandList = brands;
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching Category entries: " + Thread.currentThread());
                return  database.categories().getAll();
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(categories -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with category size: " +categories.size() + " " + Thread.currentThread());
            categoryList = categories;
            setupSpinners();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            // dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Brand and Category entries: " + err)
                    .setPositiveButton("OK", ((dialog, which) -> dialog.dismiss()));
            dialogBuilder.create().show();
        }));
    }

    private void setupSpinners() {
        if (supplierList == null || brandList == null || categoryList == null) {
            dialogBuilder.setTitle("Error")
                    .setMessage("Unable to display Brands and Categories")
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
            return;
        }

        supplierSpinner.setAdapter(new SupplierSpinnerAdapter(this, supplierList));
        brandSpinner.setAdapter(new BrandSpinnerAdapter(this, brandList));
        categorySpinner.setAdapter(new CategorySpinnerAdapter(this, categoryList));

        emptySupplierSpinner.setVisibility(supplierList.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        emptyBrandSpinner.setVisibility(brandList.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        emptyCategorySpinner.setVisibility(categoryList.isEmpty() ? View.VISIBLE : View.INVISIBLE);
    }

    private boolean validated() {
        boolean isValid = true;

        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError("Required");
            isValid = false;
        }
        if (etPrice.getText().toString().trim().isEmpty()) {
            etPrice.setError("Required");
            isValid = false;
        }
        return isValid;
    }

    private void saveAndClose() {
        if (mSelectedBrand == null && mSelectedCategory == null) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("No selected Brand and/or Category")
                    .setPositiveButton("OK", ((dialog, which) -> dialog.dismiss()));
            dialogBuilder.create().show();
            return;
        }

        Product product = new Product();
        product.productName = Utils.normalize(etName.getText().toString());
        product.fkSupplierId = mSelectedSupplier.supplierId;
        product.fkBrandId = mSelectedBrand.brandId;
        product.fkCategoryId = mSelectedCategory.categoryId;
        product.price = Double.parseDouble(etPrice.getText().toString().trim());
        String str = etCritLevel.getText().toString().trim();
        int level = str.isBlank() ? 1 : Integer.parseInt(str);
        if (level < 1) level = 1;
        product.criticalLevel = level;
        product.productDescription = Utils.normalize(etDescription.getText().toString());

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Product entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).products().insert(product);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            Log.d(TAG, "Returned with ID:" + id + " " + Thread.currentThread());
            progressGroup.setVisibility(View.GONE);
            goBack();
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            progressGroup.setVisibility(View.GONE);

            // dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while saving Product entry: " + err)
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
        }));
    }

    private void decrementQuantity() {
        String str = etCritLevel.getText().toString();
        int qty = str.isBlank() ? 1 : Integer.parseInt(str.trim());
        qty -= 1;
        if (qty < 1) qty = 1;
        etCritLevel.setText(String.valueOf(qty));
    }

    private void incrementQuantity() {
        String str = etCritLevel.getText().toString();
        int qty = str.isBlank() ? 1 : Integer.parseInt(str.trim());
        qty += 1;
        etCritLevel.setText(String.valueOf(qty));
    }

    private void goBack() {
        getOnBackPressedDispatcher();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
