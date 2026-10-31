{ pkgs, lib }:
{
  hello = pkgs.hello;
  name = pkgs.stdenv.hostPlatform.name;
  options = (lib.evalModules { modules = [ ]; }).options;
  lib = lib.lib;
  other = pkgs.notOther;
  fallback = pkgs.fallback or null;
}
