{ map, lib }:
let
  inherit (lib) toString;
  true = false;
in
rec {
  import = 1;
  x = [
    map
    toString
    true
    import
  ];
}
