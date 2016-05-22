'use strict';

angular.module('crisisResponse.collection', ['ngRoute','ngFileUpload','google.places','ngMaterialDatePicker'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/collection', {
    templateUrl: 'views/collection.html',
    controller: 'CollectionController'
  });
}])

.controller('CollectionController', function($scope,$http,$mdDialog,$location,Upload,gloVars, $timeout) {

  /** EVENT HANDLING **/

  $scope.events = [];
  function loadEvents() {
    return $http.get("/api/events").then(function(response) {
      $scope.events = response.data;
    })
  }

  loadEvents();

  function updateEvents(){
    loadEvents().then(function(){
      if($scope.events.filter(function(e){return e.status < 2}).length > 0)
        $timeout(updateEvents,5000)
    })
  }

  $scope.selectEvent = function(event){
    gloVars.selectEvent(event);
    $location.path("/selection");
  };

  $scope.active = function(event){
    return event.id == gloVars.filter().event;
  };


  $scope.delete = function(e,event) {
    var dialog = $mdDialog.confirm()
      .title('Do you really want to delete this event?')
      .ariaLabel("Confirm deletion")
      .targetEvent(e)
      .ok("Delete")
      .cancel("Cancel");
    $mdDialog.show(dialog).then(function() {
      $http.delete("/api/event/"+event.id).then(function(){
        loadEvents();
      })
    });
  };

  /** FILE IMPORT **/

  $scope.file = {
    name: null,
    time: null,
    location: null,
    file: null,
    ready: function(){
      return $scope.file.name != null && $scope.file.name != "" &&
        $scope.file.file != null &&
        $scope.file.time != null && $scope.fiile.time != "" &&
        $scope.file.location && typeof $scope.file.location === 'object' && "geometry" in $scope.file.location
    }
  };
  $scope.upload = function () {
    $http.post("/api/event/upload/",{
        name: $scope.file.name,
        lat: $scope.file.location.geometry.location.lat(),
        lon: $scope.file.location.geometry.location.lon(),
        time: moment($scope.file.time).unix(),
        query: $scope.file.file
    }).then(function success(response) {
      $scope.file.name = null;
      $scope.file.time = null;
      $scope.file.location = null;
      $scope.file = null;
      updateEvents();
    }, console.log);
  };

  /** STREAM IMPORT **/

  $scope.stream = {
    name: null,
    time: null,
    location: null,
    active: false,
    count: 0,
    ready: function(){
      return $scope.stream.name != null && $scope.stream.name != "" &&
        $scope.streamtags.length > 0 &&
        $scope.stream.time != null && $scope.stream.time != "" &&
        $scope.stream.location && typeof $scope.stream.location === 'object' && "geometry" in $scope.stream.location
    }
  };
  $scope.streamtags = [];

  $scope.startStream = function(){
    $http.post('/api/event/stream/start',{
      name: $scope.stream.name,
      lat: $scope.stream.location.geometry.location.lat(),
      lon: $scope.stream.location.geometry.location.lng(),
      time: moment($scope.stream.time).unix(),
      query: $scope.streamtags.join(",")
    }).then(function(){
      $scope.stream.active = true;
      setTimeout(loadEvents,500);
      setTimeout(loadCount,500);
    })
  };

  $scope.stopStream = function(){
    $http.post("/api/event/stream/end").then(function(response){
      $scope.stream.active = false;
      $scope.stream.name = null;
      $scope.stream.time = null;
      $scope.stream.location = null;
      $scope.streamtags = [];
      setTimeout(updateEvents, 500);
    })
  };

  function loadCount(){
    $http.get('/api/event/stream/status').then(function(response){
      $scope.stream.count = response.data.count;
      if ($scope.stream.active)
        setTimeout(loadCount,500)
    })
  }

  /** TRENDS **/

  $scope.trends = {
    load: function(woeid){
      if (woeid != null)
        $http.get('/api/trends/'+woeid).then(function(response){
          $scope.trends.values = response.data;
        })
    },
    locations: [{name: "Worldwide", woeid: 1}],
    filtered: function(){
      if ($scope.trends.searchText && $scope.trends.searchText.length > 2)
        return $scope.trends.locations.filter(function(e){
          return e.name.toLowerCase().startsWith($scope.trends.searchText.toLowerCase())}
        );
      else
        return $scope.trends.locations;
    },
    values: []
  };

  $http.get('/api/trends/options').then(function(response){
    $scope.trends.locations = response.data
  });
  $scope.trends.load(1);
});
