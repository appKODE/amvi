package ru.kode.amvi.viewmodel

import app.cash.turbine.test
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine

class ViewModelLifecycleTest :
  ShouldSpec({
    should("call lifecycle hooks in order") {
      val sut = RecordingViewModel()

      sut.attach(LifecycleIntents())
      sut.detach()
      sut.destroy()

      sut.calls shouldBe listOf(
        "preAttach",
        "postAttach",
        "preDetach",
        "postDetach",
        "preDestroy",
        "postDestroy",
      )
    }

    should("log dispatched intents with the view model name") {
      val log = Channel<String>(Channel.UNLIMITED)
      val sut = RecordingViewModel(log = { log.trySend(it()) })
      val intents = LifecycleIntents()
      sut.attach(intents)
      intents.awaitAllBindings()

      intents.add(5)

      withTimeout(3000) { log.receive() } shouldBe "[RecordingViewModel, intent.name=add]"
    }

    should("run initial action once across reattach") {
      val sut = RecordingViewModel()

      sut.attach(LifecycleIntents())
      sut.detach()
      sut.attach(LifecycleIntents())

      withTimeout(3000) { sut.initialActions.first { it > 0 } }
      delay(100)
      sut.initialActions.value shouldBe 1
    }

    should("route intents only from the currently attached intents") {
      val sut = RecordingViewModel()
      val oldIntents = LifecycleIntents()
      val newIntents = LifecycleIntents()

      sut.attach(oldIntents)
      sut.detach()
      sut.attach(newIntents)
      newIntents.awaitAllBindings()

      sut.viewStateFlow.test {
        awaitItem() shouldBe listOf(0)
        oldIntents.add(1)
        newIntents.add(2)
        awaitItem() shouldBe listOf(0, 2)
        newIntents.add(3)
        awaitItem() shouldBe listOf(0, 2, 3)
        delay(100)
        expectNoEvents()
      }
    }

    should("route each intent only to its own binding") {
      val sut = RecordingViewModel()
      val intents = LifecycleIntents()

      sut.attach(intents)
      intents.awaitAllBindings()

      sut.viewStateFlow.test {
        awaitItem() shouldBe listOf(0)
        intents.add(7)
        awaitItem() shouldBe listOf(0, 7)
        intents.clear()
        awaitItem() shouldBe emptyList()
        delay(100)
        expectNoEvents()
      }
    }

    should("stop reacting to non-intent sources after destroy") {
      val sut = RecordingViewModel()

      sut.attach(LifecycleIntents())
      withTimeout(3000) { sut.events.subscriptionCount.first { it > 0 } }
      sut.destroy()

      withTimeout(3000) { sut.events.subscriptionCount.first { it == 0 } }
      sut.events.tryEmit(42)
      delay(100)
      sut.viewStateFlow.value shouldBe listOf(0)
    }
  })

// 2 intent bindings + 1 logger subscribe to the intents stream on attach
private suspend fun ViewIntents.awaitAllBindings() {
  withTimeout(3000) { (stream as MutableSharedFlow<UiIntent>).subscriptionCount.first { it >= 3 } }
}

private class LifecycleIntents : ViewIntents() {
  val add = intent<Int>(name = "add")
  val clear = intent(name = "clear")
}

private class RecordingViewModel(log: (() -> String) -> Unit = {}) : ViewModel<List<Int>, LifecycleIntents>(log = log) {
  val calls = mutableListOf<String>()
  val events = MutableSharedFlow<Int>(extraBufferCapacity = 1)
  val initialActions = MutableStateFlow(0)

  override fun buildMachine(): Machine<List<Int>> = machine {
    initial = listOf(0) to { initialActions.update { it + 1 } }

    onEach(events) {
      transitionTo { state, payload -> state + payload }
    }

    onEach(intent(LifecycleIntents::add)) {
      transitionTo { state, payload -> state + payload }
    }

    onEach(intent(LifecycleIntents::clear)) {
      transitionTo { _, _ -> emptyList() }
    }
  }

  override fun preAttach() {
    calls += "preAttach"
  }

  override fun postAttach() {
    calls += "postAttach"
  }

  override fun preDetach() {
    calls += "preDetach"
  }

  override fun postDetach() {
    calls += "postDetach"
  }

  override fun preDestroy() {
    calls += "preDestroy"
  }

  override fun postDestroy() {
    calls += "postDestroy"
  }
}
