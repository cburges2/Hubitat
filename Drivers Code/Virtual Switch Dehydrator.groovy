import java.text.SimpleDateFormat 

metadata {
    definition (name: "Virtual Switch Dehydrator", namespace: "hubitat", author: "cburges") {

        capability "Switch"		//"on", "off"

        attribute "offTime", "String"
        attribute "autoOff", "ENUM"
        attribute "display", "String"

        command "setAutoOff", [[name:"motion",type:"ENUM", description:"Set Sanitization", constraints:["1","2","3","4","5","6","7","8","9","10","11","12","16","20","24"]]]
 	}   
    
    preferences {       
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "forceUpdate", type: "bool", title: "Force State Update", description: "Send event everytime, regardless of current status. ie Send/Do On even if already On.",  defaultValue: false
        input name: "autoOff", type: "enum", description: "Automatically turns off the device after selected time.", title: "Enable Auto-Off",
         options: [[0:"Disabled"],[1:"1hr"],[2:"2hr"],[3:"3hr"],[4:"4hr"],[5:"5hr"],[6:"6hr"],[7:"7hr"],[8:"8hr"],[9:"9hr"],[10:"10hr"],[11:"11hr"],[12:"12hr"],[16:"16hr"],[20:"20hr"],[24:"24hr"]], defaultValue: 0  
    } 
}

def updated(){  
    runIn(1,updateAutoOff) 
}

def updateAutoOff() {
    def offHours = settings?.autoOff+"hr"
    sendEvent(name: "autoOff", value: offHours, stateChange: true)
    runIn(1,setDisplay)
}

def off() {
    sendEvent(name: "switch", value: "off", isStateChange: forceUpdate)
    unschedule("off")
    logTxt "turned Off"
    sendEvent(name: "offTime", value: "Done", stateChange: true)
    runIn(1,setDisplay)
}

def on() {
    sendEvent(name: "switch", value: "on", isStateChange: forceUpdate)
    logTxt "turned On"
    if (autoOff.toInteger()>0){      
        def hours = autoOff.toInteger() * 3600 
        runIn(hours, off)
        def offTime = getFutureTime(autoOff.toInteger() * 60)
        sendEvent(name: "offTime", value: offTime, stateChange: true)
    }
    runIn(1,setDisplay)
}

// Get future time from interval in mintues
String getFutureTime(int interval) {
    Date date = new Date();   // current date
    Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
    calendar.setTime(date);   // assigns calendar to current date/time 
    calendar.add(Calendar.MINUTE, interval)
	Date newDate = calendar.getTime()
	def newTime = new SimpleDateFormat("hh:mm a").format(newDate)
    newTime = newTime
    return newTime
}

def setAutoOff(set) {
    def withHour = set+"hr"
    sendEvent(name: "autoOff", value: withHour, stateChange: true)
    device.updateSetting("autoOff",[value: set, type:"enum"])
    runIn(1,setDisplay)
}

def setDisplay() {
    String display = "Auto Off: "+device.currentValue("autoOff")+"<br>Off Time: "+device.currentValue("offTime")
    sendEvent(name: "display", value: display, stateChange: true)
}

def installed() {
}

void logTxt(String msg) {
	if (logEnable) log.info "${device.displayName} ${msg}"
}
 
