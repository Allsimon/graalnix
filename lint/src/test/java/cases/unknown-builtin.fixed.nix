[
  builtins.map
  builtins.mapAttrs'
  (builtins.fetchClosure or null)
  # Tested first, so the file works where Nix has it.
  (builtins.getFlakeAttr or null)
  (
    let
      builtins = { };
    in
    builtins.custom
  )
]
