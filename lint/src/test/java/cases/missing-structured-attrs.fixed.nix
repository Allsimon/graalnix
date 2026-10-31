{
  stdenv,
  buildGoModule,
  buildPythonPackage,
  buildHomeAssistantComponent,
  pname,
  version,
}:
{
  plain = stdenv.mkDerivation {
    pname = "plain";
    version = "1.0";

    strictDeps = true;
    __structuredAttrs = true;
  };

  final = stdenv.mkDerivation (finalAttrs: {
    inherit pname version;

    __structuredAttrs = true;
    strictDeps = true;
    meta.description = "${finalAttrs.pname} example";
  });

  go = buildGoModule {
    __structuredAttrs = true;

    # The comment stays with `src`.
    src = ./.;
    vendorHash = null;
  };

  set = stdenv.mkDerivation {
    pname = "set";
    __structuredAttrs = true;
    strictDeps = true;
  };

  # Deliberately off.
  off = stdenv.mkDerivation {
    pname = "off";
    __structuredAttrs = false;
    strictDeps = true;
  };

  # Defaults to `true`.
  python = buildPythonPackage {
    inherit pname version;
  };

  # So does Home Assistant's builder.
  component = buildHomeAssistantComponent {
    inherit pname version;
  };
}
