{ lib }:
[
  builtins.groupBy
  builtins.zipAttrsWith
  lib.mapAttrs
  (_: builtins.groupBy)
]
