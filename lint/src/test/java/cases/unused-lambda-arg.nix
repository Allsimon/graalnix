let
  ignored = x: 1;
  underscore = _x: 1;
  used = x: x;
  # Renaming `y` to `_` would capture the outer `_`.
  capture = _: y: _;
in
[
  ignored
  underscore
  used
  capture
]
