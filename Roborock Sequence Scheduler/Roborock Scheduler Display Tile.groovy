/**
 *  Roborock Scheduler Display Tile (Child Device Component)
 *
 *  v. 2.41 - 11/08/26 - Added historyList attribute + addHistoryEvent command + historySize preference
 */
metadata {
    definition (name: "Roborock Scheduler Display Tile", namespace: "Hubitat", author: "Chris Burgess") {
        capability "PushableButton"
        capability "Actuator"

        attribute "schedulerStatus", "string"
        attribute "roomKey", "string"
        attribute "sequenceListVac1", "string"
        attribute "sequenceListVac2", "string"
        attribute "sequenceListVac3", "string"
        attribute "sequenceListVac4", "string"
        attribute "sequenceListVac5", "string"
        attribute "dashboardSettings", "string"
        attribute "historyList", "string"

        attribute "binPercent1", "string"
        attribute "binPercent2", "string"
        attribute "binPercent3", "string"
        attribute "binPercent4", "string"
        attribute "binPercent5", "string"
        attribute "binPercent6", "string"
        attribute "binPercent7", "string"
        attribute "binPercent8", "string"
        attribute "binPercent9", "string"
        attribute "binPercent10", "string"
    }
}

preferences {
    input (name: "historySize", type: "number",
           title: "History events to display (5-20)",
           defaultValue: 10, range: "5..20", required: true)
}

def installed() {
    log.info "Roborock Display Tile Component initialized."
    sendEvent(name: "numberOfButtons", value: 1000)
    setAppFrame()
    if (!state.historyList) state.historyList = []
    sendEvent(name: "historyList", value: renderHistoryHtml(state.historyList))
}

def updated() {
    setAppFrame()
    // Re-trim history in case size preference shrank
    if (state.historyList) {
        int maxSize = getHistoryMaxSize()
        List trimmed = state.historyList.take(maxSize)
        state.historyList = trimmed
        sendEvent(name: "historyList", value: renderHistoryHtml(trimmed))
    } else {
        sendEvent(name: "historyList", value: renderHistoryHtml([]))
    }
}

def push(buttonNumber) {
    Integer btnNum = 0
    try {
        btnNum = buttonNumber != null ? buttonNumber.toString().toInteger() : 0
    } catch (Exception e) {
        log.warn "Invalid button number received: ${buttonNumber}"
        return
    }

    sendEvent(name: "pushed", value: btnNum, isStateChange: true)

    if (parent) {
        parent.childDeviceButtonHandler(btnNum)
    } else {
        log.error "No parent app found for Roborock Scheduler Display Tile"
    }
}

def setNumberOfButtons(Integer num) {
    if (num != null && num > 0) {
        sendEvent(name: "numberOfButtons", value: num)
    }
}

// ---------------------------------------------------------------------------
// History Management
// ---------------------------------------------------------------------------

def addHistoryEvent(String roomName, String status) {
    if (!roomName) return
    if (!status) return
    String s = status.toString().trim().toLowerCase()
    if (!(s in ["started", "completed", "canceled"])) {
        log.warn "addHistoryEvent: ignoring unsupported status '${status}'"
        return
    }

    if (state.historyList == null) state.historyList = []

    int maxSize = getHistoryMaxSize()
    String timestamp = new Date().format("MMM d, h:mm a")
    String entry = "${timestamp} - ${roomName} ${s}"

    List newList = [entry] + (state.historyList as List)
    if (newList.size() > maxSize) {
        newList = newList.take(maxSize)
    }
    state.historyList = newList

    sendEvent(name: "historyList", value: renderHistoryHtml(newList))
}

def clearHistory() {
    state.historyList = []
    sendEvent(name: "historyList", value: renderHistoryHtml([]))
}

int getHistoryMaxSize() {
    int size = 10
    try {
        def v = device.latestSetting("historySize")
        if (v != null) {
            size = v.toString().toInteger()
        }
    } catch (Exception e) {
        size = 10
    }
    if (size < 5) size = 5
    if (size > 20) size = 20
    return size
}

String renderHistoryHtml(List entries) {
    if (!entries || entries.size() == 0) {
        return "<div style='font-size:12px; font-family:sans-serif; color:#888; padding:4px;'>No history yet.</div>"
    }
    StringBuilder sb = new StringBuilder()
    sb.append("<div style='font-size:12px; font-family:sans-serif; padding:2px; line-height:1.6;'>")
    entries.eachWithIndex { e, idx ->
        if (idx > 0) sb.append("<br>")
        String entryText = (e?.toString() ?: '')
        entryText = entryText.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
        sb.append(entryText)
    }
    sb.append("</div>")
    return sb.toString()
}

// ---------------------------------------------------------------------------
// App Settings Frame
// ---------------------------------------------------------------------------

def setAppFrame() {
    def hub = location.hubs[0]
    def hubIP = hub?.getDataValue("localIP")
    def appId = device.getParentAppId() ?: parent?.id
    if (hubIP && appId) {
        def url = "http://${hubIP}/installedapp/configure/${appId}/mainPage"
        sendEvent(name: "dashboardSettings", value: getAppButton(url))
    }
}

def getAppButton(url) {
    def mId = "m${device.id}"
    def fId = "f${device.id}"
    
    String s1 = "height:100%;width:100%;background:#e0e0e0;border:2px solid #999;border-radius:4px;color:#333;font-weight:bold;font-size:16px;cursor:pointer;"
    String s2 = "display:none;position:fixed;top:5%;left:5%;width:90%;height:90%;background:rgba(0,0,0,0.8);z-index:99;"
    String s3 = "position:absolute;top:5px;right:5px;font-size:16px;color:#fff;background:#000;border:1px solid #fff;cursor:pointer;padding:2px 6px;z-index:100;"
    
    return "<button style='${s1}' onclick='document.getElementById(\"${mId}\").style.display=\"block\";document.getElementById(\"${fId}\").src=\"${url}\";'>Settings</button>" +
           "<div id='${mId}' style='${s2}'>" +
           "<button style='${s3}' onclick='document.getElementById(\"${mId}\").style.display=\"none\";'>X</button>" +
           "<iframe id='${fId}' style='width:100%;height:100%;border:none;'></iframe>" +
           "</div>"
}