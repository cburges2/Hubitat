/**
 *  Roborock Scheduler Display Tile (Child Device Component)
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

def installed() {
    log.info "Roborock Display Tile Component initialized."
    sendEvent(name: "numberOfButtons", value: 1000)
    setAppFrame()
}

def updated() {
    setAppFrame()
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

// Bring up the app settings page in an iFrame with dashboard button
def setAppFrame() {
    def hub = location.hubs[0]
    def hubIP = hub?.getDataValue("localIP")
    def appId = device.getParentAppId() ?: parent?.id
    if (hubIP && appId) {
        def url = "http://${hubIP}/installedapp/configure/${appId}/mainPage"
        sendEvent(name: "dashboardSettings", value: getAppButton(url))
    }
}

// create the button attribute for the dashboard to launch a url in an iFrame
def getAppButton(url) {
    def mId = "m${device.id}"
    def fId = "f${device.id}"
    
    // Minified CSS to stay under the 1024 character limit
    String s1 = "height:100%;width:100%;background:#e0e0e0;border:2px solid #999;border-radius:4px;color:#333;font-weight:bold;font-size:16px;cursor:pointer;"
    String s2 = "display:none;position:fixed;top:5%;left:5%;width:90%;height:90%;background:rgba(0,0,0,0.8);z-index:99;"
    String s3 = "position:absolute;top:5px;right:5px;font-size:16px;color:#fff;background:#000;border:1px solid #fff;cursor:pointer;padding:2px 6px;z-index:100;"
    
    return "<button style='${s1}' onclick='document.getElementById(\"${mId}\").style.display=\"block\";document.getElementById(\"${fId}\").src=\"${url}\";'>Settings</button>" +
           "<div id='${mId}' style='${s2}'>" +
           "<button style='${s3}' onclick='document.getElementById(\"${mId}\").style.display=\"none\";'>X</button>" +
           "<iframe id='${fId}' style='width:100%;height:100%;border:none;'></iframe>" +
           "</div>"
}