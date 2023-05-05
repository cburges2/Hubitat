/**
 *  Broadlink Device Manager (BETA) for Hubitat
 *
 *
***********************************************************************************************************************/
public static String version()      {  return "v0.46  2020-05-01"  }

definition(
    name: "Broadlink Device Manager (BETA)",
    namespace: "cybr",
    author: "CybrMage",
    description: "Discover and manage Broadlink RM/RM Pro/RM Mini/SP devices",
	singleInstance: true,
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
	documentationLink: "https://community.hubitat.com/t/beta-native-broadlink-rm-rm-pro-rm-mini-sp-driver-rc-hvac-manager/31344"
)


preferences 
{
    page(name: "MainPage")
    page(name: "DiscoveryPage")
    page(name: "configureDevicePage")
    page(name: "configureDevice")
    page(name: "editDevicePage")
    page(name: "editDevice")
    page(name: "CodesPage")
    page(name: "codeEditPage")
    page(name: "learnCodes")
    page(name: "learnCodesProcess")
    page(name: "importCodes")
    page(name: "exportCodes")
}

def installed() 
{
	log.debug "Broadlink Device Manager ${version()} - Installed with settings: ${settings}"
    state.AppIsInstalled = true
	initialize()
}

def updated() 
{
	log.debug "Broadlink Device Manager ${version()} - Updated with settings: ${settings}"

	initialize()
}

def initialize() 
{
	if (!state.codeStore) { state.codeStore = [:] }
    if (!state.broadlinkDevices) { state.broadlinkDevices = [:] }
    state.configuringDevice = false
	app.clearSetting("configureDeviceName")
    state.editingDevice = false
	state.addingCode = false
	state.editingCode = false
	state.learnStage = null
	state.importingDeviceCodes = false

	log.debug "Broadlink Device Manager ${version()} - Initialized"
}

def MainPage() {
	log_debug("(MainPage) APP [${version()}]  debug [${debug}]  debugVerbose [${debugVerbose}]")
	if (!state.AppIsInstalled) {
		return dynamicPage(name: "MainPage", title: "", install:true, uninstall: true){
			section("<h2>Broadlink Device Manager</h2>") {
				paragraph ""
				paragraph"This software is provided \"AS IS\", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose and noninfringement."
				paragraph ""
				paragraph "<h2>To complete the installation, click on \"Done\"</h2>"
			}
		}
	}
	state.configuringDevice = false
	app.clearSetting("configureDeviceName")
	state.editingDevice = false
	state.addingCode = false
	state.editingCode = false
	state.learnStage = null
	state.importingDeviceCodes = false
	return dynamicPage(name: "MainPage", title: "Broadlink Manager ${version()}", install:true, uninstall: true){
		section("Device Management") {
			href(name: "discovery",title: "Devices",required: false,page: "DiscoveryPage",description: "tap to manage Broadlink devices")
		}
		section("Code Management") {
			href(name: "codes",title: "Remote codes",required: false,page: "CodesPage",description: "tap to manage IR/RF codes")
		}
		section("Options") {
			input("AutomaticCodeImport", "bool", title:"Enable automatic Remote Code import from child devices.",defaultValue:true, required:false)
			input("debug", "bool", title:"Enable Debug logging.",defaultValue:false, required:false)
			input("debugVerbose", "bool", title:"Enable VerboseDebug logging.",defaultValue:false, required:false)
		}
	}
}

