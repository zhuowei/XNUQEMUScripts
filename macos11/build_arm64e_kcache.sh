#!/bin/sh
# Builds an arm64e boot kernel extension collection (the bootable portion of macOS 11's new kernel cache system)
# KextAudit and AppleKeyStore are excluded because they cause kmutil to error
# AppleHSSPIHIDDriver is excluded because it depends on the excluded AppleHSSPISupport 
# The other kexts are excluded as they require ACPI: see printexcludekexts.sh
# Tested on macOS 11.0.1 beta 1 (20B5012D)
exec kmutil create --verbose \
--arch arm64e \
--new boot \
--boot-path ~/kcache_out/bootcache-arm64e \
--kernel /System/Library/Kernels/kernel.release.t8020 \
--strip none \
--system-path /tmp/asdf \
--auxiliary-path /tmp/asdf2 \
--elide-identifier com.apple.private.KextAudit \
--elide-identifier com.apple.driver.AppleKeyStore \
--elide-identifier com.apple.driver.AppleHSSPIHIDDriver \
--elide-identifier com.apple.driver.AppleIntelLpssSpiController \
--elide-identifier com.apple.driver.AppleIntelLpssDmac \
--elide-identifier com.apple.driver.AppleHPET \
--elide-identifier com.apple.iokit.BroadcomBluetoothHostControllerUSBTransport \
--elide-identifier com.apple.iokit.IOBluetoothHostControllerUARTTransport \
--elide-identifier com.apple.driver.AppleSMC \
--elide-identifier com.apple.driver.AppleBusPowerController \
--elide-identifier com.apple.driver.AppleHSSPISupport \
--elide-identifier com.apple.driver.AppleIntelLpssGspi \
--elide-identifier com.apple.driver.AppleACPIButtons \
--elide-identifier com.apple.driver.AppleACPIEC \
--elide-identifier com.apple.driver.AppleACPIPlatform \
--elide-identifier com.apple.driver.AppleSMCRTC \
--elide-identifier com.apple.driver.AppleSmartBatteryManager \
--elide-identifier com.apple.driver.AppleIntelLpssI2C \
--elide-identifier com.apple.driver.AppleRTC \
--elide-identifier com.apple.driver.usb.AppleUSBUHCIPCI \
--elide-identifier com.apple.driver.usb.AppleUSBXHCIPCI \
--elide-identifier com.apple.driver.usb.AppleUSBOHCIPCI \
--elide-identifier com.apple.driver.usb.AppleUSBVHCIBCE \
--elide-identifier com.apple.driver.usb.AppleUSBHub \
--elide-identifier com.apple.driver.usb.AppleUSBXHCI \
--elide-identifier com.apple.driver.usb.AppleUSBEHCIPCI \
--elide-identifier com.apple.driver.usb.AppleUSBEHCI \
--elide-identifier com.apple.iokit.IOUSBHostFamily \
--elide-identifier com.apple.driver.AppleIntelLpssI2CController \
