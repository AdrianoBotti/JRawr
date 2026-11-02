# Contributing to JRawr

Thanks for taking the time to improve JRawr. This project is a port of the original Rawr engine,
so correctness and reproducibility matter more than stylistic preferences.

## License

By submitting a contribution you agree that it is licensed under the Apache License, Version 2.0,
the same license as JRawr. You must have the right to submit the code, data and documentation you
contribute. Do not paste code from projects with incompatible licenses, and do not submit game data
or extracted assets you do not have permission to redistribute.

## Development

Requirements: Java 25. The Gradle wrapper is included.

```bash
cd jrawr
./gradlew build
```

Run the web application with:

```bash
cd jrawr
./gradlew :rawr-web:bootRun
```

The application reads game data from `jrawr/data` and writes characters to `jrawr/characters` by
default. Both are ignored by git.

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for the module layout, data loading, oracle
regeneration and test workflow.

## Pull requests

- Keep changes focused. Separate engine changes, web changes and documentation changes where
  practical.
- Run `./gradlew build` before opening a pull request and make sure it is green.
- Add or update tests for behavior changes. Engine changes must keep the golden-master suites
  passing.
- Never weaken a golden tolerance to make a test pass. If the original behavior is ambiguous, add a
  fixture that shows the difference instead of guessing.
- Do not commit `legacy/`, build output, character files, tool binaries or generated local data.
  The `.gitignore` already covers them.
- Update the public documentation when a user-facing behavior, command or requirement changes.
- Do not include secrets, tokens or personal data.

## Reporting issues

Open an issue at <https://github.com/AdrianoBotti/JRawr/issues>. Include the JRawr version, the
Java version, the operating system, the model or character involved (remove personal data), the
expected result and what happened. For engine differences, include the character XML if possible
and the relevant golden fixture name.

## Generated and AI-assisted work

AI-assisted contributions are allowed. Disclose substantial AI generation in the pull request and
make sure a human has reviewed the result. The project's golden-master tests are the contract for
engine behavior; generated code does not get a pass because it compiles.