def CodesPage(params = null) {
	log_debug("(CodesPage) params [${params}]  editing [${state.editingCode}]  adding [${state.addingCode}]  newCodeName [${newCodeName}]  AutomaticCodeImport [${AutomaticCodeImport}]")
	unsubscribe()
	if (state.editingCode ) { 
		state.editingCode = false
		state.codeStore[(state.editedCodeName)] = newCodeData
		log_info("(CodesPage) Updating stored code [${state.editedCodeName}]")
		state.editedCodeName = null
		app.clearSetting("newCodeData")
	}
	if ((state.addingCode) && ((newCodeName == null) || (newCodeName == "") || (newCodeData == null) || (newCodeData == ""))) {
		log_info("(CodesPage) Code entry cancelled")
		state.addingCode = false
		app.clearSetting("newCodeName")
		app.clearSetting("newCodeData")
	}
	if (state.addingCode) {
		if (state.codeStore == null) { state.codeStore = [:] }
		def codeName = newCodeName.tokenize(" ,!@#\$%^&()").join("_")
		def errorPage = null
		log_info("(CodesPage) Adding stored code [${codeName}=${newCodeData}]")
		if (state.codeStore[(codeName)] != null) {
			codeName = codeName + "_1"
		}
		if (state.codeStore[(codeName)] == null) {
			def addCode = newCodeData.toUpperCase()
			if (addCode.take(4) == "0000") {
				log_info("(CodesPage) Converting code from PRONTO format")
				addCode = convertProntoCode(addCode)
			} else if ((addCode[0] == "J") ||(addCode[-1] == "=") ||(addCode[-1] == "A")) { 
				log_info("(CodesPage) Converting code from Base64 Broadlink format")
				addCode = hubitat.helper.HexUtils.byteArrayToHexString(data.decodeBase64())
			}
			if ((addCode.take(4) == "2600") || (addCode.take(4) == "B200") || (addCode.take(4) == "D700")) {
				log_info("(CodesPage) Code in correct Broadlink format")
			} else {
				log_info("(CodesPage) Code [${codeName}] not added - unrecognized code format")
				addCode = null
				errorPage = dynamicPage(name:"CodePage", title: oTitle, install: false, uninstall: false){
					section("<h2>Code ERROR</h2>"){
						paragraph ""
						paragraph "<h2>Code not added.</h2>"
						paragraph "Code format not recognized"
					}
				}
			}

			if ((addCode != null) && (errorPage == null)) {
				// remove any trailing zero bytes
				while (addCode[-2..-1] == "00") { addCode = addCode[0..-3] }
				if (validateCode(hubitat.helper.HexUtils.hexStringToByteArray(addCode))) {
					log_info("(CodesPage) Added stored code [${codeName}]")
					state.codeStore[(codeName)] = addCode
				} else {
					log_info("(CodesPage) Code [${codeName}] not added - Validation failed")
					errorPage = dynamicPage(name:"CodePage", title: oTitle, install: false, uninstall: false){
						section("<h2>Code ERROR</h2>"){
							paragraph ""
							paragraph "<h2>Code not added.</h2>"
							paragraph "Code Validation failed"
						}
					}
				}
			}
		} else {
				log_info("(CodesPage) Code [${codeName}] not added - code already exists")
				errorPage = dynamicPage(name:"CodePage", title: oTitle, install: false, uninstall: false){
					section("<h2>Code ERROR</h2>"){
						paragraph ""
						paragraph "<h2>Code not added.</h2>"
						paragraph "Code already exists"
					}
				}
		}
		state.addingCode = false
		app.clearSetting("newCodeName")
		app.clearSetting("newCodeData")
		if (errorPage != null) { return errorPage }
	}
	state.learnStage = null
	
    return dynamicPage(name: "CodesPage", title: "IR/RF Code Manager", install:false, uninstall:false){
		def sDevice = null
		cDevs = getChildDevices()
		if (cDevs.size() > 0) {
			if (!params || !params.deviceNetworkId) { params = [deviceNetworkId: cDevs[0].deviceNetworkId, deviceTypeName: cDevs[0].name, deviceName: cDevs[0].label] }
			sDevice = "${params.deviceTypeName} - ${params.deviceName}  [${params.deviceNetworkId}]"
			state.learnDevice = params.deviceNetworkId
		} else {
			sDevice = "NO DEVICES CONFIGURED"
		}
		section("<h2>Selected learn Device: ${sDevice}</h2>"){
			if (cDevs.size() > 1) {
				cDevs.sort({ a, b -> a.name <=> b.name }).each{
					href (name: "learnCodesProcess", title: "${it.name} - ${it.label}",
					  description: "Click to select",
					  params: [deviceNetworkId: it.deviceNetworkId, deviceTypeName: it.name, deviceName: it.label],
					  page: "CodesPage")
				}
			}
		}
		section("<h2>Management functions</h2>") {
			if (cDevs.size() > 0) {
				href(name: "learn_IR",title: "Learn IR code",required: false,page: "learnCodes",description: "tap to Learn IR code", params: [operation: "learnIR", learnDevice: params.deviceNetworkId])
				href(name: "learn_RF",title: "Learn RF code",required: false,page: "learnCodes",description: "tap to Learn RF code", params: [operation: "learnRF", learnDevice: params.deviceNetworkId])
			}
			href(name: "manual_Codes",title: "Manually add IR/RF codes",required: false,page: "importCodes",description: "tap to manually enter IR/RF codes")
			if (cDevs.size() > 0) {
				href(name: "sync_Codes",title: "Syncronize Remote codes",required: false,page: "exportCodes",description: "tap to syncronize IR/RF codes to configured devices")
				if (!AutomaticCodeImport) {
					input "importDeviceCodesBTN", "button", title: "Import Stored codes from child devices"
				}
			}
        }
		def iMsg = ""
		if (AutomaticCodeImport || (state.importingDeviceCodes == true)) {
			state.importingDeviceCodes = false
			if (cDevs.size() > 0) { 
				iCount = importDeviceCodes() 
				if (iCount > 0) { iMsg = "\t\t( ${iCount} codes imported)" }
			}
		}
		section("<h2>Available codes</h2>${iMsg}") {
			log_info("(listCodes)")

//			state.codeStore.sort({ a, b -> a.key <=> b.key }).each{
			foundCodes = 0
			state.codeStore?.sort()?.each{
				log_info("(listCodes)  code [${it.key}]")
				def code = it.value
				def tCode = code[0..1].toUpperCase()
				def cType = (tCode == "26") ? "IR code" : (tCode == "B2") ? "433MHz RF code" : (tCode == "D7") ? "315MHz RF code" : "[UNKNOWN CODE]"
				foundCodes += 1
				href (name: "codeEditPage", title: "${it.key} - ${cType}",
				  description: "Click to edit",
				  params: [codeName: it.key],
				  page: "codeEditPage")
			}
			
		}
    }
}

