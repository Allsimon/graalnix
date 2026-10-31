# graalnix

An implementation of the Nix language on GraalVM, as a Truffle AST interpreter.
It evaluates nixpkgs unmodified and computes store paths and `.drv` paths in
memory, without writing anything to `/nix/store`.

```sh
nix build .#graalnix          # static native binary (musl)
./result/bin/graalnix eval --strict -E 'builtins.map (x: x * 2) [ 1 2 3 ]'
./result/bin/graalnix drv-path -I nixpkgs=/path/to/nixpkgs -E 'import <nixpkgs> {}' -A hello
```

The documentation lives in [`docs/`](docs/index.adoc): the command line,
parity status, architecture and how to build. `nix build .#docs` renders it to
HTML.
