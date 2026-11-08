package ru.kode.amvi.component.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewIntents
import ru.kode.amvi.viewmodel.ViewModel

class MviComponentTest :
  ShouldSpec({
    should("attach on enter and detach on dispose") {
      withComposition {
        val viewModel = CountingViewModel()
        val intents = TestIntents()
        setContent { MviComponent(viewModel, intents) { _, _ -> } }

        viewModel.attached shouldBe 1
        viewModel.detached shouldBe 0

        composition.dispose()

        viewModel.detached shouldBe 1
      }
    }

    should("render initial and updated view state") {
      withComposition {
        val viewModel = CountingViewModel()
        val rendered = mutableListOf<Int>()
        setContent { MviComponent(viewModel, TestIntents()) { state, _ -> rendered += state } }

        rendered shouldBe listOf(0)
        withTimeout(3000) { viewModel.events.subscriptionCount.first { it > 0 } }
        viewModel.events.emit(5)
        awaitUntil { rendered.last() == 5 }
      }
    }

    should("pass intents to content and route them to the view model") {
      withComposition {
        val viewModel = CountingViewModel()
        var renderedState = -1
        var contentIntents: TestIntents? = null
        val intents = TestIntents()
        setContent {
          MviComponent(viewModel, intents) { state, i ->
            renderedState = state
            contentIntents = i
          }
        }

        contentIntents shouldBeSameInstanceAs intents
        // intent bindings subscribe asynchronously after attach: re-send until one is delivered
        awaitUntil {
          if (renderedState == 0) intents.add(3)
          renderedState >= 3
        }
      }
    }

    should("detach previous view model and attach new one when it changes") {
      withComposition {
        val first = CountingViewModel()
        val second = CountingViewModel()
        var current by mutableStateOf(first)
        setContent { MviComponent(current, TestIntents()) { _, _ -> } }

        current = second
        awaitUntil { second.attached == 1 }

        first.attached shouldBe 1
        (first.detached >= 1) shouldBe true
        second.detached shouldBe 0

        composition.dispose()
        second.detached shouldBe 1
      }
    }

    should("keep remembered intents across recompositions") {
      withComposition {
        var trigger by mutableStateOf(0)
        val created = mutableListOf<TestIntents>()
        val seen = mutableListOf<TestIntents>()
        setContent {
          trigger.let { }
          seen += rememberViewIntents { TestIntents().also { created += it } }
        }

        trigger = 1
        awaitUntil { seen.size == 2 }

        created.size shouldBe 1
        seen[1] shouldBeSameInstanceAs seen[0]
      }
    }

    should("create intents reflectively with reified rememberViewIntents") {
      withComposition {
        var trigger by mutableStateOf(0)
        val seen = mutableListOf<TestIntents>()
        setContent {
          trigger.let { }
          seen += rememberViewIntents<TestIntents>()
        }

        trigger = 1
        awaitUntil { seen.size == 2 }

        seen[1] shouldBeSameInstanceAs seen[0]
      }
    }

    should("create new intents for a new composition") {
      val first = withComposition {
        lateinit var i: TestIntents
        setContent { i = rememberViewIntents() }
        i
      }
      val second = withComposition {
        lateinit var i: TestIntents
        setContent { i = rememberViewIntents() }
        i
      }

      first shouldNotBeSameInstanceAs second
    }
  })

private class ComposeHarness(val composition: Composition, private val clock: BroadcastFrameClock) {
  fun setContent(content: @Composable () -> Unit) = composition.setContent(content)

  suspend fun awaitUntil(condition: () -> Boolean) = withTimeout(3000) {
    while (!condition()) {
      Snapshot.sendApplyNotifications()
      clock.sendFrame(System.nanoTime())
      delay(10)
    }
  }
}

private suspend fun <T> withComposition(block: suspend ComposeHarness.() -> T): T = coroutineScope {
  val clock = BroadcastFrameClock()
  val recomposer = Recomposer(coroutineContext + clock)
  val loop = launch(clock) { recomposer.runRecomposeAndApplyChanges() }
  val composition = Composition(UnitApplier, recomposer)
  try {
    ComposeHarness(composition, clock).block()
  } finally {
    composition.dispose()
    recomposer.cancel()
    loop.cancelAndJoin()
  }
}

private object UnitApplier : AbstractApplier<Unit>(Unit) {
  override fun insertTopDown(index: Int, instance: Unit) = Unit
  override fun insertBottomUp(index: Int, instance: Unit) = Unit
  override fun remove(index: Int, count: Int) = Unit
  override fun move(from: Int, to: Int, count: Int) = Unit
  override fun onClear() = Unit
}

class TestIntents : ViewIntents() {
  val add = intent<Int>(name = "add")
}

private class CountingViewModel : ViewModel<Int, TestIntents>(log = {}) {
  val events = MutableSharedFlow<Int>(extraBufferCapacity = 1)
  var attached = 0
  var detached = 0

  override fun buildMachine(): Machine<Int> = machine {
    initial = 0 to null

    onEach(events) {
      transitionTo { _, payload -> payload }
    }

    onEach(intent(TestIntents::add)) {
      transitionTo { state, payload -> state + payload }
    }
  }

  override fun postAttach() {
    attached++
  }

  override fun postDetach() {
    detached++
  }
}
