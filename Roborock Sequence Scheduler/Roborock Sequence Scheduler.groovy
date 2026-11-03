/**
 *  **************** Roborock Sequence Scheduler  ****************
 *
 * Usage: 
* Schedule multiple Roborock Vacuums to perform room sequence cleanings.  
* Choose the rooms cleaned and the room cleaning order for the sequence, as well as mop settings for each room.
* Each robot can run their sequences concurrently, if desired, and the app will keep track of the sequence states independently. 
* Optional: a blocking device can be chosen to stop sequences from starting until the blocking device attribute clears
* Optional: multiple blocking devices can be added to stop individual rooms from starting during the sequence, until the room blocking device attribute clears. Option for = and != value. 
* Optional: speaker device for each room to announce a room cleaning is about to start
* Optional: use a chosen speaker device to annouce robot and dock errors when they happen.
* Optional: auto empty for self-emptying vacs: Enter a Sq foot cleaned limit for each vac since last empty, to cause the vac to empty between rooms if it goes over limit. 
* Optional: signal light using an RGB bulb device for status.  Green=Cleaning, Yellow=Blocked/waiting, Blue=Water Erros, Red=Other Robot Error, also includes brightness and hue settings 
* Optional: create a child pushable button device to create html attributes and room clean buttons for dashboards, with Sequence Schedules, room clean button codes, and status.
*           Child device also contains a button attribute, to pull up the app settings page in an iframe, to modify settings from a dashboard attribute tile or the device page. 
* 
* 
Version History:
 *  v. 1.0a- 04/17/26 - Initial Code Structure, UI settings page - No AI used
 *  v. 1.0 - 08/01/26 - Gemini AI code refactor and finish
 DeepSeek AI Version History:
 *  v. 1.1 - 08/14/26 - Stability fixes (stable room IDs, pause flag, concurrent sequence guard, dynamic buttons)
 *  v. 1.2 - 08/15/26 - Fixed runIn targetStep unwrapping bug, added debug logging
 *  v. 1.3 - 08/28/26 - Fixed delay-resume step tracking, added duplicate-room detection
 *  v. 1.4 - 09/05/26 - Per-room delay devices (global pool, per-room subset)
 *  v. 1.5 - 09/12/26 - Per-room attribute + trigger value; general settings reduced
 *  v. 1.6 - 09/18/26 - Simplified delay: one device per room; global delay list removed
 *  v. 1.7 - 09/25/26 - Per-room speakers (sequences + instant clean); announce room on start only
 *  v. 1.8 - 10/02/26 - Vacuum error alerts: subscribe to 'error' and 'dockError', announce via global speaker
 *  v. 1.9 - 10/10/26 - UI Overhaul: visual grouping, 12-column alignment
 *  v. 2.0 - 10/15/26 - Fixed cron scheduling overwrite bug (unique handlers), UI alignment fix
 *  v. 2.1 - 10/20/26 - Added Room Order feature (dropdown sorting)
 *  v. 2.2 - 10/23/26 - Added Sequence-level start delay feature
 *  v. 2.3 - 10/26/26 - Implemented Auto-Empty (SqFt Limit) feature
 *  v. 2.4 - 10/28/26 - Added timeout for block devices; Button 0 aborts waiting sequences
 *  v. 2.5 - 10/30/26 - Fixed speech command handling, added safe fallbacks for room names
 *  v. 2.6 - 10/30/26 - Conditionally hide SqFt Limit if Auto-Empty is false; updated label
 *  v. 2.7 - 10/30/26 - Split sequenceList into separate attributes per vacuum to avoid string length limits
 *  v. 2.8 - 10/31/26 - Added vacuum name to sequence lists; removed mop and speaker settings from lists
 *  v. 2.9 - 10/31/26 - Overhauled engine to support concurrent sequences running on multiple vacuums at once
 *  v. 2.10 - 10/31/26 - Fixed null pointer exception in delay device and vacuum state handlers
 *  v. 2.11 - 10/31/26 - Include vacuum name in room cleaning speech announcements
 *  v. 2.12 - 10/31/26 - Use sequence name instead of number in schedulerStatus messages
 *  v. 2.13 - 10/31/26 - Added detailed debug logging for block states; updated tile status for block waits
 *  v. 2.14 - 10/31/26 - Added exhaustive skip-condition logging in vacuumStateHandler to diagnose stuck sequences
 *  v. 2.15 - 10/31/26 - Fixed state serialization bug (String vs Integer keys) causing sequences to fail to resume
 *  v. 2.16 - 10/31/26 - Added sequence enable/disable switches; removed unnecessary debug log in delay handler
 *  v. 2.17 - 10/31/26 - Removed hard-coded 10 sequence limit; use overwrite: false and data map
 *  v. 2.18 - 10/31/26 - Added multiple blocking devices per room, with Add/Remove buttons and = / != operators
 *  v. 2.19 - 10/31/26 - Adjusted layout of room blocking UI fields and updated labels
 *  v. 2.20 - 10/31/26 - Matched room header font size to sequence header; kept bold
 *  v. 2.21 - 10/31/26 - Added iframe Settings button to child driver; app triggers frame refresh on update
 *  v. 2.22 - 10/31/26 - Styled iframe Settings button with outline, grey background, and 3D effect
 *  v. 2.23 - 10/31/26 - Minified iframe Settings button HTML/CSS to prevent exceeding 1024 char dashboard limit
 *  v. 2.24 - 10/31/26 - Reset SqFt counter when vacuum enters 'emptying' state (manual empty detection)
 *  v. 2.25 - 10/31/26 - Calculate and send bin percent full to binPercent1...10 attributes on display tile
 *  v. 2.30 - 11/01/26 - Added per-vac color light for running state and error alerts
 *  v. 2.31 - 11/01/26 - Fixed Map<String,def> generic syntax error in updateVisualLights()
 *  v. 2.32 - 11/01/26 - Added vac block separators/headers in UI; clarified light is optional
 *  v. 2.33 - 11/02/26 - Added light dim level setting; Added yellow color for delay waiting states
 *  v. 2.34 - 11/02/26 - Added custom hue inputs for each color signal state
 *  v. 2.35 - 11/03/26 - Split block timeouts: 30min default for room blocks, 12hr default for seq blocks
 *
 **/

import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

definition (
    name: "Roborock Sequence Scheduler AI",
    namespace: "Hubitat",
    author: "Chris Burgess",
    description: "Multi-Vac Room Scheduler",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page(name: "mainPage")
}

// ---------------------------------------------------------------------------
// Logging & Helpers
// ---------------------------------------------------------------------------

void logDebug(String msg) {
    if (settings?.logEnable) {
        log.info "RR-Scheduler: ${msg}"
    }
}

String getSeqName(int seqIdx) {
    def val = settings["seqName_${seqIdx}"]?.toString()?.trim()
    if (val == null || val == "") return "Sequence ${seqIdx}"
    return val
}

String getSeqBlockDeviceName(int seqIdx) {
    def dev = firstOrNull(settings["seq_${seqIdx}_seqDelayDevice"])
    return dev ? dev.displayName : "Unknown Device"
}

List normalizeToList(val) {
    if (val == null) return []
    if (val instanceof List) return val
    return [val]
}

def firstOrNull(val) {
    if (val == null) return null
    if (val instanceof List) return val ? val[0] : null
    return val
}

Date parseTimeInput(timeInput) {
    if (timeInput == null) return null
    if (timeInput instanceof Date) return timeInput

    String s = timeInput.toString()
    def formats = [
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "HH:mm"
    ]
    for (String fmt in formats) {
        try { return new SimpleDateFormat(fmt).parse(s) } catch (Exception ignored) { }
    }

    def matcher = (s =~ /(\d{1,2}):(\d{2})/)
    if (matcher.find()) {
        Calendar cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, matcher.group(1).toInteger())
        cal.set(Calendar.MINUTE, matcher.group(2).toInteger())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.getTime()
    }
    return null
}

boolean isClearErrorValue(String val) {
    if (val == null) return true
    String v = val.trim().toLowerCase()
    if (v == "") return true
    if (v in ["0", "none", "no error", "no_error", "clear", "cleared", "ok", "normal", "false"]) return true
    return false
}

// ---------------------------------------------------------------------------
// UI Sections
// ---------------------------------------------------------------------------

def mainPage() {
    def numVacs = (settings["numVacs"] ?: 0) as int
    app.updateSetting("numVacs", [type: "number", value: numVacs])

    dynamicPage(
        name: "mainPage",
        title: "Setup Panel",
        install: true,
        uninstall: true
    ) {
        section("App Name") {
            label title: "Assign Name", required: false
        }

        section("Diagnostics") {
            input (name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true, submitOnChange: true)
        }

        buildVacuumSetupSection(numVacs)
        buildGlobalMatrixEngine(numVacs)
        buildSpeechSetupSection()
        buildErrorAlertSection()
        buildDelayEngineSection()
        buildSequenceConfigSection()
        buildInstantMopOverrideSection()
        buildDashboardTileSection()
    }
}

