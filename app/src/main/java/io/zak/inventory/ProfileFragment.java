package io.zak.inventory;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.User;

public class ProfileFragment extends Fragment {

    private static final String TAG = "Profile";

    // Widgets
    private TextView tvUsername, tvPosition, tvContact, tvAddress;
    private ImageView imageView;
    private ImageButton btnEdit;
    private Button btnLogout;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        getWidgets(view);
        setListeners();
        return view;
    }

    private void getWidgets(View view) {
        tvUsername = view.findViewById(R.id.tv_username);
        tvPosition = view.findViewById(R.id.tv_position);
        tvContact = view.findViewById(R.id.tv_contact);
        tvAddress = view.findViewById(R.id.tv_address);
        btnEdit = view.findViewById(R.id.btn_edit);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setListeners() {
        btnEdit.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), EditProfileActivity.class));
        });
        btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
        int id = Utils.getLoginId(requireActivity());
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Retrieving User entry for ID " + id + " " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(requireActivity().getApplicationContext()).users().getUser(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(users -> {
            Log.d(TAG, "Returned with list size=" + users.size() + " " + Thread.currentThread());
            displayInfo(users.get(0));
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            logout();
        }));
    }

    private void displayInfo(User user) {
        if (user != null) {
            tvUsername.setText(user.fullName == null ? user.username : user.fullName);
            if (user.position != null) tvPosition.setText(user.position);
            if (user.contactNo != null) tvContact.setText(user.contactNo);
            if (user.address != null) tvAddress.setText(user.address);
        }
    }

    private void logout() {
        Utils.logout(requireActivity());
        startActivity(new Intent(getActivity(), LoginActivity.class));
    }
}
