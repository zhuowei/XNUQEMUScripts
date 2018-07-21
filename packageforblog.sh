#!/bin/bash
set -e
rm -r dist xnuqemu_dist.zip || true
mkdir dist
cd dist
cp ../lldbit.sh ../lldbscript.lldb ../runqemu.sh ../README.md ../fixbootdelay_lldbscript_doc.txt ./
cp ../gdbit.sh ../gdbscript.gdb ./
cp ../readdevicetree.py ../modifydevicetree.py ../devicetreefromim4p.py ./
cp ../devicetree.dtb ../kcache_out.bin ../ramdisk.dmg ./
cp ../linux_installgdb.sh ../windows_installgdb.sh ./
7z a ../xnuqemu_dist.zip .
