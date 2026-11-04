package com.example.lenote

import com.example.lenote.data.AppLanguage
import com.example.lenote.ink.PageBackground
import com.example.lenote.ink.ShapeSnappingHelper
import com.example.lenote.ui.i18n.ResolvedLanguage
import com.example.lenote.ui.i18n.resolveAppLanguage
import com.example.lenote.ui.i18n.stringsForLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class HandwritingRefinementUnitTest {

    @Test
    fun calculateBaselineSnapDeltaY_alignsToLowerRuledGrid() {
        val wordBottoms = listOf(92f, 96f, 100f)
        // Mediana = 96f, pauta Ruled = 48f -> 96 é múltiplo exato de 48 (2 * 48 = 96), deltaY = 0f
        val deltaY1 = ShapeSnappingHelper.calculateBaselineSnapDeltaY(wordBottoms, 48f)
        assertEquals(0f, deltaY1, 0.001f)

        // Mediana = 90f -> pauta mais próxima é 96f -> deltaY deve ser +6f
        val wordBottoms2 = listOf(88f, 90f, 92f)
        val deltaY2 = ShapeSnappingHelper.calculateBaselineSnapDeltaY(wordBottoms2, 48f)
        assertEquals(6f, deltaY2, 0.001f)

        // Mediana = 102f -> pauta mais próxima é 96f -> deltaY deve ser -6f
        val wordBottoms3 = listOf(100f, 102f, 104f)
        val deltaY3 = ShapeSnappingHelper.calculateBaselineSnapDeltaY(wordBottoms3, 48f)
        assertEquals(-6f, deltaY3, 0.001f)
    }

    @Test
    fun calculateBaselineSnapDeltaY_handlesEmptyList() {
        val deltaY = ShapeSnappingHelper.calculateBaselineSnapDeltaY(emptyList(), 48f)
        assertEquals(0f, deltaY, 0.001f)
    }

    @Test
    fun resolveAppLanguage_honorsSystemLocaleAndExplicitSelection() {
        assertEquals(ResolvedLanguage.PT, resolveAppLanguage(AppLanguage.SYSTEM, Locale("pt", "BR")))
        assertEquals(ResolvedLanguage.ES, resolveAppLanguage(AppLanguage.SYSTEM, Locale("es", "ES")))
        assertEquals(ResolvedLanguage.EN, resolveAppLanguage(AppLanguage.SYSTEM, Locale("en", "US")))
        // Fallback para idiomas não suportados diretamente -> EN
        assertEquals(ResolvedLanguage.EN, resolveAppLanguage(AppLanguage.SYSTEM, Locale("fr", "FR")))

        // Override explícito do usuário independe do locale do sistema
        assertEquals(ResolvedLanguage.PT, resolveAppLanguage(AppLanguage.PT, Locale("en", "US")))
        assertEquals(ResolvedLanguage.EN, resolveAppLanguage(AppLanguage.EN, Locale("pt", "BR")))
        assertEquals(ResolvedLanguage.ES, resolveAppLanguage(AppLanguage.ES, Locale("pt", "BR")))

        val ptStrings = stringsForLanguage(ResolvedLanguage.PT)
        val enStrings = stringsForLanguage(ResolvedLanguage.EN)
        val esStrings = stringsForLanguage(ResolvedLanguage.ES)

        assertEquals("Pautada", ptStrings.pageBackgroundLabel(PageBackground.RULED))
        assertEquals("Ruled", enStrings.pageBackgroundLabel(PageBackground.RULED))
        assertEquals("Rayada", esStrings.pageBackgroundLabel(PageBackground.RULED))
    }
}
