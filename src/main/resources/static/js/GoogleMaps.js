var map = null;
var bounds = null;

function mapLoadViewer(mapDivId, mapType, north, south, east, west, onTilesLoaded){
    // setup the Viewer corner points
    var sw = new google.maps.LatLng(south, west);
    var ne = new google.maps.LatLng(north, east);

    // setup the Viewer bounds
    bounds = new google.maps.LatLngBounds(sw, ne);

    // setup the Map (using the ROADMAP as default)
    var mapOptions = {
        center: bounds.getCenter(),
        mapTypeId: mapType,
        draggable: true,
        keyboardShortcuts: true,
        mapTypeControl: true,
        navigationControl: true,
        scaleControl: true,
        scrollwheel: true,
        streetViewControl: true,
        noClear: true
    };
    map = new google.maps.Map(document.getElementById(mapDivId), mapOptions);

    // fit the Map to the Viewer Bounds
    map.fitBounds(bounds);

    if (onTilesLoaded !== undefined && onTilesLoaded !== null) {
        google.maps.event.addListenerOnce(map, 'tilesloaded', onTilesLoaded);
    }
}

function addCircleToMap(lat, lon, r, colour, desc) {
    if (map != null) {
    	console.log("Adding circle for " + desc);

        // setup lat/lon point
        var point = new google.maps.LatLng(lat, lon);

        // extend map to include point location if necessary
        if (bounds == null || bounds == undefined)
        {
            bounds = map.getBounds();
            if (bounds != null && bounds != undefined && !bounds.contains(point)) {
                bounds.extend(point);
            }
        }

        // create circle on map for point
        var circleOptions = {
            center: point,
            strokeColor: colour,
            strokeOpacity: 0.8,
            strokeWeight: 2,
            fillColor: colour,
            fillOpacity: 0.35,
            radius: r,
            map: map
        };
        var circle = new google.maps.Circle(circleOptions);

        // setup info window for circle
        if (desc != null && desc !== "") {
            var infoOptions = {
                content: desc,
                position: point
            };
            var infoWindow = new google.maps.InfoWindow(infoOptions);

            // on-click event for the marker
            google.maps.event.addListener(circle, 'click', function(){ infoWindow.open(map); });
        }
    }
}
