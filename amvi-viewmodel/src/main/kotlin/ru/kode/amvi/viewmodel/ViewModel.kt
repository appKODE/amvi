package ru.kode.amvi.viewmodel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.dimsuz.unicorn2.Machine

abstract class ViewModel<VS : Any, VI : ViewIntents, VE : Any>(
  dispatcher: CoroutineDispatcher = Dispatchers.Default,
  private val log: (() -> String) -> Unit = { lazyMessage -> println(lazyMessage()) },
) {

  protected val viewModelScope: CoroutineScope = CoroutineScope(dispatcher + CoroutineName("ViewModel"))
  private val attachedScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())

  protected abstract fun buildMachine(): Machine<VS>

  // lazy initialization is needed here because otherwise buildMachine() will be called during class initialization,
  // and before inheriting classes had a chance to initialize
  // their eventPayloads (things that are passed to onEach(...))
  private val machine: Machine<VS> by lazy(LazyThreadSafetyMode.NONE) {
    buildMachine()
  }

  private val stateFlow by lazy(LazyThreadSafetyMode.NONE) { MutableStateFlow(machine.initial.first) }
  val viewStateFlow: StateFlow<VS> get() = stateFlow

  private val intentBinders = arrayListOf<IntentBinder<VI>>()

  private var isFirstViewAttach = true

  private fun bindIntents() {
    machine.states
      .onEach { stateFlow.emit(it) }
      .catch { throw IllegalStateException("exception while reducing view state", it) }
      .launchIn(viewModelScope)
  }

  fun attach(intents: VI) {
    check(intents === intents) {
      "Expected View.intents to always return the same instance, internal error"
    }

    preAttach()
    if (isFirstViewAttach) {
      isFirstViewAttach = false
      bindIntents()
    }
    intentBinders.map { binder ->
      val intent = binder.intent(intents)
      intents.stream
        .filter {
          intent.isOwnerOf(it)
        }
        .catch {
          throw IllegalStateException("intent \"${intent.name}\" has thrown an exception", it)
        }
        .onEach {
          binder.relay.emit(it.payload)
        }
        .launchIn(attachedScope)
    }

    intents.stream
      .onEach { intent ->
        log {
          val screenName =
            this@ViewModel::class.simpleName?.takeLastWhile { it != '.' }?.takeWhile { it != '$' }
          "[$screenName, intent.name=${intent.name}]"
        }
      }
      .launchIn(attachedScope)

    if (isFirstViewAttach) {
      isFirstViewAttach = false
    }
    postAttach()
  }

  fun detach() {
    preDetach()
    attachedScope.coroutineContext.cancelChildren()
    postDetach()
  }

  fun destroy() {
    preDestroy()
    viewModelScope.cancel()
    attachedScope.cancel()
    postDestroy()
  }

  protected open fun preAttach() = Unit
  protected open fun postAttach() = Unit
  protected open fun preDetach() = Unit
  protected open fun postDetach() = Unit
  protected open fun preDestroy() = Unit
  protected open fun postDestroy() = Unit

  @Suppress("UNCHECKED_CAST") // internally type of payload is irrelevant
  private fun <I : Any> intentInternal(bindOp: (VI) -> UiIntentFactory): Flow<I> {
    val binder = IntentBinder(
      bindOp,
      MutableSharedFlow()
    )
    intentBinders.add(binder)
    return binder.relay as Flow<I>
  }

  @Suppress("UNCHECKED_CAST") // we actually know the type of payload
  @JvmName("intent1")
  fun <I : Any> intent(bindOp: (VI) -> UiIntentFactory1<I>): Flow<I> {
    return intentInternal(bindOp)
  }

  @Suppress("UNCHECKED_CAST") // we actually know the type of payload
  @JvmName("intent0")
  fun intent(bindOp: (VI) -> UiIntentFactory0): Flow<Unit> {
    return intentInternal(bindOp)
  }

  private data class IntentBinder<VI : Any>(
    val intent: (VI) -> UiIntentFactory,
    val relay: MutableSharedFlow<Any>,
  )
}
