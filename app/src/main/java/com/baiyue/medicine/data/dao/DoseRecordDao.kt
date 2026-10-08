package com.baiyue.medicine.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.baiyue.medicine.data.entity.DoseRecord
import com.baiyue.medicine.model.RecordCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DoseRecord): Long

    @Query("SELECT * FROM dose_records ORDER BY date DESC, time DESC")
    fun observeAll(): Flow<List<DoseRecord>>

    @Query("SELECT * FROM dose_records WHERE date = :date ORDER BY time DESC")
    fun observeByDate(date: String): Flow<List<DoseRecord>>

    /** 按计划日期查询（补签跨日会记到实际当天，这里用于按「计划时段所属日期」去重） */
    @Query("SELECT * FROM dose_records WHERE plannedDate = :plannedDate ORDER BY time DESC")
    fun observeByPlannedDate(plannedDate: String): Flow<List<DoseRecord>>

    @Query("SELECT * FROM dose_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DoseRecord?

    @Query("SELECT * FROM dose_records WHERE medicationId = :medicationId AND date = :date AND time = :time AND category = :category LIMIT 1")
    suspend fun find(
        medicationId: Long,
        date: String,
        time: String,
        category: RecordCategory
    ): DoseRecord?

    @Delete
    suspend fun delete(record: DoseRecord)
}