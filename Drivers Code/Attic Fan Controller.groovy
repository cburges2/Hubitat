/*
	Attic Fan Controller

	Copyright 2023 -> C. Burgess

	This driver works with a companion app, "Attic Fan Controller App", to control an attic fan to vent for temperature and/or humidity.  

	v. 1.0 - 4/8/23  - Inital code to create an attic fan controller using temp and humidity to turn on a fan switch
	v. 1.2 - 4/11/23 - Updated to use a fan speed controller (low, medium, high) and set fan speed manually or auto in pref.  Auto sets speed based on humidity and temperature differences. 
    v. 1.5 - 4/14/23 - Added Pref for setting the fan speed thresholds (difference between temp or humidity setpoints to change to medium or high)
    v. 1.6 - 4/25/23 - Added Override Temp to vent for Temperature if attic temp is over override temp, regardless of outside humidity. 
*/

metadata {
	definition (
		name: "Attic Fan Controller",
		namespace: "hubitat",
		author: "Chris B"
	) {
        //capability "Light"
        capability "Switch"           // makes this a switch device, if needed
		capability "Actuator"
        capability "Presence Sensor"  // overidden as presence of "humidity", "temperature", "both" or "none" (for dashboard colors)

		// attributes
		attribute "operatingState", "ENUM"  		// ["venting", "idle"]
		attribute "atticHumidity", "ENUM"	        // set from app
		attribute "outsideHumidity", "ENUM"			// set from app
		attribute "display", "STRING"       	    // attribute for dashboad status tile showing temps, humids, operatitonal state and what venting for. 
        attribute "display2", "STRING"				// attribute for dashboad status tile showing fanSpeed, setpoints and offset. 
        attribute "atticTemp", "NUMBER"             // set from app
        attribute "outsideTemp", "NUMBER"           // set from app
        attribute "atticTempSetpoint", "NUMBER"     // fan will not run for temperature difference if attic temp is less than this setpoint (will still run for humidity)
        attribute "atticHumidSetpoint", "ENUM"      // fan will not run for humidity difference if attic humidity is greater than this setpoint (will still run for temp)
        attribute "fanSpeed", "ENUM"   			    // low, medium, high - manually set or auto set fan speed, based on pref setting
        attribute "presence", "ENUM"                // venting for humidity, temp, both, or none
		attribute "maxTempHumidity","ENUM"          // max outside humidity to allow for temperature venting (set in preferences)
		attribute "overrideTemp","NUMBER"			// attic temp where fan will run regardless of outside humidtiy and max temp humidity setting (set in prefrences)
		attribute "icon", "STRING"

		// Commands needed to change internal attributes of virtual device.
        command "setOperatingState", [[name:"operatingState",type:"ENUM", description:"Set Operating State", constraints:["venting","idle"]]]
		command "setAtticHumidity", ["ENUM"]
		command "setOutsideHumidity", ["ENUM"]
        command "setHysteresis", ["NUMBER"]
        command "manageCycle"
        command "setDisplay"
        command "setAtticTemp", ["NUMBER"]
        command "setOutsideTemp", ["NUMBER"]
        command "setAtticTempSetpoint", ["NUMBER"]
        command "setAtticHumidSetpoint", ["ENUM"]
        command "setFanSpeed", [[name:"fanSpeed",type:"ENUM", description:"Set Fan Speed", constraints:["low","medium","high"]]]
        command "setPresence", [[name:"presence",type:"ENUM", description:"Set Why Venting", constraints:["humidity","temperature","both","none"]]]
		command "setIcon", ["STRING"]
}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
		input( name: "maxTempHumidity", type:"enum", title: "Max Outside Humidity to run for Temperature", defaultValue:"55", options:[30:"30%",35:"35%",40:"40%",45:"45%",50:"50%",55:"55%",60:"60%",65:"65%",70:"70%",75:"75%"])
		input( name: "overrideTemp", type:"number", title: "Attic Temp that overrides Max Outside Humidity", defaultValue:"95")
		input( name: "autoFan", type:"bool", title: "Enable auto fan speed", defaultValue:true)
		input( name: "fanMediumThreshold", type:"enum", title: "Fan Medium Speed Threshold", defaultValue:"3", options:[1:"1",2:"2",3:"3",4:"4",5:"5"])
		input( name: "fanHighThreshold", type:"enum", title: "Fan High Speed Threshold", defaultValue:"6", options:[3:"3",4:"4",5:"5",6:"6",7:"7",8:"8",9:"9",10:"10",11:"11",12:"12"])
	}
}