def codeEditPage(params = null) {
	log_debug("(codeEditPage) params [${params}]")
	if (!params) {return}
	def activeCode = null
	if (state.codeStore[(params.codeName)]) { activeCode = [key: params.codeName, value: state.codeStore[(params.codeName)]] }
	log_info("(codeEditPage) activeCode [${activeCode}]")
	if (activeCode) {
		state.editingCode = true
		state.editedCodeName = params.codeName
		log.debug("(codeEditPage) editing activeCode: [${activeCode}] ")
		app.clearSetting("newCodeData")
		dynamicPage(name: "codeEditPage", title: ""){
			section("<h1>Edit an IR/RF code</h1>"){
				paragraph "Code Name: ${activeCode.key}"
				paragraph ""
				input "newCodeData", "textbox", title: "Code Data", required: true, multiple: false, defaultValue: activeCode.value, submitOnChange: false
				paragraph ""
				input "testStoredCodeBTN", "button", title: "test Stored code"
				paragraph ""
				input "removeStoredCodeBTN", "button", title: "Remove Stored code"
			}
		}
	} else {
		log.debug("(codeEditPage) no code to edit")
		return CodesPage()
	}
}

def learnCodes(params = null) {
	log_debug("(learnCodes) params [${params}]  learnStage [${state.learnStage}]  learnResult [${state.learnResult}]  newCodeName [${newCodeName}]")
	if (params == null) { log.debug("(learnCodes) params are NULL"); return }
	def oTitle = (params.operation == "learnIR")?"Learn IR Code":"Learn RF Code"
	def sDevice = getChildDevice(params.learnDevice)
	if (state.learnStage == null) {
		state.learnStage = "Starting learn process"
		state.learnResult = null
		unsubscribe()
		if (params.operation == "learnIR") {
			log.warn("(learnCodes) Starting IR learn process with device [${sDevice.deviceNetworkId}].")
			subscribe(sDevice,"IR_Status","handle_learn_status")
			sDevice.learnIR()
		} else {
			log.warn("(learnCodes) Starting RF learn process with device [${sDevice.deviceNetworkId}].")
			subscribe(sDevice,"RF_Status","handle_learn_status")
			sDevice.learnRF()
		}
		state.CODEDATA = null
		subscribe(sDevice,"CODEDATA","handle_learn_status")
	}
	def sDeviceDesc = "${sDevice.name} - ${sDevice.label}  [${sDevice.deviceNetworkId}]"
	if (state.learnResult == null) {
		return dynamicPage(name:"learnCodes", title: oTitle, install: false, uninstall: false, refreshInterval : 1){
			section("<h2>Selected Device: ${sDeviceDesc}</h2>"){
				paragraph ""
				paragraph ""
				paragraph "<h2>${state.learnStage}</h2>"
			}
		}
	} else if (state.learnResult == "failed") {
		unsubscribe()
		state.learnStage = null
		state.learnResult = null
		return dynamicPage(name:"learnCodes", title: oTitle, install: false, uninstall: false){
			section("<h2>Selected Device: ${sDeviceDesc}</h2>"){
				paragraph ""
				paragraph "<h2>Learn failed.</h2>"
			}
		}
	} else if (state.learnResult == "learned") {
		unsubscribe()
		state.learnStage = null
		state.learnResult = null
		app.clearSetting("newCodeName")
		app.clearSetting("newCodeData")
		state.addingCode = true
		return dynamicPage(name:"learnCodes", title: oTitle, install: false, uninstall: false){
			section("<h2>Selected Device: ${sDeviceDesc}</h2>"){
				paragraph ""
				paragraph "<h2>Learn Complete.</h2>"
				paragraph ""
				input "newCodeName", "text", title: "Code Name", required: true, multiple: false, defaultValue: "", submitOnChange: false
				paragraph ""
				input "newCodeData", "textbox", title: "Code Data", required: true, multiple: false, defaultValue: state.CODEDATA, submitOnChange: false
				paragraph ""
			}
		}
	}
}

