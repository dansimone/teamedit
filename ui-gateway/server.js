/**
 * Node JS server-side handling for UI-Gateway frontend.
 */

var express = require('express');
var app = express();
var fs = require("fs");
const uuidV4 = require('uuid/v4');
app.use(express.static('./public'));

LIVEEDIT_HOST = verifyEnvVar("LIVEEDIT_HOST", process.env.LIVEEDIT_HOST);
LIVEEDIT_PORT = verifyEnvVar("LIVEEDIT_PORT", process.env.LIVEEDIT_PORT);

//
// Starts server
//
var server = app.listen(getValueOrDefault(process.env.PORT, 8080), function () {
  var host = server.address().address;
  var port = server.address().port;
  console.log("App listening at http://%s:%s", host, port);
})

// Gets the WebSocket URL of the Backend-Service
app.get('/liveedit_ws_url', function (req, res) {
  res.setHeader('Content-Type', 'application/json');
  res.send(JSON.stringify("ws://" + LIVEEDIT_HOST + ":" + LIVEEDIT_PORT));
})

// Returns a unique UUID
app.get('/uuid', function (req, res) {
  res.setHeader('Content-Type', 'application/json');
  res.send(JSON.stringify(uuidV4()));
})

function getValueOrDefault(value, defaultValue) {
  return value != null ? value : defaultValue;
}

function verifyEnvVar(name, value) {
  if (value == null) {
    throw ('Environment variable ' + name + ' must be set');
  }
  return value;
}