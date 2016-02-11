'use strict';

angular.module('crisisResponse.selection', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/selection', {
    templateUrl: 'views/selection.html',
    controller: 'SelectionController'
  });
}])

.controller('SelectionController', function($scope, gloVars, $location) {
  $scope.collection = gloVars.collection;

  $scope.setFilter = function(filter){
    gloVars.filter(filter);
    $location.path("/analysis");
  }

  $scope.setFilter({});

});
