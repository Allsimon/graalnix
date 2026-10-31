{ x, d }:
[
  (x.a or d)
  (x.y.a.b or (d + 1))
  (if x ? a then x.b else d)
]