def handle_learn_status(evnt) {
	log.warn("(handle_learn_status) received ir status [${evnt.source}] [${evnt.name}] [${evnt.value}]")
	def eStatus = null
	if (evnt.name == "RF_Status") {
		if (evnt.value.contains(":")) { eStatus = evnt.value.tokenize(":")[2] } else { eStatus = evnt.value }
	} else if (evnt.name == "IR_Status") {
		if (evnt.value.contains(":")) { eStatus = evnt.value.tokenize(":")[1] } else { eStatus = evnt.value }
	} else if (evnt.name == "CODEDATA") {
		unsubscribe()
		state.learnResult = "learned"
		state.CODEDATA = (evnt.value == "") ? null : evnt.value
		eStatus = "IDLE"
	}
	if (eStatus == "TIMEOUT") {
		unsubscribe()
		state.learnResult = "failed"
	}
	if ((eStatus == "IDLE") && (state.learnResult == null)) {
		state.learnResult = "failed"
	} else if (eStatus == "IDLE") {
		unsubscribe()
	}
	state.learnStage = eStatus
	log.warn("(handle_learn_status) learnStage [${state.learnStage}]  learnResult [${state.learnResult}]")
}

def learnCodeProcess(params = null) {
	log_debug("(learnCodeProcess) params [${params}]")
	return dynamicPage(name:"learnCodes", title:"Learn IR/RF Codes", install: false, uninstall: false){
		section("<h2>learnCodeProcess placeholder</h2>"){
		}
	}
}

def importCodes() {
	log_debug("(importCodes)")
	state.addingCode = true
	log.debug("(codeEditPage) editing activeCode: [${activeCode}] ")
	app.clearSetting("newCodeName")
	app.clearSetting("newCodeData")
	return dynamicPage(name: "importCodes", title: "Manually enter IR/RF Codes", install: false, uninstall: false){
		section("<h1>Enter an IR/RF code</h1>"){
			input "newCodeName", "text", title: "Code Name", required: false, multiple: false, defaultValue: "", submitOnChange: false
			paragraph ""
			input "newCodeData", "textbox", title: "Code Data", required: false, multiple: false, defaultValue: "", submitOnChange: false
			paragraph ""
			paragraph "To exit (cancel code entry), leave both input fields empty and click \"Done\""
		}
	}
}

def getCode(codeName) {
	// code query from child driver
	codeName = codeName.tokenize(" ,!@#\$%^&()").join("_")
	if (state.codeStore[(codeName)] == null) {
		log_info("(getCode) ERROR: A code is not stored under this name.")
	} else {
		log_info("(getCode) Returning data for code [${codeName}]")
		return state.codeStore[(codeName)]
	}
}

def exportCodes() {
	// send codes stored in app to configured devices
	log_debug("(exportCodes)")
	getChildDevices().each{ dev ->
		dev.importCodes(state.codeStore)
	}
	return dynamicPage(name:"exportCodes", title:"Syncronize IR/RF Codes", install: false, uninstall: false){
		section(""){
			paragraph "All IR/RF codes have been syncronized with all configured devices"
		}
	}
}

def importDeviceCodes() {
	// import learned codes from child devices
	def codesImported = 0
	log_debug("(importDeviceCodes)")
	getChildDevices().each{ dev ->
		def storedCodes = dev.exportCodes()
		storedCodes.each { code ->
			if ( !state.codeStore[code.key] ) {
				// add code stored on the device to the app codeStore
				state.codeStore[code.key] = code.value
				codesImported = codesImported + 1
			}
		}
	}
	return codesImported
}

