'use strict';

angular.module('crisisResponse.collection', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/collection', {
    templateUrl: 'views/collection.html',
    controller: 'CollectionController'
  });
}])

.controller('CollectionController', function($scope,$http) {
  $scope.collections = [];
  $scope.selectedCollection = null;
  $scope.collections = $http.get("/api/collection/")
  .then(function success(response){
    $scope.collections = response.data;
    for (var i = 0; i < $scope.collections.length && $scope.selectedCollection == null; i++){
      if ($scope.collections[i].ready)
        $scope.selectedCollection = $scope.collections[i]
    }
  })
});
