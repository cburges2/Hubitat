/*
	Generic Component Virtual Switch
	Copyright 2016 -> 2020 Hubitat Inc. All Rights Reserved

    2025-10-26 - Chris B 
        - Forked to Generic Component Virtual Switch with the switch attribute added for use with a parent App with virtual switch control by user. 
          Added delayOff() command and offDelay prefrence, and an autoOff prefrence for offDelay to be used by the switch to act as a button. 
    2020-04-16 2.2.0 maxwell
        -refactor
	2018-12-15 maxwell
	    -initial pub
*/

metadata {
    definition(name: "Generic Component Virtual Switch", namespace: "hubitat", author: "mike maxwell", component: true) {
        capability "Switch"
        capability "Refresh"
        capability "Actuator"

        attribute "switch", "ENUM"
        command "delayOff"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "autoOff", type: "bool", title: "Use as a button device where switch will auto turn off (instead of using delayOff command)", defaultValue: false
        input name: "offDelay", type: "enum", description: "Off Delay with delayOff command or auto off", title: "Button Off Delay", options: [[100:"100msec"],[200:"200msec"],[300:"300msec"],[400:"400msec"],[500:"500msec"],[600:"600msec"],[700:"700msec"],[800:"800msec"]], defaultValue: 500
    }
}

void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    sendEvent(name: "switch", value: "off")
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List description) {
    description.each {
        if (it.name in ["switch"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
    }
}

void on() {
    sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch set to on"))
    if (autoOff) delayOff()
    parent?.componentOn(this.device)  
}

void off() {
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch set to off"))
    parent?.componentOff(this.device)
}

void delayOff() {
    log.info "Delay off is ${settings?.offDelay}"
    def delay = settings?.offDelay.toInteger()
    runInMillis(delay, off)
}

void refresh() {
    parent?.componentRefresh(this.device)
}

def getDescriptionText(msg) {
    def descriptionText = "${device.displayName} ${msg}"
    if (settings?.txtEnable) log.info "${descriptionText}"
    return descriptionText
}