def DiscoveryPage() 
{
	log_debug("(DiscoveryPage)")
	
	if (state.configuringDevice ) { configureDevice() }
	if (state.editingDevice ) { editDevice() }

	
	findDevices()

	return dynamicPage(name:"DiscoveryPage", title:"Broadlink Device Manager", install: false, uninstall: false){

		section("<h2>Available Devices</h2>"){
			def devCount = 0
			getDevices().sort({ a, b -> a.value.Name <=> b.value.Name }).each{
				def statusMsg = ""
				def hPage = "configureDevicePage"
				def hDesc = "Click to configure"
				if ( !getChildDevice(it.value.MAC) ) { 
					if (it.value.cloudLocked) { statusMsg = "(UNUSABLE - CLOUD LOCKED)" }
					if (!it.value.supported) { statusMsg += " (Unsupported device)" }
					devCount = devCount + 1
					href (name: "configureDevicePage", title: "${it.value.devTypeName} - ${it.value.Name}    ${statusMsg}",
					  description: "[${it.value.MAC}] Click to configure",
					  params: [device: it.value],
					  page: "configureDevicePage")
				}
			}
			if (devCount == 0) {
				paragraph "  No unconfigured Broadlink devices found"
			}
		}

		section("<h2>Configured Devices</h2>"){
			getChildDevices().sort({ a, b -> a.name <=> b.name }).each{
				log.warn("(DiscoveryPage) Processing child device  [${it.deviceNetworkId}]")
				def updateMSG = ""
				if (it && it.currentValue("deviceConfig")) {
					def deviceConfig = parseJson(it.currentValue("deviceConfig"))
					def cIP = deviceConfig.IP
					def nIP = getDevice(deviceConfig.MAC)?.IP
					log.warn("(DiscoveryPage) Already configured device [${deviceConfig.MAC}] - current IP [${cIP}]  new IP [${nIP}]")
					updateMSG = ""
					if ((cIP != nIP) && (nIP != null)) {
						// IP address has changed
						log.warn("(DiscoveryPage) Already configured device [${deviceConfig.MAC}] - IP address has changed from [${cIP}] to [${nIP}]")
						it.updateSetting("destIp", nIP)
						deviceConfig.IP = nIP
						it.reset()
						it.updated()
						updateMSG = "\t\tDevice IP Address updated"
					}
				} else {
					updateMSG = " (driver does not have deviceConfig data)"
				}
				if (!getDevice(it.deviceNetworkId)) {
					updateMSG = " (Missing device)"
				}
				def dCodes = it.exportCodes()
				def nRepeats = it.getSettings().codeRepeats
				href (name: "editDevicePage", title: "${it.name} - ${it.label}  (Repeats = ${nRepeats},  ${dCodes.size()} Stored IR/RF Codes)    ${updateMSG}",
					  description: "[${it.deviceNetworkId}] Click to edit",
					  params: [deviceNetworkId: it.deviceNetworkId],
					  page: "editDevicePage")
			}
		}

	}
}

def configureDevicePage(params) {
	if (params == null) { log_debug("(configureDevicePage) params are NULL"); return }
	log_debug("(configureDevicePage) params [${params}]")
	params = params.device
	if (params.cloudLocked) {
		return dynamicPage(name: "configureDevicePage", title: ""){
			section("<h1>Configure a device</h1>"){
				paragraph "This device is cloud locked and can not be used."
				paragraph "Type: ${params.devType}\nType Name: ${params.devTypeName}\nIP: ${params.IP}\nMAC: ${params.MAC}\n"
			}
		}
	}
	if (!params.supported) {
		return dynamicPage(name: "configureDevicePage", title: ""){
			section("<h1>Configure a device</h1>"){
				paragraph "This device is not supported and can not be used."
				paragraph "Type: ${params.devType}\nType Name: ${params.devTypeName}\nIP: ${params.IP}\nMAC: ${params.MAC}\n"
			}
		}
	}
	state.configuringDevice = true
	app.clearSetting("configureDeviceName")
	state.configuringDeviceData = params
	dynamicPage(name: "configureDevicePage", title: ""){
		section("<h1>Configure a device</h1>"){
			paragraph "Create a Hubitat device for a discovered Broadlink Device"
			paragraph "Type: ${params.devTypeName}\nIP: ${params.IP}\nMAC: ${params.MAC}\n"
			input "configureDeviceName", "text", title: "Device Name", required: true, multiple: false, defaultValue: params.Name, submitOnChange: false
			input ("codeRepeats", "enum", title: "Select number of code repeats", required: false, multiple: false, options: ["0", "1", "2", "3", "4", "5"], defaultValue: "0", submitOnChange: false)
		}
	}
}

def configureDevice(params = null) {
	log_debug("(configureDevice) device [${state.configuringDeviceData}]  configureDeviceName [${configureDeviceName}]  codeRepeats [${codeRepeats}]")
	params = state.configuringDeviceData
	if (configureDeviceName != "") { params.Name = configureDeviceName }
	// check for device by MAC
	log_info("(configureDevice) Checking for device: ${params.MAC}")
	def isChild = getChildDevice(params.MAC)
	if (!isChild) {
		log_info("(configureDevice) Attempting to add device for: ${params.MAC}")
		try {
			def newDevice = addChildDevice(
				"cybr",
				"Broadlink (BETA)",
				params.MAC,
				location.hub.id,
				[
					"label" : params.Name,
					"name" : params.devTypeName,
					isComponent: true
				]
			)
			newDevice.updateSetting("destIp",[type:"text", value: params.IP])
			newDevice.updateSetting("codeRepeats",[type:"enum", value: codeRepeats])
			newDevice.updated()
			log_debug("(configureDevice) Added device: ${params.MAC} (${params.devTypeName}) - (${params.IP}) ${params.Name}")
		} catch (error) {
			log_debug("(configureDevice) Failed to install ${params.MAC}.  Driver most likely not installed.\r${error}")
		}
	} else {
		log_debug("(configureDevice) Device DNI ${params.MAC} already exists")
	}
	state.configuringDevice = false
	state.configuringDeviceData = null
	app.clearSetting("configureDeviceName")
	app.clearSetting("codeRepeats")
	return DiscoveryPage()
}

