# Shelfify


Shelfify ist eine Android-App zur **Verwaltung von Lebensmitteln** und zur **Rezepte-Organisation**.  
Ziel ist es, Lebensmittel per Barcode oder manuell einzutragen, deren Ablaufdaten im Blick zu behalten und Rezepte basierend auf vorhandenen Zutaten zu nutzen.  
Alle Daten können optional über eine API synchronisiert und mit **Datagroups** geteilt werden.


---


## ✨ Features ✨

- **Lebensmittelverwaltung**
  - Produkte hinzufügen (Name, EAN, Ablaufdatum)
  - Produkte entfernen
  - Abgelaufene Produkte automatisch anzeigen
- **Rezepte** *(in Bearbeitung)*
  - Rezepte mit Zutaten, Zubereitung und Dauer speichern
  - Filterbuttons im RecyclerView für gezieltes Durchsuchen
- **Barcode-Scanner**
  - Integration von Google ML Kit für schnelles Eintragen per Barcode
- **Benachrichtigungen**
  - Erinnerung an ablaufende Produkte
  - Einstellbare E-Mail-Benachrichtigungen und Notify-Optionen
- **Datagroups** *(in Bearbeitung)*
  - Teilen von Daten mit mehreren Nutzern
  - Einladung per E-Mail-Link direkt in die App
- **E-Mail-Integration**
  - E-Mail-Verknüpfung mit der App-ID
  - Token-basierte Authentifizierung für Änderungen
  - **E-Mail-Verifizierung bereits integriert**
- **Sicherheit & API**
  - HTTPS-Unterstützung ist in Planung
  - Lokale App-ID für eindeutige Zuordnung von Nutzerdaten


---


## API (ShelfifyApi)

Shelfify nutzt eine eigene Spring Boot API. Repo: [ShelfifyApi](https://github.com/Iloveschnitzel09/ShelfifyApi)

