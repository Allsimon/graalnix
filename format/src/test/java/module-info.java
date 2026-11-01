/**
 * Runs nixfmt's test suite against the formatter.
 *
 * @jenesis.release 25
 * @jenesis.bom pin-graalnix.properties
 * @jenesis.bom palantir:tool/pin-graalnix.properties
 * @jenesis.test org.graalnix.format
 * @jenesis.pin palantir/module/build.jenesis 0.18.0 SHA-256/35a94fc0761110ebcd2fa51b2226a3e7e0a942b14cd69d67cc584bf5f3519f70
 * @jenesis.pin palantir/module/build.jenesis.plugin.palantir 0.2.0 SHA-256/f898acd9e03d753cdcdc7e41e0eeb9f21915496e99eb0317aaf443c8df49b73e
 */
module org.graalnix.format.test {
    requires org.graalnix.format;
    requires org.graalnix.parser;
    requires org.junit.jupiter;
    requires org.assertj.core;
}
