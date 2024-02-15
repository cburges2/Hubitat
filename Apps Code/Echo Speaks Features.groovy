/**
 *  ****************  Echo Speask Features ****************
 *
 *  Usage:
 *  This was designed to add features to Echo Speaks App.
 *  
 *  Install with companion driver, echo Speaks Features Driver.  User Driver as app inputs in other apps, ect. 
 *  Includes methods for random sayings that are chosen using feature handler. 
 *  
 *  whichEcho will take a whichEcho parameter and text to speak.  When set, will send the spoken text to all echos listed in parameter
 *      front, bedroom, office ...  both is just front and bedroom.  all is all three. 
 *
 *  App also allows for scedulting of alarms and reminders
**/

definition (
    name: "Echo Speaks Features",
    namespace: "Hubitat",
    author: "Burgess",
    description: "",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Front Echo Device</b>") {

            input (
              name: "frontEcho", 
              type: "capability.speechSynthesis", 
              title: "Select Front Echo Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Bedroom Echo Device</b>") {

            input (
              name: "bedroomEcho", 
              type: "capability.speechSynthesis", 
              title: "Select Bedroom Echo Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Office Echo Device</b>") {

            input (
              name: "officeEcho", 
              type: "capability.speechSynthesis", 
              title: "Select Office Echo Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Echo Speaks Features Driver</b>") {

            input (
              name: "features", 
              type: "capability.actuator", 
              title: "Select Echo Speaks Features Driver", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("") {
            input (
                name: "echoMonkey", 
                type: "bool", 
                title: "Route Front Echo Speaks to Voice Monkey", 
                required: true, 
                defaultValue: false
            )
        }

        section("") {
            input (
                name: "monkeySecret", 
                type: "string", 
                title: "Monkey Secret Code", 
                required: false, 
            )
        }

        section("") {
            input (
                name: "monkeyAccess", 
                type: "string", 
                title: "Monkey Access Code", 
                required: false, 
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
    }
}

def installed() {

}

def updated() {

    if (settings?.debugMode) runIn(3600, logDebugOff)   // one hour
    initialize()

}
def initialize() {

    // features
    subscribe(features, "feature", featureHandler)
    subscribe(features, "whichEcho", speakTextHandler)
    subscribe(features, "cmdEcho", voiceCmdTextHandler)
    subscribe(features, "monkeyEcho", voiceMonkeyHandler)
    subscribe(features, "routineEcho", runRoutineIdHandler)

    // Alarms and Reminders Schedule
    schedule('0 0 19 * * ?', pillsSchedule)                 // 7pm every day
    schedule('0 30 06 * * ? * MON-FRI', getUpSchedule)      // 6:30am mon-fri
    schedule('0 0 18 * * ? * SUN,WED,FRI', jeevesSchedule)  // 6pm every Sun, Wed, Fri
}

// send to voice monkey
def voiceMonkeyHandler(evt) {
    logDebug("voiceMonkeyHandler ${evt.value}")

    if (evt.value != "idle") {
        def device = evt.value.toString()
        def text = features.currentValue("monkeyText").toString()
        def voice = features.currentValue("monkeyVoice").toString()
        logDebug("monkeyText is ${text}")

        setVoiceMonkey(device, text, voice)
    }
}

// Set Voice Monkey Send Devices
def setVoiceMonkey(device, text, voice) {
        if (device == "all" || device == "front" || device == "both" || device == "updown") sendVoiceMonkey("speak-front", text, voice)
        if (device == "all" || device == "bedroom" || device == "both") sendVoiceMonkey("speak-bedroom", text, voice)
        if (device == "all" || device == "office" || device == "updown" || device == "tree") sendVoiceMonkey("speak-office", text, voice)
        if (device == "tree") sendVoiceMonkey("pc-echo", text, voice)
}

def sendVoiceMonkey(sendDevice, text, voice) {

    String access = settings?.monkeyAccess 
    String secret = settings?.monkeySecret 
    String monkeyUri = "https://api-v2.voicemonkey.io/announcement?token="+access+"_"+secret+"&device="+sendDevice+"&text="+text+"&voice="+voice+""

    monkeyUri = monkeyUri.replace(" ","%20")
    logDebug("Monkey Uri is ${monkeyUri}")

    def getParams = [
		uri: monkeyUri,
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['CustomHeader':'CustomHeaderValue'],
	]
    asynchttpGet('monkeyCallbackMethod', getParams, [dataitem1: "datavalue1"])
}

def monkeyCallbackMethod(response, data) {
    def result = response.status.toInteger()
    logDebug("status of get call to monkey is: ${result}")
    if (result != 200) logDebug("Failed to send monkey: ${result}")
}

def pillsSchedule() {

    def randPills = ["Angela, I am reminding you to take your pills","Angela, did you take your pills today?","Angela, those pills are not going to take themselves","Angela, don't forget to take your pills","Angela, It is that time.  Take your pills","Angela, I hope you did not forget to take your pills","Angela, your pills. You need to take them","Angela, it is pill time again","Pill time angela!","Pills, pills, pills, pills. It is pill-time"]
    Collections.shuffle randPills // or with Groovy 3: nameList.shuffle()

    def speakText = randPills.first() 
    frontEcho.speak(speakText)
}

def getUpSchedule() {
    bedroomEcho.speak("Chris, it is 6:30AM")
}

def jeevesSchedule() {
    frontEcho.speak("Jeeves needs to be emptied")
}

// run the feature called by name
def featureHandler(evt) {
    logDebug("Feature is ${evt.value}")
    def feature = evt.value

    if (feature != "idle") {
        if (feature == "Current Mode") currentMode()
        else if (feature == "Greeting") greeting()
        else if (feature == "Wise Words") wiseWords()
        else if (feature == "Something About") tellMe()
        else if (feature == "Home Status") homeStatus()
        else if (feature == "Mailbox Status") mailStatus()
        else if (feature == "Goodbye") goodbye()
        else if (feature == "Good Nap") goodNap()
        else if (feature == "Goodnight") goodnight()
        else if (feature == "Pets") pets()
    }
}

// speak to several or specific echos with the same speak text
def speakTextHandler(evt) {
    logDebug("speakTextHandler called for ${evt.value}")
    def text = features.currentValue("speakText").toString()
    def device = evt.value.toString()
    
    if (settings?.echoMonkey && (evt.value == "front" || evt.value == "updown" || evt.value == "both")) {
        def voice = features.currentValue("monkeyVoice").toString()
        setVoiceMonkey("front", text, voice)        
        if (device == "updown" || device == "both") {
            if (device == "both") bedroomEcho.speak(text)
            if (device == "updown") officeEcho.speak(text)
        }
    } else if (evt.value != "idle") {           
        logDebug("speakText is ${text}")
        if (device == "front" || device == "all" || device == "both" || device == "updown") frontEcho.speak(text)
        if (device == "bedroom" || device == "all" || device == "both") bedroomEcho.speak(text)
        if (device == "office" || device == "all" || device == "updown") officeEcho.speak(text)
    }
}

// Send a text as voice command using a specific echo
def voiceCmdTextHandler(evt) {
    logDebug("voiceCmdTextHandler called for ${evt.value}")

    if (evt.value != "idle") {
        def device = evt.value
        def text = features.currentValue("cmdText")
        logDebug("cmdkText is ${text}")

        if (device == "front") frontEcho.voiceCmdAsText(text)
        else if (device == "bedroom") bedroomEcho.voiceCmdAsText(text)
        else if (device == "office") officeEcho.voiceCmdAsText(text)
    }
}

// run an alexa routine ID
def runRoutineIdHandler(evt) {
    logDebug("runRoutineIdHandler called for ${evt.value}")

    if (evt.value != "idle") {
        def device = evt.value.toString()
        def routine = features.currentValue("routineID").toString()

        if (device == "front") frontEcho.executeRoutineId(routine)
        if (device == "bedroom") bedroomEcho.executeRoutineId(routine)
        if (device == "office") officeEcho.executeRoutineId(routine)
    }
}

def currentMode() {

}

def greeting() {
    logDebug("Greeting Called")

    def randGreeting = ["Hello", "Welcome Back", "Nice to see you", "Welcome Home","Hey There","I'm glad you are back"]
    Collections.shuffle randGreeting // or with Groovy 3: nameList.shuffle()
    frontEcho.speak(randGreeting.first())
}

def wiseWords() {
    logDebug("Wise Words Called")

    def randThing = ["dream","day","run","walk","sleep","game","conversation","meal","cup of coffee","tv show","garden","drink","beer","drinkee","steak","laugh","lobster"]
    def randType = ["good","great","nice","fun","delightful","pleasurable","fantastic","extraoridary","terrific","amazing","cute","funny","cuddley","sweet","entertaining","nutty"]  
    def randResults = ["will bring you","is","will find you","will lead to","brings everyone","is always","is so","is very"]  
    def randFeeling = ["joy","pleasure","fun","delight","calm","contentment","happiness"]

    // Randomize lists
    Collections.shuffle randThing
    Collections.shuffle randResults 
    Collections.shuffle randFeeling
    Collections.shuffle randType 

    def speakText = "A ${randType.first()} ${randThing.first()} ${randResults.first()} ${randFeeling}"
    logDebug("speak text is ${speakText}")
    bedroomEcho.speak(speakText)

}

def tellMe() {
    logDebug("Something About Called")

    def randAbout = ["something about dogs","a joke","the moon phase tonight", "the weather tomorrow"]
    Collections.shuffle randAbout 

    def command = "Tell me ${randAbout.first()}"

    bedroomEcho.voiceCmdAsText(command)

}

def pets () {
    logDebug("Pets Called")

    def randPet = ["Nutmeg","Sisko"]
    def randPetFeeling = ["joy","pleasure","fun","delight","calm","contentment","happiness"]
    def randResults = ["will bring you","is","will find you","will lead to","brings everyone","is always","is so","is very"]

    // Randomize lists
    Collections.shuffle randPet 
    Collections.shuffle randResults 
    Collections.shuffle randPetFeeling 

    def speakText = "${randPet.first()} ${randResults.first()} ${randPetFeeling.first()}"
    logDebug("speak text is ${speakText}")
    bedroomEcho.speak(speakText)    
}

def homeStatus() {
  // needs data drivers for data
}

def mailStatus () {
 // needs front data or mailbox switch
}

def goodbye() {
    logDebug("GoodBye Called")

    def randGoodbye = ["Goodbye", "See you later", "Have a good one", "See you Soon","Later alligator"]
    Collections.shuffle randGoodbye 
    frontEcho.speak(randGoodbye.first())
}

def goodNap() {

    def randType = ["a good","a sweet","a wonderful","a great","a super","a nice","a splended","an amazing","a delightful","an enjoyable","a marvelous","a fantastic","a lovely","an awesome","a sensational","a spectacular","a sound","a deep"]
    Collections.shuffle randType 

    def goodNap = "Have ${randType.first()} nap"
    bedroomEcho.speak(goodNap)
}

def goodnight() {

    def randType = ["a good","a sweet","a wonderful","a great","a super","a nice","a splended","an amazing","a delightful","an enjoyable","a marvelous","a fantastic","a lovely","an awesome","a sensational","a spectacular","a sound","a deep"]
    Collections.shuffle randType 

    def goodnight = "Have ${randType.first()} sleep"
    bedroomEcho.speak(goodnight)

}

def logDebug(txt){
    try {
        if (settings?.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logDebugOff() {
    logDebug("Turning off debugMode")
    app.updateSetting("debugMode",[value:"false",type:"bool"])
}