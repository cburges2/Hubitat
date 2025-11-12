/*
	Doywoy
	Copyright 2025 -> Chris B

*/

import java.time.*

metadata {
	definition (
			name: "Doywoy",
			namespace: "Hubitat",
			author: "Chris B"
	) {
		capability "Actuator"

        attribute "dayOfYear", "NUMBER"
        attribute "weekOfYear", "NUMBER"
        attribute "dayOfWeek", "NUMBER"

        command "calc"
        command "initialize"
}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
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
    schedule("0 01 00 ? * *", calc)
}

def parse(String description) { noCommands("parse") }

def calc() {

    def now = new Date()
    def dow = now[Calendar.DAY_OF_WEEK]
    def doy = now[Calendar.DAY_OF_YEAR]
    logDebug("Day of Year is ${doy}")
    logDebug("Day of Week is ${dow}")
    sendEvent(name: "dayOfYear", value: doy)
    sendEvent(name: "dayOfWeek", value: dow)

    calcWoy(now, doy, dow)
}

def calcWoy(now, julian, dow) {

    def parsedDate = now.format("yyyy-MM-dd HH:mm:ss")
    logDebug("parsedDate is ${parsedDate}")    

    def lyI = (parsedDate[0..1].toInteger()) - 1
    def cI = parsedDate[0..3].toInteger()

    def yy = lyI as float
    def c = cI as float
    def doy = julian as float

    def f = (1.0 + ((13.0 * 11.0 - 1.0) / 5.0) + yy + (yy / 4.0) + (c / 4.0) - (2.0 * c))

    def dowJan1
    if (f < 0) {Math.round(dowJan1 = f % -7.0)}
    else {Math.round(dowJan1 = f + 7)}

    def weekNum = Math.round((doy + 6.0) / 7.0)
    if (dow < dowJan1) {weekNum++}

    logDebug("Week of Year is ${weekNum}")
    sendEvent(name: "weekOfYear", value: weekNum)
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}