import sys
from devicetreefromim4p import *

#removeCompatibles = [b"aic,1", b"pmgr1,t8015", b"wdt,t8015\x00wdt,s5l8960x", b"gpio,t8015\x00gpio,s5l8960x", b"sochot,s5l8955x", b"tempsensor,t8015", b"aes,s8000"]
keepCompatibles = [b"uart-1,samsung", b"D22AP\x00iPhone10,3\x00AppleARM"]
removeNames = [b"wdt", b"backlight"]
removeDeviceTypes = [b"wdt", b"backlight"]

# pexpert/pexpert/device_tree.h
def u32(a, i):
	return a[i] | a[i+1] << 8 | a[i+2] << 16 | a[i+3] << 24
def w32(a, i, v):
	a[i] = v & 0xff
	a[i+1] = (v >> 8) & 0xff
	a[i+2] = (v >> 16) & 0xff
	a[i+3] = (v >> 24) & 0xff
def writenode(nodebytes, nodeoffset, nodedepth):
	nProperties = u32(nodebytes, nodeoffset)
	nChildren = u32(nodebytes, nodeoffset + 4)
	ptr = 8
	for p in range(nProperties):
		ptr += writeproperty(nodebytes, nodeoffset + ptr, nodedepth)
	for c in range(nChildren):
		ptr += writenode(nodebytes, nodeoffset + ptr, nodedepth + 1)
	return ptr

def padStringNull(instr, lenstr=32):
	return instr.encode("ascii") + b"\x00"*(lenstr - len(instr))

def writeproperty(nodebytes, nodeoffset, nodedepth):
	kPropNameLength = 32
	propname = nodebytes[nodeoffset:nodeoffset + kPropNameLength].rstrip(b"\x00").decode("utf-8")
	ptr = kPropNameLength
	proplen = u32(nodebytes, nodeoffset + ptr) & 0x7fffffff
	if u32(nodebytes, nodeoffset + ptr) != proplen:
		w32(nodebytes, nodeoffset + ptr, proplen)
	ptr += 4
	if propname == "timebase-frequency" and u32(nodebytes, nodeoffset + ptr) == 0:
		print("setting timebase")
		w32(nodebytes, nodeoffset + ptr, (1000 * 1000 * 1000)//16)
	if propname == "random-seed":
		print("setting random seed")
		w32(nodebytes, nodeoffset + ptr, 0xdeadf00d)
	if propname == "dram-vendor-id":
		print("Removing dram-vendor-id")
		nodebytes[nodeoffset:nodeoffset + kPropNameLength] = padStringNull("chip-epoch")
		nodebytes[nodeoffset + ptr:nodeoffset + ptr + proplen] = b"\x00" * proplen
	if propname == "display-corner-radius":
		print("Removing display-corner-radius")
		nodebytes[nodeoffset:nodeoffset + kPropNameLength] = padStringNull("security-domain")
		nodebytes[nodeoffset + ptr:nodeoffset + ptr + proplen] = b"\x00" * proplen
	if propname == "compatible" and not nodebytes[nodeoffset+ptr:nodeoffset+ptr+proplen-1] in keepCompatibles:
		print("removing compatible for", nodebytes[nodeoffset+ptr:nodeoffset+ptr+proplen-1].decode("ascii"))
		nodebytes[nodeoffset+ptr:nodeoffset + ptr + proplen - 1] = b"~" * (proplen - 1)
	if propname == "name" and nodebytes[nodeoffset+ptr:nodeoffset+ptr+proplen-1] in removeNames:
		print("removing name for", nodebytes[nodeoffset+ptr:nodeoffset+ptr+proplen-1].decode("ascii"))
		nodebytes[nodeoffset+ptr] = ord("~")
	if propname == "device_type" and nodebytes[nodeoffset+ptr:nodeoffset+ptr+proplen-1] in removeDeviceTypes:
		print("removing device type for", nodebytes[nodeoffset+ptr:nodeoffset+ptr+proplen-1].decode("ascii"))
		nodebytes[nodeoffset+ptr] = ord("~")
	ptr += proplen
	ptr = (ptr + 0x3) & ~0x3 #round up to nearest 4
	return ptr

def printone(filename, outname):
	with open(filename, "rb") as infile:
		indata = infile.read()
	devicetreebytes = bytearray(devicetreefromim4p(indata))
	size = writenode(devicetreebytes, 0, 0)
	with open(outname, "wb") as outfile:
		outfile.write(devicetreebytes[:size])

if __name__ == "__main__":
	printone(sys.argv[1], sys.argv[2])
