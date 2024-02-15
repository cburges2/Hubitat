/*
	Echo Speasks Features Driver

	Copyright 2023 -> C. Burgess

*/

metadata {
	definition (
			name: "Echo Speaks Features Driver",
			namespace: "chrisb",
			author: "Chris B"
	) {
		capability "Actuator"

        attribute "feature", "ENUM"  
        attribute "speakText", "STRING"   
		attribute "cmdText", "STRING"
        attribute "whichEcho", "ENUM"
		attribute "cmdEcho", "ENUM"
		attribute "monkeyEcho", "ENUM"
		attribute "monkeyText", "STRING"
		attribute "monkeyVoice", "ENUM"
		attribute "routineEcho", "ENUM"
		attribute "routineID", "STRING"

        command "setFeature", [[name:"feature",type:"ENUM", description:"Set Feature to Run", constraints:["Current Mode","Greeting","Wise Words","Something About","Pets","Home Status","Mailbox Status","Goodbye","Good Nap","Goodnight"]]]
        //command "setSpeakDevice", [[name:"speakText",type:"STRING", description:"Set Speak Text"],[name:"whichEcho",type:"ENUM", description:"Set Echo Device", constraints:["all","both","office","bedroom","front"]]]
        command "setSpeakDevice", [[name:"whichEcho",type:"ENUM", description:"Set Echo Speak Device", constraints:["office","bedroom","front","all","both","updown"]],[name:"speakText",type:"STRING", description:"Set Speak Text"]]
		command "setVoiceCmdDevice",[[name:"cmdEcho",type:"ENUM", description:"Set Echo Command Device", constraints:["office","bedroom","front","all","both","updown"]],[name:"cmdText",type:"STRING", description:"Set Command Text"]]
		command "setVoiceMonkeyDevice",[[name:"monkeyEcho",type:"ENUM", description:"Set Echo Command Device", constraints:["office","bedroom","front","all","both","updown","tree"]],[name:"monkeyText",type:"STRING", description:"Set Voice Monkey Text"],
		[name:"monkeyVoice",type:"ENUM", description:"Set Monkey Voice", constraints:["Alexa","Brian","Amy","Matthew","Joanna"]]]
		command "runRoutineID", [[name:"routineEcho",type:"ENUM", description:"Set Echo Routine Device", constraints:["office","bedroom","front"]],[name:"routineID",type:"STRING", description:"Set Routine ID"]]
}
// [name:"whichEcho",type:"ENUM", description:"Set Echo Device", constraints:["All","Both","office","bedroom","front"],name:"speakText",type:"STRING"]
	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
        input name: "autoOff", type: "enum", description: "Automatically sets motion inactive after selected time.", title: "Enable Auto-inactive", options: [[0:"Disabled"],[1:"1s"],[2:"2s"],[5:"5s"],[10:"10s"],[20:"20s"],[30:"30s"],[60:"1m"],[120:"2m"],[300:"5m"],[600:"10m"],[1800:"30m"],[3200:"60m"]], defaultValue: 0
	}
}

def installed() {
	log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true])
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
   
}

def setFeature(feature) {
	logDebug "setFeature(${feature}) was called"
    sendEvent(name: "feature", value: feature, descriptionText: getDescriptionText("feature set to ${feature}"))
    runIn(3, resetFeature)
}

def resetFeature() {
    sendEvent(name: "feature", value: "idle", descriptionText: getDescriptionText("feature set to idle"))
}

def setSpeakAll(evt) {
    logDebug("Speak All called ${evt.value}")
    
    sendEvent(name: "speakText", value: evt.value, descriptionText: getDescriptionText("speakText set to ${evt.value}"))
    runIn(3,resetSpeakText)
}

// Echo Speak
def setSpeakDevice(which, speak) {
    logDebug("Set Speak Text called with ${speak} and ${which}")

    def text = speak
    def device = which

    logDebug(text)
    logDebug(device)

    sendEvent(name: "speakText", value: text, descriptionText: getDescriptionText("speakText set to ${text}"))
    sendEvent(name: "whichEcho", value: device, descriptionText: getDescriptionText("whichEcho set to ${device}"))

    runIn(3,resetWhichEcho)
}

def resetWhichEcho() {
    sendEvent(name: "whichEcho", value: "idle", descriptionText: getDescriptionText("whichEcho set to idle"))
}

// Voice Monkey
def setVoiceMonkeyDevice(which, text, voice) {
    logDebug("Set Speak Text called with ${speak}, ${which} and ${voice}")

    sendEvent(name: "monkeyText", value: text, descriptionText: getDescriptionText("monkeyText set to ${text}"))
    sendEvent(name: "monkeyEcho", value: which, descriptionText: getDescriptionText("monkeyEcho set to ${which}"))
	sendEvent(name: "monkeyVoice", value: voice, descriptionText: getDescriptionText("monkeyVoice set to ${voice}"))

    runIn(3,resetMonkeyEcho)
}

def resetMonkeyEcho() {
    sendEvent(name: "monkeyEcho", value: "idle", descriptionText: getDescriptionText("monkeyEcho set to idle"))
}

// Echo Voice Command
def setVoiceCmdDevice(which, text) {
    logDebug("Set VoiceCmdDevice called with ${text} and ${which}")

    def device = which

    logDebug(text)
    logDebug(device)

    sendEvent(name: "cmdText", value: text, descriptionText: getDescriptionText("cmdText set to ${text}"))
    sendEvent(name: "cmdEcho", value: device, descriptionText: getDescriptionText("cmdEcho set to ${device}"))

    runIn(3,resetCmdEcho)
}

def resetCmdEcho() {
    sendEvent(name: "cmdEcho", value: "idle", descriptionText: getDescriptionText("cmdEcho set to idle"))
}

def runRoutineID(which, routine) {
	logDebug("runRoutineID called with ${which} and ${routine}")

    sendEvent(name: "routineEcho", value: which, descriptionText: getDescriptionText("routineEcho set to ${which}"))
    sendEvent(name: "routineID", value: routine, descriptionText: getDescriptionText("routineID set to ${routine}"))	

	rnIn(3,resetRoutineEcho)
}

def resetRoutineEcho() {
    sendEvent(name: "routineEcho", value: "idle", descriptionText: getDescriptionText("routineEcho set to idle"))
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}