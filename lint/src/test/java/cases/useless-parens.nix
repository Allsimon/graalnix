{ f, a }:
{
  value = (f a);
  atom = f (a);
  select = (a.b).c;
  fallback = f (a.b or null);
  call = (f a) a;
  interpolation = "${(f a)}";
  string = f ("x");
  nested = f ((a));
  tight = f(a);
  tighter = f((a));
  path = f(./x);
  operator = (a + a) * 2;
}
