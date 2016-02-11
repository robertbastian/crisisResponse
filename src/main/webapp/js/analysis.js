'use strict';

angular.module('crisisResponse.analysis', [
  'ngRoute',
  'uiGmapgoogle-maps',
  'chart.js',
  'angular-jqcloud'
])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/analysis', {
    templateUrl: 'views/analysis.html',
    controller: 'AnalysisController'
  });
}])

.controller('AnalysisController', function($scope,$http,gloVars,$location) {

  if (!gloVars.filter() || !gloVars.collection()){
    $location.path("/selection");
    return;
  }

  $scope.view = "map";

  $scope.map = {
    conf: {center: {latitude: 51.219053, longitude: 4.404418}, zoom: 14},
    selected: {
      tweet: null,
      graph: []
    },
    graphlabels: ["Recency", "Corroboration", "Proximity", "Competence", "Popularity"],
    markerClicked: function (marker, event, info) {
      $http.get("/api/tweet/" + info.id).then(function (response) {
        var details = response.data;
        $http.get("/api/user/" + details.author).then(function (response) {
          details.author = response.data;
          $scope.map.selected.tweet = details;
          $scope.map.selected.graph[0] = [details.recency, details.corroboration, details.proximity, details.author.competence, details.author.popularity];
        })
      })
    }
  };

  var loaded = {
    map: false,
    wordcloud: false,
    sng: false
  };

  $scope.viewChange = function(){
    if (!loaded[$scope.view]){
      if ($scope.view == "map") {
        $http.post("/api/locations", {collection: gloVars.collection().id}).then(function (response) {
          $scope.tweets = response.data;
          loaded.map = true;
        });
      }
      else if ($scope.view == 'wordcloud'){
        $('#wordcloud').jQCloud([{text:"hi",weight:1},{text:"test",weight:2}],{height: $('md-card:first').height(),width: $('md-card:first').width()});
        loaded.wordcloud = true;
      }
    }
  };

  $scope.viewChange();
});
