/**
 *  Broadlink RM/RM Pro/RM Mini/SP driver for Hubitat
 *      by CybrMage
 */

def version() {return "v0.46  2020-05-01"}

import groovy.transform.Field
import groovy.json.JsonSlurper
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import java.util.Date

@Field static volatile byte[] AES_KEY = []
@Field static volatile byte[] INIT_VECTOR = []
@Field static volatile long LEARN = 0

preferences {
	input ("destIp", "text", title: "IP", description: "The IP Address of the Broadlink device",required:true)
	
	input ("codeRepeats", "enum", title: "Select number of code repeats", required: false, multiple: false, options: ["0", "1", "2", "3", "4", "5"], defaultValue: "0", submitOnChange: false)

	input ("logDebug", "bool", title: "Enable debug logging", defaultValue: false)
	input ("verboseDebug", "bool", title: "Enable verbose debug logging", defaultValue: false)
	input ("eventLog", "bool", title: "Enable event logging", defaultValue: false)
}

metadata {
	definition (name: "Broadlink (BETA)", namespace: "cybr", author: "CybrMage") {
		capability "Actuator"
		capability "Initialize"
		capability "Switch"
		capability "TemperatureMeasurement"

		attribute "deviceConfig", "object"
		attribute "IR_Status", "string"
		attribute "RF_Status", "string"
		
		command "getStatus"
		command "reset"
		
		command "SendCode", [[name: "Code*", type:"STRING", description: "Enter Broadlink Code Data"]]
		command "SendProntoCode", [[name: "Code*", type:"STRING", description: "Enter Pronto Code Data"]]
		
		command "learnIR"
		command "learnRF"

		command "StoreCode", [[name: "Name*", type:"STRING", description: "Enter Name for stored code data"]]
		command "SendStoredCode", [[name: "Name*", type:"STRING", description: "Enter Name for stored code data to send"]]
		command "SendLastCode"

		command "push", [[name: "Name*", type:"STRING", description: "Enter Name for stored code button"]]

		command "generateIR", [
			[name: "offBurst*", type:"STRING", description: "Burst pair for ZERO bit"],
			[name: "onBurst*", type:"STRING", description: "Burst pair for ONE bit"],
			[name: "leadIn*", type:"STRING", description: "Burst pairs for leadIn data"],
			[name: "bitmap*", type:"STRING", description: "Bitmap of data to send"],
			[name: "leadOut*", type:"STRING", description: "Burst pairs for leadOut data"]
		]
	}
}

// AES CBC methods
def AES_setKey(String newKey) {
	// convert hex encoded string to string
	if ((newKey == null) || (newKey == "")) {newKey = "097628343fe99e23765c1513accf8b02"}
	AES_KEY = hubitat.helper.HexUtils.hexStringToByteArray(newKey)
}

def AES_setIV(String newIV) {
	// convert hex encoded string to string
	if ((newIV == null) || (newIV == "")) { newIV = "562e17996d093d28ddb3ba695a2e6f58" }
	INIT_VECTOR = hubitat.helper.HexUtils.hexStringToByteArray(newIV)
}

def AES_getKey() {
	// convert byte array to hex encoded string
	return hubitat.helper.HexUtils.byteArrayToHexString(AES_KEY)
}

def AES_getIV() {
	// convert string to hex encoded string
	return hubitat.helper.HexUtils.byteArrayToHexString(INIT_VECTOR)
}

def byte [] AES_Encrypt(value) {
    try {
        IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR)
        SecretKeySpec skeySpec = new SecretKeySpec(AES_KEY, "AES")

		Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv)
		if (value instanceof List) { value = arrayListToByteArray(value) }
		if (value instanceof String) { value = value.getBytes() }
		log_info("AES_Encrypt(\"${hubitat.helper.HexUtils.byteArrayToHexString(value)}\")")
        byte[] encrypted = cipher.doFinal(value)
		log_info("  ENCRYPTED(\"${hubitat.helper.HexUtils.byteArrayToHexString(encrypted)}\")")
        return encrypted
//        return encrypted.encodeBase64()
    } catch (e) {
		log_debug("AES_Encrypt: Caught Execption [${e}]")
    }
    return null;
}

def byte [] AES_Decrypt(encrypted) {
    try {
        IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR)
        SecretKeySpec skeySpec = new SecretKeySpec(AES_KEY, "AES")
 
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv)
		if (encrypted instanceof List) { encrypted = arrayListToByteArray(encrypted) }
		if (encrypted instanceof String) { encrypted = encrypted.getBytes() }
		log_info("AES_Decrypt(\"${hubitat.helper.HexUtils.byteArrayToHexString(encrypted)}\")")
        byte[] original = cipher.doFinal(encrypted)
		log_info("  DECRYPTED(\"${hubitat.helper.HexUtils.byteArrayToHexString(original)}\")")
        return original
    } catch (e) {
		log_debug("AES_Decrypt: Caught Execption [${e}]")
    }
 
    return null
}

// driver framework functions
def initialize() {
	unschedule()
	if (logDebug == null) {	logDebug =  true; device.updateSetting("logDebug",logDebug)	}
	if (verboseDebug == null) {	verboseDebug = false; device.updateSetting("verboseDebug",verboseDebug)	}
	if (eventLog == null) {	eventLog = false; device.updateSetting("eventLog",eventLog)	}
	if (codeRepeats == null) { codeRepeats = 0; device.updateSetting("codeRepeats",codeRepeats); state.codeRepeats = 0 } else { device.updateSetting("codeRepeats",codeRepeats.toInteger()); state.codeRepeats=codeRepeats.toInteger() }
	// set the initial Broadlink key and IV
	AES_setKey("097628343fe99e23765c1513accf8b02")
	AES_setIV("562e17996d093d28ddb3ba695a2e6f58")
	log_debug("(initialize ${version()}) DEBUG [${logDebug}]  VERBOSE [${verboseDebug}]  KEY [${hubitat.helper.HexUtils.byteArrayToHexString(AES_KEY)}]  IV [${hubitat.helper.HexUtils.byteArrayToHexString(INIT_VECTOR)}]  codeRepeats [${codeRepeats}]")
	
	// If the IP address is not set, there is nothing to do...
	if ((destIp == null)||(destIp == "")) {
		log_error("(initialize) ERROR - Device IP Address not set")
		return
	}

	if ( !getParent() ) {
		// this is only needed by a standalone driver
		// set the deviceNetworkIdentifier. Ideally, this should be the MAC address of the device
		// If the MAC can not be determined, use the IP address
		def deviceMAC = null
		def macTries = 0
		while ((macTries < 5) && (deviceMAC == null)) {
			macTries++
			deviceMAC = getMACFromIP(destIp)
			if (deviceMAC == null) {
				log_info("(initialize) Device MAC address not yet available. Retry in 1 second.")
				pauseExecution(1000)
			}
		}
		if (deviceMAC != null) {
			device.deviceNetworkId = "$deviceMAC"
		} else {
			device.deviceNetworkId = convertIPtoHex(destIp)
		}
		log_info("(initialize) Set DNI = [${device.deviceNetworkId}]")
	}
	
	state.packetCount = 0
	state.deviceConfig = null
	def cData = device.latestValue("deviceConfig")
	if ((cData == null) ||(cData == "") ||(cData == "[]")) {} else {state.deviceConfig = new JsonSlurper().parseText(cData)}
	if (state.deviceConfig == null) {
		// the device has not been "discovered" (has been manually added)
		log_error("(initialize) ERROR - Device has not been configured")
		getDeviceConfig()
	} else {
		log.debug("(initialize) CONFIGURED - deviceConfig [${state.deviceConfig}]")
		if (state.deviceConfig.hasAuth != true) { 
			getAuth(state.deviceConfig) 
		}
		send_Event(name: "IR_Status", value: "IDLE", displayed:false, isStateChange: true)
		send_Event(name: "RF_Status", value: "IDLE", displayed:false, isStateChange: true)
		getStatus()
		if (state.deviceConfig.hasAuth && ((state.deviceConfig.hasTemp == true) || (state.deviceConfig.relayCount != 0))) {
			runEvery5Minutes("getStatus")
		}
	}
	return null
}

def updated() {
	log_debug("(updated) DEBUG [${logDebug}]  VERBOSE [${verboseDebug}]  codeRepeats [${codeRepeats}]")
	device.updateSetting("logDebug",logDebug)
	device.updateSetting("verboseDebug",verboseDebug)
	initialize()
}

def installed() {
	log_debug("(installed)")
}

def reset() {
	log_debug("(reset) DEBUG [${logDebug}]  VERBOSE [${verboseDebug}] - Reseting device configuration")
	state.deviceConfig = null
	send_Event(name: "deviceConfig", value: [], displayed:false, isStateChange: true)
}

