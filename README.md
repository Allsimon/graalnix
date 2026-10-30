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
java build/jenesis/Make.java                    # compile and run the tests (upstream lang + tests/graalnix)
java build/jenesis/Execute.java eval -E '1 + 1'  # run the launcher on the JVM
java -Djenesis.make.profiles=native build/jenesis/Make.java stage
                                                # native launcher in target/stage/native/output
```

After changing a dependency, run `java build/jenesis/Make.java pin`. Then
update `outputHash` of `deps` in `nix/package.nix`; `nix build .#graalnix.deps`
prints the new hash.

## Status

Checked against Nix 2.34.9 and nixpkgs `31ecbaca1bde` on x86_64-linux:

- **Language tests.** graalnix passes NixOS/nix's `tests/functional/lang`
  suite (303 tests) apart from the cases listed with reasons in
  `tests/lang-skip.txt`.
- **Top-level packages.** For every top-level nixpkgs attribute
  (`scripts/toplevel-drvpaths.nix`), graalnix and `nix-instantiate` produce
  24,268 identical drvPaths. 680 attributes fail in both, and none differ or
  fail in only one evaluator. The run takes 3 min 08 s on the JVM, against
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

- `language/`: the Truffle language. It contains:
  - `parser/`: a hand-written lexer and parser.
  - `Translator`: static scope resolution into Truffle nodes.
  - `nodes/` and `runtime/`: the evaluator and its values.
  - `builtins/`: the builtins.
  - `store/`: NAR hashing, Nix base32 and store-path computation.
- `launcher/`: the CLI, built on the polyglot API, plus its native-image build.
- `tests/lang/`: vendored from NixOS/nix (LGPL-2.1).
  `language/src/main/resources/org/graalnix/corepkgs/` holds Nix's
  `derivation.nix` and `fetchurl.nix`, also vendored from NixOS/nix.
