package com.example.itbipoa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.itbipoa.data.cache.CsvCache
import com.example.itbipoa.data.model.ItbiRecord
import com.example.itbipoa.data.repository.IndiceRepository
import com.example.itbipoa.data.repository.ItbiRepository
import com.example.itbipoa.ui.detail.DetailScreen
import com.example.itbipoa.ui.detail.DetailViewModel
import com.example.itbipoa.ui.search.SearchScreen
import com.example.itbipoa.ui.search.SearchViewModel
import com.example.itbipoa.ui.theme.ItbiPoaTheme

/** Estados de navegação simples (sem depender da lib navigation-compose). */
private sealed class Tela {
    data object Busca : Tela()
    data class Detalhe(val registro: ItbiRecord) : Tela()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val csvCache = CsvCache(applicationContext)
        val itbiRepository = ItbiRepository(csvCache)
        val indiceRepository = IndiceRepository()

        setContent {
            ItbiPoaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(itbiRepository, indiceRepository)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    itbiRepository: ItbiRepository,
    indiceRepository: IndiceRepository
) {
    var tela by remember { mutableStateOf<Tela>(Tela.Busca) }

    // Sem isso, o botão/gesto de voltar do sistema fecha o app direto
    // (não sabe que existe uma tela anterior, já que a navegação aqui é
    // feita "na mão"). Intercepta o voltar do sistema só quando estamos no
    // detalhe, e volta pra busca em vez de fechar o app.
    BackHandler(enabled = tela is Tela.Detalhe) {
        tela = Tela.Busca
    }

    val searchViewModel: SearchViewModel = viewModel(
        factory = viewModelFactory { SearchViewModel(itbiRepository) }
    )

    when (val telaAtual = tela) {
        is Tela.Busca -> {
            SearchScreen(
                viewModel = searchViewModel,
                onAbrirDetalhe = { registro -> tela = Tela.Detalhe(registro) }
            )
        }
        is Tela.Detalhe -> {
            val detailViewModel: DetailViewModel = viewModel(
                key = telaAtual.registro.hashCode().toString(),
                factory = viewModelFactory { DetailViewModel(indiceRepository) }
            )
            DetailScreen(
                registro = telaAtual.registro,
                viewModel = detailViewModel,
                onVoltar = { tela = Tela.Busca }
            )
        }
    }
}

/** Helper pequeno para criar uma ViewModelProvider.Factory a partir de uma lambda. */
private inline fun <VM : ViewModel> viewModelFactory(crossinline criar: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = criar() as T
    }
