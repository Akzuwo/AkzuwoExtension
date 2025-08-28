# AkzuwoExtension

AkzuwoExtension ist ein Spigot-Plugin, das ein erweitertes Reportsystem mit Discord-Benachrichtigungen und PlaceholderAPI-Integration bereitstellt. Berichte werden in einer MySQL-Datenbank gespeichert und können im Chat oder in einer GUI verwaltet werden. Optional lässt sich die RankPointsAPI für einen Platzhalter zur Punkteanzeige einbinden.

## Commands
- `/report <Spieler> <Grund>` – Meldet einen Spieler. Meldungen werden an Teammitglieder und auf Discord weitergeleitet.
- `/viewreports` – Zeigt offene oder in Bearbeitung befindliche Reports im Chat an.
- `/viewreportsgui` – Öffnet eine GUI zur Verwaltung der Reports. Rechtsklick erhöht, Linksklick verringert den Status.
- `/deletereport <ID>` – Markiert einen Report zur Löschung.
- `/akzuwoextension confirm` – Bestätigt das Löschen eines zuvor markierten Reports.

## Placeholder
- `%akzuwoextension_report_count%` – Anzahl der gespeicherten Reports.
- `%akzuwoextension_cet_time%` – Aktuelle Uhrzeit in Mitteleuropa.
- `%akzuwo_rankpoints%` – RankPoints des Spielers (nur wenn die RankPointsAPI konfiguriert ist).

## Installation
1. Lege die `config.yml` an bzw. passe sie an (Datenbank- & Discord-Einstellungen).
2. Platziere das Plugin in deinem `plugins`-Ordner.
3. Stelle sicher, dass PlaceholderAPI installiert ist. Für `%akzuwo_rankpoints%` muss zusätzlich die RankPointsAPI erreichbar sein.

## Abhängigkeiten
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI)
- RankPointsAPI (optional für `%akzuwo_rankpoints%`)
