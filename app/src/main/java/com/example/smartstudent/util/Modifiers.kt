package com.example.smartstudent.util

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Root modifier for form screens that sit outside MainScaffold. */
@Composable
fun Modifier.formScreen(): Modifier =
    this.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState())
