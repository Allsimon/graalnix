# `or` after a term that is not a selection is the (deprecated) identifier
# `or`, used in application position. nixfmt currently parses and silently
# discards the `or <term>` clause here, losing user code.
let
  or = a: b: a;
in
[
  (fold or
    [ ]
    [
      true
      false
    ]
  )
  (t or a)
]
