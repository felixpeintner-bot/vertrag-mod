# Vertrag (Fabric, Minecraft 26.2)

Schreibe einen Vertrag in ein Buch, drücke **R**, unterschreibe mit der Maus.

## So funktioniert es

1. Nimm ein **Buch und Feder** (oder ein signiertes Buch) in die Hand und schreibe den Vertragstext hinein.
   Die **erste Zeile** muss `VERTRAG` heißen (siehe Vorlage unten).
2. Drücke **R** (Taste ist unter Steuerung > "Vertrag" änderbar). Der Vertragsbildschirm öffnet sich.
3. Zeichne mit gedrückter linker Maustaste deine Unterschrift in das Feld und klicke **Unterschreiben & abschließen**.
4. Andere Spieler können dasselbe Buch öffnen und mit **Als Gegenpartei unterschreiben** gegenzeichnen.
5. **Kündigen** kann nur der Spieler, der den Vertrag erstellt (zuerst unterschrieben) hat. Die Prüfung läuft auf dem Server.

Der Vertragsstatus (Ersteller, Unterschriften, gültig/gekündigt) wird direkt im Buch-Item gespeichert
und bleibt erhalten, wenn das Buch weitergegeben wird. Wird der Text nach der Unterschrift geändert,
zeigt der Bildschirm eine Warnung (einfacher Hash, kein kryptografischer Schutz).

## Vertragsvorlage (so in das Buch schreiben)

```
VERTRAG
Vertragspartner: <Name A> und <Name B>

§1 Gegenstand
<Name A> liefert <Name B> 64 Eisenbarren.

§2 Gegenleistung
<Name B> zahlt dafür 10 Diamanten.

§3 Laufzeit
Der Vertrag gilt ab Unterschrift auf unbestimmte Zeit
und kann nur vom Ersteller gekündigt werden.
```

## Bauen

Voraussetzungen: **JDK 25**, Internetzugang für Gradle.

```
gradle wrapper --gradle-version 9.5.1   # einmalig, erzeugt gradlew
./gradlew build
```

Die fertige Mod liegt danach unter `build/libs/vertrag-1.0.0.jar`.
Zum Testen: `./gradlew runClient`.

Benötigt: Minecraft 26.2, Fabric Loader >= 0.19.3, Fabric API (0.161.0+26.2 getestet vorgesehen).

## Modrinth

- Projekttyp: Mod, Loader: Fabric, Spielversion: 26.2
- Umgebung: Client **und** Server (beide brauchen die Mod)
- Abhängigkeit: Fabric API (erforderlich)
- Lizenz: MIT

## Struktur

- `src/main` – gemeinsame Logik: Data Component, Netzwerkpaket, Server-Regeln
- `src/client` – R-Taste und Vertragsbildschirm mit Unterschriftsfeld
