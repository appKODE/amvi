package ru.kode.amvi.viewmodel

import androidx.compose.runtime.Stable
import java.util.UUID

data class UiIntent internal constructor(
  val id: Long,
  val factoryId: String,
  val name: String,
  val payload: Any,
)

interface UiIntentFactory {
  val name: String
  fun isOwnerOf(intent: UiIntent): Boolean

  override fun equals(other: Any?): Boolean
  override fun hashCode(): Int
}

@Stable
class UiIntentFactory0 internal constructor(
  override val name: String,
  emit: (UiIntent) -> Unit,
) : Function0<Unit>, UiIntentFactory {
  private val wrapped = UiIntentFactory1<Unit>(name, emit)

  override fun invoke() {
    wrapped.invoke(Unit)
  }

  override fun isOwnerOf(intent: UiIntent): Boolean {
    return wrapped.isOwnerOf(intent)
  }

  override fun equals(other: Any?): Boolean {
    return when {
      this === other -> true
      other is UiIntentFactory0 -> this.wrapped == other.wrapped
      else -> this.wrapped == other
    }
  }

  override fun hashCode(): Int {
    return wrapped.hashCode()
  }
}

@Stable
class UiIntentFactory1<T : Any> internal constructor(
  override val name: String,
  private val emit: (UiIntent) -> Unit,
) : Function1<T, Unit>, UiIntentFactory {
  private val factoryId = UUID.randomUUID().toString()
  private var nextId = 0L

  override fun isOwnerOf(intent: UiIntent) = intent.factoryId == factoryId

  override fun invoke(payload: T) {
    emit(UiIntent(nextId++, factoryId, name, payload))
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false
    other as UiIntentFactory1<*>
    if (factoryId != other.factoryId) return false
    return true
  }

  override fun hashCode(): Int {
    return factoryId.hashCode()
  }
}
