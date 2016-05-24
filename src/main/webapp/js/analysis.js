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
        mapdata.cluster = new MarkerClusterer(map,mapdata.markers,{
          imagePath: 'https://cdn.rawgit.com/googlemaps/js-marker-clusterer/gh-pages/images/m'
        });
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
    $scope.wordcloudColoring = "sentiment";
    $http.post("/api/words",gloVars.filter()).then(function (response){
      function withGradient(entry){
        return {
          text: entry.text,
          weight: entry.weight,
          html: {
            "data-sentiment": JSON.stringify(entry.sentiments),
            "data-trustworthiness": JSON.stringify(entry.trustworthinesses)
          }
        }
      }
      $scope.setGradients = function(arg){
        $('span[data-sentiment]').css("background", function(){
          var limits = $(this).data($scope.wordcloudColoring);
          return "-webkit-linear-gradient(left, #FF6961, " +
            "#FF6961 "+(limits[0]*100)+"%, " +
            "#FDFD96 "+(limits[0]*100)+"%, " +
            "#FDFD96 "+((1-limits[1])*100)+"%, " +
            "#77DD77 "+((1-limits[1])*100)+"%, " +
            "#77DD77 100%)"
        })
      }
      function withLink(entry){
        var stripped = entry.text.replace(/https*\:\/\/(www.)*/g,'');
        return {
          text: stripped.length > 20 ? stripped.substr(0,17) + '...' : stripped,
          weight: entry.weight,
          link: {href: entry.text, target: "_blank"}
        }
      }
      $.each(['hashtags','names','words','urls'],function(i,type){
        var processed = response.data[type].map(type == 'urls' ? withLink : withGradient);
        var object = $("#wordcloud-"+type);
        object.jQCloud(processed,{
          height: object.parent().height(),
          width: object.parent().width(),
          afterCloudRender: $scope.setGradients
        });
      });
    });
    loaded.wordcloud = true;
  }

  function initGraph(){
    $http.post("/api/interactions",gloVars.filter()).then(function (response){

      //noinspection JSJQueryEfficiency
      var width = $("ng-map").parent().parent().parent().width() - 16, height = $("ng-map").parent().parent().parent().height()-16;
      var color = d3.scale.linear()
        .domain([-1, 0, 5, 10])
        .range(["gray", "red", "yellow", "green"]);

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
        .attr("height", height)
        .call(d3.behavior.zoom().on("zoom", function () {
          svg.attr("transform", "translate(" + d3.event.translate + ")" + " scale(" + d3.event.scale + ")")
        }))
        .append("g");

      var link = svg.selectAll(".link")
        .data(force.links())
        .enter().append("line")
          .style("stroke-width",function(d){ return d.value | 1})
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
        .attr("r", function(u) {return 5 + Math.ceil(u.popularity)})
        .style("fill", function(u) { return color(u.competence); });

      node.append("text")
        .attr("x", 12)
        .attr("dy", ".35em")
        .text(function(d) { return d.name; });

      function draw() {
        if (force.alpha() < .03){
          link
            .attr("x1", function(d) { return d.source.x; })
            .attr("y1", function(d) { return d.source.y; })
            .attr("x2", function(d) { return d.target.x; })
            .attr("y2", function(d) { return d.target.y; });

          node
            .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });
        }
      }

    });
    loaded.sng = true;
  }

  //function initGraph(){
  //  $http.post("/api/interactions",gloVars.filter()).then(function (response){
  //
  //    //noinspection JSJQueryEfficiency
  //    var width = $("ng-map").parent().parent().parent().width() - 16, height = $("ng-map").parent().parent().parent().height()-16;
  //
  //
  //    var x = d3.scale.linear()
  //      .domain([0, width])
  //      .range([0, width]);
  //
  //    var y = d3.scale.linear()
  //      .domain([0, height])
  //      .range([height, 0]);
  //
  //    var svg = d3.select("#sng")
  //      .append("svg")
  //        .attr("width", width)
  //        .attr("height", height)
  //      .append("g");
  //
  //    svg.append("rect")
  //      .attr("class", "overlay")
  //      .attr("width", width)
  //      .attr("height", height);
  //
  //    var force = d3.layout.force()
  //      .nodes(response.data.nodes)
  //      .links(response.data.edges)
  //      .size([width, height])
  //      .linkDistance(60)
  //      .charge(-300)
  //      .on("tick", tick);
  //
  //    var link = svg.selectAll(".link")
  //      .data(force.links())
  //      .enter()
  //        .append("line")
  //        .style("stroke-width",function(d){ return d.value | 1})
  //        .attr("class", "link")
  //        .attr("transform", transform);
  //
  //    var nodeColor = d3.scale.linear()
  //      .domain([-1, 0, 5, 10])
  //      .range(["gray", "red", "yellow", "green"]);
  //
  //    var node = svg.selectAll(".node")
  //      .data(force.nodes()).enter()
  //      .append("g")
  //        .attr("class", "node")
  //        .attr("transform", transform);
  //
  //    node
  //      .append("circle")
  //      .attr("r", function(u) {return 5 + Math.ceil(u.popularity)})
  //      .style("fill", function(u) { return nodeColor(u.competence); });
  //
  //    node.append("text")
  //      .attr("x", 12)
  //      .attr("dy", ".35em")
  //      .text(function(d) { return d.name; });
  //
  //    //node.on("dblclick.zoom", function(d) {
  //    //  d3.event.stopPropagation();
  //    //  var dcx = (window.innerWidth/2-d.x*zoom.scale());
  //    //  var dcy = (window.innerHeight/2-d.y*zoom.scale());
  //    //  zoom.translate([dcx,dcy]);
  //    //  g.attr("transform", "translate("+ dcx + "," + dcy  + ")scale(" + zoom.scale() + ")");
  //    //});
  //
  //    node.on("click",function(d){
  //      console.log(d);
  //      $scope.selected.user = $(d3.select(this)[0]).find("text").html().toLowerCase();
  //      $scope.selected.tweets = [];
  //      $http.post("/api/interactions/"+$scope.selected.user,gloVars.filter()).then(function(response){
  //        $scope.selected.tweets = response.data
  //      });
  //    });
  //
  //    function zoom() {
  //      node.attr("transform", transform);
  //      link.attr("transform", transform);
  //    }
  //
  //    function transform(d) {
  //      return "translate(" + x(d[0]) + "," + y(d[1]) + ")";
  //    }
  //
  //    function tick() {
  //      link
  //        .attr("x1", function(d) { return d.source.x; })
  //        .attr("y1", function(d) { return d.source.y; })
  //        .attr("x2", function(d) { return d.target.x; })
  //        .attr("y2", function(d) { return d.target.y; });
  //      node
  //        .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });
  //    }
  //
  //    force.start();
  //  });
  //
  //  loaded.sng = true;
  //}
});
