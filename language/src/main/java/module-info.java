/**
 * The Nix language, implemented as a Truffle language.
 *
 * @jenesis.release 25
 * @jenesis.plugin maven/org.graalvm.truffle/truffle-dsl-processor
 * @jenesis.pin org.graalvm.polyglot/polyglot 25.4.4.1.1 SHA-256/cbacc2628fd5f99dbcebff7d398bcddde43ea15ebadc873d1da4454c718727d8
 * @jenesis.pin org.graalvm.sdk/collections 25.4.4.1.1 SHA-256/1b72cb9c9db533991bc308f2026a6fe986a4ca44b70b68fa6a2bb6c2fb9887ea
 * @jenesis.pin org.graalvm.sdk/jniutils 25.4.4.1.1 SHA-256/a9d7e6830e5068dcd6b780ccf2903c075da04b1b2f1fa6c8197b4e9b06d4c075
 * @jenesis.pin org.graalvm.sdk/nativeimage 25.4.4.1.1 SHA-256/d633cb9895f01db443dbd2b6e54211cd505e679279f6745a84b1fee843156677
 * @jenesis.pin org.graalvm.sdk/word 25.4.4.1.1 SHA-256/f20b7adf174a8c61018ce4e717d9dda65fec27d00540e547e63f1bafa400ac9a
 * @jenesis.pin org.graalvm.truffle/truffle-api 25.4.4.1.1 SHA-256/9dcc81b6890d6fcc9ab57d17bc4430335f071d82b108c0af8556a28381978da6
 * @jenesis.pin plugin/maven/org.graalvm.truffle/truffle-dsl-processor 25.4.4.1.1 SHA-256/f6fdebcb85da6587d764ffcb0f859a84f5ae1b53866b96012d9e822412cf1518
 */
module org.graalnix {
    requires org.graalvm.truffle;
    requires org.graalvm.polyglot;

    provides com.oracle.truffle.api.provider.TruffleLanguageProvider with org.graalnix.NixLanguageProvider;
}
