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
    .primaryPalette('lime')
})

.controller('AppController', function($scope,$location,gloVars){
  $scope.tabs = [
    {title:"Collection",url:"/collection", disabled: function(){ return false}},
    {title:"Selection", url:"/selection", disabled: function(){ return gloVars.collection() == null}},
    {title:"Analysis", url:"/analysis", disabled: function(){ return gloVars.filter() == null}}
  ];

  $scope.switchTab = function(tab) {
    $location.path(tab.url)
  }

  $scope.isActive = function(tab){
    return $location.path() == tab.url
  }

  $scope.openMenu = function($mdOpenMenu, ev) {
    $mdOpenMenu(ev);
  };
})

.service('gloVars', function () {
  var collection = null;
  var filter = null;
  return {
    collection: function(value) {
      return arguments.length ? (collection = value) : collection;
    },
    filter: function(value){
      return arguments.length ? (filter = value) : filter;
    }
  };
});


