/**
 *  ****************  Illuminance Calculations and Sync  ****************
 *
 *  Usage:
 *  This was designed to update a virtual illuminance attribute driver attributes based on illuminance
 *  Updates sunset, lowLight, dayLight bools as illuminance values change
 *    
 * v.2.0 - 10/23 - get states directly from defice instead of saving as state variable
 * v.2.1 - 10/24 - added season
**/
import groovy.time.*

definition (
    name: "Illuminance Calculations And Sync",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Calculate Illuminance Data and Set Illuminance Values in Illuminance Data Device",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Illuminance Data Device</b>") {

            input (
              name: "illuminanceData", 
              type: "capability.actuator",  
              title: "Select Illuminance Data Device", 
              required: true, 
              multiple: false,     
              submitOnChange: true
            )
        }
   
        section("<b>Weather Station</b>") {
            input (
                name: "weatherStation", 
                type: "capability.illuminanceMeasurement",
                title: "Select Weather Station Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )       
        }      
   
        section("<b>Inside Illuminance Sensor Device</b>") {
            input (
                name: "indoorSensor", 
                type: "capability.illuminanceMeasurement",
                title: "Select Inside Illuminance Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )      
        }        

        section("<b>Virtual Auto Light Target Dimmer Device</b>") {
            input (
                name: "lightTarget", 
                type: "capability.switchLevel",
                title: "Select Virtual Light Target Dimmer Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )          
        }   

        section("<b>Moon Phase Device</b>") {
            input (
                name: "moonPhase", 
                type: "capability.actuator",
                title: "Select Moon Phase Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )   
        }

        section("<b>Log To Google Device</b>") {
            input (
                name: "googleLogs", 
                type: "capability.actuator",
                title: "Select Log to Google Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
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
                options: [1:"Info", 2:"Warning", 3:"Debug"],
                multiple: false,
                defaultValue: 2,
                required: true
            )
        }        
    }
}

def installed() {

    state.luxArray = [0,0,0,0,0]
    state.cloudinessArray = ["","","","",""]
    state.maxIlluminance = 0.0
    state.hourlyAdd = 0.0
    state.addsInHour = 0.0
    state.minInHour = 0.0
    state.maxInHour = 0.0
    state.maxInDay = 0.0
    state.variance = 0.0   
    initialize()
}

def updated() {

    //state.cloudinessArray = ["","","","",""]
    /*state.luxArray = [0,0,0,0,0]

    state.maxIlluminance = 0.0
    state.hourlyAdd = 0.0
    state.addsInHour = 0.0
    state.minInHour = 0.0
    state.maxInHour = 0.0
    state.maxInDay = 0.0
    state.variance = 0.0   */

    /*unsubscribe(illuminanceController)
    unsubscribe(cloudCeilingController)
    unsubscribe(insideSensorController)
    unsubscribe(lightTargetController)*/

    if (settings?.debugMode && settings?.logLevel == "3") {
        runIn(3600, logDebugOff)   // one hour
        logDebug("Log Level will change from Debug to Info after 1 hour")
    }
}

def initialize() {

    state.moonPhase = "none"
    subscribe(weatherStation, "illuminance", illuminanceController)
    subscribe(weatherStation, "temperature", cloudCeilingController)
    subscribe(indoorSensor, "illuminance", insideSensorController)
    subscribe(lightTarget, "level", lightTargetController)
    subscribe(moonPhase, "moonPhase", setMoonPhaseController)

    schedule('0 07 22 * * ?', setMaxIlluminance)
    schedule('0 00 05 * * ?', updateNoonIlluminance)

    schedule('0 0 0 21 MAR ?', setSpring,)
    schedule('0 0 0 21 JUN ?', setSummer)
    schedule('0 0 0 21 SEP ?', setFall)
    schedule('0 0 0 21 DEC ?', setWinter)

}

def setSpring() {illuminanceData.setSeason("spring")}
def setSummer() {illuminanceData.setSeason("summer")}
def setFall() {illuminanceData.setSeason("fall")}
def setWinter() {illuminanceData.setSeason("winter")}

def illuminanceController(evt) {
    logDebug("Illuminance Sensor Event = ${evt.value}",1)

    def lux = evt.value.toInteger()
    illuminanceData.setSensorIlluminance(lux)

    // set max illuminance
    if (lux > illuminanceData.currentValue("maxIlluminance")) {illuminanceData.setMaxIlluminance(lux)}

    // add to lux list
    state?.luxArray.push(lux)

    if (state?.luxArray.size() > 5)
        state.luxArray.removeAt(0)

    logDebug(state?.luxArray,3)

    calcVariance()
    cloudsFromIlluminance(lux)
    calcWeatherStats(lux)
    calcLightIntensity()
}

def cloudCeilingController(evt) {
    logDebug("Cloud Ceiling Controller Called ${evt.value}",3)

    double temp = weatherStation.currentValue("temperature")
    double dewPoint = weatherStation.currentValue("dewPoint")
    logDebug("Temp = ${temp} and dewPoint = ${dewPoint}",3)

    double ceiling = (temp-dewPoint)/2.5 * 1000
    logDebug("ceiling = ${ceiling}",3)
    ceiling = Math.round(ceiling)
    logDebug("Cloud Ceiling is ${ceiling}",1)

    def clouds = "Not Set"
    if (ceiling > 23000 && ceiling <= 45000) clouds = "High Clouds"
    else if (ceiling > 6500 && ceiling <= 23000) clouds = "Mid-Level Clouds"
    else if (ceiling >= 0 && ceiling <= 6500) clouds = "Low Clouds"
    else if (ceiling > 45000) {
        ceiling = 0
        clouds = "No Clouds"
    }
    weatherStation.setCloudCeiling(ceiling)
    state.cloudCeiling = ceiling
}

def calcVariance() {
  
    int size   
    double varience
    double mean

    size = state?.luxArray.size();
    double sum = 0.0;
    for(double a : state?.luxArray)
        sum += a;
    mean = sum/size;

    double lux = 0;
    for(double a :state?.luxArray)
        lux += (a-mean)*(a-mean)
    variance = lux/(size-1)
    variance = variance / 10000
    variance = variance.round(2)

    logDebug("Variance is ${variance}",1)
    state.variance = variance
    logDebug("Adding illuminaceVariance to Illumiance Data: ${variance}",3)
    illuminanceData.setIlluminanceVariance(variance)
}   

def cloudsFromIlluminance(lux) {

    double illuminance = lux
    double variance = state?.variance
    
    // get sunrise and sunset, now
    def riseAndSet = getSunriseAndSunset()
    def sunRise = riseAndSet.sunrise
    def sunSet = riseAndSet.sunset
    def now = new Date()

    double sunMinute
    def afternoon = false
    double morningEvening
    double noon
    double sunValue
    double partlyCloudyVariance
    double cloudyVariance

    // get Minutes since Sunrise
    def sunriseStart = sunRise //new Date(sunRise)
    use(TimeCategory)
    {
        def timeSinceSunrise = now - sunriseStart
        state.sunriseDuration = timeSinceSunrise
    }
    def minSinceSunrise = (state?.sunriseDuration.getHours() * 60) + state?.sunriseDuration.getMinutes()
    logDebug("Minutes Since Sunrise is ${minSinceSunrise}",3)

    // get Minutes until Sunset
    def sunsetEnd = sunSet //new Date(sunSet)
    use(TimeCategory)
    {
        def timeToSunset = sunsetEnd - now
        state.sunsetDuration = timeToSunset
    }
    def minToSunset = (state?.sunsetDuration.getHours() * 60) + state?.sunsetDuration.getMinutes()
    logDebug("Minutes To Sunset is ${minToSunset}",3)


    // Determine if After or Before Noon
    if (minToSunset < minSinceSunrise) {
        sunMinute = minToSunset
        afternoon = true
    } else {
        sunMinute = minSinceSunrise
        afternoon = false
    }
    logDebug("sunMinute set to ${sunMinute}",3)

    // Calculate sunValue using formulas for the day segments
    double noonIlluminance = illuminanceData.currentValue("noonIlluminance") - 9000 // used for noon calcs for Max

    // set the formula used for sunValue based on time of day
    morningEvening = Math.pow(sunMinute,2) + (sunMinute * 10) //morningEvening = (257.5 * (sunMinute - 108) + 10680)   
    noon = (Math.pow(sunMinute,2) * 0.05) + (sunMinute * 15) + (noonIlluminance - (noonIlluminance * 0.1))

    // Determine which sunValue used for day segment
    if (morningEvening < noon) {
        sunValue = morningEvening
        logDebug("sunValue used is Morning/Evening",3)
    } else if (morningEvening > noon) {
        sunValue = noon
        logDebug("sunValue used is Noon",3)    
    }
    logDebug("sunValue is ${sunValue}",3)

    // cloudySunRatio -- limit for the ratio between sunMinute and actual lux for cloudy determination
    double cloudySunRatio = ((sunMinute / sunValue) * 100) + (sunMinute * 0.005)
    if (sunMinute < 120) cloudySunRatio = cloudySunRatio - (0.01 * sunMinute)  // reduce in ealy morning/evening

    // variance get larger as the day gets brighter, and smaller as the day gets dimmer
    // Use min since sunrise and min to sunset and divide to reduce to a partly cloudy condition .
    // cloudy variance marks an illumination variance that is too high to be all cloudy. 
    double partCloudyDiv = 0.5  // partly cloudy diviser for minute to get an expected variance        
    double cloudyDiv = 3        // cloudy variance diviser for minute
    double newPartlyCloudyVariance
    double newCloudyVariance
    if (minSinceSunrise < minToSunset) {
        newPartlyCloudyVariance = minSinceSunrise / partCloudyDiv
        newCloudyVariance = minSinceSunrise / cloudyDiv
    } else {
        newPartlyCloudyVariance = minToSunset / partCloudyDiv
        newCloudyVariance = minToSunset / cloudyDiv
    }      

    // Set cloudySunRatio and rounded variance variables
    partlyCloudyVariance = newPartlyCloudyVariance.round(2)
    cloudyVariance = newCloudyVariance.round(2) 
    logDebug("cloudySunRatio set to ${cloudySunRatio}",3)  
    logDebug("partlyCloudyVariance set to ${partlyCloudyVariance}",3)  
    logDebug("cloudyVariance set to ${cloudyVariance}",3)   
    logDebug("Adding cloudySunRatio to Illumiance Data: ${cloudySunRatio}",3)
    illuminanceData.setCloudySunRatio(cloudySunRatio) 

    // set Polarity
    def polarity = "positive"
    if (illuminance < sunValue) polarity = "negative"
    logDebug("polarity is ${polarity}",3)

    // set sunRatio
    logDebug("Adding sunValue to Illumiance Data: ${sunValue}",3)
    illuminanceData.setSunValue(sunValue)
    def sunRatio = sunValue / illuminance
    logDebug("sunRatio is ${sunRatio}",3)
    
    // Determine if it is cloudy (not in sunny range)
    def cloudy = false 
    if ((sunRatio >= cloudySunRatio) && polarity == "negative") {cloudy = true}

    // Determine how cloudy/sunny it is
    def cloudiness = "Not Set"
    if (variance > partlyCloudyVariance) {
        if (cloudy == true) cloudiness = "Partly Cloudy"
        else cloudiness = "Partly Sunny"
    } else if (variance <= partlyCloudyVariance && variance >= cloudyVariance) {   //   
        if (cloudy == true) cloudiness = "Mostly Cloudy"
        else cloudiness = "Mostly Sunny"
    } else if (variance <= cloudyVariance) {
        if (cloudy == true) cloudiness = "Cloudy"
            else cloudiness = "Sunny"
    }

    if (cloudiness != "Not Set") {
        logDebug("Adding cloudiness as cloudiness to Illumiance Data: ${cloudiness}",3)
        illuminanceData.setCloudiness(cloudiness)       
        weatherStation.setCloudConditions(cloudiness)
    } else {logDebug("${app.label} - cloudiness was not set!",2)}  

    // update to most common last 5
    cloudiness = getMostCommonCloudiness(cloudiness)      
    
    // set cloudiness to sunrise and sunset at edges of day
    if (minSinceSunrise > 0 && minSinceSunrise < 15) {
        cloudiness = "Sunrise"
    }
    if (minToSunset > 0 && minToSunset < 15) {
        cloudiness = "Sunset"
    }

    // insert rain/snow if raining or snowing
    def precip = weatherStation.currentValue("rainRate") > 0
    if (precip) {
        cloudiness = getRainRate()
        logDebug("cloudiness updated to precip ${cloudiness}")
    } else if ((minToSunset < 0 || minSinceSunrise < 0)) {       
        cloudiness = moonPhase.currentValue("moonPhase")
        iconFile =  moonPhase.currentValue("moonPhaseImage")   // update to moon Phase if night and not raining/snowing       
    }    

    // get the icon file name from cloudiness
    def iconFile = getIcon(cloudiness)

    logDebug("Adding cloudiness as currentCondtions to Weather Station: ${cloudiness}",3)
    weatherStation.setCurrentConditions(cloudiness)
    logDebug("Adding icon filename as currentIcon to Weather Station: ${iconFile}",3)
    weatherStation.setCurrentIcon(iconFile)
    logDebug("Cloudiness is ${cloudiness}",1)

    // Log to Google
    def logParams = "Lux="+lux+"&Clouds="+state?.cloudCeiling+"&Variance="+state?.variance+"&Sun="+sunValue+"&Cloudy="+cloudiness+"&Sun Ratio="+sunRatio+"&Cloudy Sun Ratio="+cloudySunRatio
    googleLogs.sendLog("illuminance", logParams)
}

String getRainRate() {
    double rainRate = weatherStation.currentValue("rainRate")
    def rainSnow = "Rain"
    def currentText = ""

    if (weatherStation.currentValue("temperature").toInteger() < 34) {
        rainSnow = "Snow"
    } 
    if (rainRate >= 0.001 && rainRate <= 0.098) currentText = "Light "+rainSnow
    else if (rainRate >= 0.099 && rainRate <= 0.3) currentText = "Moderate "+rainSnow
    else if (rainRate >= 0.31 && rainRate <= 2.0) currentText = "Heavy "+rainSnow
    else if (rainRate > 2.0) currentText = "Violent "+rainSnow
    logDebug("Rain Rate is ${currentText}")

    return currentText
}

String getIcon(cloudiness) {

    def icon = cloudiness.toLowerCase()
    icon = icon.replace(" ","-")
    def iconFile = icon + ".svg"      
    return iconFile
}

String getMostCommonCloudiness(String cloudiness) {

    // add to lux list
    state?.cloudinessArray.push(cloudiness)

    if (state?.cloudinessArray.size() > 5)
        state.cloudinessArray.removeAt(0)

    logDebug("Cloudiness Array: ${state?.cloudinessArray}",2)    
    def sentIcon = iconFile
    // Use most common of last five as cloudiness, unless sunset, sunrise, or night
    if (cloudiness == "Sunset" || cloudiness == "Sunrise" || cloudiness == "Sunny" || cloudiness == "Partly Sunny" || cloudiness == "Mostly Sunny" || cloudiness == "Mostly Cloudy" || cloudiness == "Partly Cloudy" || cloudiness == "Cloudy"){
        def sunny = 0
        def partlySunny = 0
        def mostlySunny = 0
        def mostlyCloudy = 0
        def partlyCloudy = 0
        def cloudy = 0       
        for (x=0; x<5; x++) {
            if (state?.cloudinessArray[x] == "Sunny") sunny++           
            if (state?.cloudinessArray[x] == "Partly Sunny") partlySunny++           
            if (state?.cloudinessArray[x] == "Mostly Sunny") mostlySunny++
            if (state?.cloudinessArray[x] == "Mostly Cloudy") mostlyCloudy++     
            if (state?.cloudinessArray[x] == "Partly Cloudy") partlyCloudy++        
            if (state?.cloudinessArray[x] == "Cloudy") cloudy++         
        }
        def mostCommon = "Sunny"
        def greatest = sunny
        if (partlySunny > greatest) mostCommon = "Partly Sunny"; greatest == partlySunny
        if (mostlySunny > greatest) mostCommon = "Mostly Sunny"; greatest == mostlySunny
        if (mostlyCloudy > greatest) mostCommon = "Mostly Cloudy"; greatest == mostlyCloudy
        if (partlyCloudy > greatest) mostCommon = "Partly Cloudy"; greatest == partlyCloudy
        if (cloudy > greatest) mostCommon = "Cloudy"
       
        logDebug("Most Common is ${mostCommon}",3)
        cloudiness = mostCommon   
    }

    return cloudiness        
}

def setMoonPhaseController(evt) {
    logDebug("Moon Phase Data ${evt.value}",3)

    def newPhase = evt.value()
    def current = state?.moonPhase
    if (current != newPhase) {
        weatherData.setMoonPhase(newPhase)
        logDebug("Moon Phase updated to ${newPHase}",1)
    }
}

def calcLightIntensity(lux) {
    logDebug("Calculating Light Intensity",3)

    def solarRadiation = weatherStation.currentValue("solarRadiation")
    def noonIlluminance = illuminanceData.currentValue("noonIlluminance")
    def noonRadiation = (noonIlluminance/100)
    def intensity = (solarRadiation / noonRadiation) * 95
    intensity = Math.round(intensity)

    logDebug("Sending Light Intensity to Illumination Data: ${intensity}",3)
    illuminanceData.setLightIntensity(intensity)
    logDebug("Light Intensity is ${intensity}",1)
}

// Calc other illuminance Data
def calcWeatherStats(lux) {
    logDebug("Calculating Stats with Lux",3)

    state.maxInHour = lux
    if (lux > state?.maxInDay) state.maxInDay = lux 
    if (illuminance < state?.minInHour) state.minInHour = lux
}

// set Max Illumance - scedule at end of day
def setMaxIlluminance() {
    logDebug("Calculating MaxIlluminance for Day",3)
    if (state?.maxInDay > state?.maxIlluminance) {
        logDebug("Updating Max Illuminance in Illuminance Data: ${state?.maxIlluminance}",3)
    }
}

def updateNoonIlluminance() {
    app.updateSetting("logLevel",[value:"3",type:"enum"])
    def noonIlluminance = illuminanceData.currentValue("noonIlluminance").toInteger()
    logDebug("Current Noon Illuminance is ${noonIlluminance}",1)

    // get the year
    def now = new Date()
    String year = now.format("yyyy")

    // create date strings
    String summerSolstice = "${year}-06-20 00:00:00"
    String winterSolstice = "${year}-12-21 00:00:00"
    String yearStart = "${year}-01-1 00:00:01"
    String yearEnd = "${year}-12-31 23:59:59"    

    // convert strings to dates
    def summer = Date.parse("yyyy-MM-dd hh:mm:ss", summerSolstice)
    def winter = Date.parse("yyyy-MM-dd hh:mm:ss", winterSolstice)
    def start = Date.parse("yyyy-MM-dd hh:mm:ss", yearStart)
    def end = Date.parse("yyyy-MM-dd hh:mm:ss", yearEnd)

    def solsticeState = ""

    // determine if before or after summer solstice
    if (timeOfDayIsBetween(start, summer, now)) solsticeState = "before"
    if (timeOfDayIsBetween(summer, winter, now)) solsticeState = "after"
    if (timeOfDayIsBetween(winter, end, now)) solsticeState = "before"

    logDebug("solsticeState is ${solsticeState} solstice",1)

    // add or substrct to noon illuminance based on summer solstice
    def luxPerDay = illuminanceData.currentValue("luxChangePerDay").toInteger()
    logDebug("Lux per Day is ${luxPerDay}")
    if (solsticeState == "after") {
        noonIlluminance = noonIlluminance - luxPerDay
    }

    if (solsticeState == "before") {
        noonIlluminance = noonIlluminance + luxPerDay
    }

    illuminanceData.setNoonIlluminance(noonIlluminance)
    logDebug("Updated Noon Illuminance is ${noonIlluminance}", 1)
}

// sync indoor illuminance to illuminance data
def insideSensorController(evt) {
    state.insideSensor = evt.value.toInteger()
    logDebug("Inside Sensor Event = ${state?.insideSensor}",3)

    illuminanceData.setIndoorIlluminance(evt.value)
}

// sync light target changes to illuminance data
def lightTargetController(evt) {
    state.lightTarget = evt.value.toInteger()
    logDebug("Light Target Event = ${state?.lightTarget}",3)

    illuminanceData.setLightTarget(evt.value)
}

// log debug if no logLevel added
def logDebug(txt) {
    try {
        if (settings?.debugMode) {
            log.debug("${app.label} - ${txt}")   // debug
        }
    } catch(ex) {
        log.error("bad debug message")
    }    
}

// log by level when lvl supplied
def logDebug(txt, lvl){
    try {
        logLevel = settings?.logLevel.toInteger()
        if (settings?.debugMode) {
            if (lvl == 3 && logLevel == 3) log.debug("${app.label} - ${txt}")       // debug
            else if (lvl >= 2 && logLevel >= 2) log.warn("${app.label} - ${txt}")   // warn
            else if (lvl >= 1 && logLevel >= 1) log.info("${app.label} - ${txt}")   // info
        }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logDebugOff() {
    logDebug("Turning off debugMode")
    app.updateSetting("logLevel",[value:"1",type:"enum"])
}