{ stdenv }:
stdenv.mkDerivation {
  pname = "example";
  version = "1.0";

  installPhase = ''
    mkdir -p $out/bin
    cp example $out/bin/
  '';

  # Only the missing call is added.
  buildPhase = ''
    runHook preBuild
    make
  '';

  checkPhase = ''
    runHook preCheck
    make check
    runHook postCheck
  '';

  # Not fixed: the calls need lines of their own.
  configurePhase = "./configure --prefix=$out";
  fixupPhase = ''true'';
}
