# AkzuwoExtension

AkzuwoExtension ist ein Spigot-Plugin, das ein erweitertes Reportsystem mit Discord-Benachrichtigungen und PlaceholderAPI-Integration bereitstellt. Berichte werden in einer MySQL-Datenbank gespeichert und können im Chat oder in einer GUI verwaltet werden. Optional lässt sich die RankPointsAPI für einen Platzhalter zur Punkteanzeige einbinden.

## Commands
- `/report <Spieler> <Grund>` – Meldet einen Spieler. Meldungen werden an Teammitglieder und auf Discord weitergeleitet.
- `/viewreports` – Zeigt offene oder in Bearbeitung befindliche Reports im Chat an.
- `/viewreportsgui` – Öffnet eine GUI zur Verwaltung der Reports. Rechtsklick erhöht, Linksklick verringert den Status.
- `/deletereport <ID>` – Markiert einen Report zur Löschung.
- `/akzuwoextension confirm` – Bestätigt das Löschen eines zuvor markierten Reports.

## Verwendung des GUI
- Es gibt 3 Zustände für Reports (offen/in Bearbeitung/gschlossen)
- Mit rechtsklick kann der Zustand von 'offen' z'u in Bearbeitung' und von 'in Bearbeitung' zu 'gschlossen' geändert werden
- Mit linksklick kann der Zustand in die andere Richtung geändert werden

## Placeholder
- `%akzuwoextension_report_count%` – Anzahl der gespeicherten Reports.
- `%akzuwoextension_cet_time%` – Aktuelle Uhrzeit in Mitteleuropa.
- `%akzuwo_rankpoints%` – RankPoints des Spielers (nur wenn die RankPointsAPI konfiguriert ist).

## Installation
1. Platziere das Plugin in deinem `plugins`-Ordner.
2. Starte den Server damit die config.yml generiert wird.
3. Konfiguriere die config.yml und starte dann den Server neu.
4. Falls du RankPointsAPI in der congig.yml konfiguriert hast, kannst du jetzt auch den Placeholder `%akzuwo_rankpoints%` verwenden

## Abhängigkeiten
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI)
- RankPointsAPI (optional für `%akzuwo_rankpoints%`)
