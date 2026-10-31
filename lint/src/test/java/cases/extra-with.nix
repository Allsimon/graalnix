{ lib, pkgs }:
{
  unused = with lib; [ ];
  used = with pkgs; [ hello ];
  # Either may bind `hello`.
  both = with lib; with pkgs; [ hello ];
  shadowed = x: with lib; x;
}
