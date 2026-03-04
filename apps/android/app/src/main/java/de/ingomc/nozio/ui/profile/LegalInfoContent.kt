package de.ingomc.nozio.ui.profile

data class LegalSection(
    val title: String,
    val content: String
)

val legalSections = listOf(
    // TODO: Replace placeholders with legally reviewed real details before Play Store release.
    LegalSection(
        title = "Impressum",
        content = """
            Angaben gemäß § 5 TMG:
            Nozio
            [Bitte Name/Firma eintragen]
            [Bitte vollständige Postadresse eintragen]

            Kontakt:
            E-Mail: [Bitte Kontakt-E-Mail eintragen]

            Verantwortlich für Inhalte nach § 18 Abs. 2 MStV:
            [Bitte verantwortliche Person eintragen]
            [Bitte Anschrift eintragen]
        """.trimIndent()
    ),
    LegalSection(
        title = "Datenschutzerklärung",
        content = """
            Diese App verarbeitet personenbezogene Daten nur, soweit dies zur Bereitstellung der App-Funktionen erforderlich ist.
            Dazu gehören insbesondere:
            - vom Nutzer eingegebene Profil- und Ernährungsdaten
            - technische Verbindungsdaten beim Zugriff auf das API

            Rechtsgrundlagen:
            - Art. 6 Abs. 1 lit. b DSGVO (Vertragserfüllung/Funktionsbereitstellung)
            - Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse an Stabilität und Sicherheit)

            Speicherdauer:
            Daten werden nur so lange gespeichert, wie es für die Bereitstellung der App erforderlich ist oder gesetzliche Pflichten bestehen.

            Betroffenenrechte:
            Es bestehen Rechte auf Auskunft, Berichtigung, Löschung, Einschränkung der Verarbeitung, Datenübertragbarkeit und Widerspruch nach DSGVO.
            Außerdem besteht ein Beschwerderecht bei einer Datenschutzaufsichtsbehörde.

            Kontakt für Datenschutzanfragen:
            [Bitte Datenschutz-Kontakt eintragen]
        """.trimIndent()
    ),
    LegalSection(
        title = "Nutzungsbedingungen",
        content = """
            Die Nutzung der App erfolgt auf eigene Verantwortung.
            Die bereitgestellten Ernährungs- und Fitnessdaten dienen ausschließlich der allgemeinen Information und ersetzen keine medizinische Beratung.

            Der Betreiber haftet nur für Vorsatz und grobe Fahrlässigkeit, soweit gesetzlich zulässig.
            Bei leicht fahrlässiger Verletzung wesentlicher Vertragspflichten ist die Haftung auf den vorhersehbaren, vertragstypischen Schaden begrenzt.
        """.trimIndent()
    )
)
