package ru.kode.amvi.component.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.kode.amvi.viewmodel.ViewIntents
import ru.kode.amvi.viewmodel.ViewModel

@Composable
fun <S : Any, I : ViewIntents> MviComponent(
  viewModel: ViewModel<S, I>,
  intents: I,
  content: @Composable (state: S, intents: I) -> Unit,
) {
  LifecycleEffect(viewModel, intents)
  val state by viewModel.viewStateFlow.collectAsState()
  content(state, intents)
}

@Composable
inline fun <reified VI : ViewIntents> rememberViewIntents(): VI {
  return remember { VI::class.java.getDeclaredConstructor().newInstance() }
}

@Composable
private fun <VI : ViewIntents, VM : ViewModel<*, VI>> LifecycleEffect(viewModel: VM, intents: VI) {
  var attachedViewModel by remember { mutableStateOf<ViewModel<*, *>?>(null) }
  DisposableEffect(viewModel) {
    attachedViewModel?.detach()
    viewModel.attach(intents)
    attachedViewModel = viewModel

    onDispose {
      attachedViewModel?.detach()
    }
  }
}
