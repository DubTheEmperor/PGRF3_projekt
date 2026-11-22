# PGRF3 – Úloha 1: 3D scéna

Java + LWJGL 3.4.3, OpenGL 4.3 (GLSL 430).

## Spuštění

- Požadavky: JDK 26, LWJGL 3.4.3, GPU s OpenGL 4.3.
- IntelliJ IDEA: spustit `app.App`. Složky `res/` a `shaders/` jsou resource rooty.

## Scéna

| Objekt | Popis |
|---|---|
| Elephant | model z OBJ, mramor + normal mapa |
| Cube | cihly + normal mapa |
| Pyramid | zlato |
| Icosahedron | kostka d20 (čísla 1–20) |
| Light | žlutý dvacetistěn = poloha bodového světla |
| Osy | X červená, Y zelená, Z modrá (nejdou vybrat) |

## Ovládání

| Vstup                             | Akce                                                      |
|-----------------------------------|-----------------------------------------------------------|
| **Kamera**                        |                                                           |
| Levé tlačítko myši + tah          | bez výběru: rozhlížení; s výběrem: orbit kolem objektu    |
| Kolečko                           | bez výběru: posun vpřed/vzad; s výběrem: přiblížení       |
| `W` `S` / `A` `D`                 | posun nahoru/dolů / doleva/doprava (jen bez výběru)       |
| **Výběr**                         |                                                           |
| Pravé tlačítko myši               | výběr objektu (color picking), klik do pozadí výběr zruší |
| `←` / `→`                         | předchozí / další objekt                                  |
| **Transformace aktivního objektu** |                                                           |
| `1` `2` `3`                       | osa X / Y / Z                                             |
| `4` `5` `6`                       | posun / rotace / scale                                    |
| `Q` / `E`            		    | krok mínus / plus                                         |
| Backspace                         | návrat do výchozí polohy                                  |
| **Světlo**                        | vybrat „Light“, pak osa + `Q`/`E` (jde jen posouvat)      |
| **Debug**                         |                                                           |
| `;` (klávesa pod Esc)             | zapnout / vypnout debug zobrazení                         |
| Tab                               | další debug režim                                         |
| Esc                               | konec                                                     |

Debug režimy (v pořadí): pozice (svět xyz), hloubka, barva vrcholu, textura bez stínování, normála, texturovací souřadnice, vzdálenost od světla, tečna, difuzní člen N·L.

Vlevo nahoře je aktivní objekt, osa a režim transformace a debug režim. Vlevo dole je náhled picking textury (aktivní objekt zeleně).
