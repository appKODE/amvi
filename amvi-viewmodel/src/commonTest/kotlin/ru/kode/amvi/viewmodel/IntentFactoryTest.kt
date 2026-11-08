package ru.kode.amvi.viewmodel

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class IntentFactoryTest :
  ShouldSpec({
    should("emit intents with name, payload, increasing ids and a shared factory id") {
      val emitted = mutableListOf<UiIntent>()
      val sut = UiIntentFactory1<Int>("click", emitted::add)

      sut(10)
      sut(20)
      sut(30)

      emitted shouldHaveSize 3
      emitted.map { it.id } shouldBe listOf(0L, 1L, 2L)
      emitted.map { it.payload } shouldBe listOf(10, 20, 30)
      emitted.map { it.name }.toSet() shouldBe setOf("click")
      emitted.map { it.factoryId }.toSet() shouldHaveSize 1
    }

    should("emit Unit payload from no-arg factory") {
      val emitted = mutableListOf<UiIntent>()
      val sut = UiIntentFactory0("refresh", emitted::add)

      sut()
      sut()

      emitted.map { it.payload } shouldBe listOf(Unit, Unit)
      emitted.map { it.id } shouldBe listOf(0L, 1L)
      emitted.map { it.name }.toSet() shouldBe setOf("refresh")
    }

    should("own only intents it has emitted, even if another factory has the same name") {
      val emitted = mutableListOf<UiIntent>()
      val first = UiIntentFactory1<Int>("same", emitted::add)
      val second = UiIntentFactory1<Int>("same", emitted::add)
      val noArg = UiIntentFactory0("same", emitted::add)

      first(1)
      second(2)
      noArg()

      first.isOwnerOf(emitted[0]) shouldBe true
      first.isOwnerOf(emitted[1]) shouldBe false
      first.isOwnerOf(emitted[2]) shouldBe false
      second.isOwnerOf(emitted[1]) shouldBe true
      second.isOwnerOf(emitted[0]) shouldBe false
      noArg.isOwnerOf(emitted[2]) shouldBe true
      noArg.isOwnerOf(emitted[0]) shouldBe false
    }

    should("generate unique factory ids") {
      val ids = List(100) {
        val emitted = mutableListOf<UiIntent>()
        UiIntentFactory1<Int>("x", emitted::add)(0)
        emitted.single().factoryId
      }

      ids.toSet() shouldHaveSize 100
    }

    should("be equal only to itself") {
      val first = UiIntentFactory1<Int>("same") {}
      val second = UiIntentFactory1<Int>("same") {}
      val firstNoArg = UiIntentFactory0("same") {}
      val secondNoArg = UiIntentFactory0("same") {}

      first shouldBe first
      first shouldNotBe second
      first.hashCode() shouldBe first.hashCode()
      firstNoArg shouldBe firstNoArg
      firstNoArg shouldNotBe secondNoArg
      firstNoArg.hashCode() shouldBe firstNoArg.hashCode()
      firstNoArg shouldNotBe first
      first shouldNotBe null
    }
  })
