{
  lib,
  stdenv,
  graalvmPackages,
}:
let
  jdk = graalvmPackages.graalvm-ce-musl;

  src = lib.fileset.toSource {
    root = ../.;
    fileset = lib.fileset.unions [
      ../build/jenesis
      ../language/src/main
      ../launcher/src/main
    ];
  };

  # Every artifact is pinned by checksum in the module-info.java files, so this download is reproducible.
  deps = stdenv.mkDerivation {
    name = "graalnix-deps";
    inherit src;
    nativeBuildInputs = [ jdk ];
    buildPhase = ''
      runHook preBuild
      export HOME=$TMPDIR MAVEN_REPOSITORY_LOCAL=$out/maven
      mkdir -p $MAVEN_REPOSITORY_LOCAL
      java -Djenesis.dependency.pin=strict -Djenesis.test.skip=true build/jenesis/Make.java
      cp -r .jenesis/artifacts $out/artifacts
      runHook postBuild
    '';
    dontInstall = true;
    dontFixup = true;
    outputHashMode = "recursive";
    outputHashAlgo = "sha256";
    outputHash = "sha256-5Vf7Bmq7CzraJy01vhWxb8IOe/kOquO//mA00PlFB84=";
  };
in
stdenv.mkDerivation {
  pname = "graalnix";
  version = "0.1.0";
  inherit src;

  nativeBuildInputs = [ jdk ];

  buildPhase = ''
    runHook preBuild
    export HOME=$TMPDIR MAVEN_REPOSITORY_LOCAL=$TMPDIR/maven
    cp -r ${deps}/maven $MAVEN_REPOSITORY_LOCAL
    mkdir .jenesis
    cp -r ${deps}/artifacts .jenesis/artifacts
    chmod -R u+w $MAVEN_REPOSITORY_LOCAL .jenesis
    java -Djenesis.repository.offline=true -Djenesis.dependency.pin=strict -Djenesis.test.skip=true \
      -Djenesis.make.profiles=native build/jenesis/Make.java stage
    runHook postBuild
  '';

  installPhase = ''
    runHook preInstall
    install -Dm755 target/stage/native/output/org.graalnix.launcher $out/bin/graalnix
    runHook postInstall
  '';

  passthru = { inherit deps; };

  meta = {
    description = "Nix language implementation on GraalVM/Truffle";
    mainProgram = "graalnix";
    platforms = lib.platforms.linux;
  };
}
