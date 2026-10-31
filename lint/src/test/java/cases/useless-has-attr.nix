{ x, d }:
[
  (if x ? a then x.a else d)
  (if x.y ? a.b then x.y.a.b else d + 1)
  (if x ? a then x.b else d)
]
