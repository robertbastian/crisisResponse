'use strict';

// Declare app level module which depends on views, and components
angular.module('crisisResponse', [
  'ngRoute',
  'crisisResponse.collection',
  'crisisResponse.selection',
  'crisisResponse.analysis',
  'ngMaterial',
  'ngMdIcons'
])

.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
  $routeProvider.when("/",{
    redirectTo: "/collection"
  })

  $locationProvider.html5Mode(false);
}])

.config(function($mdThemingProvider) {
  $mdThemingProvider.theme('default')
    .primaryPalette('amber')
})

.controller('AppController', function($scope,$location){
  $scope.tabs = [
    {title:"Collection",url:"/collection"},
    {title:"Selection", url:"/selection"},
    {title:"Analysis", url:"/analysis"}
  ];

  $scope.switchTab = function(tab) {
    $location.path(tab.url)
  }

  $scope.isActive = function(tab){
    return $location.path() == tab.url
  }

  var originatorEv;
  $scope.openMenu = function($mdOpenMenu, ev) {
    originatorEv = ev;
    $mdOpenMenu(ev);
  };

});


