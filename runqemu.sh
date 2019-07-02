#!/bin/sh
export QEMU_TRUSTCACHE=iOSFiles/trustcache.bin
qemu/build-aarch64/aarch64-softmmu/qemu-system-aarch64 -M virt -cpu max \
	-kernel iOSFiles/kcache_out.bin \
	-dtb iOSFiles/devicetree.dtb \
	-m 3G -d unimp \
	-serial file:/dev/null \
	-serial file:/dev/null \
	-serial mon:stdio \
	-append "debug=0x8 kextlog=0xfff cpus=1 rd=md0 serial=0x2" \
	-initrd iOSFiles/ramdisk.dmg $@
