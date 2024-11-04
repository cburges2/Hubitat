/*

Data Storage Driver

*/

metadata {
	definition (
			name: "Illuminance Data",
			namespace: "hubitat",
			author: "Chris B."
	) { //TYPES: ENUM, STRING, DYNAMIC_ENUM, JSON_OBJECT, NUMBER, DATE, VECTOR3
	
		capability "Actuator"
		capability "PresenceSensor"

		attribute "illuminanceVariance", "NUMBER"
		attribute "indoorIlluminance", "NUMBER"
		attribute "sensorIlluminance", "NUMBER"
		attribute "luxDisplay", "STRING"
        attribute "maxIlluminance", "NUMBER"
        attribute "avgIlluminance", "NUMBER"
		attribute "lowLightLevel", "NUMBER"
        attribute "highLightLevel", "NUMBER"
        attribute "medLightLevel", "NUMBER"
        attribute "lightLevels", "STRING"
        attribute "insideLightLevel", "NUMBER"
        attribute "readingLightLevel","NUMBER"
        attribute "sunset", "BOOL"
        attribute "lowLight", "ENUM"
        attribute "dayLight", "ENUM"
        attribute "cloudiness", "ENUM"
        attribute "noonIlluminance", "NUMBER"
        attribute "daysFromSolstice", "NUMBER"
        attribute "weekOfYear","NUMBER"
        attribute "dayOfYear","NUMBER"
        attribute "lightSensor", "ENUM"
        attribute "sunValue", "NUMBER"
        attribute "lightIntensity", "NUMBER"
		attribute "lightTarget", "ENUM"
		attribute "sunsetThreshold", "ENUM"
		attribute "lowLightThreshold", "ENUM"
		attribute "dayLightThreshold", "ENUM"
		attribute "cloudySunRatio", "ENUM"
		attribute "luxChangePerDay", "ENUM"
		attribute "presence", "ENUM"
		attribute "season", "ENUM"
		//attribute "moonPhase", "ENUM"

		// Commands needed to change internal attributes of virtual device.
        command "setIlluminanceVariance",[[name:"Illuminance Variance",type:"NUMBER"]]
		command "setIndoorIlluminance", ["NUMBER"]
        command "setSensorIlluminance",[[name:"Sensor Illuminance",type:"NUMBER"]]	
        command "setMaxIlluminance",[[name:"Max Illuminance",type:"NUMBER"]]
        command "setAvgIlluminance",[[name:"Average Hourly Illuminance",type:"NUMBER"]]
        command "setLowLightLevel",[[name:"Low Light Level",type:"NUMBER"]]
		command "setHighLightLevel",[[name:"High Light Level",type:"NUMBER"]]
        command "setMedLightLevel",[[name:"Medium Light Level",type:"NUMBER"]]
        command "setLightLevels",["STRING"]
        command "setInsideLightLevel",[[name:"Inside Light Level Lux",type:"NUMBER"]]
        command "setReadingLightLevel",[[name:"Reading Light Level",type:"NUMBER"]]
        command "setSunset", [[name:"sunset",type:"ENUM", description:"Set Sunset", constraints:["true","false"]]]
        command "setDayLight",[[name:"daylight",type:"ENUM", description:"Set Daylight", constraints:["true","false"]]]
        command "setLowLight",[[name:"lowlight",type:"ENUM", description:"Set Lowlight", constraints:["true","false"]]]
		command "setSeason",[[name:"season",type:"ENUM", description:"Set Season", constraints:["spring","summer","fall","winter"]]]
        command "setCloudiness",["ENUM"]
        command "setNoonIlluminance",[[name:"Noon Illuminance",type:"NUMBER"]]
        command "setDaysFromSolstice",[[name:"Days from Solstice",type:"NUMBER"]]
        command "setWeekOfYear",[[name:"Week Number of Year",type:"NUMBER"]]
        command "setDayOfYear",[[name:"Day Number of Year",type:"NUMBER"]]
        command "setLightSensor",[[name:"lightSensor",type:"ENUM", description:"Set lightSensor", constraints:["true","false"]]]
        command "setSunValue",[[name:"Sun Value",type:"NUMBER"]]
        command "setLightIntensity",[[name:"Light Intensity",type:"NUMBER"]]
        command "setLightLevels"	
		command "setLightTarget", ["ENUM"]
		command "setSunsetThreshold", ["ENUM"]
		command "setDayLightThreshold", ["ENUM"]
		command "setLowLightThreshold", ["ENUM"]
		command "setLuxDisplay"
		command "setCloudySunRatio", ["ENUM"]
		command "setLuxChangePerDay", ["ENUM"]
		command "setPresence", [[name:"presence",type:"ENUM", description:"Set Alert Presence", constraints:["present","not present"]]]
	}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)      
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

