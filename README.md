# Collision Lag Fix

Fixes [MC-260743](https://bugs.mojang.com/browse/MC-260743): when an entity moves many blocks in one tick (TNT from a cannon, a launched item, anything flung by a command or another mod), vanilla collects the collision shape of every block in the box spanned by its whole movement before moving it, even though it only ever moves along one axis at a time. For a TNT moving 80 blocks per tick diagonally through terrain that is hundreds of thousands of blocks, every tick.

This mod collects only the blocks along the path each axis actually moves through. The entity ends up in exactly the same place as in vanilla; it just gets there without scanning the rest of the box.

- Server side (or singleplayer). Clients don't need it.
- Works alongside Lithium, which speeds up fast movement through air but still scans the whole box through solid terrain.
- `config/collision_lag_fix.properties`: `enabled`, and `minSpeed` (blocks per tick on at least two axes before the per-axis check is used, default 2).

Tests: a GameTest moves thousands of random entities through a random field of stone, slabs and fences and checks every end position against vanilla's, and another times a TNT fired into solid stone with and without the fix. Both run on 1.21.1, 1.21.11, 26.2 and 26.3 for Fabric and NeoForge, and on Fabric 1.21.1 with Lithium installed.

MIT licensed.
