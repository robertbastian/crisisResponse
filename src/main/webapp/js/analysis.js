'use strict';

angular.module('crisisResponse.analysis', [
  'ngRoute',
  'uiGmapgoogle-maps'
])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/analysis', {
    templateUrl: 'views/analysis.html',
    controller: 'AnalysisController'
  });
}])

.controller('AnalysisController', function($scope,$http,gloVars) {
  console.log("here");
  $scope.mapinfo = { center: { latitude: 45, longitude: -73 }, zoom: 3 };

  $scope.details = null;

  $scope.markerClicked = function(marker, event, info){
    $http.get("/api/tweet/"+info.id).then(function(response){
      var details = response.data;
      $http.get("/api/user/"+details.author).then(function(response){
        details.author = response.data;
        $scope.details = details;
      })
    })
  }

  $scope.tweets = []

  $http.post("/api/locations",{collection: gloVars.collection().id}).then(function(response){
    $scope.tweets = response.data;
    console.log(response)
  })

});
