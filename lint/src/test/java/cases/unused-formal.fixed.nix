[
  ({ a, ... }: a)
  # Without `...`, removing `b` would change which arguments are accepted.
  ({ a, b }: a)
  # `args` may pass `b` on.
  ({ a, b, ... }@args: args)
  ({ a, b ? a, ... }: b)
  (
    {
      c,
      ...
    }:
    c
  )
]
