#!/bin/sh
qemu/build-aarch64/aarch64-softmmu/qemu-system-aarch64 -M virt -cpu max \
	-kernel kcache_out.bin \
	-dtb devicetree.dtb \
	-monitor stdio \
	-m 2G -s -S -d unimp \
	-serial file:/dev/stdout \
	-serial file:/dev/stdout \
	-serial file:/dev/stdout \
	-append "debug=0x8 kextlog=0xfff cpus=1 rd=md0" \
	-initrd ramdisk.dmg $@
