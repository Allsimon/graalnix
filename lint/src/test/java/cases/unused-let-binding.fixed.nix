{ f }:
let
  used = 1;
  _ignored = 3;
  inherit (f) kept;
  # Only `dead` is reported: the uses of `ver` and `pkg` are inside it too.
in
[
  used
  kept
]
