'use strict';

angular.module('crisisResponse.analysis', [
  'ngRoute',
  'ngMap',
  'chart.js',
  'angular-jqcloud',
  'ngSanitize'
])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/analysis', {
    templateUrl: 'views/analysis.html',
    controller: 'AnalysisController'
  });
}])

.controller('AnalysisController', function($scope,$http,gloVars,$location,NgMap,$sanitize) {

  $scope.view = "map";
  $scope.mapmode = "clustered";
  $scope.selected = {
    tweet: null,
    graph: {
      labels: ["Recency", "Corroboration", "Proximity", "Competence", "Popularity"],
      data: []
    },
    user: null,
    tweets: []
  };

  var loaded = {
    map: false,
    wordcloud: false,
    sng: false
  };

  $scope.viewChange = function(){
    $scope.selected.tweet = null;
    if (!loaded[$scope.view]){
      if ($scope.view == "map") {
        initMap();
      }
      else if ($scope.view == 'wordcloud'){
        initWordclouds();
      }
      else if ($scope.view == 'sng'){
        initGraph();
      }
    }
    setTimeout(resizeMap,100)
  };

  $scope.init = $scope.viewChange();

  var mapdata = {
    locs: [],
    markers: [],
    cluster: null,
    heatmap: null
  };

  function initMap(callback){
    function attachListener(marker,id){
      google.maps.event.addListener(marker, 'click',function() {
        $http.get("/api/tweet/" + id).then(function (response) {
          $scope.selected.tweet = response.data[0];
          $scope.selected.tweet.author = response.data[1];
          $scope.selected.graph.data[0] = [
            $scope.selected.tweet.recency,
            $scope.selected.tweet.corroboration,
            $scope.selected.tweet.proximity,
            $scope.selected.tweet.author.competence,
            $scope.selected.tweet.author.popularity
          ].map(function(e){return Math.round(e*100)/100});
        })
      });
    }

    $http.post("/api/locations", gloVars.filter()).then(function (response) {
      for (var i = 0; i < response.data.length; i++) {
        mapdata.locs.push(new google.maps.LatLng(response.data[i].location[1], response.data[i].location[0]));
        mapdata.markers.push(new google.maps.Marker({position: mapdata.locs[i]}));
        attachListener(mapdata.markers[i], response.data[i].id);
      }
      NgMap.getMap().then(function(map) {
        mapdata.cluster = new MarkerClusterer(map,mapdata.markers,{});
        mapdata.heatmap = new google.maps.visualization.HeatmapLayer({
          data: mapdata.locs,
          map: null,
          radius: 20
        });
      });
    });
    loaded.map = true;
  }

  function resizeMap(){
    NgMap.getMap().then(function(map){
      google.maps.event.trigger(map,'resize')
    })
  }

  $scope.changeMapView = function(){
    NgMap.getMap().then(function(map) {
      mapdata.markers.forEach(function(marker){
        marker.setMap($scope.mapmode === "simple" ? map : null)
      });
      mapdata.cluster.setMap($scope.mapmode === "clustered" ? map : null);
      mapdata.heatmap.setMap($scope.mapmode === "heatmap" ? map : null);
    });
  };

  function initWordclouds(){
    $http.post("/api/wordcounts",gloVars.filter()).then(function (response){
      function withGradient(entry){
        return {
          text: entry.text,
          weight: entry.weight,
          html: {
            "style-sentiment": gradient(entry.sentiments[0],entry.sentiments[1]),
            "style": gradient(entry.trustworthinesses[0],entry.trustworthinesses[1])
          }
        }
      }
      function gradient(red, green){
        return "background: -webkit-linear-gradient(left, #FF6961, " +
          "#FF6961 "+(red*100)+"%, " +
          "#FDFD96 "+(red*100)+"%, " +
          "#FDFD96 "+((1-green)*100)+"%, " +
          "#77DD77 "+((1-green)*100)+"%, " +
          "#77DD77 100%);"
      }
      function withLink(entry){
        var stripped = entry.text.replace(/https*\:\/\/(www.)*/g,'');
        return {
          text: stripped.length > 20 ? stripped.substr(0,17) + '...' : stripped,
          weight: entry.weight,
          link: entry.text
        }
      }

      $.each(['hashtags','names','words','urls'],function(i,type){
        var processed = response.data[type].map(type == 'urls' ? withLink : withGradient);
        var object = $("#wordcloud-"+type);
        object.jQCloud(processed,{height: object.parent().height(),width: object.parent().width()});
      });
      //$('#wordcloud-words').find('span[sentiment]').each(function(i,o) {
      //  $(o).css("color", "hsl(" + (30 + 20 * $(o).attr("sentiment")) + ", 100%, 25%)")
      //})
    });
    loaded.wordcloud = true;
  }

  function initGraph(){
    $http.post("/api/interactions",gloVars.filter()).then(function (response){

      //noinspection JSJQueryEfficiency
      var width = $("ng-map").parent().parent().parent().width() - 16, height = $("ng-map").parent().parent().parent().height()-16;
      var color = d3.scale.category20();

      var force = d3.layout.force()
        .nodes(response.data.nodes)
        .links(response.data.edges)
        .size([width, height])
        .linkDistance(60)
        .charge(-300)
        .on("tick", draw)
        .start();

      var svg = d3.select("#sng").append("svg")
        .attr("width", width)
        .attr("height", height);

      var link = svg.selectAll(".link")
        .data(force.links())
        .enter().append("line")
        .attr("class", "link");

      var node = svg.selectAll(".node")
        .data(force.nodes())
        .enter().append("g")
        .on("click",function(){
          $scope.selected.user = $(d3.select(this)[0]).find("text").html().toLowerCase();
          $scope.selected.tweets = [];
          $http.post("/api/interactions/"+$scope.selected.user,gloVars.filter()).then(function(response){
            $scope.selected.tweets = response.data
          });
        })
        .attr("class", "node")
        .call(force.drag);

      node.append("circle")
        .attr("r", 8);

      node.append("text")
        .attr("x", 12)
        .attr("dy", ".35em")
        .text(function(d) { return d.name; });

      function draw() {
        link
          .attr("x1", function(d) { return d.source.x; })
          .attr("y1", function(d) { return d.source.y; })
          .attr("x2", function(d) { return d.target.x; })
          .attr("y2", function(d) { return d.target.y; });

        node
          .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });
      }

    });
    loaded.sng = true;
  }
});
