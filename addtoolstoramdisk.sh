#!/bin/bash
set -e
if [ -z "$1" ] || [ -z "$2" ]
then
	echo "Usage: ./addtoolstoramdisk.sh <path/to/048-62250-075.dmg> <path/to/dyld_shared_cache_arm64>"
	exit 1
fi

# Download iOS prebuilt utils

if [ ! -e "iosbinpack.tar" ]
then
	curl -L "https://github.com/jakeajames/rootlessJB/raw/9de6d1213550dab85a0cb4f49322d9cd2fd49595/rootlessJB/bootstrap/tars/iosbinpack.tar" > iosbinpack.tar
fi
rm -r tmpiOSTools || true
mkdir tmpiOSTools || true
cd tmpiOSTools
tar xf ../iosbinpack.tar
cd ..
echo "**iosbinpack extracted**"

# Mount the ramdisk

outfile="iOSFiles/ramdisk.dmg"
python3 extractfilefromim4p.py "$1" "$outfile"
hdiutil resize -size 1.8G -imagekey diskimage-class=CRawDiskImage "$outfile"
mountinfo="$(hdiutil attach -imagekey diskimage-class=CRawDiskImage "$outfile")"
echo "$mountinfo"

function unmountit() {
	hdiutil detach "$mountpath"
}

trap unmountit EXIT

mountpath="$( cut -f 3 <<< "$mountinfo")"
echo "**Mounted at $mountpath**"

# Copy the dyld cache

sudo diskutil enableownership "$mountpath"
sudo mkdir -p "$mountpath/System/Library/Caches/com.apple.dyld/"
sudo cp "$2" "$mountpath/System/Library/Caches/com.apple.dyld/dyld_shared_cache_arm64e"
sudo chown root "$mountpath/System/Library/Caches/com.apple.dyld/dyld_shared_cache_arm64e"
echo "**dyld_shared_cache copied**"

# Copy the utilities

sudo cp -R tmpiOSTools/iosbinpack64 "$mountpath/"
echo "**utilities copied**"

# Replace launch daemons

#sudo rm "$mountpath/System/Library/LaunchDaemons/"*
#sudo cp RamdiskPatches/com.apple.bash.plist "$mountpath/System/Library/LaunchDaemons/"
sudo cp RamdiskPatches/com.apple.restored_update.plist "$mountpath/System/Library/LaunchDaemons/"
sudo chown root:wheel "$mountpath/System/Library/LaunchDaemons/"*
echo "**launchdaemon modified - now generating hashes, please wait**"

# YOLO

# sudo cp -R "$mountpath/iosbinpack64/bin/bash" "$mountpath/sbin/launchd"
sudo cp -R "$mountpath/usr/bin/sed" "$mountpath/iosbinpack64/bin/bash"

# Generate hashes for trustcache

#./dumphashes.sh "$mountpath" >tchashes 2>/dev/null
echo "**generated hashes in tchashes - use Aleph Security's script to convert to binary**"
echo "**done**"
