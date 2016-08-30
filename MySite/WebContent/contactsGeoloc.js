var autocomplete;
var componentForm = {
  street_number: 'short_name',
  route: 'long_name',
  locality: 'long_name',
  country: 'long_name',
  postal_code: 'short_name'
};

function initialize() {
  // Create the autocomplete object, restricting the search
  // to geographical location types.
	autocomplete = new google.maps.places.Autocomplete(
		document.inContact.inAddress
		, {types: ['geocode']});
  // When the user selects an address from the dropdown,
  // populate the address fields in the form.
	google.maps.event.addListener(autocomplete, 'place_changed', fillInAddress);;

	// 2è object autocomplete => 2è <div class="pac-container">
	// donc chaque objet a son propre <div> pac-container
	autocomplete2 = new google.maps.places.Autocomplete(
		document.inContact.inAddress2
		, {types: ['geocode']});
	google.maps.event.addListener(autocomplete2, 'place_changed', fillInAddress2);


	}

function fillInAddress() {
	var place = autocomplete.getPlace();
	for (var component in componentForm)
		document.getElementById(component).value = '';
	for (var i=0, len=place.address_components.length; i < len; i++) {
		var addressType = place.address_components[i].types[0];
		if (componentForm[addressType]) {
			var val = place.address_components[i][componentForm[addressType]];
			document.getElementById(addressType).value = val;
		}
	}
}

function fillInAddress2() {
	var place = autocomplete2.getPlace();
	for (var component in componentForm)
		document.getElementById(component).value = '';
	for (var i=0, len=place.address_components.length; i < len; i++) {
		var addressType = place.address_components[i].types[0];
		if (componentForm[addressType]) {
			var val = place.address_components[i][componentForm[addressType]];
			document.getElementById(addressType).value = val;
		}
	}
}

function geolocate(elem) {
	if (navigator.geolocation) {
		navigator.geolocation.getCurrentPosition(function(position) {
			var geolocation = new google.maps.LatLng(
				position.coords.latitude, position.coords.longitude);
			autocomplete.setBounds(new google.maps.LatLngBounds(geolocation
				, geolocation));
		});
        
	}
}

function geolocate2(elem) {
	if (navigator.geolocation) {
		navigator.geolocation.getCurrentPosition(function(position) {
			var geolocation = new google.maps.LatLng(
				position.coords.latitude, position.coords.longitude);
			autocomplete2.setBounds(new google.maps.LatLngBounds(geolocation
				, geolocation));
		});
        
	}
}


function fillElemDataList(elem) {
	var addr = elem.value;
	
	if (addr.length > 3) {
	var service = new google.maps.places.AutocompleteService();
	service.getQueryPredictions({ input: addr }, function(results, status) {
		if (status == google.maps.places.PlacesServiceStatus.OK) {
console.log(results);
			for (var i=0, len=(results.length > 10)?10:results.length; i < len; i++) {
				results[i].value = results[i].description;
			}
			fillDataList(elem.getAttribute("list"),results);
console.log(document.getElementById("geolocAddresses"));
		}
	});
	}
	
}

function fillDataList(listId, results) {
	var dataList = document.getElementById(listId);
	dataList.innerHTML = "";
	if (results.length) {
		for (var i=0, len=(results.length > 10)?10:results.length; i < len; i++) {
			var opt = document.createElement("option");
			opt.setAttribute("value", results[i].value);
			dataList.appendChild(opt);
		}
	}
}
