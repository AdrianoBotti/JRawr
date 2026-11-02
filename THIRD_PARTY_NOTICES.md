# Third-party notices

JRawr depends on the components below. The versions are the ones declared by the Gradle build and
the browser assets in `rawr-web`; transitive dependency versions are managed by the Spring Boot
BOM. A complete dependency tree can be generated with:

```bash
cd jrawr
./gradlew :rawr-web:dependencies
```

## Runtime dependencies

| Component | Version | License | Project |
|---|---|---|---|
| Spring Boot | 4.1.1 | Apache License 2.0 | <https://spring.io/projects/spring-boot> |
| Spring Framework | managed by Spring Boot | Apache License 2.0 | <https://spring.io/projects/spring-framework> |
| Apache Tomcat (embedded) | 11.0.24 | Apache License 2.0 | <https://tomcat.apache.org/> |
| Thymeleaf | 3.1.5.RELEASE | Apache License 2.0 | <https://www.thymeleaf.org/> |
| Jackson (Jackson 3) | 3.1.5 | Apache License 2.0 | <https://github.com/FasterXML/jackson> |
| Apache Commons Math | 3.6.1 | Apache License 2.0 | <https://commons.apache.org/proper/commons-math/> |
| SLF4J | 2.0.18 | MIT License | <https://www.slf4j.org/> |
| Logback | 1.5.38 | EPL-1.0 or LGPL-2.1 | <https://logback.qos.ch/> |

Spring Boot brings additional Apache-2.0, MIT, EPL and BSD-style dependencies (for example
Micrometer, SnakeYAML and the Jakarta APIs). Their license files are included in the published
dependency artifacts.

## Browser and bundled assets

| Component | Version | License | Notes |
|---|---|---|---|
| htmx | 2.0.4 | BSD Zero Clause License (0BSD) | Loaded from `https://unpkg.com/htmx.org@2.0.4` at runtime; not bundled. |
| Font Awesome Free | 6.7.2 | CC BY 4.0 | A single `scroll` icon path is embedded in the application template. |

Font Awesome attribution: Font Awesome Free by Fonticons, Inc., <https://fontawesome.com/license/free>.

## Test-only dependencies

These are not part of the runtime distribution.

| Component | Version | License | Project |
|---|---|---|---|
| JUnit Jupiter / JUnit Platform | 5.11.4 / 1.11.4 | Eclipse Public License 2.0 | <https://junit.org/junit5/> |
| AssertJ | 3.27.3 | Apache License 2.0 | <https://assertj.github.io/doc/> |
| HtmlUnit | 3.11.0 | Apache License 2.0 | <https://htmlunit.sourceforge.io/> |
| Spring Boot Test / Spring MVC Test | managed by Spring Boot | Apache License 2.0 | <https://spring.io/projects/spring-boot> |
| Jackson Databind | 2.18.2 | Apache License 2.0 | <https://github.com/FasterXML/jackson> |

HtmlUnit brings transitive test-only dependencies under Apache-2.0, MIT and BSD-style licenses. They
are resolved by Gradle and are not redistributed in the JRawr release archive.

## Original Rawr special functions

The original Rawr contains GPL-licensed Cephes special-function code in `SpecialFunction.cs`. That
file was not ported. JRawr uses Apache Commons Math 3.6.1 for gamma, logGamma, erf, erfc and the
regularized beta functions.

## Game data and trademarks

The XML caches in `data/` contain World of Warcraft 3.3.5a item, enchant, buff, spell and talent data.
They are not covered by the Apache License. They are included solely to make the calculator
functional and remain the property of Blizzard Entertainment or their respective owners. Item icons
are hotlinked from third-party sites and are not redistributed by JRawr.

JRawr is not affiliated with or endorsed by Blizzard Entertainment. World of Warcraft, Wrath of the
Lich King, and related names are trademarks of Blizzard Entertainment.
