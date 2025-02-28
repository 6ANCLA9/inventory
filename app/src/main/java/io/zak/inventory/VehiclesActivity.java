package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.VehicleListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.data.entities.Vehicle;

public class VehiclesActivity extends AppCompatActivity implements VehicleListAdapter.OnItemClickListener {

    private static final String TAG = "Vehicles";

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoVehicles;
    private Button btnBack, btnAdd;
    private RelativeLayout progressGroup;

    // for RecyclerView
    private List<Vehicle> vehicleList;
    private VehicleListAdapter adapter;
    private final Comparator<Vehicle> comparator = Comparator.comparing(vehicle -> vehicle.vehicleName);

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicles);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoVehicles = findViewById(R.id.tv_no_vehicles);
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VehicleListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);
    }

    private void setListeners() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onSearch(newText);
                return false;
            }
        });

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
            finish();
        });

        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, AddVehicleActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Vehicle entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).vehicles().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Fetched " + list.size() + " items: " + Thread.currentThread());
            progressGroup.setVisibility(View.GONE);
            vehicleList = list;
            adapter.replaceAll(list);
            tvNoVehicles.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Vehicle vehicle = adapter.getItem(position);
            Log.d(TAG, "Vehicle selected (ID: " + vehicle.vehicleId + ")");

            Intent intent = new Intent(this, ViewVehicleActivity.class);
            intent.putExtra("vehicleId", vehicle.vehicleId);
            startActivity(intent);
        }
    }

    private void onSearch(String query) {
        List<Vehicle> filteredList = filterList(vehicleList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Vehicle> filterList(List<Vehicle> ref, String query) {
        String str = query.toLowerCase();
        final List<Vehicle> list = new ArrayList<>();
        for (Vehicle vehicle : ref) {
            if (vehicle.vehicleName.toLowerCase().contains(str)) {
                list.add(vehicle);
            }
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
