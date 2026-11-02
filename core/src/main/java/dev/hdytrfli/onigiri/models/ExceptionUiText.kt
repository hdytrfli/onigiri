package dev.hdytrfli.onigiri.models

data class ExceptionUiText(val uiText: UiText) : Exception()

data class ExceptionUiTexts(val uiTexts: Collection<UiText>) : Exception()
