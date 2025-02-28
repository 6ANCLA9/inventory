package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Vehicle;

@Dao
public interface VehicleDao {

    @Insert
    long insert(Vehicle vehicle);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Vehicle...vehicles);

    @Update
    int update(Vehicle vehicle);
    @Update
    void updateAll(Vehicle...vehicles);

    @Delete
    int delete(Vehicle vehicle);

    @Query("SELECT * FROM vehicles")
    List<Vehicle> getAll();

    @Query("SELECT * FROM vehicles WHERE vehicleId=:id")
    List<Vehicle> getVehicle(int id);

    @Query("SELECT COUNT(*) FROM vehicles")
    int getSize();
}