def send_Event(evnt) {
	if ((eventLog == true) || (evnt.name == "IR_Status") || (evnt.name == "RF_Status")) {log.trace("${evnt.name}: ${evnt.value}")}
	sendEvent(evnt)
}

def log_debug(debugData) {
	if (logDebug) log.debug("${device.name} - " + debugData)
}

def log_warn(debugData) {
	if (logDebug) log.warn("${device.name} - " + debugData)
}

def log_info(debugData) {
	if (verboseDebug) log_debug(debugData)
}

private log_error(debugData) {
	log.error("${device.name} - " + debugData)
}

private String convertIPtoHex(ipAddress) {
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
	return hexport
}


// Broadlink specific functions
def on() {
	setTarget(state.deviceConfig, true)
}

def off() {
	setTarget(state.deviceConfig, false)
}

def getStatus() {
	if (state.deviceConfig == null) {log_debug("(getStatus) ERROR: Device not configured"); return true }
	log_debug("(getStatus)  deviceConfig [${state.deviceConfig}]  AUTH [${state.deviceConfig.hasAuth}]")
	if (state.deviceConfig == null) {
//		state.deviceConfig = getDeviceConfig()
	}
	if ((state.deviceConfig.relayCount == 0) && (state.deviceConfig.hasTemp == false)) { return null }
	
	if (state.deviceConfig.hasTemp) {
		check_sensors()
	}
	
	if (state.deviceConfig.relayCount == 0) {
		return null
	}
	
	byte [] payload = [0x00, 0x00, 0x00, 0x00,0x00, 0x00, 0x00, 0x00,0x00, 0x00, 0x00, 0x00,0x00, 0x00, 0x00, 0x00]
	if (state.deviceConfig.relayCount == 4) {
		payload[0] = (byte) 0x0a
		payload[2] = (byte) 0xa5
		payload[3] = (byte) 0xa5
		payload[4] = (byte) 0x5a
		payload[5] = (byte) 0x5a
		payload[6] = (byte) 0xae
		payload[7] = (byte) 0xc0
		payload[8] = (byte) 0x01
	} else {
		payload[0] = (byte) 0x01
	}
	def err = send_packet(state.deviceConfig, 0x6a, payload, "parseStatusData")
	return null
}

