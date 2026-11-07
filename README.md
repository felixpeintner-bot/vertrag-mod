# Vertrag (Fabric, Minecraft 26.2)

Die Mod gehoert auf den **Server** (mit Fabric API). Spieler brauchen nichts zu installieren und koennen alles
mit `/vertrag` bedienen. Wer die gleiche JAR zusaetzlich bei sich im mods-Ordner hat, bekommt die Taste **R**:

- Neuer Vertrag im Buch in der Hand: R unterschreibt mit dem eigenen Spielernamen.
- Vertrag wartet auf die Gegenpartei: R unterschreibt als Gegenpartei.
- Sonst: R zeigt den Vertrag (mit Button zum Aufloesen).
- Ohne Vertrags-Buch in der Hand macht R gar nichts, es kommt keine Meldung.

## Ablauf

1. Buch und Feder in die Hand, Vertrag schreiben. Erste Zeile: `VERTRAG`.
2. `/vertrag` tippen und auf **[Unterschreiben & abschliessen]** klicken, dann den Namen hinter den Befehl
   schreiben: `/vertrag unterschreiben Max Muster`. Das Buch wird zum **ORIGINAL**.
3. Die andere Person haelt das Buch in der Hand, tippt `/vertrag` und klickt
   **[Als Gegenpartei unterschreiben]**: `/vertrag gegenzeichnen Erika Muster`.
4. Ab jetzt gelten die Regeln im Text.
5. Aufloesen kann nur der Ersteller: `/vertrag aufloesen` (oder auf den Button klicken).

## Regeln (feste Saetze)

```
VERTRAG

WENN Ersteller stirbt
DANN gibt Gegenpartei dem Ersteller 2 Diamanten

WENN Gegenpartei stirbt
DANN wird Ersteller getötet
```

- Auslöser: `WENN Ersteller stirbt`, `WENN Gegenpartei stirbt`
- Aktionen: `DANN gibt <Rolle> dem/der <Rolle> <Zahl> <Item>` oder `DANN wird <Rolle> getötet`
- Items: Diamanten, Eisenbarren, Goldbarren, Smaragde, Netheritbarren, Kupferbarren, Kohle,
  Lapislazuli, Redstone, Enderperlen, Goldaepfel, Aepfel, Brote, Pfeile
- Maximal 10 Regeln pro Vertrag, maximal 2304 Stueck pro Regel.
- `/vertrag` zeigt, welche Regeln erkannt wurden.

## Original-Buch

Das Buch traegt goldenen Namen, Glitzern und Vertragsnummer. Solange der Vertrag gueltig ist, ist es als
Gegenstand in der Welt unzerstoerbar (kein Despawn, Feuer, Lava, Explosion). Der Vertragstext und die
Unterschriften liegen auf dem Server, Aenderungen am Buchtext aendern den Vertrag nicht.

## Schulden

Gibt eine Regel Items weiter, entsteht zuerst eine Schuld. Sobald der Schuldner die Items im Inventar hat
und beide online sind, wird automatisch abgebucht und dem Glaeubiger gegeben. `/vertrag schulden` zeigt sie.

## Bauen

JDK 25 und Gradle 9.5.1: `gradle build` -> `build/libs/vertrag-1.0.0.jar`
(oder per GitHub Actions, siehe `.github/workflows/build.yml`).
