package ru.kode.amvi.component.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.kode.amvi.viewmodel.ViewIntents

@Composable
inline fun <reified VI : ViewIntents> rememberViewIntents(): VI =
  remember { VI::class.java.getDeclaredConstructor().newInstance() }
