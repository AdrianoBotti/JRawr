# JRawr documentation

Version **1.0.1**. Last verified 2026-11-02.

| Document | Read it for |
|---|---|
| [User Guide](https://github.com/AdrianoBotti/JRawr/wiki) | Installation and everyday use: build editor, charts, optimizer, tools, import/export and troubleshooting |
| [../README.md](../README.md) | Project overview, quick start, features and limitations |
| [Architecture handbook](architecture/README.md) | How the application is put together, from HTTP request to model calculation |
| [Development](DEVELOPMENT.md) | Build, test, run, data loading and oracle regeneration |
| [Releasing](RELEASING.md) | Versioning, release archives, checksums and the tag workflow |
| [Legal](LEGAL.md) | License, attribution, game-data carve-out and third-party notices |
| [../THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) | Dependency and asset licenses |
| [../CHANGELOG.md](../CHANGELOG.md) | Release history |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | Contribution rules and pull-request checklist |

The architecture handbook is the public design reference. Start with
[system context](architecture/system-context.md), then read
[modules and dependencies](architecture/modules.md). The code is the final authority; if this
documentation and the code disagree, fix the documentation.

The original C# port source and oracle tools are kept separately in the
[JRawrOriginalRawrSnapshot](https://github.com/AdrianoBotti/JRawrOriginalRawrSnapshot) repository at
tag `jrawr-oracle-1`.

Development-only notes, session logs and the porting decision record live in `docs/dev/`. That
directory is local and gitignored; it is not part of the published documentation.
