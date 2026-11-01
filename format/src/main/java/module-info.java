/**
 * A formatter for the Nix language, in the style of nixfmt (RFC 166).
 *
 * <p>It ports nixfmt 1.5.0 (https://github.com/NixOS/nixfmt, MPL-2.0) on top of graalnix's parser.
 *
 * @jenesis.release 21
 * @jenesis.bom pin-graalnix.properties
 * @jenesis.bom palantir:tool/pin-graalnix.properties
 * @jenesis.pin palantir/module/build.jenesis 0.18.0 SHA-256/35a94fc0761110ebcd2fa51b2226a3e7e0a942b14cd69d67cc584bf5f3519f70
 * @jenesis.pin palantir/module/build.jenesis.plugin.palantir 0.2.0 SHA-256/f898acd9e03d753cdcdc7e41e0eeb9f21915496e99eb0317aaf443c8df49b73e
 */
module org.graalnix.format {
    requires org.graalnix.parser;

    exports org.graalnix.format;
}