def setSeason(value) {
	logDebug "setSeason(${value}) was called"
	sendEvent(name: "season", value: value, descriptionText: getDescriptionText("season was set to ${value}"),stateChange: true)
}

// Commands needed to change internal attributes of virtual device.
def setLuxDisplay() {
	String luxDisplay = "Out: "+device.currentValue("sensorIlluminance")+" lux<br>Inside: "+device.currentValue("indoorIlluminance")+" lux<br>Target: "+device.currentValue("lightTarget")+" lux"
    sendEvent(name: "luxDisplay", value: luxDisplay, descriptionText: getDescriptionText("luxDisplay was set to ${luxDisplay}"),stateChange: true)
}

def setLowLightLevel(value) {
	logDebug "setLowLightLevel(${value}) was called"
	sendEvent(name: "lowLightLevel", value: value, descriptionText: getDescriptionText("lowLightLevel was set to ${value}"),stateChange: true)
    runIn(3,setLightLevels)
}

def setMedLightLevel(value) {
	logDebug "setMedLightLevel(${value}) was called"
	sendEvent(name: "medLightLevel", value: value, descriptionText: getDescriptionText("medLightLevel was set to ${value}"),stateChange: true)
    runIn(2,setLightLevels)
}

def setInsideLightLevels(value) {
	logDebug "setinsideLightLevels(${value}) was called"
	sendEvent(name: "insideLightLevels", value: value, descriptionText: getDescriptionText("insideLightLevels was set to ${value}"),stateChange: true)
	runIn(1, setLuxDisplay)
}

def setHighLightLevel(value) {
	logDebug "setHighLightLevel(${value}) was called"
	sendEvent(name: "highLightLevel", value: value, descriptionText: getDescriptionText("highLightLevel was set to ${value}"),stateChange: true)
    runIn(1,setLightLevels)
}

def setReadingLightLevel(value) {
	logDebug "setReadingLightLevel(${value}) was called"
	sendEvent(name: "readingLightLevel", value: value, descriptionText: getDescriptionText("readingLightLevel was set to ${value}"),stateChange: true)
}

def setSunset(bool) {
	logDebug "setSunset(${bool}) was called"
	sendEvent(name: "sunset", value: bool, descriptionText: getDescriptionText("sunset was set to ${bool}"),stateChange: true)
}

def setDayLight(bool) {
	logDebug "setDayLight(${bool}) was called"
	sendEvent(name: "dayLight", value: bool, descriptionText: getDescriptionText("dayLight was set to ${bool}"),stateChange: true)
}

def setLowLight(bool) {
	logDebug "setLowLight(${bool}) was called"
	sendEvent(name: "lowLight", value: bool, descriptionText: getDescriptionText("lowLight was set to ${bool}"),stateChange: true)
}

def setIlluminanceVariance(value) {
	logDebug "setIlluminanceVariance(${value}) was called"
	sendEvent(name: "illuminanceVariance", value: value, descriptionText: getDescriptionText("illuminanceVariance was set to ${value}"),stateChange: true)
}

