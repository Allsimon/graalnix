/**
 * Runs NixOS/nix's language test suite against graalnix.
 *
 * @jenesis.release 25
 * @jenesis.bom pin-graalnix.properties
 * @jenesis.test org.graalnix
 * @jenesis.pin jacoco/module/build.jenesis 0.17.0 SHA-256/d3f83ea608953c2cb1745b75d34b66a0f0a89dc78944f11430f2cfc9bcbe6437
 * @jenesis.pin jacoco/module/build.jenesis.plugin.jacoco 0.1.0 SHA-256/2a3a9560feb3ddfe2a5294cd86a819d57c7d19e40a20c5fa08f2cd276b14d828
 * @jenesis.pin jacoco:agent/maven/org.jacoco/org.jacoco.agent/jar/runtime 0.8.16 SHA-256/37ae2f374ad77e85c59c6e5b46565226c3ac3cb460ac19de0bcb844ac2f43e33
 * @jenesis.pin jacoco:tool/maven/args4j/args4j 2.0.28 SHA-256/11b029a602e787e2bc08eb3b77eda1a4f5e8b263d22e3c5d6220cd5c51f30b18
 * @jenesis.pin jacoco:tool/maven/org.jacoco/org.jacoco.cli 0.8.16 SHA-256/46f01f2816e719c7244505340ad055694a6b06086628af3888c3c35a5ea1a101
 * @jenesis.pin jacoco:tool/maven/org.jacoco/org.jacoco.core 0.8.16 SHA-256/5d2db23c5d698e5645f47f07d1b6019ccd03e15cc933d60d1d4e798ac6bd1f84
 * @jenesis.pin jacoco:tool/maven/org.jacoco/org.jacoco.report 0.8.16 SHA-256/c8e51b392b3689312e72ded4887fdbccb091ceed93207846e14d27f3b82e5f55
 * @jenesis.pin jacoco:tool/maven/org.ow2.asm/asm 9.11.1 SHA-256/ae4e6e10d59d0a1500317764b21881ad883f8550e33c66e6142f7032c6fb566c
 * @jenesis.pin jacoco:tool/maven/org.ow2.asm/asm-commons 9.11.1 SHA-256/6a5095ec53726156a78b2cd2efd45bc4e877754eca7c234f61224a53d6bfa661
 * @jenesis.pin jacoco:tool/maven/org.ow2.asm/asm-tree 9.11.1 SHA-256/2b15c3c1b288a9fad3bb2cd6d988ba9521554914ed1f2a2029141fa3f483154a
 * @jenesis.pin palantir/module/build.jenesis 0.17.0 SHA-256/d3f83ea608953c2cb1745b75d34b66a0f0a89dc78944f11430f2cfc9bcbe6437
 * @jenesis.pin palantir/module/build.jenesis.plugin.palantir 0.1.0 SHA-256/7ed90f505775e2a785967c91f62cb7ce07de44377225cba2f3d4973908836401
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.core/jackson-annotations 2.22 SHA-256/21ddb598807d3a51a876704eb979d9296e1c6a6f47ab1826ff88c6d6a127a2d0
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.core/jackson-core 2.22.4 SHA-256/2d30c375cddea9657a1e1ef97547a2694b663b355facb4de53a097782b312d1a
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.core/jackson-databind 2.22.4 SHA-256/d4cbf8947087f305869826801913abdc50735a1fab2074a1993e7bc67ca974a3
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.datatype/jackson-datatype-guava 2.22.4 SHA-256/844d10482f84dc5a423124a766512a5dfe2ba20672343b64e884daf1afa9ee5a
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.datatype/jackson-datatype-jdk8 2.22.4 SHA-256/2dea0c4f0a5dafaf1bd8b5e694090fe0043c29ba04367dbd38ec0869aca2bce9
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.module/jackson-module-parameter-names 2.22.4 SHA-256/577cab851e0fd0e86efc51e63debf2155a8b7d0b5e177b64a4a12c89ccd4c328
 * @jenesis.pin palantir:tool/maven/com.google.code.findbugs/jsr305 3.0.2 SHA-256/766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7
 * @jenesis.pin palantir:tool/maven/com.google.errorprone/error_prone_annotations 2.50.0 SHA-256/4667724877f1d37a689202da191e23efa7657c62eef93ccdac406eccfe5cdd0a
 * @jenesis.pin palantir:tool/maven/com.google.guava/failureaccess 1.0.3 SHA-256/cbfc3906b19b8f55dd7cfd6dfe0aa4532e834250d7f080bd8d211a3e246b59cb
 * @jenesis.pin palantir:tool/maven/com.google.guava/guava 33.7.2-jre SHA-256/b530942257fb935f8b2cfaa5f8eb5bd59c555fd8e8d01b8ce98912e077ea606c
 * @jenesis.pin palantir:tool/maven/com.google.guava/listenablefuture 9999.0-empty-to-avoid-conflict-with-guava SHA-256/b372a037d4230aa57fbeffdef30fd6123f9c0c2db85d0aced00c91b974f33f99
 * @jenesis.pin palantir:tool/maven/com.google.j2objc/j2objc-annotations 3.1 SHA-256/84d3a150518485f8140ea99b8a985656749629f6433c92b80c75b36aba3b099b
 * @jenesis.pin palantir:tool/maven/com.palantir.javaformat/palantir-java-format 2.103.0 SHA-256/19efbe85c7c51676f5b9b939762baacb3752eb9c39af1a2b38243acc41d1f552
 * @jenesis.pin palantir:tool/maven/com.palantir.javaformat/palantir-java-format-spi 2.103.0 SHA-256/16f8d1c4b9d9d4a9faf93c27254249d65899b30a311493d2469f65ea784636ca
 * @jenesis.pin palantir:tool/maven/org.functionaljava/functionaljava 5.0 SHA-256/377ad140e7d26ba04fadf219b09d7e1c74bc0232fa4010b20c1c79db11f9670e
 * @jenesis.pin palantir:tool/maven/org.jspecify/jspecify 1.0.1 SHA-256/070d75f261fe4c5b8202508366715f7f2d4660f88c8ef7e6d3575e48c9683b66
 */
module org.graalnix.test {
    requires org.graalnix;
    requires org.graalvm.polyglot;
    requires org.junit.jupiter;
    requires org.assertj.core;
}
