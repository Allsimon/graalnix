{
  stdenv,
  fetchurl,
  buildPythonPackage,
  fetchPypi,
  lib,
}:
{
  hello = stdenv.mkDerivation rec {
    pname = "hello";
    version = "2.12";
    src = fetchurl {
      url = "mirror://gnu/hello/${pname}-${version}.tar.gz";
      hash = "sha256-AAAA";
    };
    passthru = {
      inherit lib version;
    };
    meta.changelog = "https://example.org/${version}";
  };

  # Other builders give `finalAttrs` what they made of `meta`, so only `pname`, `version` and `src` are rewritten.
  python = buildPythonPackage rec {
    pname = "example";
    version = "1.0";
    src = fetchPypi { inherit pname version; };
    passthru.homepage = meta.homepage;
    meta.homepage = "https://example.org";
  };

  fixable = buildPythonPackage rec {
    pname = "other";
    version = "2.0";
    src = fetchPypi { inherit pname version; };
  };

  # Not fixed: the inner `finalAttrs` would capture `pname` and `version`.
  nested = stdenv.mkDerivation rec {
    pname = "nested";
    version = "1.0";
    modules = stdenv.mkDerivation (finalAttrs: {
      pname = "${pname}-modules";
      inherit version;
    });
  };

  # Left to `extra-rec`: no attribute refers to another.
  plain = stdenv.mkDerivation rec {
    pname = "plain";
  };
}
