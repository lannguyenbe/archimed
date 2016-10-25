var app = angular.module('discoverApp', ['ngResource','nln.ui','nln.ui.utils'
   ,'template/handlebars/primary_query.html'
   ,'template/handlebars/hidden_primary_query.html'
   ,'template/handlebars/hbs_advanced_filters.html'
   ,'template/handlebars/hbs_simple_filters.html'
   ,'template/handlebars/hidden_advanced_filters.html']);
//
// Run
app.run(['$rootScope', function($rootScope) { 
  $rootScope.endPoint = 'http://localhost:8070/rs/service';
  $rootScope.resources = {
     authors: '/authors/names'
    ,authorsJ: 'authors.json'
    ,series: '/communities/titles'
  };
}]);
//
// Factories   
app.factory('Authors', ['$rootScope', '$resource', function($rootScope, $resource) {
//      return resource = $resource($rootScope.resources.authorsJ);
  return $resource($rootScope.endPoint + $rootScope.resources.authors);
}]);    
app.factory('Series', ['$rootScope', '$resource', function($rootScope, $resource) {
//      return resource = $resource($rootScope.resources.authorsJ);
  return $resource($rootScope.endPoint + $rootScope.resources.series);
}]);    
//
// Controller
app.controller('SearchBoxController', ['$scope', '$rootScope', '$window', 'focus', 'Authors', 'Series',
function($scope, $rootScope, $window, focus, Authors, Series) {


   /* Primary search : free text query and community scope */
    $scope.primaryQuery = $window.DSpace.discovery.query;
    if (typeof $window.DSpace.discovery !== 'undefined') {
      if ($scope.primaryQuery.length == 0) {
         $scope.primaryQuery.push({
            scope: '',
            query: ''
         });
      }
    }
    $scope.scopeList = $window.DSpace.i18n.discovery.scope;

   /* Filters */
    $scope.filters = $window.DSpace.discovery.filters;
    backupOriginalFilters();
    if (typeof $window.DSpace.discovery !== 'undefined') {
      if ($scope.filters.length == 0) { addNewFilter(0); }
    }

    $scope.filterTypes = $window.DSpace.i18n.discovery.filtertype;
    $scope.filterTypesKeys = Object.keys($scope.filterTypes);
    $scope.relOperators = $window.DSpace.i18n.discovery.filter_relational_operator;
    $scope.relOperatorsKeys = Object.keys($scope.relOperators);

    function addNewFilter(idx, type, relop, query) {
      if (idx == undefined) {
          $scope.filters.push({
                type: null,
                relational_operator: null,
                query: ''                    
          });
      }
      else if (type) {
          $scope.filters.splice(idx, 0, {
                type: type,
                relational_operator: relop,
                query: query                    
          });          
      } else {
          $scope.filters.splice(idx, 0, {
                type: null,
                relational_operator: null,
                query: ''                    
          });
      }
    }

    function updateFilter(idx, type, relop, query) {
      if (type) { /* TODO */ }
      if (relop) { $scope.filters[idx].relational_operator = relop; }
      if (query) { /* TODO */ }
    }
          
    function removeFilterAtIndex(idx) {
      $scope.filters.splice(idx, 1);
      if ($scope.filters.length == 0) { addNewFilter(0); }       
    }

    $scope.addFilter = function (idx) {
      if (idx == undefined) { 
        addNewFilter(); 
      } else {
        addNewFilter(idx+1); 
      }
    }

    $scope.addNewFilter = addNewFilter;
    
    $scope.updateFilter = updateFilter;

    $scope.removeFilter = removeFilterAtIndex;

    $scope.removeFilterAtLabel = function (idx) {
        removeFilterAtIndex(idx); 
        // then submit the form
        $scope.formCommit();
    }


    $scope.notempty = function(val, idx) {
        return (val.query ? true : false);
    }

    function backupOriginalFilters() {
        $scope.orig_filters = angular.copy($scope.filters);
    }

    $scope.restoreOriginalFilters = function() {
        angular.copy($scope.orig_filters, $scope.filters);
        if ($scope.filters.length == 0) { addNewFilter(0); }

    }


   /* Typa */
    $scope.getAuthors = function (pattern, success, error) {
      return(Authors.query({pt: pattern}, success, error));
    };

    $scope.getSeries = function (pattern, success, error) {
      return(Series.query({pt: pattern}, success, error));
    }
    
    /* TODO - make this list .json - reloadable once per day from solr */
    $scope.identifierAttributors = [ /* quick and dirty !!! */
        {"name":"Sonuma TV"}
        ,{"name":"Tramontane Radio"}
        ,{"name":"Tramontane TV"}
    ];


}]);

