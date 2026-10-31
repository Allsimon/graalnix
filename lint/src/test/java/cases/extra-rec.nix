{
  plain = rec {
    a = 1;
    b = 2;
  };
  used = rec {
    a = 1;
    b = a;
  };
}
