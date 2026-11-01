/**
 * Runs the command line on files in a temporary directory.
 *
 * @jenesis.release 25
 * @jenesis.bom pin-graalnix.properties
 * @jenesis.test org.graalnix.launcher
 * @jenesis.pin palantir/module/build.jenesis 0.18.0 SHA-256/35a94fc0761110ebcd2fa51b2226a3e7e0a942b14cd69d67cc584bf5f3519f70
 * @jenesis.pin palantir/module/build.jenesis.plugin.palantir 0.2.0 SHA-256/f898acd9e03d753cdcdc7e41e0eeb9f21915496e99eb0317aaf443c8df49b73e
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.core/jackson-annotations 2.22 SHA-256/21ddb598807d3a51a876704eb979d9296e1c6a6f47ab1826ff88c6d6a127a2d0
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.core/jackson-annotations/pom 2.22 SHA-256/38c3ba52a0eade3e3eacc5b13499d56e1038ccbad8ec4bb79426dc21432b46d0
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.core/jackson-core 2.22.4 SHA-256/2d30c375cddea9657a1e1ef97547a2694b663b355facb4de53a097782b312d1a
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.core/jackson-core/pom 2.22.4 SHA-256/371b6859b58c3fc1c0823fe66e1e6d6ab251ba99b26f7dce883bbb7c43906c21
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.core/jackson-databind 2.22.4 SHA-256/d4cbf8947087f305869826801913abdc50735a1fab2074a1993e7bc67ca974a3
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.core/jackson-databind/pom 2.22.4 SHA-256/81867c3c99c0e014d133554f2d30cdad78ec58cf1b56087088204e1e5eb34baa
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.datatype/jackson-datatype-guava 2.22.4 SHA-256/844d10482f84dc5a423124a766512a5dfe2ba20672343b64e884daf1afa9ee5a
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.datatype/jackson-datatype-guava/pom 2.22.4 SHA-256/03dabe989241bee5308fcb9be19b309cd631312023f79b3538047c43edf3879f
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.datatype/jackson-datatype-jdk8 2.22.4 SHA-256/2dea0c4f0a5dafaf1bd8b5e694090fe0043c29ba04367dbd38ec0869aca2bce9
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.datatype/jackson-datatype-jdk8/pom 2.22.4 SHA-256/1f5a264adc752da74990cf86b0ff315c644679f0aafc958a5bdb7c5fe94bf027
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.datatype/jackson-datatypes-collections/pom 2.22.4 SHA-256/1ec2fb31c1f0c3e42a8dcfcca810c1df095700c542dea23d3ad6cae26e5795df
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.module/jackson-module-parameter-names 2.22.4 SHA-256/577cab851e0fd0e86efc51e63debf2155a8b7d0b5e177b64a4a12c89ccd4c328
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.module/jackson-module-parameter-names/pom 2.22.4 SHA-256/984478cee7b9e57ece8e017e4313edf2a2cb70fe6c0ba3da536152bf9d4fc796
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson.module/jackson-modules-java8/pom 2.22.4 SHA-256/20cdf333d224a91e6a4b548f8f4515790e34efc19cadc41f06173f51d97ac70d
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson/jackson-base/pom 2.22.4 SHA-256/51b5cc57df78f50a43210e577075f68399e31dde7150455e179d249e637582a1
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson/jackson-bom/pom 2.22.4 SHA-256/a3c00688a7bf8e128451137b68f90d9137957a372756083d74555a47241d1636
 * @jenesis.pin palantir:tool/maven/com.fasterxml.jackson/jackson-parent/pom 2.22 SHA-256/7d45a7efb69df207bed7cc54b87a0bb012f10952c095570a273b49485da0d129
 * @jenesis.pin palantir:tool/maven/com.fasterxml/oss-parent/pom 79 SHA-256/9206b7dd67fb138036576c3dfca947a2a8f3e2c4d2d9c1f2d1dea519b3a4dae1
 * @jenesis.pin palantir:tool/maven/com.google.code.findbugs/jsr305 3.0.2 SHA-256/766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7
 * @jenesis.pin palantir:tool/maven/com.google.code.findbugs/jsr305/pom 3.0.2 SHA-256/19889dbdf1b254b2601a5ee645b8147a974644882297684c798afe5d63d78dfe
 * @jenesis.pin palantir:tool/maven/com.google.errorprone/error_prone_annotations 2.50.0 SHA-256/4667724877f1d37a689202da191e23efa7657c62eef93ccdac406eccfe5cdd0a
 * @jenesis.pin palantir:tool/maven/com.google.errorprone/error_prone_annotations/pom 2.50.0 SHA-256/71a0680d277324bab46bec4b00f6f111c1b794a931d0d8cda79bcb9246982f92
 * @jenesis.pin palantir:tool/maven/com.google.errorprone/error_prone_parent/pom 2.50.0 SHA-256/abf45a019db125b50ff55134ec2af94ae3519761038adf0a60b9bd7b4b8b6254
 * @jenesis.pin palantir:tool/maven/com.google.guava/failureaccess 1.0.3 SHA-256/cbfc3906b19b8f55dd7cfd6dfe0aa4532e834250d7f080bd8d211a3e246b59cb
 * @jenesis.pin palantir:tool/maven/com.google.guava/failureaccess/pom 1.0.3 SHA-256/c54beff37f6d42d43e14722d54a522c9ad51ef97fc5b79277e62a3ebf882f3bb
 * @jenesis.pin palantir:tool/maven/com.google.guava/guava 33.7.2-jre SHA-256/b530942257fb935f8b2cfaa5f8eb5bd59c555fd8e8d01b8ce98912e077ea606c
 * @jenesis.pin palantir:tool/maven/com.google.guava/guava-parent/pom 26.0-android SHA-256/f8698ab46ca996ce889c1afc8ca4f25eb8ac6b034dc898d4583742360016cc04
 * @jenesis.pin palantir:tool/maven/com.google.guava/guava-parent/pom 33.4.0-android SHA-256/7220ede61026596fbc720ee4b93246fa2d14f328058532b59ef053de397c7d83
 * @jenesis.pin palantir:tool/maven/com.google.guava/guava-parent/pom 33.7.2-jre SHA-256/a81570d53c8710355e0216a4405afa93834b17c4cdd0ed9aa8beede3a132bbbe
 * @jenesis.pin palantir:tool/maven/com.google.guava/guava/pom 33.7.2-jre SHA-256/5d1501a97230f90df54ea33164831a61c6714a8b2a4c6a04e90bb729581bbabc
 * @jenesis.pin palantir:tool/maven/com.google.guava/listenablefuture 9999.0-empty-to-avoid-conflict-with-guava SHA-256/b372a037d4230aa57fbeffdef30fd6123f9c0c2db85d0aced00c91b974f33f99
 * @jenesis.pin palantir:tool/maven/com.google.guava/listenablefuture/pom 9999.0-empty-to-avoid-conflict-with-guava SHA-256/18d4b1db26153d4e55079ce1f76bb1fe05cdb862ef9954a88cbcc4ff38b8679b
 * @jenesis.pin palantir:tool/maven/com.google.j2objc/j2objc-annotations 3.1 SHA-256/84d3a150518485f8140ea99b8a985656749629f6433c92b80c75b36aba3b099b
 * @jenesis.pin palantir:tool/maven/com.google.j2objc/j2objc-annotations/pom 3.1 SHA-256/14570838500034fc1b47c8205ce1c9d6b255c1ccdd2d1afdcdcbf75e89b24a33
 * @jenesis.pin palantir:tool/maven/com.palantir.javaformat/palantir-java-format 2.103.0 SHA-256/19efbe85c7c51676f5b9b939762baacb3752eb9c39af1a2b38243acc41d1f552
 * @jenesis.pin palantir:tool/maven/com.palantir.javaformat/palantir-java-format-spi 2.103.0 SHA-256/16f8d1c4b9d9d4a9faf93c27254249d65899b30a311493d2469f65ea784636ca
 * @jenesis.pin palantir:tool/maven/com.palantir.javaformat/palantir-java-format-spi/pom 2.103.0 SHA-256/0a565834636d3660834508e869a84d1bee32402dbb1ec61adbfbee3776c8494c
 * @jenesis.pin palantir:tool/maven/com.palantir.javaformat/palantir-java-format/pom 2.103.0 SHA-256/68dd9cde7aecd71693f6e3cdd4e656e7289cfec79729e08a422085c8a9a4606e
 * @jenesis.pin palantir:tool/maven/org.functionaljava/functionaljava 5.0 SHA-256/377ad140e7d26ba04fadf219b09d7e1c74bc0232fa4010b20c1c79db11f9670e
 * @jenesis.pin palantir:tool/maven/org.functionaljava/functionaljava/pom 5.0 SHA-256/47d380d98553daf742739b510bfdc4948a741877edcb2a5396fc29199689ce09
 * @jenesis.pin palantir:tool/maven/org.jspecify/jspecify 1.0.1 SHA-256/070d75f261fe4c5b8202508366715f7f2d4660f88c8ef7e6d3575e48c9683b66
 * @jenesis.pin palantir:tool/maven/org.jspecify/jspecify/pom 1.0.1 SHA-256/55b38bf1a7b1d8ea518cf2f4f3ae9aaa236e61bbf0b6bd31ffb49753a059dd5e
 * @jenesis.pin palantir:tool/maven/org.junit/junit-bom/pom 5.14.3 SHA-256/08a02856e487c9357f9b29e38745f8ae805848111e72d15aad0352338f1632e1
 * @jenesis.pin palantir:tool/maven/org.sonatype.oss/oss-parent/pom 7 SHA-256/b51f8867c92b6a722499557fc3a1fdea77bdf9ef574722fe90ce436a29559454
 * @jenesis.pin palantir:tool/maven/org.sonatype.oss/oss-parent/pom 9 SHA-256/fb40265f982548212ff82e362e59732b2187ec6f0d80182885c14ef1f982827a
 */
module org.graalnix.launcher.test {
    requires org.graalnix.launcher;
    requires info.picocli;
    requires info.picocli.codegen;
    requires org.junit.jupiter;
    requires org.assertj.core;
}