def editDevicePage(params) {
	log_debug("editDevicePage - params [" + (params?:"NULL") + "]")
	if (!params) {return}
	def deleteOnly = false
	def activeDevice = getChildDevice(params.deviceNetworkId)
	if (activeDevice) {
		state.editingDevice = true
		state.editedDeviceDNI = params.deviceNetworkId
		def devInfo = getDevice(params.deviceNetworkId)
		if (devInfo == null) {
			log.warn("editDevicePage - rogue device [${state.deviceNetworkId}]")
			deleteOnly = true
		} else {
			log.debug("(editDevicePage) editing activeDevice: [${devInfo}] ")
		}
		def nCodes = activeDevice.exportCodes().size()
		def nRepeats = activeDevice.getSettings().codeRepeats
		log.debug("EDITDEVICEPAGE - nRepeats [${nRepeats}]")
		app.clearSetting("newDeviceLabel")
		app.clearSetting("newCodeRepeats")
		dynamicPage(name: "editDevicePage", title: ""){
			section("<h1>Edit a Broadlink device</h1>"){
				if (deleteOnly) {
					paragraph ""
					paragraph "This device [${params.deviceNetworkId}] does not appear in the latest device discovery scan."
					paragraph "Click on \"Remove Device\" to delete this device."
					paragraph "Click on \"Done\" to cancel."
					paragraph ""
				} else {
					paragraph "${devInfo.devTypeName}\n${devInfo.MAC}\n${devInfo.IP}\n${nCodes} Stored IR/RF Codes\nCode repeats: ${nRepeats}"
					paragraph ""
					input "newDeviceLabel", "text", title: "Zone Label", required: true, multiple: false, defaultValue: activeDevice.label, submitOnChange: false
					paragraph ""
					input ("newCodeRepeats", "enum", title: "Select number of code repeats", required: false, multiple: false, options: ["0", "1", "2", "3", "4", "5"], defaultValue: nRepeats, submitOnChange: false)
				}
				input "removeDeviceBTN", "button", title: "Remove Device"
				if ((nCodes > 0) && (deleteOnly = false)) {
					input "removeCodesBTN", "button", title: "Remove Stored codes"
				}
			}
		}
	} else {
		log.debug("(editDevicePage) no device to edit")
		return DiscoveryPage()
	}
}

def appButtonHandler(BTN) {
	if (BTN == "removeDeviceBTN") {
		def childDevice = getChildDevice(state.editedDeviceDNI);
		if (childDevice) {
			log.debug("(appButtonHandler) Attempting removal of device: [${state.editedDeviceDNI}]")
			deleteChildDevice(childDevice.deviceNetworkId)
			log.debug("(appButtonHandler) Removed device: [${state.editedDeviceDNI}]")
		}
		state.editingCode = false
		state.editedCodeName = null
	} else if (BTN == "removeCodesBTN") {
		def childDevice = getChildDevice(state.editedDeviceDNI);
		if (childDevice) {
			childDevice.eraseCodes()
			log.debug("(appButtonHandler) Removed all Stored codes: [${state.editedDeviceDNI}]")
		}
	} else if (BTN == "testStoredCodeBTN") {
		log.debug("(appButtonHandler) Attempting to send Stored code: [${state.editedCodeName}] [${state.learnDevice}]")
		if (state.learnDevice) {
			def sDevice = getChildDevice(state.learnDevice)
			log.debug("(appButtonHandler) Sending Stored code: [${state.editedCodeName}] to [${sDevice}]")
			sDevice.SendStoredCode(state.editedCodeName)
		}
	} else if (BTN == "removeStoredCodeBTN") {
		state.codeStore.remove(state.editedCodeName)
		log.debug("(appButtonHandler) Removed Stored code: [${state.editedCodeName}]")
		state.editingCode = false
		state.editedCodeName = null
	} else if (BTN == "importDeviceCodesBTN") {
		state.importingDeviceCodes = true
	}
}

def editDevice(){
	log_debug("(editDevice) newDeviceLabel [${newDeviceLabel}]  codeRepeats [${newCodeRepeats}]")
	def childDevice = getChildDevice(state.editedDeviceDNI);
	if (childDevice) {
		def childDeviceLabel = childDevice.getDisplayName()
		log.debug("(editDevice) Attempting edit of device: [${state.editedDeviceDNI}]")
		childDevice.setDisplayName(newDeviceLabel)
		log.debug("(editDevice) Changed device label: [${childDeviceLabel}] => [${newDeviceLabel}]")
		childDevice.updateSetting("codeRepeats",[type:"enum", value: newCodeRepeats])
		childDevice.updated()
	}
	app.clearSetting("newDeviceLabel")
	app.clearSetting("newCodeRepeats")
	state.editingDevice = false
	state.editedDeviceDNI = null
//	return DiscoveryPage()
}

