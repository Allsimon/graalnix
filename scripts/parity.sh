#!/usr/bin/env bash
# Compares drvPaths of nixpkgs attributes between nix-instantiate and graalnix.
# usage: scripts/parity.sh NIXPKGS ATTR...
set -uo pipefail
nixpkgs=$1
shift
graalnix=${GRAALNIX:-$(dirname "$0")/../launcher/build/install/graalnix/bin/graalnix}
same=0 differ=0 both=0 nixonly=0 graalonly=0
for attr in "$@"; do
    ref=$(nix-instantiate --readonly-mode -I nixpkgs="$nixpkgs" '<nixpkgs>' -A "$attr" 2>/dev/null | head -1)
    got=$("$graalnix" drv-path -I nixpkgs="$nixpkgs" -E 'import <nixpkgs> {}' -A "$attr" 2>/dev/null | head -1)
    if [[ -z $ref && -z $got ]]; then both=$((both + 1)); echo "both-fail  $attr"
    elif [[ -z $got ]]; then nixonly=$((nixonly + 1)); echo "GRAAL-FAIL $attr"
    elif [[ -z $ref ]]; then graalonly=$((graalonly + 1)); echo "NIX-FAIL   $attr"
    elif [[ $ref == "$got" ]]; then same=$((same + 1)); echo "same       $attr"
    else differ=$((differ + 1)); echo "DIFFER     $attr $ref $got"
    fi
done
echo "identical: $same, different: $differ, failed in both: $both, failed in graalnix only: $nixonly, failed in nix only: $graalonly"