//
// function $scope.<forms.formName>.commit() to trigger submit of the form 
app.directive("ngFormCommit", [function(){
    return {
        require:"form",
        link: function(scope, el, attrs, form) {
            scope.formCommit = function() {
               scope.$apply();
               el[0].submit.click();
            };
        }
    };
}]);

//
// Directives to move the input field corresponding with the resource on top
app.directive('typaInputCtrl', function() {
  return {
    controller: function($scope) {
      $scope.enableInput = function (filterType) {
         $scope.$broadcast('filterType', filterType);
      }
    }
  };
});


app.directive('typaInputAuthor', function() {
  return {
    controller: function($scope, $element) {
      function init() {
         enableInput({}, $scope.fil.type);
      }

      function enableInput(ev, ft) {
         if (ft == 'author') { 
            // enable
            $element.removeAttr('disabled').show('fast');
         } else {
            $element.attr('disabled','true').hide('normal');
         }
      }

      $scope.onSelectAddAuthor = function(idx, selVal) {
         $scope.addNewFilter(idx, 'author', 'equals' , selVal);
      }

      $scope.$on('filterType', enableInput);
      init();
    } //controller
  };
});


app.directive('typaInputTitleSerie', function() {
  return {
    controller: function($scope, $element) {
      function init() {
         enableInput({}, $scope.fil.type);
      }

      function enableInput(ev, ft) {
         if (ft == 'titlecom') { 
            // enable
            $element.removeAttr('disabled').show('fast');
         } else {
            $element.attr('disabled','true').hide('normal');
         }
      }

      $scope.onSelectAddTitleSerie = function(idx, selVal) {
         $scope.addNewFilter(idx, 'titlecom', 'equals' , selVal);
      }

      $scope.$on('filterType', enableInput);
      init();
    } //controller
  };
});

app.directive('typaInputIdentifierAttributor', function() {
  return {
    controller: function($scope, $element) {
      function init() {
         enableInput({}, $scope.fil.type);
      }

      function enableInput(ev, ft) {
         if (ft == 'identifier_attributor') { 
            // enable
            $element.removeAttr('disabled').show('fast');
         } else {
            $element.attr('disabled','true').hide('normal');
         }
      }

      $scope.onSelectAddIdentifierAttributor = function(idx, selVal) {
         $scope.addNewFilter(idx, 'identifier_attributor', 'equals' , selVal);
      }
      
      $scope.onSelectIdentifierAttributor = function(idx, selVal) {
         $scope.updateFilter(idx, null, 'equals' , null);
      }


      $scope.$on('filterType', enableInput);
      init();
    } //controller
  };
});


app.directive('typaInputNone', function() {
  return {
    controller: function($scope, $element) {
      function init() {
         enableInput({}, $scope.fil.type);
      }

      function enableInput(ev, ft) {
         if (ft != 'author' && ft != 'titlecom' && ft != 'identifier_attributor') { 
            // enable
            $element.removeAttr('disabled').show('fast');
         } else {
            $element.attr('disabled','true').hide('normal');
         }
      }

      $scope.$on('filterType', enableInput);
      init();
    } //controller
  };
});


angular.module('template/handlebars/hidden_primary_query.html',[])
.run(['$templateCache', function ($templateCache) {
   $templateCache.put('template/handlebars/hidden_primary_query.html',
       '<p class="ds-paragraph">'
      +'    <input id="aspect_discovery_SimpleSearch_field_query" class="ds-hidden-field form-control" type="hidden" name="query" value="{{primaryQuery[0].query}}">'
      +'    <input id="aspect_discovery_SimpleSearch_field_scope" class="ds-hidden-field form-control" type="hidden" name="scope" value="{{primaryQuery[0].scope}}">'
      +'</p>'
      +'');
}]);


