# Checks that lint/src/test/java/builders.txt still gives the strictDeps and __structuredAttrs
# that nixpkgs' builders set on a derivation that does not set them.
{
  lib,
  pkgs,
  runCommand,
}:
let
  table = lib.pipe ../lint/src/test/java/builders.txt [
    builtins.readFile
    (lib.splitString "\n")
    (lib.filter (l: l != "" && !lib.hasPrefix "#" l))
    (map (l: lib.filter (w: w != "") (lib.splitString " " l)))
    (map (w: {
      name = lib.elemAt w 0;
      value = {
        strictDeps = lib.elemAt w 1 == "true";
        __structuredAttrs = lib.elemAt w 2 == "true";
      };
    }))
    lib.listToAttrs
  ];

  # The least each builder needs to evaluate its attributes; nothing gets built.
  base = {
    pname = "probe";
    version = "0";
    src = pkgs.emptyDirectory;
  };
  probes = {
    mkDerivation = pkgs.stdenv.mkDerivation base;
    buildDotnetModule = pkgs.buildDotnetModule (base // { nugetDeps = null; });
    buildDunePackage = pkgs.ocamlPackages.buildDunePackage base;
    buildGoModule = pkgs.buildGoModule (base // { vendorHash = null; });
    buildHomeAssistantComponent = pkgs.buildHomeAssistantComponent (base // { owner = "probe"; domain = "probe"; });
    buildMavenPackage = pkgs.maven.buildMavenPackage (base // { mvnHash = ""; });
    buildNpmPackage = pkgs.buildNpmPackage (base // { npmDepsHash = ""; });
    buildPythonApplication = pkgs.python3Packages.buildPythonApplication (base // { pyproject = true; });
    buildPythonPackage = pkgs.python3Packages.buildPythonPackage (base // { pyproject = true; });
    buildRustPackage = pkgs.rustPlatform.buildRustPackage (base // { cargoHash = ""; });
  };
  actual = lib.mapAttrs (_: drv: {
    strictDeps = drv.strictDeps or false;
    __structuredAttrs = drv.__structuredAttrs or false;
  }) probes;

  show = v: "strictDeps ${lib.boolToString v.strictDeps}, __structuredAttrs ${lib.boolToString v.__structuredAttrs}";
  problems =
    map (n: "${n}: builders.txt has no row for it") (lib.attrNames (removeAttrs actual (lib.attrNames table)))
    ++ map (n: "${n}: no probe in nix/lint-builders.nix") (lib.attrNames (removeAttrs table (lib.attrNames actual)))
    ++ lib.concatMap (
      n:
      lib.optional (
        actual ? ${n} && actual.${n} != table.${n}
      ) "${n}: builders.txt has ${show table.${n}}, nixpkgs ${show actual.${n}}"
    ) (lib.attrNames table);
in
runCommand "lint-builders" { } (
  if problems == [ ] then
    "touch $out"
  else
    ''
      echo ${lib.escapeShellArg (lib.concatLines problems)} >&2
      exit 1
    ''
)
