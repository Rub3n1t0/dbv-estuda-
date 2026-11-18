package com.dbv.estuda.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Quem está logado. Guardado em SharedPreferences para sobreviver ao fechamento do app. */
data class UsuarioLogado(val id: Long, val nome: String, val papel: String, val clube: String)

class Sessao(context: Context) {
    private val prefs = context.getSharedPreferences("sessao", Context.MODE_PRIVATE)
    private val _atual = MutableStateFlow(ler())
    val atual: StateFlow<UsuarioLogado?> = _atual

    private fun ler(): UsuarioLogado? {
        val id = prefs.getLong("id", -1)
        if (id < 0) return null
        return UsuarioLogado(
            id = id,
            nome = prefs.getString("nome", "") ?: "",
            papel = prefs.getString("papel", Papel.DESBRAVADOR) ?: Papel.DESBRAVADOR,
            clube = prefs.getString("clube", "") ?: "",
        )
    }

    fun entrar(u: Usuario) {
        prefs.edit()
            .putLong("id", u.id).putString("nome", u.nome)
            .putString("papel", u.papel).putString("clube", u.clube)
            .apply()
        _atual.value = UsuarioLogado(u.id, u.nome, u.papel, u.clube)
    }

    fun sair() {
        prefs.edit().clear().apply()
        _atual.value = null
    }
}
