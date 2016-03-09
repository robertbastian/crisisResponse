'use strict';

angular.module('crisisResponse.selection', ['ngRoute'])

  .config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/selection', {
      templateUrl: 'views/selection.html',
      controller: 'SelectionController'
    });
  }])

  .controller('SelectionController', function($scope, gloVars, $location,$http) {
    $scope.filter = gloVars.filter();
    $scope.collection = gloVars.collectionDetails();
    $scope.count = {};

    $scope.change = function(){
      $http.post("/api/count",$scope.filter).success(function(response){
        $scope.count = response;
      })
    };
    $scope.change();


    var histograms = [{
      selector: "sentiment",
      buckets: 5,
      color: "cyan"
    },{
      selector: "corroboration",
      buckets: 10,
      color: "cyan"
    },{
      selector: "competence",
      buckets: 10,
      color: "cyan"
    },{
      selector: "popularity",
      buckets: 10,
      color: "cyan"
    },{
      selector: "time",
      buckets: 20,
      color: "cyan"
    }];

    histograms.forEach(function(o){
      $http.post("/api/histogram/"+ o.selector + "/" + o.buckets,$scope.filter).success(function(response){
        makeHistogram(o.selector,response.buckets, response.min, response.max, o.color)
      })
    });

    function makeHistogram(selector,data,minX,maxX,color) {
      var obj = $('#'+selector);

      var barPadding = 1,
        w = obj.width(),
        h = obj.height(),
        max = Math.max(...data);

      var svg = d3.select('#'+selector)
        .append("svg")
        .attr("width", w)
        .attr("height", h);

      var x = d3.scale.linear().domain([minX,maxX]).range([20,w-20]);

      svg.selectAll("rect")
        .data(data)
        .enter()
        .append("rect")
        .attr("x", function (d, i) { return 20 +  i * ((w-40) / data.length); })
        .attr("y", function (d) { return h - (d / max * h); })
        .attr("width", (w-40) / data.length - barPadding)
        .attr("height", function (d) { return d / max * h; })
        .attr("fill", color);

      var brush = d3.svg.brush().x(x);

      brush
        .on("brushend", function() {
          $scope.filter[selector] = brush.empty() ? null : brush.extent();
          $scope.change();
        })
        .on("brush", function(){
          var extent = brush.extent().map(Math.round);
          d3.select(this).call(extent[0] == extent[1] ? brush.clear() : brush.extent(extent));
      });

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
