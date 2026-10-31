{ a, b }:
{
  plain = {
    inherit a;
  };
  # In a `rec`, `b = b;` refers to itself.
  recursive = rec {
    b = b;
  };
}
