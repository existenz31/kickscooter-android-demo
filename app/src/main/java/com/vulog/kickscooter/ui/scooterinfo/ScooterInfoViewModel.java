package com.vulog.kickscooter.ui.scooterinfo;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vulog.kickscooter.model.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScooterInfoViewModel extends ViewModel {
    private static String TAG = ScooterInfoViewModel.class.getName();

    MutableLiveData<Map<String, Vehicle>> vehicles = new MutableLiveData<>();

    public LiveData<Map<String, Vehicle>> getVehicles() {
        if (vehicles.getValue() == null) {
            FirebaseDatabase.getInstance()
                    .getReference("vehicles")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Map<String, Vehicle> vehicleMap = new HashMap();
                                for (DataSnapshot child : dataSnapshot.getChildren()) {
                                    Vehicle vehicle = child.getValue(Vehicle.class);
                                    vehicleMap.put(vehicle.getId(), vehicle);
                                    Log.d(TAG, "Vehicle id: " + vehicle.getId());
                                }
                                vehicles.postValue(vehicleMap);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                return vehicles;

            }
}
