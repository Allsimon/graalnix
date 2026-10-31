{ lib }:
[
  lib.groupBy
  lib.attrsets.zipAttrsWith
  lib.mapAttrs
  (builtins: lib.groupBy)
]
