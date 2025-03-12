/**
 *  ****************  Broadlink Switch Connectors  ****************
 *
 *  Usage:
 *  This was designed to sync up to five virtual switches to Broadlink Virtual Device code names
 *
**/

definition (
    name: "Broadlink Switch Connectors",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for virtual switches to trigger broadlink RF code names in Broadlink driver",
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

        section("<b>Broadlink Virtual Device</b>") {

            input (
              name: "broadlinkDevice", 
              type: "capability.actuator", 
              title: "Select Virtual BroadLink Device", 
              required: true, 
              multiple: false           
            )
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

        section("<b>BroadLink On Command</b>") {
            input (
              name: "blOn1", 
              type: "String", 
              title: "Enter On Command", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
        }        
        state.on1 = blOn1

        section("<b>BroadLink Off Command</b>") {
            input (
              name: "blOff1", 
              type: "String", 
              title: "Enter Off Command", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
        }  
        state.off1 = blOff1        

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

        section("<b>BroadLink On Command 2</b>") {
            input (
              name: "blOn2", 
              type: "String", 
              title: "Enter On Command 2", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            state.on2 = blOn2
        }        

        section("<b>BroadLink Off Command 2</b>") {
            input (
              name: "blOff2", 
              type: "String", 
              title: "Enter Off Command 2", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            state.off2 = blOff2
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

                section("<b>BroadLink On Command 3</b>") {
                    input (
                    name: "blOn3", 
                    type: "String", 
                    title: "Enter On Command 3", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true               
                    )
                    state.on3 = blOn3
                }        

                section("<b>BroadLink Off Command 3</b>") {
                    input (
                    name: "blOff3", 
                    type: "String", 
                    title: "Enter Off Command 3", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true               
                    )
                    state.off3 = blOff3
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

                    section("<b>BroadLink On Command 4</b>") {
                        input (
                        name: "blOn4", 
                        type: "String", 
                        title: "Enter On Command 4", 
                        required: true, 
                        multiple: false,
                        submitOnChange: true               
                        )
                        state.on4 = blOn4
                    }        

                    section("<b>BroadLink Off Command 4</b>") {
                        input (
                        name: "blOff4", 
                        type: "String", 
                        title: "Enter Off Command 4", 
                        required: true, 
                        multiple: false,
                        submitOnChange: true               
                        )
                        state.off4 = blOff4
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
                            title: "Select Virtual Switch 4 Device", 
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

                        section("<b>BroadLink On Command 5</b>") {
                            input (
                            name: "blOn5", 
                            type: "String", 
                            title: "Enter On Command 5", 
                            required: true, 
                            multiple: false,
                            submitOnChange: true               
                            )
                            state.on5 = blOn5
                        }        

                        section("<b>BroadLink Off Command 5</b>") {
                            input (
                            name: "blOff5", 
                            type: "String", 
                            title: "Enter Off Command 5", 
                            required: true, 
                            multiple: false,
                            submitOnChange: true               
                            )
                            state.off5 = blOff5
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

    state.switch = evt.value
    logDebug("Switch Event = $state.swtich")   

    if (evt.value == "on") {
        broadlinkDevice.SendStoredCode(state.on1)
    } else {
        broadlinkDevice.SendStoredCode(state.off1,2)
    }
}

def switchHandler2(evt) {

    state.switch2 = evt.value
    logDebug("Switch2 Event = $state.swtich2")   

    if (evt.value == "on") {
        broadlinkDevice.SendStoredCode(state.on2)
    } else {
        broadlinkDevice.SendStoredCode(state.off2,2)
    }
}

def switchHandler3(evt) {

    state.switch3 = evt.value
    logDebug("Switch3 Event = $state.swtich3")   

    if (evt.value == "on") {
        broadlinkDevice.SendStoredCode(state.on3)
    } else {
        broadlinkDevice.SendStoredCode(state.off3,2)
    }
}

def switchHandler4(evt) {

    state.switch4 = evt.value
    logDebug("Switch4 Event = $state.swtich4")   

    if (evt.value == "on") {
        broadlinkDevice.SendStoredCode(state.on4)
    } else {
        broadlinkDevice.SendStoredCode(state.off4,2)
    }
}

def switchHandler5(evt) {

    state.switch5 = evt.value
    logDebug("Switch5 Event = $state.swtich5")   

    if (evt.value == "on") {
        broadlinkDevice.SendStoredCode(state.on5)
    } else {
        broadlinkDevice.SendStoredCode(state.off5,2)
    }
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}