# drvPath of every top-level nixpkgs attribute, or false when evaluation throws.
{
  nixpkgs ? <nixpkgs>,
  system ? builtins.currentSystem,
}:
let
  pkgs = import nixpkgs {
    inherit system;
    config = {
      allowAliases = false;
      allowUnfree = true;
    };
  };
  attempt =
    v:
    let
      r = builtins.tryEval (if pkgs.lib.isDerivation v then v.drvPath else null);
    in
    if r.success then r.value else false;
in
builtins.mapAttrs (_: attempt) pkgs
