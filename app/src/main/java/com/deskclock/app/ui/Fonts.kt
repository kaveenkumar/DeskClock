package com.deskclock.app.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.deskclock.app.R

/**
 * Selectable clock faces. All bundled fonts are OFL-licensed from Google Fonts.
 *
 * [lineTrim] is the line height (in em) the big clock trims to: Roboto's digits are well known to
 * sit around 0.72em so it trims hard, while the display faces get conservative values since their
 * metrics vary — a font that clips at the top just needs its value raised here.
 * [variableWeight] marks variable-weight files that need an explicit wght axis instance.
 */
enum class ClockFont(
    val label: String,
    private val resId: Int?,
    val weight: FontWeight,
    val lineTrim: Float,
    private val variableWeight: Boolean = false,
) {
    ROBOTO("Roboto (default)", null, FontWeight.SemiBold, 0.82f),
    ORBITRON("Orbitron", R.font.orbitron, FontWeight.SemiBold, 0.85f, variableWeight = true),
    AUDIOWIDE("Audiowide", R.font.audiowide, FontWeight.Normal, 0.95f),
    OSWALD("Oswald", R.font.oswald, FontWeight.SemiBold, 0.90f, variableWeight = true),
    BEBAS("Bebas Neue", R.font.bebasneue, FontWeight.Normal, 0.85f),
    RUBIK_MONO("Rubik Mono One", R.font.rubikmonoone, FontWeight.Normal, 0.90f),
    SHARE_TECH("Share Tech Mono", R.font.sharetechmono, FontWeight.Normal, 0.90f),
    VT323("VT323", R.font.vt323, FontWeight.Normal, 0.95f),
    PRESS_START("Press Start 2P", R.font.pressstart2p, FontWeight.Normal, 1.0f),
    DOTO("Doto", R.font.doto, FontWeight.Bold, 0.95f, variableWeight = true),
    MONOTON("Monoton", R.font.monoton, FontWeight.Normal, 1.0f),
    POIRET("Poiret One", R.font.poiretone, FontWeight.Normal, 0.90f),
    COMFORTAA("Comfortaa", R.font.comfortaa, FontWeight.SemiBold, 0.95f, variableWeight = true),
    ANTON("Anton", R.font.anton, FontWeight.Normal, 0.90f),
    FJALLA("Fjalla One", R.font.fjallaone, FontWeight.Normal, 0.90f),
    UNICA("Unica One", R.font.unicaone, FontWeight.Normal, 0.90f),
    RIGHTEOUS("Righteous", R.font.righteous, FontWeight.Normal, 0.95f),
    BUNGEE("Bungee", R.font.bungee, FontWeight.Normal, 1.0f),
    MICHROMA("Michroma", R.font.michroma, FontWeight.Normal, 0.95f),
    ZEN_DOTS("Zen Dots", R.font.zendots, FontWeight.Normal, 0.95f),
    ALDRICH("Aldrich", R.font.aldrich, FontWeight.Normal, 0.95f),
    QUANTICO("Quantico", R.font.quantico, FontWeight.Normal, 0.92f),
    CHAKRA("Chakra Petch", R.font.chakrapetch, FontWeight.Normal, 0.92f),
    ICELAND("Iceland", R.font.iceland, FontWeight.Normal, 0.92f),
    WALLPOET("Wallpoet", R.font.wallpoet, FontWeight.Normal, 0.95f),
    MAJOR_MONO("Major Mono Display", R.font.majormono, FontWeight.Normal, 0.95f),
    DM_SERIF("DM Serif Display", R.font.dmserifdisplay, FontWeight.Normal, 0.95f),
    ;

    @OptIn(ExperimentalTextApi::class)
    fun family(): FontFamily = when {
        resId == null -> FontFamily.Default
        variableWeight -> FontFamily(
            Font(
                resId,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            ),
        )
        else -> FontFamily(Font(resId, weight = weight))
    }
}
