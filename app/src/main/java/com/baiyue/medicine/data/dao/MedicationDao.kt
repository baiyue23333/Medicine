package com.baiyue.medicine.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.baiyue.medicine.data.entity.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY isOccasional ASC, id DESC")
    fun observeAll(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Query("SELECT * FROM medications WHERE isOccasional = 1 AND enabled = 1 ORDER BY id DESC")
    fun observeOccasional(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE isScheduled = 1 AND enabled = 1 ORDER BY id DESC")
    fun observeRegular(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(medication: Medication): Long

    /** 扣减库存；未设置总量（负值）时保持不变，不足时归零 */
    @Query("UPDATE medications SET quantity = CASE WHEN quantity < 0 THEN quantity WHEN quantity >= :amount THEN quantity - :amount ELSE 0 END WHERE id = :id")
    suspend fun decrementQuantity(id: Long, amount: Double)

    /** 恢复库存（撤销打卡时）；未设置总量（负值）时保持不变 */
    @Query("UPDATE medications SET quantity = CASE WHEN quantity < 0 THEN quantity ELSE quantity + :amount END WHERE id = :id")
    suspend fun restoreQuantity(id: Long, amount: Double)

    /** 增减库存：未设置总量时，正向增减从 0 开始、负向保持不变；已设置时下限归零 */
    @Query("UPDATE medications SET quantity = CASE WHEN quantity < 0 THEN (CASE WHEN :delta > 0 THEN :delta ELSE quantity END) WHEN quantity + :delta < 0 THEN 0 ELSE quantity + :delta END WHERE id = :id")
    suspend fun adjustQuantity(id: Long, delta: Double)

    @Delete
    suspend fun delete(medication: Medication)
}