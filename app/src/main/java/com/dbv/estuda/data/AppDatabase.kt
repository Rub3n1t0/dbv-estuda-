package com.dbv.estuda.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Usuario::class, Especialidade::class, Pergunta::class,
        Prova::class, ProvaPergunta::class, Tentativa::class, Resposta::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarios(): UsuarioDao
    abstract fun conteudo(): ConteudoDao
    abstract fun provas(): ProvaDao
    abstract fun desempenho(): DesempenhoDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        fun obter(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "dbv_estuda.db")
                    .build()
                    .also { instancia = it }
            }
    }
}
