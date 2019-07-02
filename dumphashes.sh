#!/bin/bash
# taken from https://alephsecurity.com/2019/06/17/xnu-qemu-arm64-1/
# modified to find literally every executable
basepath="$1"
# https://stackoverflow.com/questions/4458120/unix-find-search-for-executable-files
for filename in $(find "$basepath" -type f -perm +111)
do
	jtool --sig --ent $filename | grep CDHash | cut -d' ' -f6 | cut -c 1-40
done

