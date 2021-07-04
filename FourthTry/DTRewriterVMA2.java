import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class DTRewriterVMA2 {
	public static class DTProperty {
		public String name;
		public byte[] value;
		public DTProperty(String name, byte[] value) {
			this.name = name;
			this.value = value;
		}
		public String getStringValue() {
			if (value.length == 0) return "";
			int endStr = value.length - 1;
			for (; value[endStr] == 0; endStr--) {}
			return new String(value, 0, endStr + 1, StandardCharsets.UTF_8);
		}
	}
	public static class DTNode {
		public List<DTProperty> properties;
		public List<DTNode> children;
	}
	public static DTNode readDeviceTree(ByteBuffer buf) {
		int propertiesSize = buf.getInt();
		int childrenSize = buf.getInt();
		List<DTProperty> properties = new ArrayList<>(propertiesSize);
		List<DTNode> children = new ArrayList<>(childrenSize);
		for (int i = 0; i < propertiesSize; i++) {
			properties.add(readProperty(buf));
		}
		for (int i = 0; i < childrenSize; i++) {
			children.add(readDeviceTree(buf));
		}
		DTNode node = new DTNode();
		node.properties = properties;
		node.children = children;
		return node;
	}

	public static DTProperty readProperty(ByteBuffer buf) {
		byte[] nameBytes = new byte[32];
		buf.get(nameBytes);
		int endStr = 31;
		for (; nameBytes[endStr] == 0; endStr--) {}
		String name = new String(nameBytes, 0, endStr + 1, StandardCharsets.UTF_8);
		int propertyLength = buf.getInt() & 0x7fffffff;
		byte[] value = new byte[propertyLength];
		buf.get(value);
		for (int i = propertyLength; i < ((propertyLength + 0x3) & ~0x3); i++) {
			buf.get();
		}
		return new DTProperty(name, value);
	}

	public static void writeDeviceTree(DTNode node, ByteBuffer buf) {
		buf.putInt(node.properties.size());
		buf.putInt(node.children.size());
		for (DTProperty property: node.properties) {
			writeProperty(property, buf);
		}
		for (DTNode child: node.children) {
			writeDeviceTree(child, buf);
		}
	}
	public static void writeProperty(DTProperty property, ByteBuffer buf) {
		byte[] nameBytes = property.name.getBytes(StandardCharsets.UTF_8);
		buf.put(nameBytes);
		for (int i = nameBytes.length; i < 32; i++) {
			buf.put((byte)0);
		}
		buf.putInt(property.value.length);
		buf.put(property.value);
		for (int i = property.value.length; i < ((property.value.length + 0x3) & ~0x3); i++) {
			buf.put((byte)0);
		}
	}

	/*
	aic needs the CPUs since it routes interrupts to them
	via provider->registerInterrupt
	*/

	public static Set<String> removeCompatibles = new HashSet<>(Arrays.asList(
		"aes,s8000", "qemu,pvpanic-mmio", "paravirtualizedgraphics,iosurface", "paravirtualizedgraphics,gpu"
	));
	public static Set<String> removeNames = new HashSet<>(Arrays.asList(
		"pram", "vram"
	));
	public static Set<String> removeDeviceTypes = new HashSet<>(Arrays.asList(
		"pram", "vram"
	));

	public static Map<String, byte[]> alternativeRegs = makeAlternativeRegs();
	private static final long kPeriphbase = 0x8000000;
	private static Map<String, byte[]> makeAlternativeRegs() {
		Map<String, byte[]> m = new HashMap<>();
		// These are relative to the periphbase at 0x0:
		// https://github.com/qemu/qemu/blob/711c0418c8c1ce3a24346f058b001c4c5a2f0f81/hw/arm/virt.c#L144
		// the size is 0x10000 because the apple one rounds it up for some reason...
		m.put("uart0", i64bytes(0x9000000 - kPeriphbase, 0x10000));
		// https://github.com/matteyeux/darwin-xnu/blob/f96c754925a29fd61ad611fe49c565b8799a4921/pexpert/arm/pe_fiq.c#L100
		// gicd_base, gicd_size, gicr_base, gicr_size
		// https://github.com/qemu/qemu/blob/711c0418c8c1ce3a24346f058b001c4c5a2f0f81/hw/arm/virt.c#L135
		// https://github.com/qemu/qemu/blob/711c0418c8c1ce3a24346f058b001c4c5a2f0f81/hw/arm/virt.c#L143
		m.put("gic", i64bytes(0x8000000 - kPeriphbase, 0x10000, 0x80a0000 - kPeriphbase, 0xf60000));
		m.put("rtc", i64bytes(0x9010000 - kPeriphbase, 0x1000));
		// https://github.com/qemu/qemu/blob/711c0418c8c1ce3a24346f058b001c4c5a2f0f81/hw/arm/virt.c#L147
		// TODO(zhuowei): is there a GPIO-button mapping?
		// TODO(zhuowei): interrupts??
		// Mac has interrupts (8): 0x25
		// QEMU has interrupts = < 0x00 0x07 0x04 >;
		m.put("buttons", i64bytes(0x9030000 - kPeriphbase, 0x1000));
		// TODO(zhuowei): interrupts
		// Mac has interrupts interrupt-base (4): 0x40
		// QEMU has a massive interrupt map...
		// TODO(zhuowei): update ranges
		m.put("pcie", i64bytes(0x10000000 - kPeriphbase, 0x10000000));
		return m;
	}
	public static Map<String, byte[]> alternativeRanges = makeAlternativeRanges();
	private static Map<String, byte[]> makeAlternativeRanges() {
		Map<String, byte[]> m = new HashMap<>();
		// https://github.com/qemu/qemu/blob/711c0418c8c1ce3a24346f058b001c4c5a2f0f81/hw/arm/virt.c#L144
		// all the peripherals are from 0x0 - 0x20000000
		m.put("arm-io", i64bytes(0x0, kPeriphbase, 0x20000000 - kPeriphbase));
		return m;
	}
	private static byte[] i64bytes(Object... args) {
		byte[] bytes = new byte[args.length * 8];
		ByteBuffer b = ByteBuffer.wrap(bytes);
		b.order(ByteOrder.LITTLE_ENDIAN);
		for (Object a: args) {
			b.putLong(((Number)a).longValue());
		}
		return bytes;
	}

	private static void clearProperty(DTProperty property) {
		for (int i = 0; i < property.value.length - 1; i++) {
			property.value[i] = '~';
		}
	}
	private static void w32(byte[] b, int v) {
		b[0] = (byte)(v & 0xff);
		b[1] = (byte)((v >> 8) & 0xff);
		b[2] = (byte)((v >> 16) & 0xff);
		b[3] = (byte)((v >> 24) & 0xff);
	}
	private static DTProperty findProperty(List<DTProperty> properties, String name) {
		for (DTProperty prop: properties) {
			if (prop.name.equals(name)) {
				return prop;
			}
		}
		return null;
	}
	private static byte[] memeset(byte[] input, int val) {
		for (int i = 0; i < input.length; i++) {
			input[i] = (byte)val;
		}
		return input;
	}

	public static void walkTree(DTNode node) {
		String nodeName = "";
		for (int i = node.properties.size() - 1; i >= 0; i--) {
			DTProperty property = node.properties.get(i);
			if (property.name.equals("compatible") &&
				removeCompatibles.contains(property.getStringValue())) {
				clearProperty(property);
			}
			if (property.name.equals("name") &&
				removeNames.contains(property.getStringValue())) {
				property.value[0] = '~';
			}
			if (property.name.equals("device_type") &&
				removeDeviceTypes.contains(property.getStringValue())) {
				property.value[0] = '~';
			}
			if (property.name.equals("secure-root-prefix")) {
				node.properties.remove(i);
			}
			if (property.name.equals("random-seed")) {
				w32(property.value, 0xdeadf00d);
			}
			if (property.name.equals("name")) {
				nodeName = property.getStringValue();
			}
			if (property.name.equals("nvram-total-size")) {
				// macOS 11: needs real NVRAM data in the device tree
				// otherwise crashes when Img4 kext gets nonce-seeds
				w32(property.value, 0x2000);
			}
			// TODO(zhuowei): fix proxy data!
			if (false && property.name.equals("nvram-proxy-data")) {
				try {
					// macOS 11: needs real NVRAM data in the device tree
					// grab NVRAM data from https://gist.github.com/bazad/1faef1a6fe396b820a43170b43e38be1
					property.value = Files.readAllBytes(Paths.get("nvrambin.bin"));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			// enables PE_i_can_has_debugger for macOS 11
			if (property.name.equals("debug-enabled")) {
				property.value[0] = 1;
			}
		}
		if (nodeName.equals("cpu0")) {
			findProperty(node.properties, "state").value = "running\u0000".getBytes(StandardCharsets.UTF_8);
		}
		if (nodeName.equals("chosen")) {
			node.properties.add(new DTProperty("security-domain", new byte[]{0x0, 0x0, 0x0, 0x0}));
			node.properties.add(new DTProperty("chip-epoch", new byte[]{0x0, 0x0, 0x0, 0x0}));
			// macOS 11 needs ram size: for virt this is 0x40000000, 0x180000000 (1GB base, 6GB size)
			node.properties.add(new DTProperty("dram-base", new byte[]{0x0, 0x0, 0x0, 0x40, 0x0, 0x0, 0x0, 0x0}));
			node.properties.add(new DTProperty("dram-size", new byte[]{0x0, 0x0, 0x0, (byte)0x80, 0x1, 0x0, 0x0, 0x0}));
		}
		if (nodeName.equals("asmb")) {
			// macOS 11: for cs_enforcement_disable, which calls csr_check
			// CSR_ALLOW_KERNEL_DEBUGGER (1 << 3)
			node.properties.add(new DTProperty("lp-sip0", new byte[] {0x8, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0}));
		}
		if (alternativeRegs.get(nodeName) != null) {
			for (int i = node.properties.size() - 1; i >= 0; i--) {
				DTProperty property = node.properties.get(i);
				if (property.name.equals("reg")) {
					property.value = alternativeRegs.get(nodeName).clone();
					break;
				}
			}
		}
		if (alternativeRanges.get(nodeName) != null) {
			for (int i = node.properties.size() - 1; i >= 0; i--) {
				DTProperty property = node.properties.get(i);
				if (property.name.equals("ranges")) {
					property.value = alternativeRanges.get(nodeName).clone();
					break;
				}
			}
		}
		for (DTNode n: node.children) {
			walkTree(n);
		}
	}

	public static void main(String[] args) throws Exception {
		byte[] input = Files.readAllBytes(new File(args[0]).toPath());
		ByteBuffer inbuf = ByteBuffer.wrap(input);
		inbuf.order(ByteOrder.LITTLE_ENDIAN);
		DTNode root = readDeviceTree(inbuf);

		walkTree(root);

		ByteBuffer outBuf = ByteBuffer.allocate(input.length + 0x100000);
		outBuf.order(ByteOrder.LITTLE_ENDIAN);
		writeDeviceTree(root, outBuf);
		outBuf.flip();
		byte[] outBytes = new byte[outBuf.limit()];
		outBuf.get(outBytes);
		Files.write(new File(args[1]).toPath(), outBytes);
	}
}
