{ a, b }:
{
  plain = {
    a = a;
  };
  # In a `rec`, `b = b;` refers to itself.
  recursive = rec {
    b = b;
  };
}
