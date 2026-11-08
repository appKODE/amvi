package ru.kode.amvi.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
open class ViewIntents {
  private val intentRelay = MutableSharedFlow<UiIntent>(extraBufferCapacity = 12)

  internal val stream: Flow<UiIntent> = intentRelay

  fun intent(name: String = "name-${Clock.System.now().toEpochMilliseconds()}"): UiIntentFactory0 =
    UiIntentFactory0(name, intentRelay::tryEmit)

  fun <T : Any> intent(name: String = "name-${Clock.System.now().toEpochMilliseconds()}"): UiIntentFactory1<T> =
    UiIntentFactory1(name, intentRelay::tryEmit)
}