// Returns a list of the found Broadlink devices from discovery scan
def getDevices()
{
	log_info("(getDevices)")
	state.broadlinkDevices = state.broadlinkDevices ?: [:]
	return state.broadlinkDevices
}

// Returns a list of the found Broadlink devices from discovery scan
def getDevice(rDev)
{
	def ret = state.broadlinkDevices.find { it.value.MAC == rDev }
	log_info("(getDevice) request [${rDev}] = [${ret?.value}]")
	return ret?.value
}

def getChecksum(packet) {
	def checksum = 0xbeaf
	for (i = 0; i < packet.size(); i++) { 
		checksum = checksum + Byte.toUnsignedInt(packet[i])
		checksum = checksum  & 0xFFFF
	}
	return checksum
}

private findDevices()
{
	log_info("findDevices - discovering broadlink devices")
	state.broadlinkDevices = [:]
	def hIP = location.hubs[0].getDataValue("localIP").tokenize( '.' )
	for (idx = 1; idx < 255; idx++) {
		hIP[3] = idx
		def tIP = hIP.join(".")
		findDevice(tIP)
	}
	pauseExecution(1000)
	log_info("findDevices - discovery done")
}

private findDevice(destIP) 
{
//	log.debug("findDevice - testing broadlink device at ${destIP}")

	def hub = location.hubs[0]
	def localIP = hub.getDataValue("localIP")
	def hIP = localIP.tokenize( '.' )
	
	def ts = now()
	def DATE = new Date()
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

	def Action = new hubitat.device.HubAction(
		packetData, 
		hubitat.device.Protocol.LAN, 
		[
			callback: "parseDiscoveryPacket",
			destinationAddress: "${destIP}",
			destinationPort: 80,
			type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, 
			encoding: hubitat.device.HubAction.Encoding.HEX_STRING,
			ignoreWarning: true
		]
	)
	try {
//		log.debug("findDevices - SENDING discovery [${Action.inspect()}]")
		sendHubCommand(Action)
	} catch(e) {
		log.error("(findDevices) ERROR - Caught exception '${e}'")
	}
}

def getString(str) {
	def rStr = ""
	def gotNull = false
	str.eachWithIndex { it, i -> 
		if ((it == 0) || (it > 0x80) || (it < 0)) { gotNull = true }
		if (gotNull == false) { rStr = rStr + new String(it) }
	}
//	log_debug("getString() = \"${rStr}\"")
	return rStr
}

def getDeviceTypeName(devtype) {
	def devTypeName = "Unknown device (${String.format('0x%04X',devtype.toInteger())})"
	if (devtype == 0) { devTypeName = "SP1"}
	else if (devtype == 0x13b) { devTypeName = "RM315"}
	else if (devtype == 0x1b1) { devTypeName = "RM433"}
	else if (devtype == 0x26) { devTypeName = "RMIR"}
	else if (devtype == 0x2710) { devTypeName = "RM1"}
	else if (devtype == 0x2711) { devTypeName = "SP2"}
	else if (devtype == 0x2712) { devTypeName = "RM2"}
	else if (devtype == 0x2714) { devTypeName = "A1"}
	else if ((devtype == 0x2719) || (devtype == 0x271a)) { devTypeName = "Honeywell SP2"}
	else if (devtype == 0x271f) { devTypeName = "RM2 Home Plus"}
	else if (devtype == 0x2720) { devTypeName = "SPMini"}
	else if (devtype == 0x2728) { devTypeName = "SPMini2"}
	else if (devtype == 0x272a) { devTypeName = "RM2 Pro Plus"}
	else if ((devtype == 0x2733) || (devtype == 0x273e)) { devTypeName = "OEM branded SPMini"}
	else if (devtype == 0x2736) { devTypeName = "SPMiniPlus"}
	else if (devtype == 0x2737) { devTypeName = "RM Mini"}
	else if (devtype == 0x273d) { devTypeName = "RM Pro Phicomm"}
	else if (devtype == 0x277c) { devTypeName = "RM2 Home Plus GDT"}
	else if (devtype == 0x2783) { devTypeName = "RM2 Home Plus"}
	else if (devtype == 0x2787) { devTypeName = "RM2 Pro Plus2"}
	else if (devtype == 0x278b) { devTypeName = "RM2 Pro Plus BL"}
	else if (devtype == 0x278f) { devTypeName = "RM Mini Shate"}
	else if (devtype == 0x2797) { devTypeName = "RM2 Pro Plus HYC"}
	else if (devtype == 0x279d) { devTypeName = "RM3 Pro Plus"}
	else if (devtype == 0x27a1) { devTypeName = "RM2 Pro Plus R1"}
	else if (devtype == 0x27a2) { devTypeName = "RM Mini R2"}
	else if (devtype == 0x27a6) { devTypeName = "RM2 Pro PP"}
	else if (devtype == 0x27a9) { devTypeName = "RM2 Pro Plus 300 / RM3 Pro Plus v2 model 3422"}
	else if (devtype == 0x27c2) { devTypeName = "RM Mini 3 B"}
	else if (devtype == 0x27c7) { devTypeName = "RM Mini 3 A"}
	else if (devtype == 0x27de) { devTypeName = "RM Mini 3 C"}
	
	else if (devtype == 0x51da) { devTypeName = "RM4b"}
	else if (devtype == 0x5f36) { devTypeName = "RM Mini 3 (V4)"}
	else if (devtype == 0x610f) { devTypeName = "RM4c (RM Mini 4 vA)"}
	else if (devtype == 0x62be) { devTypeName = "RM4c (RM Mini 4 vB)"}
	
	else if (devtype == 0x4EB5) { devTypeName = "MP1"}
	else if (devtype == 0x5f36) { devTypeName = "RM Mini 3 D"}
	else if ((devtype == 0x7530) || (devtype == 0x7918)) { devTypeName = "OEM branded SPMini2"}
	else if (devtype == 0x753e) { devTypeName = "SP3" }
	else if ((devtype > 0x752F) && (devtype < 0x7919)) { devTypeName = "OEM branded SPMini2"}
	else if ((devtype == 0x7919) || (devtype == 0x791a)) { devTypeName = "Honeywell SP2"}
	return devTypeName
}

