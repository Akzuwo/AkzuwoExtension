# AkzuwoExtension

AkzuwoExtension ist ein Spigot-Plugin, das ein erweitertes Reportsystem mit Discord-Benachrichtigungen und PlaceholderAPI-Integration bereitstellt. Berichte werden in einer MySQL-Datenbank gespeichert und können im Chat oder in einer GUI verwaltet werden. Optional lässt sich die RankPointsAPI für einen Platzhalter zur Punkteanzeige einbinden.

## Commands
- `/report <Spieler> <Grund>` – Meldet einen Spieler. Meldungen werden an Teammitglieder und auf Discord weitergeleitet.
- `/report claim <ID>` – Weist dir einen offenen Report zu (setzt den Status automatisch auf „in Bearbeitung“).
- `/report unclaim <ID>` – Gibt einen Report wieder frei.
- `/report note <ID> <Text|clear>` – Hinterlässt oder entfernt eine Notiz zu einem Report.
- `/viewreports` – Zeigt offene oder in Bearbeitung befindliche Reports im Chat an.
- `/viewreportsgui` – Öffnet eine GUI zur Verwaltung der Reports. Rechtsklick erhöht, Linksklick verringert den Status.
- `/deletereport <ID>` – Markiert einen Report zur Löschung.
- `/akzuwoextension confirm` – Bestätigt das Löschen eines zuvor markierten Reports.

## Verwendung des GUI
- Es gibt 3 Zustände für Reports (offen/in Bearbeitung/geschlossen).
- Mit Rechtsklick kann der Zustand von „offen“ zu „in Bearbeitung“ und von „in Bearbeitung“ zu „geschlossen“ geändert werden.
- Mit Linksklick kann der Zustand in die andere Richtung geändert werden.
- Beim Sneaken (Shift) wird ein Report nur ausgewählt, ohne den Status zu verändern. Die Auswahl wird hervorgehoben.
- Unter den Reports befinden sich zusätzliche Buttons zum Claimen/Freigeben sowie ein Button „Notiz bearbeiten“.
- Die aktuelle Notiz und der zuständige Staff werden sowohl in der Item-Lore als auch im Chat angezeigt.
- Beim Klicken auf „Notiz bearbeiten“ wird der Spieler aufgefordert, den Text im Chat einzugeben (`clear` entfernt die Notiz, `cancel` bricht ab).

## Workflow für Teammitglieder
1. Öffne die GUI mit `/viewreportsgui` oder nutze `/report claim <ID>`, um dir einen Report zuzuweisen.
2. Während du einen Report bearbeitest, kannst du Status, Zuständigkeit und Notizen entweder über die GUI-Buttons oder die neuen `/report`-Subcommands anpassen.
3. Änderungen an Claim oder Notizen werden automatisch an andere Teammitglieder und den Discord-Channel kommuniziert.
4. Nach Abschluss eines Reports setze den Status auf „geschlossen“ und gib den Report bei Bedarf wieder frei (`/report unclaim <ID>` oder über den GUI-Button).

## Placeholder
- `%akzuwoextension_report_count%` – Anzahl der gespeicherten Reports.
- `%akzuwoextension_cet_time%` – Aktuelle Uhrzeit in Mitteleuropa.
- `%akzuwo_rankpoints%` – RankPoints des Spielers (nur wenn die RankPointsAPI konfiguriert ist).

## Berechtigungen
- `akzuwoextension.staff` – Ermöglicht Teammitgliedern, Reports zu verwalten und einzusehen.
- `akzuwoextension.exempt` – Verhindert, dass ein Spieler über das Reportsystem gemeldet werden kann.

## Installation
1. Platziere das Plugin in deinem `plugins`-Ordner.
2. Starte den Server damit die config.yml generiert wird.
3. Konfiguriere die config.yml und starte dann den Server neu.
4. Falls du RankPointsAPI in der congig.yml konfiguriert hast, kannst du jetzt auch den Placeholder `%akzuwo_rankpoints%` verwenden

## Abhängigkeiten
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI)
- RankPointsAPI (optional für `%akzuwo_rankpoints%`)
