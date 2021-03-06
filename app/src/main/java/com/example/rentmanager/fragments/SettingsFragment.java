package com.example.rentmanager.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.rentmanager.R;
import com.example.rentmanager.Utils.Utility;
import com.example.rentmanager.activities.LoginActivity;
import com.example.rentmanager.database.DatabaseClient;
import com.example.rentmanager.database.Firebase.FirebaseDatabase;
import com.example.rentmanager.databinding.FragmentSettingsBinding;
import com.example.rentmanager.models.User;

import java.io.FileNotFoundException;
import java.io.InputStream;

import static android.app.Activity.RESULT_OK;


public class SettingsFragment extends Fragment implements LifecycleOwner {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SettingsFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    FragmentSettingsBinding binding;
    SharedPreferences sharedPreferences;
    User loggedInUser;
    public static String TAG = "SettingsFragment";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        sharedPreferences = getContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        binding.logoutButton.setOnClickListener(view -> {
            FirebaseDatabase.getInstance().logoutUser();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        });
        binding.changeImage.setOnClickListener(updateUserImage());
        binding.profileImage.setOnClickListener(updateUserImage());
        binding.updateButton.setOnClickListener(updateUserSettings());
        FirebaseDatabase.getInstance().downloadProfilePhotoFromStorage(binding.profileImage,getContext());
        GetUserFromRoomDatabase getUserFromRoomDatabase = new GetUserFromRoomDatabase();
        getUserFromRoomDatabase.execute(sharedPreferences.getLong("userId", 0));
        return binding.getRoot();
    }


    class GetUserFromRoomDatabase extends AsyncTask<Long, Void, Void> {
        @Override
        protected Void doInBackground(Long... longs) {
            loggedInUser = DatabaseClient.getInstance(getContext())
                    .getRentManagerDatabase()
                    .userDao()
                    .getUserById(longs[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            binding.userEmail.setText(loggedInUser.getEmailAddress());
            binding.userUsername.setText(loggedInUser.getUserName());
            binding.phoneNumber.setText(loggedInUser.getTelephoneNumber());
        }
    }

    class UpdateUserFromRoomDatabase extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            DatabaseClient.getInstance(getContext())
                    .getRentManagerDatabase()
                    .userDao()
                    .updateUser(loggedInUser);
            return null;
        }
    }

    private View.OnClickListener updateUserImage() {
        return view -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, 1);
        };
    }

    private void handleFirebaseUpdate(Bitmap photo) {
        FirebaseDatabase.getInstance().updateProfilePhotoIntoStorogage(photo)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(getContext(),getString(R.string.upload_successful),Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);


        if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContext().getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                binding.profileImage.setImageBitmap(selectedImage);
                handleFirebaseUpdate(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    private View.OnClickListener updateUserSettings() {
        return view -> {
            String email = binding.userEmail.getText().toString();
            String password = binding.currentPassword.getText().toString();
            String newPassword = binding.newPassword.getText().toString();
            String phoneNumber = binding.phoneNumber.getText().toString();
            String username = binding.userUsername.getText().toString();
            if(!email.equals(loggedInUser.getEmailAddress())) {
                FirebaseDatabase.getInstance().changeUserEmail(email,password)
                        .addOnCompleteListener(task -> {
                            if(task.isSuccessful()) {
                                loggedInUser.setEmailAddress(email);
                                updateDatabaseFields(phoneNumber, username);
                            } else {
                                binding.userEmail.setError(task.getException().getMessage());
                                binding.userEmail.requestFocus();
                            }
                        });
            } else {
                updateDatabaseFields(phoneNumber, username);
            }
            if(Utility.hashPassword(password).equals(loggedInUser.getUserPassword())) {
                if(!newPassword.isEmpty()) {
                    FirebaseDatabase.getInstance().changeUserPassword(newPassword,email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    loggedInUser.setUserPassword(Utility.hashPassword(newPassword));
                                    updateDatabaseFields(phoneNumber, username);
                                } else {
                                    binding.currentPassword.setError(task.getException().getMessage());
                                    binding.currentPassword.requestFocus();
                                }
                            });
                } else {
                    binding.newPassword.setError(getString(R.string.empty_password_error));
                    binding.newPassword.requestFocus();
                    return;
                }
            } else if (!password.isEmpty()){
                binding.currentPassword.setError(getString(R.string.bad_credentials_error));
                binding.currentPassword.requestFocus();
                return;
            }
        };
    }

    private void updateDatabaseFields(String phoneNumber, String username) {
        UpdateUserFromRoomDatabase updateUserFromRoomDatabase = new UpdateUserFromRoomDatabase();
        loggedInUser.setTelephoneNumber(phoneNumber);
        loggedInUser.setUserName(username);
        updateUserFromRoomDatabase.execute();
        Toast.makeText(getContext(), getString(R.string.settings_updated), Toast.LENGTH_LONG).show();
    }
}