private String arrayListToHexString(aList) {
	String hex = aList.collect { it ->
		def bValue = it.toInteger(); if (bValue < 0) { bValue = bValue + 256 }
		String.format( '%02X', bValue) 
	}.join()
	return hex
}

def parseDiscoveryPacket(packet) {
	log_info("parseDiscoveryData - received description: ${packet}")
	def resp = parseLanMessage(packet.description)
	if (resp == null) {log.debug("parseDiscoveryPacket: Could not parse packet."); return }
	byte [] payload = hubitat.helper.HexUtils.hexStringToByteArray(resp.payload)
	def dTypeID = Integer.parseInt(arrayListToHexString([payload[0x35],payload[0x34]]),16)
	resp.mac = (resp.mac != "null") ? resp.mac : arrayListToHexString([payload[0x3f],payload[0x3e],payload[0x3d],payload[0x3c],payload[0x3b],payload[0x3a]])
	def blDevice = [:]
	blDevice["Name"] = (getString(payload[0x40..-1]) == "") ? "[UNNAMED]" : getString(payload[0x40..-1])
	blDevice["MAC"] = resp.mac
	blDevice["IP"] = (resp.ip != "ffffffff") ? convertHexToIP(resp.ip) : convertHexToIP(arrayListToHexString([payload[0x39],payload[0x38],payload[0x37],payload[0x36]]))
	blDevice["devType"] = dTypeID
	blDevice["devTypeName"] = getDeviceTypeName(dTypeID)
	blDevice["cloudLocked"] = (payload[0x7f] == 0) ? false : true
	blDevice["supported"] = (blDevice["devTypeName"].contains("Unknown device")) ? false : true
	state.broadlinkDevices << ["${resp.mac}" : blDevice]
	log_info("(parseDiscoveryData) state.broadlinkDevices: ${state.broadlinkDevices}")
}

private Integer convertHexToInt(hex) 
{
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) 
{
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPtoHex(ipAddress) 
{ 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    trace("IP address entered is $ipAddress and the converted hex code is $hex")
    return hex
}

private String convertPortToHex(port) 
{
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}
private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

def uninstalled() {
	removeChildDevices(getChildDevices())
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
			if ((codeData[-2] != 0x0d) || (codeData[-1] != 0x05)) {
				if (codeData.size() == (cLength +4)) {
					// code was stripped of lead out data before being encoded
					log_info("(validateCode) Valid code - WARNING:  IR lead-out removed")
					return true
				}
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

def getOptions() {
	return [emulateResponse: emulateResponse, useAirTempSensors: useAirTempSensors, periodicUpdates: periodicUpdates, updatePeriod: updatePeriod]
}

def log_debug(msg) { if (debug) log.debug(msg) }

def log_info(msg) { if (debug && debugVerbose) log.info(msg) }

/**
 *  Broadlink Device Manager (BETA) for Hubitat
**/