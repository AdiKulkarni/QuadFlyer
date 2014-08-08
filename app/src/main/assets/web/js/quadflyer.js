var app = angular.module('quadflyer', []);

app.controller('MainCtrl', function($scope, $http) {
    $scope.update = function(key, val) {
        var url = 'api/videoData?';
        url += 'key='+key+'&';
        url += 'value=' + val;

        $http.get(url).success(function(data, status) {
        });
    }

    $http.get('api/videoData').success(function(data, status) {
        $scope.data = data;
    }).error(function(data, status) {
        alert("Error getting camera data");
    });
});
