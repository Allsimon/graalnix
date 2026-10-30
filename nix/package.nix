{
  lib,
  stdenv,
  gradle_9,
  graalvmPackages,
}:
let
  jdk = graalvmPackages.graalvm-ce-musl;
  gradle = gradle_9.override { java = jdk; };
in
stdenv.mkDerivation (finalAttrs: {
  pname = "graalnix";
  version = "0.1.0";

  src = lib.fileset.toSource {
    root = ../.;
    fileset = lib.fileset.unions [
      ../settings.gradle.kts
      ../build.gradle.kts
      ../gradle.properties
      ../language/build.gradle.kts
      ../language/src/main
      ../launcher/build.gradle.kts
      ../launcher/src/main
    ];
  };

  nativeBuildInputs = [ gradle ];

  mitmCache = gradle.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./deps.json;
  };

  gradleBuildTask = ":launcher:nativeCompile";
  gradleFlags = [ "-Dorg.gradle.java.home=${jdk}" ];

  installPhase = ''
    runHook preInstall
    install -Dm755 launcher/build/native/nativeCompile/graalnix $out/bin/graalnix
    runHook postInstall
  '';

  meta = {
    description = "Nix language implementation on GraalVM/Truffle";
    mainProgram = "graalnix";
    platforms = lib.platforms.linux;
  };
})
