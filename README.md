# graalnix

An implementation of the Nix language on GraalVM, as a Truffle AST interpreter.
It evaluates nixpkgs unmodified and computes store paths and `.drv` paths in
memory, without writing anything to `/nix/store`.

## Usage

```sh
nix build .#graalnix          # static native binary (musl)
./result/bin/graalnix eval --strict -E 'builtins.map (x: x * 2) [ 1 2 3 ]'
./result/bin/graalnix drv-path -I nixpkgs=/path/to/nixpkgs -E 'import <nixpkgs> {}' -A hello
```

`eval` accepts `--strict`, `--json`, `--raw`, `-A`, `--arg`, `--argstr`, `-I`,
`--system` and `--show-trace`. It prints results in the same format as
`nix-instantiate --eval`. Any other `--option=value` is passed to the polyglot
engine; for example, `--inspect` opens the Chrome DevTools debugger and
`--cpusampler` profiles an evaluation.

The build uses [Jenesis](https://jenesis.build), vendored in `build/jenesis`.
Each module's `module-info.java` declares its build, and the tool settings live
in `META-INF/build.jenesis/` next to the module's sources. To develop:

```sh
nix develop
java build/jenesis/Make.java                    # compile, check formatting (palantir-java-format) and run the tests
java -Djenesis.format.rewrite=true build/jenesis/Make.java   # reformat the sources instead
java build/jenesis/Execute.java eval -E '1 + 1'  # run the launcher on the JVM
java -Djenesis.make.profiles=native build/jenesis/Make.java stage
                                                # native launcher in target/stage/native/output
```

Dependency versions and checksums live in one bill of materials,
`build.jenesis/pin-graalnix.properties`, which every module imports with
`@jenesis.bom`. After changing a dependency, regenerate it with
`java -Djenesis.pin.file=build.jenesis/pin-graalnix.properties build/jenesis/Make.java pin`
and run `java build/jenesis/Make.java pin` for the tool pins left in the
modules. Then update `outputHash` of `deps` in `nix/package.nix`;
`nix build .#graalnix.deps` prints the new hash.

## Status

Checked against Nix 2.34.9 and nixpkgs `31ecbaca1bde` on x86_64-linux:

- **Language tests.** graalnix passes NixOS/nix's `tests/functional/lang`
  suite (303 tests) and Tvix's `eval/src/tests/tvix_tests`, apart from the
  cases listed with reasons in `language/src/test/java/lang-skip.txt`.
- **Top-level packages.** For every top-level nixpkgs attribute
  (`scripts/toplevel-drvpaths.nix`), graalnix and `nix-instantiate` produce
  24,268 identical drvPaths. 680 attributes fail in both, and none differ or
  fail in only one evaluator. The run takes 1 min 40 s on the JVM, against
  2 min 08 s for Nix.
- **Heavier attributes.** `scripts/parity.sh` finds identical drvPaths for
  `firefox`, `chromium`, `libreoffice`, `texliveFull`,
  `haskellPackages.pandoc`, `nixosTests.firefox`,
  `pkgsCross.aarch64-multiplatform.hello` and `pkgsStatic.hello`.

## Not supported

- Fetchers (`fetchurl`, `fetchTarball`, `fetchGit`, `fetchTree`) and flakes.
- Import from derivation.
- Content-addressed and impure derivations.
- `--xml` output.
- The `--lint-*` flags.
- Rejection of NUL bytes in strings, and UTF-8 validation in `toJSON`.

## Layout

- `parser/`: a hand-written lexer and parser.
- `store/`: NAR hashing, Nix base32 and store-path computation.
- `language/`: the Truffle language. It contains:
  - `Translator`: static scope resolution into Truffle nodes.
  - `nodes/` and `runtime/`: the evaluator and its values.
  - `builtins/`: the builtins.
- `launcher/`: the CLI, built on the polyglot API, plus its native-image build.
- `language/src/test/java/`: the test module. Its data are packaged with it,
  so the tests rerun whenever the data change:
  - `lang/`: NixOS/nix's `tests/functional/lang` (LGPL-2.1), kept in sync
    with [vendir](https://carvel.dev/vendir/). To follow a new Nix release,
    change `ref` in `vendir.yml`, run `vendir sync`, and update
    `lang-symlinks.txt` if upstream added symlinks (the jar cannot hold them).
  - `graalnix/`: graalnix's own regression tests.
- `language/src/main/java/org/graalnix/corepkgs/` holds Nix's `derivation.nix`
  and `fetchurl.nix`, also vendored from NixOS/nix.
