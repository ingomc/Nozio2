package de.ingomc.nozio.ui.profile

data class LegalSection(
    val title: String,
    val content: String
)

val legalSections = listOf(
    LegalSection(
        title = "Impressum",
        content = """
            Angaben gemaess § 5 TMG:
            Nozio
            [Bitte Name/Firma eintragen]
            [Bitte vollstaendige Postadresse eintragen]

            Kontakt:
            E-Mail: [Bitte Kontakt-E-Mail eintragen]

            Verantwortlich fuer Inhalte nach § 18 Abs. 2 MStV:
            [Bitte verantwortliche Person eintragen]
            [Bitte Anschrift eintragen]
        """.trimIndent()
    ),
    LegalSection(
        title = "Datenschutzerklaerung",
        content = """
            Diese App verarbeitet personenbezogene Daten nur, soweit dies zur Bereitstellung der App-Funktionen erforderlich ist.
            Dazu gehoeren insbesondere:
            - vom Nutzer eingegebene Profil- und Ernaehrungsdaten
            - technische Verbindungsdaten beim Zugriff auf das API

            Rechtsgrundlagen:
            - Art. 6 Abs. 1 lit. b DSGVO (Vertragserfuellung/Funktionsbereitstellung)
            - Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse an Stabilitaet und Sicherheit)

            Speicherdauer:
            Daten werden nur so lange gespeichert, wie es fuer die Bereitstellung der App erforderlich ist oder gesetzliche Pflichten bestehen.

            Betroffenenrechte:
            Es bestehen Rechte auf Auskunft, Berichtigung, Loeschung, Einschraenkung der Verarbeitung, Datenuebertragbarkeit und Widerspruch nach DSGVO.
            Ausserdem besteht ein Beschwerderecht bei einer Datenschutzaufsichtsbehoerde.

            Kontakt fuer Datenschutzanfragen:
            [Bitte Datenschutz-Kontakt eintragen]
        """.trimIndent()
    ),
    LegalSection(
        title = "Nutzungsbedingungen",
        content = """
            Die Nutzung der App erfolgt auf eigene Verantwortung.
            Die bereitgestellten Ernaehrungs- und Fitnessdaten dienen ausschliesslich der allgemeinen Information und ersetzen keine medizinische Beratung.

            Der Betreiber haftet nur fuer Vorsatz und grobe Fahrlaessigkeit, soweit gesetzlich zulaessig.
            Bei leicht fahrlaessiger Verletzung wesentlicher Vertragspflichten ist die Haftung auf den vorhersehbaren, vertragstypischen Schaden begrenzt.
        """.trimIndent()
    )
)
