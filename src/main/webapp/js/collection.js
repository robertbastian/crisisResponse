'use strict';

angular.module('crisisResponse.collection', ['ngRoute','ngFileUpload'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/collection', {
    templateUrl: 'views/collection.html',
    controller: 'CollectionController'
  });
}])

.controller('CollectionController', function($scope,$http,$mdDialog,$location,Upload,gloVars) {
  $scope.collections = [];

  loadCollections();

  function loadCollections() {
    $http.get("/api/collection/").then(function(response) {
      $scope.collections = response.data;
    })
  }

  function deleteCollection(collection){
    $http.delete("/api/collection/"+collection.id).then(function(){
      loadCollections();
    })
  }

  $scope.selectCollection = function(collection){
    gloVars.collection(collection);
    $location.path("/selection");
  }

  $scope.active = function(collection){
    return collection.id == gloVars.collection().id;
  }


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
    console.log($scope.import_.file)
    Upload.upload({
      url: "/api/collection/"+$scope.import_.name,
      data: {file: $scope.import_.file},
      headers : {
        'Content-Type': undefined
      }
    }).success(function (response) {
      loadCollections()
    });
  }

});
