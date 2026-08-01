# Contributing

Thanks for considering contributing to Aerial.

## Code Of Conduct

This project follows the [Code of Conduct](CODE_OF_CONDUCT.md). By
participating, you are expected to uphold it.

## Reporting Bugs And Requesting Features

Please use the [issue tracker](https://github.com/shapeshed/aerial/issues).
Search existing issues first to avoid duplicates. For security
vulnerabilities, see [SECURITY.md](SECURITY.md) instead of opening a public
issue.

## Development Setup

Build, test, and release instructions live in
[DEVELOPERS.md](../DEVELOPERS.md). In short:

```sh
./gradlew quality        # compile, lint, and unit tests
./gradlew assembleDebug
```

## Submitting A Pull Request

1. Fork the repository and create a branch off `main`.
2. Make your change. Keep pull requests focused on a single change where
   possible.
3. Run `./gradlew quality` locally before pushing; CI runs the same check.
4. Write commit messages in the
   [Conventional Commits](https://www.conventionalcommits.org/) style used
   throughout the project's history, for example `fix(player): ...` or
   `feat(home): ...`.
5. Open a pull request describing what changed and why. Link any related
   issue.

## Station Registry Changes

Aerial's bundled station registry is generated in a separate repository,
[aerial-registry](https://github.com/shapeshed/aerial-registry). Changes to
curated stations, logos, or provider coverage belong there rather than in
this repository — see that project's own contributing guidelines.

## Translating Aerial

Translations are managed through Weblate. The Android string resources in
`app/src/main/res/values/strings.xml` are the source strings; translated
resources live in the locale-specific `values-*` directories.

To contribute a translation:

1. Open the [Aerial translations in Weblate](https://hosted.weblate.org/projects/aerial/strings/)
   and choose a language.
2. Translate or review the suggested strings, preserving placeholders such as
   `%1$s`, XML markup, and string names.
3. Submit the translation in Weblate. Weblate will create or update the
   corresponding Android resource file in a pull request.

You may also submit a normal pull request that updates the appropriate Android
string resource directly. In either workflow, preserve placeholders such as
`%1$s`, XML markup, and string names.

## License

By contributing, you agree that your contributions will be licensed under
the project's [Apache License 2.0](../LICENSE).