def check_sensors() {
	log_debug("(check_sensors)  deviceConfig [${state.deviceConfig}]  AUTH [${state.deviceConfig.hasAuth}]")
	if (state.deviceConfig.hasTemp == false) { return true }
	byte [] payload = [0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	def err = send_packet(state.deviceConfig, 0x6a, payload, "parseSensorData")
}

def getDeviceConfig() {
	state.deviceConfig = []
	getDiscoveryData(destIp)
}

def getString(str) {
	def rStr = ""
	def gotNull = false
	str.eachWithIndex { it, i -> 
		if ((it < 1) || (it > 0x7F)) { gotNull = true }
		if (gotNull == false) { rStr = rStr + new String(it) }
	}
//	log_debug("getString() = \"${rStr}\"")
	return rStr
}

private byte [] arrayListToByteArray(aList) {
	String hex = aList.collect {  String.format( '%02X', (it.toInteger() < 0) ? (it.toInteger() + 256) : it.toInteger()) }.join()
	byte [] bArray = hubitat.helper.HexUtils.hexStringToByteArray(hex) 
	return bArray
}

private String arrayListToHexString(aList) {
	String hex = aList.collect {  String.format( '%02X', (it.toInteger() < 0) ? (it.toInteger() + 256) : it.toInteger()) }.join()
	return hex
}

private String decodeHexIP(IP) {
	def dotted = hubitat.helper.HexUtils.hexStringToByteArray(IP).collect {  String.format( '%s', (it.toInteger() < 0) ? (it.toInteger() + 256) : it.toInteger()) }.join(".")
	return dotted
}

def getDeviceTypeInfo(devtype) {
	log_debug("(getDeviceTypeInfo) devtype = (${devtype})")
	def supported = false
	def isPro = false
	def hasTemp = false
	def hasIR = false
	def isRM4 = false
	def relayCount = 0
	def devTypeName = ""
	if (devtype == 0) { devTypeName = "SP1"; hasIR = false; isPro = false; hasTemp = false; relayCount = -1 }
	else if (devtype == 0x13b) { devTypeName = "RM315"; hasIR = false; isPro = true;  hasTemp = false}
	else if (devtype == 0x1b1) { devTypeName = "RM433"; hasIR = false; isPro = true;  hasTemp = false}
	else if (devtype == 0x26) { devTypeName = "RMIR"; hasIR = true; isPro = false; hasTemp = false}
	else if (devtype == 0x2710) { devTypeName = "RM1"; hasIR = true; isPro = false; hasTemp = true}
	else if (devtype == 0x2711) { devTypeName = "SP2"; hasIR = false; isPro = false; hasTemp = false}
	else if (devtype == 0x2712) { devTypeName = "RM2"; hasIR = true; isPro = false; hasTemp = false}
	else if (devtype == 0x2714) { devTypeName = "A1"; hasIR = false; isPro = false; hasTemp = false}
	else if ((devtype == 0x2719) || (devtype == 0x271a)) { devTypeName = "Honeywell SP2"; hasIR =false; isPro = false; hasTemp = false; relayCount = 1}
	else if (devtype == 0x271f) { devTypeName = "RM2 Home Plus"; hasIR = true; isPro = false; hasTemp = true}
	else if (devtype == 0x2720) { devTypeName = "SPMini"; hasIR = false; isPro = false; hasTemp = false; relayCount = 1}
	else if (devtype == 0x2728) { devTypeName = "SPMini2"; hasIR = false; isPro = false; hasTemp = false; relayCount = 1}
	else if (devtype == 0x272a) { devTypeName = "RM2 Pro Plus"; hasIR = true; isPro = true; hasTemp = true}
	else if ((devtype == 0x2733) || (devtype == 0x273e)) { devTypeName = "OEM branded SPMini"; hasIR = false; isPro = false; hasTemp = false; relayCount = 1}
	else if (devtype == 0x2736) { devTypeName = "SPMiniPlus"; hasIR = false; isPro = false; hasTemp = false; relayCount = 1}
	else if (devtype == 0x2737) { devTypeName = "RM Mini"; hasIR = true; isPro = false; hasTemp = false}
	else if (devtype == 0x273d) { devTypeName = "RM Pro Phicomm"; hasIR = true; isPro = true; hasTemp = true}
	else if (devtype == 0x277c) { devTypeName = "RM2 Home Plus GDT"; hasIR = true; isPro = false; hasTemp = true}
	else if (devtype == 0x2783) { devTypeName = "RM2 Home Plus"; hasIR = true; isPro = false; hasTemp = true}
	else if (devtype == 0x2787) { devTypeName = "RM2 Pro Plus2"; hasIR = true; isPro = true; hasTemp = true}
	else if (devtype == 0x278b) { devTypeName = "RM2 Pro Plus BL"; hasIR = true; isPro = true; hasTemp = true}
	else if (devtype == 0x278f) { devTypeName = "RM Mini Shate"; hasIR = true; isPro = false;  hasTemp = false}
	else if (devtype == 0x2797) { devTypeName = "RM2 Pro Plus HYC"; hasIR = true; isPro = true; hasTemp = true}
	else if (devtype == 0x279d) { devTypeName = "RM3 Pro Plus"; hasIR = true; isPro = true; hasTemp = false}
	else if (devtype == 0x27a1) { devTypeName = "RM2 Pro Plus R1"; hasIR = true; isPro = true; hasTemp = false}
	else if (devtype == 0x27a2) { devTypeName = "RM Mini R2"; hasIR = true; isPro = false; hasTemp = false}
	else if (devtype == 0x27a6) { devTypeName = "RM2 Pro PP"; hasIR = true; isPro = true; hasTemp = true}
	else if (devtype == 0x27a9) { devTypeName = "RM2 Pro Plus 300 / RM3 Pro Plus v2 model 3422"; hasIR = true; isPro = true; hasTemp = false}
	else if (devtype == 0x27c2) { devTypeName = "RM Mini 3 B"; hasIR = true; isPro = false; hasTemp = false}
	else if (devtype == 0x27c7) { devTypeName = "RM Mini 3 A"; hasIR = true; isPro = false; hasTemp = false}
	else if (devtype == 0x27de) { devTypeName = "RM Mini 3 C"; hasIR = true; isPro = false; hasTemp = false}
	
	else if (devtype == 0x51da) { devTypeName = "RM4b"; hasIR = true; isPro = false; hasTemp = false; isRM4 = true}
	else if (devtype == 0x5f36) { devTypeName = "RM Mini 3 (V4)"; hasIR = true; isPro = false; hasTemp = false; isRM4 = true}
	else if (devtype == 0x610f) { devTypeName = "RM4c (RM Mini 4 vA)"; hasIR = true; isPro = false; hasTemp = false; isRM4 = true}
	else if (devtype == 0x62be) { devTypeName = "RM4c (RM Mini 4 vB)"; hasIR = true; isPro = false; hasTemp = false; isRM4 = true}

	else if (devtype == 0x4EB5) { devTypeName = "MP1"; hasIR = false; isPro = false; hasTemp = false; relayCount = 4}
	else if (devtype == 0x5f36) { devTypeName = "RM Mini 3 D"; hasIR = true; isPro = false; hasTemp = false}
	else if ((devtype == 0x7530) || (devtype == 0x7918)) { devTypeName = "OEM branded SPMini2"; hasIR = false; isPro = false; hasTemp = false; relayCount = 1}
	else if (devtype == 0x753e) { devTypeName = "SP3"; hasIR = false; isPro = false; hasTemp = false; relayCount = 1}
	else if ((devtype > 0x752F) && (devtype < 0x7919)) { devTypeName = "OEM branded SPMini2"; hasIR = false; isPro = false; hasTemp = false; relayCount = 1}
	else if ((devtype == 0x7919) || (devtype == 0x791a)) { devTypeName = "Honeywell SP2"; hasIR = false; isPro = false; hasTemp = false; relayCount = 1}
	if (devTypeName == "") { devTypeName = "unknown (0x"+String.format("%04x",devtype)+")" } else { supported = true }
	log_debug("(getDeviceTypeInfo) return [devTypeName: ${devTypeName}, isPro: ${isPro}, hasIr: ${hasIR}, hasTemp: ${hasTemp}, relayCount: ${relayCount}]")
	return [devTypeName: devTypeName, isPro: isPro, hasIR: hasIR, hasTemp: hasTemp, relayCount: relayCount, isRM4: isRM4, supported: supported]
}

def getAuth(deviceConfig) {
	log_debug("(getAuth) ip [${deviceConfig.IP}]")
	byte [] payload = [0x00, 0x00, 0x00, 0x00, 0x31, 0x31, 0x31, 0x31,
					   0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31,
					   0x31, 0x31, 0x31, 0x01, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
					   ]
	payload[0x1e] = 0x01;
	payload[0x2d] = 0x01;
	payload[0x30] = (byte) 'H';
	payload[0x31] = (byte) 'u';
	payload[0x32] = (byte) 'b';
	payload[0x33] = (byte) 'i';
	payload[0x34] = (byte) 't';
	payload[0x35] = (byte) 'a';
	payload[0x36] = (byte) 't';
	err = send_packet(deviceConfig, 0x65, payload, "parseAuthData")
	if (err) {
		return [true, "No response"]
	}
	
}

def setTarget(deviceConfig, on) {
	if (deviceConfig == null) {log_debug("(setTarget) ERROR: Device not configured"); return true }
	log_debug("(setTarget) ip [${deviceConfig.IP}]  target [${on}]")
	if ((deviceConfig.relayCount == null) || (deviceConfig.hasIR == null) || (deviceConfig.isPro == null)) {
		def tInfo = getDeviceTypeInfo(deviceConfig.devType)
		deviceConfig.isPro = tInfo.isPro
		deviceConfig.hasIR = tInfo.hasIR
		deviceConfig.hasTemp = tInfo.hasTemp
		deviceConfig.relayCount = tInfo.relayCount
		deviceConfig.isRM4 = tInfo.isRM4
		deviceConfig.supported = tInfo.supported
	}
	TargetCMD = 0x6a
	if ((deviceConfig.relayCount == 0) || (deviceConfig.isPro == true) || (deviceConfig.hasIR == true)) { 
		log_debug("(setTarget) ERROR - Device does not support this function")
		return nil 
	}
	byte [] payload = []
	if (deviceConfig.relayCount == -1) {
		TargetCMD = 0x66
		payload = [0x00, 0x00, 0x00, 0x00]
		payload[0] = on ? 1 : 0
	} else if (deviceConfig.relayCount == 1) {
		TargetCMD = 0x6a
		payload = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
		payload[0] = 2
		payload[4] = on ? 1 : 0
	}
	send_packet(deviceConfig, TargetCMD, payload,"parseTargetResponse")
	return null
}

def getChecksum(packet) {
	def checksum = 0xbeaf
	for (i = 0; i < packet.size(); i++) { 
		checksum = checksum + Byte.toUnsignedInt(packet[i])
		checksum = checksum  & 0xFFFF
	}
	return checksum
}

def send_packet(deviceConfig, COMMAND, payload, callback = "parse") {
	log_debug("(send_packet) Called send_packet( ${deviceConfig.IP}, 0x${String.format("%02X",COMMAND)})")
	if (deviceConfig.cloudLocked) { log.warn("(send_packet) Device Configuration indicates target device is CLOUD LOCKED. Local API access will not function!") }
	state.packetCount = (state.packetCount + 1) & 0xffff
	
	byte [] packet = [ 0x5a, 0xa5, 0xaa, 0x55, 0x5a, 0xa5, 0xaa, 0x55,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x2a, 0x27, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
					  ]
	packet[0x24] = (byte) (deviceConfig.devType & 0xff)
	packet[0x25] = (byte) (deviceConfig.devType >> 8)

	packet[0x26] = (byte) COMMAND
	packet[0x28] = (byte) (state.packetCount & 0xff)
	packet[0x29] = (byte) (state.packetCount >> 8)
	packet[0x2a] = (byte) hubitat.helper.HexUtils.hexStringToInt(deviceConfig.MAC[10,11])
	packet[0x2b] = (byte) hubitat.helper.HexUtils.hexStringToInt(deviceConfig.MAC[8..9])
	packet[0x2c] = (byte) hubitat.helper.HexUtils.hexStringToInt(deviceConfig.MAC[6..7])
	packet[0x2d] = (byte) hubitat.helper.HexUtils.hexStringToInt(deviceConfig.MAC[4..5])
	packet[0x2e] = (byte) hubitat.helper.HexUtils.hexStringToInt(deviceConfig.MAC[2..3])
	packet[0x2f] = (byte) hubitat.helper.HexUtils.hexStringToInt(deviceConfig.MAC[0..1])
	packet[0x30] = (byte) hubitat.helper.HexUtils.hexStringToInt(deviceConfig.internalID[0..1])
	packet[0x31] = (byte) hubitat.helper.HexUtils.hexStringToInt(deviceConfig.internalID[2..3])
	packet[0x32] = (byte) hubitat.helper.HexUtils.hexStringToInt(deviceConfig.internalID[4..5])
	packet[0x33] = (byte) hubitat.helper.HexUtils.hexStringToInt(deviceConfig.internalID[6..7])

	//pad the payload for AES encryption
	if (payload.size() > 0) {
		startIdx = payload.size()
		numpad = 16 - (payload.size() % 16)
		log_info("(send_packet) Called send_packet - Payload size = ${payload.size()} - ${numpad} bytes padding")
		def padding = "00" * numpad
		payload = hubitat.helper.HexUtils.hexStringToByteArray(hubitat.helper.HexUtils.byteArrayToHexString(payload) + padding)
		log_info("(send_packet) Called send_packet - Padded Payload size = ${payload.size()}")
		log_info("(send_packet) PADDED PAYLOAD [${hubitat.helper.HexUtils.byteArrayToHexString(payload)}]")
	}
    
	def checksum = getChecksum(payload)
	packet[0x34] = (byte)(checksum & 0xff)
	packet[0x35] = (byte)(checksum >> 8)
	log_info("(send_packet) PACKET CHECKSUM ${String.format("0x%04X",checksum)}")
	
	if (payload.size() > 0) {
		byte [] ePayload = encryptPacket(deviceConfig, payload)
		log_info("(send_packet) payload ENCRYPTED")

		log_info("(send_packet) PACKET [${hubitat.helper.HexUtils.byteArrayToHexString(packet)}]")
		log_info("(send_packet) ENCRYPTED PAYLOAD [${hubitat.helper.HexUtils.byteArrayToHexString(ePayload)}]")

		packet = hubitat.helper.HexUtils.hexStringToByteArray(hubitat.helper.HexUtils.byteArrayToHexString(packet) + hubitat.helper.HexUtils.byteArrayToHexString(ePayload))
		log_info("(send_packet) PAYLOAD ADDED")

	}
	
	checksum = getChecksum(packet)
	packet[0x20] = (byte)(checksum & 0xff)
	packet[0x21] = (byte)(checksum >> 8)
	log_info("(send_packet) PACKET + PAYLOAD CHECKSUM ${String.format("0x%04X",checksum)}")
	log_info("(send_packet) FULL [${hubitat.helper.HexUtils.byteArrayToHexString(packet)}]")
	def sendError = sendMessage(deviceConfig, packet, callback)
	log_debug("(send_packet) completed")
	if (sendError) {return true}
	return false
}

def sendMessage(deviceConfig, packet, callback = "parse") {
	log_debug("(sendMessage) Called sendMessage(${deviceConfig.IP})")
	
	def packetData = hubitat.helper.HexUtils.byteArrayToHexString(packet)
	def Action = new hubitat.device.HubAction(
		packetData, 
		hubitat.device.Protocol.LAN, 
		[
			callback: callback,
			destinationAddress: deviceConfig.IP,
			type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, 
			encoding: hubitat.device.HubAction.Encoding.HEX_STRING
		]
	)
	try {
		log_debug("(sendMessage) sending packet to ${deviceConfig.IP}")
		log_info("(sendMessage) sending packet [${packetData}]")
		sendHubCommand(Action)
		log_debug("(sendMessage) completed")
		return false
	} catch(e) {
		log_error("(sendMessage) ERROR - Caught exception '${e}'")
		log_debug("(sendMessage) completed with error")
		return true
	}
	
}

// this function MUST be passed a byte array
def sendCodeData(data) {
	log_debug("(sendCodeData) ip [${state.deviceConfig.IP}]")
	log_info("(sendCodeData) code [${data}]")
	byte [] packet = [0x02, 0x00, 0x00, 0x00]

	packet = hubitat.helper.HexUtils.hexStringToByteArray(hubitat.helper.HexUtils.byteArrayToHexString(packet) + hubitat.helper.HexUtils.byteArrayToHexString(data))

	if (packet[0x04] == 0x26) {
		if (state.deviceConfig.hasIR == false) {
			log_debug("(sendCodeData) ERROR: Device (${state.deviceConfig.IP} - [${state.deviceConfig.devTypeName}]) can not send IR data")
			return true
		}
	} else {
		// trying to send RF data
		if (state.deviceConfig.isPro == false) {
			log_debug("(sendCodeData) ERROR: Device (${state.deviceConfig.IP} - [${state.deviceConfig.devTypeName}]) can not send RF data")
			return true
		}
	}
	state.CODEDATA = hubitat.helper.HexUtils.byteArrayToHexString(data)

	if (state.deviceConfig.isRM4) {
		packet = hubitat.helper.HexUtils.hexStringToByteArray("D000" + hubitat.helper.HexUtils.byteArrayToHexString(packet))
	}
	def repeats = state.codeRepeats.toInteger()
	send_packet(state.deviceConfig, 0x6a, packet, "parseCommandData")
	if (repeats > 0) { send_packet(state.deviceConfig, 0x6a, packet, "parseCommandData") }
	if (repeats > 1) { send_packet(state.deviceConfig, 0x6a, packet, "parseCommandData") }
	if (repeats > 2) { send_packet(state.deviceConfig, 0x6a, packet, "parseCommandData") }
	if (repeats > 3) { send_packet(state.deviceConfig, 0x6a, packet, "parseCommandData") }
	if (repeats > 4) { send_packet(state.deviceConfig, 0x6a, packet, "parseCommandData") }
	
	log_debug("(sendCodeData) completed")
	return false
}

def encryptPacket(deviceConfig, data){
	AES_setKey(deviceConfig.KEY)
	AES_setIV(deviceConfig.IV)
	def encData = AES_Encrypt(data)
	if (encData) { return encData }
	return null
}

def decryptPacket(deviceConfig, data) {
	AES_setKey(deviceConfig.KEY)
	AES_setIV(deviceConfig.IV)
	def decData = AES_Decrypt(data)
	if (decData) { return decData }
	return null
}

def getDiscoveryData(targetIP) {
	log_debug("(getDiscoveryData) discovering configuration for ${targetIP}")

	log_info("(createDiscoveryPacket) creating discovery packet")
	def hub = location.hubs[0]
	def localIP = hub.getDataValue("localIP")
	def hIP = localIP.tokenize( '.' )
	log_info("(createDiscoveryPacket) HUB IP ${hIP}")
	
	def ts = now()
	def DATE = new Date()
	log_info("(createDiscoveryPacket) DATE ${DATE.format("YYYY L d H m s u")}")
	//													DATE 2020 1 53 41 3 5 7
	def YEAR = DATE.format("YYYY") as int
	def YEAR1 = (byte)(YEAR & 0xff)
	def YEAR2 = (byte)(YEAR >> 8)
	def MONTH = DATE.format("L") as int
	def DAY = DATE.format("d") as int
	def HOURS = DATE.format("H") as int
	def MINS = DATE.format("m") as int
	def SECS = DATE.format("s") as int
	def WDAY = DATE.format("u") as int; if (WDAY == 7) { WDAY = 0 }
	def tzOffset = 0
	def (tz1, tz2, tz3, tz4) = [tzOffset, 0, 0, 0]
	if (tzOffset < 0) {
			(tz1, tz2, tz3, tz4) = [0xff + tzOffset + 1, 0xff, 0xff, 0xff]
	}
	tz1 = tz1%0xff
	byte [] dPacket = [ 0x5a, 0xa5, 0xaa, 0x55, 0x5a, 0xa5, 0xaa, 0x55,
				    tz1, tz2, tz3, tz4, YEAR1, YEAR2, SECS, MINS,
				    HOURS, DAY, WDAY, MONTH, 0x00, 0x00, 0x00, 0x00,
				    hIP[0] as int, hIP[1] as int, hIP[2]as int, hIP[3] as int, 0x80, 0x0d, 0x00, 0x00,
				    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x00,
				    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
				   ]

	def checksum = getChecksum(dPacket)
	dPacket[0x20] = (byte)(checksum & 0xff)
	dPacket[0x21] = (byte)(checksum >> 8)
	
	def packetData = hubitat.helper.HexUtils.byteArrayToHexString(dPacket)
	log_info("(getDiscoveryData) sending packet [${packetData}] to ${targetIP}")

	def Action = new hubitat.device.HubAction(
		packetData, 
		hubitat.device.Protocol.LAN, 
		[
			callback: "parseDiscoveryPacket",
			destinationAddress: targetIP,
			destinationPort: 80,
			type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, 
			encoding: hubitat.device.HubAction.Encoding.HEX_STRING
		]
	)
	log_info("getDiscoveryData - 2 sending PACKET")
	try {
		log_debug("getDiscoveryData - SENDING packet to ${targetIP}")
		log_info("getDiscoveryData - SENDING packet [${packetData}]")
		sendHubCommand(Action)
	} catch(e) {
		log_error("getDiscoveryData - ERROR - Caught exception '${e}'")
	}
}

def learnIR() {
	if (state.deviceConfig == null) {log_debug("(learnIR) ERROR: Device not configured"); return true }
	if (state.deviceConfig.hasIR != true) {
		log_debug("(learnIR) ERROR: Device (${state?.deviceConfig?.IP}) - [${state?.deviceConfig?.devTypeName}] can not Learn IR data")
		return true
	}
	log.trace("(learnIR) Attempting to enter IR learn mode...")
	log_debug("(learnIR) Attempting to enter IR learn mode...")
	byte [] packet = [0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	if (state.deviceConfig.isRM4 == true) packet = [0x04, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	return send_packet(state.deviceConfig, 0x6a, packet, "parseDataIR")
}

def checkDataIR() {
	log_debug("(checkDataIR) Checking for IR code data")
	byte [] packet = [0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	if (state.deviceConfig.isRM4 == true) packet = [0x04, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	return send_packet(state.deviceConfig, 0x6a, packet, "parseDataIR")
}

def checkDataRF() {
	log_debug("(checkDataRF) Checking for RF code data")
	byte [] packet = [0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	if (state.deviceConfig.isRM4 == true) packet = [0x04, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	return send_packet(state.deviceConfig, 0x6a, packet, "parseDataRF")
}

def learnRF() {
	if (state.deviceConfig == null) {log_debug("(learnRF) ERROR: Device not configured"); return true }
	if (state.deviceConfig.isPro != true) {
		log_debug("(learnRF) ERROR: Device (${deviceConfig.IP}) - [${deviceConfig.devTypeName}] can not Learn IR data")
		return true
	}
	log_debug("(learnRF) Attempting to enter RF learn mode")
	byte [] packet = [0x19, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	if (state.deviceConfig.isRM4 == true) packet = [0x04, 0x00, 0x19, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	return send_packet(state.deviceConfig, 0x6a, packet, "parseDataRF")
}

def checkFrequencyRF() {
	log_debug("(checkDataRF) Checking for RF data")
	byte [] packet = [0x1a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	if (state.deviceConfig.isRM4 == true) packet = [0x04, 0x00, 0x1a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	return send_packet(state.deviceConfig, 0x6a, packet, "parseDataRF")
}

def findPacketRF() {
	log_debug("(findPacketRF) Checking for RF data..")
	byte [] packet = [0x1b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	if (state.deviceConfig.isRM4 == true) packet = [0x04, 0x00, 0x1b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	return send_packet(state.deviceConfig, 0x6a, packet, "parseDataRF")
}

def cancelRF() {
	log_debug("(cancelRF) Cancelling RF sweep")
	byte [] packet = [0x1e, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	if (state.deviceConfig.isRM4 == true) packet = [0x04, 0x00, 0x1e, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
	return send_packet(state.deviceConfig, 0x6a, packet, "parseDataRF")
}

// parse functions
def parse(description) {
	def resp = parseLanMessage(description)
	if (resp == null) {log_debug("(parse) Could not parse packet."); return }
	byte [] parseData = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload)
	def dErr = parseData[0x22] | (parseData[0x23] << 8)
	if (parseData.size() > 0x38) {payload = decryptPacket(state.deviceConfig,parseData[0x38..-1])}
	log_debug("(parse) received description: ${description}")
	if (payload) {log_debug("(parse) received payload: [${hubitat.helper.HexUtils.byteArrayToHexString(payload)}]")}
	if (dErr != 0) {log_debug("(parse) ERROR - Device indicated error code ${String.format("%02X",dErr & 0xffff)}")}
}

def parseStatusData(description) {
	log_debug("(parseStatusData) received description: ${description}")
	def resp = parseLanMessage(description)
	if (resp == null) {log_debug("(parseStatusData) ERROR - Could not parse packet."); return }
	byte [] statusData = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload)
	log_info("(parseStatusData) parsing response packet.  IP [${resp.ip}]\n${resp.payload}")
	def dErr = statusData[0x22] + (statusData[0x23] * 256)
	if (dErr == 0) {
		if (statusData.size() == 0x38) {
			log_debug("(parseStatusData) ERROR - Device did not return a status payload.")
			return null
		}
		def payload = decryptPacket(state.deviceConfig,statusData[0x38..-1])
		if (payload) {
			if (state.deviceConfig.relayCount == 4) {
				local powerState = payload[0x0e]
				def S1 = (powerState && 0x01) ? true : false
				def S2 = (powerState && 0x02) ? true : false
				def S3 = (powerState && 0x04) ? true : false
				def S4 = (powerState && 0x08) ? true : false
				def powerAll = S1 || S2 || S3 || S4
				log_debug("(parseStatusData) return [powered: ${powerAll}, poweredS1: ${S1}, poweredS2: ${S2}, poweredS3: ${S3}, poweredS4: ${S4}]")
				return [powered: powerAll, poweredS1: S1, poweredS2: S2, poweredS3: S3, poweredS4: S4]
			} else {
				def powerState = (payload[0x05] > 0) ? true : false
				log_debug("(parseStatusData) return [powered: ${powerState}]")
				return {powered: powerState}
			}
		} else {log_debug("(parseStatusData) ERROR - Could not decrypt packet ${statusData[0x38..-1]}")}
	} else {
		log_debug("(parseStatusData) ERROR - Device indicated error code ${String.format("%02X",dErr & 0xffff)}")
	}
}

def parseTargetResponse(description) {
	log_debug("(parseTargetData) - received description: ${description}")
	def resp = parseLanMessage(description)
	if (resp == null) {log_debug("parseTargetData: Could not parse packet."); return }
	byte [] targetData = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload)
	log_info("(parseTargetData) parsing response packet.  IP [${resp.ip}]\n${resp.payload}")
	def dErr = targetData[0x22] + (targetData[0x23] * 256)
	if (dErr == 0) {
		if (targetData.size() == 0x38) {
			log_debug("(parseTargetData) ERROR - Device did not return a status payload.")
			return null
		}
		def payload = decryptPacket(state.deviceConfig,targetData[0x38..-1])
		if (payload) {
			if (state.deviceConfig.relayCount == 4) {
				def powerState = payload[0x0f]
				def S1 = (powerState && 0x01) ? true : false
				def S2 = (powerState && 0x02) ? true : false
				def S3 = (powerState && 0x04) ? true : false
				def S4 = (powerState && 0x08) ? true : false
				def powerAll = S1 || S2 || S3 || S4
				log_debug("(parseTargetData) return [powered: ${powerAll}, poweredS1: ${S1}, poweredS2: ${S2}, poweredS3: ${S3}, poweredS4: ${S4}]")
				return [powered: powerAll, poweredS1: S1, poweredS2: S2, poweredS3: S3, poweredS4: S4]
			} else {
				def powerState = (payload[0x05] > 0) ? true : false
				log_debug("(parseTargetData) return [powered: ${powerState}]")
				return [powered: powerState]
			}
		} else {log_debug("(parseTargetData) Could not decrypt packet ${statusData[0x38..-1]}")}
	} else {
		log_debug("(parseTargetData) ERROR - Device indicated error code ${String.format("%02X",dErr & 0xffff)}")
	}
}

def parseAuthData(description) {
	log_debug("(parseAuthData) received description: ${description}")
	def resp = parseLanMessage(description)
	if (resp == null) {log_debug("parseAuthData: Could not parse packet."); return }
	byte [] authData = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload)
	log_info("(parseAuthData) parsing response packet.  IP [${resp.ip}]\n${resp.payload}")

	if (authData.size() == 0x38) {log_debug("(parseAuthData) ERROR - packet does not contain an Auth payload."); return }
	byte [] payload = authData[0x38..-1]
	if ((payload == null) || (payload == "")) { log_debug("(parseAuthData) ERROR - Could not decrypt packet ${authData[0x38..-1]}"); return }
	byte [] dPayload = decryptPacket(state.deviceConfig,payload)

	log_debug("(parseAuthData) packet ${hubitat.helper.HexUtils.byteArrayToHexString(dPayload)}")
	def internalID = ""
	try { internalID = arrayListToHexString(dPayload[0x00..0x03]) } catch(e) {}
	def KEY =  ""
	try { KEY = arrayListToHexString(dPayload[0x04..0x13]) } catch(e) {log_debug(e)}

	if (internalID != "") { state.deviceConfig.internalID = internalID }
	if (KEY != "") { state.deviceConfig.KEY = KEY; state.deviceConfig.hasAuth = true }
	if (state.deviceConfig.hasAuth) {
		def configJson = new groovy.json.JsonOutput().toJson(state.deviceConfig)
		send_Event(name: "deviceConfig", value: configJson, displayed:false, isStateChange: true)
	}

	if ((state.deviceConfig.hasAuth)&&(state.deviceConfig.hasTemp)) {
		check_sensors()
	} else {
		send_Event(name: "temperature", value: -100, displayed:false, isStateChange: true)
	}
	if ((state.deviceConfig.hasAuth) && ((state.deviceConfig.hasTemp) || (state.deviceConfig.relayCount != 0))) {
		unschedule()
		runEvery5Minutes("getStatus")
	}
	log_debug("(parseAuthData) Parsed response packet.  ID [${state.deviceConfig.internalID}]  KEY [${state.deviceConfig.KEY}]  AUTH [${state.deviceConfig.hasAuth}]")
	return null
}

def parseSensorData(description) {
	log_debug("parseSensorData - received description: ${description}")
	def resp = parseLanMessage(description)
	if (resp == null) {log_debug("parseSensorData: Could not parse packet."); return }
	byte [] sensorData = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload)
	log_info("parseSensorData: parsing response packet.  IP [${resp.ip}]\n${resp.payload}")
	def dErr = sensorData[0x22].toInteger() + (sensorData[0x23].toInteger() * 256)
	if (dErr == 0) {
		byte [] payload = decryptPacket(state.deviceConfig,sensorData[0x38..-1])
		//print("check_sensors: decrypted payload ["..hex_dump(payload).."]")
		def temp = ((payload[0x04].toInteger() * 10) + payload[0x05].toInteger()) / 10
		log_debug("parseSensorData: return temperature = ${temp} C")
		if (location.temperatureScale == "F") {
			temp = celsiusToFahrenheit(temp)
			log_debug("parseSensorData: converted temperature = ${temp} F")
		}
		send_Event(name: "temperature", value: temp, displayed:false, isStateChange: true)
		return false
	}
	log_debug("parseSensorData: FAILED to retreive sensor data")
	return true
}

def parseDiscoveryPacket(description) {
	log_debug("(parseDiscoveryData) received description: ${description}")
	def resp = parseLanMessage(description)
	if (resp == null) {log_debug("(parseDiscoveryPacket) ERROR - Could not parse packet."); return }
	byte [] payload = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload)
	log_info("(parseDiscoveryPacket) parsing response packet.  IP [${resp.ip}]\n${resp.payload}")
	def blDevice = [
		Name: (getString(payload[0x40..-1]) == "")?"[UNNAMED]":getString(payload[0x40..-1]),
		MAC: arrayListToHexString(payload[0x3a..0x3f].reverse()),
		IP: decodeHexIP(resp.ip),
		devType: hubitat.helper.HexUtils.hexStringToInt("0x"+arrayListToHexString(payload[0x35]) + arrayListToHexString(payload[0x34])),
		devTypeName: "",
		internalID: "00000000",
		hasAuth: false,
		isPro: false,
		hasIR: false,
		hasTemp: false,
		isRM4: false,
		relayCount: -1,
		supported: false,
		cloudLocked: ((payload[0x7f] == 0) ? false : true)
	]
	def tInfo = getDeviceTypeInfo(blDevice.devType)
	blDevice.devTypeName = tInfo.devTypeName
	blDevice.isPro = tInfo.isPro
	blDevice.hasIR = tInfo.hasIR
	blDevice.hasTemp = tInfo.hasTemp
	blDevice.relayCount = tInfo.relayCount
	blDevice.isRM4 = tInfo.isRM4
	blDevice.supported = tInfo.supported

	log_debug("(parseDiscoveryData) Discovered device: ${blDevice}")

	state.deviceConfig = blDevice
	getAuth(blDevice)
	
}

def parseCommandData(description) {
	log_debug("(parseCommandData) received description: ${description}")
	def resp = parseLanMessage(description)
	if (resp == null) {log_debug("(parseCommandData) ERROR - Could not parse packet."); return }
	byte [] commandData = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload)
	log_info("(parseCommandData) parsing response packet.  IP [${resp.ip}]\n${resp.payload}")
	def dErr = commandData[0x22].toInteger() + (commandData[0x23].toInteger() * 256)
	if (dErr == 0) {
		byte [] payload = decryptPacket(state.deviceConfig,commandData[0x38..-1])
		log_debug("(parseCommandData) return [${payload}]")
		return false
	}
	log_debug("(parseCommandData) ERROR - Failed to retreive command response data")
	return true
}

def parseDataIR(description) {
	log_debug("(parseDataIR) received description: ${description}")
	def resp = parseLanMessage(description)
	if (resp == null) {log_debug("(parseDataIR) ERROR - Could not parse packet."); return }
	byte [] learnData = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload)
	log_info("(parseDataIR) parsing response packet.  IP [${resp.ip}]\n${resp.payload}")
	def dErr = learnData[0x22].toInteger() + (learnData[0x23].toInteger() * 256)
	byte [] payload = []
	if (learnData.size() > 0x38) {
		payload = decryptPacket(state.deviceConfig,learnData[0x38..-1])
		if (state.deviceConfig.isRM4) { payload = payload[2..-1] }
	}
	if (dErr == 0) {
		if ((payload == null) || (payload.size() == 0)) {
			log_debug("(parseDataIR) ERROR - Response packet does not contain a payload.")
		} else if (payload[0] == 3) {
			log_debug("(parseDataIR) Entered LEARN Mode.")
			LEARN = now()
			send_Event(name: "IR_Status", value: "Learning: Press button on IR Remote.", displayed:false, isStateChange: true)
			pauseExecution(500)
			return checkDataIR()
		} else if (payload[0] == 4) {
			if (payload.size() > 0x20) {
				// received IR data
				def CODEDATA = arrayListToHexString(payload[4..-1])
				log_debug("(parseDataIR) Received IR Data [${CODEDATA}].")
				LEARN = 0
				state.CODEDATA = CODEDATA
				send_Event(name: "CODEDATA", value: CODEDATA, displayed:false, isStateChange: true)
				send_Event(name: "IR_Status", value: "Learned Code", displayed:false, isStateChange: true)
				pauseExecution(1500)
				send_Event(name: "IR_Status", value: "IDLE", displayed:false, isStateChange: true)
				return null
			} else {
				log_debug("(parseDataIR) TIMER [${now() - LEARN}].")
				pauseExecution(500)
				return checkDataIR()
			}
		}
	} else {
		if ((payload == null) || (payload.size() == 0)) {
			log_debug("(parseDataIR) ERROR - Device reported error [${String.format("%04X",dErr&0xffff)}]")
			// do not exit despite the error - some devices report error unit data is available
			if ( (LEARN != 0) && ((now() - LEARN) < 30000) ) {
				pauseExecution(500)
				return checkDataIR()
			}
		} else if (payload[0] == 3) {
			log_debug("(parseDataIR) ERROR - Failed to enter LEARN Mode.")
			LEARN = 0
			send_Event(name: "IR_Status", value: "FAILED", displayed:false, isStateChange: true)
			pauseExecution(1500)
		} else if (payload[0] == 4) {
			log_info("(parseDataIR) ERROR - LEARN [${LEARN}]  NOW [${now()}]  TIMER [${now() - LEARN}].")
			if ( (LEARN == 0) || ((now() - LEARN) > 30000) ) {
				// learn mode has ended
				log_debug("(parseDataIR) ERROR - LEARN Mode timed out.")
				LEARN = 0
				state.CODEDATA = ""
				send_Event(name: "CODEDATA", value: "", displayed:false, isStateChange: true)
				send_Event(name: "IR_Status", value: "Timeout", displayed:false, isStateChange: true)
				pauseExecution(1500)
				send_Event(name: "IR_Status", value: "IDLE", displayed:false, isStateChange: true)
				return null
			}
			pauseExecution(500)
			return checkDataIR()
		}
	}
	LEARN = 0
	state.CODEDATA = ""
	log_debug("parseDataIR) ERROR - Failed to retreive learn response data")
	send_Event(name: "IR_Status", value: "ERROR", displayed:false, isStateChange: true)
	pauseExecution(1500)
	send_Event(name: "IR_Status", value: "IDLE", displayed:false, isStateChange: true)
	return true
}

def parseDataRF(description) {
	log_debug("(parseDataRF) received description: ${description}")
	def resp = parseLanMessage(description)
	if (resp == null) {log_debug("(parseDataRF) Could not parse packet."); return }
	byte [] learnData = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload)
	log_info("(parseDataRF) parsing response packet.  IP [${resp.ip}]\n${resp.payload}")
	def dErr = learnData[0x22].toInteger() + (learnData[0x23].toInteger() * 256)
	byte [] payload = []
	if (learnData.size() > 0x38) {		
		payload = decryptPacket(state.deviceConfig,learnData[0x38..-1])
		if (state.deviceConfig.isRM4) { payload = payload[2..-1] }
	}
	if (dErr == 0) {
		if ((payload == null) || (payload.size() == 0)) {
			log_debug("(parseDataRF) ERROR - Response packet does not contain a payload.")
		} else if (payload[0] == 0x19) {
			log_debug("(parseDataRF) Entered RF FREQUENCY SWEEP Mode.")
			LEARN = now()
			send_Event(name: "RF_Status", value: "SWEEP: LED = ON : Press AND Hold button on RF Remote.", displayed:false, isStateChange: true)
			pauseExecution(500)
			return checkFrequencyRF()
		} else if (payload[0] == 0x1a) {
			// checkFrequencyRF response - wait until byte 4 is 1 (success) or 4 (failed)
			if (payload[4] == 0) {
				// continue waiting for frequency determination
				pauseExecution(500)
				return checkFrequencyRF()
			} else if (payload[4] == 1) {
				log_debug("(parseDataRF) Found RF FREQUENCY...")
				send_Event(name: "RF_Status", value: "SWEEP : LED = OFF : Frequency Found. Release RF Remote button.", displayed:false, isStateChange: true)
				pauseExecution(1000)
				// reset the timeout
				LEARN = now()
				return findPacketRF()
			} else {
				// the frequency scan failed... cancel the process
				log_debug("(parseDataRF) ERROR - RF FREQUENCY SWEEP failed.")
				send_Event(name: "RF_Status", value: "SWEEP : LED = OFF : FAILED. Release RF Remote button.", displayed:false, isStateChange: true)
				return cancelRF()
			}
		} else if (payload[0] == 0x1b) {
			// findPacketRF response - this is an ack only - move to next step
			// reset the timeout
			send_Event(name: "RF_Status", value: "FIND : LED = ON : Press and Release RF Remote button repeatedly.", displayed:false, isStateChange: true)
			LEARN = now()
			pauseExecution(500)
			return checkDataRF()
		} else if (payload[0] == 0x1e) {
			// ack for cancelRF()
			log_debug("(parseDataRF) RF FREQUENCY SWEEP Mode cancelled.")
			send_Event(name: "RF_Status", value: "CANCELLED", displayed:false, isStateChange: true)
			LEARN = 0
			state.CODEDATA = ""
			send_Event(name: "CODEDATA", value: "", displayed:false, isStateChange: true)
			pauseExecution(1500)
			send_Event(name: "RF_Status", value: "IDLE", displayed:false, isStateChange: true)
			return null
		} else if (payload[0] == 4) {
			if (payload.size() > 0x20) {
				// received RF data
				def CODEDATA = arrayListToHexString(payload[4..-1])
				log_debug("(parseDataRF) Received RF Data [${CODEDATA}].")
				send_Event(name: "RF_Status", value: "LEARNED", displayed:false, isStateChange: true)
				LEARN = 0
				state.CODEDATA = CODEDATA
				send_Event(name: "CODEDATA", value: CODEDATA, displayed:false, isStateChange: true)
				pauseExecution(1500)
				send_Event(name: "RF_Status", value: "IDLE", displayed:false, isStateChange: true)
				return null
			} else {
				log_info("(parseDataRF) TIMER [${now() - LEARN}].")
				pauseExecution(500)
				return checkDataRF()
			}
		}
	} else {
		if ((payload == null) || (payload.size() == 0)) {
			log_debug("(parseDataRF) ERROR - Device reported error [${String.format("%02X",dErr&0xffff)}]")
		} else if (payload[0] == 0x19) {
			log_debug("(parseDataRF) Failed to enter RF FREQUENCY SWEEP Mode.")
			send_Event(name: "RF_Status", value: "SWEEP: FAILED.", displayed:false, isStateChange: true)
			state.CODEDATA = ""
			LEARN = 0
			pauseExecution(1500)
			send_Event(name: "RF_Status", value: "IDLE", displayed:false, isStateChange: true)
			return null
		} else if (payload[0] == 0x1a) {
			// checkFrequencyRF response - wait until byte 4 is 1 (success) or 4 (failed)
			if (payload[4] == 0) {
				// continue waiting for frequency determination
				pauseExecution(500)
				return checkFrequencyRF()
			} else if (payload[4] == 1) {
				log_debug("(parseDataRF) Found RF FREQUENCY...")
				send_Event(name: "RF_Status", value: "SWEEP : LED = OFF : Frequency Found. Release RF Remote button.", displayed:false, isStateChange: true)
				pauseExecution(1000)
				// reset the timeout
				LEARN = now()
				return findPacketRF()
			} else {
				// the frequency scan failed... cancel the process
				log_debug("(parseDataRF) ERROR - RF FREQUENCY SWEEP failed.")
				return cancelRF()
			}
		} else if (payload[0] == 0x1b) {
			// findPacketRF response - this is an ack only - move to next step
			// reset the timeout
			send_Event(name: "RF_Status", value: "FIND : LED = AMBER : Press and Release RF Remote button repeatedly.", displayed:false, isStateChange: true)
			LEARN = now()
			pauseExecution(500)
			return checkDataRF()
		} else if (payload[0] == 0x1e) {
			// ack for cancelRF()
			log_debug("(parseDataRF) RF FREQUENCY SWEEP Mode cancelled.")
			send_Event(name: "RF_Status", value: "CANCELLED", displayed:false, isStateChange: true)
			state.CODEDATA = ""
			send_Event(name: "CODEDATA", value: "", displayed:false, isStateChange: true)
			pauseExecution(1500)
			send_Event(name: "RF_Status", value: "IDLE", displayed:false, isStateChange: true)
			LEARN = 0
			return null
		} else if (payload[0] == 4) {
			log_info("(parseDataRF) ERR  LEARN [${LEARN}]  NOW [${now()}]  TIMER [${now() - LEARN}].")
			if ( (LEARN == 0) || ((now() - LEARN) > 30000) ) {
				// learn mode has ended
				log_debug("(parseDataRF) LEARN Mode timed out.")
				send_Event(name: "RF_Status", value: "TIMEOUT", displayed:false, isStateChange: true)
				state.CODEDATA = ""
				send_Event(name: "CODEDATA", value: "", displayed:false, isStateChange: true)
				pauseExecution(1500)
				send_Event(name: "RF_Status", value: "IDLE", displayed:false, isStateChange: true)
				LEARN = 0
				return null
			}
			pauseExecution(500)
			return checkDataRF()
		}
	}
	log_debug("(parseDataRF) FAILED to retreive RF response data")
	send_Event(name: "RF_Status", value: "ERROR", displayed:false, isStateChange: true)
	pauseExecution(1500)
	send_Event(name: "RF_Status", value: "IDLE", displayed:false, isStateChange: true)
	return true
}


// IR/RF Code data functions
def StoreCode(codeName) {
	state.codeStore = (state.codeStore?state.codeStore:[:])
	codeName = codeName.tokenize(" ,!@#\$%^&()").join("_")
	log_debug("(StoreCode) Store code : [${codeName}] = [${state.CODEDATA}]")
	if ((codeName == null) || (codeName == "")) {
		log_debug("(StoreCode) ERROR: A name for the code must be provided.")
		return null
	}
	if (state.codeStore[(codeName)] != null) {
		log_debug("(StoreCode) ERROR: A code is already stored under this name.")
		return null
	}
	if (state.CODEDATA == null) {
		log_debug("(StoreCode) ERROR: A code is available for storing..")
		return null
	}
	state.codeStore[(codeName)] = state.CODEDATA
	log_debug("(StoreCode) Code Stored : Stored codes [${state.codeStore.size()}]")
}

def SendStoredCode(codeName) {
	def sCode = null
	state.codeStore = (state.codeStore?state.codeStore:[:])
	codeName = codeName.tokenize(" ,!@#\$%^&()").join("_")
	log_debug("(SendStoredCode) Requested code : [${codeName}]")
	if (state.codeStore[(codeName)] == null) {
		if ( getParent() ) {
			def pCode = getParent().getCode(codeName)
			if (pCode == null) {
				log_debug("(SendStoredCode) ERROR: A code [${codeName}] is not stored in the device or the parent application under this name.")
				return null
			} else {
				log_debug("(SendStoredCode) Retrieved code [${codeName}] from parent App.")
				sCode = pCode
			}
		} else {
			log_debug("(SendStoredCode) ERROR: A code [${codeName}] is not stored under this name.")
			return null
		}
	} else {
		sCode = state.codeStore[(codeName)]
	}
	log_debug("(SendStoredCode) Found code for [${codeName}]")
	byte [] codeData = hubitat.helper.HexUtils.hexStringToByteArray(sCode)
	if (codeData) {
		def sErr = sendCodeData(codeData)
		if (sErr == false) {
			log_debug("(SendStoredCode)  Sent code data.")
			return true
		} else {
			log_debug("(SendStoredCode) ERROR - Could not send code data.")
			return false
		}
	} else {
		log_debug("(SendStoredCode) ERROR - Could not decode code data.")
		return false
	}
}

def push(button) {
	log_debug("(push) Request to push button [${button}]")
	SendStoredCode(button)
}

def SendLastCode() {
	if (state.CODEDATA == null) {
		log_debug("(SendLastCode) ERROR: A code is not available for sending..")
		return null
	}
	def sCode = state.CODEDATA
	log_debug("(SendLastCode) Sending last used code.")
	byte [] codeData = hubitat.helper.HexUtils.hexStringToByteArray(sCode)
	if (codeData) {
		def sErr = sendCodeData(codeData)
		if (sErr == false) {
			log_debug("(SendLastCode) Sent code data.")
			return true
		} else {
			log_debug("(SendLastCode) ERROR - Could not send code data.")
			return false
		}
	} else {
		log_debug("(SendLastCode) ERROR - Could not decode code data.")
		return false
	}
}

def SendCode(data) {
	log_info("(sendCode) code [${data}]")
	byte [] code = []
	// is the code a hex encoded string?
	if ((data[0] == "J") ||(data[-1] == "=") || ((data[0] == "J") && (data[-1] == "A"))) { 
		code = data.decodeBase64()
		log_info("(sendCode) INFO: Converted code from Base64 encoded to Hex Encoded [${hubitat.helper.HexUtils.byteArrayToHexString(code)}].")
	} else {
		code = hubitat.helper.HexUtils.hexStringToByteArray(data)
	}
	if (code == []) {
		log_debug("(sendCode) ERROR: Provided IR/RF code is in a recognized format.")
		return null
	}
	// remove any trailing zero bytes
	def tStr = hubitat.helper.HexUtils.byteArrayToHexString(code)
	while (tStr[-2..-1] == "00") { tStr = tStr[0..-3] }
	code = hubitat.helper.HexUtils.hexStringToByteArray(tStr)
	
	if (!validateCode(code)) {
			log_debug("(sendCode) ERROR: Provided IR/RF code is not valid.")
			return null
	}
	def sErr = sendCodeData(code)
	if (sErr == false) {
		log_debug("(SendCode) Sent code data.")
		return true
	} else {
		log_debug("(SendCode) ERROR - Could not send code data.")
		return false
	}
}

def SendProntoCode(ProntoCode) {
	log_debug("(SendProntoCode) Pronto Code: ${ProntoCode}")
	if ((ProntoCode == null) || (ProntoCode == "")) {
		log_debug("(SendProntoCode) ERROR - No Pronto data provided.")
		return false
	}
	def sCommand = convertProntoCode(ProntoCode)
	if (!sCommand) {
		// CodeData does not contain a convertable Pront IR code
		log_debug("(SendProntoCode) ERROR - Could not convert Pronto data.")
		return false
	}
	byte [] codeData = hubitat.helper.HexUtils.hexStringToByteArray(sCommand)
	if (codeData) {
		if (!validateCode(codeData)) {
			log_debug("(SendProntoCode) ERROR - Code data did not convert to a valid broadlink code.")
			return false
		}
		def sErr = sendCodeData(codeData)
		if (sErr == false) {
			log_debug("(SendProntoCode) Sent code data.")
			return true
		} else {
			log_debug("(SendProntoCode) ERROR - Could not send code data.")
			return false
		}
	} else {
		log_debug("(SendProntoCode) ERROR - Could not decode code data.")
		return false
	}
}

private convertProntoCode(pcode) {
	//               [[0000 006d 0000 0022 00ac 00ac 0015 0040 0015 0040 0015 0040 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0040 0015 0040 0015 0040 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0040 0015 0040 0015 0015 0015 0015 0015 0040 0015 0040 0015 0040 0015 0040 0015 0015 0015 0015 0015 0040 0015 0040 0015 0015 0015 0689]]
	// pronto format = 0000 FFFF xxxx yyyy O1O1 .. OXOX R1R1 .. RXRX TPTP
    //     where       0000 = 4 digit Pronto code header - always 0000
    //                 FFFF = 4 digit hex frequency divisor - ie 006D = 109 = math.floor(4146/109) = 38KHz
    //                 xxxx = size of once burst pairs
    //                 yyyy = size of repeat burst pairs
    //                 O1O1 .. OXOX = once code data to encode in hex words MSB -> LSB
    //                 R1R1 .. RXRX = repeat code data to encode in hex words MSB -> LSB
    //                 TPTP = 4 digit hex number trailing pulse
	log_debug("(convertProntoCode): Starting IR Code conversion.")
	log_info("(convertProntoCode):   Initial Pronto Code [${pcode}]")

	def xwords = pcode.tokenize(" ")
	def pwords = xwords.collect{ it -> 
		return Integer.parseUnsignedInt(it,16)
	}
	def bwords = []
	if ( (pwords == null) || (pwords[0] != 0) || (pwords[1] == 0) || ((pwords[2] == 0) && (pwords[3] == 0))) {
		log_debug("(convertProntoCode): ERROR -  IR Code conversion failed. Unrecognized format.")
		return null
	}
	def n = pwords[2]
	if (n == 0) { n = pwords[3] }
	if (((n * 2) + 4) > pwords.size()) { 
		log_debug("(convertProntoCode): ERROR -  IR Code conversion failed.")
		return null 
	}
	int freq = Math.floor(4145 / pwords[1].toInteger())
	def ct = (1000/freq) * 269 / 8192
	log_debug("(convertProntoCode): ct [${ct}]")
	for ( i = 4; i < pwords.size(); i++) {
		def bVal = ((pwords[i] * ct) + 0.5).toInteger()
		if (bVal > 256) {
			bwords[i-4] = "00" + String.format("%02X",(bVal & 0xFF)) + String.format("%02X",(bVal >> 8))
		} else {
			bwords[i-4] = String.format("%02X",bVal)
		}
	}
	def codeData = bwords.join() + "0d05"
	int cSize = codeData.size() /2
	int L1 = (cSize & 0xff)
	int L2 = (cSize >> 8)
	def newcode = String.format("%02X",freq) + "00" + String.format("%02X",L1) + String.format("%02X",L2) + codeData //+ codePadding
	log_debug("(convertProntoCode):   Converted Broadlink Code [${newcode}]")
	log_debug("(convertProntoCode): IR Code conversion completed.")
	return newcode
}

// this function must be passed a byte array
def validateCode(codeData) {
	log_info("(validateCode) Validating IR/RF code [${codeData}]")
	if ((codeData[0] == 0x26) || (((codeData[0] < 0)?(codeData[0]+256):codeData[0]) == 0xb2) || (((codeData[0] < 0)?(codeData[0]+256):codeData[0]) == 0xd7)) {
		def cLength = codeData[2] + (codeData[3] * 256)
		if (codeData[0] == 0x26) {
			if (codeData[1] > 2)  {
				log_info("(validateCode) Valid code - WARNING:  Excessive IR repeat count ( repeat > 2 )")
				return true	// "Valid Code: WARNING - Excessive IR repeat count ( repeat > 2 )"
			}
			if (codeData.size() == (cLength +4)) {
				// code was stripped of lead out data before being encoded
				log_info("(validateCode) Valid code - WARNING:  IR lead-out removed")
				return true
			}
			if (((codeData[-2] != 0x0d) || (codeData[-1] != 0x05)) && ((codeData[-4] != 0x0d) || (codeData[-3] != 0x05))) {
				log_info("(validateCode) Code ERROR:  invalid IR lead-out data")
				return false	// "Code ERROR: invalid IR lead-out data"
			}
		}
		if ( (codeData[0] == 0xb2) && (codeData[1] > 0x15) ) {
			log_info("(validateCode) Valid code - WARNING:  Excessive RF433 repeat count ( repeat > 21 )")
			return true	// "Valid Code: WARNING - Excessive RF433 repeat count ( repeat > 21 )"
		}
		if ( (codeData[0] == 0xd7) && (codeData[1] > 0x16) ) {
			log_info("(validateCode) Valid code - WARNING:  Excessive RF315 repeat count ( repeat > 22 )")
			return true	// "Valid Code: WARNING - Excessive RF315 repeat count ( repeat > 22 )"
		}
		log_info("(validateCode) Valid code:No code errors found")
		return true	// "Valid Code: No code errors found"
	} else {
		log_info("(validateCode) Code ERROR: Invalid code header")
		return false // "Code ERROR: Invalid code header"
	}
}

def importCodes(codeData) {
	// accept IR/RF codes from the application and integrate into locally stored codes
	log_debug("(importCodes) Importing codes")
	if (state.codeStore == null) { state.codeStore = [:] }
	codeData.each { code ->
		if ( !state.codeStore[code.key] ) {
			// add code stored on the device to the app codeStore
			state.codeStore[code.key] = code.value
		}
	}
	log_debug("(importCodes) Imported codes [${state.codeStore}]")
}

def exportCodes() {
	// send locally stored IR/RF codes to the application
	if (state.codeStore == null) { state.codeStore = [:] }
	log_debug("(exportCodes) Exporting stored codes")
	return state.codeStore
}

def eraseCodes() {
	// erase locally stored IR/RF codes from the driver
	log_debug("(eraseCodes) Erasing stored codes")
	state.codeStore = [:]
	return true
}

def getSettings() {
	log_debug("(getSettings) Retrieving device settings: codeRepeats [${codeRepeats}]  destIP [${destIp}]")
	return [codeRepeats: codeRepeats, destIp: destIp]
}

def generateIR( offBurst = null, onBurst = null, leadIn = null, bitmap = null, leadOut = null) {
	log_debug("(generateIR) Generating IR code\noffBurst: ${offBurst}\nonBurst: ${onBurst}\nleadIn: ${leadIn}\nleadOut: ${leadOut}\nbitmap: ${bitmap}")
	if ((leadIn == null) || (leadIn == null) || (leadIn == null) || (leadIn == null) || (leadIn == null)) {
		log_debug("(generateIR) Generating IR code - missing code generation data")
		return null
	}
	def newCode = ""
	def burst = ["0": offBurst, "1": onBurst]
	for (i = 0; i<bitmap.size(); i++) {
		newCode = newCode + burst[bitmap[i]].value
	}
	newCode = leadIn + newCode + leadOut
	def gCode = "2600" + String.format("%02X",((newCode.size() >> 1) & 0xFF)) + String.format("%02X",((newCode.size() >> 1) >> 8)) + newCode + "0D05"
	log_debug("(generateIR) Generated IR code [${gCode.toUpperCase()}]")
	return SendCode(gCode)
}

/**
 *  Broadlink RM/RM Pro/RM Mini/SP driver for Hubitat
 *      by CybrMage
 */