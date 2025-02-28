package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Supplier;

@Dao
public interface SupplierDao {

    @Insert
    long insert(Supplier supplier);

    @Update
    int update(Supplier supplier);

    @Update
    void updateAll(Supplier...suppliers);

    @Delete
    int delete(Supplier supplier);

    @Query("SELECT * FROM suppliers")
    List<Supplier> getAll();

    @Query("SELECT * FROM suppliers WHERE supplierId=:id")
    List<Supplier> getSupplier(int id);

    @Query("SELECT COUNT(*) FROM suppliers")
    int getSize();
}
