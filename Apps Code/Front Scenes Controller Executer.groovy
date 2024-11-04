/**
 *  ****************  Front Scenes Controller Executer  ****************
 *
 *  Usage:
 *  Handle motion sensors, scene switches, and constraint switches, to activate Living Room and Kitchen Scene Execution.  
 *  
 *  v. 1.0: 11/5/23 intial code
 *  
 *  v. 2.0: 1/25/24: Many fixes/updates - also just added in update() to map device names to their index to save searching for devices in arrays each time. 
 *  v. 2.1: 2/04/24: Added Pantry Light and Motion Sensor
 *  v. 2.2: 11/1/24: Morning/Evening Scenes now change based on light conditions in illuminance data
**/
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat 
import java.util.Date

definition (
    name: "Front Scenes Controller Executer",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controls the Open Concept Kitchen and Living Room in the Front of the house.  All scenes for a room are stored in a single json string, which is parsed at scene execution.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Living Room Scene Devices: Bottle Lamp,Entry Stairs,Fireplace,Living Room Ceiling,Standing Lamp, Table Lamp,Tree Lamp</b>") {
            input (
              name: "livingRoomSceneDevices", 
              type: "capability.*", 
              title: "Select Scene Devices", 
              required: true, 
              multiple: true,
              submitOnChange: true             
            )
        }
        
        section("<b>Kitchen Scene Devices: Corner Lamp,Fan Light,Island Lights,Kitchen Ceiling,Kitchen Fan,Kitchen Sink Light,Under Cab Lights,Pantry Light</b>") {
            input (
              name: "kitchenSceneDevices", 
              type: "capability.*", 
              title: "Select Kitchen Scene Devices", 
              required: true, 
              multiple: true,
              submitOnChange: true             
            )
        }

        section("<b>Front Scene Switch Devices: Bright, Dawn, Day, Dim, Dinner, Evening, Late Evening, Morning, Night, TV Time</b>") {
            input (
              name: "frontSceneSwitches", 
              type: "capability.switch", 
              title: "Select All Front Room Scene Switches except On/Off", 
              required: true, 
              multiple: true,
              submitOnChange: true             
            )
        }        

        section("<b>Front Outdoor Switch Devices: Front Floodlight,Front Lanterns</b>") {
            input (
              name: "frontWallSwitches", 
              type: "capability.switch", 
              title: "Select All Outdoor Wall Switches", 
              required: true, 
              multiple: true,
              submitOnChange: true             
            )
        } 

        section("<b>Front Sensor On/Off Switch: Front Sensor</b>") {
            input (
              name: "frontSensorSwitch", 
              type: "capability.switch", 
              title: "Select Front Sensor Switch", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        } 
        
        section("<b>Front Scene On/Off Switch: Scene On/Off</b>") {
            input (
              name: "frontSceneSwitch", 
              type: "capability.switch", 
              title: "Select front Scene On/Off Switch", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }         

        section("<b>Front Scene Update Switch: Scenes Update</b>") {
            input (
              name: "frontUpdateSwitch", 
              type: "capability.switch", 
              title: "Select front Scene Update Switch", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }  

        section("<b>Living Room Thermostat: Living Room Thermostat</b>") {
            input (
              name: "frontThermo", 
              type: "capability.thermostat", 
              title: "Select Thermostat", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }
        
        section("<b>Dru Home Switch: Dru Home</b>") {
            input (
              name: "druHomeSwitch", 
              type: "capability.switch", 
              title: "Select Drue Home Switch", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        } 

        section("<b>Reading Mode Switch: Reading Light</b>") {
            input (
              name: "readingSwitch", 
              type: "capability.switch", 
              title: "Select Reading Mode Switch", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Living Room Motion Device: Living Room Sensor</b>") {
            input (
              name: "livingRoomMotion", 
              type: "capability.motionSensor", 
              title: "Select Living Room Motion Sensor", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        } 

        section("<b>Kitchen Motion Device: Kitchen Sensor</b>") {
            input (
              name: "kitchenMotion", 
              type: "capability.motionSensor", 
              title: "Select Kitchen Motion Sensor", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }      

        section("<b>Entry Stairs Motion Device: Entry Stairs</b>") {
            input (
              name: "entryStairsMotion", 
              type: "capability.motionSensor", 
              title: "Select Entry Stairs Motion Sensor", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }   

        section("<b>Front Door Motion Device: Front Door Sensor</b>") {
            input (
              name: "frontDoorMotion", 
              type: "capability.motionSensor", 
              title: "Select Front Door Motion Sensor", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }  

        section("<b>Pantry Motion Device: Pantry Sensor</b>") {
            input (
              name: "pantryMotion", 
              type: "capability.motionSensor", 
              title: "Select Pantry Motion Sensor", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }  

        section("<b>Mailbox Motion Device: Mail Sensor</b>") {
            input (
              name: "mailboxMotion", 
              type: "capability.motionSensor", 
              title: "Select Mailbox Motion Sensor", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }  

        section("<b>Mailbox Status Switch: Mailbox Status</b>") {
            input (
              name: "mailStatus", 
              type: "capability.switch", 
              title: "Select Mail Status Switch Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )               
        }

        section("<b>Echo Features Device: Echo Features</b>") {
            input (
              name: "echoFeatures", 
              type: "capability.actuator", 
              title: "Select Echo Features Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )              
        }

        section("<b>Scene Activator Device: Scene Activator</b>") {
            input (
              name: "sceneActivator", 
              type: "capability.actuator", 
              title: "Select Scene Activator Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        } 

        section("<b>Alexa Trigger Switch: Alexa Trigger Switch H</b>") {
            input (
              name: "alexaTrigger", 
              type: "capability.switch", 
              title: "Select Alexa Trigger Dimmer Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        } 

        section("<b>Front Sensor Data Device: Front Sensor Data</b>") {
            input (
              name: "frontData", 
              type: "capability.actuator", 
              title: "Select Front Sensor Data Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )           
        }

         section("<b>Illuminance Data Device: Illuminance Data</b>") {
            input (
              name: "illuminanceData", 
              type: "capability.actuator", 
              title: "Select Illuminance Data Actuator Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )                
        }        

         section("<b>Signal Lamp Driver</b>") {
            input (
              name: "signalLamp", 
              type: "capability.actuator", 
              title: "Select Signal Lamp Driver Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )             
        } 

        section("<b>File Manager Device</b>") {
            input (
              name: "fileManager", 
              type: "capability.actuator", 
              title: "Select File Manager Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }  

        section("") {
            input (
                name: "executeDelay", 
                type: "number", 
                title: "Execute Delay in millisecons", 
                required: true, 
                defaultValue: 500
            )
        }

        section("") {
            input (
                name: "xmasLights", 
                type: "bool", 
                title: "Control Christmas Lights", 
                required: true, 
                defaultValue: false
            )
        }

        section("") {
            input (
                name: "fileMode", 
                type: "bool", 
                title: "Enable file logging", 
                required: true, 
                defaultValue: false
            )
            input (
                name: "fileOffSecs", 
                type: "enum", 
                title: "File Logging Off Hours", 
                options: ["3660":"1", "21600":"6", "43200":"12", "86400":"24"],
                required: true, 
                defaultValue: "1"
            ) 
            input (
                name: "logFileName", 
                type: "string", 
                title: "File Name for Logging", 
                required: true, 
                defaultValue: "1"
            )                     
        }

        section("") {
            input (
                name: "debugMode", 
                type: "bool", 
                title: "Enable logging", 
                required: true, 
                defaultValue: false
            )
        }

        section("") {
            input (
                name: "logLevel",
                type: "enum",
                title: "<font style='font-size:14px; color:#1a77c9'>Logging Level</font>",
                options: [1:"Info", 2:"Warning", 3:"Debug", 4:"Extra Debug"],
                multiple: false,
                defaultValue: 2,
                required: true
            )
        }
    }
}

def installed() {

    state.xmasLights = false
    state.xmasRan == false    
    
    state.frontSceneSwitches = [:]
    state.frontWallSwitches = [:]
    state.kitchenSceneDevices = [:]
    state.livingRoomSceneDevices = [:]

    initialize()
}

def updated() {

    if (settings?.debugMode && settings?.logLevel == "3") {
        runIn(3600, logDebugOff)   // one hour
        logDebug("Log Level will change from Debug to Info after 1 hour")
    }

    if (settings?.fileMode) {
        clearLog()
        fileLogOff = settings?.fileOffSecs.toInteger()
        runIn(fileLogOff, logFileOff)  
        logDebug("File Logging will be turned off in ${fileLogOff} hours",2)
    }

    state.frontSceneSwitches = [:]
    state.frontWallSwitches = [:]
    state.kitchenSceneDevices = [:]
    state.livingRoomSceneDevices = [:]
    state.druHome = false
    state.modeAtDinner = "Late Evening"

    unsubscribe()
    unschedule()

    initialize()

}
def initialize() {

    setDeviceMaps()  // map device names to index in device array

    // Front Sensor Data Handlers
    subscribe(frontData, "frontMotionStatus", frontTimeoutHandler)
    subscribe(frontData, "frontMode", frontModeChangeHandler)
    subscribe(frontData, "kitchenMotion", kitchenActiveHandler)
    subscribe(frontData, "kitchenActivityMotion", kitchenActivityActiveHandler)
    subscribe(frontData, "livingRoomMotion", livingRoomActiveHandler)
    subscribe(frontData, "stairsMotion", stairsActiveHandler)
    subscribe(frontData, "pantryMotion", pantryActiveHandler)
    subscribe(frontData, "frontDoorMotion", frontDoorActiveHandler)
    subscribe(frontData, "xmasLights", xmasLightsHandler)

    // Alexa Triggers Handler
    subscribe(alexaTrigger, "level", alexaHandler)

    // Motion Sensor Handlers
    subscribe(livingRoomMotion, "motion", livingRoomMotionHandler)
    subscribe(kitchenMotion, "motion", kitchenMotionHandler)
    subscribe(entryStairsMotion, "motion", entryStairsMotionHandler)
    subscribe(frontDoorMotion, "motion", frontDoorMotionHandler)
    subscribe(mailboxMotion, "motion", mailActiveHandler)
    subscribe(pantryMotion, "motion", pantryMotionHandler)

    // Timeout Handlers
    subscribe(frontData,"livingRoomMotionStatus", livingRoomTimeoutHandler)
    subscribe(frontData,"kitchenMotionStatus", kitchenTimeoutHandler)
    subscribe(frontData,"kitchenActivityStatus", kitchenActivityTimeoutHandler)
    subscribe(frontData,"stairsMotionStatus", stairsTimeoutHandler)
    subscribe(frontData,"frontDoorMotionStatus", frontDoorTimeoutHandler)
    subscribe(frontData,"pantryMotionStatus", pantryTimeoutHandler)

    // All Scene Switches
    subscribe(frontSceneSwitches[state?.frontSceneSwitches["Dawn"]],"switch", frontDawnSwitchHandler)
    subscribe(frontSceneSwitches[state?.frontSceneSwitches["Morning"]],"switch", frontMorningSwitchHandler)
    subscribe(frontSceneSwitches[state?.frontSceneSwitches["Day"]],"switch", frontDaySwitchHandler)
    subscribe(frontSceneSwitches[state?.frontSceneSwitches["Evening"]],"switch", frontEveningSwitchHandler)
    subscribe(frontSceneSwitches[state?.frontSceneSwitches["Dinner"]],"switch", frontDinnerSwitchHandler)
    subscribe(frontSceneSwitches[state?.frontSceneSwitches["Late Evening"]],"switch", frontLateEveningSwitchHandler)
    subscribe(frontSceneSwitches[state?.frontSceneSwitches["TV Time"]],"switch", frontTVTimeSwitchHandler)
    subscribe(frontSceneSwitches[state?.frontSceneSwitches["Night"]],"switch", frontNightSwitchHandler)
    subscribe(frontSceneSwitches[state?.frontSceneSwitches["Dim"]],"switch", frontDimSwitchHandler)
    subscribe(frontSceneSwitches[state?.frontSceneSwitches["Bright"]],"switch", frontBrightSwitchHandler)

    // Constraint and feature Switches
    subscribe(frontSensorSwitch,"switch", frontSensorSwitchHandler)
    subscribe(frontSceneSwitch,"switch", frontSceneSwitchHandler)    // scene on/off
    subscribe(druHomeSwitch, "switch", druHomeSwitchHandler)
    subscribe(frontUpdateSwitch,"switch", frontUpdateSwitchHandler) // reload scenes
    subscribe(mailStatus, "switch", mailStatusSwitchHandler)
    subscribe(readingSwitch, "switch", readingModeSwitchHandler)
    subscribe(sceneActivator, "livingRoomScene", runLivingRoomHandler)
    subscribe(sceneActivator, "kitchenScene", runKitchenHandler)

    // Doubletap Front Wall Switches
    subscribe(kitchenSceneDevices[state?.kitchenSceneDevices["Island Lights"]],"doubleTapped", islandWallHandler)
    subscribe(kitchenSceneDevices[state?.kitchenSceneDevices["Kitchen Ceiling"]],"doubleTapped", kitchenCeilingWallHandler)
    subscribe(kitchenSceneDevices[state?.kitchenSceneDevices["Kitchen Fan"]],"doubleTapped", kitchenFanWallHandler)
    subscribe(livingRoomSceneDevices[state?.livingRoomSceneDevices["Entry Stairs"]],"doubleTapped", stairsWallHandler)
    subscribe(livingRoomSceneDevices[state?.livingRoomSceneDevices["Living Room Ceiling"]],"doubleTapped", livingRoomCeilingWallHandler)

    // Outdoor Light Switches
    subscribe(frontWallSwitches[state?.frontWallSwitches["Front Floodlight"]],"doubleTapped", floodlightWallHandler)
    subscribe(frontWallSwitches[state?.frontWallSwitches["Front Lanterns"]],"doubleTapped", lanternsWallHandler)

    // illuminance Triggers for scenes
    subscribe(illuminanceData,"sunset", sunsetHandler)
    subscribe(illuminanceData,"lowLight", lowLightHandler)
    subscribe(illuminanceData,"dayLight", dayLightHandler)
    //subscribe(kitchenSceneDevices[findKitchenDevice("Pantry Light")],"switch", pantrySwitchHandler)

    setSchedules()
}

def setDeviceMaps() {
    logDebug("Setting Device maps")

    def label = "none"
    def t = 0

    // map front scene switches
    for (t=0; t < frontSceneSwitches.size(); t++) {
        label = frontSceneSwitches[t].getLabel()
        if (label == null) {label = frontSceneSwitches[t].getName()}
        logDebug("label is ${label}")
        state.frontSceneSwitches[label] = t
    }

    // map front wall switches
    for (t=0; t < frontWallSwitches.size(); t++) {
        label = frontWallSwitches[t].getLabel()
        if (label == null) {label = frontWallSwitches[t].getName()}
        logDebug("label is ${label}")
        state.frontWallSwitches[label] = t
    }

    // map kitchen scene devices
    for (t=0; t < kitchenSceneDevices.size(); t++) {
        label = kitchenSceneDevices[t].getLabel()
        if (label == null) {label = kitchenSceneDevices[t].getName()}
        logDebug("label is ${label}")
        state.kitchenSceneDevices[label] = t
    }

    // map living room scene devices
    for (t=0; t < livingRoomSceneDevices.size(); t++) {
        label = livingRoomSceneDevices[t].getLabel()
        if (label == null) {label = livingRoomSceneDevices[t].getName()}
        logDebug("label is ${label}")
        state.livingRoomSceneDevices[label] = t
    }
}

// Set Scene Schedules
def setSchedules() {
    logDebug("Set Schedules called",3)

    // Scene Schedules
    //schedule('0 30 5 * * ?', dawnScheduleHandler)       // also set with sunrise
    //schedule('0 0 7 * * ?', morningScheduleHandler)     // also set with lowLight
    //schedule('0 0 09 * * ?', dayScheduleHandler)       // also set with dayLight
    //schedule('0 0 17 * * ?', eveningScheduleHandler)     // also set with lowLight
    //schedule('0 0 20 * * ?', lateEveningScheduleHandler) // also set with sunset
    schedule('0 0 20 * * ?', tvTimeScheduleHandler) // Revert to pre-dinner mode    afterDinnerScheduleHandler
    //schedule('0 0 21 * * ?', tvTimeScheduleHandler)
    schedule('0 30 22 * * ?', dimScheduleHandler)
    schedule('0 30 23 * * ?', nightScheduleHandler) 

    // Device Schedules
    schedule('0 00 18 * * ?', fireplaceOnScheduleHandler)
    schedule('0 30 21 * * ?', fireplaceOffScheduleHandler)

    // Reset mailbox each morning
    schedule('0 31 05 * * ?', resetMailbox)

    subscribe(illuminanceData,"sunset", sunsetHandler)
    subscribe(illuminanceData,"lowLight", lowLightHandler)
    subscribe(illuminanceData,"dayLight", dayLightHandler) 
}

// Set xmas lights state variable when changed
def xmasLightsHandler(evt) {
    logDebug("xmasLights changed to ${evt.value}",1)

    if (evt.value == "true") state.xmasLights = true
    else state.xmasLights = false
}

// ************************ Illuminance State Handlers - for mode change triggers ***************************

// Set Dawn mode, Late Evening mode, and Front Lanterns from Sunset triggers
def sunsetHandler(evt) {

    def mode = frontData.currentValue("frontMode")

    // Sunset
    if (evt.value == "true") {
        logDebug("Sunset, mode is ${mode}",3)
        if (mode == "Evening" && mode != "Dinner" && mode != "Late Evening") {
            lateEveningScheduleHandler()                    
        }  else if (mode == "Dinner") {
            logDebug("Got sunset during dinner for mode ${mode}")
            state.modeAtDinner = "Late Evening"
            // create a one time schedule for 8pm when dinner is done
/*             def year = getDateString("yyyy")  
            def month = getDateString("MM")   
            def day = getDateString("dd")
            def lateEve = "0 0 20 " + day + " " + month + " " + "? " + year    
            logDebug("Late Evening Schedule set as ${schedule}")          
            schedule(lateEve, lateEveningScheduleHandler) */
        }       
        frontWallSwitches[findWallSwitch("Front Lanterns")].setLevel(25,5)
    }
    // Sunrise
        if (evt.value == "false") {
            logDebug("Sunrise and front mode is ${mode}",3)
            if (mode == "Night" && mode != "Morning") {
                dawnScheduleHandler()
            }
        frontWallSwitches[findWallSwitch("Front Lanterns")].off()
        frontWallSwitches[findWallSwitch("Front Floodlight")].off()        
    }
}

String getDateString(part) {

    Date now = new Date()
    def dateString = now.format("yyyy-MM-dd HH:mm:ss")
 
    logDebug("${dateString}",3)

    if (part == "yyyy") {return dateString.substring(0,4)}
    else if (part == "MM") {return dateString.substring(5,7) }
    else if (part == "dd") {return dateString.substring(8,10)}
    else if (part == "HH") {return dateString.substring(11,13)}
    else if (part == "mm") {return dateString.substring(14,16)}
    else if (part == "ss") {return dateString.substring(17,19)}
    else {return "Bad Date-Part Parameter"}
}

// Set Morning with lowLight 
def lowLightHandler(evt) {
    def mode = frontData.currentValue("frontMode")
    // Late Evening from Evening if not dinner
    if (evt.value == "true") {
/*         if (mode != "Evening" && mode != "Dinner") {
            lateEveningScheduleHandler()
        } */
        if (mode == "Dawn") {
            morningScheduleHandler()
        }
    }
/*     // Morning
    if (evt.value == "false") {
        if (mode == "Dawn" && mode != "Evening") {
            morningSchedulehandler()
        } 
    } */
}

// Set Day from Daylight true
def dayLightHandler(evt) {
    def mode = frontData.currentValue("frontMode")
    // Daylight Level - Day from Morning
    if (evt.value == "true") {
        if (mode == "Morning") {
            dayScheduleHandler()
        }
    }
    // Daylight Diminished - Evening from day
    if (evt.value == "false") {
        def hour = getDateString("HH").toInteger()
        if (hour > 15) {
            if (mode == "Day") {
                eveningScheduleHandler()
            }
        }
    } 
}

// ************************************** Mode, Device and Thermostat Schedule Handlers **************************************

// Dawn Schedule Handler
def dawnScheduleHandler() {
    logDebug("Dawn Handler Called",3)
    if (frontSensorSwitch.currentValue("switch") == "off" && druHomeSwitch.currentValue("switch") == "off") frontSensorSwitch.on()
    if (sceneSwitchOffCheck("Dawn")) turnOnFrontSceneSwitch("Dawn")  
    //if (frontSceneSwitches[findFrontSceneSwitchIndex("Night")] == "on") frontSceneSwitches[findFrontSceneSwitchIndex("Night")].off()
}

// Morning Schedule Handler
def morningScheduleHandler() {
    logDebug("Morning Handler Called",3)
    if (sceneSwitchOffCheck("Morning"))turnOnFrontSceneSwitch("Morning") 
}

// Day Schedule Handler
def dayScheduleHandler() {
    logDebug("Day Handler Called",3)
    if (sceneSwitchOffCheck("Day")) turnOnFrontSceneSwitch("Day")  
}

// Evening Schedule Handler
def eveningScheduleHandler() {
    logDebug("Evening Handler Called",3)
    if (sceneSwitchOffCheck("Evening")) turnOnFrontSceneSwitch("Evening") 
}

// Dinner Schedule Handler
def dinnerScheduleHandler() {
    state.modeAtDinner = frontData.currentValue("frontMode")
    logDebug("Dinner Handler Called",3)
    if (sceneSwitchOffCheck("Dinner")) turnOnFrontSceneSwitch("Dinner")  
}

// After Dinner Schedule Handler - Revert back to mode before dinner
def afterDinnerScheduleHandler() {
    def mode = state?.modeAtDinner
    logDebug("After Dinner Handler Called - Mode Change is ${mode}",3)
    if (sceneSwitchOffCheck(mode)) turnOnFrontSceneSwitch(mode)  
}

// Late Evening Schedule Handler
def lateEveningScheduleHandler() {
    logDebug("Late Evening Handler Called",3)
    if (sceneSwitchOffCheck("Late Evening")) turnOnFrontSceneSwitch("Late Evening")
}

// TV Time Schedule Handler
def tvTimeScheduleHandler() {
    logDebug("TV Time Handler Called",3)
    if (sceneSwitchOffCheck("TV Time")) turnOnFrontSceneSwitch("TV Time") 
}

// Dim Schedule Handler
def dimScheduleHandler() {
    logDebug("Dim Handler Called",3)
    if (sceneSwitchOffCheck("Dim")) turnOnFrontSceneSwitch("Dim")     
}

// Night Schedule Handler
def nightScheduleHandler() {
    logDebug("Night Handler Called",3)
    if (sceneSwitchOffCheck("Night") && state?.druHome == false) turnOnFrontSceneSwitch("Night")  
}

// Fireplace On Schedule Handler
def fireplaceOnScheduleHandler() {
    logDebug("Fireplace On Handler Called",3)
    if (livingRoomSceneDevices[findLivingRoomDevice("Fireplace")].currentValue("switch") == "off") 
        livingRoomSceneDevices[findLivingRoomDevice("Fireplace")].on()
}

// Fireplace Off Schedule Handler
def fireplaceOffScheduleHandler() {
    logDebug("Fireplace Off Handler Called",3)
    if (livingRoomSceneDevices[findLivingRoomDevice("Fireplace")].currentValue("switch") == "on") 
        livingRoomSceneDevices[findLivingRoomDevice("Fireplace")].off()
}

// Check if scene swich is off before turning on
boolean sceneSwitchOffCheck(name) {
    logDebug("sceneSwitchOffCheck called with ${name}",3)

    def isOff = false
    if (frontSceneSwitches[findFrontSceneSwitchIndex(name)].currentValue("switch") == "off") {
        isOff = true      
    }
    logDebug("sceneSwitchOffCheck returned ${isOff}",3)
    return isOff
}

// TURN ON SCENE SWITCH and SET MODE for Scheduled Mode Change
def turnOnFrontSceneSwitch(name) {
    logDebug("turnOnFrontSceneSwitch called with ${name}",3)
    frontData.setFrontScheduledMode(name)
    frontSceneSwitches[findFrontSceneSwitchIndex(name)].on()
}

// ************************************* Front Scene Switch Handlers **********************************************

def frontDawnSwitchHandler(evt) {
    logDebug("Dawn Scene Switch Handler ${evt.value}",3)
    if (evt.value == "on") activateFrontSceneSwitch("Dawn")  
}

def frontMorningSwitchHandler(evt) {
    logDebug("Morning Scene Switch Handler ${evt.value}",3)
    if (evt.value == "on") activateFrontSceneSwitch("Morning")  
}

def frontDaySwitchHandler(evt) {
    logDebug("Day Scene Switch Handler ${evt.value}",3)
    if (evt.value == "on") activateFrontSceneSwitch("Day")  
}

def frontEveningSwitchHandler(evt) {
    logDebug("Evening Scene Switch Handler ${evt.value}",3)
    if (evt.value == "on") activateFrontSceneSwitch("Evening")  
}

def frontDinnerSwitchHandler(evt) {
    logDebug("Dinner Scene Switch Handler ${evt.value}",3)
    if (evt.value == "on") activateFrontSceneSwitch("Dinner")  
}

def frontLateEveningSwitchHandler(evt) {
    logDebug("Late Evening Scene Switch Handler ${evt.value}",3)
    if (evt.value == "on") activateFrontSceneSwitch("Late Evening")  
}

def frontTVTimeSwitchHandler(evt) {
    logDebug("TV Time Scene Switch Handler ${evt.value}",3)
    if (evt.value == "on") activateFrontSceneSwitch("TV Time")  
}

def frontDimSwitchHandler(evt) {
    logDebug("Dim Scene Switch Handler ${evt.value}",3)
    if (evt.value == "on") activateFrontSceneSwitch("Dim")  
}

def frontBrightSwitchHandler(evt) {
    logDebug("Bright Scene Switch Handler ${evt.value}",3)
    if (evt.value == "on") activateFrontSceneSwitch("Bright")  
}

def frontNightSwitchHandler(evt) {
    logDebug("Night Scene Switch Handler ${evt.value}",3)
    if (evt.value == "on") activateFrontSceneSwitch("Night")  
}

// Activate the scene for switch and turn off other switches
def activateFrontSceneSwitch(scene) {
    logDebug("Activate Front Scene Switch ${scene}",3)      
    if (activeCheck("kitchen")) runScene("kitchen", scene) 
    pauseExecution(500)
    if (activeCheck("livingRoom")) runScene("livingRoom", scene) 
    toggleFrontSceneSwitchesOff(scene)  
    frontData.setFrontMode(scene)
}

// flip other Front scene switches off
def toggleFrontSceneSwitchesOff(scene) {
    logDebug("Other Front Scene Switches Off ${scene}",3)
    for (d=0; d < frontSceneSwitches.size(); d++) {
        if (frontSceneSwitches[d].getName() != scene) 
            frontSceneSwitches[d].off()
    }
}

// Find front Scene Switch in List
int findFrontSceneSwitchIndex(name) {
    return state?.frontSceneSwitches[name]
}

// *********************************** Reading Mode Handlers *************************************

// Reading Mode Switch Handler
def readingModeSwitchHandler(evt) {
    logDebug("Reading Mode Switch Event ${evt.value}",3)

    if (evt.value == "on") setReadingMode()
    if (evt.value == "off") turnOffReadingMode()
}

// Set Reading Mode
def setReadingMode() {
    frontData.setReadingLight("true")
    def lvl = illuminanceData.currentValue("readingLightLevel").toInteger()
    livingRoomSceneDevices[findLivingRoomDevice("Standing Lamp")].setLevel(lvl,3)
    livingRoomSceneDevices[findLivingRoomDevice("Table Lamp")].setLevel(lvl,3)
    runIn(10800, turnOffReadingMode)   // 3 hours auto off
}

// Turn Off Reading Mode
def turnOffReadingMode() {
    frontData.setReadingLight("false")
    if (readingSwitch.currentValue("switch") == "on") readingSwitch.off()
    updateScenes("livingRoom")
}

// ********************************** Alexa Event Handlers *************************************************

// Handle Alexa events sent via Alexa Trigger Switch (Dimmer level)
def alexaHandler(evt) {
    logDebug("Alexa Trigger Event ${evt.value}",1)
    def trigger = evt.value.toInteger()
    def scheduled = frontData.currentValue("frontMode")

    if (trigger == 37) frontSensorSwitch.off()                      // front sensor off
    else if (trigger == 38) frontSensorSwitch.on()                  // front sensor on
    else if (triggere == 60) frontSceneSwitch.on()                  // front lights on
    else if (trigger == 61) frontSceneSwitch.off()                  // front light off
    else if (trigger == 62) runScenesMotionCheck("Dinner")          // dinner scene
    else if (trigger == 63) afterDinnerScheduleHandler()            // Dinner is over
    else if (trigger == 64) druHomeEnable()                         // dru home
    else if (trigger == 66) runScenesMotionCheck("Evening")         // evening scene
    else if (trigger == 67) runScenesMotionCheck(scheduled)         // scheduled mode
    else if (trigger == 68) setRedingMode()                         // Reading Mode
    else if (trigger == 69) {}                                      // echo mailbox status - depricated
    else if (trigger == 70) {}                                      // unused
    else if (trigger == 71) {}                                      // unused
    else if (trigger == 72) runScenesMotionCheck("Bright")          // bright Scene
    else if (trigger == 80) {}                                      // fix mailbox - depricated
    else if (trigger == 90) speakMode()                             // echo speak current mode
}

def runSceneMotionCheck(scene) {
//???

}

// Echo speak current mode
def speakMode() {
    def mode = frontData.currentValue("frontMode")
    //frontEcho.speak("The current mode is ${mode}")
    echoFeatures.setSpeakDevice("front","The current mode is ${mode}")
}

//************************************* Mailbox Handler ******************************************

// Mail Active Handler
def mailActiveHandler(evt) {

    if (evt.value == "active") {
        logDebug("Mail Status Activity Event ${evt.value}",3)
        def fullBox = frontData.currentValue("mailStatus") == "true"
        if (fullBox) {mailboxEmpty()}
        else {mailboxFull()}
    }
}

// Mail status Switch Handler
def mailStatusSwitchHandler(evt) {
    logDebug("Mail Status Switch Event ${evt.value}",3)
     def mailboxFull = frontData.currentValue("mailStatus") == "true"

    if (evt.value == "on" && mailboxFull == "false") mailboxFull()
    if (evt.value == "off" && mailboxFull == "true") mailboxEmpty()
}

def mailboxFull() {
    frontData.setMailStatus("true") 
    echoFeatures.setSpeakDevice("front","There is mail in the mailbox")
    frontData.setFrontActivity("The Mail has arrived")
    mailStatus.on()
    startMailSignal()
    signalLamp.colorFlash(2,"Green",2)
}

def mailboxEmpty() {
    mailStatus.off()        
    echoFeatures.setSpeakDevice("front","Someone Got the Mail")
    frontData.setMailStatus("false") 
    frontData.setFrontActivity("The Mailbox is Empty")
}

def resetMailbox() {
    mailStatus.off()        
    frontData.setMailStatus("false") 
    frontData.setFrontActivity("The Mailbox was reset to empty")    
}

// Mail Signal
def mailSignal() {
    logDebug("Mail Signal Event",3)
    def index = findLivingRoomDevice("Tree Lamp")
    def switchOff = livingRoomSceneDevices[index].currentValue("switch") == "off"

    if (mailStatus.currentValue("switch") == "on") {    
        if (switchOff) {
            livingRoomSceneDevices[index].on()
            pauseExecution(500)
            livingRoomSceneDevices[index].off()
        }
        if (!switchOff) {
            livingRoomSceneDevices[index].off()
            pauseExecution(500)
            livingRoomSceneDevices[index].on()               
        }
    }           // signal timer will not restart if mail switch is off and this method runs
}

// signal timer start
def startMailSignal() {
    runIn(600,mailSignal)  // every 10 minutes
}

// Find Device in Living Scene Devices
Integer findLivingRoomDevice(name) {
    return state?.livingRoomSceneDevices[name].toInteger()
}

// Find Device in Kitchen Scene Devices
Integer findKitchenDevice(name) {
    return state?.kitchenSceneDevices[name].toInteger()
}

// ********************************** Wall Switch Double Tap Handlers (1=on 2=off) *******************************************

def islandWallHandler(evt) {
    logDebug("Island Wall Switch Event ${evt.value}",3)

    if (evt.value == "1") {
        // front room on
    }
    if (evt.value =="2") {
        // front room off
    }

}

def kitchenCeilingWallHandler(evt) {
    logDebug("Kitchen Ceiling Wall Switch Event ${evt.value}",3)

    if (evt.value == "1") {
        // dinner scene
    }
    if (evt.value =="2") {
        // scheduled mode
    }
}

def kitchenFanWallHandler(evt) {
    logDebug("Kitchen Fan Wall Switch Event ${evt.value}",3)

    if (evt.value == "1") {
        // druHome on
        // front off
    }
    if (evt.value =="2") {
        // druHome off 
        // refresh scnes
    }
}

def stairsWallHandler(evt) {
    logDebug("Stairs Wall Switch Event ${evt.value}",3)

    if (evt.value == "1") {
        // tv time
    }
    if (evt.value =="2") {
        // scheduled mode
    }
}

def livingRoomCeilingWallHandler(evt) {
    logDebug("Living Room Ceiling Wall Switch Event ${evt.value}",3)

    if (evt.value == "1") {

    }
    if (evt.value =="2") {
        
    }
}

def floodlightWallHandler(evt) {
    logDebug("Floodlight Wall Switch Event ${evt.value}",3)

    if (evt.value == "1") {

    }
    if (evt.value =="2") {
        
    }
}

def lanternsWallHandler(evt) {
    logDebug("Front Lanterns Wall Switch Event ${evt.value}",3)

    if (evt.value == "1") {

    }
    if (evt.value =="2") {
        
    }
}

// find wall switch index in front doubletap devices
int findWallSwitch(name) {
    return state?.frontWallSwitches[name]
}


// ********************** Front Timeout Handler ********************************

// Front Timeout Handler (turn off front after room timeouts)
def frontTimeoutHandler(evt) {
    logDebug("Front Timeout Handler ${evt.value}",3)

    if (evt.value == "Timeout") {
        runScene("kitchen", "All Off")
        runScene("livingRoom", "All Off")
    }
}

// ******************* Scene Switch On/Off, Dru Home, and Sensor Switch Handlers ***************************

// frontSceneSwitch On/Off Handler
def frontSceneSwitchHandler(evt) {
    logDebug("Front Scene Switch On/Off ${evt.value}",3)

    if (evt.value == "on") {
        frontMode = frontData.currentValue("frontMode")
        if (timeoutCheck("kitchen")) frontData.setKitchenMotionStatus("Active")
            else runScene("kitchen", frontMode)
        if (timeoutCheck("livingRoom")) {
            frontData.setLivingRoomMotionStatus("Active")
            if (state?.xmasLights) sceneActivator.setChristmasScene(frontData.currentValue("frontMode"))
        } else runScene("livingRoom", frontMode)
    }
    if (evt.value == "off") {
        if (activeCheck("kitchen")) frontData.setKitchenMotionStatus("Timeout")
            else runScene("kitchen", "All Off")
        if (activeCheck("livingRoom")) {
            frontData.setLivingRoomMotionStatus("Timeout")  
            if (state?.xmasLights) sceneActivator.setChristmasScene("All Off")
        } else {
            runScene("livingRoom", "All Off")
        }
        frontWallSwitches[findKitchen("Kitchen Sink Light")].setLevel(0, 5)
    }
}

// Front Sensor Switch Handler (sync frontData attribute)
def frontSensorSwitchHandler(evt) {
    logDebug("Front Sensor Switch ${evt.value}",3)
    if (evt.value == "on") frontData.setFrontSensor("On") 
    if (evt.value == "off") frontData.setFrontSensor("Off")
}

// Dru Home Switch Handler
def druHomeSwitchHandler(evt) {
    logDebug("Dru Home Switch ${evt.value}",3)

    if (evt.value == "on") druHomeEnable()
    if (evt.value == "off") druHomeReset()
}

// Dru Home Mode On
def druHomeEnable() {
    logDebug("Dru Home Enabled",1)
    
    //frontSensorSwitch.off()
    def todayDate = new Date()

    // scheduole start todnight at 11 pm
    def parsedDate = todayDate.format("yyyy-MM-dd HH:mm:ss")
    logDebug("${parsedDate}",3)

    def year = parsedDate.substring(0,4)       
    def month = parsedDate.substring(5,7)    
    def day = parsedDate.substring(8,10)     

    def startCron = "0 0 23 ${day} ${month} ?"
    schedule(startCron, startDruHome)

    // Shecedule auto reset for tomorrow
    // Get new data/time 24 hours in future
    def tomorrowDate
    use(groovy.time.TimeCategory) {
        def tomorrow = new Date() + 24.hour
        tomorrowDate = tomorrow.format("yyyy-MM-dd HH:mm:ss")
    }
    logDebug("Tomorrow Date is ${tomorrowData}")
    day = tomorrowDate.substring(8,10) 
    month = tomorrowDate.substring(5,7)  

    logDebug("Tomorrow Month is ${month}",3)
    logDebug("Tomorrow Day is ${day}",3)
    def cron = "0 30 8 ${day} ${month} ?"
    schedule(cron, druHomeReset)    
}

def startDruHome() {
    state.druHome = true
    frontData.setDruHome("Yes")
    frontSensorSwitch.off()
}

// dru home reset
def druHomeReset() {
    druHomeSwitch.off()
    state.druHome = false
    frontData.setDruHome("No")
    frontSensorSwitch.on()
    unschedule("druHomeReset")
    unschedule("startDruHome")
    def scene = frontData.currentValue("frontMode")
    if (frontData.currentValue("kitchenMotionStatus") != "Timeout") {
        runScene("kitchen", scene)
    }
    if (frontData.currentValue("livingRoomMotionStatus") != "Timeout") {
        runScene("livingRoom", scene)
    }    
}

// ******************* Kitchen and Living Room MOTION ACTIVE (if Timed out)  ***************************

// LivingRoom set motion Active in sensor data with Motion Sensor active
def livingRoomMotionHandler(evt) {
    logDebug("Living Room Motion ${evt.value}",3)

    if (evt.value == "active") {
        logDebug("Setting Living Room to Active",3)
        frontData.setLivingRoomMotionStatus("Active")
        signalLamp.colorFade(1,"Green",3)
    }
    if (evt.value == "inactive") {
        logDebug("Setting Living Room to Inactive",3)
        frontData.setLivingRoomMotionStatus("Inactive")
    } 
}

// activate living room scene when sensor data changes livingRoomMotion to active
def livingRoomActiveHandler(evt) {
    if (evt.value == "active") {
        logDebug("Living Room received active",3)
        runScene("livingRoom", frontData.currentValue("frontMode")) 
    }
}

// Kitchen set motion Active in sensor data with Motion Sensor Active
def kitchenMotionHandler(evt) {
    logDebug("Kitchen Motion ${evt.value}",3)

    if (evt.value == "active") {
        logDebug("Setting Kitchen to Active",3)
        frontData.setKitchenActivityStatus("Active")
        pauseExecution(200)
        frontData.setKitchenMotionStatus("Active")     
        signalLamp.colorFade(1,"Blue",3)  
    }
     if (evt.value == "inactive") {
        logDebug("Setting Kitchen to Inactive",3)
        frontData.setKitchenActivityStatus("Inactive")
        frontData.setKitchenMotionStatus("Inactive")
    } 
}

// activate kitchen scene when sensor data changes kitchenMotion to active
def kitchenActiveHandler(evt) {
    
    if (evt.value == "active") {
        logDebug("Kitchen received active",3)
        runScene("kitchen", frontData.currentValue("frontMode"))        
    }
}

def kitchenActivityActiveHandler(evt) {
    
    if (evt.value == "active" ) {
        logDebug("Kitchen Activity recieved active",3)
        def frontMode = frontData.currentValue("frontMode")

        Integer level = 0
        if (frontMode == "Late Evening" || frontMode == "TV Time") level = 45
        else if (frontMode == "Morning" || frontMode == "Day") level = 70
        else if (frontMode == "Evening" || frontMode == "Dinner" || frontMode == "Bright") level = 80
        else if (frontMode == "Dawn" || frontMode == "Dim") level = 25
        else if (frontMode == "Night" || frontMode == "TV Time") level = 5

        if (frontData.currentValue("frontMode") != "Night" && state?.druHome == false)
            kitchenSceneDevices[findKitchenDevice("Kitchen Sink Light")].setLevel(level, 5)  
    }
}

def kitchenActivityHandler() {

}

// CHECK if Room is active (returns true if active)
boolean activeCheck(room) {
    logDebug("Active check for ${room}",3)
    def active = false
   
    if (room == "livingRoom") {
        if (frontData.currentValue("livingRoomMotionStatus") == "Active") {
            active = true
            logDebug("Living Room is Active",3)
        }
    } else {logDebug("Living Room is Timeout",3)}

    if (room == "kitchen") {
        if (frontData.currentValue("kitchenMotionStatus") == "Active") {
            active = true 
            logDebug("Kitchen is active",3)
        }
    } else {logDebug("Kitchen is Timeout",3)}

    return active
}

// ******************* Other Sensor Motion Active (if Timeed out) ***************************

// Entry Stairs set motion Active if Timeout
def entryStairsMotionHandler(evt) {
    logDebug("Entry Stairs Motion Activity ${evt.value}",3)
    if (evt.value == "active" && frontData.currentValue("frontMode") != "Night") {
        frontData.setStairsMotionStatus("Active")
        signalLamp.colorFade(1,"Yellow",3)
    }
}

// activate stairs light when sensor data changes stairsMotion to active
def stairsActiveHandler(evt) {    
    if (evt.value == "Active") {
        logDebug("Entry Stairs Motion ${evt.value}",1)
        if (frontData.currentValue("frontSensor") == "On" && frontData.currentValue("frontMode") != "Night") {
            livingRoomSceneDevices[findLivingRoomDevice("Entry Stairs")].setLevel(15)
            logDebug("Entry Stairs set to level 15",3)
        }
    }
}

// Front Door set motion Active if Timeout
def frontDoorMotionHandler(evt) {    
    if (evt.value == "active") {
        logDebug("Front Door Motion Activity ${evt.value}",3)
        frontData.setFrontDoorMotionStatus("Active")
        signalLamp.colorFade(1,"Violet",3)
    }
}

/* def pantrySwitchHandler(evt) {
    if (evt.value == "on") {
        runIn(1800, turnOffPantryLight)
    }
} */

def turnOffPantryLight() {
    kitchenSceneDevices[findKitchenDevice("Pantry Light")].setLevel(0)
}

// Set Pantry Motion Active
def pantryMotionHandler(evt) {    
    logDebug("Pantry Motion ${evt.value}",3)
    if (evt.value == "active") {
        frontData.setPantryMotionStatus("Active")
    }
}

// Activate Pantry Light
def pantryActiveHandler(evt) {    
    if (evt.value == "active") {
        logDebug("Pantry Motion ${evt.value}",1)
        if (frontData.currentValue("frontMode") != "Night") {
            logDebug("Pantry light index is ${findKitchenDevice("Pantry Light")}",3)
            kitchenSceneDevices[findKitchenDevice("Pantry Light")].setLevel(100)
            logDebug("Pantry Turned On",3)
        }
    }
}

// activate Front door when sensor data changes frontDoorMotion to active
def frontDoorActiveHandler(evt) {
    logDebug("Front Door Motion ${evt.value}",3)
    if (evt.value == "active") {
        if (illuminanceData.currentValue("sunset") == "true") {
            logDebug("Turning on outside lights",1)
            frontWallSwitches[findWallSwitch("Front Floodlight")].setLevel(90,5)     
            frontWallSwitches[findWallSwitch("Front Lanterns")].setLevel(50,5) 
        }           
    }
}

// ******************* Scene Timeout Handlers ***************************

// run LivingRoom All Off if Timeout
def livingRoomTimeoutHandler(evt) {    
    if (evt.value == "Timeout") {  
        logDebug("Living Room Timeout",1)
        runScene("livingRoom", "All Off")
    }
}

// run Kitchen All Off if Timeout
def kitchenTimeoutHandler(evt) {    
    if (evt.value == "Timeout") {    
        logDebug("Kitchen Timeout",1)
        kitchenSceneDevices[findKitchenDevice("Kitchen Sink Light")].setLevel(0, 5)       
        runScene("kitchen", "All Off")
    } 
}

// kitchen Activity Timeout Handler - sink light
def kitchenActivityTimeoutHandler(evt) { 
    if (evt.value == "Timeout") {   
        logDebug("Kitchen Activity Timeout",1)
        logDebug("Turning Off Kitchen Sink Light",3)       
        kitchenSceneDevices[findKitchenDevice("Kitchen Sink Light")].setLevel(0, 5)
    }
}

// Stairs Sensor Timeout - entry stairs
def stairsTimeoutHandler(evt) {   
    if (evt.value == "Timeout" && frontData.currentValue("frontMode") != "Dinner") {
        logDebug("Stairs Timeout",1)  
        frontWallSwitches[findLivingRoomDevice("Entry Stairs")].setLevel(0)
        logDebug("Entry Stairs turned off",3)
    }
}

// Front Door Lights Timeout - floodLight and frontLanterns
def frontDoorTimeoutHandler(evt) {   
    if (evt.value == "Timeout") {
        logDebug("Front Door Timeout",1)  
        if (illuminanceData.currentValue("sunset") == "true") {
            frontWallSwitches[findWallSwitch("Front Floodlight")].setLevel(0,5)     
            frontWallSwitches[findWallSwitch("Front Lanterns")].setLevel(25,5)
        } else {
            frontWallSwitches[findWallSwitch("Front Floodlight")].off()     
            frontWallSwitches[findWallSwitch("Front Lanterns")].off()   
        }   
    }
}

// Pantry Timeout Handler
def pantryTimeoutHandler(evt) {   
    logDebug("Pantry Timeout called with ${evt.value}",3) 
    if (evt.value == "Timeout") {
        logDebug("Pantry Timeout",1)  
        kitchenSceneDevices[findKitchenDevice("Pantry Light")].off()
        logDebug("Pantry turned off",3)
        kitchenSceneDevices[findKitchenDevice("Pantry Light")].off()
    }
}

// CHECK if Room is Timeout (returns true if timed out)
boolean timeoutCheck(room) {
    logDebug("Active check for ${room}",3)
    def timedOut = false

    livingRoomTimeout = frontData.currentValue("livingRoomMotionStatus") == "Timeout"
    kitchenTimeout = frontData.currentValue("kitchenMotionStatus") == "Timeout"
    if (room == "livingRoom" && livingRoomTimeout) timedOut = true
    if (room == "kitchen" && kitchenTimeout) timedOut = true

    return timedOut
}

// ******************* Scene Update Switch and Update Scenes ***************************

// update Scenes Switch Handler Event
def frontUpdateSwitchHandler(evt) {
    logDebug("Front Update Handler from Switch ${evt.value}",3)
    if (evt.value == "on") {
        updateScenes("both")   // switch has auto off
        //if (state?.xmasLights) sceneActivator.setChristmasScene(frontData.currentValue("frontMode"))
    }
}

def updateScenes(room) {
    logDebug("Update Scenes Called",3)
    currentMode = frontData.currentValue("frontMode")
    if (currentMode != "Night") {        
        if (room == "livingRoom" || room == "both") runScene("livingRoom", frontData.currentValue("frontMode"))
        pauseExecution(500)
        if (room == "kitchen" || room == "both") runScene("kitchen", frontData.currentValue("frontMode"))
    }
}

// ************************* MODE CHANGE ACTIONS: Mode Change Actions - Extra things to do only when the mode changes *********************

// FRONT MODE CHANGE Handler for dashboard Activity message 
def frontModeChangeHandler(evt) {
    logDebug("Front Mode changed to ${evt.value}",1)

    def scene = evt.value
    def activity = "Front Scene Changed to ${scene}"

    frontData.setFrontActivity(activity)                // set activity message on dashboard mode changed

    modeChangeActions(scene)                            // set other actions to take by scene     
}

def modeChangeActions(scene) {
    logDebug("Mode Change Actions for ${scene}",3)

    if (scene != "All Off" && scene != "Lamps Off") 

    if (scene == "Night") {

    }
    if (scene == "Morning") {
0 
    }    
    // ...
}

// ********************************************************                        ******************************************************************
// ******************************************************** EXECUTE SCENE METHODS  ******************************************************************
// ********************************************************                        ******************************************************************

// ****************************************** FROM SCENE ACTIVATOR - Runs with light level update  *********************************************
// Run Living Room Scene from Activator
def runLivingRoomHandler(evt) {
    def mode = evt.value
    logDebug("Run Living Room Scene from Activator ${mode}",3)

    if (evt.value != "Idle" && frontData.currentValue("livingRoomMotionStatus") != "Timeout") {
        if (checksPassed(mode)) executeScene("livingRoom", mode)
        logDebug("Running Living Room Scene from Activator",1)
    }
}

// Run Kitchen Scene from Activator
def runKitchenHandler(evt) {
    def mode = evt.value 
    logDebug("Run Kitchen Scene from Activator ${mode}",3)

    if (evt.value != "Idle" && frontData.currentValue("kitchenMotionStatus") != "Timeout") {
        if (checksPassed(mode)) executeScene("kitchen", mode)
        logDebug("Running Kitchen Scene from Activator",1)
    }
}

// **********************************************************    RUN SCENE      ******************************************************
// ************************* Will execute scene when conditions are met - most motion events use this *********************************

// Note: Motion event methods only call runScene when the roomm becomes active after timeout, and this is handled in Front Sensor Data with timeoutMinutes. 
def runScene(room, scene) {

    logDebug("runScene Event: ${room} ${scene} scene with DruHome switch ${state?.druHome}",3)
    if ((room == "livingRoom" || room == "kitchen") && scene != "All Off" && scene != "Lamps Off") frontData.setFrontMode(scene)
    def xmasEve = frontData.currentValue("xmasStatus") == "Eve"
    def xmasDay = frontData.currentValue("xmasStatus") == "Day"  // xmasStatus attribue set by Front Scene Controller Executoer Scheduler

    // Execute Front Scenes
    if (checksPassed(scene)) {
        if (xmasEve || xmasDay) {
            logDebug("Run Scene Called with Eve ${xmasEve} and Day ${xmasDay}",3)
            if (xmasEve) {
                executeScene("kitchen", "xmasEve") 
                executeScene("livingRoom", "xmasEve")
            }
            if (xmasDay) {
                executeScene("kitchen", "xmasDay") 
                executeScene("livingRoom", "xmasDay")                
            }
        } else {
            logDebug("Run Scene Called with ${scene}",3)
            if (room == "livingRoom") {
                executeScene("livingRoom", scene)           
            }
            if (room == "kitchen") {
                executeScene("kitchen", scene)    
            }        
            if (state?.xmasLights && !state?.xmasRan) {
                state.xmasRan == true
                sceneActivator.setChristmasScene(scene)
                runIn(10, resetXmasRan)
            }
        }
    }   
}

def resetXmasRan() {
    state.xmasRan == false
}

// checks for druHome, Sensor On and not null
boolean checksPassed(scene) {

    def druHomeCondition = (state?.druHome == false) ||  (state?.druHome == true && (scene == "Night" || scene == "All Off"))
    def frontSensorOn = frontSensorSwitch.currentValue("switch") == "on"

    if (druHomeCondition && frontSensorOn && scene != null) return true
    else return false
}

// **************************************************************************************************************************************
// ************  EXECUTE SCENE: Scenes will run the scene whenever called, with no checks for sensors, switches, attributes *************
// **************************************************************************************************************************************

def executeScene(room, scene) {

    logDebug("getScene Event: ${room} ${scene} scene",3)

    parser = new groovy.json.JsonSlurper() 

    def jsonText = ""
  
    if (room == "livingRoom") {
        jsonText = """{
        "Dawn":[ {"d":"Bottle Lamp","s":"off","l":"NA"}, {"d":"Ceramic Lamp","s":"on","l":"10"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"on","l":"20"},{"d":"Standing Lamp","s":"on","l":"lLL"},{"d":"Table Lamp","s":"on","l":"lLL"},{"d":"Tree Lamp","s":"off","l":"NA"}],
        "Morning":[ {"d":"Bottle Lamp","s":"off","l":"NA"}, {"d":"Ceramic Lamp","s":"on","l":"25"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"on","l":"20"},{"d":"Standing Lamp","s":"on","l":"lLL"},{"d":"Table Lamp","s":"on","l":"lLL"},{"d":"Tree Lamp","s":"on","l":"50"}],
        "Day":[ {"d":"Bottle Lamp","s":"off","l":"NA"}, {"d":"Ceramic Lamp","s":"on","l":"mLL"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"on","l":"lLL-10"},{"d":"Standing Lamp","s":"on","l":"mLL"},{"d":"Table Lamp","s":"on","l":"mLL"},{"d":"Tree Lamp","s":"on","l":"50"}],
        "Late Evening":[ {"d":"Bottle Lamp","s":"on","l":"mLL"}, {"d":"Ceramic Lamp","s":"on","l":"75"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"on","l":"20"},{"d":"Standing Lamp","s":"on","l":"mLL"},{"d":"Table Lamp","s":"on","l":"mLL"},{"d":"Tree Lamp","s":"off","l":"NA"}],
        "Dinner":[ {"d":"Bottle Lamp","s":"on","l":"mLL"}, {"d":"Ceramic Lamp","s":"on","l":"100"},{"d":"Entry Stairs","s":"on","l":"15"},{"d":"Living Room Ceiling","s":"on","l":"20"},{"d":"Standing Lamp","s":"on","l":"65"},{"d":"Table Lamp","s":"on","l":"65"},{"d":"Tree Lamp","s":"on","l":"lLL"}],
        "Evening":[ {"d":"Bottle Lamp","s":"on","l":"lLL"}, {"d":"Ceramic Lamp","s":"on","l":"50"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"on","l":"20"},{"d":"Standing Lamp","s":"on","l":"mLL"},{"d":"Table Lamp","s":"on","l":"lLL"},{"d":"Tree Lamp","s":"on","l":"lLL"}],
        "TV Time":[ {"d":"Bottle Lamp","s":"on","l":"25"}, {"d":"Ceramic Lamp","s":"on","l":"25"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"on","l":"25"},{"d":"Standing Lamp","s":"on","l":"lLL"},{"d":"Table Lamp","s":"on","l":"lLL"},{"d":"Tree Lamp","s":"on","l":"25"}],
        "Dim":[ {"d":"Bottle Lamp","s":"off","l":"NA"}, {"d":"Ceramic Lamp","s":"on","l":"5"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"on","l":"20"},{"d":"Standing Lamp","s":"on","l":"lLL"},{"d":"Table Lamp","s":"on","l":"lLL"},{"d":"Tree Lamp","s":"off","l":"NA"}],
        "Bright":[ {"d":"Bottle Lamp","s":"on","l":"mLL"}, {"d":"Ceramic Lamp","s":"on","l":"100"},{"d":"Entry Stairs","s":"on","l":"20"},{"d":"Living Room Ceiling","s":"on","l":"hLL"},{"d":"Standing Lamp","s":"on","l":"hLL"},{"d":"Table Lamp","s":"on","l":"hLL"},{"d":"Tree Lamp","s":"on","l":"mLL"}],
        "Lamps Off":[ {"d":"Bottle Lamp","s":"off","l":"NA"}, {"d":"Ceramic Lamp","s":"off","l":"0"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"on","l":"20"},{"d":"Standing Lamp","s":"off","l":"NA"},{"d":"Table Lamp","s":"off","l":"NA"},{"d":"Tree Lamp","s":"off","l":"NA"}],
        "All Off":[ {"d":"Bottle Lamp","s":"off","l":"NA"}, {"d":"Ceramic Lamp","s":"off","l":"0"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"off","l":"NA"},{"d":"Standing Lamp","s":"off","l":"NA"},{"d":"Table Lamp","s":"off","l":"NA"},{"d":"Tree Lamp","s":"off","l":"0"}],
        "Xmas Eve":[ {"d":"Bottle Lamp","s":"off","l":"NA"}, {"d":"Ceramic Lamp","s":"on","l":"50"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"off","l":"NA"},{"d":"Standing Lamp","s":"on","l":"mLL"},{"d":"Table Lamp","s":"on","l":"mLL"},{"d":"Tree Lamp","s":"on","l":"35"}],
        "Xmas Morning":[ {"d":"Bottle Lamp","s":"off","l":"NA"}, {"d":"Ceramic Lamp","s":"on","l":"75"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"off","l":"NA"},{"d":"Standing Lamp","s":"off","l":"NA"},{"d":"Table Lamp","s":"off","l":"NA"},{"d":"Tree Lamp","s":"on","l":"50"}],
        "Night":[ {"d":"Bottle Lamp","s":"off","l":"NA"}, {"d":"Ceramic Lamp","s":"off","l":"NA"},{"d":"Entry Stairs","s":"off","l":"NA"},{"d":"Living Room Ceiling","s":"off","l":"NA"},{"d":"Standing Lamp","s":"off","l":"NA"},{"d":"Table Lamp","s":"off","l":"NA"},{"d":"Tree Lamp","s":"off","l":"NA"}]""".stripIndent()
    }
    if (room == "kitchen") {
        jsonText = """{
        "Dawn":[ {"d":"Corner Lamp","s":"on","l":"lLL"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"on","l":"lLL"},{"d":"Kitchen Ceiling","s":"on","l":"20"},{"d":"Under Cab Lights","s":"on","l":"NA"}],
        "Morning":[ {"d":"Corner Lamp","s":"on","l":"lLL"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"on","l":"mLL+15"},{"d":"Kitchen Ceiling","s":"on","l":"20"},{"d":"Under Cab Lights","s":"on","l":"NA"}],
        "Day":[ {"d":"Corner Lamp","s":"on","l":"mLL"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"on","l":"mLL+15"},{"d":"Kitchen Ceiling","s":"on","l":"20"},{"d":"Under Cab Lights","s":"on","l":"NA"}],
        "Late Evening":[ {"d":"Corner Lamp","s":"on","l":"mLL"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"on","l":"mLL+15"},{"d":"Kitchen Ceiling","s":"on","l":"20"},{"d":"Under Cab Lights","s":"on","l":"NA"}], 
        "Evening":[ {"d":"Corner Lamp","s":"on","l":"lLL"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"on","l":"mLL"},{"d":"Kitchen Ceiling","s":"on","l":"20"},{"d":"Under Cab Lights","s":"on","l":"NA"}], 
        "Dinner":[ {"d":"Corner Lamp","s":"on","l":"hLL"}, {"d":"Fan Light","s":"on","l":"20"},{"d":"Island Lights","s":"on","l":"85"},{"d":"Kitchen Ceiling","s":"on","l":"20"},{"d":"Under Cab Lights","s":"on","l":"NA"}], 
        "TV Time":[ {"d":"Corner Lamp","s":"on","l":"lLL"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"on","l":"lLL"},{"d":"Kitchen Ceiling","s":"on","l":"20"},{"d":"Under Cab Lights","s":"on","l":"NA"}], 
        "Night":[ {"d":"Corner Lamp","s":"off","l":"NA"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"off","l":"NA"},{"d":"Kitchen Ceiling","s":"off","l":"NA"},{"d":"Under Cab Lights","s":"off","l":"NA"}], 
        "All Off":[ {"d":"Corner Lamp","s":"off","l":"NA"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"off","l":"NA"},{"d":"Kitchen Ceiling","s":"off","l":"NA"},{"d":"Under Cab Lights","s":"off","l":"NA"}], 
        "Lamps Off":[ {"d":"Corner Lamp","s":"off","l":"NA"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"off","l":"NA"},{"d":"Kitchen Ceiling","s":"NA","l":"NA"},{"d":"Under Cab Lights","s":"off","l":"NA"}], 
        "Xmas Eve":[ {"d":"Corner Lamp","s":"on","l":"hLL"}, {"d":"Fan Light","s":"on","l":"mLL"},{"d":"Island Lights","s":"on","l":"hLL"},{"d":"Kitchen Ceiling","s":"on","l":"20"},{"d":"Under Cab Lights","s":"on","l":"NA"}],
        "Xmas Morning":[ {"d":"Corner Lamp","s":"off","l":"NA"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"off","l":"NA"},{"d":"Kitchen Ceiling","s":"off","l":"NA"},{"d":"Under Cab Lights","s":"on","l":"NA"}], 
        "Bright":[ {"d":"Corner Lamp","s":"on","l":"hLL"}, {"d":"Fan Light","s":"on","l":"mLL"},{"d":"Island Lights","s":"on","l":"hLL"},{"d":"Kitchen Ceiling","s":"on","l":"20"},{"d":"Under Cab Lights","s":"on","l":"NA"}], 
        "Dim":[ {"d":"Corner Lamp","s":"on","l":"lLL"}, {"d":"Fan Light","s":"off","l":"NA"},{"d":"Island Lights","s":"on","l":"lLL"},{"d":"Kitchen Ceiling","s":"off","l":"NA"},{"d":"Under Cab Lights","s":"off","l":"NA"}]""".stripIndent()        
    }
    def data = parser.parseText(jsonText)
    logDebug("Scene data is ${data[scene]}",3)

    // set the scene that is executing ??
    if ((room == "livingRoom" || room == "kitchen") && scene != "All Off" && scene != "Lamps Off") frontData.setFrontMode(scene)

    def devicesInScene = data[scene].d // get devices in a scene to a list
    def sceneSwitches = data[scene].s // get switches in a scene to a list
    def sceneLevels = data[scene].l // get levels in a scene to a list

    def numSceneDevices = devicesInScene.size()

    logDebug("Executing ${room} ${scene} scene",1)
    for (i=0; i < numSceneDevices; i++) {

        if (!(sceneSwitches[i] == "NA" && sceneLevels[i] == "NA")) {
            def sceneDevice = devicesInScene[i]
            def deviceSwitch = sceneSwitches[i]
            def deviceLevel = sceneLevels[i]
            logDebug("${sceneDevice} : ${deviceSwitch} : ${deviceLevel}",3)
            commandDevice(sceneDevice,deviceSwitch,deviceLevel,room)
            pauseExecution(50)
        }        
    }
    extraActions(scene)    // run any extra actions on top of the scene devices
}

// run the scene commands    
def commandDevice(dev, sw, lvl, room) {
    logDebug("Execute delay is ${settings?.executeDelay}",4)
    def exDelay = settings?.executeDelay.toInteger()

    logDebug("commandDevice recived: dev=${dev}, sw=${sw}, lvl=${lvl}, room=${room}",4)
    if (lvl == "mLL") {logDebug("saw mLL in comand",4)}

    def i = 0
    if (room == "kitchen") {
        i = state?.kitchenSceneDevices[dev]
        logDebug("Kitchen ${dev} Index is ${i}",4)
    }
    if (room == "livingRoom") {
        i = state?.livingRoomSceneDevices[dev]
        logDebug("Living Room ${dev} Index is ${i}",4)
    }

    def label = dev
    logDebug("Sending commands for ${label}",3)   

    if (sw != "NA") {   
        logDebug("Turning Switch ${sw}",4)
        if (room == "livingRoom") {
            if (sw == "on" && livingRoomSceneDevices[i].currentValue("switch") != sw) livingRoomSceneDevices[i].on()
            if (sw == "off" && livingRoomSceneDevices[i].currentValue("switch") != sw) livingRoomSceneDevices[i].off()
        }
        if (room == "kitchen") {
            if (sw == "on" && kitchenSceneDevices[i].currentValue("switch") != sw ) kitchenSceneDevices[i].on()
            if (sw == "off" && kitchenSceneDevices[i].currentValue("switch") != sw ) kitchenSceneDevices[i].off()
        }
    }

    pauseExecution(exDelay)

    if (lvl != "NA" && sw != "off") {
        logDebug("Scene Level default is ${lvl}",4)

        def lvli = getLightLevel(lvl,label)
        if (lvli < 0) {lvli = 0}
      
        logDebug("Sending ${label} level ${lvli}",4)
        if (room == "livingRoom") livingRoomSceneDevices[i].setLevel(lvli,3)
        if (room == "kitchen") kitchenSceneDevices[i].setLevel(lvli,3)           
    }
}

// get level as integer for special light levels
Integer getLightLevel(lvl, label) {

    def level = lvl.toString()
    def add = 0
    if (lvl == "mLL") {logDebug("saw mLL in light levels",4)}

    if (label == "Standing Lamp" || label == "Table Lamp") { 
        if (frontData.currentValue("readingLight") == "true") {
            logDebug("Reading Mode is Enabled for Living Room",3)
            lvl = illuminanceData.currentValue("readingLightLevel")
        } 
    } else {
        logDebug("Evaluating lvl String for +",4)
        if (level.contains("+")) {
            lvl = level.split('\\+')[0]
            add = level.split('\\+')[1].toInteger()
            logDebug("Adjusting ${add} to level",4)      
        }  
        logDebug("Evaluating lvl String for -",4)
        if (level.contains("-")) {
            lvl = level.split('\\-')[0]
            add = level.split('\\-')[1].toInteger()
            add = -add
            logDebug("Adjusting ${add} to level",4)      
        }          
    }  
    logDebug("Evaluating lvl String for Light Levels",4)
    logDebug("lvl before light levels is ${lvl}",4)
    if (lvl == "mLL") {logDebug("saw mLL",4)}
    if (level.contains("lLL")) {lvl = illuminanceData.currentValue("lowLightLevel")}
    else if (level.contains("mLL") || lvl == "mLL") {lvl = illuminanceData.currentValue("medLightLevel")}
    else if (level.contains("hLL")) {lvl = illuminanceData.currentValue("highLightLevel")}      

    logDebug("lvl is ${lvl}",4)
    def lvli = lvl.toInteger() + add
    if (lvli > 100) lvli = 100

    return lvli
}

// ****************************** EXTRA ACTIONS - run extra actions not in scene - run with every scene update **************************
def extraActions(scene) {

    if (scene == "Night") {

    }
}

// log debug if no logLevel added
def logDebug(txt) {
    try {
        if (settings?.fileMode) {logToFile(txt)}
        if (settings?.debugMode) {
            log.warn("${app.label} - ${txt}")   // debug           
        }
    } catch(ex) {
        log.error("bad debug message")
    }    
}

// log by level when lvl supplied
def logDebug(txt, lvl){
    try {
        logLevel = settings?.logLevel.toInteger()
        if (settings?.fileMode && logLevel < 4) {logToFile(txt, lvl)}
        if (settings?.debugMode) {
            if (lvl == 3 && logLevel == 3) {
                log.debug("${app.label} - ${txt}")               
            }       // debug
            else if (lvl >= 2 && logLevel >= 2) {
                log.warn("${app.label} - ${txt}")
            }   // warn
            else if (lvl >= 1 && logLevel >= 1) {
                log.info("${app.label} - ${txt}")
            }   // info
        }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logToFile(txt) {
    logToFile(txt,0)
}

def logToFile(txt,lvl) {
    def level = getLogLevel(lvl)
    String log = "(${level}) ${txt}"  
    fileManager.log(settings?.logFileName,log)
}

def clearLog() {
    fileManager.clearLog(settings?.logFileName)
}

String getLogLevel(lvl) {
    def level = "None"
    if (lvl == 1) {level = "Info"}
    if (lvl == 2) {level = "Warn"}
    if (lvl == 3) {level = "Debg"}
    if (lvl == 4) {level = "Xtra"}
    return level
}

def logDebugOff() {
    logDebug("Turning off debugMode")
    app.updateSetting("logLevel",[value:"1",type:"enum"])
}

def logFileOff() {
    logDebug("Turning off fileMode Logging")
    app.updateSetting("fileMode",[value:false,type:"bool"])
}