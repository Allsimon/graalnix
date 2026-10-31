{ pkgs, lib }:
{
  inherit (pkgs) hello;
  inherit (pkgs.stdenv.hostPlatform) name;
  inherit (lib.evalModules { modules = [ ]; }) options;
  inherit (lib) lib;
  other = pkgs.notOther;
  fallback = pkgs.fallback or null;
}
