/**
 * Angular JS controller for UI-Gateway.
 */

'use strict';
var app = angular.module('app', ['ngWebSocket']);
app.controller('AppController', ['$scope', '$http', '$websocket', function ($scope, $http, $websocket) {
  $scope.name = 'world';
  var lastTextArea = "";
  var dataStream = null;
  var uuid = null;
  var backgroundProcessorId = null;

  // Initialize everything
  $scope.init = function () {
    // Create client UUID
    initializeClientUUID();
    // Initiate WebSocket connection to Backend-Service
    initializeWebSocketConnection();
    // Start text processing loop
    backgroundProcessorId = setTimeout(processTextArea, 1000);
    $scope.textArea = "";
  };

  // Initializes the UUID for the current client
  function initializeClientUUID() {
    $http.get('/uuid')
      .success(function (data, status, headers, config) {
        console.log("Setting client UUID to: " + data);
        uuid = data;
      })
      .error(function (data, status, header, config) {
        console.log("Failed to get client UUID, trying again in 5 seconds...");
        setTimeout(initializeClientUUID, 5000);
      });
  }

  // Initializes the WebSocket connection to the Backend-Service
  function initializeWebSocketConnection() {
    $http.get('/liveedit_ws_url')
      .success(function (data, status, headers, config) {
        console.log("Making WebSocket connection to: " + data);
        dataStream = $websocket(data + '/edit');
        dataStream.onMessage(function (message) {
            console.log("Received message: " + message.data);
            var json = JSON.parse(message.data);
            if (json.client_id != uuid) {
              // TODO - process the diff in the text area from the $lastTextArea
              // For now, this is only a really dumb version: just append the change
              // to the current text

              // Process any local differences before merging remote changes
              clearTimeout(backgroundProcessorId);
              processTextArea();

              $scope.textArea += json.change;
              lastTextArea = $scope.textArea;
            }
          })
          .onOpen(function () {
              console.log("WebSocket opened, starting text processing loop...");
            }
          )
          .onClose(function () {
              console.log("WebSocket closed, trying to reopen in 1 second...");
              setTimeout(initializeWebSocketConnection, 1000);
            }
          )
          .onError(function () {
              console.log("WebSocket error");
            }
          );
      })
      .error(function (data, status, header, config) {
        console.log("Failed to get WebSocket URL, trying again in 5 seconds...");
        setTimeout(initializeWebSocketConnection, 5000);
      });
  }

  // Process text changes, send the delta to the Backend-Service, and reevaluate
  // every 5 seconds.
  function processTextArea() {
    // TODO - process the diff in the text area from the $lastTextArea
    // For now, this is only a really dumb version: if the length of the textArea has
    // grown, send the delta
    if (dataStream.readyState == 1 && $scope.textArea.length > lastTextArea.length) {
      var delta = $scope.textArea.substr(lastTextArea.length, $scope.textArea.length);
      console.log("Sending delta: " + delta);
      // TODO - handle the case where the WS send fails
      dataStream.send({"client_id": uuid, "change": delta});
      lastTextArea = $scope.textArea;
    }
    backgroundProcessorId = setTimeout(processTextArea, 5000);
  };
}]);
