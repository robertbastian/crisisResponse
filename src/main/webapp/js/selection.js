'use strict';

angular.module('crisisResponse.selection', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/selection', {
    templateUrl: 'views/selection.html',
    controller: 'SelectionController'
  });
}])

.controller('SelectionController', function($scope, gloVars) {
  $scope.collection = gloVars.collection
});
