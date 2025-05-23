function doGet(request) {

  var ss = SpreadsheetApp.openById("");
  var xtab = request.parameter["sheet"];             
  var tab = xtab.replace("%20"," ");
  console.log(tab);
  sheet = ss.getSheetByName(tab);
  var values = sheet.getDataRange().getValues();
  var pastDate = new Date();
  var deleteDays = request.parameter["days"];
  pastDate.setDate(pastDate.getDate()-deleteDays);

  var numRows = 0;
  for( var row = values.length -1; row >= 1; --row ) { 
    var cellDate = values[row][0];
    if (cellDate < pastDate) {
      sheet.deleteRow(parseInt(row)+1);
      numRows++;
    } 
  }  
  // insert blank rows to replace those deleted
  //sheet.insertRowsAfter(sheet.getMaxRows(), numRows);
};