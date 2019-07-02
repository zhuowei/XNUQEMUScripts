#!/bin/sh
export QEMU_TRUSTCACHE=iOSFiles/trustcache.bin
qemu/build-aarch64/aarch64-softmmu/qemu-system-aarch64 -M virt -cpu max \
	-kernel iOSFiles/kcache_out.bin \
	-dtb iOSFiles/devicetree.dtb \
	-monitor stdio \
	-m 2G -s -S -d unimp \
	-serial file:/dev/stdout \
	-serial file:/dev/stdout \
	-serial file:/dev/stdout \
	-append "debug=0x8 kextlog=0xfff cpus=1 rd=md0" \
	-initrd iOSFiles/ramdisk.dmg $@
