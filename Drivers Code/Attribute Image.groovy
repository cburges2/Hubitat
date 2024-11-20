/*

Data Storage Driver -Attribute Image

c. 2024 Chris B

v 1.0 - 11/19/24 - Save three images as attributes for use on dashboards

*/

metadata {
	
	definition (
			name: "Attribute Image",
			namespace: "hubitat",
			author: "Chris B."			
	) { 
		capability "Actuator"

        attribute "image1", "string"
		attribute "image2", "string"
		attribute "lastFile1", "string"
		attribute "lastFile2", "string"
		attribute "lastFile3", "string"

        command "setImage1", ["string"]
		command "setImage2", ["string"]
		command "setImage3", ["string"]
	}

	preferences {
		
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)      
		input( name: "ipAddress", type:"STRING", title: "Enter Hub IP Address")  
	}
}


def installed() {
	log.warn "installed..."
	initialize()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	log.warn "description logging is: ${txtEnable == true}"
	if (logEnable) runIn(1800,logsOff)
	initialize()
}

def initialize() {
	if (state?.lastRunningMode == null) {
        
	}
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

// Commands needed to change internal attributes of virtual device.

def setImage1(image) {
	logDebug "setAttribImage() was called"
    String imagePath = "http://" + settings?.ipAddress + ":8080/local/"
    sendEvent(name: "image1", value: "<img class='image' src='${imagePath}${image}' />")
	sendEvent(name: "lastFile1", value: image, descriptionText: getDescriptionText("lastFile1 was set to ${file}"))
}

def setImage2(image) {
	logDebug "setAttribImage2() was called"
    String imagePath = "http://" + settings?.ipAddress + ":8080/local/"  
    sendEvent(name: "image2", value: "<img class='image' src='${imagePath}${image}' />")
	sendEvent(name: "lastFile2", value: image, descriptionText: getDescriptionText("lastFile2 was set to ${file}"))
}

def setImage3(image) {
	logDebug "setAttribImage3() was called"
    String imagePath = "http://" + settings?.ipAddress + ":8080/local/"  
    sendEvent(name: "image3", value: "<img class='image' src='${imagePath}${image}' />")
	sendEvent(name: "lastFile3", value: image, descriptionText: getDescriptionText("lastFile3 was set to ${file}"))
}

def parse(String description) {
	logDebug "$description"
}


private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}