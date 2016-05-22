'use strict';

angular.module('crisisResponse.selection', ['ngRoute'])

  .config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/selection', {
      templateUrl: 'views/selection.html',
      controller: 'SelectionController'
    });
  }])

  .controller('SelectionController', function($scope, gloVars, $location,$http) {

    $scope.goAnalyze = function(){
      $location.path('/analysis')
    };

    var HISTOGRAMS = ["sentiment","corroboration","competence","popularity","time"];

    $scope.filter = gloVars.filter();
    $scope.count = {selected: "?", all: "?"};

    $scope.change = function(){
      $http.post("/api/count",$scope.filter).success(function(response){
        $scope.count = response;
      });
    };
    $scope.change();

    HISTOGRAMS.forEach(function(selector){
      $http.post("/api/histogram/"+selector,$scope.filter).then(function(response){
        makeHistogram(selector,response.data.buckets, response.data.min, response.data.max)
      },console.log);
    });

    function makeHistogram(selector,data,minX,maxX) {
      var obj = $('#'+selector);

      obj.empty();

      var barPadding = 1,
        w = obj.width(),
        h = obj.height(),
        max = Math.max(...data);

      var svg = d3.select('#'+selector)
        .append("svg")
        .attr("width", w)
        .attr("height", h);

      var x = d3.scale.linear().domain([Math.floor(minX),Math.ceil(maxX)]).range([20,w-20]);

      var xAxis = d3.svg.axis()
        .orient("bottom")
        .scale(x)
        .ticks(2)
        .tickFormat(d3.format("d"));

      svg.selectAll("rect")
        .data(data)
        .enter()
        .append("rect")
        .attr("x", function (d, i) { return 20 +  i * ((w-40) / data.length); })
        .attr("y", function (d) { return h - (d / max * h); })
        .attr("width", (w-40) / data.length - barPadding)
        .attr("height", function (d) { return d / max * h; })
        .attr("fill", ["cyan","green","orange","gray"][Math.floor(Math.random()*4)]);

      var brush = d3.svg.brush().x(x);
      if ($scope.filter[selector] != null)
        brush.extent($scope.filter[selector]);

      brush
        .on("brush", function(){
          var extent = brush.extent().map(Math.round);
          d3.select(this).call(extent[0] == extent[1] ? brush.clear() : brush.extent(extent));
          $scope.filter[selector] = brush.empty() ? null : extent;
        })
        .on("brushend", $scope.change);

      var brushg = svg.append("g")
        .attr("class", "brush")
        .call(brush);

      brushg.selectAll(".resize").append("path")
        .attr("transform", "translate(0," +  h / 2 + ")")
        .attr("fill","lightgray")
        .attr("d",
          d3.svg.arc()
          .outerRadius(20)
          .startAngle(0)
          .endAngle(function(d, i) { return i ? -Math.PI : Math.PI; })
        );

      brushg.selectAll("rect")
        .attr("height", h);
    }
  });
