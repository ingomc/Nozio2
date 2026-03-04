package de.ingomc.nozio.ui.profile

import org.junit.Assert.assertTrue
import org.junit.Test

class LegalInfoContentTest {

    @Test
    fun legalSectionsContainRequiredGermanDocuments() {
        val titles = legalSections.map { it.title }
        assertTrue(titles.contains("Impressum"))
        assertTrue(titles.contains("Datenschutzerklärung"))
        assertTrue(titles.contains("Nutzungsbedingungen"))
    }

    @Test
    fun legalSectionsHaveNonBlankContent() {
        assertTrue(legalSections.all { it.content.isNotBlank() })
    }
}
