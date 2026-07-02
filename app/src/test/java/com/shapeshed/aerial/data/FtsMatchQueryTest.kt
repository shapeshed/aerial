package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FtsMatchQueryTest {

    @Test
    fun singleTermGetsPrefixWildcard() {
        assertEquals("jazz*", toFtsMatchQuery("jazz"))
    }

    @Test
    fun multipleTermsEachGetPrefixWildcard() {
        assertEquals("radio* jazz*", toFtsMatchQuery("radio jazz"))
    }

    @Test
    fun punctuationAndOperatorsAreStripped() {
        // Splitting on non-alphanumerics also removes FTS operator chars, avoiding syntax errors.
        assertEquals("rock* roll*", toFtsMatchQuery("rock & roll"))
        assertEquals("radio* 1*", toFtsMatchQuery("radio-1"))
    }

    @Test
    fun accentsArePreservedInTheTerm() {
        // Folding is done by the tokenizer at query time, not here.
        assertEquals("café*", toFtsMatchQuery("Café"))
    }

    @Test
    fun blankQueryProducesEmptyMatch() {
        assertEquals("", toFtsMatchQuery("   "))
        assertEquals("", toFtsMatchQuery("!!!"))
    }
}
