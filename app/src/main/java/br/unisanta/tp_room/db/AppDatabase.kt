package br.unisanta.tp_room.db

import androidx.room.Database
import androidx.room.RoomDatabase
import br.unisanta.tp_room.dao.ProductDao
import br.unisanta.tp_room.model.Product

@Database(entities = [Product::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}