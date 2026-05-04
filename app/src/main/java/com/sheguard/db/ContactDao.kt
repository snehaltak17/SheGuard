package com.sheguard.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact)

    @Update
    suspend fun update(contact: Contact)

    @Query("SELECT * FROM contacts WHERE userId = :userId ORDER BY priorityLevel ASC, name ASC")
    fun getContacts(userId: String): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :contactId LIMIT 1")
    suspend fun getContactById(contactId: Int): Contact?

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContact(contactId: Int)
}
