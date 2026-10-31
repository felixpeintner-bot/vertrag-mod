# Vertrag (Fabric, Minecraft 26.2)

Schreibe einen Vertrag in ein Buch, drücke **R**, unterschreibe mit der Maus.
Der Vertragstext wird vom Server ausgeführt, sobald **beide** Personen unterschrieben haben.

## Ablauf

1. Buch und Feder in die Hand, Vertrag schreiben. Erste Zeile: `VERTRAG`.
2. **R** drücken, mit gedrückter Maustaste unterschreiben, "Unterschreiben & abschließen".
3. Die andere Person hält das Buch in der Hand, drückt **R** und klickt "Als Gegenpartei unterschreiben".
4. Ab jetzt gelten die Regeln im Text. Kündigen kann nur der Ersteller.

## Regeln (feste Sätze)

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
- Maximal 10 Regeln pro Vertrag, maximal 2304 Stück pro Regel.
- Im Vertragsfenster steht, wie viele Regeln erkannt wurden.

## Schulden

Gibt eine Regel Items weiter, entsteht zuerst eine **Schuld**. Sobald der Schuldner die Items im
Inventar hat und beide Personen online sind, wird automatisch abgebucht und dem Gläubiger gegeben.
Hat der Schuldner zu wenig, wird das Vorhandene übergeben und der Rest bleibt als Schuld offen.
Schulden bleiben auch nach einer Kündigung bestehen.

## Bauen

JDK 25 und Gradle 9.5.1: `gradle build` -> `build/libs/vertrag-1.0.0.jar`
(oder per GitHub Actions, siehe `.github/workflows/build.yml`).

Benötigt: Minecraft 26.2, Fabric Loader >= 0.19.3, Fabric API.
