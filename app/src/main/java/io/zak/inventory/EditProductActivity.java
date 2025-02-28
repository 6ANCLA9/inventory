package io.zak.inventory;

import android.content.Intent;
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
import android.widget.Toast;

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

public class EditProductActivity extends AppCompatActivity {

    private static final String TAG = "EditProduct";

    private EditText etName, etPrice, etDescription, etCritLevel;
    private Spinner supplierSpinner, brandSpinner, categorySpinner;
    private TextView emptySupplierSpinner, emptyBrandSpinner, emptyCategorySpinner;

    private Supplier mSelectedSupplier;
    private Brand mSelectedBrand;
    private Category mSelectedCategory;

    private ImageButton btnBack, btnMinus, btnPlus, btnDelete;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private List<Supplier> supplierList;
    private List<Brand> brandList;
    private List<Category> categoryList;

    private Product mProduct;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        // Widgets
        TextView tvTitle = findViewById(R.id.title);
        tvTitle.setText(R.string.edit_product);

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

        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setVisibility(View.VISIBLE); // make it visible

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
        btnDelete.setOnClickListener(v -> {
            if (mProduct != null) {
                dialogBuilder.setTitle("Confirm Delete")
                        .setMessage("This will delete all data related to this Product entry. " +
                                "Are you sure you want to delete this Product entry?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            dialog.dismiss();
                            deleteProduct();
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

        int id = getIntent().getIntExtra("product_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Product id.")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Product entry: " + Thread.currentThread());
            return database.products().getProduct(id);
        }).flatMap(products -> {
            mProduct = products.get(0);
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching Supplier entries: " + Thread.currentThread());
                return database.suppliers().getAll();
            });
        }).flatMap(suppliers -> {
            supplierList = suppliers;
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching Brand entries: " + Thread.currentThread());
                return database.brands().getAll();
            });
        }).flatMap(brands -> {
            brandList = brands;
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching Category entries: " + Thread.currentThread());
                return database.categories().getAll();
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(categories -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with category size: " + categories.size() + " " + Thread.currentThread());
            categoryList = categories;
            setupSpinners();
            displayInfo(mProduct);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Product entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void setupSpinners() {
        if (supplierList == null || brandList == null || categoryList == null) {
            dialogBuilder.setTitle("Error")
                    .setMessage("Unable to display Suppliers, Brands and Categories")
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

    private void displayInfo(Product product) {
        if (product != null) {
            etName.setText(product.productName);
            etPrice.setText(String.valueOf((int) product.price));
            etCritLevel.setText(String.valueOf(product.criticalLevel));
            etDescription.setText(product.productDescription);

            // Set selected supplier
            for (int i = 0; i < supplierList.size(); i++) {
                if (supplierList.get(i).supplierId == product.fkSupplierId) {
                    supplierSpinner.setSelection(i);
                    mSelectedSupplier = supplierList.get(i);
                    break;
                }
            }

            // Set selected brand
            for (int i = 0; i < brandList.size(); i++) {
                if (brandList.get(i).brandId == product.fkBrandId) {
                    brandSpinner.setSelection(i);
                    mSelectedBrand = brandList.get(i);
                    break;
                }
            }

            // Set selected category
            for (int i = 0; i < categoryList.size(); i++) {
                if (categoryList.get(i).categoryId == product.fkCategoryId) {
                    categorySpinner.setSelection(i);
                    mSelectedCategory = categoryList.get(i);
                    break;
                }
            }
        }
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
        if (mSelectedSupplier == null || mSelectedBrand == null || mSelectedCategory == null) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("No selected Supplier, Brand and/or Category")
                    .setPositiveButton("OK", ((dialog, which) -> dialog.dismiss()));
            dialogBuilder.create().show();
            return;
        }

        mProduct.productName = Utils.normalize(etName.getText().toString());
        mProduct.fkSupplierId = mSelectedSupplier.supplierId;
        mProduct.fkBrandId = mSelectedBrand.brandId;
        mProduct.fkCategoryId = mSelectedCategory.categoryId;
        mProduct.price = Double.parseDouble(etPrice.getText().toString().trim());
        String str = etCritLevel.getText().toString().trim();
        int level = str.isBlank() ? 1 : Integer.parseInt(str);
        if (level < 1) level = 1;
        mProduct.criticalLevel = level;
        mProduct.productDescription = Utils.normalize(etDescription.getText().toString());

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Updating Product entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).products().update(mProduct);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Row affected=" + rowCount + ": " + Thread.currentThread());
            if (rowCount > 0) {
                Toast.makeText(this, "Updated Product entry.", Toast.LENGTH_SHORT).show();
            }
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            // show dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while updating Product entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void deleteProduct() {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Deleting Product entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).products().delete(mProduct);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with row count=" + rowCount + " " + Thread.currentThread());
            if (rowCount > 0) {
                Toast.makeText(this, "Deleted Product entry.", Toast.LENGTH_SHORT).show();
            }
            goToProductList();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while deleting Product entry: " + err)
                    .setPositiveButton("OK", (d, w) -> {
                        d.dismiss();
                        goToProductList();
                    });
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
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    private void goToProductList() {
        startActivity(new Intent(this, ProductsActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}