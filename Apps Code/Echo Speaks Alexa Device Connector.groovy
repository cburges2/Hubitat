/**
 *  ****************  Echo Speaks Alexa Device Connector  ****************
 *
 *  Usage:
 *  This was designed to control an Alexa device from Hubitat using the speak text command in Echo Speaks App to control Alexa Devices
 *
**/

definition (
    name: "Echo Speaks Alexa Device Connector",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for Alexa connected dimmer devices from Hubitat using Echo Speaks Text as Command feature",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {
lock
    dynamicPage(name: "mainPage") {

        section("<b>Echo Device for Commands</b>") {

            input (
              name: "echoDevice", 
              type: "capability.speechSynthesis", 
              title: "Select Echo Device to use to send commands", 
              required: true, 
              multiple: false            
            )
        }

        section("<b>Action Words for On Commands</b>") {
            input (
              name: "onAction", 
              type: "String", 
              title: "On Action",
              defaultValue: "Turn on",
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            state.onAction = onAction
        }         

        section("<b>Action Words for Off Commands</b>") {
            input (
              name: "offAction", 
              type: "String", 
              title: "Off Action", 
              defaultValue: "Turn off",
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            state.offAction = offAction
        }         

        section("<b>Name of Alexa Device</b>") {
            input (
              name: "alexaName", 
              type: "String", 
              title: "Alexa Device Name", 
              defaultValue: "Turn off",
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            state.alexaName = alexaName
        }    

        section("<b>Virtual Switch Device</b>") {

            input (
              name: "virtualSwitch", 
              type: "capability.switch", 
              title: "Select Virtual Switch Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )

            if (virtualSwitch) {
                input (
                    name: "trackVirtualSwitch", 
                    type: "bool", 
                    title: "Track Virtual Switch Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 
        }

        section("<b>Add More Devices</b>") {
            input (
              name: "selectController", 
              type: "bool", 
              title: "Turn on to add more devices", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController) {
                state.more1 = true
            } else {
                state.more1 =  false           
            }
        }

        if (state.more1) {

            section("<b>Virtual Switch Device 2</b>") {

                input (
                name: "virtualSwitch2", 
                type: "capability.switch", 
                title: "Select Virtual Switch Device", 
                required: true, 
                multiple: false,
                submitOnChange: true             
                )

                if (virtualSwitch2) {
                    input (
                        name: "trackVirtualSwitch2", 
                        type: "bool", 
                        title: "Track Virtual Switch2 Changes", 
                        required: true, 
                        defaultValue: "true"
                    )
                } 
            }

            section("<b>Add More Devices</b>") {
                input (
                name: "selectController2", 
                type: "bool", 
                title: "Turn on to add more devices", 
                required: true, 
                multiple: false,
                submitOnChange: true               
                )
                if (settings.selectController2) {
                    state.more2 = true
                } else {
                    state.more2 =  false           
                }
            }

            if (state.more2) {
                section("<b>Virtual Switch Device 3</b>") {

                    input (
                    name: "virtualSwitch3", 
                    type: "capability.switch", 
                    title: "Select Virtual Switch 3 Device", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true             
                    )

                    if (virtualSwitch2) {
                        input (
                            name: "trackVirtualSwitch3", 
                            type: "bool", 
                            title: "Track Virtual Switch3 Changes", 
                            required: true, 
                            defaultValue: "true"
                        )
                    } 
                }

                section("<b>Add More Devices</b>") {
                    input (
                    name: "selectController3", 
                    type: "bool", 
                    title: "Turn on to add more devices", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true               
                    )
                    if (settings.selectController3) {
                        state.more3 = true
                    } else {
                        state.more3 =  false           
                    }
                }     

                if (state.more3) {  
                    section("<b>Virtual Switch Device 4</b>") {

                        input (
                        name: "virtualSwitch4", 
                        type: "capability.switch", 
                        title: "Select Virtual Switch 4 Device", 
                        required: true, 
                        multiple: false,
                        submitOnChange: true             
                        )

                        if (virtualSwitch4) {
                            input (
                                name: "trackVirtualSwitch4", 
                                type: "bool", 
                                title: "Track Virtual Switch4 Changes", 
                                required: true, 
                                defaultValue: "true"
                            )
                        } 
                    }

                    section("<b>Add More Devices</b>") {
                        input (
                        name: "selectController4", 
                        type: "bool", 
                        title: "Turn on to add more devices", 
                        required: true, 
                        multiple: false,
                        submitOnChange: true               
                        )
                        if (settings.selectController3) {
                            state.more4 = true
                        } else {
                            state.more4 =  false           
                        }
                    } 

                    if (state.more4) {  
                        section("<b>Virtual Switch Device 5</b>") {

                            input (
                            name: "virtualSwitch5", 
                            type: "capability.switch", 
                            title: "Select Virtual Switch 5 Device", 
                            required: true, 
                            multiple: false,
                            submitOnChange: true             
                            )

                            if (virtualSwitch4) {
                                input (
                                    name: "trackVirtualSwitch5", 
                                    type: "bool", 
                                    title: "Track Virtual Switch4 Changes", 
                                    required: true, 
                                    defaultValue: "true"
                                )
                            } 
                        }
                    } // end more 4
                } // end more 3        
            } // end more 2
        } // end more 1   

        section("") {
            input (
                name: "debugMode", 
                type: "bool", 
                title: "Enable logging", 
                required: true, 
                defaultValue: false
            )
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(virtualSwitch, "switch", switchHandler) 
    if (state.more1) {
        subscribe(virtualSwitch2, "switch", switchHandler2)
    }
    if (state.more2) {
        subscribe(virtualSwitch3, "switch", switchHandler3)
    }
    if (state.more3) {
        subscribe(virtualSwitch4, "switch", switchHandler4)
    }
    if (state.more4) {
        subscribe(virtualSwitch5, "switch", switchHandler5)
    }
}    

def switchHandler(evt) {

    state.switch1 = evt.value
    logDebug("Switch Event = $state.switch1")   

    def action = getAction(evt.value)
    def cmd = action + " " + state.alexaName
    logDebug("cmd = $cmd")
    echoDevice.voiceCmdAsText(cmd)
}

def switchHandler2(evt) {

    state.switch2 = evt.value
    logDebug("Switch2 Event = $state.switch2")   

    def action = getAction(evt.value)
    def cmd = action + " " + virtualSwitch2.getName()
    logDebug("cmd = $cmd")
    echoDevice.voiceCmdAsText(cmd)
}

def switchHandler3(evt) {

    state.switch3 = evt.value
    logDebug("Switch3 Event = $state.switch3")   

    def action = getAction(evt.value)
    def cmd = action + " " + virtualSwitch3.getName()
    logDebug("cmd = $cmd")
    echoDevice.voiceCmdAsText(cmd)
}

def switchHandler4(evt) {

    state.switch4 = evt.value
    logDebug("Switch4 Event = $state.switch4")   

    def action = getAction(evt.value)
    def cmd = action + " " + virtualSwitch4.getName()
    logDebug("cmd = $cmd")
    echoDevice.voiceCmdAsText(cmd)
}

def switchHandler5(evt) {

    state.switch5 = evt.value
    logDebug("Switch5 Event = $state.switch5")

    def action = getAction(evt.value)
    def cmd = action + " " + virtualSwitch5.getName()
    logDebug("cmd = $cmd")
    echoDevice.voiceCmdAsText(cmd)
}

def getAction(type) {

    if (type == "on") state.onAction
    else return state.offAction

}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}