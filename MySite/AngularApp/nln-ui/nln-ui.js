  angular.module('nln.ui',['nln.ui.tpls', 'nln.ui.typeahead']);
  angular.module('nln.ui.tpls',['template/typeahead/typa-container.html']);
  angular.module('nln.ui.typeahead',['ngResource','nln.ui.utils'])
    //
    // Directives
    .directive('nlnTypaPopup', function () {
      return {
        restrict: 'E'
       ,replace: true
       ,templateUrl: 'template/typeahead/typa-container.html'
      }
    })
    .directive('nlnTypahead', function ($compile, $timeout, $filter, $parse, $document) {
      return {
        restrict: 'A'
       ,require:'ngModel'
       ,link: function postLink(originalScope, element, attrs, ngModelCtrl) {

          // Mandatory attributes
          // resource where to search from
          var resource = originalScope.$eval(attrs.nlnTypaResource);

          // Supported attributes
          // do not apply filter on results returned from resource
          var notFilter = originalScope.$eval(attrs.nlnTypaNotFilter) || false;
          // minimal nb of characters that needs to be entered before typeahead kicks-in
          var minLength = originalScope.$eval(attrs.nlnTypaMinLength) || 1;
          // minimal wait time after last character typed before typehead kicks-in
          var waitTime = originalScope.$eval(attrs.nlnTypaWaitMs) || 0;

          // Setter : given a string representing the name of the property, 
          // use setter to assign value to that property.
          // setter for input field
          var ngModelGetter = $parse(attrs.ngModel); // is a function
          var ngModelSetter = ngModelGetter.assign; // is a function
          // setter for output container where selected values will be returned
          if (attrs.nlnTypaSelected) {
            var selectedSetter = $parse(attrs.nlnTypaSelected).assign;
          }

          // internal variables & local functions
          var hasFocus;
          var timeoutPromise;
          var forceClose;
          var activeIdx;

          var selectedList = []; // keep the selected items
          if (selectedSetter) { selectedSetter(originalScope, selectedList); }

          // initialize keydown event handlers
          var keys = [];
          var handledKeys = {}; // calulated from keys on init
          keys.push({keyCode: 40, keyName:"down", handle: function() { if (isPopupOpen()) { incrActiveIdx(1); }}});
          keys.push({keyCode: 38, keyName:"up", handle: function() { if (isPopupOpen()) { incrActiveIdx(-1); }}});
          keys.push({keyCode: 13, keyName:"enter", handle: function() { if (isPopupOpen()) { select(activeIdx); } }});
          keys.push({keyCode: 27, keyName:"esc", handle: function() { forceClose = ((scope.hitsList||[]).length > 0); }});
          keys.push({keyCode: 'ctrl+13', keyName:"ctrl+enter", handle: function() { if ((scope.hitsList||[]).length > 0 && !forceClose) { selectAdd(activeIdx); } }});
 
          for (var i = 0; i < keys.length; i++) {
            handledKeys[keys[i].keyCode] = keys[i].keyName;
          }

          function isPopupOpen() {
            if (forceClose) { // popup is hidden 
              forceClose = false;
              return false;
            } else if ((scope.hitsList||[]).length) { // popup is already open
              return true;
            } else if (ngModelCtrl.$viewValue) { // popup is not populated
              getHits(ngModelCtrl.$viewValue);
            }
            return false;
          }

          function incrActiveIdx(n) {
            var modulo = (scope.filterResults) ? scope.filterResults.length : scope.hitsList.length;
            activeIdx = (activeIdx+modulo+n)%modulo;
          }

          function select(idx, n) {
            var nbr = n || 1;
            selectAdd(idx, nbr);
            ngModelSetter(originalScope, scope.hitsList[idx][scope.label]);
            forceClose = true; // hide               	
          }

          function selectAdd(idx, n) {
            var nbr = n || 1;
            for (var i = 0; i < nbr; i++, idx++) {
              if (selectedList.indexOf(scope.hitsList[idx][scope.label]) >= 0) { continue; }
              selectedList.push(scope.hitsList[idx][scope.label]);          
            }
          }

          // bind keydown to input field
          element.bind('keydown', function (ev) {
            var kcode = (ev.ctrlKey) ? 'ctrl+'+ev.keyCode : ev.keyCode;
            var key = handledKeys[kcode];

            if ( !key || ev.shiftKey || ev.altKey ) { return; } 

            ev.preventDefault();
            ev.stopPropagation();

            keys.forEach(function(o) {
              if (o.keyName !== key) {return;}
              o.handle();
              scope.$apply(); // trigger all listeners to propagate changes
            });
          });

          element.bind('blur', function (ev) {
            hasFocus = false;
          });

          // Keep reference to click handler to unbind it.
          var dismissClickHandler = function (ev) {
            if (element[0] !== ev.target) {
              forceClose = true;
              // scope.$digest();
            }
          };

          $document.bind('click', dismissClickHandler);

          originalScope.$on('$destroy', function(){
            $document.unbind('click', dismissClickHandler);
          });          

 
          // Preparing to query resource
          function scheduleSearchWithTimeout(inputValue) {
            timeoutPromise = $timeout(function() {
               getHits(inputValue);
            }, waitTime);
          }

          function cancelPreviousTimeout() {
            if (timeoutPromise) { $timeout.cancel(timeoutPromise); }
          }

          function resetHitsList() {
            if ((scope.hitsList||[]).length > 0) { scope.hitsList = []; }
            activeIdx = undefined;
            forceClose = false;
          }

          var getHits;
          if (resource instanceof Function) { 
             if (notFilter)
               getHits = getHitsAsyncNotFilter; 
             else
               getHits = getHitsAsync; 
          } else { 
             getHits = getHitsStatic; 
          }

          function getHitsAsync(pattern) {
            resetHitsList();
            resource(pattern, function (d) { // on success
               if (hasFocus && d.length) {
                 scope.hitsList = $filter('filter')(d, {$: pattern});
                 scope.label = Object.keys(d[0])[0]; // first property
                 activeIdx = 0;
               }
            }, function (e) { // on error
               console.log(e); // TODO retry
            });
          }

          function getHitsAsyncNotFilter(pattern) {
            resetHitsList();
            scope.hitsList = resource(pattern, function (d) { // on success
               if (hasFocus && d.length) {
                 scope.label = Object.keys(d[0])[0]; // first property
                 activeIdx = 0;
               }
            }, function (e) { // on error
               console.log(e); // TODO retry
            });
          }

          function getHitsStatic(pattern) {
            resetHitsList();
            var d = scope.hitsList = $filter('filter')(resource, {$: pattern});
            if (d.length) { 
              scope.label = Object.keys(d[0])[0]; // first property
              activeIdx = 0;
            }
          }

          // use $parsers to query resource when input changes
          // ngModelCtrl $parsers kick-in on all the changes coming from the view as well as manually triggered by $setViewValue
          ngModelCtrl.$parsers.unshift(function(inputValue) {

            hasFocus = true;

            if (inputValue && inputValue.length >= minLength) {
              if (waitTime > 0) {
                cancelPreviousTimeout();
                scheduleSearchWithTimeout(inputValue);
              } else {
                getHits(inputValue);
              }
            } else {
              cancelPreviousTimeout();
              resetHitsList();
            }
          });


          // Create child scope for popup
          var scope = originalScope.$new();
          originalScope.$on('$destroy', function(){
            scope.$destroy();
          });
          // Add properties to child scope
          scope.hitsList = [];

          // Add methods to child scope
          scope.isEmpty = function () {
            return ((scope.hitsList||[]).length == 0);
          }

          scope.isForceClose = function () {
            return (forceClose ? true : false);
          }

          scope.isActive = function (idx) {
            return (activeIdx === idx);
          }

          scope.setActive = function (idx) {
            activeIdx = idx;
          }

          scope.selectActive = function (ev) {
            if (ev.ctrlKey)
              selectAdd(activeIdx);
            else
              select(activeIdx);

            // return focus to the input element if a match was selected via a mouse click event
            // use timeout to avoid $rootScope:inprog error
            $timeout(function() { element[0].focus(); }, 0, false);
          }

          scope.selectAll = function (ev) {
            var all = (scope.filterResults) ? scope.filterResults.length : scope.hitsList.length;
            selectAdd(0, all);
            select(activeIdx);

            // return focus to the input element if a match was selected via a mouse click event
            // use timeout to avoid $rootScope:inprog error
            $timeout(function() { element[0].focus(); }, 0, false);
          }


          // Create popup element
          var popUpEl = angular.element('<nln-typa-popup/>');

          // Compile the popup and attach it to the DOM
          var popup = $compile(popUpEl)(scope);
          element.after(popup);
        }
      } // return link
    });

    angular.module('nln.ui.utils',[])
    .factory('focus', ['$timeout', function($timeout) {
      return function(id) {
        $timeout(function() {
          var element = document.getElementById(id);
          if (element) { element.focus(); }
        });
      }
    }]);
    
    angular.module('template/typeahead/typa-container.html',[])
    .run(['$templateCache', function($templateCache) {
      $templateCache.put('template/typeahead/typa-container.html',
        '<div class="typa-container" ng-show="!isEmpty() && !isForceClose()" style="width: 234px;">\n'+
        '  <div class="typa-item" ng-repeat="item in hitsList | limitTo:7 as filterResults" ng-click="selectActive($event)"\n'+
        '    ng-class="{\'active\': isActive($index)}"\n'+
        '    ng-mouseenter="setActive($index)">\n'+
        '    <span class="typa-icon typa-icon-marker"></span>\n'+
        '    <span class="typa-item-query">{{item[label]}}</span>\n'+
        '  </div>\n'+
        '  <div>\n'+
        '    <button ng-if="filterResults.length > 1" ng-click="selectAll($event)" type="button">Simone, envoie tout !</button>\n'+
        '  </div>\n'+
        '</div>\n'+
        '');
    }]);
