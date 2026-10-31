{ stdenv, python3 }:
stdenv.mkDerivation {
  pname = "example";
  version = "1.0";

  __structuredAttrs = true;
  strictDeps = true;

  postPatch = ''
    # Don't let this apostrophe open a quote, nor this one: "''${out}".
    substituteInPlace Makefile --replace-fail /usr/bin/env env
    substituteInPlace $(pwd)/config.mk --replace-fail gcc cc
    substituteInPlace setup.py \
      --replace-fail "version='0'" "version='1.0'" \
      --replace-fail "python" "${python3.interpreter}"
    substitute config.in config --replace-fail @out@ $out
    # Quotes and interpolations over several lines are part of the same call.
    substituteInPlace tagtime.pl \
      --replace-fail '$logf = 1;' \
        '$logf = 2;
    $more = 3;' \
      --replace-fail '"__ED__"' '"vi"'
    substituteInPlace latex.py --replace-fail "pdflatex" "${
      python3
    }/bin/pdflatex" \
      --replace-fail "pdf2svg" "x"

    # Not a substitute call.
    patchelf --replace-needed libfoo.so libbar.so example
    ./configure --replace
  '';
}