def setSensorIlluminance(value) {
	logDebug "setSensorIlluminance(${value}) was called"
	sendEvent(name: "sensorIlluminance", value: value, descriptionText: getDescriptionText("sensorIlluminance was set to ${value}"),stateChange: true)
	// Update flags
	def lux = value.toInteger()
    Date date = new Date();   // given date
    Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
    calendar.setTime(date);   // assigns calendar to given date 
    def hour24 = calendar.get(Calendar.HOUR_OF_DAY).toInteger() 

    def sunsetThres = device.currentValue("sunsetThreshold").toInteger()
    def lowLightThres = device.currentValue("lowLightThreshold").toInteger()
    def dayLightThres = device.currentValue("dayLightThreshold").toInteger()
    logDebug("${sunsetThres} ${lowLightThres} ${dayLightThres}")

    def sunset = device.currentValue("sunset") == "true"
    def lowLight = device.currentValue("lowLight") == "true"
    def dayLight = device.currentValue("dayLight") == "true"
    logDebug("${sunset} ${lowLight} ${dayLight}")

    if ((lux < sunsetThres) && !sunset && hour24 > 15) {
        setSunset("true")
        logDebug("sunset set to true")
		setLowLight("false")
    }
    if ((lux > sunsetThres) && sunset && hour24 < 10) {
        setSunset("false")
        logDebug("sunset set to false")
    }    
    if ((lux < lowLightThres && lux > sunsetThres) && !lowLight) {
        setLowLight("true")
        logDebug("lowLight set to true")
    }
/*     if ((lux < lowLightThres) && !lowLight) {
        setLowLight("false")
        logDebug("lowLight set to true")
    }	 */
    if ((lux > lowLightThres && lux < dayLightThres) && lowLight) {
        setLowLight("false")
        logDebug("lowLight set to false")
    }
    if ((lux > dayLightThres) && (!dayLight) && (hour24 < 12)) {
        setDayLight("true")
        logDebug("dayLight set to true")
    }
    if ((lux < dayLightThres) && (dayLight) && (hour24 > 15)) {
        setDayLight("false")
        logDebug("dayLight set to false")
    }    	
	runIn(1, setLuxDisplay)
}
                                     
def setMaxIlluminance(value) {
	logDebug "setMaxIlluminance(${value}) was called"
	sendEvent(name: "maxIlluminance", value: value, descriptionText: getDescriptionText("maxIlluminance was set to ${value}"),stateChange: true)
}

def setCloudiness(clouds) {
	logDebug "setCloudiness(${clouds}) was called"
	sendEvent(name: "cloudiness", value: clouds, descriptionText: getDescriptionText("cloudiness was set to ${clouds}"),stateChange: true)
}

def setNoonIlluminance(lux) {
	logDebug "setNoonIlluminance(${lux}) was called"
	sendEvent(name: "noonIlluminance", value: lux, descriptionText: getDescriptionText("noonIlluminance was set to ${lux}"),stateChange: true)
}

def setDaysFromSolstice(days) {
	logDebug "setDaysFromSolstice(${days}) was called"
	sendEvent(name: "daysFromSolstice", value: days, descriptionText: getDescriptionText("daysFromSolstice was set to ${days}"),stateChange: true) 
}

def setWeekOfYear(week) {
	logDebug "setDaysFromSolstice(${week}) was called"
	sendEvent(name: "weekOfYear", value: week, descriptionText: getDescriptionText("weekOfYear was set to ${week}"),stateChange: true) 
}

def setDayOfYear(day) {
	logDebug "setDayOfYear(${day}) was called"
	sendEvent(name: "dayOfYear", value: day, descriptionText: getDescriptionText("dayOfYear was set to ${day}"),stateChange: true) 
}

def setLightSensor(set) {
	logDebug "setLightSensor(${set}) was called"
	sendEvent(name: "lightSensor", value: set, descriptionText: getDescriptionText("lightSensor was set to ${set}"),stateChange: true) 
	runIn(1, setLuxDisplay)
}

def setSunValue(set) {
	logDebug "setSunValue(${set}) was called"
	sendEvent(name: "sunValue", value: set, descriptionText: getDescriptionText("sunValue was set to ${set}"),stateChange: true) 
}

def setLightIntensity(set) {
	logDebug "setLightIntensity(${set}) was called"
	sendEvent(name: "lightIntensity", value: set, descriptionText: getDescriptionText("lightIntensity was set to ${set}"),stateChange: true) 
}

