{ f }:
let
  used = 1;
  unused = 2;
  self = x: self x;
  a = b;
  b = a;
  _ignored = 3;
  inherit (f) kept dropped;
  inherit (f) gone;
  nested.x = 1;
  nested.y = 2;
  # Only `dead` is reported: the uses of `ver` and `pkg` are inside it too.
  dead = ver: pkg: pkg.${ver};
in
[
  used
  kept
]