void buildVacuumSetupSection(int numVacs) {
    section("Choose Vacuum Devices") {
        if (numVacs <= 0) { return }
        for (int i = 1; i <= numVacs; i++) {
            if (i > 1) {
                paragraph "<hr style='border: 1px solid #ccc; margin-top: 20px; margin-bottom: 10px;'/>"
            }
            paragraph "<div style='background-color:#e8f0fe; color:#1a73e8; padding:8px; border-radius:4px; font-size:16px;'><b>Vacuum #${i}</b></div>"
            
            input (name: "vac${i}", type: "capability.actuator", title: "Select Vacuum Device", required: false, submitOnChange: true)
            input (name: "vac${i}AutoEmpty", type: "bool", title: "Has Auto-Empty", defaultValue: true, submitOnChange: true)
            
            if (settings["vac${i}AutoEmpty"] != false) {
                input (name: "vac${i}BinSqrFeet", type: "number", title: "SqFt Cleaned to cause Auto-Empty", defaultValue: 50, submitOnChange: true)
            }
            
            input (name: "vac${i}Light", type: "capability.colorControl", title: "Visual State Light (optional)", required: false, submitOnChange: true, width: 8)
            input (name: "vac${i}LightLevel", type: "number", title: "Level (1-100)", defaultValue: 100, range: "1..100", required: false, submitOnChange: true, width: 4)
            
            input (name: "vac${i}LightHueGreen", type: "number", title: "Green Hue", defaultValue: 33, range: "0..100", required: false, submitOnChange: true, width: 3)
            input (name: "vac${i}LightHueYellow", type: "number", title: "Yellow Hue", defaultValue: 16, range: "0..100", required: false, submitOnChange: true, width: 3)
            input (name: "vac${i}LightHueRed", type: "number", title: "Red Hue", defaultValue: 0, range: "0..100", required: false, submitOnChange: true, width: 3)
            input (name: "vac${i}LightHueBlue", type: "number", title: "Blue Hue", defaultValue: 66, range: "0..100", required: false, submitOnChange: true, width: 3)
            
            paragraph "<i style='font-size:12px; color:#666;'>Leave light blank to disable visual state signals for this vacuum. If set, light turns green while this vac is running a sequence, yellow when waiting on a delay, red on error, blue for water tank issues, and off when idle or cleared.</i>"
        }
    }
    section("") {
        input (name: "addVac", type: "button", title: "Add Vac", width: 1, submitOnChange: true)
        input (name: "RemoveVac", type: "button", title: "Remove Vac", width: 2, submitOnChange: true)
    }
}

void buildGlobalMatrixEngine(int numVacs) {
    def numChosen = 0
    if (numVacs > 0) {
        for (int i = 1; i <= numVacs; i++) {
            if (settings?."vac${i}") { numChosen++ }
        }
    }

    if (numVacs <= 0 || numChosen != numVacs) {
        state.globalRoomMatrix = [:]
        state.maxButtonCode = 0
        return
    }

    Map tempMatrix = [:]
    String keyString = ""
    int maxButton = 0

    for (int i = 1; i <= numVacs; i++) {
        def vac = settings?."vac${i}"
        if (!vac) continue

        String vacLabel = vac.getLabel() ?: "Vac ${i}"
        def roomsRaw = vac.currentValue("rooms")
        if (!roomsRaw) continue

        Map numKeyMap = null
        try {
            if (roomsRaw instanceof Map) { numKeyMap = roomsRaw }
            else { numKeyMap = new JsonSlurper().parseText(roomsRaw.toString()) as Map }
        } catch (Exception e) {
            log.error "Failed to parse rooms attribute for vac${i}: ${e.message}"
            continue
        }
        if (!numKeyMap) continue

        numKeyMap.each { nativeKey, roomName ->
            int nativeKeyInt
            try { nativeKeyInt = nativeKey.toString().toInteger() } catch (Exception e) { return }
            int buttonCode = (i * 100) + nativeKeyInt
            if (buttonCode > maxButton) { maxButton = buttonCode }

            tempMatrix[buttonCode.toString()] = [
                buttonCode    : buttonCode,
                vacuumIndex   : i,
                roomName      : roomName,
                nativeRoomId  : nativeKey.toString(),
                vacLabel      : vacLabel
            ]
            keyString += "${buttonCode}: ${roomName} (${vacLabel})<br>"
        }
    }

    state.globalRoomMatrix = tempMatrix
    state.maxButtonCode = maxButton

    def childDevice = getChildDevice("rr_scheduler_child_" + app.id)
    if (childDevice) {
        childDevice.sendEvent(name: "roomKey", value: keyString)
        if (maxButton > 0) {
            childDevice.sendEvent(name: "numberOfButtons", value: maxButton + 10)
        }
    }
}

void buildSpeechSetupSection() {
    section("Speech") {
        input (name: "echoCommand", type: "string", title: "Speech command method (usually 'speak' or 'speakMessage')", defaultValue: "speak", width: 6)
        paragraph "Speakers are chosen per-room in each sequence below, and per-room for instant-clean buttons. Leave a room's speaker blank to keep that room silent."
    }
}

void buildErrorAlertSection() {
    section("Vacuum Error Alerts") {
        paragraph "Announces changes to 'error' or 'dockError' on any selected vacuum. Clear values (empty, 'none', '0', 'false', etc.) are not announced unless recovering from an active error."
        input (name: "errorAlertSpeaker", type: "capability.speechSynthesis", title: "Speaker for vacuum error alerts (empty = silent)", width: 6, submitOnChange: true)
    }
}

void buildDelayEngineSection() {
    if (!state.globalRoomMatrix || state.globalRoomMatrix.size() <= 0) { return }

    section("Delay Engine") {
        paragraph "Delay devices, attributes, and trigger values are configured per-room and per-sequence below."
        input (name: "delayCheckInterval", type: "number", title: "Retry check interval while paused (minutes)", defaultValue: 5, range: "1..30", required: true, width: 12)
        input (name: "maxBlockWaitHours", type: "number", title: "Max hours to wait for a blocked sequence to start before dropping it", defaultValue: 12, range: "1..48", required: true, width: 12)
        input (name: "maxRoomBlockWaitMinutes", type: "number", title: "Max minutes to wait for a blocked room before skipping to the next room", defaultValue: 30, range: "1..180", required: true, width: 12)
    }
}