def setAvgIlluminance(set) {
	logDebug "setAvgIlluminance(${set}) was called"
	sendEvent(name: "avgIlluminance", value: set, descriptionText: getDescriptionText("avgIlluminance was set to ${set}"),stateChange: true) 
}

def setLightLevels() {
	logDebug "setLightLevels() was called"
    String levels = "Low Level: "+(device.currentValue("lowLightLevel"))+"%  Medium: "+(device.currentValue("medLightLevel"))+"%  High: "+(device.currentValue("highLightLevel"))+"%"
	levels = getScrollingAttribute(levels)
	
	sendEvent(name: "lightLevels", value: levels, descriptionText: getDescriptionText("lightLevels was set to ${levels}"),stateChange: true)
	runIn(1, setLuxDisplay)
}

def getScrollingAttribute(string) {
	return setScrollingAttribute(string ,"slide","transparent", "up", "24", "0", "-1", "10", "100", "0", "80", "11", "center", "18")
}

String setScrollingAttribute(value, behavior, bgcolor, direction, height, hspace, loop, scrollamount, scrolldelay, vspace, width, fontSize, textAlign, textHeight) {
    logDebug("${value} ${behavior} ${bgcolor} ${direction} ${height} ${hspace} ${scrollamount} ${scrolldelay} ${vspace} ${width} ${fontSize} ${textAlign} ${textHeight}")
    String descriptionText = "${device.displayName} ${attribute} ${value}"
	logDebug("setAttribute() was called")

    String html = """<marquee behavior="${behavior}" bgcolor="${bgcolor}" direction="${direction}" height=
    "${height}" hspace="${hspace}" loop="${loop}" scrollamount="${scrollamount}" scrolldelay="${scrolldelay}" vspace"${vspace}" width="${width}%" style=
    "font-size:${fontSize}pt; text-align:${textAlign}; height:${textHeight}px;">${value}</marquee>"""

    logDebug("${html}")
	return html
}

def setIndoorIlluminance(set) {
	logDebug "setIndoorIlluminance(${set}) was called"
	sendEvent(name: "indoorIlluminance", value: set, descriptionText: getDescriptionText("indoorIlluminance was set to ${set}"),stateChange: true) 
	runIn(1, setLuxDisplay)
}

def setLightTarget(set) {
	logDebug "setLightTarget(${set}) was called"
	sendEvent(name: "lightTarget", value: set, descriptionText: getDescriptionText("lightTarget was set to ${set}"),stateChange: true) 
	runIn(1, setLuxDisplay)
}

def setDayLightThreshold(set) {
	logDebug "setDayLightThreshold(${set}) was called"
	sendEvent(name: "dayLightThreshold", value: set, descriptionText: getDescriptionText("dayLightThreshold was set to ${set}"),stateChange: true) 
}

def setLowLightThreshold(set) {
	logDebug "setLowLightThreshold(${set}) was called"
	sendEvent(name: "lowLightThreshold", value: set, descriptionText: getDescriptionText("lowLightThreshold was set to ${set}"),stateChange: true) 
}

def setSunsetThreshold(set) {
	logDebug "setSunsetThreshold(${set}) was called"
	sendEvent(name: "sunsetThreshold", value: set, descriptionText: getDescriptionText("sunsetThreshold was set to ${set}"),stateChange: true) 
}

def setCloudySunRatio(set) {
	logDebug "setCloudySunRatio(${set}) was called"
	sendEvent(name: "cloudySunRatio", value: set, descriptionText: getDescriptionText("cloudySunRatio was set to ${set}"),stateChange: true) 
}

def setLuxChangePerDay(set) {
	logDebug "setLuxChangePerDay(${set}) was called"
	sendEvent(name: "luxChangePerDay", value: set, descriptionText: getDescriptionText("luxChangePerDay was set to ${set}"),stateChange: true) 
}

def setPresence(set) {
	logDebug "setPresence(${set}) was called"
	sendEvent(name: "presence", value: set, descriptionText: getDescriptionText("presence was set to ${set}"),stateChange: true) 	
}

def parse(String description) {
	logDebug "$description"
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