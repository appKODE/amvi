package ru.kode.amvi.viewmodel

import app.cash.turbine.test
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class ViewIntentsTest :
  ShouldSpec({
    should("use explicit intent names") {
      val sut = NamedIntents()

      sut.noArg.name shouldBe "noArg"
      sut.withArg.name shouldBe "withArg"
    }

    should("generate default intent names") {
      val sut = DefaultNamedIntents()

      sut.noArg.name shouldStartWith "name-"
      sut.withArg.name shouldStartWith "name-"
    }

    should("publish invoked intents to the stream in order") {
      val sut = NamedIntents()

      sut.stream.test {
        sut.withArg("a")
        sut.noArg()

        val first = awaitItem()
        first.name shouldBe "withArg"
        first.payload shouldBe "a"
        sut.withArg.isOwnerOf(first) shouldBe true

        val second = awaitItem()
        second.name shouldBe "noArg"
        second.payload shouldBe Unit
        sut.noArg.isOwnerOf(second) shouldBe true
      }
    }
  })

private class NamedIntents : ViewIntents() {
  val noArg = intent(name = "noArg")
  val withArg = intent<String>(name = "withArg")
}

private class DefaultNamedIntents : ViewIntents() {
  val noArg = intent()
  val withArg = intent<String>()
}
