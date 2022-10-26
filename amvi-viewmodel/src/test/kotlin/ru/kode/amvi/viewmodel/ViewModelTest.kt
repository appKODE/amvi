package ru.kode.amvi.viewmodel

import app.cash.turbine.test
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import ru.dimsuz.unicorn2.machine

class ViewModelTest : ShouldSpec({
  should("emit state on start only once") {
    val sut = object : ViewModel<Int, TestIntents>() {
      override fun buildMachine() = machine {
        initial = 3 to null
      }
    }

    sut.viewStateFlow.test {
      awaitItem() shouldBe 3
      delay(100)
      expectNoEvents()
    }
  }

  should("execute transitions in response to non-intent sources after attach") {
    val sut = object : ViewModel<List<Int>, TestIntents>(dispatcher = Dispatchers.Default) {
      override fun buildMachine() = machine<List<Int>> {
        initial = listOf(3) to null

        onEach(flowOf(11, 22, 33).onEach { delay(20) }) {
          transitionTo { state, payload ->
            state + payload
          }
        }
      }
    }

    sut.attach(TestIntents())

    sut.viewStateFlow.test {
      awaitItem() shouldBe listOf(3)
      awaitItem() shouldBe listOf(3, 11)
      awaitItem() shouldBe listOf(3, 11, 22)
      awaitItem() shouldBe listOf(3, 11, 22, 33)
      delay(100)
      expectNoEvents()
    }
  }

  should("execute transitions in response to intent sources after attach") {
    val sut = object : ViewModel<List<Int>, TestIntents>(dispatcher = Dispatchers.Default) {
      override fun buildMachine() = machine<List<Int>> {
        initial = listOf(3) to null

        onEach(intent(TestIntents::intentNoArg)) {
          transitionTo { state, _ ->
            state + 11
          }
        }

        onEach(intent(TestIntents::intentWithArg)) {
          transitionTo { state, payload ->
            state + payload
          }
        }
      }
    }

    val intents = TestIntents()
    sut.attach(intents)

    sut.viewStateFlow.test {
      awaitItem() shouldBe listOf(3)
      intents.intentWithArg(88)
      awaitItem() shouldBe listOf(3, 88)
      intents.intentWithArg(77)
      awaitItem() shouldBe listOf(3, 88, 77)
      intents.intentNoArg()
      awaitItem() shouldBe listOf(3, 88, 77, 11)
      delay(100)
      expectNoEvents()
    }
  }

  should("not execute transitions in response to intent sources after detach") {
    fail("todo")
  }

  should("not execute transitions in response to non-intent sources after detach") {
    fail("todo")
  }
})

private class TestIntents : ViewIntents() {
  val intentNoArg = intent(name = "intentNoArg")
  val intentWithArg = intent<Int>(name = "intentWithArg")
}