def installed() {
	log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true])
	
	setOperatingState("idle")
    setAtticHumidSetpoint(45)
    setAtticHumidity(50)
    setAtticHumidityOffset(2)
    setAtticTemp(75.0)
    setAtticTempSetpoint(80)
    setfanSpeed("auto")
    setOutsideHumidity(50)
    setOutsideTemp(75.0)
    setOutsideHumidity(50)
    setPresence("none")
    setDisplay()
    state.humidTrend = "steady"
    state.tempTrend = "steady"
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
	    sendEvent(name: "Operating State", value: (operatingState ?: "idle"))              
		state.lastRunningMode = "idle"
    }	
}

def parse(String description) { noCommands("parse") }

private eventSend(name,verb,value,unit = ""){
    String descriptionText = "${device.displayName} ${name} ${verb} ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    if (unit != "") sendEvent(name: name, value: value ,descriptionText: descriptionText, unit:unit)
    else  sendEvent(name: name, value: value ,descriptionText: descriptionText)
}

def on() {
    String verb = (device.currentValue("switch") == "on") ? "is" : "was turned"
    eventSend("switch",verb,"on")
    sendEvent(name: "operatingState", value: "venting", descriptionText: getDescriptionText("operatingState set to venting")) 
    runIn(1,setDisplay)
}

def off() {
    String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
    eventSend("switch",verb,"off")
    sendEvent(name: "operatingState", value: "idle", descriptionText: getDescriptionText("operatingState set to idle")) 
    runIn(1,setDisplay)    
}

Integer limitIntegerRange(value,min,max) {
    Integer limit = value.toInteger()
    return (limit < min) ? min : (limit > max) ? max : limit
}

def setDisplay() {
	logDebug "setDisplay() was called"
    String display = "Attic: "+(device.currentValue("atticHumidity"))+"% "+(device.currentValue("atticTemp"))+"°<br>  Out: "+(device.currentValue("outsideHumidity"))+"% "+(device.currentValue("outsideTemp"))+"°<br> "+(device.currentValue("operatingState"))+" "+(device.currentValue("presence")) 
    String display2 = "%° Setpoint: "+settings?.maxTempHumidity+"%<br>°% Setpoint: "+settings?.overrideTemp+"°<br>% Setpoint: "+(device.currentValue("atticHumidSetpoint"))+"% <br> ° Setpoint: "+(device.currentValue("atticTempSetpoint"))+"°" 
    sendEvent(name: "display", value: display, descriptionText: getDescriptionText("display set to ${display}"))
    sendEvent(name: "display2", value: display2, descriptionText: getDescriptionText("display2 set to ${display2}"))
	setPrefAttributes()
}

