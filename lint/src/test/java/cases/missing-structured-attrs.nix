{
  stdenv,
  buildGoModule,
  buildPythonPackage,
  pname,
  version,
}:
{
  plain = stdenv.mkDerivation {
    pname = "plain";
    version = "1.0";

    strictDeps = true;
  };

  final = stdenv.mkDerivation (finalAttrs: {
    inherit pname version;
    meta.description = "${finalAttrs.pname} example";
  });

  go = buildGoModule {
    # The comment stays with `src`.
    src = ./.;
    vendorHash = null;
  };

  set = stdenv.mkDerivation {
    pname = "set";
    __structuredAttrs = true;
  };

  # Deliberately off.
  off = stdenv.mkDerivation {
    pname = "off";
    __structuredAttrs = false;
  };

  # Defaults to `true`.
  python = buildPythonPackage {
    inherit pname version;
  };
}
