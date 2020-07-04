import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class DTRewriter {
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

	public static Set<String> keepCompatibles = new HashSet<>(Arrays.asList(
	/*"uart-1,samsung", "D321AP\0iPhone11,2\0AppleARM", "iop,ascwrap-v2", "iop-nub,rtbuddy-v2", "aic,1",
        "arm-io,t8020", "J421AP\0iPad8,12\0AppleARM", "arm-io,t8027", "usb-drd,t8027", "usb3-phy,t8027",
        "atc-phy,t8027", "iommu-mapper", "dart,t8020", "avd,t8020", "mca-switch,t8027", "pmgr1,t8027",
	"apple,tempest\0ARM,v8", "apple,vortex\0ARM,v8"*/
		"uart-1,samsung", "J421AP\0iPad8,12\0AppleARM", 
		"arm-io,t8027", "aic,1",
		"apple,tempest\0ARM,v8", "apple,vortex\0ARM,v8",
		"apcie,t8027", "usb-drd,t8027"
	));
	public static Set<String> removeNames = new HashSet<>(Arrays.asList(
		"wdt", "backlight", "dockchannel-uart"
	));
	public static Set<String> removeDeviceTypes = new HashSet<>(Arrays.asList(
		"wdt", "backlight"
	));
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
				!keepCompatibles.contains(property.getStringValue())) {
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
			if (property.name.equals("pll-fcal-bypass-code")) {
				w32(property.value, 0x23456789);
			}
			if (property.name.equals("random-seed")) {
				w32(property.value, 0xdeadf00d);
			}
			if (property.name.equals("name")) {
				nodeName = property.getStringValue();
			}
		}
		if (nodeName.equals("cpu0")) {
			findProperty(node.properties, "state").value = "running\u0000".getBytes(StandardCharsets.UTF_8);
		}
		if (nodeName.equals("chosen")) {
			node.properties.add(new DTProperty("security-domain", new byte[]{0x0, 0x0, 0x0, 0x0}));
			node.properties.add(new DTProperty("chip-epoch", new byte[]{0x0, 0x0, 0x0, 0x0}));
		}
		// TODO(zhuowei): can't get the pmgr working
		if (false && nodeName.equals("pmgr")) {
			for (int i = 0; i < 32; i++) {
				node.properties.add(new DTProperty("bridge-settings-" + i, new byte[128]));
			}
			for (int i = 0; i < 16; i++) {
				if (findProperty(node.properties, "voltage-states" + i) != null) continue;
				// setting to all zeroes also seems to work?
				node.properties.add(new DTProperty("voltage-states" + i,
					findProperty(node.properties, "voltage-states0").value));
			}
		}
		// TODO(zhuowei): does this get rid of the _phy error
		if (false && nodeName.equals("usb-drd0")) {
			/*for (int i = node.properties.size() - 1; i >= 0; i--) {
				DTProperty property = node.properties.get(i);
				if (property.name.equals("atc-phy-parent")) {
					node.properties.remove(i);
					break;
				}
			}*/
			findProperty(node.properties, "atc-phy-parent").value[0] = 1;
		}
		if (nodeName.equals("cpu0")) {
			for (int i = node.properties.size() - 1; i >= 0; i--) {
				DTProperty property = node.properties.get(i);
				String name = property.name;
				if (name.startsWith("function-") && !name.startsWith("function-ipi_dispatch")) {
					// not sure how to get AppleARMFunctions to work
					// needed so cpu0 init succeeds, aic init needs cpu
					// the ipi ones are necessary (it checks them)
					// the others seems to be provided by pmgr which i can't get working
					node.properties.remove(i);
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
