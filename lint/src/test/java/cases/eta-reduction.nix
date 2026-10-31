{ f, lib }:
[
  (x: f x)
  (x: lib.length x)
  (x: f x x)
  # Partial applications are kept.
  (x: lib.map f x)
  (map (x: f x) [ ])
]