void buildSequenceConfigSection() {
    if (!state.globalRoomMatrix || state.globalRoomMatrix.size() <= 0) { return }

    section("Sequences Configuration") {
        def numSequences = (settings["numSequences"] ?: 1) as int
        app.updateSetting("numSequences", [type: "number", value: numSequences])

        Map roomOptions = [:]
        state.globalRoomMatrix.each { codeKey, data ->
            roomOptions[codeKey] = "${data.roomName} (${data.vacLabel})"
        }

        for (int s = 1; s <= numSequences; s++) {
            String customName = settings["seqName_${s}"]?.toString()?.trim()
            String headerText = "Sequence #${s}"
            if (customName && customName != "Sequence ${s}") {
                headerText += " - ${customName}"
            }

            // Sequence Header Block
            paragraph "<hr style='border: 1px solid #ccc; margin-top: 25px;'/>"
            paragraph "<div style='background-color:#e8f0fe; color:#1a73e8; padding:8px; border-radius:4px; font-size:16px;'><b>${headerText}</b></div>"

            input(name: "seqEnabled_${s}", type: "bool", title: "Enable Sequence ${s}", defaultValue: true, width: 12, submitOnChange: true)

            if (settings["seqEnabled_${s}"] != false) {
                // Sequence Basics (4+4+4 = 12)
                input (name: "seqName_${s}", type: "string", title: "Name", defaultValue: "Sequence ${s}", width: 4)
                input (name: "seqTime_${s}", type: "time", title: "Time", width: 4)
                input (name: "seqUsePeriodic_${s}", type: "bool", title: "Use Periodic Days", defaultValue: false, width: 4, submitOnChange: true)

                if (settings["seqUsePeriodic_${s}"] == true) {
                    input (name: "seqPeriodicDays_${s}", type: "enum", title: "Run every X days", options: ["2", "3", "4", "5", "6"], defaultValue: "2", width: 12, submitOnChange: true)
                } else {
                    input (name: "seqDays_${s}", type: "enum", title: "Days of Week", options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"], multiple: true, width: 12)
                }
                
                // Room Selection (12)
                input (name: "seqRooms_${s}", type: "enum", title: "Select Rooms for Sequence", options: roomOptions, multiple: true, submitOnChange: true, width: 12)

                List selectedRooms = normalizeToList(settings["seqRooms_${s}"])
                int numSelected = selectedRooms.size()
                Map orderOptions = [:]
                for (int i = 1; i <= numSelected; i++) {
                    orderOptions[i.toString()] = i.toString()
                }

                selectedRooms.eachWithIndex { roomCode, idx ->
                    Map roomMeta = state.globalRoomMatrix[roomCode.toString()]
                    if (roomMeta) {
                        // Room Header Block
                        paragraph "<div style='border-top: 1px dotted #aaa; margin-top: 15px; padding-top: 10px;'><b style='font-size:16px; color:#333;'>Room: ${roomMeta.roomName} (${roomMeta.vacLabel})</b></div>"

                        // Order (2) + Mop (3) + Speaker (7) = 12
                        input(name: "seq_${s}_order_${roomCode}", type: "enum", title: "Clean Order", options: orderOptions, defaultValue: "${idx + 1}", width: 2)
                        input (name: "seq_${s}_mop_${roomCode}", type: "enum", title: "Mop Level", options: ["DEFAULT", "AUTO", "CUSTOM", "OFF", "LOW", "MEDIUM", "HIGH"], defaultValue: "DEFAULT", width: 3)
                        input (name: "seq_${s}_speechDevice_${roomCode}", type: "capability.speechSynthesis", title: "Speaker (empty = silent)", width: 7)

                        buildRoomDelayInputs(s, roomCode.toString(), roomMeta)
                    }
                }
                
                // Sequence Footer Block
                paragraph "<div style='margin-top: 15px;'></div>"
                input (name: "seqDelayAfter_${s}", type: "number", title: "Delay Buffer (mins) after sequence finishes", defaultValue: 2, required: true, width: 12)

                // Sequence Start Delay
                paragraph "<div style='border-top: 1px dotted #aaa; margin-top: 15px; padding-top: 10px;'><b style='font-size:16px; color:#333;'>Block Sequence Start</b></div>"
                input(name: "seq_${s}_seqDelayEnable", type: "bool", title: "Delay entire sequence from starting?", defaultValue: false, width: 12, submitOnChange: true)

                if (settings["seq_${s}_seqDelayEnable"]) {
                    input(name: "seq_${s}_seqDelayDevice", type: "capability.*", title: "Delay Device", width: 7, submitOnChange: true)
                    input(name: "seq_${s}_seqDelayEq", type: "enum", title: "Delay cleaning if", options: ["=", "!="], defaultValue: "=", width: 2, submitOnChange: true)
                    def seqDelayDev = firstOrNull(settings["seq_${s}_seqDelayDevice"])
                    if (seqDelayDev) {
                        List<String> seqAttrs = []
                        seqDelayDev.getSupportedAttributes()?.each { attr ->
                            String n = attr.getName()
                            if (!seqAttrs.contains(n)) seqAttrs << n
                        }
                        seqAttrs.sort()
                        if (seqAttrs.size() > 0) {
                            input(name: "seq_${s}_seqDelayAttr", type: "enum", title: "Attribute to Monitor", options: seqAttrs, width: 6, submitOnChange: true)
                            
                            String currentSeqAttr = settings["seq_${s}_seqDelayAttr"]?.toString()
                            if (currentSeqAttr && seqAttrs.contains(currentSeqAttr)) {
                                def metaAttr = seqDelayDev.getSupportedAttributes()?.find { it.getName() == currentSeqAttr }
                                if (metaAttr) {
                                    String dt = metaAttr.dataType?.toUpperCase() ?: "STRING"
                                    def vals = metaAttr.getValues()
                                    if (vals && vals.size() > 0) {
                                        List enumVals = vals.collect { it.toString() }.sort()
                                        input(name: "seq_${s}_seqDelayValueEnum", type: "enum", title: "Attribute Value", options: enumVals, width: 6, submitOnChange: true)
                                    } else if (dt in ["NUMBER", "INTEGER", "FLOAT", "DOUBLE", "BIGDECIMAL", "LONG"]) {
                                        input(name: "seq_${s}_seqDelayNumOp", type: "enum", title: "Operator", options: ["=", ">", "<", "<=", ">="], defaultValue: "=", width: 2, submitOnChange: true)
                                        input(name: "seq_${s}_seqDelayValueNumber", type: "number", title: "Value", width: 4, submitOnChange: true)
                                    } else {
                                        input(name: "seq_${s}_seqDelayValueString", type: "string", title: "Attribute Value", width: 6, submitOnChange: true)
                                    }
                                }
                            }
                        } else {
                            paragraph "<span style='color:#b00;'>⚠ Device has no supported attributes.</span>"
                        }
                    }
                }
            } else {
                paragraph "<i style='color:#888;'>Sequence disabled. Enable it above to configure.</i>"
            }
        }
    }
    section("") {
        input (name: "addSeq", type: "button", title: "Add Sequence", width: 2, submitOnChange: true)
        input (name: "removeSeq", type: "button", title: "Remove Sequence", width: 2, submitOnChange: true)
    }
}

void buildRoomDelayInputs(int s, String roomCode, Map roomMeta) {
    String enableName = "seq_${s}_delayEnable_${roomCode}"
    input (name: enableName, type: "bool", title: "Delay this room?", defaultValue: false, width: 12, submitOnChange: true)

    if (!settings[enableName]) { return }

    int count = (settings["seq_${s}_delayCount_${roomCode}"] ?: 1) as int

    for (int i = 1; i <= count; i++) {
        String suffix = (i == 1) ? "" : "_${i}"
        String devName    = "seq_${s}_delayDevice${suffix}_${roomCode}"
        String eqName     = "seq_${s}_delayEq${suffix}_${roomCode}"
        String attrName   = "seq_${s}_delayAttr${suffix}_${roomCode}"
        String enumName   = "seq_${s}_delayValueEnum${suffix}_${roomCode}"
        String numName    = "seq_${s}_delayValueNumber${suffix}_${roomCode}"
        String strName    = "seq_${s}_delayValueString${suffix}_${roomCode}"
        String opName     = "seq_${s}_delayNumOp${suffix}_${roomCode}"

        if (!settings[eqName]) app.updateSetting(eqName, [type: "enum", value: "="])

        paragraph "<hr style='border-top: 1px dashed #ddd; margin: 10px 0;'/>"
        
        // Row 1: Device (9) + Remove (3) = 12
        if (i > 1) {
            input (name: devName, type: "capability.*", title: "Blocking Device ${i}", width: 9, submitOnChange: true)
            input(name: "removeRoomDelay_${s}_${roomCode}_${i}", type: "button", title: "Remove", width: 3, submitOnChange: true)
        } else {
            input (name: devName, type: "capability.*", title: "Blocking Device ${i}", width: 12, submitOnChange: true)
        }

        def delayDev = firstOrNull(settings[devName])
        if (delayDev) {
            List<String> attrs = []
            delayDev.getSupportedAttributes()?.each { attr ->
                String n = attr.getName()
                if (!attrs.contains(n)) attrs << n
            }
            attrs.sort()

            if (attrs.size() == 0) {
                paragraph "<span style='color:#b00;'>⚠ ${delayDev.displayName} has no supported attributes.</span>"
                continue
            }

            String savedAttr = settings[attrName]?.toString()
            if (savedAttr && !attrs.contains(savedAttr)) {
                paragraph "<span style='color:#b00;'>⚠ Previously selected attribute '${savedAttr}' is not on ${delayDev.displayName}. Please re-select.</span>"
            }

            // Row 2: Attribute (4) + Eq (3) + Value (5) = 12
            input (name: attrName, type: "enum", title: "Attribute to Monitor", options: attrs, width: 4, submitOnChange: true)
            input (name: eqName, type: "enum", title: "Delay cleaning if", options: ["=", "!="], defaultValue: "=", width: 3, submitOnChange: true)

            String currentAttr = settings[attrName]?.toString()
            if (!currentAttr || !attrs.contains(currentAttr)) { continue }

            def metaAttr = delayDev.getSupportedAttributes()?.find { it.getName() == currentAttr }
            if (!metaAttr) { continue }
            String dt = metaAttr.dataType?.toUpperCase() ?: "STRING"
            def vals = metaAttr.getValues()

            if (vals && vals.size() > 0) {
                List enumVals = vals.collect { it.toString() }.sort()
                input (name: enumName, type: "enum", title: "Attribute Value", options: enumVals, width: 5, submitOnChange: true)
            } else if (dt in ["NUMBER", "INTEGER", "FLOAT", "DOUBLE", "BIGDECIMAL", "LONG"]) {
                input (name: opName, type: "enum", title: "Operator", options: ["=", ">", "<", "<=", ">="], defaultValue: "=", width: 2, submitOnChange: true)
                input (name: numName, type: "number", title: "Value", width: 3, submitOnChange: true)
            } else {
                input (name: strName, type: "string", title: "Attribute Value", width: 5, submitOnChange: true)
            }
        }
    }

    if (count < 5) {
        input(name: "addRoomDelay_${s}_${roomCode}", type: "button", title: "Add Blocking Device", width: 12, submitOnChange: true)
    }
}

void buildInstantMopOverrideSection() {
    if (!state.globalRoomMatrix || state.globalRoomMatrix.size() <= 0) { return }

    section("Instant Clean - Mop & Speaker Overrides") {
        paragraph "Per-room mop level and speaker for instant-push dashboard buttons. Leave speaker blank for silent."
        state.globalRoomMatrix.each { codeKey, data ->
            input (name: "instantMop_${data.buttonCode}", type: "enum", title: "Mop Level [Code ${data.buttonCode}: ${data.roomName}]", options: ["DEFAULT", "AUTO", "CUSTOM", "OFF", "LOW", "MEDIUM", "HIGH"], defaultValue: "DEFAULT", width: 6)
            input (name: "instantSpeech_${data.buttonCode}", type: "capability.speechSynthesis", title: "Speaker [Code ${data.buttonCode}: ${data.roomName}]", width: 6)
        }
    }
}

void buildDashboardTileSection() {
    if (!state.globalRoomMatrix || state.globalRoomMatrix.size() <= 0) { return }
    section("Dashboard Tile") {
        input (name: "createChildDevice", type: "bool", title: "Create Status Tile?", defaultValue: false, submitOnChange: true)
    }
}

// ---------------------------------------------------------------------------
// Button handler / Lifecycle
// ---------------------------------------------------------------------------

def appButtonHandler(String buttonName) {
    logDebug "APP BUTTON PUSHED: ${buttonName}"
    
    if (buttonName.startsWith("addRoomDelay_")) {
        def parts = buttonName.split("_")
        int s = parts[1].toInteger()
        String roomCode = parts[2]
        String countKey = "seq_${s}_delayCount_${roomCode}"
        int count = (settings[countKey] ?: 1) as int
        app.updateSetting(countKey, [type: "number", value: count + 1])
        return
    }
    
    if (buttonName.startsWith("removeRoomDelay_")) {
        def parts = buttonName.split("_")
        int s = parts[1].toInteger()
        String roomCode = parts[2]
        int idx = parts[3].toInteger()
        String countKey = "seq_${s}_delayCount_${roomCode}"
        int count = (settings[countKey] ?: 1) as int
        
        // Shift settings down
        for (int j = idx; j < count; j++) {
            String suffixA = (j == 1) ? "" : "_${j}"
            String suffixB = (j + 1 == 1) ? "" : "_${j + 1}"
            app.updateSetting("seq_${s}_delayDevice${suffixA}_${roomCode}", [type: "capability.*", value: settings["seq_${s}_delayDevice${suffixB}_${roomCode}"]])
            app.updateSetting("seq_${s}_delayEq${suffixA}_${roomCode}", [type: "enum", value: settings["seq_${s}_delayEq${suffixB}_${roomCode}"]])
            app.updateSetting("seq_${s}_delayAttr${suffixA}_${roomCode}", [type: "enum", value: settings["seq_${s}_delayAttr${suffixB}_${roomCode}"]])
            app.updateSetting("seq_${s}_delayValueEnum${suffixA}_${roomCode}", [type: "enum", value: settings["seq_${s}_delayValueEnum${suffixB}_${roomCode}"]])
            app.updateSetting("seq_${s}_delayValueNumber${suffixA}_${roomCode}", [type: "number", value: settings["seq_${s}_delayValueNumber${suffixB}_${roomCode}"]])
            app.updateSetting("seq_${s}_delayValueString${suffixA}_${roomCode}", [type: "string", value: settings["seq_${s}_delayValueString${suffixB}_${roomCode}"]])
            app.updateSetting("seq_${s}_delayNumOp${suffixA}_${roomCode}", [type: "enum", value: settings["seq_${s}_delayNumOp${suffixB}_${roomCode}"]])
        }
        
        // Remove last
        String lastSuffix = (count == 1) ? "" : "_${count}"
        app.removeSetting("seq_${s}_delayDevice${lastSuffix}_${roomCode}")
        app.removeSetting("seq_${s}_delayEq${lastSuffix}_${roomCode}")
        app.removeSetting("seq_${s}_delayAttr${lastSuffix}_${roomCode}")
        app.removeSetting("seq_${s}_delayValueEnum${lastSuffix}_${roomCode}")
        app.removeSetting("seq_${s}_delayValueNumber${lastSuffix}_${roomCode}")
        app.removeSetting("seq_${s}_delayValueString${lastSuffix}_${roomCode}")
        app.removeSetting("seq_${s}_delayNumOp${lastSuffix}_${roomCode}")
        
        app.updateSetting(countKey, [type: "number", value: count - 1])
        return
    }

    switch(buttonName) {
        case "addVac":
            app.updateSetting("numVacs", [type: "number", value: ((settings["numVacs"] ?: 0) as int) + 1])
            break
        case "RemoveVac":
            int vCur = (settings["numVacs"] ?: 0) as int
            if (vCur > 0) {
                app.removeSetting("vac${vCur}")
                app.removeSetting("vac${vCur}AutoEmpty")
                app.removeSetting("vac${vCur}BinSqrFeet")
                app.removeSetting("vac${vCur}Light")
                app.removeSetting("vac${vCur}LightLevel")
                app.removeSetting("vac${vCur}LightHueGreen")
                app.removeSetting("vac${vCur}LightHueYellow")
                app.removeSetting("vac${vCur}LightHueRed")
                app.removeSetting("vac${vCur}LightHueBlue")
                app.updateSetting("numVacs", [type: "number", value: vCur - 1])
            }
            break
        case "addSeq":
            app.updateSetting("numSequences", [type: "number", value: ((settings["numSequences"] ?: 1) as int) + 1])
            break
        case "removeSeq":
            int sCur = (settings["numSequences"] ?: 1) as int
            if (sCur > 1) {
                app.removeSetting("seqEnabled_${sCur}")
                app.removeSetting("seqName_${sCur}")
                app.removeSetting("seqTime_${sCur}")
                app.removeSetting("seqUsePeriodic_${sCur}")
                app.removeSetting("seqPeriodicDays_${sCur}")
                app.removeSetting("seqDays_${sCur}")
                app.removeSetting("seqRooms_${sCur}")
                app.removeSetting("seqDelayAfter_${sCur}")
                app.removeSetting("seq_${sCur}_seqDelayEnable")
                app.removeSetting("seq_${sCur}_seqDelayDevice")
                app.removeSetting("seq_${sCur}_seqDelayEq")
                app.removeSetting("seq_${sCur}_seqDelayAttr")
                app.removeSetting("seq_${sCur}_seqDelayValueEnum")
                app.removeSetting("seq_${sCur}_seqDelayValueNumber")
                app.removeSetting("seq_${sCur}_seqDelayValueString")
                app.removeSetting("seq_${sCur}_seqDelayNumOp")
                if (state.globalRoomMatrix) {
                    state.globalRoomMatrix.each { codeKey, data ->
                        app.removeSetting("seq_${sCur}_order_${codeKey}")
                        app.removeSetting("seq_${sCur}_mop_${codeKey}")
                        app.removeSetting("seq_${sCur}_speechDevice_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayEnable_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayCount_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayDevice_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayDevice_2_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayDevice_3_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayDevice_4_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayDevice_5_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayEq_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayEq_2_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayEq_3_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayEq_4_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayEq_5_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayAttr_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayAttr_2_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayAttr_3_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayAttr_4_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayAttr_5_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueEnum_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueEnum_2_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueEnum_3_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueEnum_4_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueEnum_5_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueNumber_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueNumber_2_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueNumber_3_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueNumber_4_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueNumber_5_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueString_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueString_2_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueString_3_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueString_4_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayValueString_5_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayNumOp_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayNumOp_2_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayNumOp_3_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayNumOp_4_${codeKey}")
                        app.removeSetting("seq_${sCur}_delayNumOp_5_${codeKey}")
                    }
                }
                app.updateSetting("numSequences", [type: "number", value: sCur - 1])
            }
            break
    }
}

def installed() { initialize() }
def updated() { initialize() }

def initialize() {
    logDebug "Initializing app..."
    unschedule()
    unsubscribe()

    state.activeSequences = [:]
    if (state.lastErrorAnnounce == null) state.lastErrorAnnounce = [:]
    if (state.sentVacLists == null) state.sentVacLists = [:]
    if (state.sentBinPercents == null) state.sentBinPercents = [:]

    int numVacs = (settings["numVacs"] ?: 0) as int
    if (numVacs > 0) {
        for (int i = 1; i <= numVacs; i++) {
            if (state."vac${i}TotalSqFt" == null) state."vac${i}TotalSqFt" = 0
            if (state."vac${i}Errors" == null) state."vac${i}Errors" = [:]
            
            def vacDevice = settings?."vac${i}"
            if (vacDevice) {
                subscribe(vacDevice, "state", "vacuumStateHandler")
                try {
                    Set attrNames = vacDevice.getSupportedAttributes()?.collect { it.getName() } ?: [] as Set
                    if ("error" in attrNames) subscribe(vacDevice, "error", "vacuumErrorHandler")
                    if ("dockError" in attrNames) subscribe(vacDevice, "dockError", "vacuumErrorHandler")
                } catch (Exception e) {}
            }
        }
    }

    Map subsByAttr = [:]  
    int numSeqs = (settings["numSequences"] ?: 1) as int
    for (int s = 1; s <= numSeqs; s++) {
        if (settings["seqEnabled_${s}"] == false) continue
        
        if (settings["seq_${s}_seqDelayEnable"]) {
            def dev = firstOrNull(settings["seq_${s}_seqDelayDevice"])
            String attr = settings["seq_${s}_seqDelayAttr"]?.toString()
            if (dev && attr) {
                if (!subsByAttr[attr]) subsByAttr[attr] = [:]
                subsByAttr[attr][dev.id.toString()] = dev
            }
        }

        List rooms = normalizeToList(settings["seqRooms_${s}"])
        rooms.each { code ->
            if (!(settings["seq_${s}_delayEnable_${code}"] as Boolean)) return
            int count = (settings["seq_${s}_delayCount_${code}"] ?: 1) as int
            for (int i = 1; i <= count; i++) {
                String suffix = (i == 1) ? "" : "_${i}"
                def dev = firstOrNull(settings["seq_${s}_delayDevice${suffix}_${code}"])
                String attr = settings["seq_${s}_delayAttr${suffix}_${code}"]?.toString()
                if (dev && attr) {
                    if (!subsByAttr[attr]) subsByAttr[attr] = [:]
                    subsByAttr[attr][dev.id.toString()] = dev
                }
            }
        }
    }
    subsByAttr.each { attr, devMap ->
        subscribe(devMap.values() as List, attr, "delayDeviceChangeHandler")
    }

    manageChildDevice()
    buildGlobalMatrixEngine(numVacs)
    buildAutomatedCronSchedules()
    compileAndPublishSequenceList()
    updateBinPercentages()
    updateVisualLights()

    logDebug "initialize() complete. Rooms in matrix: ${state.globalRoomMatrix?.size() ?: 0}"
}

void manageChildDevice() {
    String childNetworkId = "rr_scheduler_child_" + app.id
    def childDevice = getChildDevice(childNetworkId)
    boolean wantDevice = settings.createChildDevice ?: false

    if (wantDevice && childDevice) { 
        childDevice.setAppFrame()
        return 
    }
    if (!wantDevice && !childDevice) { return }

    if (!wantDevice) {
        logDebug "Deleting child device"
        deleteChildDevice(childNetworkId)
        return
    }

    Map childProperties = [:]
    childProperties.label = "Roborock Scheduler Tile"
    childProperties.isComponent = true
    logDebug "Creating child device ${childNetworkId}"
    def newDevice = addChildDevice("Hubitat", "Roborock Scheduler Display Tile", childNetworkId, null, childProperties)
    if (newDevice) newDevice.setAppFrame()
    updateDashboardHtml("Ready.")
}

// ---------------------------------------------------------------------------
// Visual State Light Engine
// ---------------------------------------------------------------------------

void updateVisualLights() {
    if (!state.activeSequences) state.activeSequences = [:]
    Map desiredStates = [:] // lightId -> state ("green", "red", "blue", "yellow", "off")
    
    int numVacs = (settings["numVacs"] ?: 0) as int
    
    for (int i = 1; i <= numVacs; i++) {
        def light = settings["vac${i}Light"]
        if (!light) continue
        
        String lid = light.id.toString()
        String currentDesired = desiredStates[lid] ?: "off"
        
        // 1. Check errors
        Map errs = state."vac${i}Errors" ?: [:]
        String errorColor = null
        errs.each { k, v ->
            if (v) {
                String lower = v.toLowerCase()
                if (lower.contains("waste water tank full") || lower.contains("water empty")) {
                    errorColor = "blue"
                } else if (!errorColor) {
                    errorColor = "red"
                }
            }
        }
        
        if (errorColor) {
            if (errorColor == "blue" || currentDesired != "blue") {
                desiredStates[lid] = errorColor
            }
        } else {
            // 2. Check waiting/running states
            boolean waiting = false
            boolean running = false
            
            state.activeSequences.each { seqKey, seqState ->
                if (seqState) {
                    if (seqState.waitingForStart) {
                        // Check if this vac is part of the sequence
                        List seqRooms = normalizeToList(settings["seqRooms_${seqKey}"])
                        if (seqRooms.any { rc -> state.globalRoomMatrix[rc.toString()]?.vacuumIndex == i }) {
                            waiting = true
                        }
                    } else if (seqState.running) {
                        int step = seqState.step as int
                        List rooms = seqState.rooms as List
                        if (step > 0 && step <= rooms.size()) {
                            String roomCode = rooms[step - 1]
                            Map rData = state.globalRoomMatrix[roomCode.toString()]
                            if (rData?.vacuumIndex == i) {
                                if (seqState.pausedForDelay) {
                                    waiting = true
                                } else {
                                    running = true
                                }
                            }
                        }
                    }
                }
            }
            
            if (waiting && currentDesired != "red" && currentDesired != "blue") {
                desiredStates[lid] = "yellow"
            } else if (running && currentDesired != "red" && currentDesired != "blue" && currentDesired != "yellow") {
                desiredStates[lid] = "green"
            }
        }
    }
    
    // Apply states
    Map processedLights = [:]
    for (int i = 1; i <= numVacs; i++) {
        def light = settings["vac${i}Light"]
        if (light) {
            String lid = light.id.toString()
            if (processedLights.containsKey(lid)) continue
            processedLights[lid] = true
            
            String stateStr = desiredStates[lid] ?: "off"
            int level = (settings["vac${i}LightLevel"] ?: 100) as int
            if (level <= 0 || level > 100) level = 100
            
            int hueG = (settings["vac${i}LightHueGreen"] != null) ? settings["vac${i}LightHueGreen"].toString().toInteger() : 33
            int hueY = (settings["vac${i}LightHueYellow"] != null) ? settings["vac${i}LightHueYellow"].toString().toInteger() : 16
            int hueR = (settings["vac${i}LightHueRed"] != null) ? settings["vac${i}LightHueRed"].toString().toInteger() : 0
            int hueB = (settings["vac${i}LightHueBlue"] != null) ? settings["vac${i}LightHueBlue"].toString().toInteger() : 66
            
            if (stateStr == "green") {
                light.setColor([hue: hueG, saturation: 100, level: level])
            } else if (stateStr == "red") {
                light.setColor([hue: hueR, saturation: 100, level: level])
            } else if (stateStr == "blue") {
                light.setColor([hue: hueB, saturation: 100, level: level])
            } else if (stateStr == "yellow") {
                light.setColor([hue: hueY, saturation: 100, level: level])
            } else if (stateStr == "off") {
                light.off()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Scheduling
// ---------------------------------------------------------------------------

void buildAutomatedCronSchedules() {
    int totalSeqs = (settings["numSequences"] ?: 1) as int
    for (int s = 1; s <= totalSeqs; s++) {
        if (settings["seqEnabled_${s}"] == false) {
            logDebug "Sequence ${s} is disabled. Skipping schedule."
            continue
        }

        def timeInput = settings["seqTime_${s}"]
        if (!timeInput) continue

        try {
            Date parsedTime = parseTimeInput(timeInput)
            if (!parsedTime) continue
            Calendar cal = Calendar.getInstance()
            cal.setTime(parsedTime)

            String cronExpression = ""
            if (settings["seqUsePeriodic_${s}"] == true) {
                int interval = (settings["seqPeriodicDays_${s}"] ?: 2) as int
                cronExpression = "0 ${cal.get(Calendar.MINUTE)} ${cal.get(Calendar.HOUR_OF_DAY)} 1/${interval} * ?"
                logDebug "Scheduled sequence ${s} PERIODICALLY (every ${interval} days) with cron '${cronExpression}'"
            } else {
                List daysInput = normalizeToList(settings["seqDays_${s}"])
                if (daysInput.size() == 0) continue
                String daysString = daysInput.collect { it.toString().substring(0,3).toUpperCase() }.join(',')
                cronExpression = "0 ${cal.get(Calendar.MINUTE)} ${cal.get(Calendar.HOUR_OF_DAY)} ? * ${daysString}"
                logDebug "Scheduled sequence ${s} WEEKLY with cron '${cronExpression}'"
            }

            Map data = [seqIdx: s]
            schedule(cronExpression, "automatedSequenceTrigger", [data: data, overwrite: false])
        } catch (Exception e) {
            log.error "Failed to schedule sequence ${s}: ${e.message}"
        }
    }
}

void automatedSequenceTrigger(Map data) {
    Integer seqIdx = data?.seqIdx
    if (seqIdx != null) {
        logDebug "Cron trigger fired for sequence ${seqIdx}"
        startSequenceExecution(seqIdx)
    }
}

// ---------------------------------------------------------------------------
// Sequence engine
// ---------------------------------------------------------------------------

void startSequenceExecution(int seqIndex) {
    logDebug "STARTSEQUENCEEXECUTION: Checking Seq ${seqIndex}"
    if (settings["seqEnabled_${seqIndex}"] == false) {
        logDebug "Sequence ${seqIndex} is disabled. Ignoring trigger."
        return
    }

    if (state.activeSequences == null) state.activeSequences = [:]
    
    String seqKey = seqIndex.toString()
    if (state.activeSequences.containsKey(seqKey)) {
        log.warn "Sequence ${seqIndex} skipped: already running or waiting to start."
        return
    }

    if (isSequenceDelayedForSequence(seqIndex)) {
        state.activeSequences[seqKey] = [
            running: false,
            waitingForStart: true,
            seqBlockedSince: now()
        ]
        String blockDev = getSeqBlockDeviceName(seqIndex)
        updateDashboardHtml("${getSeqName(seqIndex)} is waiting for ${blockDev}")
        logDebug "Seq ${seqIndex} BLOCKED by ${blockDev}. Waiting to start. Retry in ${(settings.delayCheckInterval ?: 5)} min."
        updateVisualLights()
        runIn(((settings.delayCheckInterval ?: 5) as int) * 60, "retrySequenceStart", [data: [seqIdx: seqIndex]])
        return
    }
    
    launchSequence(seqIndex)
}

void launchSequence(int seqIndex) {
    String seqKey = seqIndex.toString()
    def seqState = state.activeSequences?.get(seqKey)
    if (seqState?.running) return

    List roomsToClean = normalizeToList(settings["seqRooms_${seqIndex}"])
    if (!roomsToClean) return

    List roomObjs = []
    roomsToClean.eachWithIndex { code, idx ->
        def orderVal = settings["seq_${seqIndex}_order_${code}"]
        int orderNum = idx + 1
        if (orderVal != null) {
            try { orderNum = orderVal.toString().toInteger() } catch (Exception e) {}
        }
        roomObjs << [ code: code, order: orderNum, origIdx: idx ]
    }
    roomObjs.sort { a, b -> 
        if (a.order == b.order) return a.origIdx <=> b.origIdx
        return a.order <=> b.order 
    }
    List sortedRooms = roomObjs.collect { it.code }

    if (state.activeSequences == null) state.activeSequences = [:]
    state.activeSequences[seqKey] = [
        running: true,
        waitingForStart: false,
        step: 1,
        rooms: sortedRooms,
        intermissionDelayActive: false,
        pausedForDelay: false,
        roomBlockedSince: null,
        lastStartedStep: null
    ]

    logDebug "LAUNCHSEQUENCE: START seq ${seqIndex}: ${sortedRooms.size()} room(s) -> ${sortedRooms}"
    updateVisualLights()
    executeSequenceEngine([seqIdx: seqIndex, targetStep: 1])
}

void retrySequenceStart(Map data) {
    Integer seqIdx = data?.seqIdx
    if (seqIdx == null) return
    String seqKey = seqIdx.toString()
    def seqState = state.activeSequences?.get(seqKey)
    if (!seqState || !seqState.waitingForStart) return

    if (isSequenceDelayedForSequence(seqIdx)) {
        long maxMs = ((settings.maxBlockWaitHours ?: 12) as long) * 3600000L
        long blockedSince = (seqState.seqBlockedSince ?: now()) as long
        
        if (now() - blockedSince > maxMs) {
            log.warn "Sequence ${seqIdx} TIMED OUT waiting for start block to clear. Dropping this run."
            updateDashboardHtml("${getSeqName(seqIdx)} timed out waiting to start.")
            state.activeSequences.remove(seqKey)
            updateVisualLights()
            return
        }
        
        String blockDev = getSeqBlockDeviceName(seqIdx)
        logDebug "RETRYSEQSTART: Seq ${seqIdx} still BLOCKED by ${blockDev}. Retrying in ${(settings.delayCheckInterval ?: 5)} min."
        int delaySecs = ((settings.delayCheckInterval ?: 5) as int) * 60
        runIn(delaySecs, "retrySequenceStart", [data: [seqIdx: seqIdx]])
    } else {
        logDebug "RETRYSEQSTART: Seq ${seqIdx} start block CLEARED. Launching."
        launchSequence(seqIdx)
    }
}

void executeSequenceEngine(Map data) {
    Integer seqIdx = data?.seqIdx
    int targetStep = data?.targetStep ?: 1
    if (seqIdx == null) return
    
    String seqKey = seqIdx.toString()
    def seqState = state.activeSequences?.get(seqKey)
    if (!seqState || !seqState.running) return

    if (seqState.lastStartedStep == targetStep) return

    seqState.intermissionDelayActive = false
    seqState.step = targetStep
    state.activeSequences[seqKey] = seqState

    if (targetStep > seqState.rooms.size()) {
        terminateSequence(seqIdx, true)
        return
    }

    String currentRoomCode = seqState.rooms[targetStep - 1]
    Map roomData = state.globalRoomMatrix ? state.globalRoomMatrix[currentRoomCode.toString()] : null
    String roomLabel = roomData?.roomName ?: currentRoomCode
    String vacLabel = roomData?.vacLabel ?: "Vacuum"

    String blockingDevice = getRoomDelayBlockingDevice(seqIdx, currentRoomCode)
    if (blockingDevice != null) {
        // Room block timeout: use maxRoomBlockWaitMinutes (default 30)
        int maxMin = (settings.maxRoomBlockWaitMinutes ?: 30) as int
        long maxMs = (maxMin as long) * 60000L
        long blockedSince = (seqState.roomBlockedSince ?: now()) as long
        
        if (now() - blockedSince > maxMs) {
            log.warn "Room ${roomLabel} TIMED OUT waiting for block to clear (${maxMin} min). Skipping to next room."
            // Emit skip status first so it logs as a distinct child-device event
            updateDashboardHtml("${getSeqName(seqIdx)}: Skipped room '${roomLabel}' (blocked by ${blockingDevice} for ${maxMin}+ min)")
            seqState.roomBlockedSince = null
            seqState.pausedForDelay = false
            state.activeSequences[seqKey] = seqState
            executeSequenceEngine([seqIdx: seqIdx, targetStep: targetStep + 1])
            return
        }
        
        if (!seqState.pausedForDelay) {
            seqState.pausedForDelay = true
            if (seqState.roomBlockedSince == null) seqState.roomBlockedSince = now()
            state.activeSequences[seqKey] = seqState
            updateDashboardHtml("${vacLabel} is waiting for ${blockingDevice}")
            logDebug "EXECENGINE: Step ${targetStep} (${roomLabel}) BLOCKED by ${blockingDevice}. Waiting..."
            updateVisualLights()
        }
        
        runIn(((settings.delayCheckInterval ?: 5) as int) * 60, "executeSequenceEngine", [data: [seqIdx: seqIdx, targetStep: targetStep]])
        return
    }

    // It's clear!
    seqState.pausedForDelay = false
    seqState.roomBlockedSince = null
    seqState.lastStartedStep = targetStep
    state.activeSequences[seqKey] = seqState
    
    if (!roomData) {
        logDebug "EXECENGINE: Room data missing for ${currentRoomCode}, skipping."
        executeSequenceEngine([seqIdx: seqIdx, targetStep: targetStep + 1])
        return
    }

    def targetVacDevice = settings?."vac${roomData.vacuumIndex}"
    if (!targetVacDevice) {
        logDebug "EXECENGINE: Vac ${roomData.vacuumIndex} not configured, skipping."
        executeSequenceEngine([seqIdx: seqIdx, targetStep: targetStep + 1])
        return
    }

    logDebug "EXECENGINE: Seq ${seqIdx} Step ${targetStep} CLEAR. Starting ${roomLabel} on ${vacLabel}."
    updateDashboardHtml("${getSeqName(seqIdx)} Active: Cleaning ${roomLabel}")
    updateVisualLights()
    executeStepAnnouncement(seqIdx, currentRoomCode, roomData.roomName, roomData.vacLabel)

    String mopConfig = settings["seq_${seqIdx}_mop_${currentRoomCode}"] ?: "DEFAULT"
    try {
        targetVacDevice.appRoomClean(roomData.nativeRoomId.toString(), mopConfig)
    } catch (Exception e) {
        log.error "appRoomClean failed for room ${roomData.roomName}: ${e.message}"
        runIn(5, "executeSequenceEngine", [data: [seqIdx: seqIdx, targetStep: targetStep + 1]])
    }
}

void vacuumStateHandler(evt) {
    String newState = evt.value?.toString()?.trim()?.toLowerCase()
    logDebug "VAC-STATE: ${evt.device?.displayName} -> '${newState}'"
    
    // Detect "emptying" state to reset SqFt counter (manual or auto empties)
    if (newState?.contains("emptying") || newState == "empty_bin") {
        String vacId = evt.deviceId?.toString() ?: evt.getDevice()?.getId()?.toString()
        int numVacs = (settings["numVacs"] ?: 0) as int
        for (int i = 1; i <= numVacs; i++) {
            def vd = settings?."vac${i}"
            if (vd?.getId()?.toString() == vacId) {
                boolean autoEmpty = settings["vac${i}AutoEmpty"] ?: false
                if (autoEmpty) {
                    BigDecimal prev = (state."vac${i}TotalSqFt" ?: 0) as BigDecimal
                    state."vac${i}TotalSqFt" = 0
                    logDebug "VAC-STATE: ${vd.displayName} is emptying bin. Reset SqFt counter (was ${prev})."
                    updateDashboardHtml("${vd.displayName} emptied bin. SqFt counter reset.")
                    updateBinPercentages()
                }
                break
            }
        }
        return  // Not a sequence completion state
    }
    
    if (!state.activeSequences) {
        logDebug "VAC-STATE: No active sequences."
        return
    }
    
    if (!(newState in ["charging", "docked", "charging_complete"])) {
        logDebug "VAC-STATE: Ignoring state '${newState}'"
        return
    }

    String vacId = evt.deviceId?.toString() ?: evt.getDevice()?.getId()?.toString()
    if (!vacId) {
        logDebug "VAC-STATE: Could not determine vacId from event."
        return
    }
    
    List<String> seqKeys = state.activeSequences.keySet().toList()
    for (String seqKey in seqKeys) {
        def seqState = state.activeSequences[seqKey]
        if (!seqState) {
            logDebug "VAC-STATE: Seq ${seqKey} state is null."
            continue
        }
        if (!seqState.running) {
            logDebug "VAC-STATE: Seq ${seqKey} is not running."
            continue
        }
        if (seqState.intermissionDelayActive) {
            logDebug "VAC-STATE: Seq ${seqKey} is in intermission delay."
            continue
        }
        if (seqState.pausedForDelay) {
            logDebug "VAC-STATE: Seq ${seqKey} is paused for delay."
            continue
        }
        
        Integer seqIdx = seqKey.toInteger()
        int currentStep = seqState.step as int
        List roomsList = seqState.rooms as List
        if (currentStep <= 0 || currentStep > roomsList.size()) {
            logDebug "VAC-STATE: Seq ${seqIdx} step ${currentStep} out of bounds."
            continue
        }
        
        String currentRoomCode = roomsList[currentStep - 1]
        Map roomData = state.globalRoomMatrix ? state.globalRoomMatrix[currentRoomCode.toString()] : null
        if (!roomData) {
            logDebug "VAC-STATE: Seq ${seqIdx} roomData null for code ${currentRoomCode}."
            continue
        }
        
        def expectedVac = settings?."vac${roomData.vacuumIndex}"
        if (!expectedVac) {
            logDebug "VAC-STATE: Seq ${seqIdx} expectedVac not found for index ${roomData.vacuumIndex}."
            continue
        }
        
        if (expectedVac?.getId()?.toString() != vacId) {
            logDebug "VAC-STATE: Seq ${seqIdx} vacuum mismatch. Expected ${expectedVac.displayName}, got ${evt.device?.displayName}."
            continue
        }
        
        logDebug "VAC-STATE: ${evt.device?.displayName} finished cleaning ${roomData.roomName}."
        int vacIdx = roomData.vacuumIndex
        
        boolean autoEmpty = settings["vac${vacIdx}AutoEmpty"] ?: false
        if (autoEmpty) {
            def areaStr = expectedVac.currentValue("cleanArea")
            if (areaStr != null) {
                BigDecimal area = 0
                try { area = areaStr.toString() as BigDecimal } catch (Exception e) {}
                
                BigDecimal currentTotal = (state."vac${vacIdx}TotalSqFt" ?: 0) as BigDecimal
                currentTotal += area
                
                BigDecimal limit = (settings["vac${vacIdx}BinSqrFeet"] ?: 50) as BigDecimal
                
                if (currentTotal >= limit) {
                    logDebug "VAC-STATE: Auto-empty threshold reached (${currentTotal} >= ${limit}). Triggering dust collection."
                    try { expectedVac.execute("app_start_collect_dust") } catch (Exception e) {}
                    currentTotal = 0
                }
                state."vac${vacIdx}TotalSqFt" = currentTotal
                logDebug "VAC-STATE: Area total is now ${currentTotal} / ${limit}"
                updateBinPercentages()
            }
        }

        int bufferMinutes = (settings["seqDelayAfter_${seqIdx}"] ?: 2) as int
        seqState.intermissionDelayActive = true
        state.activeSequences[seqKey] = seqState
        
        logDebug "VAC-STATE: Seq ${seqIdx} waiting ${bufferMinutes} min before next room."
        
        if (bufferMinutes > 0) {
            runIn(bufferMinutes * 60, "executeSequenceEngine", [data: [seqIdx: seqIdx, targetStep: currentStep + 1]])
        } else {
            executeSequenceEngine([seqIdx: seqIdx, targetStep: currentStep + 1])
        }
    }
}

void delayDeviceChangeHandler(evt) {
    if (!state.activeSequences) return

    List<String> seqKeys = state.activeSequences.keySet().toList()
    for (String seqKey in seqKeys) {
        def seqState = state.activeSequences[seqKey]
        if (!seqState) continue
        
        Integer seqIdx = seqKey.toInteger()
        
        if (seqState.waitingForStart) {
            if (!isSequenceDelayedForSequence(seqIdx)) {
                logDebug "DELAY-EVT: Seq ${seqIdx} start block CLEARED. Launching."
                launchSequence(seqIdx)
            }
        } else if (seqState.running && seqState.pausedForDelay && !seqState.intermissionDelayActive) {
            int currentStep = seqState.step as int
            List roomsList = seqState.rooms as List
            if (currentStep > 0 && currentStep <= roomsList.size()) {
                String currentRoomCode = roomsList[currentStep - 1]
                if (getRoomDelayBlockingDevice(seqIdx, currentRoomCode) == null) {
                    logDebug "DELAY-EVT: Seq ${seqIdx} room ${currentRoomCode} block CLEARED. Resuming."
                    executeSequenceEngine([seqIdx: seqIdx, targetStep: currentStep])
                }
            }
        }
    }
}

void vacuumErrorHandler(evt) {
    String attr = evt.name?.toString() ?: "?"
    String newVal = evt.value?.toString()?.trim() ?: ""
    String oldVal = evt.oldValue?.toString()?.trim() ?: ""

    // Find which vacuum this belongs to
    String vacId = evt.deviceId?.toString() ?: evt.getDevice()?.getId()?.toString()
    int vacIdx = -1
    int numVacs = (settings["numVacs"] ?: 0) as int
    for (int i = 1; i <= numVacs; i++) {
        if (settings?."vac${i}"?.getId()?.toString() == vacId) {
            vacIdx = i
            break
        }
    }

    if (vacIdx != -1) {
        if (state."vac${vacIdx}Errors" == null) state."vac${vacIdx}Errors" = [:]
        if (isClearErrorValue(newVal)) {
            state."vac${vacIdx}Errors".remove(attr)
        } else {
            state."vac${vacIdx}Errors"[attr] = newVal
        }
        updateVisualLights()
    }

    logDebug "ERROR-EVT: ${evt.device?.displayName} ${attr} changed from '${oldVal}' to '${newVal}'"

    boolean newClear = isClearErrorValue(newVal)
    boolean oldClear = isClearErrorValue(oldVal)
    String deviceName = evt.device?.displayName ?: "Vacuum"

    def spk = settings.errorAlertSpeaker
    if (!spk) return

    String msg = null
    if (!newClear) {
        msg = "${deviceName} reports ${attr}: ${newVal}"
    } else if (!oldClear && newClear) {
        msg = "${deviceName} ${attr} cleared"
    } else {
        return
    }

    String dedupKey = "${evt.device?.id ?: '?'}:${attr}:${newVal}"
    Map lastAnnounce = (state.lastErrorAnnounce ?: [:]) as Map
    long nowMs = now()
    long lastMs = (lastAnnounce[dedupKey] ?: 0) as long
    if ((nowMs - lastMs) < 60000) {
        logDebug "ERROR-EVT: Suppressed duplicate announcement."
        return
    }

    lastAnnounce[dedupKey] = nowMs
    state.lastErrorAnnounce = lastAnnounce

    String command = (settings.echoCommand ?: "speak").trim().toLowerCase()
    logDebug "ERROR-EVT: Announcing to ${spk.displayName}: '${msg}'"
    try { 
        if (command == "speakmessage") spk.speakMessage(msg) else spk.speak(msg)
    } catch (Exception e) {
        log.error "Error announcement failed on ${spk.displayName}: ${e.message}"
    }
}

void updateBinPercentages() {
    def childDevice = getChildDevice("rr_scheduler_child_" + app.id)
    if (!childDevice) return
    
    if (state.sentBinPercents == null) state.sentBinPercents = [:]
    
    int numVacs = (settings["numVacs"] ?: 0) as int
    for (int i = 1; i <= 10; i++) {
        if (i <= numVacs) {
            def vac = settings?."vac${i}"
            boolean autoEmpty = settings["vac${i}AutoEmpty"] ?: false
            if (vac && autoEmpty) {
                BigDecimal total = (state."vac${i}TotalSqFt" ?: 0) as BigDecimal
                BigDecimal limit = (settings["vac${i}BinSqrFeet"] ?: 50) as BigDecimal
                if (limit <= 0) limit = 50
                
                int percent = Math.min(100, Math.round((total / limit) * 100))
                String val = "${percent}%"
                
                childDevice.sendEvent(name: "binPercent${i}", value: val)
                state.sentBinPercents[i] = true
            } else {
                if (state.sentBinPercents[i]) {
                    childDevice.sendEvent(name: "binPercent${i}", value: "")
                    state.sentBinPercents[i] = false
                }
            }
        } else {
            if (state.sentBinPercents[i]) {
                childDevice.sendEvent(name: "binPercent${i}", value: "")
                state.sentBinPercents[i] = false
            }
        }
    }
}

String getRoomDelayBlockingDevice(int seqIdx, String roomCode) {
    if (!(settings["seq_${seqIdx}_delayEnable_${roomCode}"] as Boolean)) return null
    int count = (settings["seq_${seqIdx}_delayCount_${roomCode}"] ?: 1) as int

    for (int i = 1; i <= count; i++) {
        String suffix = (i == 1) ? "" : "_${i}"
        String devName    = "seq_${seqIdx}_delayDevice${suffix}_${roomCode}"
        String eqName     = "seq_${seqIdx}_delayEq${suffix}_${roomCode}"
        String attrName   = "seq_${seqIdx}_delayAttr${suffix}_${roomCode}"
        String enumName   = "seq_${seqIdx}_delayValueEnum${suffix}_${roomCode}"
        String numName    = "seq_${seqIdx}_delayValueNumber${suffix}_${roomCode}"
        String strName    = "seq_${seqIdx}_delayValueString${suffix}_${roomCode}"
        String opName     = "seq_${seqIdx}_delayNumOp${suffix}_${roomCode}"

        def dev = firstOrNull(settings[devName])
        if (!dev) continue
        String attr = settings[attrName]?.toString()
        if (!attr) continue
        String eq = settings[eqName]?.toString() ?: "="

        def metaAttr = dev.getSupportedAttributes()?.find { it.getName() == attr }
        if (!metaAttr) continue

        String dt = metaAttr.dataType?.toUpperCase() ?: "STRING"
        def vals = metaAttr.getValues()

        boolean blockMatch = false

        if (vals && vals.size() > 0) {
            String target = settings[enumName]?.toString()?.trim()
            if (!target) continue
            String currentVal = dev.currentValue(attr)?.toString()?.trim()
            blockMatch = (currentVal == target)
            if (eq == "!=") blockMatch = !blockMatch
            logDebug "BlockEval: Room ${roomCode}, Dev ${dev.displayName}, ${attr}: '${currentVal}' == '${target}' ? ${blockMatch}"
        } else if (dt in ["NUMBER", "INTEGER", "FLOAT", "DOUBLE", "BIGDECIMAL", "LONG"]) {
            def rawNum = settings[numName]
            if (rawNum == null) continue
            BigDecimal targetNum
            try { targetNum = rawNum.toString() as BigDecimal } catch (Exception e) { continue }

            String operator = settings[opName]?.toString() ?: "="
            def cv = dev.currentValue(attr)
            if (cv == null) continue
            BigDecimal cn
            try { cn = cv.toString() as BigDecimal } catch (Exception e) { continue }

            switch (operator) {
                case "=":  blockMatch = cn == targetNum; break
                case ">":  blockMatch = cn > targetNum; break
                case "<":  blockMatch = cn < targetNum; break
                case "<=": blockMatch = cn <= targetNum; break
                case ">=": blockMatch = cn >= targetNum; break
            }
            if (eq == "!=") blockMatch = !blockMatch
            logDebug "BlockEval: Room ${roomCode}, Dev ${dev.displayName}, ${attr}: ${cn} ${operator} ${targetNum} (Eq: ${eq}) ? ${blockMatch}"
        } else {
            String target = settings[strName]?.toString()?.trim()
            if (!target) continue
            String currentVal = dev.currentValue(attr)?.toString()?.trim()
            blockMatch = (currentVal == target)
            if (eq == "!=") blockMatch = !blockMatch
            logDebug "BlockEval: Room ${roomCode}, Dev ${dev.displayName}, ${attr}: '${currentVal}' == '${target}' (Eq: ${eq}) ? ${blockMatch}"
        }

        if (blockMatch) {
            return dev.displayName
        }
    }
    return null
}

Boolean isSequenceDelayedForSequence(int seqIdx) {
    if (!(settings["seq_${seqIdx}_seqDelayEnable"] as Boolean)) return false
    def dev = firstOrNull(settings["seq_${seqIdx}_seqDelayDevice"])
    if (!dev) return false
    String attr = settings["seq_${seqIdx}_seqDelayAttr"]?.toString()
    if (!attr) return false
    String eq = settings["seq_${seqIdx}_seqDelayEq"]?.toString() ?: "="

    def metaAttr = dev.getSupportedAttributes()?.find { it.getName() == attr }
    if (!metaAttr) return false

    String dt = metaAttr.dataType?.toUpperCase() ?: "STRING"
    def vals = metaAttr.getValues()

    String enumName = "seq_${seqIdx}_seqDelayValueEnum"
    String numName  = "seq_${seqIdx}_seqDelayValueNumber"
    String strName  = "seq_${seqIdx}_seqDelayValueString"
    String opName   = "seq_${seqIdx}_seqDelayNumOp"

    boolean blockMatch = false

    if (vals && vals.size() > 0) {
        String target = settings[enumName]?.toString()?.trim()
        if (!target) return false
        blockMatch = dev.currentValue(attr)?.toString()?.trim() == target
        if (eq == "!=") blockMatch = !blockMatch
        return blockMatch
    }

    if (dt in ["NUMBER", "INTEGER", "FLOAT", "DOUBLE", "BIGDECIMAL", "LONG"]) {
        def rawNum = settings[numName]
        if (rawNum == null) return false
        BigDecimal targetNum
        try { targetNum = rawNum.toString() as BigDecimal } catch (Exception e) { return false }

        String operator = settings[opName]?.toString() ?: "="
        def cv = dev.currentValue(attr)
        if (cv == null) return false
        BigDecimal cn
        try { cn = cv.toString() as BigDecimal } catch (Exception e) { return false }

        switch (operator) {
            case "=":  blockMatch = cn == targetNum; break
            case ">":  blockMatch = cn > targetNum; break
            case "<":  blockMatch = cn < targetNum; break
            case "<=": blockMatch = cn <= targetNum; break
            case ">=": blockMatch = cn >= targetNum; break
        }
        if (eq == "!=") blockMatch = !blockMatch
        return blockMatch
    }

    String target = settings[strName]?.toString()?.trim()
    if (!target) return false
    blockMatch = dev.currentValue(attr)?.toString()?.trim() == target
    if (eq == "!=") blockMatch = !blockMatch
    return blockMatch
}

void childDeviceButtonHandler(Integer buttonNumber) {
    logDebug "BUTTON PUSHED: ${buttonNumber}"
    if (buttonNumber == 0) {
        if (state.activeSequences && state.activeSequences.size() > 0) {
            log.warn "Emergency Abort triggered for all active sequences."
            List<String> seqKeys = state.activeSequences.keySet().toList()
            for (String seqKey in seqKeys) {
                def seqState = state.activeSequences[seqKey]
                if (seqState && seqState.running) {
                    int currentStep = seqState.step as int
                    List roomsList = seqState.rooms as List
                    if (currentStep > 0 && currentStep <= roomsList.size()) {
                        String cRoomCode = roomsList[currentStep - 1]
                        Map rData = state.globalRoomMatrix ? state.globalRoomMatrix[cRoomCode.toString()] : null
                        if (rData) { settings?."vac${rData.vacuumIndex}"?.appDock() }
                    }
                }
                terminateSequence(seqKey.toInteger(), false)
            }
        }
        return
    }

    if (buttonNumber < 100) return

    Map targetRoomData = state.globalRoomMatrix ? state.globalRoomMatrix[buttonNumber.toString()] : null
    if (!targetRoomData) return
    
    boolean vacBusy = false
    if (state.activeSequences) {
        List<String> seqKeys = state.activeSequences.keySet().toList()
        for (String seqKey in seqKeys) {
            def seqState = state.activeSequences[seqKey]
            if (seqState && seqState.running) {
                int currentStep = seqState.step as int
                List roomsList = seqState.rooms as List
                if (currentStep > 0 && currentStep <= roomsList.size()) {
                    String cRoomCode = roomsList[currentStep - 1]
                    Map rData = state.globalRoomMatrix[cRoomCode.toString()]
                    if (rData?.vacuumIndex == targetRoomData.vacuumIndex) {
                        vacBusy = true
                        break
                    }
                }
            }
        }
    }
    
    if (vacBusy) {
        log.warn "Command blocked: Vacuum ${targetRoomData.vacLabel} is currently running a sequence."
        return
    }

    def targetVacDevice = settings?."vac${targetRoomData.vacuumIndex}"
    if (!targetVacDevice) return

    String chosenMopLevel = settings["instantMop_${buttonNumber}"] ?: "DEFAULT"
    logDebug "BUTTON: Instant Clean ${targetRoomData.roomName} on ${targetRoomData.vacLabel}"
    updateDashboardHtml("Instant Clean: ${targetRoomData.roomName} (Mop: ${chosenMopLevel})")
    executeInstantAnnouncement(buttonNumber, targetRoomData.roomName, targetRoomData.vacLabel)

    try {
        targetVacDevice.appRoomClean(targetRoomData.nativeRoomId.toString(), chosenMopLevel)
    } catch (Exception e) {}
}

void executeStepAnnouncement(int seqIdx, String roomCode, String roomName, String vacName) {
    def spk = firstOrNull(settings["seq_${seqIdx}_speechDevice_${roomCode}"])
    if (!spk) {
        logDebug "SPEECH: No speaker configured for sequence ${seqIdx}, room ${roomCode}."
        return
    }
    
    String safeRoomName = roomName ? roomName.toString() : "the room"
    String safeVacName = vacName ? vacName.toString() : "The vacuum"
    String msg = "${safeVacName} is cleaning the ${safeRoomName}"
    String command = (settings.echoCommand ?: "speak").trim().toLowerCase()

    logDebug "SPEECH: Announcing to ${spk.displayName}: '${msg}' (command: ${command})"
    try { 
        if (command == "speakmessage") spk.speakMessage(msg) else spk.speak(msg)
    } catch (Exception e) {
        log.error "Speech failed on ${spk.displayName}: ${e.message}"
    }
}

void executeInstantAnnouncement(Integer buttonNumber, String roomName, String vacName) {
    def spk = firstOrNull(settings["instantSpeech_${buttonNumber}"])
    if (!spk) {
        logDebug "SPEECH: No instant-clean speaker configured for button ${buttonNumber}."
        return
    }
    
    String safeRoomName = roomName ? roomName.toString() : "the room"
    String safeVacName = vacName ? vacName.toString() : "The vacuum"
    String msg = "${safeVacName} is cleaning the ${safeRoomName}"
    String command = (settings.echoCommand ?: "speak").trim().toLowerCase()

    logDebug "SPEECH: Instant clean announcing to ${spk.displayName}: '${msg}' (command: ${command})"
    try { 
        if (command == "speakmessage") spk.speakMessage(msg) else spk.speak(msg)
    } catch (Exception e) {
        log.error "Speech failed on ${spk.displayName}: ${e.message}"
    }
}

void updateDashboardHtml(String statusMessage) {
    def childDevice = getChildDevice("rr_scheduler_child_" + app.id)
    if (!childDevice) return
    String formattedHtml = "<div style='font-size:14px; font-family:sans-serif; padding:5px;'><div style='color:#1a77c9; font-weight:bold; margin-bottom:4px;'>Status:</div><div style='background:#f5f5f5; border-left:3px solid #1a77c9; padding:6px; border-radius:3px;'>${statusMessage}</div></div>"
    childDevice.sendEvent(name: "schedulerStatus", value: formattedHtml)
    logDebug "DASHBOARD STATUS UPDATE: ${statusMessage}"
}

void terminateSequence(Integer seqIdx, Boolean regularCompletion = true) {
    logDebug "TERMINATE: seq=${seqIdx}, regular=${regularCompletion}"
    String seqKey = seqIdx.toString()
    if (state.activeSequences?.containsKey(seqKey)) {
        state.activeSequences.remove(seqKey)
    }
    updateVisualLights()
    
    if (regularCompletion) { updateDashboardHtml("${getSeqName(seqIdx)} completed.") }
    else { updateDashboardHtml("${getSeqName(seqIdx)} aborted.") }
}

void compileAndPublishSequenceList() {
    def childDevice = getChildDevice("rr_scheduler_child_" + app.id)
    if (!childDevice) return
    
    int totalSeqs = (settings["numSequences"] ?: 1) as int
    Map vacLists = [1:[], 2:[], 3:[], 4:[], 5:[]]

    for (int s = 1; s <= totalSeqs; s++) {
        if (settings["seqEnabled_${s}"] == false) continue // Skip disabled sequences in the list

        String seqName = getSeqName(s)
        List roomsList = normalizeToList(settings["seqRooms_${s}"])
        
        List roomObjs = []
        roomsList.eachWithIndex { code, idx ->
            def orderVal = settings["seq_${s}_order_${code}"]
            int orderNum = idx + 1
            if (orderVal != null) {
                try { orderNum = orderVal.toString().toInteger() } catch (Exception e) {}
            }
            roomObjs << [ code: code, order: orderNum, origIdx: idx ]
        }
        roomObjs.sort { a, b -> 
            if (a.order == b.order) return a.origIdx <=> b.origIdx
            return a.order <=> b.order 
        }
        List sortedRoomCodes = roomObjs.collect { it.code }

        def rawTime = settings["seqTime_${s}"]
        List daysList = normalizeToList(settings["seqDays_${s}"])

        String scheduleText = "Not Scheduled"
        if (rawTime) {
            String cleanTime = "??:??"
            Date t = parseTimeInput(rawTime)
            if (t) cleanTime = new SimpleDateFormat("HH:mm").format(t)
            if (settings["seqUsePeriodic_${s}"] == true) {
                int interval = (settings["seqPeriodicDays_${s}"] ?: 2) as int
                scheduleText = "Every ${interval} days @ ${cleanTime}"
            } else if (daysList.size() > 0) {
                String daysJoined = daysList.collect { it.toString().substring(0, 3) }.join(", ")
                scheduleText = daysJoined + " @ " + cleanTime
            }
        }

        Map vacRooms = [:]
        sortedRoomCodes.each { roomCode ->
            Map roomMeta = state.globalRoomMatrix ? state.globalRoomMatrix[roomCode.toString()] : null
            if (roomMeta) {
                int vIdx = roomMeta.vacuumIndex
                if (vIdx > 5) vIdx = 5 // Safeguard
                if (!vacRooms[vIdx]) vacRooms[vIdx] = []
                vacRooms[vIdx] << roomCode
            }
        }

        vacRooms.each { vIdx, rooms ->
            String vacLabel = state.globalRoomMatrix[rooms[0].toString()]?.vacLabel ?: "Vac ${vIdx}"
            String block = "<b>${seqName} (${vacLabel})</b><br>Scheduled: ${scheduleText}<br>"
            rooms.each { roomCode ->
                Map roomMeta = state.globalRoomMatrix[roomCode.toString()]
                boolean delayOn = settings["seq_${s}_delayEnable_${roomCode}"] as Boolean
                String delayStr = ""
                if (delayOn) {
                    int count = (settings["seq_${s}_delayCount_${roomCode}"] ?: 1) as int
                    List<String> devNames = []
                    for (int i = 1; i <= count; i++) {
                        String suffix = (i == 1) ? "" : "_${i}"
                        def dev = firstOrNull(settings["seq_${s}_delayDevice${suffix}_${roomCode}"])
                        if (dev) devNames << dev.displayName
                    }
                    if (devNames.size() > 0) {
                        delayStr = " [delay: ${devNames.join(', ')}]"
                    }
                }
                block += "- ${roomMeta.roomName}${delayStr}<br>"
            }
            block += "<br>"
            vacLists[vIdx] << block
        }
    }

    if (state.sentVacLists == null) state.sentVacLists = [:]
    for (int i = 1; i <= 5; i++) {
        if (vacLists[i].size() > 0) {
            childDevice.sendEvent(name: "sequenceListVac${i}", value: vacLists[i].join(""))
            state.sentVacLists[i] = true
        } else if (state.sentVacLists[i]) {
            childDevice.sendEvent(name: "sequenceListVac${i}", value: "")
            state.sentVacLists[i] = false
        }
    }
}