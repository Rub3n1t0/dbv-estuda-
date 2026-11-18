package com.dbv.estuda

import android.app.Application
import com.dbv.estuda.data.AppDatabase
import com.dbv.estuda.data.Repositorio
import com.dbv.estuda.data.Sessao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Dependências compartilhadas pelo app inteiro. */
class AppContainer(app: Application) {
    val db = AppDatabase.obter(app)
    val repo = Repositorio(db)
    val sessao = Sessao(app)
}

class DbvApp : Application() {
    lateinit var container: AppContainer
        private set

    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        escopo.launch { container.repo.semearSeNecessario(this@DbvApp) }
    }
}
