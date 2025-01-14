/*
	Generic Component Switch
	Copyright 2016 -> 2020 Hubitat Inc. All Rights Reserved
    2020-04-16 2.2.0 maxwell
        -refactor
	2018-12-15 maxwell
	    -initial pub
    2025-01-14 chrisb
        - added autoOff()

*/

metadata {
    definition(name: "Generic Component Switch", namespace: "hubitat", author: "mike maxwell", component: true) {
        capability "Switch"
        capability "Refresh"
        capability "Actuator"
    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "autoReset", type: "enum", description: "Automatically turn off based on interval.", title: "Enable Auto-Off", options: [[0:"Disabled"],[1:"1sec"],[2:"2sec"],[3:"3sec"],[4:"4sec"],[5:"5hr"],[8:"8sec"],[10:"10sec"]], defaultValue: 0
    }
}

void updated() {
    log.info "Updated..."
    log.warn "description logging is: ${txtEnable == true}"
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
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
    parent?.componentOn(this.device)
    if (autoReset.toInteger()>0){      
        def secs = autoReset.toInteger()
        runIn(secs, off)
    }    
}

void off() {
    parent?.componentOff(this.device)
}

void refresh() {
    parent?.componentRefresh(this.device)
}