angular.module('template/handlebars/primary_query.html', [])
.run(['$templateCache', function ($templateCache) {
   $templateCache.put('template/handlebars/primary_query.html',
       '<div class="col-sm-3">'
      +' <p>'
      +'    <select id="aspect_discovery_SimpleSearch_field_scope" class="ds-select-field form-control" name="scope" ng-model="primaryQuery[0].scope" ng-init="primaryQuery[0].scope = primaryQuery[0].scope||scopeList[0].id">'
      +'        <option ng-repeat="sco in scopeList" value="{{sco.id}}" ng-selected="primaryQuery[0].scope== sco.id">{{sco.label}}</option>'
      +'    </select>'
      +'</p>'
      +'</div>'
      +'<div class="col-sm-9">'
      +' <p class="input-group">'
      +'    <input id="aspect_discovery_SimpleSearch_field_query" class="ds-text-field form-control" name="query" type="text" ng-model="primaryQuery[0].query"><span class="input-group-btn"><button id="aspect_discovery_SimpleSearch_field_submit" class="ds-button-field btn btn-default search-icon search-icon" name="submit" type="submit">Go</button></span>'
      +'</p>'
      +'</div>'
      +'');
}]);

angular.module('template/handlebars/hidden_advanced_filters.html',[])
.run(['$templateCache', function ($templateCache) {
   $templateCache.put('template/handlebars/hidden_advanced_filters.html',
       '<p ng-repeat="fil in filters | filter:notempty" class="ds-paragraph">'
      +'    <input id="aspect_discovery_SimpleSearch_field_filtertype_{{$index}}" class="ds-hidden-field form-control" name="filtertype_{{$index}}" type="hidden" value="{{fil.type}}">'
      +'    <input id="aspect_discovery_SimpleSearch_field_filter_relational_operator_{{$index}}" class="ds-hidden-field form-control" name="filter_relational_operator_{{$index}}" type="hidden" value="{{fil.relational_operator}}">'
      +'    <input id="aspect_discovery_SimpleSearch_field_filter_{{$index}}" class="ds-hidden-field form-control" name="filter_{{$index}}" type="hidden" value="{{fil.query}}">'
      +'</p>'
      +'');
}]);

angular.module('template/handlebars/hbs_simple_filters.html',[])
.run(['$templateCache', function ($templateCache) {
   $templateCache.put('template/handlebars/hbs_simple_filters.html',
       '    <label ng-repeat="fil in filters | filter:notempty"  ng-click="removeFilterAtLabel($index)" href="#" class="label label-primary">{{fil.query}}&nbsp;&times;</label>'
      +'');
}]);

