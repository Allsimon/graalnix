{ lib, pkgs }:
{
  unused = [ ];
  used = with pkgs; [ hello ];
  # Either may bind `hello`.
  both = with lib; with pkgs; [ hello ];
  shadowed = x: x;
}
