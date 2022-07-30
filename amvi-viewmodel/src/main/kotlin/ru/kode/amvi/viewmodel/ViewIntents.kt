package ru.kode.amvi.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

open class ViewIntents {
  private val intentRelay = MutableSharedFlow<UiIntent>(extraBufferCapacity = 12)

  internal val stream: Flow<UiIntent> = intentRelay

  fun intent(name: String = "name-${System.currentTimeMillis()}"): UiIntentFactory0 {
    return UiIntentFactory0(name, intentRelay::tryEmit)
  }

  fun <T : Any> intent(name: String = "name-${System.currentTimeMillis()}"): UiIntentFactory1<T> {
    return UiIntentFactory1(name, intentRelay::tryEmit)
  }
}
