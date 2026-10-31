{
  stdenv,
  fetchurl,
  buildPythonPackage,
  fetchPypi,
  lib,
}:
{
  hello = stdenv.mkDerivation (finalAttrs: {
    pname = "hello";
    version = "2.12";

    __structuredAttrs = true;
    strictDeps = true;
    src = fetchurl {
      url = "mirror://gnu/hello/${finalAttrs.pname}-${finalAttrs.version}.tar.gz";
      hash = "sha256-AAAA";
    };
    passthru = {
      inherit lib; inherit (finalAttrs) version;
    };
    meta.changelog = "https://example.org/${finalAttrs.version}";
  });

  # Other builders give `finalAttrs` what they made of `meta`, so only `pname`, `version` and `src` are rewritten.
  python = buildPythonPackage rec {
    pname = "example";
    version = "1.0";
    src = fetchPypi { inherit pname version; };
    passthru.homepage = meta.homepage;
    meta.homepage = "https://example.org";
  };

  fixable = buildPythonPackage (finalAttrs: {
    pname = "other";
    version = "2.0";
    src = fetchPypi { inherit (finalAttrs) pname version; };
  });

  # Not fixed: the inner `finalAttrs` would capture `pname` and `version`.
  nested = stdenv.mkDerivation (finalAttrs: {
    pname = "nested";
    version = "1.0";

    __structuredAttrs = true;
    strictDeps = true;
    modules = stdenv.mkDerivation (_: {
      pname = "${finalAttrs.pname}-modules";
      inherit (finalAttrs) version;

      __structuredAttrs = true;
      strictDeps = true;
    });
  });

  # Left to `extra-rec`: no attribute refers to another.
  plain = stdenv.mkDerivation {
    pname = "plain";

    __structuredAttrs = true;
    strictDeps = true;
  };
}
