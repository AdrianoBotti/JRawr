# Legal overview

This page is a short guide to the legal files in the repository. The files themselves are
authoritative.

## License

JRawr is licensed under the [Apache License, Version 2.0](../LICENSE). You may use, modify and
distribute the source code and documentation under that license, including the patent grant and
the notice requirements it contains.

## Attribution and provenance

[NOTICE](../NOTICE) contains:

- the original Rawr attribution (Copyright 2008 Chadd Nervig, "HugeHoss", "Toskk", and the Rawr
  Developers);
- the WotLK 3.3.5a fork and published snapshot provenance;
- a summary of the changes JRawr makes on top of the original;
- the game-data and trademark carve-out.

The original C# port source and oracle tools are published at
<https://github.com/AdrianoBotti/JRawrOriginalRawrSnapshot>, tag `jrawr-oracle-1`.

## Third-party components

[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) lists the runtime, browser and test-only
components, their versions, licenses and project links. The most important ones are:

- Spring Boot, the Spring Framework, Apache Tomcat, Thymeleaf, Jackson and Apache Commons Math
  under Apache License 2.0;
- htmx 2.0.4 under the BSD Zero Clause License, loaded from unpkg;
- Font Awesome Free 6.7.2 under CC BY 4.0 for a single embedded icon path.

The original Rawr's GPL-licensed Cephes special-function code in `SpecialFunction.cs` was not
ported. JRawr uses Apache Commons Math 3.6.1 instead.

## Game data, icons and trademarks

The XML caches in `data/` contain World of Warcraft 3.3.5a item, enchant, buff, spell and talent
data. The Apache License does not apply to that data. It remains the property of Blizzard
Entertainment or its respective owners and is included solely to make the calculator functional.
Item icons are hotlinked from third-party sites and are not redistributed by JRawr.

JRawr is not affiliated with or endorsed by Blizzard Entertainment. World of Warcraft, Wrath of the
Lich King, and related names are trademarks of Blizzard Entertainment. Do not use the JRawr name or
assets in a way that suggests Blizzard endorsement.

## Contributions

Contributions are accepted under the Apache License, Version 2.0. See
[CONTRIBUTING.md](../CONTRIBUTING.md). Contributors must have the rights to submit the code, data
and documentation they contribute.

## AI-assisted development

JRawr was developed with substantial AI assistance. The details and the maintainer's
responsibility statement are in the [README](../README.md#ai-assisted-development).
