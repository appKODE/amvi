package ru.kode.amvi.component.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import ru.kode.amvi.viewmodel.ViewIntents
import ru.kode.amvi.viewmodel.ViewModel

@Composable
fun <S : Any, I : ViewIntents> MviComponent(
  presenter: ViewModel<S, I>,
  intents: I,
  content: @Composable (state: S, intents: I) -> Unit,
) {
  LifecycleEffect(presenter, intents)
  val state by presenter.viewStateFlow.collectAsState()
  content(state, intents)
}

@Composable
inline fun <reified VI : ViewIntents> rememberViewIntents(): VI {
  return remember { VI::class.java.getDeclaredConstructor().newInstance() }
}

@Composable
private fun <VI : ViewIntents, VM : ViewModel<*, VI>> LifecycleEffect(viewModel: VM, intents: VI) {
  LaunchedEffect(Unit) {
    viewModel.attach(intents)
  }
  DisposableEffect(Unit) {
    onDispose {
      viewModel.detach()
    }
  }
}
