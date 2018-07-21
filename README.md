Some scripts for modifying iOS device trees.

See [the tutorial](https://worthdoingbadly.com/xnuqemu2/) for usage instructions.

- modifydevicetree.py: modifies an iOS device tree for QEMU.
- ./runqemu.sh: starts qemu. Assumes kernel at kcache_out.bin, ramdisk at ramdisk.dmg, devicetree at devicetree.dtb, and QEMU at qemu/build-aarch64/qemu-aarch64-softmmu.
- ./lldbit.sh: starts lldb with required breakpoints to boot iOS to userspace.

All scripts are licensed under [CC0](https://creativecommons.org/publicdomain/zero/1.0/) - do whatever you want with them.
