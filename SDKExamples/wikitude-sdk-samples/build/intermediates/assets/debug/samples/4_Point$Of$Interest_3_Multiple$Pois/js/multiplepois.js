// implementation of AR-Experience (aka "World")
var World = {
	// true once data was fetched
	initiallyLoadedData: false,

	// different POI-Marker assets
	markerDrawable_idle: null,
	markerDrawable_selected: null,

	// list of AR.GeoObjects that are currently shown in the scene / World
	markerList: [],

	// The last selected marker
	currentMarker: null,

	// called to inject new POI data
	loadPoisFromJsonData: function loadPoisFromJsonDataFn(poiData) {
		// empty list of visible markers
		World.markerList = [];

		// start loading marker assets
		World.markerDrawable_idle = new AR.ImageResource("assets/marker_idle.png");
		World.markerDrawable_selected = new AR.ImageResource("assets/marker_selected.png");

		// loop through POI-information and create an AR.GeoObject (=Marker) per POI
		for (var currentPlaceNr = 0; currentPlaceNr < poiData.length; currentPlaceNr++) {
			var singlePoi = {
				"id": poiData[currentPlaceNr].id,
				"latitude": parseFloat(poiData[currentPlaceNr].latitude),
				"longitude": parseFloat(poiData[currentPlaceNr].longitude),
				"altitude": parseFloat(poiData[currentPlaceNr].altitude),
				"title": poiData[currentPlaceNr].name,
			    "description": "",
			    "otherThing": poiData[currentPlaceNr].description
			};

			/*
				To be able to deselect a marker while the user taps on the empty screen, 
				the World object holds an array that contains each marker.
			*/
			World.markerList.push(new Marker(singlePoi));
		}

		World.updateStatusMessage(currentPlaceNr + ' places loaded');
	},

	// updates status message shon in small "i"-button aligned bottom center
	updateStatusMessage: function updateStatusMessageFn(message, isWarning) {

		var themeToUse = isWarning ? "e" : "c";
		var iconToUse = isWarning ? "alert" : "info";

		$("#status-message").html(message);
		$("#popupInfoButton").buttonMarkup({
			theme: themeToUse
		});
		$("#popupInfoButton").buttonMarkup({
			icon: iconToUse
		});
	},

	// location updates, fired every time you call architectView.setLocation() in native environment
	locationChanged: function locationChangedFn(lat, lon, alt, acc) {

		/*
			The custom function World.onLocationChanged checks with the flag World.initiallyLoadedData if the function was already called. With the first call of World.onLocationChanged an object that contains geo information will be created which will be later used to create a marker using the World.loadPoisFromJsonData function.
		*/
		if (!World.initiallyLoadedData) {
			/* 
				requestDataFromLocal with the geo information as parameters (latitude, longitude) creates different poi data to a random location in the user's vicinity.
			*/
			World.requestDataFromLocal(lat, lon);
			World.initiallyLoadedData = true;
		}
	},

	// fired when user pressed maker in cam
	onMarkerSelected: function onMarkerSelectedFn(marker) {

		// deselect previous marker
		if (World.currentMarker) {
			if (World.currentMarker.poiData.id == marker.poiData.id) {
				return;
			}
			World.currentMarker.setDeselected(World.currentMarker);
		}

		// highlight current one
		marker.setSelected(marker);
		World.currentMarker = marker;
	},

	// screen was clicked but no geo-object was hit
	onScreenClick: function onScreenClickFn() {
		if (World.currentMarker) {
			World.currentMarker.setDeselected(World.currentMarker);
		}
	},

	// request POI data
	requestDataFromLocal: function requestDataFromLocalFn(centerPointLatitude, centerPointLongitude) {
		var poisToCreate = 10;
		var poiData = [];
/*
	    for (var i = 0; i < poisToCreate; i++) {  //Create mini-database for showcase
			poiData.push({
				"id": (i + 1),
				"longitude": (centerPointLongitude + (Math.random() / 5 - 0.1)),
				"latitude": (centerPointLatitude + (Math.random() / 5 - 0.1)),
				"description": ("This is our description!" + (i + 1)),
				"altitude": "100.0",
                "name": ("POI#" + (i + 1) + "!")      //Name of POI
			});
		}
*/
        poiData.push({
        				"id": 1,
        				"longitude": -80.540149,
        				"latitude": 43.472797,
        				"description": ("Our table"),
        				"altitude": "100.0",
                        "name": ("HTN HQ")      //Name of POI
        			});

        poiData.push({
        				"id": 2,
        				"longitude": -80.539230,
        				"latitude": 43.472245,
        				"description": ("Restaurant"),
        				"altitude": "100.0",
                        "name": ("Mongolian Grill")      //Name of POI
        			});


        poiData.push({
        				"id": 3,
        				"longitude": -80.537739,
        				"latitude": 43.472226,
        				"description": ("Restaurant"),
        				"altitude": "100.0",
                        "name": ("Aunty's Kitchen")      //Name of POI
        			});

        poiData.push({
        				"id": 4,
        				"longitude": -80.537449,
        				"latitude": 43.472919,
        				"description": ("Restaurant"),
        				"altitude": "100.0",
                        "name": ("Molly Bloom's Irish Pub")      //Name of POI
        			});

        poiData.push({
        				"id": 5,
        				"longitude": -80.541248,
                        "latitude": 43.472750,
                        "description": ("Bus Station"),
                        "altitude": "100.0",
                        "name": ("Davis Centre")      //Name of POI
        			});

        poiData.push({
        				"id": 6,
        				"longitude": -80.541681,
        				"latitude": 43.473285,
        				"description": ("Bus Station"),
        				"altitude": "100.0",
                        "name": ("University of Waterloo")      //Name of POI
        			});

		World.loadPoisFromJsonData(poiData);
	}

};

/* 
	Set a custom function where location changes are forwarded to. There is also a possibility to set AR.context.onLocationChanged to null. In this case the function will not be called anymore and no further location updates will be received. 
*/
AR.context.onLocationChanged = World.locationChanged;

/*
	To detect clicks where no drawable was hit set a custom function on AR.context.onScreenClick where the currently selected marker is deselected.
*/
AR.context.onScreenClick = World.onScreenClick;