'use strict';

// Declare app level module which depends on views, and components
angular.module('crisisResponse', [
  'ngRoute',
  'crisisResponse.collection',
  'crisisResponse.selection',
  'crisisResponse.analysis'
]).
config(['$routeProvider', function($routeProvider) {
  $routeProvider.when("/",{
    redirectTo: "/collection"
  })
}])
.controller('AppController', function($scope){
  $scope.tab = 0;
  $scope.changeTab = function changeTab(tab){
    $scope.tab = tab;
  }
});
