{
  lib,
  stdenv,
  graalvmPackages,
}:
let
  jdk = graalvmPackages.graalvm-ce-musl;

  sources =
    paths:
    lib.fileset.toSource {
      root = ../.;
      fileset = lib.fileset.unions (
        [
          ../build/jenesis
          ../build.jenesis
          ../jenesis.properties
          ../format/src/main
          ../language/src/main
          ../launcher/src/main
          ../lint/src/main
          ../parser/src/main
          ../store/src/main
        ]
        ++ paths
      );
    };
  testPaths = [
    ../language/src/test
    ../launcher/src/test
    ../format/src/test
    ../lint/src/test
    ../docs/cli
    ../docs/_lint-rules.adoc
  ];
  docPaths = [
    ../docs
    ../jenesis.plugins.pin.properties
  ];
  src = sources [ ];
  testSrc = sources testPaths;

  # Every artifact is pinned by checksum in the module-info.java files, so this download is reproducible.
  deps = stdenv.mkDerivation {
    name = "graalnix-deps";
    src = sources (testPaths ++ docPaths);
    nativeBuildInputs = [ jdk ];
    buildPhase = ''
      runHook preBuild
      export HOME=$TMPDIR MAVEN_REPOSITORY_LOCAL=$out/maven JENESIS_REPOSITORY_LOCAL=$out/modules
      mkdir -p $MAVEN_REPOSITORY_LOCAL $JENESIS_REPOSITORY_LOCAL
      java -Djenesis.make.mode=source -Djenesis.dependency.pin=strict -Djenesis.test.skip=true build/jenesis/Make.java
      runHook postBuild
    '';
    dontInstall = true;
    dontFixup = true;
    outputHashMode = "recursive";
    outputHashAlgo = "sha256";
    outputHash = "sha256-FPEyn5/ZJJZMFENBkvFcOtcsWmMMK674dhYSH4FASN8=";
  };

  offline = ''
    export HOME=$TMPDIR MAVEN_REPOSITORY_LOCAL=$TMPDIR/maven JENESIS_REPOSITORY_LOCAL=$TMPDIR/modules
    cp -r ${deps}/maven $MAVEN_REPOSITORY_LOCAL
    cp -r ${deps}/modules $JENESIS_REPOSITORY_LOCAL
    chmod -R u+w $MAVEN_REPOSITORY_LOCAL $JENESIS_REPOSITORY_LOCAL
  '';

  tests = stdenv.mkDerivation {
    name = "graalnix-tests";
    src = testSrc;
    nativeBuildInputs = [ jdk ];
    buildPhase = ''
      runHook preBuild
      ${offline}
      java -Djenesis.make.mode=source -Djenesis.repository.offline=true -Djenesis.dependency.pin=strict -Djenesis.documentation.asciidoctor=false \
        build/jenesis/Make.java
      runHook postBuild
    '';
    outputs = [
      "out"
      "coverage"
    ];
    installPhase = ''
      runHook preInstall
      touch $out
      cp -r target/build/modules/compose/module/*/produce/assemble/observed/observations/jacoco/report/output/reports/jacoco $coverage
      runHook postInstall
    '';
  };

  docs = stdenv.mkDerivation {
    name = "graalnix-docs";
    src = sources docPaths;
    nativeBuildInputs = [ jdk ];
    buildPhase = ''
      runHook preBuild
      ${offline}
      java -Djenesis.make.mode=source -Djenesis.repository.offline=true -Djenesis.dependency.pin=strict -Djenesis.test.skip=true \
        build/jenesis/Make.java stage
      runHook postBuild
    '';
    installPhase = ''
      runHook preInstall
      cp -r target/stage/project/output/docs $out
      runHook postInstall
    '';
  };
in
stdenv.mkDerivation {
  pname = "graalnix";
  version = "0.1.0";
  inherit src;

  nativeBuildInputs = [ jdk ];

  buildPhase = ''
    runHook preBuild
    ${offline}
    java -Djenesis.make.mode=source -Djenesis.repository.offline=true -Djenesis.dependency.pin=strict -Djenesis.test.skip=true \
      -Djenesis.documentation.asciidoctor=false -Djenesis.make.profiles=native build/jenesis/Make.java stage
    runHook postBuild
  '';

  installPhase = ''
    runHook preInstall
    install -Dm755 target/stage/native/output/org.graalnix.launcher $out/bin/graalnix
    runHook postInstall
  '';

  passthru = {
    inherit
      deps
      tests
      docs
      ;
  };

  meta = {
    description = "Nix language implementation on GraalVM/Truffle";
    mainProgram = "graalnix";
    platforms = lib.platforms.linux;
  };
}