def setOperatingState (state) {
	logDebug "setOperatingState(${state}) was called"
    sendEvent(name: "operatingState", value: state, descriptionText: getDescriptionText("operatingState set to ${state}"))   
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def setPrefAttributes() {
	def mth = settings?.maxTempHumidity.toInteger()
	sendEvent(name: "maxTempHumidity", value: mth, descriptionText: getDescriptionText("maxTempHumidity set to ${mth}"))
	def ot = settings?.overrideTemp.toBigDecimal()
	sendEvent(name: "overrideTemp", value: ot, descriptionText: getDescriptionText("overrideTemp set to ${ot}"))
}

def manageCycle() {
	def atticHumidity = device.currentValue("atticHumidity").toInteger()
	def outsideHumidity = device.currentValue("outsideHumidity").toInteger()
	def atticHumidSetpoint = (device.currentValue("atticHumidSetpoint")).toInteger()	

	def atticTemp = (device.currentValue("atticTemp")).toBigDecimal()
	def outsideTemp = (device.currentValue("outsideTemp")).toBigDecimal()
	def atticTempSetpoint = (device.currentValue("atticTempSetpoint")).toBigDecimal()

	def operatingState = device.currentValue("operatingState")
	def presence = device.currentValue("presence")
	def fanMode = device.currentValue("fanMode")
	def maxHumid = settings?.maxTempHumidity.toInteger()
	def maxTemp = settings?.overrideTemp.toBigDecimal()
    
	// temp and humidity checks
	def humidTemp = (outsideHumidity <= maxHumid) || (atticTemp > maxTemp)
	def atticHumidityOn = (atticHumidity > outsideHumidity) && (atticHumidity > atticHumidSetpoint)		  // checks if vent for temp within outside humidity setpoint
	def runTemp = (atticHumidity < outsideHumidity)                           // checks humidity difference and humidTemp setpoint before venting for temperature difference
	def tempOn = (atticTemp > outsieTemp) && (atticTemp > atticTempSetpoint)  // checks to vent for temp difference only if attic temp greater than setpoint. 

    // define temperature actions
	def onTemp = (tempOn && runTemp && humidTemp)             // vent for temp if idle, and humidity in range 
	def offTemp = (!tempOn && !atticHumidityOn)  // idle for temp if venting, unless needs to stay on for humidity

    // define humidity actions
	def onHumid = (atticHumidityOn && humidTemp)   // vent for humidity if idle and within checks
	def offHumid = (!atticHumidityOn)              // idle for humidity if not already idle

	logDebug "onTemp is (${onTemp})"
	logDebug "offTemp is (${offTemp})"
	logDebug "onHumid is (${onHumid})"
	logDebug "offHumid is (${offHumid})"

    // if any conditions are true, update presence
	if ((offTemp==true || offHumid==true) && (onTemp==false && onHumid==false)) {
		setPresence("none")
		presence = "none"	
		setIcon("attic-idle.svg")	
	} 

	if ((offTemp==false && offHumid==false) && (onTemp==true & onHumid==true)) {
		setPresence("both")
		presence = "both"
		setIcon("temp-humidity.svg")
	} else {
		if (onTemp==true || onHumid==true) {
			// vent or idle for temp
			if (onTemp) {
				setPresence("temperature")
				presence = "temperature"
				setIcon("attic-temp.svg")
			}
			// vent or idle for humdity
		    if (onHumid) {    	   
		    	setPresence("humidity")
		    	presence = "humidity"
				setIcon("attic-humidity.svg")
		    }   
		}
	}

    // set auto speed
	if (settings?.autoFan) {

		logDebug("Setting Auto Fan Speed")
		def humidDiff = atticHumidity - atticHumidSetpoint
		def tempDiff = atticTemp - atticTempSetpoint

	    def diffUsed = "humid"

	    if (presence == "temperature") {
	        diffUsed = "temp"
	        logDebug("Using Temperature Difference $diffUsed")       
	    	} else if (presence == "humidity") {
		        logDebug("Using Humidity Difference of $diffUsed")
		    } else if (presence == "both") {
	    		if (tempDiff > humidDiff) {
	    			diffUsed = "temp"
	    			logDebug("Using Temperature Difference $diffUsed") 
	    		} else {
	    			diffUsed = "humid"
	    			logDebug("Using Humidity Difference of $diffUsed")
	    		}
   			} else if (presence == "none") {
	        	logDebug("speed not set - none")
	        	diffUsed = "none"
	    	}
        if (diffUsed == "humid") def diff = humidDiff
            else def diff = tempDiff
		def medDiff = settings?.fanMediumThreshold.toInteger()
        def highDiff = settings?.fanHighThreshold.toInteger()
	    if (diff > 0 && diffUsed != "none") {
	        if (diff > 0 && diff < medDiff) {
	            setFanSpeed("low")
	            logDebug("speed set to low")
	           } else if (diff >= medDiff && diff < highDiff) {
	                setFanSpeed("medium")
	                logDebug("speed set to medium")
	                } else if (diff >= highDiff) {
	                    setFanSpeed("high")
	                    logDebug("speed set to high")
	                }     
	    	} else {
	      	  logDebug("difference is less than zero")
	   		}

	    // bump up speed if humid or temp is rising while at low or medium speed
	    def speed = device.currentValue("fanSpeed")
	    if ((state.tempTrend == "rising" && onTemp) || (state.humidTrend == "rising" && onHumid)) {
	    	if (speed == "low") setFanSpeed("medium")
	    	if (speed == "medium") setFanSpeed("high")
	    }
	}     

    // change fan State based on presence and operatingState 
    if (presence != "none" && operatingState != "venting") {
    	setOperatingState("venting")
    	runIn(1, on)
        logDebug "venting on"
    }
    else if (presence == "none" && operatingState !="idle") {
        setOperatingState("idle")
    	runIn(1, off)
    	logDebug "venting off"
    }	

    state.humidTrend = "steady"
    state.tempTrend = "steady"
    
	state.lastRunningMode = device.currentValue(operatingState)
	logDebug "Finshed manageCycle()"
    runIn(1,setDisplay)	
}

// Commands needed to change internal attributes of virtual device.
def setAtticHumidity(atticHumidity) {
	logDebug "setAtticHumidity(${atticHumidity}) was called"
	def current = device.currentValue("atticHumidity").toInteger()
	if (current > atticHumidity.toInteger()) state.humidTrend = "rising"
	else state.humidTrend = "falling"		
    sendEvent(name: "atticHumidity", value: atticHumidity, unit: "%", descriptionText: getDescriptionText("atticHumidity set to ${atticHumidity}%"))
    runIn(1, manageCycle)
}

def setOutsideHumidity(setpoint) {
	logDebug "setOutsideHumidity(${setpoint}) was called"
    sendEvent(name: "outsideHumidity", value: setpoint, descriptionText: getDescriptionText("outsideHumidity set to ${setpoint}"))
    runIn(1, manageCycle)    
}

def setAtticTemp(temp) {
	logDebug "setAtticTemp(${temp}) was called"
	def current = device.currentValue("atticTemp").toInteger()
	if (current > temp.toInteger()) state.tempTrend = "rising"
	else state.tempTrend = "falling"	
    sendEvent(name: "atticTemp", value: temp, descriptionText: getDescriptionText("atticTemp set to ${temp}"))
    runIn(1, manageCycle)
}

def setOutsideTemp(temp) {
	logDebug "setOutsideTemp(${temp}) was called"
    sendEvent(name: "outsideTemp", value: temp, descriptionText: getDescriptionText("outsideTemp set to ${temp}"))
    runIn(1, manageCycle)
}

def setAtticTempSetpoint(temp) {
	logDebug "setAtticTempSetpoint(${temp}) was called"
    sendEvent(name: "atticTempSetpoint", value: temp, descriptionText: getDescriptionText("atticTempSetpoint set to ${temp}"))
    runIn(1, manageCycle)
}

def setAtticHumidSetpoint(humid) {
	logDebug "setAtticHumidSetpoint(${humid}) was called"
    sendEvent(name: "atticHumidSetpoint", value: humid, descriptionText: getDescriptionText("atticHumidSetpoint set to ${humid}"))
    runIn(1, manageCycle)
}

def setPresence(state) {
	logDebug "setPresence(${state}) was called"
    sendEvent(name: "presence", value: state, descriptionText: getDescriptionText("presence set to ${state}"))
}

def setFanSpeed(speed) {
	logDebug "setFanSpeed(${speed}) was called"
    sendEvent(name: "fanSpeed", value: speed, descriptionText: getDescriptionText("fanSpeed set to ${speed}"))
}

def setFanMode(mode) {
	logDebug "setFanMode(${mode}) was called"
    sendEvent(name: "fanMode", value: mode, descriptionText: getDescriptionText("fanMode set to ${mode}"))
}

def setIcon(img) {
    String iconPath = "https://cburges2.github.io/ecowitt.github.io/Dashboard%20Icons/"
    sendEvent(name: "icon", value: "<img class='icon' src='${iconPath}${img}' />")
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}
