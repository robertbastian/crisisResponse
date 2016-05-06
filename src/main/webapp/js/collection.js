'use strict';

angular.module('crisisResponse.collection', ['ngRoute','ngFileUpload','google.places','ngMaterialDatePicker'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/collection', {
    templateUrl: 'views/collection.html',
    controller: 'CollectionController'
  });
}])

.controller('CollectionController', function($scope,$http,$mdDialog,$location,Upload,gloVars, $timeout) {
  $scope.collections = [];

  loadCollections();

  function loadCollections() {
    return $http.get("/api/collection/").then(function(response) {
      $scope.collections = response.data;
    })
  }

  function deleteCollection(collection){
    $http.delete("/api/collection/"+collection.id).then(function(){
      loadCollections();
    })
  }

  $scope.selectCollection = function(collection){
    gloVars.setCollection(collection);
    $location.path("/selection");
  };

  $scope.active = function(collection){
    return collection.id == gloVars.filter().collection;
  };


  $scope.delete = function(ev,collection) {
    var confirm = $mdDialog.confirm()
      .title('Do you really want to delete this collection?')
      .textContent("The collection "+collection.name+" contains "+collection.size+" tweets.")
      .ariaLabel("Confirm deletion")
      .targetEvent(ev)
      .ok("Delete")
      .cancel("Cancel");
    $mdDialog.show(confirm).then(function() {
      deleteCollection(collection)
    });
  };
  
  $scope.upload = function () {
    Upload.upload({
      url: "/api/collection/"+$scope.import_.name,
      data: {file: $scope.import_.file},
      headers : {
        'Content-Type': undefined
      }
    }).then(function success(response) {
      $scope.import_ = null;

      updateCollections();

    }, function error(e){
      alert(e);
    });
  };

  function updateCollections(){
    loadCollections().then(function(){
      if($scope.collections.filter(function(e,i){return e.status < 2}).length > 0)
        $timeout(updateCollections,1000)
    })
  }

  $scope.trends = {
    load: function(woeid){
      if (woeid != null)
        $http.get('/api/trending/'+woeid).then(function(response){
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

  $http.get('/api/trending/options').then(function(response){
    $scope.trends.locations = response.data
  });
  $scope.trends.load(1);


  $scope.streamtags = [];
  $scope.stream = {
    active: false,
    name: null,
    time: null,
    count: 0,
    location: null,
    start: function() {
      var config = {
        name: $scope.stream.name,
        query: $scope.streamtags,
        lat: $scope.stream.location.geometry.location.lat(),
        lon: $scope.stream.location.geometry.location.lng(),
        time: moment($scope.stream.time).unix()
      }
      $http.post('/api/stream/start',config).then(function(){
        $scope.stream.active = true;
        setTimeout(loadCollections,500);
        setTimeout($scope.stream.loadCount,500);
      })
    },
    stop: function(){
      $http.post("/api/stream/end").then(function(response){
        $scope.stream.active = false;
        $scope.stream.name = null;
        $scope.stream.time = null;
        $scope.stream.location = null;
        $scope.streamtags = [];
        setTimeout(updateCollections, 500);
      })
    },
    ready: function(){
      return $scope.stream.name != null && $scope.stream.name != "" &&
             $scope.streamtags.length > 0 &&
             $scope.stream.time != null && $scope.stream.time != "" &&
             $scope.stream.location && typeof $scope.stream.location === 'object' && "geometry" in $scope.stream.location
    },
    loadCount: function(){
      $http.get('/api/stream/status').then(function(response){
        $scope.stream.count = response.data.count;
        if ($scope.stream.active)
          setTimeout($scope.stream.loadCount,500)
      })
    }
  };
});