angular.module('template/handlebars/hbs_advanced_filters.html', [])
.run(['$templateCache', function ($templateCache) {
   $templateCache.put('template/handlebars/hbs_advanced_filters.html',
       '<div typa-input-ctrl ng-repeat="fil in filters" id="aspect_discovery_SimpleSearch_row_filter-new-{{$index}}"'
      +'     class="ds-form-item row advanced-filter-row search-filter">'
      +'    <div class="col-xs-4 col-sm-2">'
      +'        <p>'
      +'            <select id="aspect_discovery_SimpleSearch_field_filtertype_{{$index}}" class="ds-select-field form-control"'
      +'                    name="filtertype_{{$index}}" ng-model="fil.type" ng-init="fil.type = fil.type||filterTypesKeys[0]"'
      +'                    ng-change="enableInput(fil.type)">'
      +'                <option ng-repeat="ft in filterTypes" value="{{filterTypesKeys[$index]}}" ng-selected="fil.type==filterTypesKeys[$index]">{{filterTypes[filterTypesKeys[$index]]}}</option>'
      +'            </select>'
      +'        </p>'
      +'    </div>'
      +'    <div class="col-xs-4 col-sm-2">'
      +'        <p>'
      +'            <select id="aspect_discovery_SimpleSearch_field_filter_relational_operator_{{$index}}"'
      +'                    class="ds-select-field form-control" name="filter_relational_operator_{{$index}}" ng-model="fil.relational_operator" ng-init="fil.relational_operator = fil.relational_operator||relOperatorsKeys[0]">'
      +'                <option ng-repeat="op in relOperators" value="{{relOperatorsKeys[$index]}}" ng-selected="fil.relational_operator==relOperatorsKeys[$index]">{{relOperators[relOperatorsKeys[$index]]}}</option>'
      +'            </select>'
      +'        </p>'
      +'    </div>'
      +'    <div class="col-xs-4 col-sm-6">'
/*      +'        <p ng-init="fil.selected = []">'*/
      +'        <p>'
      +'            <input id="aspect_discovery_SimpleSearch_field_filter_{{$index}}"'
      +'                   typa-input-none'
      +'                   class="ds-text-field form-control discovery-filter-input discovery-filter-input"'
      +'                   name="filter_{{$index}}" type="text" ng-model="fil.query">'
      +'            <input id="aspect_discovery_SimpleSearch_field_filter_{{$index}}"'
/*      +'                   typa-input-author autocomplete="off" nln-typahead nln-typa-resource="getAuthors" nln-typa-not-filter="true" nln-typa-selected="fil.selected" nln-typa-callback="onSelectAddAuthor"'*/
      +'                   typa-input-author autocomplete="off" nln-typahead nln-typa-resource="getAuthors" nln-typa-not-filter="true" nln-typa-callback-add="onSelectAddAuthor"'
      +'                   class="ds-text-field form-control discovery-filter-input discovery-filter-input"'
      +'                   name="filter_{{$index}}" type="text" ng-model="fil.query">'
      +'            <input id="aspect_discovery_SimpleSearch_field_filter_{{$index}}"'
/*      +'                   typa-input-title-serie autocomplete="off" nln-typahead nln-typa-resource="getSeries" nln-typa-not-filter="true" nln-typa-selected="fil.selected" nln-typa-callback="onSelectAddTitleSerie"'*/
      +'                   typa-input-title-serie autocomplete="off" nln-typahead nln-typa-resource="getSeries" nln-typa-not-filter="true" nln-typa-callback-add="onSelectAddTitleSerie"'
      +'                   class="ds-text-field form-control discovery-filter-input discovery-filter-input"'
      +'                   name="filter_{{$index}}" type="text" ng-model="fil.query">'
      +'            <input id="aspect_discovery_SimpleSearch_field_filter_{{$index}}"'
/*      +'                   typa-input-identifier-attributor autocomplete="off" nln-typahead nln-typa-resource="identifierAttributors" nln-typa-not-filter="true" nln-typa-selected="fil.selected" nln-typa-callback="onSelectAddIdentifierAttributor"'*/
      +'                   typa-input-identifier-attributor autocomplete="off" nln-typahead nln-typa-resource="identifierAttributors" nln-typa-not-filter="true" nln-typa-callback="onSelectIdentifierAttributor" nln-typa-callback-add="onSelectAddIdentifierAttributor"'
      +'                   class="ds-text-field form-control discovery-filter-input discovery-filter-input"'
      +'                   name="filter_{{$index}}" type="text" ng-model="fil.query">'
/* TODO to remove */      
/*      +'{{fil.selected}}'*/
      +'        </p>'
      +'    </div>'
      +'    <div class="hidden-xs col-sm-2">'
      +'        <div class="btn-group btn-group-justified">'
      +'                <p class="btn-group">'
      +'                    <button id="aspect_discovery_SimpleSearch_field_add-filter_{{$index}}" ng-click="addFilter($index)"'
      +'                            class="ds-button-field btn btn-default filter-control filter-add filter-control filter-add"'
      +'                            name="add-filter_{{$index}}" type="button" title="Add Filter"><span'
      +'                            class="glyphicon glyphicon-plus-sign" aria-hidden="true"></span></button>'
      +'                </p>'
      +'                <p class="btn-group">'
      +'                    <button id="aspect_discovery_SimpleSearch_field_remove-filter_{{$index}}" ng-click="removeFilter($index)"'
      +'                            class="ds-button-field btn btn-default filter-control filter-remove filter-control filter-remove"'
      +'                            name="remove-filter_{{$index}}" type="button" title="Remove"><span'
      +'                            class="glyphicon glyphicon-minus-sign" aria-hidden="true"></span></button>'
      +'                </p>'
      +'        </div>'
      +'    </div>'
      +'</div>'
      +'');
}]);































































;






































































































































































