{
  merged =
    let
      a = 1;
    in
    let
      b = a;
    in
    b;
  # Merged, the inner `b` would replace the argument in `a = b;`.
  capture =
    b:
    let
      a = b;
    in
    let
      b = 2;
    in
    a + b;
}
